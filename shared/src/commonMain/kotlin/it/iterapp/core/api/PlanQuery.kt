package it.iterapp.core.api

import it.iterapp.core.wire.GraphQlRequest
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/** Transit modes offered to the planner. */
enum class PlanTransitMode { BUS, SUBWAY, TRAM, RAIL }

/**
 * Parameters for one plan request. [date] (`YYYY-MM-DD`) and [time] (`HH:MM`)
 * are in the router's local timezone; both null means "leave now".
 */
data class PlanParams(
  val fromLat: Double,
  val fromLon: Double,
  val toLat: Double,
  val toLon: Double,
  val date: String? = null,
  val time: String? = null,
  val arriveBy: Boolean = false,
  val numItineraries: Int = 5,
  val wheelchair: Boolean = false,
  val walkReluctance: Double = 2.0,
  val walkSpeed: Double = 1.33,
  val maxTransfers: Int? = null,
  val transitModes: Set<PlanTransitMode> = PlanTransitMode.entries.toSet(),
)

/**
 * Builds the OTP GTFS GraphQL `plan` query. Everything is passed as GraphQL
 * variables except [PlanParams.maxTransfers], inlined only when set so the
 * argument can be omitted entirely. The leg selection deliberately includes
 * every field the gateway's rerank/predict transforms key on: `mode`,
 * `transitLeg`, `realTime`, `duration`, `distance`, `route.gtfsId`,
 * `trip.directionId`, `from.stop.gtfsId`, `startTime`, `endTime`.
 */
fun buildPlanRequest(params: PlanParams): GraphQlRequest {
  val hasDateTime = params.date != null && params.time != null
  val varDecls = buildString {
    append("\$from: InputCoordinates!, \$to: InputCoordinates!, ")
    append("\$numItineraries: Int!, \$wheelchair: Boolean!, ")
    append("\$walkReluctance: Float!, \$walkSpeed: Float!, ")
    append("\$transportModes: [TransportMode]!")
    if (hasDateTime) append(", \$date: String!, \$time: String!, \$arriveBy: Boolean!")
  }
  val args = buildString {
    append("from: \$from, to: \$to, ")
    append("numItineraries: \$numItineraries, wheelchair: \$wheelchair, ")
    append("walkReluctance: \$walkReluctance, walkSpeed: \$walkSpeed, ")
    append("transportModes: \$transportModes")
    if (hasDateTime) append(", date: \$date, time: \$time, arriveBy: \$arriveBy")
    params.maxTransfers?.let { append(", maxTransfers: $it") }
  }

  val query = """
    query Plan($varDecls) {
      plan($args) {
        itineraries {
          startTime endTime duration numberOfTransfers walkDistance
          legs {
            mode transitLeg realTime realtimeState
            startTime endTime duration distance
            departureDelay arrivalDelay headsign
            from { name lat lon stop { gtfsId name } }
            to { name lat lon stop { gtfsId name } }
            route { gtfsId shortName longName color textColor mode }
            trip { gtfsId directionId tripHeadsign }
            legGeometry { points length }
            intermediateStops { gtfsId name lat lon }
          }
        }
      }
    }
  """.trimIndent()

  val variables = buildJsonObject {
    putJsonObject("from") {
      put("lat", params.fromLat)
      put("lon", params.fromLon)
    }
    putJsonObject("to") {
      put("lat", params.toLat)
      put("lon", params.toLon)
    }
    put("numItineraries", params.numItineraries)
    put("wheelchair", params.wheelchair)
    put("walkReluctance", params.walkReluctance)
    put("walkSpeed", params.walkSpeed)
    putJsonArray("transportModes") {
      addJsonObject { put("mode", "WALK") }
      params.transitModes.sortedBy { it.ordinal }.forEach { mode ->
        addJsonObject { put("mode", mode.name) }
      }
    }
    if (hasDateTime) {
      put("date", params.date)
      put("time", params.time)
      put("arriveBy", params.arriveBy)
    }
  }

  return GraphQlRequest(query = query, variables = variables)
}
