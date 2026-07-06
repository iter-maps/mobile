package it.iterapp.core.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.client.plugins.timeout
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.HttpStatement
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.encodeURLParameter
import io.ktor.http.encodeURLPathPart
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import it.iterapp.core.wire.ApiErrorEnvelope
import it.iterapp.core.wire.BoardEntry
import it.iterapp.core.wire.Collections
import it.iterapp.core.wire.FreshnessManifest
import it.iterapp.core.wire.GraphQlRequest
import it.iterapp.core.wire.OtpPlanResponse
import it.iterapp.core.wire.PhotonFeatureCollection
import it.iterapp.core.wire.Place
import it.iterapp.core.wire.RelatedResponse
import it.iterapp.core.wire.ReliabilityResponse
import it.iterapp.core.wire.StaticHealth
import it.iterapp.core.wire.StatsSummary
import it.iterapp.core.wire.Station
import it.iterapp.core.wire.SyncPutResponse
import it.iterapp.core.wire.TelemetryBatch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.random.Random
import okio.BufferedSink

/** A downloaded sync blob: raw ciphertext plus its version (the `ETag` value). */
class SyncBlob(
  val bytes: ByteArray,
  val version: String?,
)

/**
 * The one client for the one gateway origin (ADR 0004). Every call goes
 * through [request], which converts non-2xx responses into
 * [IterApiException] (decoding the JSON envelope when there is one) and
 * transport failures into [IterTransportException]. Per-endpoint quirks —
 * bare 404s for disabled features, fail-soft empty bodies — are handled where
 * the contract defines them.
 */
