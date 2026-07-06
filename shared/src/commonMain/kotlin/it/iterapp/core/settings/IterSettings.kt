package it.iterapp.core.settings

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Basemap flavor; light/dark is resolved from the theme. */
enum class MapMode { STANDARD, TRANSIT }

/** The platform's sensible default gateway origin for debug/dev use. */
expect fun defaultGatewayOrigin(): String

/**
 * The handful of client preferences, persisted via Multiplatform-Settings and
 * exposed as StateFlows for reactive UIs. Single-process source of truth.
 */
class IterSettings(
  private val store: Settings,
) {

  private val _gatewayOrigin = MutableStateFlow(store.getString(KEY_ORIGIN, defaultGatewayOrigin()))
  val gatewayOrigin: StateFlow<String> = _gatewayOrigin

  private val _themeMode = MutableStateFlow(
    runCatching { ThemeMode.valueOf(store.getString(KEY_THEME, ThemeMode.SYSTEM.name)) }
      .getOrDefault(ThemeMode.SYSTEM),
  )
  val themeMode: StateFlow<ThemeMode> = _themeMode

  // Brand palette by default; wallpaper tinting is opt-in (ADR 0013).
  private val _dynamicColor = MutableStateFlow(store.getBoolean(KEY_DYNAMIC, false))
  val dynamicColor: StateFlow<Boolean> = _dynamicColor

  private val _mapMode = MutableStateFlow(
    runCatching { MapMode.valueOf(store.getString(KEY_MAP_MODE, MapMode.STANDARD.name)) }
      .getOrDefault(MapMode.STANDARD),
  )
  val mapMode: StateFlow<MapMode> = _mapMode

  fun setGatewayOrigin(origin: String) {
    val cleaned = origin.trim().trimEnd('/')
    store.putString(KEY_ORIGIN, cleaned)
    _gatewayOrigin.value = cleaned
  }

  fun setThemeMode(mode: ThemeMode) {
    store.putString(KEY_THEME, mode.name)
    _themeMode.value = mode
  }

  fun setDynamicColor(enabled: Boolean) {
    store.putBoolean(KEY_DYNAMIC, enabled)
    _dynamicColor.value = enabled
  }

  fun setMapMode(mode: MapMode) {
    store.putString(KEY_MAP_MODE, mode.name)
    _mapMode.value = mode
  }

  private companion object {
    const val KEY_ORIGIN = "gateway_origin"
    const val KEY_THEME = "theme_mode"
    const val KEY_DYNAMIC = "dynamic_color"
    const val KEY_MAP_MODE = "map_mode"
  }
}
