package it.iterapp.core.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * [Connectivity] over [ConnectivityManager]'s default-network callback.
 * Requires `ACCESS_NETWORK_STATE` (declared in the app manifest).
 */
class AndroidConnectivity(context: Context) : Connectivity {

  private val manager =
    context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

  private val _isOnline = MutableStateFlow(currentlyOnline())
  override val isOnline: StateFlow<Boolean> = _isOnline

  init {
    manager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
      override fun onAvailable(network: Network) {
        _isOnline.value = true
      }

      override fun onLost(network: Network) {
        _isOnline.value = currentlyOnline()
      }

      override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
        _isOnline.value = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
      }
    })
  }

  private fun currentlyOnline(): Boolean {
    val active = manager.activeNetwork ?: return false
    val caps = manager.getNetworkCapabilities(active) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
  }
}
