package it.iterapp.core.settings

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IterSettingsTest {

  @Test
  fun onboardingDefaultsToUnseen() {
    val settings = IterSettings(MapSettings())
    assertFalse(settings.hasSeenOnboarding.value)
  }

  @Test
  fun onboardingSeenPersistsAndReloads() {
    val store = MapSettings()
    IterSettings(store).setOnboardingSeen(true)
    // A fresh instance over the same store reads the durable flag.
    assertTrue(IterSettings(store).hasSeenOnboarding.value)
  }

  @Test
  fun onboardingSeenUpdatesFlow() {
    val settings = IterSettings(MapSettings())
    assertFalse(settings.hasSeenOnboarding.value)
    settings.setOnboardingSeen(true)
    assertTrue(settings.hasSeenOnboarding.value)
    settings.setOnboardingSeen(false)
    assertFalse(settings.hasSeenOnboarding.value)
  }

  @Test
  fun mapModeRoundTripsThroughStore() {
    val store = MapSettings()
    IterSettings(store).setMapMode(MapMode.TRANSIT)
    assertEquals(MapMode.TRANSIT, IterSettings(store).mapMode.value)
  }
}
