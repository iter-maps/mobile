package it.iterapp.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import it.iterapp.core.settings.ThemeMode

/**
 * Material You when available and enabled (ADR 0009); otherwise a full tonal
 * scheme derived from the brand seed. Transit line/status colors are semantic
 * tokens in Color.kt, deliberately outside this scheme.
 *
 * Dark keeps the original TonalSpot look off [BrandSeed] (the reviewed,
 * liked scheme). Light re-anchors off the darker [BrandInk] with a Vibrant
 * ramp and positive contrast so the primary reads branded rather than a pale
 * indigo, and the surface-container ladder actually separates by elevation
 * instead of bunching near white (ADR 0014).
 */
@Composable
fun IterTheme(
  themeMode: ThemeMode = ThemeMode.SYSTEM,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val darkTheme = when (themeMode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
  }
  val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    else -> dynamicColorScheme(
      seedColor = if (darkTheme) BrandSeed else BrandInk,
      isDark = darkTheme,
      isAmoled = false,
      style = if (darkTheme) PaletteStyle.TonalSpot else PaletteStyle.Vibrant,
      contrastLevel = if (darkTheme) 0.0 else 0.3,
    )
  }
  MaterialTheme(
    colorScheme = colorScheme,
    typography = IterTypography,
    shapes = IterShapes,
    content = content,
  )
}
