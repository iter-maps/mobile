package it.iterapp.app.trains

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.iterapp.core.repo.TrainsRepository
import it.iterapp.core.wire.BoardEntry
import it.iterapp.core.wire.Station
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

enum class BoardTab { DEPARTURES, ARRIVALS }

sealed interface BoardState {
  data object Idle : BoardState
  data object Loading : BoardState
  data class Loaded(val entries: List<BoardEntry>) : BoardState
  data object Error : BoardState
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class TrainBoardViewModel(
  private val repository: TrainsRepository,
) : ViewModel() {

  val stationQuery = MutableStateFlow("")
  val selectedStation = MutableStateFlow<Station?>(null)
  val tab = MutableStateFlow(BoardTab.DEPARTURES)

  val stationResults: StateFlow<List<Station>> = stationQuery
    .debounce(300)
    .flatMapLatest { q ->
      flow {
        emit(
          try {
            repository.searchStations(q)
          } catch (_: Exception) {
            emptyList()
          },
        )
      }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  /** Board rows, re-polled at the contract's cadence while observed. */
  val board: StateFlow<BoardState> =
    combine(selectedStation, tab) { station, tab -> station to tab }
      .flatMapLatest { (station, tab) ->
        flow {
          if (station == null) {
            emit(BoardState.Idle)
            return@flow
          }
          emit(BoardState.Loading)
          while (true) {
            try {
              val entries = when (tab) {
                BoardTab.DEPARTURES -> repository.departures(station.id)
                BoardTab.ARRIVALS -> repository.arrivals(station.id)
              }
              emit(BoardState.Loaded(entries))
            } catch (_: Exception) {
              emit(BoardState.Error)
            }
            delay(TrainsRepository.BOARD_POLL_SECONDS * 1_000L)
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

  fun reset() {
    stationQuery.value = ""
    selectedStation.value = null
    tab.value = BoardTab.DEPARTURES
  }
}
