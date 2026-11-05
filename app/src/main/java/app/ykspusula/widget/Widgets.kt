package app.ykspusula.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import app.ykspusula.MainActivity
import app.ykspusula.R
import app.ykspusula.YksApp
import app.ykspusula.data.startOf
import app.ykspusula.data.today
import app.ykspusula.domain.Scoring
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Ana ekran bileşenleri yerel veriden beslenir, internet kullanmaz. */
object Widgets {
    val PROVIDERS = listOf(CountdownWidget::class.java, GoalWidget::class.java, TasksWidget::class.java, FocusWidget::class.java)

    fun route(c: Context, route: String, code: Int): PendingIntent {
        val i = Intent(c, MainActivity::class.java).putExtra("route", route).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(c, code, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    /** Veri değişince çağrılır; art arda gelen değişiklikler tek güncellemede birleşir. */
    fun updateAll(c: Context) {
        val m = AppWidgetManager.getInstance(c)
        PROVIDERS.forEach { p ->
            val ids = m.getAppWidgetIds(ComponentName(c, p))
            if (ids.isNotEmpty()) c.sendBroadcast(Intent(c, p).setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids))
        }
    }

    suspend fun countdown(c: Context): RemoteViews {
        val repo = (c.applicationContext as YksApp).repo
        val cfg = repo.user.examConfig("TYT")
        val left = cfg?.let { ChronoUnit.DAYS.between(today(), LocalDate.parse(it.examDate)) }
        return RemoteViews(c.packageName, R.layout.widget_countdown).apply {
            setTextViewText(R.id.w_value, if (left == null) "–" else if (left >= 0) "$left" else "0")
            setTextViewText(R.id.w_label, if (left == null) "Sınav tarihi yok" else if (left >= 0) "gün kaldı" else "Sınav tarihi geçti")
            setTextViewText(R.id.w_detail, "${cfg?.examYear ?: ""} YKS")
            setOnClickPendingIntent(R.id.w_root, route(c, "home", 201))
        }
    }

    suspend fun goal(c: Context): RemoteViews {
        val repo = (c.applicationContext as YksApp).repo
        val p = repo.user.profileNow()
        val d = today()
        val minutes = repo.user.minutesBetweenNow(startOf(d), startOf(d.plusDays(1)))
        val goal = p?.dailyMinutes ?: 0
        val pct = Scoring.percent(minutes, goal).coerceAtMost(100)
        return RemoteViews(c.packageName, R.layout.widget_goal).apply {
            setTextViewText(R.id.w_value, "%$pct")
            setTextViewText(R.id.w_label, "$minutes / $goal dk")
            setProgressBar(R.id.w_progress, 100, pct, false)
            setOnClickPendingIntent(R.id.w_root, route(c, "home", 202))
        }
    }

    suspend fun tasks(c: Context): RemoteViews {
        val repo = (c.applicationContext as YksApp).repo
        val all = repo.user.tasksBetweenNow(today().toString(), today().toString())
        val open = all.filter { !it.done && !it.skipped }
        val rows = listOf(R.id.w_task1, R.id.w_task2, R.id.w_task3)
        return RemoteViews(c.packageName, R.layout.widget_tasks).apply {
            setTextViewText(R.id.w_label, "Bugünkü görevler • ${all.size - open.size}/${all.size}")
            rows.forEachIndexed { i, id ->
                val t = open.getOrNull(i)
                setTextViewText(id, t?.let { "${it.time}  ${it.title}" } ?: if (i == 0 && open.isEmpty()) (if (all.isEmpty()) "Bugün için görev yok" else "Bugünkü görevler tamam") else "")
            }
            setOnClickPendingIntent(R.id.w_root, route(c, "plan", 203))
        }
    }

    fun focus(c: Context): RemoteViews = RemoteViews(c.packageName, R.layout.widget_focus).apply {
        setOnClickPendingIntent(R.id.w_root, route(c, "pomodoro", 204))
    }
}

abstract class BaseWidget : AppWidgetProvider() {
    abstract suspend fun build(c: Context): RemoteViews

    override fun onUpdate(c: Context, m: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        (c.applicationContext as YksApp).scope.launch {
            try {
                (c.applicationContext as YksApp).ready.first { it }
                val v = build(c)
                ids.forEach { m.updateAppWidget(it, v) }
            } finally { pending.finish() }
        }
    }
}

class CountdownWidget : BaseWidget() { override suspend fun build(c: Context) = Widgets.countdown(c) }
class GoalWidget : BaseWidget() { override suspend fun build(c: Context) = Widgets.goal(c) }
class TasksWidget : BaseWidget() { override suspend fun build(c: Context) = Widgets.tasks(c) }
class FocusWidget : BaseWidget() { override suspend fun build(c: Context) = Widgets.focus(c) }
