package it.iterapp.app.common

import kotlin.test.Test
import kotlin.test.assertEquals

class FormattersTest {

  @Test
  fun durationsCompactBelowAnHour() {
    assertEquals("7 min", formatDuration(7 * 60))
    assertEquals("59 min", formatDuration(59 * 60 + 30))
    assertEquals("1 h 12 min", formatDuration(72 * 60))
  }

  @Test
  fun distancesRoundSensibly() {
    assertEquals("350 m", formatDistance(347.0))
    assert(formatDistance(2400.0).endsWith("km"))
  }

  @Test
  fun delaysSwitchUnitAt90Seconds() {
    assertEquals("45 s", formatDelay(45.0))
    assertEquals("3 min", formatDelay(180.0))
  }

  @Test
  fun routerTimeRoundTripsHourAndMinute() {
    // A day roll preserves the router wall-clock hour and minute.
    assertEquals(14 to 30, routerHourMinute(routerTimeNextAt(14, 30)))
    assertEquals(0 to 5, routerHourMinute(routerTimeNextAt(0, 5)))
  }

  @Test
  fun routerTimeNextAtNeverLandsInThePast() {
    val (h, m) = routerHourMinute(System.currentTimeMillis() - 60 * 60_000L)
    assert(routerTimeNextAt(h, m) >= System.currentTimeMillis() - 5 * 60_000L)
  }

  @Test
  fun routerHourMinuteReadsRouterWallClock() {
    // 2026-07-07T12:00:00Z is 14:00 in Europe/Rome (CEST).
    assertEquals(14 to 0, routerHourMinute(1783425600000L))
  }
}
