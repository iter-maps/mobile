package it.iterapp.core.wire

import kotlinx.serialization.Serializable

/**
 * `GET /places/enrich` — one normalized place fused from open sources.
 * Empty collections/maps are omitted on the wire, never null.
 */
@Serializable
data class Place(
  /** Scheme-prefixed canonical id: `wd:Q…` | `osm:N|W|R<id>` | `ov:<gers>` | `wp:<lang>:<title>`. */
  val id: String,
  val name: String,
  val names: Map<String, String>? = null,
  val category: Category,
  val location: LonLat,
  val description: String? = null,
  val summary: String? = null,
  val image: PlaceImage? = null,
  val address: Address? = null,
  val facets: Facets? = null,
  val related: List<Related>? = null,
  val provenance: List<Provenance>? = null,
)

@Serializable
data class Category(
  val primary: String,
  val tags: List<String>? = null,
)

/** `proxied` is true for images served through `GET /places/image`. */
@Serializable
data class PlaceImage(
  val url: String,
  val license: String? = null,
  val author: String? = null,
  val attribution: String? = null,
  val proxied: Boolean,
)

@Serializable
data class Address(
  val street: String? = null,
  val housenumber: String? = null,
  val postcode: String? = null,
  val city: String? = null,
)

@Serializable
data class Facets(
  val website: String? = null,
  val phone: String? = null,
  val openingHours: String? = null,
  val wheelchair: String? = null,
  val cuisine: String? = null,
  val diet: List<String>? = null,
  val outdoorSeating: Boolean? = null,
)

@Serializable
data class Related(
  val id: String,
  val name: String,
  val category: String? = null,
  /** `sameAddress` | `sameBrand` | `sameBuilding` | `nearbyCategory`. */
  val relation: String,
  val location: LonLat,
  val distanceM: Double? = null,
)

@Serializable
data class Provenance(
  val field: String,
  val source: String,
  val license: String? = null,
  val url: String? = null,
)

/** `GET /places/related` — always 200, possibly empty. */
@Serializable
data class RelatedResponse(
  val related: List<Related> = emptyList(),
)

/** `GET /places/collections` — share-alike editorial content; render the attribution. */
@Serializable
data class Collections(
  val destination: String,
  val lang: String,
  val source: String,
  val license: String,
  val url: String,
  val listings: List<Listing> = emptyList(),
)

@Serializable
data class Listing(
  /** `see`|`do`|`buy`|`eat`|`drink`|`sleep`|`other`. */
  val kind: String,
  val name: String,
  val alt: String? = null,
  val description: String? = null,
  val url: String? = null,
  val address: String? = null,
  val directions: String? = null,
  val phone: String? = null,
  val hours: String? = null,
  val price: String? = null,
  /** Wikidata QID — hop to `/places/enrich?wikidata=`. */
  val wikidata: String? = null,
  val wikipedia: String? = null,
  /** Commons file name — feeds `/places/image?file=`. */
  val image: String? = null,
  val location: LonLat? = null,
)
