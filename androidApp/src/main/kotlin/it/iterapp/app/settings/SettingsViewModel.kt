package it.iterapp.app.settings

import androidx.lifecycle.ViewModel
import it.iterapp.core.settings.IterSettings
import it.iterapp.core.settings.MapMode
import it.iterapp.core.settings.ThemeMode
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(
  private val settings: IterSettings,
) : ViewModel() {

  val themeMode: StateFlow<ThemeMode> = settings.themeMode
  val dynamicColor: StateFlow<Boolean> = settings.dynamicColor
  val mapMode: StateFlow<MapMode> = settings.mapMode
  val gatewayOrigin: StateFlow<String> = settings.gatewayOrigin

  fun setThemeMode(mode: ThemeMode) = settings.setThemeMode(mode)
  fun setDynamicColor(enabled: Boolean) = settings.setDynamicColor(enabled)
  fun setMapMode(mode: MapMode) = settings.setMapMode(mode)
  fun setGatewayOrigin(origin: String) = settings.setGatewayOrigin(origin)

  /** Clears the first-run flag so the intro shows again (minimal onboarding). */
  fun replayOnboarding() = settings.setOnboardingSeen(false)
}
