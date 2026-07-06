package it.iterapp.app.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.iterapp.app.location.LocationProvider
import it.iterapp.core.model.SearchResult
import it.iterapp.core.repo.SearchRepository
import java.util.Locale
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
    .debounce(300)
    .flatMapLatest { q ->
      flow {
        if (q.trim().length < 2) {
          emit(emptyList())
          return@flow
        }
        _isSearching.value = true
        _error.value = false
        try {
          emit(
            repository.search(
              query = q.trim(),
              lang = Locale.getDefault().language,
              bias = locationProvider.lastKnown(),
            ),
          )
        } catch (e: Exception) {
          _error.value = true
          emit(emptyList())
        } finally {
          _isSearching.value = false
        }
      }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  fun onQueryChange(value: String) {
    query.value = value
  }

  fun reset() {
    query.value = ""
    _error.value = false
  }
}
