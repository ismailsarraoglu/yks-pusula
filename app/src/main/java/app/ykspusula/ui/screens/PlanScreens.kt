package app.ykspusula.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ykspusula.data.*
import app.ykspusula.domain.Scoring
import app.ykspusula.domain.Timers
import app.ykspusula.notify.Notifier
import app.ykspusula.ui.LocalNav
import app.ykspusula.ui.components.*
import app.ykspusula.ui.longDate
import app.ykspusula.ui.repo
import app.ykspusula.ui.shortDate
import app.ykspusula.ui.theme.Yks
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

private val WEEK = listOf("Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi", "Pazar")

@Composable
fun PlanScreen() {
    val r = repo()
    val nav = LocalNav.current
    val scope = app.ykspusula.ui.rememberUiScope()
    var week by rememberSaveable { mutableStateOf(weekStart(today()).toString()) }
    val start = LocalDate.parse(week)
    val end = start.plusDays(6)
    val tasks by remember(week) { r.user.tasksBetween(start.toString(), end.toString()) }.collectAsStateWithLifecycle(emptyList())
    var moving by remember { mutableStateOf<StudyTask?>(null) }
    var removing by remember { mutableStateOf<StudyTask?>(null) }
    var info by remember { mutableStateOf("") }
    val done = tasks.count { it.done }

    Screen("Haftalık program", { nav.popBackStack() }) {
        ScrollColumn {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton({ week = start.minusDays(7).toString() }) { Icon(Icons.Rounded.ChevronLeft, "Önceki hafta", tint = Yks.c.text) }
                Text("${longDate(start)} haftası", style = MaterialTheme.typography.titleMedium, color = Yks.c.text, modifier = Modifier.weight(1f))
                IconButton({ week = start.plusDays(7).toString() }) { Icon(Icons.Rounded.ChevronRight, "Sonraki hafta", tint = Yks.c.text) }
            }
            YCard {
                val pct = Scoring.percent(done, tasks.size)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("$done / ${tasks.size} görev", style = MaterialTheme.typography.titleMedium, color = Yks.c.text, modifier = Modifier.weight(1f))
                    Text("%$pct", style = MaterialTheme.typography.titleMedium, color = Yks.c.accent)
                }
                Spacer(Modifier.height(Space.s))
                ProgressBar(pct / 100f)
            }
            MainButton("Otomatik program oluştur", Modifier.fillMaxWidth(), icon = Icons.Rounded.AutoAwesome) {
                scope.launch {
                    val n = r.generatePlan(if (start > today()) start else today())
                    info = if (n > 0) "$n görev oluşturuldu. Tamamlanan görevlerin korunur." else "Program oluşturulamadı: sınav tarihi geçmiş olabilir."
                }
            }
            if (info.isNotEmpty()) Muted(info)
            (0..6).forEach { i ->
                val day = start.plusDays(i.toLong())
                val list = tasks.filter { it.date == day.toString() }
                Section(WEEK[i] + if (day == today()) " • Bugün" else "") {
                    IconButton({ nav.navigate("taskEdit/$day") }) { Icon(Icons.Rounded.Add, "${WEEK[i]} için görev ekle", tint = Yks.c.accent) }
                }
                if (list.isEmpty()) Muted("Görev yok")
                else GroupCard {
                    list.forEachIndexed { k, t ->
                        if (k > 0) Line()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f)) {
                                TaskRow(t, { d -> scope.launch { r.toggleTask(t, d) } }) { if (t.topicId != null) nav.navigate("topic/${t.topicId}") else if (t.kind == "WRONGS") nav.navigate("wrongs") }
                            }
                            IconButton({ moving = t }) { Icon(Icons.Rounded.EventRepeat, "Başka güne taşı", tint = Yks.c.muted) }
                            IconButton({ removing = t }) { Icon(Icons.Rounded.DeleteOutline, "Görevi sil", tint = Yks.c.muted) }
                        }
                    }
                }
            }
        }
    }
    moving?.let { t ->
        AlertDialog(
            onDismissRequest = { moving = null }, containerColor = Yks.c.card, title = { Text("Hangi güne taşınsın?") },
            text = {
                Column {
                    (0..6).forEach { i ->
                        val d = start.plusDays(i.toLong())
                        Text(
                            "${WEEK[i]} (${d.dayOfMonth})", color = Yks.c.text,
                            modifier = Modifier.fillMaxWidth().clickable { scope.launch { r.user.saveTask(t.copy(date = d.toString())) }; moving = null }.padding(vertical = 12.dp),
                        )
                    }
                }
            },
            confirmButton = { TextButton({ moving = null }) { Text("Vazgeç") } },
        )
    }
    removing?.let { t ->
        ConfirmDialog("Görev silinsin mi?", t.title, "Sil", true, { removing = null }) { scope.launch { r.user.deleteTask(t) }; removing = null }
    }
}

