package it.iterapp.app.trains

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.iterapp.core.api.AppFailure
import it.iterapp.core.api.toAppFailure
import it.iterapp.core.net.Connectivity
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

enum class BoardTab { DEPARTURES, ARRIVALS }

sealed interface BoardState {
  data object Idle : BoardState
  data object Loading : BoardState

  /**
   * [stale] marks a board kept from the last good poll after a failure;
   * [forTab] records which tab the rows belong to, so outgoing content
   * keeps its own labeling during the tab-switch transition.
   */
  data class Loaded(
    val entries: List<BoardEntry>,
    val stale: Boolean = false,
    val forTab: BoardTab = BoardTab.DEPARTURES,
  ) : BoardState

  /** Nothing to show at all; [failure] carries the typed cause when known. */
  data class Error(val failure: AppFailure? = null) : BoardState
}

/**
 * Board poll loop: a transient failure keeps the last good board on screen
 * (marked stale) instead of wiping the list the user is reading on the
 * platform; Error is reserved for having nothing to show at all. A seeded
 * [initial] board (manual refresh of the same station/tab) skips the
 * Loading emission so the visible list refreshes in place.
 */
internal fun boardPollFlow(
  pollMs: Long,
  initial: List<BoardEntry>? = null,
  classify: (Throwable) -> AppFailure = { it.toAppFailure() },
  fetch: suspend () -> List<BoardEntry>,
): Flow<BoardState> = flow {
  var lastGood: List<BoardEntry>? = initial
  if (initial == null) emit(BoardState.Loading)
  while (true) {
    // Only the fetch is guarded: emissions stay outside the try so flow
    // cancellation (take, flatMapLatest switch) propagates untouched.
    val (entries, error) = try {
      fetch() to null
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      null to e
    }
    if (entries != null) {
      lastGood = entries
      emit(BoardState.Loaded(entries))
    } else {
      val kept = lastGood
      emit(
        if (kept == null) BoardState.Error(error?.let(classify)) else BoardState.Loaded(kept, stale = true),
      )
    }
    delay(pollMs)
  }
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class TrainBoardViewModel(
  private val repository: TrainsRepository,
  private val connectivity: Connectivity,
) : ViewModel() {

  val stationQuery = MutableStateFlow("")
  val selectedStation = MutableStateFlow<Station?>(null)
  val tab = MutableStateFlow(BoardTab.DEPARTURES)

  private val _isSearching = MutableStateFlow(false)
  val isSearching: StateFlow<Boolean> = _isSearching

  private val _searchFailure = MutableStateFlow<AppFailure?>(null)
  val searchFailure: StateFlow<AppFailure?> = _searchFailure

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
            _isSearching.value = false
            _searchFailure.value = null
            emit(emptyList())
            return@flow
          }
          _isSearching.value = true
          _searchFailure.value = null
          // Only the fetch is guarded: emitting from a catch block violates
          // flow exception transparency and can swallow cancellation.
          val found = try {
            repository.searchStations(q)
          } catch (e: CancellationException) {
            _isSearching.value = false
            throw e
          } catch (e: Exception) {
            _searchFailure.value = e.toAppFailure(connectivity.isOnline.value)
            null
          }
          _isSearching.value = false
          emit(found ?: emptyList())
        }
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  /** The last (station, tab) the poll ran for, guarding refresh seeding. */
  private var lastBoardKey: Pair<String, BoardTab>? = null

  /** Board rows, re-polled at the contract's cadence while observed. */
  val board: StateFlow<BoardState> =
    combine(selectedStation, tab, refreshTick) { station, tab, _ -> station to tab }
      .flatMapLatest { (station, tab) ->
        if (station == null) {
          lastBoardKey = null
          flowOf(BoardState.Idle)
        } else {
          // A refresh of the same station/tab seeds the poll with the
          // visible board so it updates in place instead of blanking.
          val key = station.id to tab
          val seed = if (key == lastBoardKey) (board.value as? BoardState.Loaded)?.entries else null
          lastBoardKey = key
          boardPollFlow(
            pollMs = TrainsRepository.BOARD_POLL_SECONDS * 1_000L,
            initial = seed,
            classify = { it.toAppFailure(connectivity.isOnline.value) },
          ) {
            when (tab) {
              BoardTab.DEPARTURES -> repository.departures(station.id)
              BoardTab.ARRIVALS -> repository.arrivals(station.id)
            }
          }.map { s -> if (s is BoardState.Loaded) s.copy(forTab = tab) else s }
        }
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BoardState.Idle)

  fun onQueryChange(value: String) {
    // The spinner must cover the debounce window too, or the empty state
    // flashes before the fetch even starts.
    if (value.trim().length >= 2) _isSearching.value = true
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
    _isSearching.value = false
    _searchFailure.value = null
  }
}
