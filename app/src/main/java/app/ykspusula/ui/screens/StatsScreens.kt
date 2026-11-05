package app.ykspusula.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ykspusula.data.*
import app.ykspusula.domain.*
import app.ykspusula.ui.LocalNav
import app.ykspusula.ui.components.*
import app.ykspusula.ui.repo
import app.ykspusula.ui.theme.Yks
import java.time.format.DateTimeFormatter
import java.util.Locale

private val SHORT = DateTimeFormatter.ofPattern("d MMM", Locale("tr", "TR"))

@Composable
fun StatsScreen() {
    val r = repo()
    val nav = LocalNav.current
    val date = remember { today() }
    val since = remember { startOf(date.minusDays(120)) }
    val minutesTotal by r.user.minutesTotal().collectAsStateWithLifecycle(0)
    val solved by r.user.solvedTotal().collectAsStateWithLifecycle(0)
    val mocks by r.user.mockSummaries().collectAsStateWithLifecycle(emptyList())
    val completed by r.user.completedTopics().collectAsStateWithLifecycle(0)
    val active by r.user.activeDays().collectAsStateWithLifecycle(emptyList())
    val perDayMin by remember { r.user.minutesPerDay(since) }.collectAsStateWithLifecycle(emptyList())
    val perDayQ by remember { r.user.questionsPerDay(since) }.collectAsStateWithLifecycle(emptyList())
    val subjectStats by r.user.subjectStats().collectAsStateWithLifecycle(emptyList())
    val weak by r.user.weakTopics(3, 8).collectAsStateWithLifecycle(emptyList())
    val strongest by r.user.weakTopics(1, 500).collectAsStateWithLifecycle(emptyList())
    val reasons by r.user.wrongReasons().collectAsStateWithLifecycle(emptyList())
    val streak = Streaks.current(active, date)
    val minMap = perDayMin.associate { it.day to it.value }
    val qMap = perDayQ.associate { it.day to it.value }
    val rights = subjectStats.sumOf { it.right }
    val attempts = subjectStats.sumOf { it.total }

    var compare by remember { mutableStateOf<List<Repository.PeriodCompare>>(emptyList()) }
    LaunchedEffect(solved, minutesTotal) { compare = listOf(7, 30, 90).map { r.comparePeriod(it) } }
    val hasData = solved > 0 || minutesTotal > 0 || mocks.isNotEmpty()

    ScrollColumn(Modifier.statusBarsPadding()) {
        Text("İstatistik", style = MaterialTheme.typography.headlineMedium, color = Yks.c.text)
        if (!hasData) {
            Empty(Icons.Rounded.BarChart, "Henüz veri yok", "Soru çözdükçe, oturum tamamladıkça ve deneme ekledikçe grafiklerin burada oluşur.", "Soru çöz") { nav.navigate("bank") }
            return@ScrollColumn
        }
        Row2({ Tile("${minutesTotal / 60} sa ${minutesTotal % 60} dk", "Toplam çalışma", it) }, { Tile("$solved", "Çözülen soru", it) })
        Row2({ Tile("${mocks.size}", "Deneme", it) }, { Tile(Scoring.formatNet(mocks.sumOf { m -> m.net }), "Toplam net", it) })
        Row2({ Tile("$completed", "Tamamlanan konu", it) }, { Tile("$streak gün", "Çalışma serisi (en uzun: ${Streaks.longest(active)})", it) })
        val wk = compare.firstOrNull { it.days == 7 }
        val mo = compare.firstOrNull { it.days == 30 }
        Row2({ Tile("%${wk?.success ?: 0}", "Haftalık başarı", it) }, { Tile("%${mo?.success ?: 0}", "Aylık başarı", it) })

        Section("Başarı analizi")
        GroupCard {
            compare.forEachIndexed { i, c ->
                if (i > 0) Line()
                val diff = c.success - c.prevSuccess
                Row(Modifier.fillMaxWidth().padding(Space.l), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Son ${c.days} gün", style = MaterialTheme.typography.bodyLarge, color = Yks.c.text)
                        Muted("${c.solved} soru • ${c.minutes} dk • %${c.success} başarı")
                    }
                    if (c.solved > 0 && c.prevSolved > 0) {
                        val up = diff >= 0
                        Icon(if (up) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown, null, tint = if (up) Yks.c.success else Yks.c.error, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (up) "+$diff puan gelişme" else "$diff puan gerileme", style = MaterialTheme.typography.labelMedium, color = if (up) Yks.c.success else Yks.c.error)
                    } else Muted("Karşılaştırma yok")
                }
            }
        }

        var period by rememberSaveable { mutableStateOf("7") }
        Section("Çalışma")
        PillGroup(listOf("7" to "7 gün", "30" to "30 gün"), period) { period = it }
        val n = period.toInt()
        val days = (n - 1 downTo 0).map { date.minusDays(it.toLong()) }
        val every = if (n > 10) 7 else 1
        val lbl = days.map { if (n > 10) it.format(SHORT) else it.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale("tr", "TR")) }
        BarChart("Günlük çalışma süresi", lbl, days.map { (minMap[it.toString()] ?: 0).toDouble() }, "dakika", labelEvery = every)
        val weeks = (7 downTo 0).map { weekStart(date).minusWeeks(it.toLong()) }
        BarChart("Haftalık soru çözümü", weeks.map { it.format(SHORT) }, weeks.map { w -> (0..6).sumOf { d -> qMap[w.plusDays(d.toLong()).toString()] ?: 0 }.toDouble() }, "soru", labelEvery = 2)
        val months = (3 downTo 0).map { date.withDayOfMonth(1).minusMonths(it.toLong()) }
        BarChart("Aylık çalışma", months.map { it.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale("tr", "TR")) }, months.map { m ->
            minMap.filterKeys { it.startsWith(m.toString().take(7)) }.values.sum().toDouble()
        }, "dakika")
        HabitCalendar("Çalışma alışkanlığı takvimi", (83 downTo 0).map { date.minusDays(it.toLong()) }, minMap.toMutableMap().also { map -> qMap.forEach { (k, v) -> map[k] = (map[k] ?: 0) + v } })

        Section("Netler")
        val fmt: (Double) -> String = { Scoring.formatNet(it) }
        listOf("TYT", "AYT").forEach { e ->
            val list = mocks.filter { it.exam == e }
            if (list.isNotEmpty()) LineChart("$e net değişimi", list.map { app.ykspusula.ui.shortDate(it.createdAt) }, list.map { it.net }, fmt)
            else YCard { Text("$e net değişimi", style = MaterialTheme.typography.titleMedium, color = Yks.c.text); Muted("$e denemesi eklediğinde grafik oluşur.") }
        }
        if (mocks.size >= 2) {
            val last = mocks.takeLast(6)
            BarChart("Deneme karşılaştırması", last.indices.map { "${it + 1}" }, last.map { it.net.coerceAtLeast(0.0) }, "net")
            Muted(last.mapIndexed { i, m -> "${i + 1}: ${m.title}" }.joinToString("\n"))
        }

        Section("Başarı")
        if (subjectStats.isNotEmpty()) {
            PercentBars("Derslere göre başarı", subjectStats.map { Triple(it.key, Scoring.percent(it.right, it.total), "${it.right} doğru / ${it.total} soru") })
        }
        if (strongest.isNotEmpty()) {
            PercentBars("Konulara göre başarı", strongest.sortedByDescending { it.total }.take(10).map { Triple(it.name, Scoring.percent(it.right, it.total), "${it.subjectName} • ${it.total} soru") })
        }
        val blank = mocks.sumOf { it.blank }
        val mockRight = mocks.sumOf { it.correct }
        val mockWrong = mocks.sumOf { it.wrong }
        val extraRight = (rights - 0).coerceAtLeast(0)
        val extraWrong = (attempts - rights).coerceAtLeast(0)
        StackedBar(
            "Doğru, yanlış ve boş",
            listOf(
                Triple("Doğru", maxOf(extraRight, mockRight), Yks.c.success),
                Triple("Yanlış", maxOf(extraWrong, mockWrong), Yks.c.error),
                Triple("Boş (denemeler)", blank, Yks.c.muted),
            ),
        )
        Section("Zayıf konular")
        if (weak.isEmpty()) YCard { Muted("Bir konunun listelenmesi için en az 3 soru gerekir.") }
        else GroupCard {
            weak.forEachIndexed { i, w ->
                if (i > 0) Line()
                NavRow(w.name, "${w.subjectName} • %${Scoring.percent(w.right, w.total)} başarı • ${w.total} soru", onClick = { nav.navigate("topic/${w.topicId}") })
            }
        }
        if (reasons.isNotEmpty()) {
            val total = reasons.sumOf { it.value }
            PercentBars("Yanlış nedenleri", reasons.map { Triple(it.name, Scoring.percent(it.value, total), "${it.value} kayıt") })
        }
    }
}