class IterGatewayClient(
  val http: HttpClient,
  private val baseUrlProvider: () -> String,
) {

  val baseUrl: String get() = baseUrlProvider().trimEnd('/')

  // ── Health & ops ──

  suspend fun health(): StaticHealth = request { get("$baseUrl/health.json") }.body()

  suspend fun manifest(): FreshnessManifest = request { get("$baseUrl/manifest") }.body()

  /** Null when the stats surface is disabled (bare 404). */
  suspend fun stats(): StatsSummary? = optional { request { get("$baseUrl/stats") } }?.body()

  // ── Map assets ──

  /** The style URL handed to MapLibre; `name` is one of [it.iterapp.core.wire.StyleNames]. */
  fun styleUrl(name: String): String = "$baseUrl/styles/$name.json"

  fun overlayUrl(kind: String): String = "$baseUrl/overlays/$kind.geojson"

  /** Overlay GeoJSON as text; empty FeatureCollection means "draw nothing". */
  suspend fun overlay(kind: String): String =
    request { get(overlayUrl(kind)) }.bodyAsText()

  suspend fun style(name: String): String =
    request { get(styleUrl(name)) }.bodyAsText()

  // ── Geocoding (Photon passthrough) ──

  suspend fun geocode(
    query: String,
    lang: String? = null,
    limit: Int? = null,
    biasLat: Double? = null,
    biasLon: Double? = null,
    biasScale: Double? = null,
    layers: List<String> = emptyList(),
    bbox: String? = null,
  ): PhotonFeatureCollection = request {
    get("$baseUrl/api") {
      parameter("q", query)
      lang?.let { parameter("lang", it) }
      limit?.let { parameter("limit", it) }
      if (biasLat != null && biasLon != null) {
        parameter("lat", biasLat)
        parameter("lon", biasLon)
        biasScale?.let { parameter("location_bias_scale", it) }
      }
      layers.forEach { parameter("layer", it) }
      bbox?.let { parameter("bbox", it) }
    }
  }.body()

  suspend fun reverse(
    lat: Double,
    lon: Double,
    lang: String? = null,
    limit: Int? = null,
    radiusKm: Double? = null,
  ): PhotonFeatureCollection = request {
    get("$baseUrl/reverse") {
      parameter("lat", lat)
      parameter("lon", lon)
      lang?.let { parameter("lang", it) }
      limit?.let { parameter("limit", it) }
      radiusKm?.let { parameter("radius", it) }
    }
  }.body()

  // ── Routing ──

  /**
   * POST the plan query; [rerank] is one of
   * [it.iterapp.core.wire.RerankProfiles], [predictHistorical] opts into
   * historical delay annotation. GraphQL errors come back inside the body
   * (HTTP 200) — the caller checks `errors`.
   */
  suspend fun plan(
    body: GraphQlRequest,
    rerank: String? = null,
    predictHistorical: Boolean = false,
  ): OtpPlanResponse = request {
    post("$baseUrl/otp/gtfs/v1") {
      rerank?.let { parameter("rerank", it) }
      if (predictHistorical) parameter("predict", "historical")
      contentType(ContentType.Application.Json)
      setBody(body)
    }
  }.body()

  // ── Places ──

  suspend fun enrichByWikidata(qid: String, lang: String? = null): Place = request {
    get("$baseUrl/places/enrich") {
      parameter("wikidata", qid)
      lang?.let { parameter("lang", it) }
    }
  }.body()

  suspend fun enrichByWikipedia(langTitle: String, lang: String? = null): Place = request {
    get("$baseUrl/places/enrich") {
      parameter("wikipedia", langTitle)
      lang?.let { parameter("lang", it) }
    }
  }.body()

  suspend fun enrichByTitle(title: String, lang: String): Place = request {
    get("$baseUrl/places/enrich") {
      parameter("title", title)
      parameter("lang", lang)
    }
  }.body()

  /** URL for a Commons image proxied by the gateway; width clamped server-side to 16..2048. */
  fun placeImageUrl(file: String, width: Int = 640): String {
    // Query-parameter encoding: Commons names legally contain '&', '+', '='.
    val encoded = file.encodeURLParameter()
    return "$baseUrl/places/image?file=$encoded&width=$width"
  }

  suspend fun relatedPlaces(
    street: String? = null,
    housenumber: String? = null,
    city: String? = null,
    brand: String? = null,
    lat: Double? = null,
    lon: Double? = null,
  ): RelatedResponse = request {
    get("$baseUrl/places/related") {
      street?.let { parameter("street", it) }
      housenumber?.let { parameter("housenumber", it) }
      city?.let { parameter("city", it) }
      brand?.let { parameter("brand", it) }
      lat?.let { parameter("lat", it) }
      lon?.let { parameter("lon", it) }
    }
  }.body()

  suspend fun collections(
    dest: String,
    lang: String? = null,
    kinds: List<String> = emptyList(),
  ): Collections = request {
    get("$baseUrl/places/collections") {
      parameter("dest", dest)
      lang?.let { parameter("lang", it) }
      if (kinds.isNotEmpty()) parameter("kinds", kinds.joinToString(","))
    }
  }.body()

  // ── Live trains ──

  suspend fun searchStations(query: String): List<Station> = request {
    get("$baseUrl/trenitalia/stations/search") { parameter("q", query) }
  }.body()

  suspend fun stations(region: Int? = null): List<Station> = request {
    get("$baseUrl/trenitalia/stations") { region?.let { parameter("region", it) } }
  }.body()

  suspend fun departures(stationId: String): List<BoardEntry> = request {
    get("$baseUrl/trenitalia/departures") { parameter("station", stationId) }
  }.body()

  suspend fun arrivals(stationId: String): List<BoardEntry> = request {
    get("$baseUrl/trenitalia/arrivals") { parameter("station", stationId) }
  }.body()

  // ── Reliability ──

  /** Always 200 by contract; empty `cells` means no history. Ids passed verbatim (URL-encoded). */
  suspend fun reliability(route: String, direction: String, stop: String): ReliabilityResponse =
    request {
      get(
        "$baseUrl/reliability/${route.encodeURLPathPart()}/" +
          "${direction.encodeURLPathPart()}/${stop.encodeURLPathPart()}",
      )
    }.body()

  // ── Offline ──

  /**
   * Streams an offline bundle ZIP into [sink]. [bbox] is
   * `minLon,minLat,maxLon,maxLat`. Emits [onProgress] with (bytesRead,
   * totalOrNull). Throws [IterApiException] on the contract's 400/413/503.
   */
  suspend fun downloadOfflineBundle(
    bbox: String,
    maxzoom: Int? = null,
    minzoom: Int? = null,
    styles: List<String> = emptyList(),
    glyphs: Boolean = true,
    sprite: Boolean = true,
    overlays: Boolean = true,
    sink: BufferedSink,
    onProgress: (Long, Long?) -> Unit = { _, _ -> },
  ) {
    downloadToSink(sink, onProgress) {
      prepareGet("$baseUrl/offline/bundle") {
        parameter("bbox", bbox)
        maxzoom?.let { parameter("maxzoom", it) }
        minzoom?.let { parameter("minzoom", it) }
        if (styles.isNotEmpty()) parameter("styles", styles.joinToString(","))
        parameter("glyphs", glyphs)
        parameter("sprite", sprite)
        parameter("overlays", overlays)
        timeout { requestTimeoutMillis = DOWNLOAD_TIMEOUT_MS }
      }
    }
  }

  /** Streams a bare bbox-clipped PMTiles extract into [sink]. */
  suspend fun downloadOfflineExtract(
    bbox: String,
    maxzoom: Int? = null,
    minzoom: Int? = null,
    sink: BufferedSink,
    onProgress: (Long, Long?) -> Unit = { _, _ -> },
  ) {
    downloadToSink(sink, onProgress) {
      prepareGet("$baseUrl/offline/extract") {
        parameter("bbox", bbox)
        maxzoom?.let { parameter("maxzoom", it) }
        minzoom?.let { parameter("minzoom", it) }
        timeout { requestTimeoutMillis = DOWNLOAD_TIMEOUT_MS }
      }
    }
  }

  // ── Telemetry & sync (default-OFF surfaces) ──

  /** True when accepted (202), false when the surface is disabled (bare 404). */
  suspend fun sendTelemetry(batch: TelemetryBatch): Boolean {
    val response = optional {
      request {
        post("$baseUrl/telemetry") {
          contentType(ContentType.Application.Json)
          setBody(batch)
        }
      }
    }
    return response != null
  }

  /** Null when disabled, key unknown, or blob absent — indistinguishable by design. */
  suspend fun syncGet(keyId: String): SyncBlob? {
    val response = optional { request { get("$baseUrl/sync/${keyId.encodeURLPathPart()}") } }
      ?: return null
    val etag = response.headers["ETag"]?.trim('"')
    return SyncBlob(response.bodyAsBytes(), etag)
  }

  /**
   * PUT ciphertext. [ifMatch] must be the current version on replace and null
   * on create; a stale precondition raises `VERSION_CONFLICT` (409).
   */
  suspend fun syncPut(keyId: String, ciphertext: ByteArray, ifMatch: String? = null): SyncPutResponse =
    request {
      put("$baseUrl/sync/${keyId.encodeURLPathPart()}") {
        ifMatch?.let { header("If-Match", "\"$it\"") }
        setBody(ciphertext)
      }
    }.body()

  /**
   * Idempotent delete: a 404 (disabled feature / unknown key / already gone —
   * indistinguishable by contract) is swallowed. A stale [ifMatch] still
   * raises `VERSION_CONFLICT`.
   */
  suspend fun syncDelete(keyId: String, ifMatch: String? = null) {
    optional {
      request {
        delete("$baseUrl/sync/${keyId.encodeURLPathPart()}") {
          ifMatch?.let { header("If-Match", "\"$it\"") }
        }
      }
    }
  }

  // ── Plumbing ──

  private suspend fun request(block: suspend HttpClient.() -> HttpResponse): HttpResponse {
    val response = try {
      http.block()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      throw IterTransportException("gateway unreachable: ${e.message}", e)
    }
    if (response.status.isSuccess()) return response
    throw response.toApiException()
  }

  /** Swallows 404 into null — for the contract's "may mean feature off" routes. */
  private suspend fun optional(block: suspend () -> HttpResponse): HttpResponse? =
    try {
      block()
    } catch (e: IterApiException) {
      if (e.status == 404) null else throw e
    }

  /**
   * True streaming download: `prepareGet(...).execute { }` hands the response
   * over before the body is consumed, so the bundle never sits in memory.
   */
  private suspend fun downloadToSink(
    sink: BufferedSink,
    onProgress: (Long, Long?) -> Unit,
    block: suspend HttpClient.() -> HttpStatement,
  ) {
    val statement = try {
      http.block()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      throw IterTransportException("gateway unreachable: ${e.message}", e)
    }
    statement.execute { response ->
      if (!response.status.isSuccess()) throw response.toApiException()
      val total = response.headers["Content-Length"]?.toLongOrNull()
      val channel = response.bodyAsChannel()
      val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)
      var read = 0L
      while (true) {
        val n = channel.readAvailable(buffer, 0, buffer.size)
        if (n == -1) break
        if (n > 0) {
          sink.write(buffer, 0, n)
          read += n
          onProgress(read, total)
        }
      }
      sink.flush()
    }
  }

  private companion object {
    const val DOWNLOAD_BUFFER_SIZE = 64 * 1024
    const val DOWNLOAD_TIMEOUT_MS = 15 * 60 * 1000L
  }
}

/** Correlation id for the `x-request-id` header: 16 lowercase hex chars. */
internal fun randomRequestId(): String {
  val chars = "0123456789abcdef"
  return buildString(16) { repeat(16) { append(chars[Random.nextInt(16)]) } }
}

private suspend fun HttpResponse.toApiException(): IterApiException {
  val contentType = headers["Content-Type"] ?: ""
  if (contentType.startsWith("application/json")) {
    val envelope = try {
      IterJson.decodeFromString<ApiErrorEnvelope>(bodyAsText())
    } catch (_: Exception) {
      null
    }
    if (envelope != null) {
      return IterApiException(status.value, envelope.error.code, envelope.error.message)
    }
  }
  return IterApiException(status.value, null, "HTTP ${status.value}")
}
