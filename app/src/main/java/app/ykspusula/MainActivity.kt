package app.ykspusula

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ykspusula.data.AppSettings
import app.ykspusula.ui.AppRoot
import app.ykspusula.ui.theme.YksTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val app = LocalContext.current.applicationContext as YksApp
            val settings by app.repo.settings.collectAsStateWithLifecycle(AppSettings())
            val systemDark = isSystemInDarkTheme()
            val dark = when (settings.theme) { "DARK" -> true; "LIGHT" -> false; else -> systemDark }
            DisposableEffect(dark) {
                val clear = Color.Transparent.toArgb()
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(clear, clear) { dark },
                    navigationBarStyle = SystemBarStyle.auto(clear, clear) { dark },
                )
                onDispose { }
            }
            YksTheme(settings.theme, settings.fontScale) { AppRoot() }
        }
    }
}
