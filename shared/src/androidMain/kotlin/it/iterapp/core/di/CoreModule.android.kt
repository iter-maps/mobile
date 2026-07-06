package it.iterapp.core.di

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformCoreModule(): Module = module {
  single<Settings> {
    SharedPreferencesSettings(
      androidContext().getSharedPreferences("iter_prefs", Context.MODE_PRIVATE),
    )
  }
  single<FileSystem> { FileSystem.SYSTEM }
  single<Path>(OfflineRootQualifier) {
    androidContext().filesDir.absolutePath.toPath() / "offline"
  }
}
