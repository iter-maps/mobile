package it.iterapp.app.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.iterapp.app.location.LocationProvider
import it.iterapp.core.geo.haversineMeters
import it.iterapp.core.model.GeoPoint
import it.iterapp.core.model.SearchResult
import it.iterapp.core.repo.SearchRepository
import it.iterapp.core.repo.TrainsRepository
import it.iterapp.core.settings.IterSettings
import it.iterapp.core.settings.MapMode
import it.iterapp.core.settings.ThemeMode
import it.iterapp.core.wire.Station
import it.iterapp.core.wire.StyleNames
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A station near the user, for the home sheet's Nearby section. */
data class NearbyStation(
  val station: Station,
  val distanceMeters: Double,
)

class HomeViewModel(
  private val settings: IterSettings,
  private val locationProvider: LocationProvider,
  private val searchRepository: SearchRepository,
  private val trainsRepository: TrainsRepository,
) : ViewModel() {

  /** Set by the UI on composition (isSystemInDarkTheme + theme setting). */
  private val darkTheme = MutableStateFlow(false)

  /** Set by the UI once location permission is granted. */
  private val locationEnabled = MutableStateFlow(locationProvider.hasPermission())

  /** The place currently highlighted on the map (from search / tap). */
  val selectedPlace = MutableStateFlow<SearchResult?>(null)

  /** Places opened this session, most recent first (persistence is roadmapped). */
  private val _recentPlaces = MutableStateFlow<List<SearchResult>>(emptyList())
  val recentPlaces: StateFlow<List<SearchResult>> = _recentPlaces

  /** The closest rail stations, once a position is known — home's Nearby section. */
  private val _nearbyStations = MutableStateFlow<List<NearbyStation>>(emptyList())
  val nearbyStations: StateFlow<List<NearbyStation>> = _nearbyStations

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

  // Declared after userLocation: this launches immediately on construction and
  // touches the flow, so property initialization order is load-bearing.
  init {
    viewModelScope.launch {
      // The station list is server-cached; retry on later fixes until it lands.
      userLocation.filterNotNull().collect { here ->
        if (_nearbyStations.value.isNotEmpty()) return@collect
        _nearbyStations.value = try {
          trainsRepository.stations()
            .mapNotNull { station ->
              val lat = station.lat ?: return@mapNotNull null
              val lon = station.lon ?: return@mapNotNull null
              NearbyStation(station, haversineMeters(here, GeoPoint(lat, lon)))
            }
            .sortedBy { it.distanceMeters }
            .take(3)
        } catch (_: Exception) {
          emptyList()
        }
      }
    }
  }

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
    if (place != null) {
      _recentPlaces.value =
        (listOf(place) + _recentPlaces.value.filterNot { it.id == place.id }).take(8)
    }
  }

  private var identifyJob: Job? = null

  /**
   * Apple-Maps-style tap-to-identify: reverse-geocode the tapped point and
   * surface it as the selected place. Silent on failure — a map tap must
   * never error at the user.
   */
  fun identify(point: GeoPoint, onResult: (SearchResult) -> Unit) {
    identifyJob?.cancel()
    identifyJob = viewModelScope.launch {
      val result = try {
        searchRepository.reverse(point, Locale.getDefault().language)
      } catch (_: Exception) {
        null
      }
      if (result != null) {
        selectedPlace.value = result
        onResult(result)
      }
    }
  }
}
