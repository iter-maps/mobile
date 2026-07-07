package it.iterapp.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import it.iterapp.core.model.LegMode

/** Seed for the fallback tonal palette (docs/design/tokens.md). */
val BrandSeed = Color(0xFF888FFA)

/** brand.ink (docs/design/tokens.md) — badge ink, map identity accents. */
val BrandInk = Color(0xFF4248C9)

/**
 * Transit identity colors — semantic, palette-independent (ADR 0009). A GTFS
 * `route_color` always wins; these are category fallbacks.
 */
object LineColors {
  val Metro = Color(0xFF0570B5)
  val Tram = Color(0xFF7A9E4E)
  val Bus = Color(0xFF3E7CB1)
  val Rail = Color(0xFF7B4EA3)
  val Night = Color(0xFF3F4358)
  val Walk = Color(0xFF5F6368)

  fun forMode(mode: LegMode): Color = when (mode) {
    LegMode.WALK, LegMode.BICYCLE, LegMode.CAR, LegMode.OTHER -> Walk
    LegMode.SUBWAY -> Metro
    LegMode.TRAM -> Tram
    LegMode.BUS -> Bus
    LegMode.RAIL, LegMode.FERRY, LegMode.FUNICULAR, LegMode.GONDOLA -> Rail
  }
}

/** Delay coloring for boards and legs. */
object DelayColors {
  val OnTime = Color(0xFF2E9E63)
  val Minor = Color(0xFFC98A2B)
  val Severe = Color(0xFFC94242)
  val Early = Color(0xFF3E7CB1)

  fun forMinutes(delayMinutes: Int): Color = when {
    delayMinutes < 0 -> Early
    delayMinutes <= 2 -> OnTime
    delayMinutes <= 10 -> Minor
    else -> Severe
  }
}

/** Parses a GTFS hex color (`RRGGBB`, with or without `#`); null when invalid. */
fun parseGtfsColor(hex: String?): Color? {
  val cleaned = hex?.trim()?.removePrefix("#") ?: return null
  if (cleaned.length != 6) return null
  return cleaned.toLongOrNull(16)?.let { Color(0xFF000000 or it) }
}

/** Resolves a leg's line color: GTFS color first, category fallback otherwise. */
fun lineColor(routeColor: String?, mode: LegMode): Color =
  parseGtfsColor(routeColor) ?: LineColors.forMode(mode)

/** Readable text color on a line badge, by luminance — not by theme. */
fun onLineColor(background: Color): Color =
  if (background.luminance() > 0.5f) Color(0xFF1B1D29) else Color.White
