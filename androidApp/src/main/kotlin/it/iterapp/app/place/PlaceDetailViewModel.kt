package it.iterapp.app.place

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.iterapp.core.model.SearchResult
import it.iterapp.core.repo.PlacesRepository
import it.iterapp.core.wire.Place
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Optional enrichment for a selected place. Search results carry no Wikidata
 * seed, so this tries a Wikipedia title lookup for POI-ish results and stays
 * quiet on misses — the basic card never depends on it.
 */
class PlaceDetailViewModel(
  private val repository: PlacesRepository,
) : ViewModel() {

  private val _enriched = MutableStateFlow<Place?>(null)
  val enriched: StateFlow<Place?> = _enriched

  private var job: Job? = null

  fun load(place: SearchResult) {
    job?.cancel()
    _enriched.value = null
    if (!place.isPoiLike) return
    job = viewModelScope.launch {
      _enriched.value = try {
        repository.enrichByTitle(place.name, Locale.getDefault().language)
      } catch (_: Exception) {
        null
      }
    }
  }

  fun imageUrl(place: Place): String? = repository.imageUrl(place)

  fun clear() {
    job?.cancel()
    _enriched.value = null
  }
}

private val SearchResult.isPoiLike: Boolean
  get() = osmKey in setOf("tourism", "historic", "amenity", "leisure", "man_made", "building")
