package it.iterapp.core.api

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import it.iterapp.core.repo.PlanRepository
import it.iterapp.core.repo.SearchRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

private fun clientWith(engine: MockEngine): IterGatewayClient =
  IterGatewayClient(iterHttpClient(engine)) { "http://gw.test" }

class IterGatewayClientTest {

  @Test
  fun healthDecodes() = runTest {
    val engine = MockEngine { request ->
      assertEquals("/health.json", request.url.encodedPath)
      respond(
        """{"status":"ok","version":"1.2.3","gtfsLoaded":"2026-07-01T00:00:00Z",
           "tilesBuiltAt":"2026-07-01T02:00:00Z","bootstrappedAt":"2026-07-01T02:10:00Z"}""",
        headers = headersOf(HttpHeaders.ContentType, "application/json"),
      )
    }
    val health = clientWith(engine).health()
    assertEquals("ok", health.status)
  }

  @Test
  fun envelopeErrorsBecomeTypedExceptions() = runTest {
    val engine = MockEngine {
      respond(
        """{"error":{"code":"AREA_TOO_LARGE","message":"bbox area exceeds cap"}}""",
        status = HttpStatusCode.PayloadTooLarge,
        headers = headersOf(HttpHeaders.ContentType, "application/json"),
      )
    }
    val e = assertFailsWith<IterApiException> {
      clientWith(engine).manifest()
    }
    assertEquals(413, e.status)
    assertEquals("AREA_TOO_LARGE", e.code)
  }

  @Test
  fun bareNotFoundHasNoCode() = runTest {
    val engine = MockEngine { respond("", status = HttpStatusCode.NotFound) }
    val e = assertFailsWith<IterApiException> { clientWith(engine).health() }
    assertEquals(404, e.status)
    assertNull(e.code)
    assertTrue(e.isBareNotFound)
  }

  @Test
  fun disabledStatsIsNullNotError() = runTest {
    val engine = MockEngine { respond("", status = HttpStatusCode.NotFound) }
    assertNull(clientWith(engine).stats())
  }

  @Test
  fun disabledTelemetryReturnsFalse() = runTest {
    val engine = MockEngine { respond("", status = HttpStatusCode.NotFound) }
    val accepted = clientWith(engine).sendTelemetry(
      it.iterapp.core.wire.TelemetryBatch(token = "a".repeat(16), samples = emptyList()),
    )
    assertEquals(false, accepted)
  }

  @Test
  fun geocodeParsesPhotonFeatures() = runTest {
    val engine = MockEngine { request ->
      assertEquals("colosseo", request.url.parameters["q"])
      respond(
        """{"type":"FeatureCollection","features":[
             {"type":"Feature","geometry":{"type":"Point","coordinates":[12.4922,41.8902]},
              "properties":{"osm_id":123,"osm_type":"W","osm_key":"tourism","osm_value":"attraction",
                            "type":"other","name":"Colosseo","city":"Roma","countrycode":"IT"}}]}""",
        headers = headersOf(HttpHeaders.ContentType, "application/json"),
      )
    }
    val results = SearchRepository(clientWith(engine)).search("colosseo")
    assertEquals(1, results.size)
    assertEquals("Colosseo", results[0].name)
    assertEquals(41.8902, results[0].point.lat)
    assertEquals("osm:W123", results[0].id)
  }

  @Test
  fun requestsCarryARequestId() = runTest {
    val engine = MockEngine { request ->
      val id = request.headers["x-request-id"]
      assertTrue(!id.isNullOrBlank() && id.length == 16, "expected minted x-request-id, got $id")
      respond("""{"type":"FeatureCollection","features":[]}""",
        headers = headersOf(HttpHeaders.ContentType, "application/json"))
    }
    clientWith(engine).geocode("x")
  }

  @Test
  fun geocodeToleratesSyntheticStringIds() = runTest {
    // Gateway-injected civici carry `ov…` string ids; stations carry `S\d+`.
    val engine = MockEngine {
      respond(
        """{"type":"FeatureCollection","features":[
             {"type":"Feature","geometry":{"type":"Point","coordinates":[12.5,41.9]},
              "properties":{"osm_id":"ov4f2a","osm_key":"place","osm_value":"house",
                            "type":"house","street":"Via Roma","housenumber":"12","city":"Roma"}},
             {"type":"Feature","geometry":{"type":"Point","coordinates":[12.501,41.901]},
              "properties":{"osm_id":"S08409","type":"station","name":"Roma Termini"}},
             {"type":"Feature","geometry":{"type":"Point","coordinates":[12.502,41.902]},
              "properties":{"osm_id":123456789,"osm_type":"N","type":"other","name":"Bar"}}]}""",
        headers = headersOf(HttpHeaders.ContentType, "application/json"),
      )
    }
    val results = SearchRepository(clientWith(engine)).search("via roma 12")
    assertEquals(3, results.size)
    assertEquals("Via Roma 12", results[0].name)
    assertNull(results[0].stationId)
    assertEquals("S08409", results[1].stationId)
    assertTrue(results[1].isTrainStation)
    assertEquals("osm:N123456789", results[2].id)
  }

  @Test
  fun placeImageUrlUsesQueryParameterEncoding() = runTest {
    val engine = MockEngine { respond("") }
    val client = clientWith(engine)
    val url = client.placeImageUrl("AT&T logo+more=x.svg")
    assertEquals(
      "http://gw.test/places/image?file=AT%26T%20logo%2Bmore%3Dx.svg&width=640",
      url,
    )
  }

