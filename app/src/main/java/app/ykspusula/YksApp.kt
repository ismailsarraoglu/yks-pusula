package app.ykspusula

import android.app.Application
import app.ykspusula.data.AppDatabase
import app.ykspusula.data.BackupManager
import app.ykspusula.data.Repository
import app.ykspusula.notify.Notifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class YksApp : Application() {
    val db by lazy { AppDatabase.build(this) }
    val repo by lazy { Repository(this, db) }
    val backup by lazy { BackupManager(repo, filesDir) }
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val ready = MutableStateFlow(false)
    val startError = MutableStateFlow<String?>(null)

    override fun onCreate() {
        super.onCreate()
        Notifier.createChannels(this)
        scope.launch {
            try {
                repo.ensureSeeded()
                repo.finishExpiredExams()
                repo.settlePomodoro()
                ready.value = true
            } catch (e: Exception) {
                startError.value = e.message ?: "Bilinmeyen hata"
            }
        }
    }
}
