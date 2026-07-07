package it.iterapp.core.repo

import it.iterapp.core.api.IterApiException
import it.iterapp.core.api.IterGatewayClient
import it.iterapp.core.api.PlanParams
import it.iterapp.core.api.buildPlanRequest
import it.iterapp.core.geo.decodePolyline
import it.iterapp.core.model.DelayEstimate
import it.iterapp.core.model.GeoPoint
import it.iterapp.core.model.Itinerary
import it.iterapp.core.model.Leg
import it.iterapp.core.model.LegMode
import it.iterapp.core.model.RankingInfo
import it.iterapp.core.model.Waypoint
import it.iterapp.core.wire.OtpItinerary
import it.iterapp.core.wire.OtpLeg
import it.iterapp.core.wire.OtpPlace

class PlanRepository(
  private val client: IterGatewayClient,
) {

  /**
   * Plans a journey. [rerank] opts into gateway reranking (a profile from
   * `RerankProfiles`); [predictHistorical] opts into historical delay
   * annotation. GraphQL-level errors surface as [IterApiException] with code
   * `GRAPHQL_ERROR`.
   */
  @Throws(Exception::class)
  suspend fun plan(
    params: PlanParams,
    rerank: String? = null,
    predictHistorical: Boolean = true,
  ): List<Itinerary> {
    val response = client.plan(buildPlanRequest(params), rerank, predictHistorical)
    response.errors?.takeIf { it.isNotEmpty() }?.let { errors ->
      val message = errors.mapNotNull { it.message }.joinToString("; ").ifBlank { "plan failed" }
      throw IterApiException(200, "GRAPHQL_ERROR", message)
    }
    return response.data?.plan?.itineraries?.map { it.toDomain() } ?: emptyList()
  }
}

internal fun OtpItinerary.toDomain(): Itinerary = Itinerary(
  startMs = startTime,
  endMs = endTime,
  durationSeconds = duration.toLong(),
  numberOfTransfers = numberOfTransfers,
  walkDistanceMeters = walkDistance,
  legs = legs.map { it.toDomain() },
  ranking = if (rerankScore != null || reliabilityScore != null) {
    RankingInfo(
      reliabilityScore = reliabilityScore ?: 0.5,
      rerankScore = rerankScore ?: 0.0,
      factors = rerankFactors?.let {
        mapOf(
          "reliability" to it.reliability,
          "transfers" to it.transfers,
          "walk" to it.walk,
          "eco" to it.eco,
          "weather" to it.weather,
          "traffic" to it.traffic,
          "crowding" to it.crowding,
        )
      } ?: emptyMap(),
    )
  } else {
    null
  },
  predictedWorstDelaySeconds = predictedDelaySummary?.worstSeconds,
)

internal fun OtpLeg.toDomain(): Leg {
  val fromWp = from.toWaypoint("?")
  val toWp = to.toWaypoint("?")
  return Leg(
    mode = LegMode.fromOtp(mode),
    isTransit = transitLeg,
    from = fromWp,
    to = toWp,
    startMs = startTime,
    endMs = endTime,
    durationSeconds = duration.toLong(),
    distanceMeters = distance,
    isRealTime = realTime,
    arrivalDelaySeconds = arrivalDelay,
    departureDelaySeconds = departureDelay,
    headsign = headsign ?: trip?.tripHeadsign,
    routeGtfsId = route?.gtfsId,
    routeShortName = route?.shortName,
    routeLongName = route?.longName,
    routeColor = route?.color?.ifBlank { null },
    routeTextColor = route?.textColor?.ifBlank { null },
    directionId = trip?.directionId,
    geometry = legGeometry?.points?.let { decodePolyline(it) } ?: emptyList(),
    intermediateStops = intermediateStops.orEmpty().map {
      Waypoint(
        name = it.name ?: "?",
        point = GeoPoint(it.lat ?: 0.0, it.lon ?: 0.0),
        stopGtfsId = it.gtfsId,
      )
    },
    predictedDelay = predictedDelay?.let {
      DelayEstimate(p85Seconds = it.seconds, p50Seconds = it.p50Seconds, sampleCount = it.sampleCount)
    },
  )
}

private fun OtpPlace?.toWaypoint(fallbackName: String): Waypoint = Waypoint(
  name = this?.name ?: this?.stop?.name ?: fallbackName,
  point = GeoPoint(this?.lat ?: 0.0, this?.lon ?: 0.0),
  stopGtfsId = this?.stop?.gtfsId,
)