  @Test
  fun reliabilityEncodesPathSegments() = runTest {
    val engine = MockEngine { request ->
      assertEquals("/reliability/70/0/70001", request.url.encodedPath)
      respond(
        """{"route":"70","direction":"0","stop":"70001","cells":[]}""",
        headers = headersOf(HttpHeaders.ContentType, "application/json"),
      )
    }
    val response = clientWith(engine).reliability("70", "0", "70001")
    assertTrue(response.cells.isEmpty())
  }
}

class PlanRepositoryTest {

  @Test
  fun planMapsItinerariesWithGatewayExtras() = runTest {
    val engine = MockEngine { request ->
      assertEquals("reliability", request.url.parameters["rerank"])
      assertEquals("historical", request.url.parameters["predict"])
      respond(
        """{"data":{"plan":{"itineraries":[
             {"startTime":1720260000000,"endTime":1720261980000,"duration":1980,
              "numberOfTransfers":1,"walkDistance":420.5,
              "reliabilityScore":0.95,"rerankScore":1.0,
              "rerankFactors":{"reliability":1.0,"transfers":0.0,"walk":0.0,"eco":0.0,
                               "weather":0.0,"traffic":0.0,"crowding":0.0},
              "predictedDelaySummary":{"annotatedLegs":1,"worstSeconds":180.0,"source":"historical"},
              "legs":[
                {"mode":"WALK","transitLeg":false,"startTime":1720260000000,"endTime":1720260300000,
                 "duration":300,"distance":400.0,
                 "from":{"name":"Origin","lat":41.9,"lon":12.5},"to":{"name":"Stop","lat":41.91,"lon":12.51}},
                {"mode":"BUS","transitLeg":true,"realTime":false,"startTime":1720260300000,
                 "endTime":1720261380000,"duration":1080,"distance":5200.0,
                 "route":{"gtfsId":"ATAC:70","shortName":"70","color":"E27439"},
                 "trip":{"directionId":"0","tripHeadsign":"Termini"},
                 "from":{"name":"Stop","lat":41.91,"lon":12.51,"stop":{"gtfsId":"ATAC:70001","name":"Stop"}},
                 "to":{"name":"Dest","lat":41.95,"lon":12.55},
                 "legGeometry":{"points":"_p~iF~ps|U_ulLnnqC","length":2},
                 "predictedDelay":{"seconds":180.0,"p50Seconds":45.0,"sampleCount":12,"source":"historical"}}
              ]}]}}}""",
        headers = headersOf(HttpHeaders.ContentType, "application/json"),
      )
    }
    val itineraries = PlanRepository(clientWith(engine)).plan(
      PlanParams(fromLat = 41.9, fromLon = 12.5, toLat = 41.95, toLon = 12.55),
      rerank = "reliability",
      predictHistorical = true,
    )
    assertEquals(1, itineraries.size)
    val itinerary = itineraries[0]
    assertEquals(0.95, itinerary.ranking?.reliabilityScore)
    assertEquals(180.0, itinerary.predictedWorstDelaySeconds)
    assertEquals(2, itinerary.legs.size)
    val bus = itinerary.legs[1]
    assertTrue(bus.isTransit)
    assertEquals("E27439", bus.routeColor)
    assertEquals(180.0, bus.predictedDelay?.p85Seconds)
    assertEquals(2, bus.geometry.size)
  }

  @Test
  fun planToleratesDecimalDurations() = runTest {
    // OTP's Leg.duration is a Float — decimal seconds must decode.
    val engine = MockEngine {
      respond(
        """{"data":{"plan":{"itineraries":[
             {"startTime":1,"endTime":2,"duration":1980.0,"numberOfTransfers":0,
              "walkDistance":10.5,
              "legs":[{"mode":"WALK","transitLeg":false,"startTime":1,"endTime":2,
                       "duration":300.5,"distance":400.0,
                       "from":{"name":"A","lat":1.0,"lon":1.0},
                       "to":{"name":"B","lat":1.1,"lon":1.1}}]}]}}}""",
        headers = headersOf(HttpHeaders.ContentType, "application/json"),
      )
    }
    val itineraries = PlanRepository(clientWith(engine)).plan(
      PlanParams(fromLat = 1.0, fromLon = 1.0, toLat = 1.1, toLon = 1.1),
    )
    assertEquals(1980, itineraries[0].durationSeconds)
    assertEquals(300, itineraries[0].legs[0].durationSeconds)
  }

  @Test
  fun graphQlErrorsSurfaceAsTypedFailure() = runTest {
    val engine = MockEngine {
      respond(
        """{"errors":[{"message":"ValidationError: unknown field"}]}""",
        headers = headersOf(HttpHeaders.ContentType, "application/json"),
      )
    }
    val e = assertFailsWith<IterApiException> {
      PlanRepository(clientWith(engine)).plan(
        PlanParams(fromLat = 0.0, fromLon = 0.0, toLat = 1.0, toLon = 1.0),
      )
    }
    assertEquals("GRAPHQL_ERROR", e.code)
  }

  @Test
  fun planQuerySelectsTransformKeyFields() {
    val request = buildPlanRequest(
      PlanParams(fromLat = 41.9, fromLon = 12.5, toLat = 41.95, toLon = 12.55, maxTransfers = 2),
    )
    val q = request.query
    for (field in listOf("transitLeg", "realTime", "directionId", "gtfsId", "startTime", "endTime")) {
      assertTrue(field in q, "plan query must select $field")
    }
    assertTrue("maxTransfers: 2" in q)
    val vars = request.variables.toString()
    assertTrue("WALK" in vars && "BUS" in vars)
  }
}
