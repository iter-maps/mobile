package it.iterapp.core.offline

import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class StoreZipTest {

  private fun buildStoreZip(entries: List<Pair<String, ByteArray>>): ByteArray =
    StoreZipTestBuilder.build(entries)

  @Test
  fun extractsEntriesWithNestedDirs() {
    val fs = FakeFileSystem()
    val zip = "/bundle.zip".toPath()
    val bytes = buildStoreZip(
      listOf(
        "manifest.json" to """{"pmtiles":"area.pmtiles"}""".encodeToByteArray(),
        "styles/light.json" to """{"version":8}""".encodeToByteArray(),
        "area.pmtiles" to ByteArray(1024) { (it % 251).toByte() },
      ),
    )
    fs.write(zip) { write(bytes) }

    val extracted = StoreZip.extract(fs, zip, "/out".toPath())

    assertEquals(3, extracted.size)
    assertEquals("""{"version":8}""", fs.read("/out/styles/light.json".toPath()) { readUtf8() })
    assertEquals(1024, fs.metadata("/out/area.pmtiles".toPath()).size)
  }

  @Test
  fun rejectsZipSlip() {
    val fs = FakeFileSystem()
    val zip = "/evil.zip".toPath()
    fs.write(zip) { write(buildStoreZip(listOf("../escape.txt" to "x".encodeToByteArray()))) }
    assertFailsWith<StoreZip.ZipException> {
      StoreZip.extract(fs, zip, "/out".toPath())
    }
  }

  @Test
  fun rejectsCompressedEntries() {
    val fs = FakeFileSystem()
    val zip = "/deflate.zip".toPath()
    // Hand-tweak: method byte lives at offset 8 of the local header and 10 of the central entry.
    val bytes = buildStoreZip(listOf("a.txt" to "hello".encodeToByteArray()))
    bytes[8] = 8 // local header method → DEFLATE
    val cdStart = bytes.size - 22 - (46 + 5)
    bytes[cdStart + 10] = 8 // central directory method → DEFLATE
    fs.write(zip) { write(bytes) }
    assertFailsWith<StoreZip.ZipException> {
      StoreZip.extract(fs, zip, "/out".toPath())
    }
  }
}
