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

/**
 * Home "Nearby" section state — the empty list alone can't distinguish
 * "no permission" from "still locating" from "fetch failed", and each needs
 * different copy.
 */
sealed interface NearbyUiState {
  data object NoPermission : NearbyUiState
  data object Locating : NearbyUiState
  data class Loaded(val stations: List<NearbyStation>) : NearbyUiState
  data object Unavailable : NearbyUiState
}

/** Beyond this a station isn't "nearby" — better an honest empty section. */
private const val NEARBY_MAX_METERS = 30_000.0

class HomeViewModel(
  private val settings: IterSettings,
  private val locationProvider: LocationProvider,
  private val searchRepository: SearchRepository,
  private val trainsRepository: TrainsRepository,
) : ViewModel() {

  /** System dark flag from the UI; the theme setting is combined in [styleUrl]. */
  private val systemDark = MutableStateFlow(false)

  /** Set by the UI once location permission is granted. */
  private val locationEnabled = MutableStateFlow(locationProvider.hasPermission())

  /** The place currently highlighted on the map (from search / tap). */
  val selectedPlace = MutableStateFlow<SearchResult?>(null)

  /** One-shot guard for the launch camera move; survives recomposition, not process death. */
  var initialCameraDone = false

  /** Places opened this session, most recent first (persistence is roadmapped). */
  private val _recentPlaces = MutableStateFlow<List<SearchResult>>(emptyList())
  val recentPlaces: StateFlow<List<SearchResult>> = _recentPlaces

  /** The closest rail stations, once a position is known — home's Nearby section. */
  private val _nearbyState = MutableStateFlow<NearbyUiState>(
    if (locationProvider.hasPermission()) NearbyUiState.Locating else NearbyUiState.NoPermission,
  )
  val nearbyState: StateFlow<NearbyUiState> = _nearbyState

  /** Station list cache: fetched once, distances re-ranked per fix. */
  private var allStations: List<Station>? = null

  val styleUrl: StateFlow<String> =
    combine(
      settings.gatewayOrigin, settings.mapMode, settings.themeMode, systemDark,
    ) { origin, mode, theme, sysDark ->
      val dark = when (theme) {
        ThemeMode.SYSTEM -> sysDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
      }
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
      // Stations are fetched once (retried on later fixes until the fetch
      // lands); distances re-rank on every fix so they never freeze at the
      // session's first position while the user rides away from it.
      userLocation.filterNotNull().collect { here ->
        val stations = allStations
          ?: try {
            trainsRepository.stations().also { allStations = it }
          } catch (_: Exception) {
            if (_nearbyState.value !is NearbyUiState.Loaded) {
              _nearbyState.value = NearbyUiState.Unavailable
            }
            null
          }
          ?: return@collect
        _nearbyState.value = NearbyUiState.Loaded(
          stations
            .mapNotNull { station ->
              val lat = station.lat ?: return@mapNotNull null
              val lon = station.lon ?: return@mapNotNull null
              NearbyStation(station, haversineMeters(here, GeoPoint(lat, lon)))
            }
            .filter { it.distanceMeters <= NEARBY_MAX_METERS }
            .sortedBy { it.distanceMeters }
            .take(3),
        )
      }
    }
  }

  fun onDarkThemeChange(dark: Boolean) {
    systemDark.value = dark
  }

  fun onLocationPermissionGranted() {
    locationEnabled.value = locationProvider.hasPermission()
    if (locationEnabled.value && _nearbyState.value is NearbyUiState.NoPermission) {
      _nearbyState.value = NearbyUiState.Locating
    }
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
