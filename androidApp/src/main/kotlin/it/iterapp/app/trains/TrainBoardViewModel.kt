package it.iterapp.app.trains

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.iterapp.core.repo.TrainsRepository
import it.iterapp.core.wire.BoardEntry
import it.iterapp.core.wire.Station
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

enum class BoardTab { DEPARTURES, ARRIVALS }

sealed interface BoardState {
  data object Idle : BoardState
  data object Loading : BoardState

  /** [stale] marks a board kept from the last good poll after a failure. */
  data class Loaded(val entries: List<BoardEntry>, val stale: Boolean = false) : BoardState
  data object Error : BoardState
}

/**
 * Board poll loop: a transient failure keeps the last good board on screen
 * (marked stale) instead of wiping the list the user is reading on the
 * platform; Error is reserved for having nothing to show at all.
 */
internal fun boardPollFlow(
  pollMs: Long,
  fetch: suspend () -> List<BoardEntry>,
): Flow<BoardState> = flow {
  emit(BoardState.Loading)
  var lastGood: List<BoardEntry>? = null
  while (true) {
    // Only the fetch is guarded: emissions stay outside the try so flow
    // cancellation (take, flatMapLatest switch) propagates untouched.
    val entries = try {
      fetch()
    } catch (e: CancellationException) {
      throw e
    } catch (_: Exception) {
      null
    }
    if (entries != null) {
      lastGood = entries
      emit(BoardState.Loaded(entries))
    } else {
      val kept = lastGood
      emit(if (kept == null) BoardState.Error else BoardState.Loaded(kept, stale = true))
    }
    delay(pollMs)
  }
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class TrainBoardViewModel(
  private val repository: TrainsRepository,
) : ViewModel() {

  val stationQuery = MutableStateFlow("")
  val selectedStation = MutableStateFlow<Station?>(null)
  val tab = MutableStateFlow(BoardTab.DEPARTURES)

  private val _isSearching = MutableStateFlow(false)
  val isSearching: StateFlow<Boolean> = _isSearching

  private val _searchError = MutableStateFlow(false)
  val searchError: StateFlow<Boolean> = _searchError

  private val searchRetryTick = MutableStateFlow(0)
  private val refreshTick = MutableStateFlow(0)

  val stationResults: StateFlow<List<Station>> =
    combine(stationQuery, searchRetryTick) { q, _ -> q }
      // Real queries debounce; clears and resets propagate immediately so no
      // stale list lingers under an emptied field.
      .debounce { q -> if (q.trim().length < 2) 0L else 300L }
      .flatMapLatest { q ->
        flow {
          if (q.trim().length < 2) {
            _searchError.value = false
            emit(emptyList())
            return@flow
          }
          _isSearching.value = true
          _searchError.value = false
          try {
            emit(repository.searchStations(q))
          } catch (_: Exception) {
            _searchError.value = true
            emit(emptyList())
          } finally {
            _isSearching.value = false
          }
        }
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  /** Board rows, re-polled at the contract's cadence while observed. */
  val board: StateFlow<BoardState> =
    combine(selectedStation, tab, refreshTick) { station, tab, _ -> station to tab }
      .flatMapLatest { (station, tab) ->
        if (station == null) {
          flowOf(BoardState.Idle)
        } else {
          boardPollFlow(TrainsRepository.BOARD_POLL_SECONDS * 1_000L) {
            when (tab) {
              BoardTab.DEPARTURES -> repository.departures(station.id)
              BoardTab.ARRIVALS -> repository.arrivals(station.id)
            }
          }
        }
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BoardState.Idle)

  fun onQueryChange(value: String) {
    stationQuery.value = value
  }

  fun selectStation(station: Station?) {
    selectedStation.value = station
  }

  fun setTab(value: BoardTab) {
    tab.value = value
  }

  /** Restarts the board poll now — also the retry path after an error. */
  fun refresh() {
    refreshTick.value++
  }

  fun retryStationSearch() {
    searchRetryTick.value++
  }

  fun reset() {
    stationQuery.value = ""
    selectedStation.value = null
    tab.value = BoardTab.DEPARTURES
    _searchError.value = false
  }
}
