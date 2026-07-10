package it.iterapp.core.net

import kotlinx.coroutines.flow.StateFlow

/**
 * Device network reachability, the platform-neutral signal that lets the app
 * tell "you're offline" (offline zones still work) apart from "the server is
 * unreachable" (ADR 0015). Backed by ConnectivityManager on Android and
 * NWPathMonitor on iOS — no GMS (ADR 0011). Optimistically `true` until the
 * platform monitor reports otherwise, so a slow monitor never blocks the UI.
 */
interface Connectivity {
  val isOnline: StateFlow<Boolean>
}
