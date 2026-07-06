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
      seedColor = BrandSeed,
      isDark = darkTheme,
      isAmoled = false,
      style = PaletteStyle.TonalSpot,
    )
  }
  MaterialTheme(
    colorScheme = colorScheme,
    typography = IterTypography,
    shapes = IterShapes,
    content = content,
  )
}
