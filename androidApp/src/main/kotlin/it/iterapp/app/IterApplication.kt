package it.iterapp.app

import android.app.Application
import it.iterapp.app.di.appModule
import it.iterapp.core.di.coreModules
import org.maplibre.android.MapLibre
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class IterApplication : Application() {

  override fun onCreate() {
    super.onCreate()
    MapLibre.getInstance(this)
    startKoin {
      androidLogger()
      androidContext(this@IterApplication)
      modules(coreModules() + appModule)
    }
  }
}