@Composable
fun TaskEditScreen(date: String) {
    val r = repo()
    val nav = LocalNav.current
    val scope = app.ykspusula.ui.rememberUiScope()
    val profile by r.user.profile().collectAsStateWithLifecycle(null)
    val subjects by r.content.subjects().collectAsStateWithLifecycle(emptyList())
    var day by rememberSaveable { mutableStateOf(date) }
    var subject by rememberSaveable { mutableStateOf("") }
    var topic by rememberSaveable { mutableStateOf("") }
    var time by rememberSaveable { mutableStateOf("09:00") }
    var minutes by rememberSaveable { mutableStateOf("50") }
    var goal by rememberSaveable { mutableStateOf("0") }
    var title by rememberSaveable { mutableStateOf("") }
    val rows by remember(subject) { if (subject.isEmpty()) kotlinx.coroutines.flow.flowOf(emptyList()) else r.content.topicRows(subject) }.collectAsStateWithLifecycle(emptyList())
    val validDate = runCatching { LocalDate.parse(day) }.isSuccess
    val validTime = Regex("([01]\\d|2[0-3]):[0-5]\\d").matches(time)
    val s = subjects.firstOrNull { it.id == subject }
    val t = rows.firstOrNull { it.id == topic }
    val computed = title.ifBlank { listOfNotNull(s?.name, t?.name).joinToString(" / ") }
    Screen("Görev ekle", { nav.popBackStack() }) {
        ScrollColumn {
            Muted("Ders")
            Dropdown(relevant(subjects, profile).map { it.id to "${it.exam} ${it.name}" }, subject, "Ders seçilmedi") { subject = it; topic = "" }
            if (subject.isNotEmpty()) {
                Muted("Konu")
                Dropdown(rows.map { it.id to it.name }, topic, "Konu seçilmedi") { topic = it }
            }
            Field(title, { title = it }, "Görev adı (isteğe bağlı)")
            Field(day, { day = it }, "Tarih (YYYY-AA-GG)")
            Field(time, { time = it.take(5) }, "Saat (SS:DD)")
            Field(minutes, { minutes = it.filter(Char::isDigit).take(3) }, "Süre (dakika)", keyboard = KeyboardOptions(keyboardType = KeyboardType.Number))
            Field(goal, { goal = it.filter(Char::isDigit).take(3) }, "Soru hedefi", keyboard = KeyboardOptions(keyboardType = KeyboardType.Number))
            MainButton("Kaydet", Modifier.fillMaxWidth(), enabled = validDate && validTime && computed.isNotBlank() && (minutes.toIntOrNull() ?: 0) > 0) {
                scope.launch {
                    r.user.saveTask(StudyTask(date = day, time = time, subjectId = subject.ifEmpty { null }, topicId = topic.ifEmpty { null }, title = computed, kind = "MANUAL", minutes = minutes.toInt(), questionGoal = goal.toIntOrNull() ?: 0))
                    nav.popBackStack()
                }
            }
        }
    }
}

/* ---------- POMODORO ---------- */

