package it.iterapp.core.wire

import kotlinx.serialization.Serializable

/**
 * Photon GeoJSON, relayed verbatim by `GET /api` and `GET /reverse`.
 * Geometry coordinates are `[lon, lat]`. Property names are Photon's
 * snake_case — kept literal here.
 */
@Serializable
data class PhotonFeatureCollection(
  val type: String = "FeatureCollection",
  val features: List<PhotonFeature> = emptyList(),
)

@Serializable
data class PhotonFeature(
  val type: String = "Feature",
  val geometry: PhotonGeometry? = null,
  val properties: PhotonProperties = PhotonProperties(),
)

@Serializable
data class PhotonGeometry(
  val type: String = "Point",
  /** `[lon, lat]`. */
  val coordinates: List<Double> = emptyList(),
)

@Serializable
data class PhotonProperties(
  val osm_id: Long? = null,
  /** `N` | `W` | `R`. */
  val osm_type: String? = null,
  val osm_key: String? = null,
  val osm_value: String? = null,
  /** The layer: house|street|locality|district|city|county|state|country|other. */
  val type: String? = null,
  val name: String? = null,
  val housenumber: String? = null,
  val street: String? = null,
  val district: String? = null,
  val locality: String? = null,
  val city: String? = null,
  val county: String? = null,
  val state: String? = null,
  val postcode: String? = null,
  val countrycode: String? = null,
  val country: String? = null,
  /** `[minLon, maxLat, maxLon, minLat]`. */
  val extent: List<Double>? = null,
  /** Metres from the query point (reverse geocoding). */
  val distance: Double? = null,
)
