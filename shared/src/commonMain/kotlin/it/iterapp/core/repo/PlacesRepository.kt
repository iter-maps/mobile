package it.iterapp.core.repo

import it.iterapp.core.api.IterApiException
import it.iterapp.core.api.IterGatewayClient
import it.iterapp.core.wire.Collections
import it.iterapp.core.wire.Place
import it.iterapp.core.wire.Related

class PlacesRepository(
  private val client: IterGatewayClient,
) {

  /** Null when the place has no article/coordinates (contract 404s). */
  @Throws(Exception::class)
  suspend fun enrichByWikidata(qid: String, lang: String? = null): Place? = swallow404 {
    client.enrichByWikidata(qid, lang)
  }

  @Throws(Exception::class)
  suspend fun enrichByWikipedia(langTitle: String, lang: String? = null): Place? = swallow404 {
    client.enrichByWikipedia(langTitle, lang)
  }

  @Throws(Exception::class)
  suspend fun enrichByTitle(title: String, lang: String): Place? = swallow404 {
    client.enrichByTitle(title, lang)
  }

  /** Resolves a displayable image URL for a Place, absolute against the gateway when proxied. */
  fun imageUrl(place: Place, width: Int = 640): String? {
    val image = place.image ?: return null
    return if (image.proxied && image.url.startsWith("/")) {
      "${client.baseUrl}${image.url}"
    } else if (image.proxied) {
      client.placeImageUrl(image.url, width)
    } else {
      image.url
    }
  }

  @Throws(Exception::class)
  suspend fun related(
    street: String?,
    housenumber: String?,
    city: String? = null,
    lat: Double? = null,
    lon: Double? = null,
  ): List<Related> =
    client.relatedPlaces(street, housenumber, city, brand = null, lat = lat, lon = lon).related

  /** Null when the destination has no guide. */
  @Throws(Exception::class)
  suspend fun collections(dest: String, lang: String? = null, kinds: List<String> = emptyList()): Collections? =
    swallow404 { client.collections(dest, lang, kinds) }

  private suspend fun <T> swallow404(block: suspend () -> T): T? = try {
    block()
  } catch (e: IterApiException) {
    if (e.status == 404) null else throw e
  }
}
