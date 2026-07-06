package it.iterapp.core.di

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import kotlinx.cinterop.ExperimentalForeignApi
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUserDomainMask

actual fun platformCoreModule(): Module = module {
  single<Settings> { NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults) }
  single<FileSystem> { FileSystem.SYSTEM }
  single<Path>(OfflineRootQualifier) { documentsPath() / "offline" }
}

@OptIn(ExperimentalForeignApi::class)
private fun documentsPath(): Path {
  val url = NSFileManager.defaultManager.URLForDirectory(
    directory = NSDocumentDirectory,
    inDomain = NSUserDomainMask,
    appropriateForURL = null,
    create = true,
    error = null,
  )
  return (url?.path ?: NSFileManager.defaultManager.currentDirectoryPath).toPath()
}
