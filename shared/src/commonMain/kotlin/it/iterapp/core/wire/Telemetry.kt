package it.iterapp.core.wire

import kotlinx.serialization.Serializable

/**
 * `POST /telemetry` request body. Server enforces `deny_unknown_fields` —
 * never attach extra keys. 202 on accept (not a durability ack), bare 404
 * when ingest is disabled (the default).
 */
@Serializable
data class TelemetryBatch(
  /** Opaque client token, 16..128 chars of `[A-Za-z0-9_-]`. */
  val token: String,
  /** 1..128 samples. */
  val samples: List<TelemetrySample>,
)

@Serializable
data class TelemetrySample(
  /** Feed-prefixed route id, e.g. `ATAC:MEA`. */
  val routeId: String,
  /** Absent/null keys as direction 0; must be 0..1 after defaulting. */
  val directionId: Int? = null,
  val stopId: String,
  /** Client wall-clock epoch millis inside the plausibility window. */
  val epochMs: Long,
  /** Closed set: `presence` | `seated` | `standing` | `crowded`. */
  val signal: String,
)

/** `PUT /sync/{keyId}` success body: the new version = SHA-256 hex = next `If-Match`. */
@Serializable
data class SyncPutResponse(
  val version: String,
)
