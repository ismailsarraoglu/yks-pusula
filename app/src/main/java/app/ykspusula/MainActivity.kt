package app.ykspusula

import android.content.Intent
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
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val yks get() = application as YksApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handle(intent)
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

    override fun onStart() {
        super.onStart()
        yks.monetize.onActivityStart(this)
    }

    /** Uygulama kapalıyken tamamlanan bekleyen ödemeler de burada yakalanır. */
    override fun onResume() {
        super.onResume()
        yks.scope.launch { yks.monetize.billing.queryPurchases() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handle(intent)
    }

    private fun handle(i: Intent?) {
        i?.getStringExtra("route")?.let { yks.pendingRoute.value = it }
        i?.removeExtra("route")
    }
}
