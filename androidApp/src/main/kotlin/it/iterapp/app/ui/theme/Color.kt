package it.iterapp.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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

  /** Walk polyline on dark map tiles — [Walk] sits at ~2.5:1 there. */
  val WalkOnDark = Color(0xFFB4BAC6)

  fun forMode(mode: LegMode): Color = when (mode) {
    LegMode.WALK, LegMode.BICYCLE, LegMode.CAR, LegMode.OTHER -> Walk
    LegMode.SUBWAY -> Metro
    LegMode.TRAM -> Tram
    LegMode.BUS -> Bus
    LegMode.RAIL, LegMode.FERRY, LegMode.FUNICULAR, LegMode.GONDOLA -> Rail
  }
}

/**
 * Delay coloring for boards and legs — the dark-surface set. UI code should
 * go through the composable `delay*Color()` accessors below, which switch to
 * a darker set on light surfaces where these fail small-text contrast.
 */
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

/** Light-surface twins of [DelayColors], deepened to hold ≥4.5:1 at 11sp. */
private object DelayColorsLight {
  val OnTime = Color(0xFF1F7A4A)
  val Minor = Color(0xFF8F5B00)
  val Severe = Color(0xFFC94242)
  val Early = Color(0xFF2E6491)
}

@Composable
private fun onLightSurface(): Boolean = MaterialTheme.colorScheme.surface.luminance() > 0.5f

@Composable
fun delayOnTimeColor(): Color =
  if (onLightSurface()) DelayColorsLight.OnTime else DelayColors.OnTime

@Composable
fun delayMinorColor(): Color =
  if (onLightSurface()) DelayColorsLight.Minor else DelayColors.Minor

@Composable
fun delaySevereColor(): Color =
  if (onLightSurface()) DelayColorsLight.Severe else DelayColors.Severe

@Composable
fun delayEarlyColor(): Color =
  if (onLightSurface()) DelayColorsLight.Early else DelayColors.Early

/** Theme-aware delay ink with [DelayColors.forMinutes]'s thresholds. */
@Composable
fun delayColor(delayMinutes: Int): Color = when {
  delayMinutes < 0 -> delayEarlyColor()
  delayMinutes <= 2 -> delayOnTimeColor()
  delayMinutes <= 10 -> delayMinorColor()
  else -> delaySevereColor()
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

/**
 * Readable text color on a line badge — whichever of white or the dark ink
 * has the higher WCAG contrast (the crossover sits at luminance ~0.21, not
 * 0.5: mid-tone greens and GTFS oranges need ink, not white).
 */
fun onLineColor(background: Color): Color {
  val l = background.luminance()
  val whiteContrast = 1.05f / (l + 0.05f)
  val inkContrast = (l + 0.05f) / 0.0627f // luminance of 0xFF1B1D29 + 0.05
  return if (inkContrast >= whiteContrast) Color(0xFF1B1D29) else Color.White
}
