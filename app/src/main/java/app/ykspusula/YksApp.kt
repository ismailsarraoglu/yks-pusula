package app.ykspusula

import android.app.Application
import app.ykspusula.data.AppDatabase
import app.ykspusula.data.BackupManager
import app.ykspusula.data.Repository
import app.ykspusula.monetize.Monetize
import app.ykspusula.notify.Notifier
import app.ykspusula.widget.Widgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

class YksApp : Application() {
    val db by lazy { AppDatabase.build(this) }
    val repo by lazy { Repository(this, db) }
    val backup by lazy { BackupManager(repo, filesDir, cacheDir) }
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val monetize by lazy { Monetize(this, scope) }
    val ready = MutableStateFlow(false)
    val startError = MutableStateFlow<String?>(null)
    /** Bildirim veya ana ekran bileşeninden gelen hedef ekran. */
    val pendingRoute = MutableStateFlow<String?>(null)
    private val dataChanges = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    @OptIn(FlowPreview::class)
    override fun onCreate() {
        super.onCreate()
        Notifier.createChannels(this)
        scope.launch {
            try {
                repo.ensureSeeded()
                repo.settlePomodoro()
                ready.value = true
                Widgets.updateAll(this@YksApp)
            } catch (e: Exception) {
                startError.value = "Veriler açılamadı. Uygulamayı yeniden başlatmayı dene. Sorun sürerse güncel sürümü kur. (${e.javaClass.simpleName})"
            }
        }
        repo.onDataChanged = { dataChanges.tryEmit(Unit) }
        scope.launch { dataChanges.debounce(1500).collect { Widgets.updateAll(this@YksApp) } }
    }
}
