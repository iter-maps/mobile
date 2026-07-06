package it.iterapp.core.geo

import it.iterapp.core.model.GeoPoint
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val EARTH_RADIUS_M = 6_371_000.0

fun haversineMeters(a: GeoPoint, b: GeoPoint): Double {
  val dLat = (b.lat - a.lat).toRadians()
  val dLon = (b.lon - a.lon).toRadians()
  val sinLat = sin(dLat / 2)
  val sinLon = sin(dLon / 2)
  val h = sinLat * sinLat + cos(a.lat.toRadians()) * cos(b.lat.toRadians()) * sinLon * sinLon
  return 2 * EARTH_RADIUS_M * atan2(sqrt(h), sqrt(1 - h))
}

private fun Double.toRadians(): Double = this * PI / 180.0

/**
 * The shared bbox wire format: `minLon,minLat,maxLon,maxLat` (WGS84, strictly
 * non-degenerate). [format] produces it; [validate] mirrors the server's
 * validation order so the UI can reject bad boxes before a round-trip.
 */
object BBoxFormat {

  fun format(minLon: Double, minLat: Double, maxLon: Double, maxLat: Double): String =
    "$minLon,$minLat,$maxLon,$maxLat"

  /** Null when valid, else the matching wire error code. */
  fun validate(minLon: Double, minLat: Double, maxLon: Double, maxLat: Double): String? {
    val values = listOf(minLon, minLat, maxLon, maxLat)
    if (values.any { it.isNaN() || it.isInfinite() }) return "BBOX_INVALID"
    if (minLon !in -180.0..180.0 || maxLon !in -180.0..180.0) return "BBOX_OUT_OF_RANGE"
    if (minLat !in -90.0..90.0 || maxLat !in -90.0..90.0) return "BBOX_OUT_OF_RANGE"
    if (minLon >= maxLon || minLat >= maxLat) return "BBOX_DEGENERATE"
    return null
  }

  /** Planar area in square degrees — the unit the offline area cap (6.0) uses. */
  fun areaDeg2(minLon: Double, minLat: Double, maxLon: Double, maxLat: Double): Double =
    (maxLon - minLon) * (maxLat - minLat)

  /** The server-side offline area cap, mirrored for pre-flight UI checks. */
  const val OFFLINE_AREA_CAP_DEG2 = 6.0
}

/** Strips an OTP feed prefix: `ATAC:70001` → `70001`; unprefixed ids pass through. */
fun gtfsLocalId(id: String): String = id.substringAfter(':')
