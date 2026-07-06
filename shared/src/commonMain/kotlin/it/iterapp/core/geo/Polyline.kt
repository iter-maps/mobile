package it.iterapp.core.geo

import it.iterapp.core.model.GeoPoint

/**
 * Decodes a Google encoded polyline (precision 1e-5), the format OTP emits in
 * `legGeometry.points`. Malformed input returns the points decoded so far.
 */
fun decodePolyline(encoded: String): List<GeoPoint> {
  val points = ArrayList<GeoPoint>(encoded.length / 4)
  var index = 0
  var lat = 0
  var lon = 0
  while (index < encoded.length) {
    var result = 0
    var shift = 0
    var byte: Int
    do {
      if (index >= encoded.length) return points
      byte = encoded[index++].code - 63
      result = result or ((byte and 0x1f) shl shift)
      shift += 5
    } while (byte >= 0x20)
    lat += if (result and 1 != 0) (result shr 1).inv() else result shr 1

    result = 0
    shift = 0
    do {
      if (index >= encoded.length) return points
      byte = encoded[index++].code - 63
      result = result or ((byte and 0x1f) shl shift)
      shift += 5
    } while (byte >= 0x20)
    lon += if (result and 1 != 0) (result shr 1).inv() else result shr 1

    points.add(GeoPoint(lat / 1e5, lon / 1e5))
  }
  return points
}
