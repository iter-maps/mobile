package it.iterapp.app.offline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.iterapp.core.api.IterApiException
import it.iterapp.core.geo.BBoxFormat
import it.iterapp.core.offline.OfflineArea
import it.iterapp.core.offline.OfflineRepository
import it.iterapp.core.wire.ApiErrorCodes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface DownloadState {
  data object Idle : DownloadState
  data class Downloading(val bytesRead: Long, val total: Long?) : DownloadState
  data object Installing : DownloadState
  data class Failed(val code: String?) : DownloadState
}

class OfflineViewModel(
  private val repository: OfflineRepository,
) : ViewModel() {

  private val _areas = MutableStateFlow<List<OfflineArea>>(emptyList())
  val areas: StateFlow<List<OfflineArea>> = _areas

  private val _download = MutableStateFlow<DownloadState>(DownloadState.Idle)
  val download: StateFlow<DownloadState> = _download

  init {
    refresh()
  }

  fun refresh() {
    viewModelScope.launch(Dispatchers.IO) {
      _areas.value = repository.list()
    }
  }

  /** Downloads the given viewport bbox as a new offline area. */
  fun downloadArea(bbox: String) {
    if (_download.value is DownloadState.Downloading) return
    // Pre-flight the server's area cap so an over-large viewport fails fast
    // with clear copy instead of a round-trip 413.
    if (exceedsAreaCap(bbox)) {
      _download.value = DownloadState.Failed(ApiErrorCodes.AREA_TOO_LARGE)
      return
    }
    viewModelScope.launch {
      _download.value = DownloadState.Downloading(0, null)
      try {
        withContext(Dispatchers.IO) {
          val id = "area-${System.currentTimeMillis()}"
          repository.install(id, bbox, maxzoom = 14) { read, total ->
            // Once bytes are all in, the remaining work is the on-device unpack.
            _download.value = if (total != null && read >= total) {
              DownloadState.Installing
            } else {
              DownloadState.Downloading(read, total)
            }
          }
        }
        _download.value = DownloadState.Idle
        refresh()
      } catch (e: IterApiException) {
        _download.value = DownloadState.Failed(e.code)
      } catch (e: Exception) {
        _download.value = DownloadState.Failed(null)
      }
    }
  }

  private fun exceedsAreaCap(bbox: String): Boolean {
    val parts = bbox.split(",").mapNotNull { it.trim().toDoubleOrNull() }
    if (parts.size != 4) return false
    return BBoxFormat.areaDeg2(parts[0], parts[1], parts[2], parts[3]) >
      BBoxFormat.OFFLINE_AREA_CAP_DEG2
  }

  fun delete(areaId: String) {
    viewModelScope.launch(Dispatchers.IO) {
      repository.delete(areaId)
      _areas.value = repository.list()
    }
  }

  fun dismissError() {
    if (_download.value is DownloadState.Failed) _download.value = DownloadState.Idle
  }

  companion object {
    fun isTooLarge(code: String?): Boolean = code == ApiErrorCodes.AREA_TOO_LARGE
    fun isInvalidArea(code: String?): Boolean =
      code == ApiErrorCodes.BBOX_INVALID || code == ApiErrorCodes.BBOX_OUT_OF_RANGE ||
        code == ApiErrorCodes.BBOX_DEGENERATE || code == ApiErrorCodes.BBOX_REQUIRED
    fun isBusy(code: String?): Boolean = code == ApiErrorCodes.BUSY
  }
}
