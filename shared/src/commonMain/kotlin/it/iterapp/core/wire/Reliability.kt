package it.iterapp.core.wire

import kotlinx.serialization.Serializable

/**
 * `GET /reliability/{route}/{direction}/{stop}` — per-stop delay distribution.
 * Fail-soft: any miss yields empty `cells`, never an error. Path params echo
 * back verbatim.
 */
@Serializable
data class ReliabilityResponse(
  val route: String,
  val direction: String,
  val stop: String,
  val cells: List<ReliabilityCell> = emptyList(),
)

/**
 * One (todBucket, dayType) slice. The five optional fields travel as a group
 * and are absent (not null) when `sampleCount == 0`.
 */
@Serializable
data class ReliabilityCell(
  /** `early`|`am-peak`|`midday`|`pm-peak`|`evening`|`night`. */
  val todBucket: String,
  /** `weekday`|`saturday`|`sunday-holiday`. */
  val dayType: String,
  val sampleCount: Long,
  /** On-time rate over `[-60s, +300s]`, 0..1. */
  val onTimeRate: Double? = null,
  /** Median delay, seconds; negative = early. */
  val p50S: Double? = null,
  val p85S: Double? = null,
  val p90S: Double? = null,
  val meanS: Double? = null,
)
