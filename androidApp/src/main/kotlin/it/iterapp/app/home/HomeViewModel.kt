package it.iterapp.app.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.iterapp.app.location.LocationProvider
import it.iterapp.core.model.GeoPoint
import it.iterapp.core.model.SearchResult
import it.iterapp.core.settings.IterSettings
import it.iterapp.core.settings.MapMode
import it.iterapp.core.settings.ThemeMode
import it.iterapp.core.wire.StyleNames
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(
  private val settings: IterSettings,
  private val locationProvider: LocationProvider,
) : ViewModel() {

  /** Set by the UI on composition (isSystemInDarkTheme + theme setting). */
  private val darkTheme = MutableStateFlow(false)

  /** Set by the UI once location permission is granted. */
  private val locationEnabled = MutableStateFlow(locationProvider.hasPermission())

  /** The place currently highlighted on the map (from search / tap). */
  val selectedPlace = MutableStateFlow<SearchResult?>(null)

  val styleUrl: StateFlow<String> =
    combine(settings.gatewayOrigin, settings.mapMode, darkTheme) { origin, mode, dark ->
      val name = when (mode) {
        MapMode.STANDARD -> if (dark) StyleNames.DARK else StyleNames.LIGHT
        MapMode.TRANSIT -> if (dark) StyleNames.TRANSIT_DARK else StyleNames.TRANSIT_LIGHT
      }
      "$origin/styles/$name.json"
    }.stateIn(
      viewModelScope,
      SharingStarted.Eagerly,
      "${settings.gatewayOrigin.value}/styles/${StyleNames.LIGHT}.json",
    )

  @OptIn(ExperimentalCoroutinesApi::class)
  val userLocation: StateFlow<GeoPoint?> = locationEnabled
    .flatMapLatest { enabled ->
      if (enabled) locationProvider.updates() else flowOf(null)
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), locationProvider.lastKnown())

  fun onDarkThemeChange(dark: Boolean) {
    val mode = settings.themeMode.value
    darkTheme.value = when (mode) {
      ThemeMode.SYSTEM -> dark
      ThemeMode.LIGHT -> false
      ThemeMode.DARK -> true
    }
  }

  fun onLocationPermissionGranted() {
    locationEnabled.value = locationProvider.hasPermission()
  }

  fun lastKnownLocation(): GeoPoint? = userLocation.value ?: locationProvider.lastKnown()

  fun select(place: SearchResult?) {
    selectedPlace.value = place
  }
}
