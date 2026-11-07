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
    /** Yarıda kalan geri yükleme açılışta geri alındıysa true; Yedekleme ekranı kullanıcıya bildirir. */
    val recoveredRestore = MutableStateFlow(false)
    val startError = MutableStateFlow<String?>(null)
    /** Bildirim veya ana ekran bileşeninden gelen hedef ekran. */
    val pendingRoute = MutableStateFlow<String?>(null)
    private val dataChanges = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    /** Ekranların kullandığı gün. Gece yarısı, saat veya saat dilimi değişince ve uygulama öne gelince yenilenir. */
    val day = MutableStateFlow(app.ykspusula.data.today())

    fun refreshDay() {
        val d = app.ykspusula.data.today()
        if (d != day.value) {
            day.value = d
            Widgets.updateAll(this)
        }
    }

    @OptIn(FlowPreview::class)
    override fun onCreate() {
        super.onCreate()
        Notifier.createChannels(this)
        scope.launch {
            try {
                repo.ensureSeeded()
                // Önceki açılışta yarıda kalan bir geri yükleme varsa veriler önceki hâline döndürülür.
                runCatching { if (backup.recoverIfInterrupted()) recoveredRestore.value = true }
                repo.settlePomodoro()
                ready.value = true
                Widgets.updateAll(this@YksApp)
            } catch (e: Exception) {
                startError.value = "Veriler açılamadı. Uygulamayı yeniden başlatmayı dene. Sorun sürerse güncel sürümü kur. (${e.javaClass.simpleName})"
            }
        }
        repo.onDataChanged = { dataChanges.tryEmit(Unit) }
        // Uygulama gece açık kalırsa gün değişimi kaçırılmasın.
        scope.launch {
            while (true) {
                val z = java.time.ZoneId.systemDefault()
                val next = java.time.LocalDate.now(z).plusDays(1).atStartOfDay(z).toInstant().toEpochMilli()
                kotlinx.coroutines.delay((next - System.currentTimeMillis()).coerceIn(1_000L, 3_600_000L) + 500)
                refreshDay()
            }
        }
        scope.launch { dataChanges.debounce(1500).collect { Widgets.updateAll(this@YksApp) } }
    }
}