@Composable
fun PomodoroScreen() {
    val r = repo()
    val nav = LocalNav.current
    val context = LocalContext.current
    val scope = app.ykspusula.ui.rememberUiScope()
    val state by r.pomodoro.collectAsStateWithLifecycle(PomodoroState())
    val settings by r.settings.collectAsStateWithLifecycle(AppSettings())
    val profile by r.user.profile().collectAsStateWithLifecycle(null)
    val subjects by r.content.subjects().collectAsStateWithLifecycle(emptyList())
    val sessions by r.user.recentSessions(12).collectAsStateWithLifecycle(emptyList())
    val date = remember { today() }
    val todayMin by r.user.minutesBetween(startOf(date), startOf(date.plusDays(1))).collectAsStateWithLifecycle(0)
    var work by rememberSaveable { mutableStateOf("") }
    var pause by rememberSaveable { mutableStateOf("") }
    var subject by rememberSaveable { mutableStateOf("") }
    var topic by rememberSaveable { mutableStateOf("") }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val rows by remember(subject) { if (subject.isEmpty()) kotlinx.coroutines.flow.flowOf(emptyList()) else r.content.topicRows(subject) }.collectAsStateWithLifecycle(emptyList())
    val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    LaunchedEffect(settings.pomodoroWork) { if (work.isEmpty()) { work = settings.pomodoroWork.toString(); pause = settings.pomodoroBreak.toString() } }
    LaunchedEffect(state.running, state.endsAt) {
        while (state.running) {
            now = System.currentTimeMillis()
            if (now >= state.endsAt) { r.settlePomodoro(now); break }
            delay(500)
        }
    }
    val w = work.toIntOrNull() ?: 25
    val b = pause.toIntOrNull() ?: 5
    val left = if (state.running) Timers.remainingSec(state.endsAt, now) else if (state.phase == "IDLE") w * 60 else state.remainingSec
    val total = (if (state.phase == "BREAK") state.breakMin else if (state.phase == "WORK") state.workMin else w) * 60
    val isIdle = state.phase == "IDLE"

    fun begin(phase: String, sec: Int, s: PomodoroState) {
        val t = System.currentTimeMillis()
        val endsAt = t + sec * 1000L
        scope.launch {
            r.savePomodoro(s.copy(phase = phase, running = true, endsAt = endsAt, remainingSec = sec, startedAt = if (s.startedAt != 0L && phase == s.phase) s.startedAt else t))
            Notifier.schedulePomodoro(context, endsAt)
        }
    }
    Screen("Odaklanma", { nav.popBackStack() }) {
        ScrollColumn {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(Space.s))
                Ring(if (total > 0) left.toFloat() / total else 0f, Timers.clock(left), size = 200.dp, color = if (state.phase == "BREAK") Yks.c.success else Yks.c.accent, description = "Kalan süre ${left / 60} dakika")
                Spacer(Modifier.height(Space.s))
                Muted(when (state.phase) { "WORK" -> "Çalışma • ${state.label.ifBlank { "Odak" }}"; "BREAK" -> "Mola"; else -> "Hazır" })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                when {
                    state.running -> SoftButton("Duraklat", Modifier.weight(1f), icon = Icons.Rounded.Pause) {
                        scope.launch { r.savePomodoro(state.copy(running = false, remainingSec = left, endsAt = 0)); Notifier.cancelPomodoro(context) }
                    }
                    isIdle -> MainButton("Başlat", Modifier.weight(1f), icon = Icons.Rounded.PlayArrow) {
                        if (Build.VERSION.SDK_INT >= 33 && !Notifier.canNotify(context)) notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
                        val s = subjects.firstOrNull { it.id == subject }
                        val tp = rows.firstOrNull { it.id == topic }
                        val label = listOfNotNull(s?.name, tp?.name).joinToString(" / ")
                        begin("WORK", w * 60, PomodoroState(workMin = w, breakMin = b, subjectId = subject, topicId = topic, label = label))
                    }
                    else -> MainButton(if (state.phase == "BREAK" && state.remainingSec == state.breakMin * 60) "Molayı başlat" else "Devam et", Modifier.weight(1f), icon = Icons.Rounded.PlayArrow) {
                        begin(state.phase, state.remainingSec, state)
                    }
                }
                if (!isIdle) SoftButton("Sıfırla", Modifier.weight(1f), icon = Icons.Rounded.RestartAlt, color = Yks.c.error) {
                    scope.launch {
                        if (state.phase == "WORK") {
                            val spent = (state.workMin * 60 - left) / 60
                            if (spent >= 1) r.user.saveSession(StudySession(subjectId = state.subjectId.ifEmpty { null }, topicId = state.topicId.ifEmpty { null }, label = state.label.ifBlank { "Odak" }, startedAt = state.startedAt, minutes = spent))
                        }
                        r.savePomodoro(PomodoroState()); Notifier.cancelPomodoro(context)
                    }
                }
            }
            if (isIdle) {
                PillGroup(listOf("25-5" to "25 / 5", "50-10" to "50 / 10", "90-15" to "90 / 15"), "$work-$pause") { v -> work = v.substringBefore('-'); pause = v.substringAfter('-') }
                Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    Field(work, { work = it.filter(Char::isDigit).take(3) }, "Çalışma (dk)", Modifier.weight(1f), keyboard = KeyboardOptions(keyboardType = KeyboardType.Number))
                    Field(pause, { pause = it.filter(Char::isDigit).take(3) }, "Mola (dk)", Modifier.weight(1f), keyboard = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
                Muted("Ders")
                Dropdown(relevant(subjects, profile).map { it.id to "${it.exam} ${it.name}" }, subject, "Ders seçilmedi") { subject = it; topic = "" }
                if (subject.isNotEmpty()) {
                    Muted("Konu")
                    Dropdown(rows.map { it.id to it.name }, topic, "Konu seçilmedi") { topic = it }
                }
            }
            Tile("$todayMin dk", "Bugünkü toplam odak süresi")
            Section("Çalışma geçmişi")
            if (sessions.isEmpty()) Muted("Henüz kayıtlı oturum yok.")
            else GroupCard {
                sessions.forEachIndexed { i, s ->
                    if (i > 0) Line()
                    Row(Modifier.fillMaxWidth().padding(Space.m), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(s.label, style = MaterialTheme.typography.bodyMedium, color = Yks.c.text)
                            Muted(shortDate(s.startedAt))
                        }
                        Text("${s.minutes} dk", style = MaterialTheme.typography.labelLarge, color = Yks.c.text)
                    }
                }
            }
            Muted("Sayaç zaman damgasıyla çalışır. Uygulama kapansa da kalan süre doğru hesaplanır; süre dolunca yerel bildirim gelir.")
        }
    }
}
