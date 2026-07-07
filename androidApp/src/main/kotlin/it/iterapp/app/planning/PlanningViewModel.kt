package it.iterapp.app.planning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.iterapp.app.common.planDateTime
import it.iterapp.app.location.LocationProvider
import it.iterapp.core.api.IterApiException
import it.iterapp.core.api.IterTransportException
import it.iterapp.core.api.PlanParams
import it.iterapp.core.model.GeoPoint
import it.iterapp.core.model.Itinerary
import it.iterapp.core.model.SearchResult
import it.iterapp.core.repo.PlanRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** One endpoint of a plan: a named point, possibly "my location". */
data class PlanEndpoint(
  val name: String,
  val point: GeoPoint,
  val isUserLocation: Boolean = false,
)

/** Ranking profile choices surfaced in the UI; null = OTP's own order. */
enum class PlanProfile(val wireValue: String?) {
  FASTEST(null),
  RELIABLE("reliability"),
  BALANCED("balanced"),
  ECO("eco"),
  COMFORT("comfort"),
}

sealed interface PlanState {
  data object Idle : PlanState

  /** [previous] keeps the outgoing results visible (dimmed) during a replan. */
  data class Loading(val previous: List<Itinerary>? = null) : PlanState
  data class Results(val itineraries: List<Itinerary>) : PlanState
  data class Error(val network: Boolean) : PlanState
}

/**
 * What a keepCurrent replan keeps on screen: live results, or the list an
 * in-flight keepCurrent replan is already keeping — so back-to-back profile
 * taps never collapse the dimmed list into first-load skeletons.
 */
internal fun keptItineraries(state: PlanState): List<Itinerary>? = when (state) {
  is PlanState.Results -> state.itineraries
  is PlanState.Loading -> state.previous
  else -> null
}

class PlanningViewModel(
  private val repository: PlanRepository,
  private val locationProvider: LocationProvider,
) : ViewModel() {

  val from = MutableStateFlow<PlanEndpoint?>(null)
  val to = MutableStateFlow<PlanEndpoint?>(null)
  val profile = MutableStateFlow(PlanProfile.FASTEST)
  /** Epoch millis of the requested departure, null = now. */
  val departureMs = MutableStateFlow<Long?>(null)
  val arriveBy = MutableStateFlow(false)

  private val _state = MutableStateFlow<PlanState>(PlanState.Idle)
  val state: StateFlow<PlanState> = _state

  val selected = MutableStateFlow<Itinerary?>(null)

  private var planJob: Job? = null

  /**
   * Entry point from a place page: destination set, origin = user location.
   * The origin is reset to the user's location unless they had explicitly
   * picked one this session, so tapping Directions on a new place never plans
   * from a previous trip's stale origin.
   */
  fun directionsTo(place: SearchResult) {
    to.value = PlanEndpoint(place.name, place.point)
    if (from.value?.isUserLocation != false) {
      from.value = locationProvider.lastKnown()
        ?.let { PlanEndpoint("", it, isUserLocation = true) }
    }
    replan()
  }

  fun setEndpoint(fromField: Boolean, endpoint: PlanEndpoint) {
    if (fromField) from.value = endpoint else to.value = endpoint
    replan()
  }

  fun useMyLocation(fromField: Boolean) {
    val point = locationProvider.lastKnown() ?: return
    setEndpoint(fromField, PlanEndpoint("", point, isUserLocation = true))
  }

  fun swap() {
    val f = from.value
    from.value = to.value
    to.value = f
    replan()
  }

  fun setProfile(value: PlanProfile) {
    if (profile.value == value) return
    profile.value = value
    // Same trip, new ranking: keep the current list on screen while it loads.
    replan(keepCurrent = true)
  }

  fun setDeparture(epochMs: Long?, arrive: Boolean) {
    departureMs.value = epochMs
    arriveBy.value = arrive
    replan(keepCurrent = true)
  }

  fun select(itinerary: Itinerary?) {
    selected.value = itinerary
  }

  fun replan(keepCurrent: Boolean = false) {
    val origin = from.value ?: return
    val destination = to.value ?: return
    val previous = if (keepCurrent) keptItineraries(_state.value) else null
    planJob?.cancel()
    planJob = viewModelScope.launch {
      _state.value = PlanState.Loading(previous)
      if (!keepCurrent) selected.value = null
      try {
        val (date, time) = departureMs.value?.let { planDateTime(it) } ?: (null to null)
        val itineraries = repository.plan(
          PlanParams(
            fromLat = origin.point.lat,
            fromLon = origin.point.lon,
            toLat = destination.point.lat,
            toLon = destination.point.lon,
            date = date,
            time = time,
            arriveBy = arriveBy.value,
          ),
          rerank = profile.value.wireValue,
          predictHistorical = true,
        )
        _state.value = PlanState.Results(itineraries)
        selected.value = itineraries.firstOrNull()
      } catch (e: IterTransportException) {
        _state.value = PlanState.Error(network = true)
      } catch (e: IterApiException) {
        _state.value = PlanState.Error(network = false)
      }
    }
  }

  fun reset() {
    planJob?.cancel()
    from.value = null
    to.value = null
    selected.value = null
    departureMs.value = null
    arriveBy.value = false
    profile.value = PlanProfile.FASTEST
    _state.value = PlanState.Idle
  }
}
