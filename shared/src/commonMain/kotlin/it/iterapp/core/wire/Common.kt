package it.iterapp.core.wire

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Wire types mirror the gateway contract exactly (ADR 0005): field names,
 * optionality and absent-vs-null semantics match the server's serde output.
 * `field: T? = null` models a key that may be absent; fields documented as
 * "always present, null when unset" carry an explicit comment.
 */

/** A WGS84 point as named fields (places surfaces). Geocoding keeps GeoJSON `[lon, lat]` arrays instead. */
@Serializable
data class LonLat(
  val lon: Double,
  val lat: Double,
)

/** Service status vocabulary, serialized lowercase. */
object StatusValues {
  const val OK = "ok"
  const val DEGRADED = "degraded"
  const val DOWN = "down"
}

/** The uniform error body: `{ "error": { code, message, details? } }`. */
@Serializable
data class ApiErrorEnvelope(
  val error: ApiErrorBody,
)

@Serializable
data class ApiErrorBody(
  /** Stable machine code — branch on this, never on [message]. */
  val code: String,
  /** Human-readable; not a stable contract. */
  val message: String,
  /** Structured context; key absent when the handler attached none. */
  val details: JsonElement? = null,
)

/** Known stable error codes. The set can grow — always tolerate unknown codes. */
object ApiErrorCodes {
  const val BAD_REQUEST = "BAD_REQUEST"
  const val NOT_FOUND = "NOT_FOUND"
  const val AREA_TOO_LARGE = "AREA_TOO_LARGE"
  const val PAYLOAD_TOO_LARGE = "PAYLOAD_TOO_LARGE"
  const val VERSION_CONFLICT = "VERSION_CONFLICT"
  const val STORE_FULL = "STORE_FULL"
  const val BUSY = "BUSY"
  const val UPSTREAM_UNAVAILABLE = "UPSTREAM_UNAVAILABLE"
  const val UPSTREAM_ERROR = "UPSTREAM_ERROR"
  const val TIMEOUT = "TIMEOUT"
  const val INTERNAL = "INTERNAL"

  // Offline surface
  const val BBOX_REQUIRED = "BBOX_REQUIRED"
  const val BBOX_INVALID = "BBOX_INVALID"
  const val BBOX_OUT_OF_RANGE = "BBOX_OUT_OF_RANGE"
  const val BBOX_DEGENERATE = "BBOX_DEGENERATE"
  const val ZOOM_INVALID = "ZOOM_INVALID"
  const val EXTRACT_FAILED = "EXTRACT_FAILED"
}
