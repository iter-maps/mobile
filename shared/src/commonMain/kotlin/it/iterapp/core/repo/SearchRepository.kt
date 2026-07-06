package it.iterapp.core.repo

import it.iterapp.core.api.IterGatewayClient
import it.iterapp.core.model.GeoPoint
import it.iterapp.core.model.SearchResult
import it.iterapp.core.wire.PhotonFeature

class SearchRepository(
  private val client: IterGatewayClient,
) {

  suspend fun search(
    query: String,
    lang: String? = null,
    bias: GeoPoint? = null,
    limit: Int = 15,
  ): List<SearchResult> =
    client.geocode(
      query = query,
      lang = lang,
      limit = limit,
      biasLat = bias?.lat,
      biasLon = bias?.lon,
      biasScale = if (bias != null) DEFAULT_BIAS_SCALE else null,
    ).features.mapNotNull { it.toResult() }

  suspend fun reverse(point: GeoPoint, lang: String? = null): SearchResult? =
    client.reverse(point.lat, point.lon, lang = lang, limit = 1)
      .features.firstOrNull()?.toResult()

  private companion object {
    const val DEFAULT_BIAS_SCALE = 0.5
  }
}

internal fun PhotonFeature.toResult(): SearchResult? {
  val coords = geometry?.coordinates ?: return null
  if (coords.size < 2) return null
  val p = properties
  val name = p.name
    ?: listOfNotNull(p.street, p.housenumber).joinToString(" ").ifBlank { null }
    ?: p.city
    ?: return null
  val detail = buildList {
    if (p.name != null && p.street != null) {
      add(listOfNotNull(p.street, p.housenumber).joinToString(" "))
    }
    p.locality?.let { add(it) }
    p.city?.takeIf { it != name }?.let { add(it) }
    if (isEmpty()) p.county?.let { add(it) }
  }.distinct().joinToString(", ").ifBlank { null }
  val id = when {
    p.osm_type != null && p.osm_id != null -> "osm:${p.osm_type}${p.osm_id}"
    else -> "pt:${coords[1]},${coords[0]}"
  }
  return SearchResult(
    id = id,
    name = name,
    detail = detail,
    point = GeoPoint(lat = coords[1], lon = coords[0]),
    osmKey = p.osm_key,
    osmValue = p.osm_value,
    layer = p.type,
    street = p.street,
    housenumber = p.housenumber,
    city = p.city,
    distanceM = p.distance,
  )
}
