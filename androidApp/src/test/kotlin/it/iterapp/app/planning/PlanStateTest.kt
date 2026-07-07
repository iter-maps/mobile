package it.iterapp.app.planning

import it.iterapp.core.model.Itinerary
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertSame

class PlanStateTest {

  private val itineraries = mutableListOf<Itinerary>()

  @Test
  fun keepsLiveResults() {
    assertSame(itineraries, keptItineraries(PlanState.Results(itineraries)))
  }

  @Test
  fun inheritsTheListAnInFlightReplanIsKeeping() {
    // Second keepCurrent replan while the first is still loading.
    assertSame(itineraries, keptItineraries(PlanState.Loading(previous = itineraries)))
  }

  @Test
  fun keepsNothingOtherwise() {
    assertNull(keptItineraries(PlanState.Idle))
    assertNull(keptItineraries(PlanState.Loading(previous = null)))
    assertNull(keptItineraries(PlanState.Error(network = true)))
  }
}
