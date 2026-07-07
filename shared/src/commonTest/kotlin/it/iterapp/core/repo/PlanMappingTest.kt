package it.iterapp.core.repo

import it.iterapp.core.wire.OtpLeg
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlanMappingTest {

  @Test
  fun legMapsBothLiveDelays() {
    val leg = OtpLeg(
      mode = "BUS",
      transitLeg = true,
      realTime = true,
      departureDelay = 120,
      arrivalDelay = 300,
    ).toDomain()
    assertTrue(leg.isRealTime)
    assertEquals(120, leg.departureDelaySeconds)
    assertEquals(300, leg.arrivalDelaySeconds)
  }

  @Test
  fun legDelaysDefaultToZeroWhenAbsent() {
    val leg = OtpLeg(mode = "WALK").toDomain()
    assertEquals(0, leg.departureDelaySeconds)
    assertEquals(0, leg.arrivalDelaySeconds)
  }
}
