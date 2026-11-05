package app.ykspusula

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ykspusula.data.AppSettings
import app.ykspusula.ui.AppRoot
import app.ykspusula.ui.theme.YksTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val app = LocalContext.current.applicationContext as YksApp
            val settings by app.repo.settings.collectAsStateWithLifecycle(AppSettings())
            YksTheme(settings.theme, settings.fontScale) { AppRoot() }
        }
    }
}
