package it.iterapp.core.wire

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * `POST /otp/gtfs/v1` — OpenTripPlanner GraphQL passthrough. The base plan
 * schema is OTP's own; the gateway only ever *adds* optional fields
 * (rerank scores per itinerary, predicted delays per RT-less transit leg),
 * modeled here as nullable extras.
 */
@Serializable
data class GraphQlRequest(
  val query: String,
  val variables: JsonObject? = null,
)

@Serializable
data class OtpPlanResponse(
  val data: OtpPlanData? = null,
  /** GraphQL errors arrive as HTTP 200 with this array. */
  val errors: List<GraphQlError>? = null,
)

@Serializable
data class GraphQlError(
  val message: String? = null,
  val path: List<JsonElement>? = null,
)

@Serializable
data class OtpPlanData(
  val plan: OtpPlan? = null,
)

@Serializable
data class OtpPlan(
  val itineraries: List<OtpItinerary> = emptyList(),
)

@Serializable
data class OtpItinerary(
  /** Epoch millis. */
  val startTime: Long = 0,
  val endTime: Long = 0,
  /** Seconds; OTP may emit a decimal, so this stays Double on the wire. */
  val duration: Double = 0.0,
  val numberOfTransfers: Int = 0,
  val walkDistance: Double = 0.0,
  val legs: List<OtpLeg> = emptyList(),
  // Gateway-additive fields (`?rerank=<profile>`); absent on passthrough.
  val reliabilityScore: Double? = null,
  val rerankScore: Double? = null,
  val rerankFactors: RerankFactors? = null,
  // Gateway-additive (`?predict=historical`); present only when ≥1 leg annotated.
  val predictedDelaySummary: PredictedDelaySummary? = null,
)

@Serializable
data class OtpLeg(
  val mode: String? = null,
  val transitLeg: Boolean = false,
  val realTime: Boolean = false,
  val realtimeState: String? = null,
  /** Epoch millis. */
  val startTime: Long = 0,
  val endTime: Long = 0,
  /** Seconds; Float in OTP's schema — never assume an integer. */
  val duration: Double = 0.0,
  /** Metres. */
  val distance: Double = 0.0,
  /** Seconds of live delay when realtime (signed). */
  val departureDelay: Int = 0,
  val arrivalDelay: Int = 0,
  val headsign: String? = null,
  val from: OtpPlace? = null,
  val to: OtpPlace? = null,
  val route: OtpRoute? = null,
  val trip: OtpTrip? = null,
  val legGeometry: OtpGeometry? = null,
  val intermediateStops: List<OtpStop>? = null,
  // Gateway-additive (`?predict=historical`); never on live-RT legs.
  val predictedDelay: PredictedDelay? = null,
)

@Serializable
data class OtpPlace(
  val name: String? = null,
  val lat: Double? = null,
  val lon: Double? = null,
  val stop: OtpStop? = null,
)

@Serializable
data class OtpStop(
  val gtfsId: String? = null,
  val name: String? = null,
  val lat: Double? = null,
  val lon: Double? = null,
)

@Serializable
data class OtpRoute(
  val gtfsId: String? = null,
  val shortName: String? = null,
  val longName: String? = null,
  /** Hex without `#`, straight from GTFS. */
  val color: String? = null,
  val textColor: String? = null,
  val mode: String? = null,
)

@Serializable
data class OtpTrip(
  val gtfsId: String? = null,
  /** String, number or null upstream; the gateway tolerates all. */
  val directionId: String? = null,
  val tripHeadsign: String? = null,
)

@Serializable
data class OtpGeometry(
  /** Google encoded polyline. */
  val points: String? = null,
  val length: Int? = null,
)

/** Per-factor weighted contributions; all seven keys present when rerank fires. */
@Serializable
data class RerankFactors(
  val reliability: Double = 0.0,
  val transfers: Double = 0.0,
  val walk: Double = 0.0,
  val eco: Double = 0.0,
  val weather: Double = 0.0,
  val traffic: Double = 0.0,
  val crowding: Double = 0.0,
)

/** Historical delay estimate on an RT-less transit leg. */
@Serializable
data class PredictedDelay(
  /** Conservative p85 delay, seconds; can be negative. */
  val seconds: Double,
  val p50Seconds: Double,
  val sampleCount: Long,
  /** Always `"historical"`. */
  val source: String,
)

@Serializable
data class PredictedDelaySummary(
  val annotatedLegs: Int,
  val worstSeconds: Double,
  val source: String,
)

/** Recognized rerank profiles; anything else is silently a passthrough. */
object RerankProfiles {
  const val RELIABILITY = "reliability"
  const val BALANCED = "balanced"
  const val ECO = "eco"
  const val COMFORT = "comfort"
  val ALL = listOf(RELIABILITY, BALANCED, ECO, COMFORT)
}
