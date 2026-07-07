package it.iterapp.app.search

import it.iterapp.core.model.GeoPoint
import it.iterapp.core.model.SearchResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SearchDistancesTest {

  private fun result(distanceM: Double? = null) = SearchResult(
    id = "osm:n1",
    name = "Duomo di Milano",
    detail = "Piazza del Duomo, Milano",
    point = GeoPoint(45.4641, 9.1919),
    osmKey = "tourism",
    osmValue = "attraction",
    layer = "house",
    distanceM = distanceM,
  )

  @Test
  fun nullOriginLeavesResultsUntouched() {
    val results = listOf(result())
    assertEquals(results, results.withDistancesFrom(null))
    assertNull(results.withDistancesFrom(null).single().distanceM)
  }

  @Test
  fun originFillsMissingDistances() {
    // Milano Centrale → Duomo is roughly 3 km.
    val origin = GeoPoint(45.4862, 9.2049)
    val d = listOf(result()).withDistancesFrom(origin).single().distanceM
    assertNotNull(d)
    assertTrue(d in 2000.0..4000.0, "expected ~3km, got $d")
  }

  @Test
  fun wireProvidedDistanceWins() {
    val origin = GeoPoint(45.4862, 9.2049)
    val d = listOf(result(distanceM = 42.0)).withDistancesFrom(origin).single().distanceM
    assertEquals(42.0, d)
  }
}
