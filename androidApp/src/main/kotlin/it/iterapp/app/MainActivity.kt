package it.iterapp.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.iterapp.app.home.HomeScreen
import it.iterapp.app.ui.theme.IterTheme
import it.iterapp.core.settings.IterSettings
import it.iterapp.core.settings.ThemeMode
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

  private val settings: IterSettings by inject()

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
    setContent {
      val themeMode by settings.themeMode.collectAsStateWithLifecycle()
      val dynamicColor by settings.dynamicColor.collectAsStateWithLifecycle()
      val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
      }
      // System bar icon contrast must follow the in-app theme, not just the
      // system one — an in-app dark theme over a light wallpaper otherwise
      // renders dark icons on the dark map.
      DisposableEffect(darkTheme) {
        enableEdgeToEdge(
          statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
          navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
        )
        onDispose {}
      }
      IterTheme(themeMode = themeMode, dynamicColor = dynamicColor) {
        HomeScreen()
      }
    }
  }
}
