package app.ykspusula.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ykspusula.data.*
import app.ykspusula.domain.*
import app.ykspusula.monetize.AdPlacement
import app.ykspusula.report.ReportExporter
import app.ykspusula.ui.LocalNav
import app.ykspusula.ui.components.*
import app.ykspusula.ui.longDate
import app.ykspusula.ui.repo
import app.ykspusula.ui.rememberUiScope
import app.ykspusula.ui.theme.Yks
import kotlinx.coroutines.launch
import java.time.LocalDate
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
    val active by r.user.activeDays().collectAsStateWithLifecycle(emptyList())
    val rest by r.user.restDays().collectAsStateWithLifecycle(emptyList())
    val perDayMin by remember { r.user.minutesPerDay(since) }.collectAsStateWithLifecycle(emptyList())
    val perDayQ by remember { r.user.questionsPerDay(since) }.collectAsStateWithLifecycle(emptyList())
    val subjectStats by r.user.subjectStats().collectAsStateWithLifecycle(emptyList())
    val weak by r.user.weakTopics(10, 8).collectAsStateWithLifecycle(emptyList())
    val reasons by r.user.wrongReasons().collectAsStateWithLifecycle(emptyList())
    val sources by r.user.sourceRows().collectAsStateWithLifecycle(emptyList())
    val rows by r.content.allTopicRows().collectAsStateWithLifecycle(emptyList())
    val wrongOpen by r.user.wrongOpen().collectAsStateWithLifecycle(0)
    val streak = Streaks.current(active, date, rest)
    val minMap = perDayMin.associate { it.day to it.value }
    val qMap = perDayQ.associate { it.day to it.value }
    val now = remember { System.currentTimeMillis() }
    val levels = remember(rows) { rows.filter { it.scope != "EXCLUDED" }.map { r.mastery(it, now).level } }
    var compare by remember { mutableStateOf<List<Repository.PeriodCompare>>(emptyList()) }
    LaunchedEffect(solved, minutesTotal) { compare = listOf(7, 30, 90).map { r.comparePeriod(it) } }
    val hasData = solved > 0 || minutesTotal > 0 || mocks.isNotEmpty()

    ScrollColumn(Modifier.statusBarsPadding()) {
        PageHeader("İstatistik", "Gelişimini verilerle keşfet.", Icons.Rounded.Summarize, "Raporlar") { nav.navigate("reports") }
        if (!hasData) {
            YCard { EmptyStateView(Icons.Rounded.BarChart, "Henüz veri yok", "Çalışma kaydı, oturum veya deneme ekledikçe grafiklerin burada oluşur.", "Çalışma kaydet") { nav.navigate("log") } }
            return@ScrollColumn
        }
        Section("Genel özet")
        Row2({ Tile("${minutesTotal / 60} sa ${minutesTotal % 60} dk", "Toplam çalışma", it) }, { Tile("$solved", "Çözülen soru", it) })
        Row2({ Tile("${mocks.size}", "Deneme", it) }, { Tile("$streak gün", "Çalışma serisi (en uzun: ${Streaks.longest(active)})", it) })
        val wk = compare.firstOrNull { it.days == 7 }
        val mo = compare.firstOrNull { it.days == 30 }
        Row2({ Tile("%${wk?.success ?: 0}", "Haftalık başarı", it) }, { Tile("%${mo?.success ?: 0}", "Aylık başarı", it) })
        GroupCard {
            compare.forEachIndexed { i, c ->
                if (i > 0) Line()
                val diff = c.success - c.prevSuccess
                Row(Modifier.fillMaxWidth().padding(Space.l), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Son ${c.days} gün", style = MaterialTheme.typography.bodyLarge, color = Yks.c.text)
                        Muted("${c.solved} soru • ${c.minutes} dk • %${c.success} başarı")
                    }
                    if (c.solved > 0 && c.prevSolved > 0 && diff != 0) {
                        val up = diff > 0
                        Icon(if (up) Icons.AutoMirrored.Rounded.TrendingUp else Icons.AutoMirrored.Rounded.TrendingDown, null, tint = if (up) Yks.c.success else Yks.c.warning, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (up) "+$diff puan" else "$diff puan", style = MaterialTheme.typography.labelMedium, color = if (up) Yks.c.success else Yks.c.warning)
                    } else if (c.solved > 0 && c.prevSolved > 0) Muted("Değişim yok") else Muted("Karşılaştırma yok")
                }
            }
        }
        AdSlot(AdPlacement.STATS)

        var period by rememberSaveable { mutableStateOf("7") }
        Section("Çalışma")
        PillGroup(listOf("7" to "7 gün", "30" to "30 gün"), period) { period = it }
        val n = period.toInt()
        val days = (n - 1 downTo 0).map { date.minusDays(it.toLong()) }
        val every = if (n > 10) 7 else 1
        val lbl = days.map { if (n > 10) it.format(SHORT) else it.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale("tr", "TR")) }
        BarChart("Günlük çalışma süresi", lbl, days.map { (minMap[it.toString()] ?: 0).toDouble() }, "dakika", labelEvery = every)
        val weeks = (7 downTo 0).map { weekStart(date).minusWeeks(it.toLong()) }
        BarChart("Haftalık soru sayısı", weeks.map { it.format(SHORT) }, weeks.map { w -> (0..6).sumOf { d -> qMap[w.plusDays(d.toLong()).toString()] ?: 0 }.toDouble() }, "soru", labelEvery = 2)
        val months = (3 downTo 0).map { date.withDayOfMonth(1).minusMonths(it.toLong()) }
        BarChart("Aylık çalışma", months.map { it.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale("tr", "TR")) }, months.map { m -> minMap.filterKeys { it.startsWith(m.toString().take(7)) }.values.sum().toDouble() }, "dakika")
        HabitCalendar("Çalışma alışkanlığı", (83 downTo 0).map { date.minusDays(it.toLong()) }, minMap.toMutableMap().also { map -> qMap.forEach { (k, v) -> map[k] = (map[k] ?: 0) + v } })

        Section("Deneme netleri")
        val fmt: (Double) -> String = { Scoring.formatNet(it) }
        listOf("TYT", "AYT", "YDT").forEach { e ->
            val list = mocks.filter { it.exam == e }
            if (list.size >= 2) LineChart("$e net gelişimi", list.map { app.ykspusula.ui.shortDate(it.createdAt) }, list.map { it.net }, fmt)
        }
        if (mocks.count { it.exam in listOf("TYT", "AYT", "YDT") } < 2) YCard { Muted("Net grafiği için aynı sınav türünde en az iki deneme gerekir.") }
        SoftButton("Deneme analizini aç", Modifier.fillMaxWidth(), icon = Icons.Rounded.Insights) { nav.navigate("examAnalysis") }

        Section("Ders performansı")
        if (subjectStats.isNotEmpty()) PercentBars("Derslere göre doğru oranı", subjectStats.map { Triple(it.key, Scoring.percent(it.right, it.total), "${it.right} doğru / ${it.total} soru") })
        else YCard { Muted("Çalışma kaydına ders ve soru sayısı ekledikçe burada görünür.") }

        Section("Konu gelişimi")
        val learned = levels.count { it >= 3 }
        ProgressCard("Öğrenilen konular", "$learned / ${levels.size}", if (levels.isEmpty()) 0f else learned.toFloat() / levels.size, Mastery.LABELS.mapIndexed { i, s -> "$s: ${levels.count { it == i }}" }.joinToString(" • ")) { nav.navigate("mastery") }
        if (weak.isNotEmpty()) GroupCard {
            weak.forEachIndexed { i, w ->
                if (i > 0) Line()
                NavRow(w.name, "${w.subjectName} • %${Scoring.percent(w.right, w.total)} doğru • ${w.total} soru", onClick = { nav.navigate("topic/${w.topicId}") })
            }
        }

        val src = sources.filter { !it.s.archived && it.s.totalPages > 0 }
        if (src.isNotEmpty()) {
            Section("Kaynak ilerlemesi")
            PercentBars("Kaynaklarım", src.take(8).map { Triple(it.s.name, Scoring.percent(maxOf(it.s.pagesDone, it.s.lastPage), it.s.totalPages), "${it.s.lastPage}/${it.s.totalPages}. sayfa") })
        }

        Section("Yanlış tekrarları")
        Tile("$wrongOpen", "Açık yanlış kaydı")
        if (reasons.isNotEmpty()) {
            val total = reasons.sumOf { it.value }
            PercentBars("Yanlış nedenleri", reasons.map { Triple(it.name, Scoring.percent(it.value, total), "${it.value} kayıt") })
        }
    }
}

