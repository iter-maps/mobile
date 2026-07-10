package it.iterapp.core.net

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.darwin.dispatch_queue_create

/** [Connectivity] over `NWPathMonitor` from the system Network framework. */
@OptIn(ExperimentalForeignApi::class)
class IosConnectivity : Connectivity {

  private val _isOnline = MutableStateFlow(true)
  override val isOnline: StateFlow<Boolean> = _isOnline

  private val monitor = nw_path_monitor_create()

  init {
    nw_path_monitor_set_update_handler(monitor) { path ->
      _isOnline.value = nw_path_get_status(path) == nw_path_status_satisfied
    }
    nw_path_monitor_set_queue(monitor, dispatch_queue_create("it.iterapp.connectivity", null))
    nw_path_monitor_start(monitor)
  }
}
