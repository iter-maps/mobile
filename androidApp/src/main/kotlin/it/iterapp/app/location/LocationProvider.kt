package it.iterapp.app.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import it.iterapp.core.model.GeoPoint
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Platform location via LocationManagerCompat — no Play Services (ADR 0011).
 * Fail-soft: without permission the flow completes immediately and
 * [lastKnown] returns null; permission UX is the caller's job.
 */
class LocationProvider(private val context: Context) {

  private val manager: LocationManager?
    get() = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

  fun hasPermission(): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
      PackageManager.PERMISSION_GRANTED ||
      ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
      PackageManager.PERMISSION_GRANTED

  /** Whether the system location toggle is on; permission is separate. */
  fun isLocationEnabled(): Boolean =
    manager?.let { LocationManagerCompat.isLocationEnabled(it) } ?: false

  // All providers worth listening to: fused alone can sit silent (notably on
  // emulators), so gps/network run alongside and the freshest fix wins.
  private fun viableProviders(lm: LocationManager): List<String> = buildList {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
      lm.allProviders.contains(LocationManager.FUSED_PROVIDER)
    ) {
      add(LocationManager.FUSED_PROVIDER)
    }
    if (lm.allProviders.contains(LocationManager.GPS_PROVIDER)) add(LocationManager.GPS_PROVIDER)
    if (lm.allProviders.contains(LocationManager.NETWORK_PROVIDER)) add(LocationManager.NETWORK_PROVIDER)
  }

  fun lastKnown(): GeoPoint? {
    if (!hasPermission()) return null
    val lm = manager ?: return null
    return try {
      lm.allProviders
        .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
        .maxByOrNull { it.time }
        ?.let { GeoPoint(it.latitude, it.longitude) }
    } catch (_: SecurityException) {
      null
    }
  }

  /** Continuous updates while collected; ~3s interval, provider-dependent. */
  // Permission is checked on entry and SecurityException handled; lint cannot
  // follow the guard through callbackFlow's early returns.
  @SuppressLint("MissingPermission")
  fun updates(): Flow<GeoPoint> = callbackFlow {
    if (!hasPermission()) {
      close()
      return@callbackFlow
    }
    val lm = manager ?: run { close(); return@callbackFlow }
    val providers = viableProviders(lm)
    if (providers.isEmpty()) {
      close()
      return@callbackFlow
    }
    lastKnown()?.let { trySend(it) }
    val listener = LocationListenerCompat { location ->
      trySend(GeoPoint(location.latitude, location.longitude))
    }
    val request = LocationRequestCompat.Builder(3_000L)
      .setQuality(LocationRequestCompat.QUALITY_BALANCED_POWER_ACCURACY)
      .setMinUpdateDistanceMeters(5f)
      .build()
    val registered = providers.filter { provider ->
      try {
        LocationManagerCompat.requestLocationUpdates(
          lm, provider, request, ContextCompat.getMainExecutor(context), listener,
        )
        true
      } catch (_: SecurityException) {
        false
      } catch (_: IllegalArgumentException) {
        false
      }
    }
    if (registered.isEmpty()) {
      close()
      return@callbackFlow
    }
    awaitClose { LocationManagerCompat.removeUpdates(lm, listener) }
  }
}
