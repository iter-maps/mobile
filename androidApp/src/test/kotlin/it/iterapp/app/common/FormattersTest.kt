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
    assertEquals(14 to 30, routerHourMinute(routerTimeTodayAt(14, 30)))
    assertEquals(0 to 5, routerHourMinute(routerTimeTodayAt(0, 5)))
  }

  @Test
  fun routerHourMinuteReadsRouterWallClock() {
    // 2026-07-07T12:00:00Z is 14:00 in Europe/Rome (CEST).
    assertEquals(14 to 0, routerHourMinute(1783425600000L))
  }
}
