package it.iterapp.core

import it.iterapp.core.di.coreModules
import org.koin.core.context.startKoin

/**
 * Framework entry point for the iOS shell: starts the Koin graph. Android
 * starts Koin in its Application class instead (it needs a Context).
 */
fun initIterCore() {
  startKoin {
    modules(coreModules())
  }
}
