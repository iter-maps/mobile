package it.iterapp.app.sheet

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SheetNavigatorTest {

  @Test
  fun startsAtHomeAndCannotPop() {
    val nav = SheetNavigator()
    assertEquals(SheetPage.Home, nav.current)
    assertFalse(nav.canPop)
    assertFalse(nav.pop())
  }

  @Test
  fun pushPopRoundTrip() {
    val nav = SheetNavigator()
    nav.push(SheetPage.Search)
    nav.push(SheetPage.Planning)
    assertEquals(SheetPage.Planning, nav.current)
    assertFalse(nav.lastWasPop)
    assertTrue(nav.pop())
    assertEquals(SheetPage.Search, nav.current)
    assertTrue(nav.lastWasPop)
  }

  @Test
  fun duplicateTopPushIsIgnored() {
    val nav = SheetNavigator()
    nav.push(SheetPage.Search)
    nav.push(SheetPage.Search)
    assertTrue(nav.pop())
    assertEquals(SheetPage.Home, nav.current)
    assertFalse(nav.canPop)
  }

  @Test
  fun transientPageSkipsHistory() {
    val nav = SheetNavigator()
    nav.push(SheetPage.MapLayers)
    assertEquals(SheetPage.MapLayers, nav.current)
    // Navigating elsewhere replaces the transient page in the stack.
    nav.push(SheetPage.Settings)
    assertTrue(nav.pop())
    assertEquals(SheetPage.Home, nav.current)
  }

  @Test
  fun resetToHomeClearsEverything() {
    val nav = SheetNavigator()
    nav.push(SheetPage.Search)
    nav.push(SheetPage.TrainBoard("roma"))
    nav.resetToHome()
    assertEquals(SheetPage.Home, nav.current)
    assertFalse(nav.canPop)
    assertTrue(nav.lastWasPop)
  }
}
