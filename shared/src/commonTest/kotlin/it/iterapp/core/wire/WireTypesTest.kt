package it.iterapp.core.wire

import it.iterapp.core.api.IterJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WireTypesTest {

  @Test
  fun errorEnvelopeDecodes() {
    val json = """{"error":{"code":"BBOX_OUT_OF_RANGE","message":"bbox coordinates out of range"}}"""
    val envelope = IterJson.decodeFromString<ApiErrorEnvelope>(json)
    assertEquals("BBOX_OUT_OF_RANGE", envelope.error.code)
    assertNull(envelope.error.details)
  }

  @Test
  fun staticHealthKeepsNullTimestamps() {
    val json = """
      {"status":"degraded","version":"0.0.0","gtfsLoaded":"unknown",
       "tilesBuiltAt":null,"bootstrappedAt":null}
    """.trimIndent()
    val health = IterJson.decodeFromString<StaticHealth>(json)
    assertEquals("degraded", health.status)
    assertEquals("unknown", health.gtfsLoaded)
    assertNull(health.tilesBuiltAt)
    assertNull(health.bootstrappedAt)
  }

  @Test
  fun boardEntryOriginNullVsPlatformAbsent() {
    val json = """
      {"trainNumber":"REG 22815","category":"REG","origin":null,
       "destination":"Roma Termini","scheduledTime":"08:15","delayMinutes":-2}
    """.trimIndent()
    val entry = IterJson.decodeFromString<BoardEntry>(json)
    assertNull(entry.origin)
    assertNull(entry.platform)
    assertEquals(-2, entry.delayMinutes)
  }

  @Test
  fun stationCoordinatesOmittedNotZero() {
    val entry = IterJson.decodeFromString<Station>("""{"id":"S08409","name":"Roma Termini"}""")
    assertNull(entry.lat)
    assertNull(entry.lon)
  }

  @Test
  fun reliabilityCellGroupsOptionalFields() {
    val empty = IterJson.decodeFromString<ReliabilityCell>(
      """{"todBucket":"night","dayType":"weekday","sampleCount":0}""",
    )
    assertEquals(0, empty.sampleCount)
    assertNull(empty.onTimeRate)
    assertNull(empty.p85S)

    val full = IterJson.decodeFromString<ReliabilityCell>(
      """{"todBucket":"am-peak","dayType":"weekday","sampleCount":42,
          "onTimeRate":0.88,"p50S":30.0,"p85S":180.5,"p90S":240.0,"meanS":55.1}""",
    )
    assertEquals(42, full.sampleCount)
    assertEquals(180.5, full.p85S)
  }

  @Test
  fun placeToleratesUnknownAndOmittedFields() {
    val json = """
      {"id":"wd:Q10285","name":"Colosseo","category":{"primary":"place"},
       "location":{"lon":12.4922,"lat":41.8902},
       "summary":"An amphitheatre.","someFutureField":{"x":1}}
    """.trimIndent()
    val place = IterJson.decodeFromString<Place>(json)
    assertEquals("wd:Q10285", place.id)
    assertNull(place.image)
    assertNull(place.facets)
    assertEquals(41.8902, place.location.lat)
  }

  @Test
  fun offlineManifestDecodes() {
    val json = """
      {"generator":"iter-gateway/1.0","createdAt":"2026-07-06T10:00:00Z",
       "bbox":[12.40,41.84,12.60,41.98],"minzoom":0,"maxzoom":14,
       "pmtiles":"area.pmtiles","styles":["light","dark"],"glyphs":true,
       "sprite":true,"overlays":["metro-stations.geojson"],"note":"rewrite __BASE_URL__"}
    """.trimIndent()
    val manifest = IterJson.decodeFromString<OfflineManifest>(json)
    assertEquals(4, manifest.bbox.size)
    assertEquals("area.pmtiles", manifest.pmtiles)
    assertTrue(manifest.glyphs)
  }

  @Test
  fun telemetrySampleOmitsNullDirection() {
    val sample = TelemetrySample(
      routeId = "ATAC:MEA",
      stopId = "ATAC:70001",
      epochMs = 1_720_260_000_000,
      signal = "presence",
    )
    val encoded = IterJson.encodeToString(TelemetrySample.serializer(), sample)
    assertTrue("directionId" !in encoded, "absent directionId must be omitted, got: $encoded")
  }
}
