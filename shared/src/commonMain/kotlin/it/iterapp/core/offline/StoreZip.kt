package it.iterapp.core.offline

import okio.Buffer
import okio.FileHandle
import okio.FileSystem
import okio.Path
import okio.use

/**
 * Minimal ZIP extractor for the offline bundle format: the gateway ships
 * STORE (uncompressed) archives by contract, so no inflater is needed and the
 * extractor stays pure Kotlin on every target. Reads the central directory
 * (authoritative sizes even when entries used data descriptors), rejects
 * compressed entries and ZIP64, and guards against zip-slip.
 */
object StoreZip {

  private const val EOCD_SIGNATURE = 0x06054b50L
  private const val CENTRAL_SIGNATURE = 0x02014b50L
  private const val LOCAL_SIGNATURE = 0x04034b50L
  private const val EOCD_MIN_SIZE = 22L
  private const val MAX_COMMENT = 65_535L

  class ZipException(message: String) : Exception(message)

  /** Extracts [zipPath] into [targetDir], returning the extracted relative paths. */
  fun extract(fileSystem: FileSystem, zipPath: Path, targetDir: Path): List<String> {
    fileSystem.createDirectories(targetDir)
    val extracted = mutableListOf<String>()
    fileSystem.openReadOnly(zipPath).use { handle ->
      val size = handle.size()
      val eocdOffset = findEocd(handle, size)
      val eocd = handle.readBuffer(eocdOffset, EOCD_MIN_SIZE)
      eocd.skip(4) // signature
      eocd.skip(6) // disk numbers, cd start disk
      val entryCount = eocd.readShortLe().toInt() and 0xffff
      eocd.skip(4) // central directory size
      val cdOffset = eocd.readIntLe().toLong() and 0xffffffffL
      if (cdOffset == 0xffffffffL) throw ZipException("ZIP64 archives are not supported")

      var cursor = cdOffset
      repeat(entryCount) {
        val head = handle.readBuffer(cursor, 46)
        if ((head.readIntLe().toLong() and 0xffffffffL) != CENTRAL_SIGNATURE) {
          throw ZipException("bad central directory entry")
        }
        head.skip(6) // versions, flags
        val method = head.readShortLe().toInt() and 0xffff
        head.skip(8) // time, date, crc
        val compressedSize = head.readIntLe().toLong() and 0xffffffffL
        val uncompressedSize = head.readIntLe().toLong() and 0xffffffffL
        val nameLen = (head.readShortLe().toInt() and 0xffff).toLong()
        val extraLen = (head.readShortLe().toInt() and 0xffff).toLong()
        val commentLen = (head.readShortLe().toInt() and 0xffff).toLong()
        head.skip(8) // disk, attrs
        val localOffset = head.readIntLe().toLong() and 0xffffffffL
        val name = handle.readBuffer(cursor + 46, nameLen).readUtf8()
        cursor += 46 + nameLen + extraLen + commentLen

        if (name.endsWith("/")) return@repeat
        if (method != 0) throw ZipException("entry '$name' is compressed; bundles are STORE-only")
        if (compressedSize != uncompressedSize) throw ZipException("size mismatch for '$name'")

        val safeTarget = resolveSafe(targetDir, name)
          ?: throw ZipException("entry '$name' escapes the target directory")

        // Local header: 30 fixed bytes + its own name/extra lengths.
        val local = handle.readBuffer(localOffset, 30)
        if ((local.readIntLe().toLong() and 0xffffffffL) != LOCAL_SIGNATURE) {
          throw ZipException("bad local header for '$name'")
        }
        local.skip(22)
        val localNameLen = (local.readShortLe().toInt() and 0xffff).toLong()
        val localExtraLen = (local.readShortLe().toInt() and 0xffff).toLong()
        val dataOffset = localOffset + 30 + localNameLen + localExtraLen

        safeTarget.parent?.let { fileSystem.createDirectories(it) }
        fileSystem.write(safeTarget) {
          var remaining = compressedSize
          var position = dataOffset
          val chunk = Buffer()
          while (remaining > 0) {
            val step = minOf(remaining, 256L * 1024)
            val read = handle.read(position, chunk, step)
            if (read <= 0) throw ZipException("truncated data for '$name'")
            write(chunk, read)
            position += read
            remaining -= read
          }
        }
        extracted.add(name)
      }
    }
    return extracted
  }

  private fun findEocd(handle: FileHandle, size: Long): Long {
    if (size < EOCD_MIN_SIZE) throw ZipException("not a ZIP archive")
    val scanLen = minOf(size, EOCD_MIN_SIZE + MAX_COMMENT)
    val start = size - scanLen
    val buffer = handle.readBuffer(start, scanLen)
    val bytes = buffer.readByteArray()
    for (i in bytes.size - EOCD_MIN_SIZE.toInt() downTo 0) {
      if (bytes[i] == 0x50.toByte() && bytes[i + 1] == 0x4b.toByte() &&
        bytes[i + 2] == 0x05.toByte() && bytes[i + 3] == 0x06.toByte()
      ) {
        return start + i
      }
    }
    throw ZipException("end of central directory not found")
  }

  private fun resolveSafe(targetDir: Path, entryName: String): Path? {
    if (entryName.startsWith("/") || entryName.contains("\\")) return null
    val segments = entryName.split('/')
    if (segments.any { it == ".." || it.isEmpty() }) return null
    var path = targetDir
    segments.forEach { path = path / it }
    return path
  }

  private fun FileHandle.readBuffer(offset: Long, length: Long): Buffer {
    val buffer = Buffer()
    var position = offset
    var remaining = length
    while (remaining > 0) {
      val read = read(position, buffer, remaining)
      if (read <= 0) throw ZipException("unexpected end of archive")
      position += read
      remaining -= read
    }
    return buffer
  }
}
