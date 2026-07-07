package it.iterapp.app.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.iterapp.app.location.LocationProvider
import it.iterapp.core.geo.haversineMeters
import it.iterapp.core.model.GeoPoint
import it.iterapp.core.model.SearchResult
import it.iterapp.core.repo.SearchRepository
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

/**
 * Debounced Photon autocomplete, biased to the user's position. Shared by the
 * Search page and the planning endpoint picker — each resets state on entry
 * (pages share one instance by design, ADR 0008).
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class SearchViewModel(
  private val repository: SearchRepository,
  private val locationProvider: LocationProvider,
) : ViewModel() {

  val query = MutableStateFlow("")
  private val _isSearching = MutableStateFlow(false)
  val isSearching: StateFlow<Boolean> = _isSearching
  private val _error = MutableStateFlow(false)
  val error: StateFlow<Boolean> = _error

  val results: StateFlow<List<SearchResult>> = query
    // Real queries debounce; clears and resets propagate immediately so no
    // stale result list lingers under an emptied field.
    .debounce { q -> if (q.trim().length < 2) 0L else 300L }
    .flatMapLatest { q ->
      flow {
        if (q.trim().length < 2) {
          _isSearching.value = false
          _error.value = false
          emit(emptyList())
          return@flow
        }
        _isSearching.value = true
        _error.value = false
        val bias = locationProvider.lastKnown()
        // Only the fetch is guarded — emitting from a catch block violates
        // flow exception transparency and can swallow cancellation.
        val found = try {
          repository.search(
            query = q.trim(),
            lang = Locale.getDefault().language,
            bias = bias,
          )
        } catch (e: CancellationException) {
          _isSearching.value = false
          throw e
        } catch (_: Exception) {
          _error.value = true
          null
        }
        _isSearching.value = false
        emit(found?.withDistancesFrom(bias) ?: emptyList())
      }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  fun onQueryChange(value: String) {
    // The spinner must cover the debounce window too, or the empty state
    // flashes before the fetch even starts.
    if (value.trim().length >= 2) _isSearching.value = true
    query.value = value
  }

  fun reset() {
    query.value = ""
    _isSearching.value = false
    _error.value = false
  }
}

/**
 * Fills [SearchResult.distanceM] from [origin] where the wire left it null
 * (Photon only returns it on reverse lookups); wire-provided values win.
 */
internal fun List<SearchResult>.withDistancesFrom(origin: GeoPoint?): List<SearchResult> =
  if (origin == null) {
    this
  } else {
    map { r -> if (r.distanceM != null) r else r.copy(distanceM = haversineMeters(origin, r.point)) }
  }