/** Haftalık ve aylık rapor: yalnızca gerçek kayıtlardan oluşur, PDF ve görsel olarak paylaşılır. */
@Composable
fun ReportsScreen() {
    val r = repo()
    val nav = LocalNav.current
    val context = LocalContext.current
    val scope = rememberUiScope()
    var kind by rememberSaveable { mutableStateOf("WEEK") }
    var offset by rememberSaveable { mutableIntStateOf(0) }
    var report by remember { mutableStateOf<Repository.Report?>(null) }
    var message by remember { mutableStateOf("") }
    LaunchedEffect(kind, offset) {
        val base = today()
        report = if (kind == "WEEK") {
            val s = weekStart(base).plusWeeks(offset.toLong())
            r.report(s, s.plusDays(6), "Haftalık rapor")
        } else {
            val s = base.withDayOfMonth(1).plusMonths(offset.toLong())
            r.report(s, s.plusMonths(1).minusDays(1), "Aylık rapor")
        }
    }
    Screen("Raporlar", { nav.popBackStack() }) {
        ScrollColumn {
            PillGroup(listOf("WEEK" to "Haftalık", "MONTH" to "Aylık"), kind) { kind = it; offset = 0 }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton({ offset-- }) { Icon(Icons.Rounded.ChevronLeft, "Önceki dönem", tint = Yks.c.text) }
                val x = report
                Text(if (x != null) "${longDate(x.from)} – ${longDate(x.to)}" else "", style = MaterialTheme.typography.titleMedium, color = Yks.c.text, modifier = Modifier.weight(1f))
                IconButton({ offset++ }, enabled = offset < 0) { Icon(Icons.Rounded.ChevronRight, "Sonraki dönem", tint = Yks.c.text) }
            }
            val x = report ?: return@ScrollColumn
            if (!x.hasData) {
                YCard { EmptyStateView(Icons.Rounded.Summarize, "Bu dönemde yeterli veri yok", "Rapor üretmek için çalışma, görev veya deneme kaydı gerekir.") }
                return@ScrollColumn
            }
            Row2({ Tile("${x.minutes / 60} sa ${x.minutes % 60} dk", "Toplam çalışma", it) }, { Tile("${x.avgMinutes} dk", "Günlük ortalama", it) })
            Row2({ Tile("${x.doneTasks}", "Tamamlanan görev", it) }, { Tile("${x.openTasks.size}", "Tamamlanmayan görev", it) })
            Row2({ Tile("${x.solved}", "Çözülen soru", it) }, { Tile("${x.wrongReviews}", "Tekrar edilen yanlış", it) })
            if (kind == "MONTH" && x.prevMinutes > 0) {
                val d = x.minutes - x.prevMinutes
                InfoBanner("Önceki aya göre ${if (d >= 0) "+" else ""}$d dakika.", if (d >= 0) Icons.AutoMirrored.Rounded.TrendingUp else Icons.AutoMirrored.Rounded.TrendingDown, if (d >= 0) Yks.c.success else Yks.c.warning)
            }
            Section("Değerlendirme")
            YCard {
                if (!x.review.hasEnoughData) Muted("Ayrıntılı değerlendirme için en az ${WeeklyCoach.MIN_ACTIVE_DAYS} gün çalışma kaydı gerekir.")
                x.review.bestTopic?.let { KeyValue("En iyi ilerleme", it) }
                x.review.hardestSubject?.let { KeyValue("Geliştirilebilecek alan", it) }
                x.review.mostStudied?.let { KeyValue("En çok çalışılan", it) }
                x.review.reviewTopics.takeIf { it.isNotEmpty() }?.let { Spacer(Modifier.height(6.dp)); Muted("Tekrar edilecek konular: ${it.joinToString(", ")}") }
                x.review.nextWeek.forEach { Spacer(Modifier.height(4.dp)); Text("• $it", style = MaterialTheme.typography.bodyMedium, color = Yks.c.text) }
            }
            if (x.subjects.isNotEmpty()) {
                val max = x.subjects.sumOf { it.second }
                PercentBars("Derslere göre süre", x.subjects.take(8).map { Triple(it.first, Scoring.percent(it.second, max), "${it.second} dk") })
            }
            if (x.topics.isNotEmpty()) YCard { Text("Çalışılan konular", style = MaterialTheme.typography.titleMedium, color = Yks.c.text); x.topics.take(10).forEach { Muted(it) } }
            if (x.sources.isNotEmpty()) YCard { Text("Kaynaklar", style = MaterialTheme.typography.titleMedium, color = Yks.c.text); x.sources.forEach { KeyValue(it.first, "${it.second} dk") } }
            if (x.finishedSources.isNotEmpty()) YCard { Text("Tamamlanan kaynaklar", style = MaterialTheme.typography.titleMedium, color = Yks.c.text); x.finishedSources.forEach { Muted(it) } }
            if (x.exams.isNotEmpty()) YCard { Text("Denemeler", style = MaterialTheme.typography.titleMedium, color = Yks.c.text); x.exams.forEach { KeyValue(it.title, Scoring.formatNet(it.net)) } }
            if (x.openTasks.isNotEmpty()) YCard { Text("Tamamlanmayan görevler", style = MaterialTheme.typography.titleMedium, color = Yks.c.text); x.openTasks.take(6).forEach { Muted(it) } }
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                MainButton("PDF paylaş", Modifier.weight(1f), icon = Icons.Rounded.PictureAsPdf) {
                    scope.launch { try { ReportExporter.share(context, ReportExporter.pdf(context, x), "application/pdf") } catch (e: Exception) { message = "Rapor oluşturulamadı." } }
                }
                SoftButton("Görsel paylaş", Modifier.weight(1f), icon = Icons.Rounded.Image) {
                    scope.launch { try { ReportExporter.share(context, ReportExporter.png(context, x), "image/png") } catch (e: Exception) { message = "Görsel oluşturulamadı." } }
                }
            }
            if (message.isNotEmpty()) Text(message, color = Yks.c.error)
        }
    }
}
