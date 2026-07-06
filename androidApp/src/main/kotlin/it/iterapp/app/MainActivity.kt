package it.iterapp.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.iterapp.app.home.HomeScreen
import it.iterapp.app.ui.theme.IterTheme
import it.iterapp.core.settings.IterSettings
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

  private val settings: IterSettings by inject()

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
    setContent {
      val themeMode by settings.themeMode.collectAsStateWithLifecycle()
      val dynamicColor by settings.dynamicColor.collectAsStateWithLifecycle()
      IterTheme(themeMode = themeMode, dynamicColor = dynamicColor) {
        HomeScreen()
      }
    }
  }
}
