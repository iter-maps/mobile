package it.iterapp.app.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class OnLineColorTest {
  private val ink = Color(0xFF1B1D29)

  @Test
  fun midToneBackgroundsGetDarkInk() {
    // Tram fallback green and a common GTFS orange: white would sit ~3:1.
    assertEquals(ink, onLineColor(Color(0xFF7A9E4E)))
    assertEquals(ink, onLineColor(Color(0xFFF7803C)))
  }

  @Test
  fun deepLineColorsKeepWhite() {
    assertEquals(Color.White, onLineColor(Color(0xFF0570B5)))
    assertEquals(Color.White, onLineColor(Color(0xFF3E7CB1)))
    assertEquals(Color.White, onLineColor(Color(0xFF7B4EA3)))
  }

  @Test
  fun extremesAreStable() {
    assertEquals(ink, onLineColor(Color.White))
    assertEquals(Color.White, onLineColor(Color.Black))
  }
}
