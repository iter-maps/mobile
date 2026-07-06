package it.iterapp.core.offline

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import it.iterapp.core.api.IterGatewayClient
import it.iterapp.core.api.iterHttpClient
import okio.Buffer
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class OfflineRepositoryTest {

  private fun bundleZip(): ByteArray {
    val manifest = """
      {"generator":"iter-gateway/1.0","createdAt":"2026-07-06T10:00:00Z",
       "bbox":[12.40,41.84,12.60,41.98],"minzoom":0,"maxzoom":14,
       "pmtiles":"area.pmtiles","styles":["light"],"glyphs":false,"sprite":false,
       "overlays":[],"note":"rewrite __BASE_URL__ to your unpack dir"}
    """.trimIndent()
    val style = """{"version":8,"sources":{"openmaptiles":{"type":"vector","url":"pmtiles://__BASE_URL__/area.pmtiles"}}}"""
    val entries = listOf(
      "area.pmtiles" to ByteArray(256) { it.toByte() },
      "styles/light.json" to style.encodeToByteArray(),
      "manifest.json" to manifest.encodeToByteArray(),
    )
    // Reuse the test builder from StoreZipTest via duplication-free helper:
    return StoreZipTestBuilder.build(entries)
  }

  @Test
  fun installUnpacksAndRewritesBaseUrl() = runTest {
    val fs = FakeFileSystem()
    val zipBytes = bundleZip()
    val engine = MockEngine { request ->
      assertEquals("/offline/bundle", request.url.encodedPath)
      assertEquals("12.4,41.84,12.6,41.98", request.url.parameters["bbox"])
      respond(
        Buffer().write(zipBytes).readByteArray(),
        headers = headersOf(HttpHeaders.ContentType, "application/zip"),
      )
    }
    val client = IterGatewayClient(iterHttpClient(engine)) { "http://gw.test" }
    val repo = OfflineRepository(client, fs, "/offline".toPath())

    var sawProgress = false
    val area = repo.install("roma", "12.4,41.84,12.6,41.98") { read, _ ->
      sawProgress = read > 0
    }

    assertTrue(sawProgress)
    assertEquals("roma", area.id)
    assertEquals(14, area.manifest.maxzoom)
    val style = fs.read("/offline/roma/styles/light.json".toPath()) { readUtf8() }
    assertTrue("__BASE_URL__" !in style)
    assertTrue("pmtiles://file:///offline/roma/area.pmtiles" in style)
    // The temp zip is cleaned up.
    assertTrue(fs.exists("/offline/roma.zip.part".toPath()).not())

    assertEquals(1, repo.list().size)
    repo.delete("roma")
    assertEquals(0, repo.list().size)
  }
}

/** Shared STORE-zip builder for tests. */
internal object StoreZipTestBuilder {
  fun build(entries: List<Pair<String, ByteArray>>): ByteArray {
    val out = Buffer()
    data class Central(val name: ByteArray, val size: Int, val offset: Long)

    val centrals = mutableListOf<Central>()
    entries.forEach { (name, content) ->
      val nameBytes = name.encodeToByteArray()
      val offset = out.size
      out.writeIntLe(0x04034b50)
      out.writeShortLe(20); out.writeShortLe(0); out.writeShortLe(0)
      out.writeShortLe(0); out.writeShortLe(0)
      out.writeIntLe(0)
      out.writeIntLe(content.size); out.writeIntLe(content.size)
      out.writeShortLe(nameBytes.size); out.writeShortLe(0)
      out.write(nameBytes); out.write(content)
      centrals.add(Central(nameBytes, content.size, offset))
    }
    val cdStart = out.size
    centrals.forEach { c ->
      out.writeIntLe(0x02014b50)
      out.writeShortLe(20); out.writeShortLe(20); out.writeShortLe(0); out.writeShortLe(0)
      out.writeShortLe(0); out.writeShortLe(0)
      out.writeIntLe(0)
      out.writeIntLe(c.size); out.writeIntLe(c.size)
      out.writeShortLe(c.name.size); out.writeShortLe(0); out.writeShortLe(0)
      out.writeShortLe(0); out.writeShortLe(0)
      out.writeIntLe(0)
      out.writeIntLe(c.offset.toInt())
      out.write(c.name)
    }
    val cdSize = out.size - cdStart
    out.writeIntLe(0x06054b50)
    out.writeShortLe(0); out.writeShortLe(0)
    out.writeShortLe(centrals.size); out.writeShortLe(centrals.size)
    out.writeIntLe(cdSize.toInt()); out.writeIntLe(cdStart.toInt())
    out.writeShortLe(0)
    return out.readByteArray()
  }
}
