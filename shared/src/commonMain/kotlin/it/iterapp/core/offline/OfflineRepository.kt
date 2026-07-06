package it.iterapp.core.offline

import it.iterapp.core.api.IterGatewayClient
import it.iterapp.core.api.IterJson
import it.iterapp.core.wire.BASE_URL_TOKEN
import it.iterapp.core.wire.OfflineManifest
import okio.FileSystem
import okio.Path
import okio.buffer
import okio.use

/** One downloaded offline area on disk. */
data class OfflineArea(
  /** Directory name under the offline root. */
  val id: String,
  val dir: Path,
  val manifest: OfflineManifest,
) {
  /** MapLibre style URL for this area (post-rewrite it points at local files). */
  fun styleUrl(styleName: String): String = "file://$dir/styles/$styleName.json"
}

/**
 * Downloads, unpacks and manages offline bundles. The bundle keeps the
 * literal `__BASE_URL__` token by contract; [install] rewrites it to the
 * unpacked directory's `file://` URL so the identical style renders offline.
 */
class OfflineRepository(
  private val client: IterGatewayClient,
  private val fileSystem: FileSystem,
  private val rootDir: Path,
) {

  /**
   * Downloads a bundle for [bbox] (`minLon,minLat,maxLon,maxLat`) and
   * installs it as [areaId]. Progress is (bytesRead, totalOrNull).
   */
  suspend fun install(
    areaId: String,
    bbox: String,
    maxzoom: Int? = null,
    onProgress: (Long, Long?) -> Unit = { _, _ -> },
  ): OfflineArea {
    fileSystem.createDirectories(rootDir)
    val zipPath = rootDir / "$areaId.zip.part"
    fileSystem.sink(zipPath).buffer().use { sink ->
      client.downloadOfflineBundle(bbox = bbox, maxzoom = maxzoom, sink = sink, onProgress = onProgress)
    }
    val areaDir = rootDir / areaId
    if (fileSystem.exists(areaDir)) fileSystem.deleteRecursively(areaDir)
    try {
      StoreZip.extract(fileSystem, zipPath, areaDir)
      rewriteStyles(areaDir)
      val manifest = readManifest(areaDir)
        ?: throw StoreZip.ZipException("bundle has no manifest.json")
      return OfflineArea(areaId, areaDir, manifest)
    } finally {
      fileSystem.delete(zipPath, mustExist = false)
    }
  }

  fun list(): List<OfflineArea> {
    if (!fileSystem.exists(rootDir)) return emptyList()
    return fileSystem.list(rootDir)
      .filter { fileSystem.metadataOrNull(it)?.isDirectory == true }
      .mapNotNull { dir ->
        readManifest(dir)?.let { OfflineArea(dir.name, dir, it) }
      }
  }

  fun delete(areaId: String) {
    val dir = rootDir / areaId
    if (fileSystem.exists(dir)) fileSystem.deleteRecursively(dir)
  }

  private fun readManifest(areaDir: Path): OfflineManifest? {
    val path = areaDir / "manifest.json"
    if (!fileSystem.exists(path)) return null
    return try {
      IterJson.decodeFromString<OfflineManifest>(fileSystem.read(path) { readUtf8() })
    } catch (_: Exception) {
      null
    }
  }

  private fun rewriteStyles(areaDir: Path) {
    val stylesDir = areaDir / "styles"
    if (!fileSystem.exists(stylesDir)) return
    val base = "file://$areaDir"
    fileSystem.list(stylesDir)
      .filter { it.name.endsWith(".json") }
      .forEach { stylePath ->
        val rewritten = fileSystem.read(stylePath) { readUtf8() }.replace(BASE_URL_TOKEN, base)
        fileSystem.write(stylePath) { writeUtf8(rewritten) }
      }
  }
}
