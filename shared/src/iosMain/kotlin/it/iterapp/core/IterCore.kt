package it.iterapp.core

import it.iterapp.core.di.coreModules
import it.iterapp.core.net.Connectivity
import it.iterapp.core.offline.OfflineRepository
import it.iterapp.core.repo.PlacesRepository
import it.iterapp.core.repo.PlanRepository
import it.iterapp.core.repo.ReliabilityRepository
import it.iterapp.core.repo.SearchRepository
import it.iterapp.core.repo.TrainsRepository
import it.iterapp.core.settings.IterSettings
import it.iterapp.core.settings.MapMode
import it.iterapp.core.settings.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.core.Koin
import org.koin.core.context.startKoin

/**
 * Swift-facing entry point: starts the DI graph once and hands the shell
 * plain repository objects — no Koin, no Flow types on the boundary (ADR
 * 0002). Settings are exposed as simple getters/setters; SwiftUI keeps its
 * own observable state and pushes changes down.
 */
class IterCore private constructor(private val koin: Koin) {

  val search: SearchRepository = koin.get()
  val plan: PlanRepository = koin.get()
  val trains: TrainsRepository = koin.get()
  val places: PlacesRepository = koin.get()
  val reliability: ReliabilityRepository = koin.get()
  val offline: OfflineRepository = koin.get()

  private val settings: IterSettings = koin.get()
  private val connectivity: Connectivity = koin.get()
  private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

  fun gatewayOrigin(): String = settings.gatewayOrigin.value
  fun setGatewayOrigin(origin: String) = settings.setGatewayOrigin(origin)

  fun themeMode(): String = settings.themeMode.value.name
  fun setThemeMode(name: String) {
    ThemeMode.entries.firstOrNull { it.name == name }?.let(settings::setThemeMode)
  }

  fun mapMode(): String = settings.mapMode.value.name
  fun setMapMode(name: String) {
    MapMode.entries.firstOrNull { it.name == name }?.let(settings::setMapMode)
  }

  fun hasSeenOnboarding(): Boolean = settings.hasSeenOnboarding.value
  fun setOnboardingSeen(seen: Boolean) = settings.setOnboardingSeen(seen)

  /** Point-in-time reachability, for classifying a caught error on the shell. */
  fun isOnline(): Boolean = connectivity.isOnline.value

  /**
   * Observe reachability from Swift: [onChange] fires with the current value
   * immediately and on every change (drives the offline-map fallback).
   */
  fun observeOnline(onChange: (Boolean) -> Unit) {
    scope.launch { connectivity.isOnline.collect { onChange(it) } }
  }

  /** MapLibre style URL for the current origin; `name` from StyleNames. */
  fun styleUrl(name: String): String = "${settings.gatewayOrigin.value}/styles/$name.json"

  companion object {
    private var instance: IterCore? = null

    fun start(): IterCore = instance ?: IterCore(
      startKoin { modules(coreModules()) }.koin,
    ).also { instance = it }
  }
}
