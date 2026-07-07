package it.iterapp.core.model

import kotlinx.serialization.Serializable

@Serializable
data class GeoPoint(
  val lat: Double,
  val lon: Double,
)

/** One search result mapped from a Photon feature. */
data class SearchResult(
  /** `osm:<type><id>` when OSM-backed, else a synthetic id. */
  val id: String,
  val name: String,
  /** Secondary line: street + civic, locality, city — whatever is known. */
  val detail: String?,
  val point: GeoPoint,
  val osmKey: String?,
  val osmValue: String?,
  /** Photon layer: house|street|locality|district|city|… */
  val layer: String?,
  val street: String? = null,
  val housenumber: String? = null,
  val city: String? = null,
  /** Metres from the query point on reverse lookups. */
  val distanceM: Double? = null,
  /** ViaggiaTreno station id (`^S\d+$`) when this is a station-typed result — the boards entry point. */
  val stationId: String? = null,
) {
  /** Train stations hop to the live-boards surface. */
  val isTrainStation: Boolean
    get() = stationId != null ||
      (osmKey == "railway" && (osmValue == "station" || osmValue == "halt"))
}

enum class LegMode {
  WALK, BUS, SUBWAY, TRAM, RAIL, FERRY, FUNICULAR, GONDOLA, BICYCLE, CAR, OTHER;

  val isTransit: Boolean
    get() = this != WALK && this != BICYCLE && this != CAR && this != OTHER

  companion object {
    fun fromOtp(mode: String?): LegMode = when (mode?.uppercase()) {
      "WALK" -> WALK
      "BUS", "TROLLEYBUS", "COACH" -> BUS
      "SUBWAY", "METRO" -> SUBWAY
      "TRAM" -> TRAM
      "RAIL" -> RAIL
      "FERRY" -> FERRY
      "FUNICULAR" -> FUNICULAR
      "GONDOLA", "CABLE_CAR" -> GONDOLA
      "BICYCLE", "SCOOTER" -> BICYCLE
      "CAR", "TAXI" -> CAR
      else -> OTHER
    }
  }
}

data class Waypoint(
  val name: String,
  val point: GeoPoint,
  val stopGtfsId: String? = null,
)

/** Historical delay estimate carried over from the gateway's predict transform. */
data class DelayEstimate(
  /** Conservative p85 seconds; negative = usually early. */
  val p85Seconds: Double,
  val p50Seconds: Double,
  val sampleCount: Long,
)

data class Leg(
  val mode: LegMode,
  val isTransit: Boolean,
  val from: Waypoint,
  val to: Waypoint,
  /** Epoch millis. */
  val startMs: Long,
  val endMs: Long,
  val durationSeconds: Long,
  val distanceMeters: Double,
  val isRealTime: Boolean,
  /** Signed seconds of live arrival delay when realtime. */
  val arrivalDelaySeconds: Int,
  /** Signed seconds of live departure delay when realtime. */
  val departureDelaySeconds: Int,
  val headsign: String?,
  val routeGtfsId: String?,
  val routeShortName: String?,
  val routeLongName: String?,
  /** GTFS hex without `#`. */
  val routeColor: String?,
  val routeTextColor: String?,
  val directionId: String?,
  val geometry: List<GeoPoint>,
  val intermediateStops: List<Waypoint>,
  val predictedDelay: DelayEstimate?,
)

/** Why an itinerary ranked where it did, when reranking was requested. */
data class RankingInfo(
  /** Mean historical on-time rate 0..1; 0.5 is neutral. */
  val reliabilityScore: Double,
  val rerankScore: Double,
  /** Weighted factor contributions, keyed by factor name. */
  val factors: Map<String, Double>,
)

data class Itinerary(
  val startMs: Long,
  val endMs: Long,
  val durationSeconds: Long,
  val numberOfTransfers: Int,
  val walkDistanceMeters: Double,
  val legs: List<Leg>,
  val ranking: RankingInfo?,
  /** Worst predicted p85 delay across annotated legs, seconds. */
  val predictedWorstDelaySeconds: Double?,
)
