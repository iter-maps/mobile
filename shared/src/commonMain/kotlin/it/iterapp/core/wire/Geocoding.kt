package it.iterapp.core.wire

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

/**
 * `osm_id` is usually a number, but gateway-injected features carry synthetic
 * STRING ids (`ov…` for build-time civici, `S\d+` for stations). Accept both,
 * normalizing to the string content.
 */
object FlexibleIdSerializer : KSerializer<String> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("FlexibleId", PrimitiveKind.STRING)

  override fun deserialize(decoder: Decoder): String {
    val json = decoder as? JsonDecoder ?: return decoder.decodeString()
    return json.decodeJsonElement().jsonPrimitive.content
  }

  override fun serialize(encoder: Encoder, value: String) {
    encoder.encodeString(value)
  }
}

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
  /** OSM element id, or a synthetic string (`ov…` civici, `S\d+` stations). */
  @Serializable(with = FlexibleIdSerializer::class)
  val osm_id: String? = null,
  /** `N` | `W` | `R`. */
  val osm_type: String? = null,
  val osm_key: String? = null,
  val osm_value: String? = null,
  /** The layer: house|street|locality|…|other, plus the gateway's `station`. */
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
