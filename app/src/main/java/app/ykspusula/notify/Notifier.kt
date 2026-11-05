package app.ykspusula.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.ykspusula.MainActivity
import app.ykspusula.R
import app.ykspusula.YksApp
import app.ykspusula.data.Reminder
import app.ykspusula.data.today
import app.ykspusula.domain.ReminderTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class ReminderType(val type: String, val title: String, val detail: String, val weekly: Boolean)

val REMINDER_TYPES = listOf(
    ReminderType("DAILY", "Günlük çalışma saati", "Her gün seçtiğin saatte hatırlatır.", false),
    ReminderType("REVIEW", "Konu tekrar zamanı", "Yalnızca tekrarı gelmiş konu veya yanlış varsa gönderilir.", false),
    ReminderType("EXAM", "Deneme günü", "Her hafta seçtiğin gün hatırlatır.", true),
    ReminderType("COUNTDOWN", "Sınava kalan gün", "Her hafta seçtiğin gün kalan süreyi bildirir.", true),
    ReminderType("CARDS", "Kart tekrarı", "Yalnızca tekrarı gelmiş kart varsa gönderilir.", false),
    ReminderType("WEEKLY", "Haftalık değerlendirme", "Haftanın özetine bakman için hatırlatır.", true),
)

object Notifier {
    private const val CH_REMINDER = "reminders"
    private const val CH_TIMER = "timer"
    private const val ACTION = "app.ykspusula.REMINDER"
    private const val POMODORO_CODE = 9000

    fun createChannels(c: Context) {
        val m = c.getSystemService(NotificationManager::class.java)
        m.createNotificationChannel(NotificationChannel(CH_REMINDER, "Hatırlatıcılar", NotificationManager.IMPORTANCE_DEFAULT))
        m.createNotificationChannel(NotificationChannel(CH_TIMER, "Odak sayacı", NotificationManager.IMPORTANCE_HIGH))
    }

    fun canNotify(c: Context) = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun pending(c: Context, type: String): PendingIntent {
        val i = Intent(c, ReminderReceiver::class.java).setAction(ACTION).putExtra("type", type)
        val code = if (type == "POMODORO") POMODORO_CODE else REMINDER_TYPES.indexOfFirst { it.type == type } + 1
        return PendingIntent.getBroadcast(c, code, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    /** Tam zamanlı alarm izni gerektirmez; Android cihazın uygun anında çalıştırır. */
    private fun alarm(c: Context, at: Long, pi: PendingIntent) {
        c.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
    }

    fun schedule(c: Context, r: Reminder) {
        val pi = pending(c, r.type)
        if (!r.enabled) {
            c.getSystemService(AlarmManager::class.java).cancel(pi)
            return
        }
        val weekly = REMINDER_TYPES.first { it.type == r.type }.weekly
        alarm(c, ReminderTime.next(System.currentTimeMillis(), r.hour, r.minute, if (weekly) r.dayOfWeek else null), pi)
    }

    fun schedulePomodoro(c: Context, at: Long) = alarm(c, at, pending(c, "POMODORO"))
    fun cancelPomodoro(c: Context) = c.getSystemService(AlarmManager::class.java).cancel(pending(c, "POMODORO"))

    fun show(c: Context, id: Int, title: String, text: String, channel: String = CH_REMINDER) {
        if (!canNotify(c)) return
        val open = PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(c, channel)
            .setSmallIcon(R.drawable.ic_notify).setContentTitle(title).setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open).setAutoCancel(true).build()
        try { NotificationManagerCompat.from(c).notify(id, n) } catch (_: SecurityException) { }
    }

    suspend fun fire(c: Context, type: String) {
        val app = c.applicationContext as YksApp
        val repo = app.repo
        val now = System.currentTimeMillis()
        val code = REMINDER_TYPES.indexOfFirst { it.type == type } + 100
        when (type) {
            "POMODORO" -> {
                val s = repo.pomodoro.first()
                repo.settlePomodoro(now)
                if (s.running) {
                    val work = s.phase == "WORK"
                    show(c, 99, if (work) "Çalışma tamamlandı" else "Mola bitti", if (work) "${s.workMin} dakikalık odak oturumu kaydedildi." else "Yeni bir odak oturumuna hazırsın.", CH_TIMER)
                }
                return
            }
            "DAILY" -> show(c, code, "Çalışma zamanı", "Bugünkü programına göz atabilirsin.")
            "REVIEW" -> {
                val wrongs = repo.user.wrongDueNow(now)
                val topics = repo.user.topicsDue(now).first()
                if (wrongs + topics > 0) show(c, code, "Tekrar zamanı", "Tekrarı gelen $topics konu ve $wrongs yanlış var.")
            }
            "EXAM" -> show(c, code, "Deneme günü", "Bugün için planladığın denemeyi çözebilirsin.")
            "COUNTDOWN" -> {
                val date = repo.user.examConfig("TYT")?.examDate ?: return
                val left = ChronoUnit.DAYS.between(today(), LocalDate.parse(date))
                if (left >= 0) show(c, code, "Sınava $left gün", "TYT tarihi: $date")
            }
            "CARDS" -> {
                val n = repo.user.cardsDueCountNow(now)
                if (n > 0) show(c, code, "Kart tekrarı", "Tekrarı gelen $n kartın var.")
            }
            "WEEKLY" -> show(c, code, "Haftalık değerlendirme", "Bu haftanın istatistiklerine bakabilirsin.")
        }
        repo.user.remindersNow().firstOrNull { it.type == type }?.let { schedule(c, it) }
    }

    suspend fun rescheduleAll(c: Context) {
        val app = c.applicationContext as YksApp
        app.repo.user.remindersNow().forEach { schedule(c, it) }
        val p = app.repo.pomodoro.first()
        if (p.running) schedulePomodoro(c, p.endsAt)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, intent: Intent) {
        val type = intent.getStringExtra("type") ?: return
        val pending = goAsync()
        (c.applicationContext as YksApp).scope.launch {
            try { Notifier.fire(c, type) } finally { pending.finish() }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, intent: Intent) {
        val pending = goAsync()
        (c.applicationContext as YksApp).scope.launch {
            try { Notifier.rescheduleAll(c) } finally { pending.finish() }
        }
    }
}
