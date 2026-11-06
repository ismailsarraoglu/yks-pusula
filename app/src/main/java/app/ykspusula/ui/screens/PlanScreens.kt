package app.ykspusula.ui.screens

import app.ykspusula.ui.currentDay
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
import app.ykspusula.ui.rememberUiScope
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

    val missed by remember { r.missedTasks() }.collectAsStateWithLifecycle(emptyList())
    Screen("Çalışma planı", { nav.popBackStack() }, actions = {
        IconButton({ nav.navigate("planHistory") }) { Icon(Icons.Rounded.History, "Plan geçmişi") }
        IconButton({ nav.navigate("planSettings") }) { Icon(Icons.Rounded.Tune, "Plan ayarları") }
    }) {
        ScrollColumn {
            if (missed.isNotEmpty()) {
                InfoBanner("Tamamlanmayan ${missed.size} görevin var. Haftanın kalanını yeniden düzenleyelim mi?", Icons.Rounded.EventRepeat, Yks.c.accentLight)
                SoftButton("Yeniden düzenle", Modifier.fillMaxWidth(), icon = Icons.Rounded.EventRepeat) { nav.navigate("replan") }
            }
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
                    val res = r.generatePlanDetailed(if (start > today()) start else today())
                    val n = res.tasks.size
                    info = (if (n > 0) "$n görev oluşturuldu: toplam ${res.studyMinutes} dk çalışma. Tamamlanan ve elle eklediğin görevler korundu." else "Program oluşturulamadı.") +
                        res.notes.joinToString("") { "\n• $it" }
                }
            }
            if (info.isNotEmpty()) Muted(info)
            (0..6).forEach { i ->
                val day = start.plusDays(i.toLong())
                val list = tasks.filter { it.date == day.toString() }
                Section(WEEK[i] + if (day == today()) " • Bugün" else "") {
                    IconButton({ nav.navigate("taskEdit/$day") }) { Icon(Icons.Rounded.Add, "${WEEK[i]} için görev ekle", tint = Yks.c.accent) }
                }
                if (list.isNotEmpty()) {
                    val study = list.sumOf { it.minutes }
                    val gaps = list.sortedBy { it.time }.zipWithNext().sumOf { (a, b) -> (clockMinutes(b.time) - clockMinutes(a.time) - a.minutes).coerceAtLeast(0) }
                    Muted("Çalışma $study dk" + if (gaps > 0) " • Mola $gaps dk" else "")
                }
                if (list.isEmpty()) Muted("Görev yok")
                else GroupCard {
                    list.forEachIndexed { k, t ->
                        if (k > 0) Line()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f)) {
                                StudyTaskCard(t, { d -> scope.launch { r.toggleTask(t, d) } }) { if (t.kind == "WRONGS") nav.navigate("wrongs") else nav.navigate("log?topic=${t.topicId.orEmpty()}&subject=${t.subjectId.orEmpty()}&task=${t.id}&source=${t.sourceId.orEmpty()}") }
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
                            modifier = Modifier.fillMaxWidth().clickable { scope.launch { r.user.saveTask(t.copy(date = d.toString(), originalDate = t.originalDate.ifBlank { t.date }, movedCount = t.movedCount + 1, updatedAt = System.currentTimeMillis())) }; moving = null }.padding(vertical = 12.dp),
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
    var day by remember { mutableStateOf(LocalDate.parse(date)) }
    var hour by rememberSaveable { mutableIntStateOf(9) }
    var minuteOf by rememberSaveable { mutableIntStateOf(0) }
    var subject by rememberSaveable { mutableStateOf("") }
    var topic by rememberSaveable { mutableStateOf("") }
    var minutes by rememberSaveable { mutableStateOf("50") }
    var goal by rememberSaveable { mutableStateOf("0") }
    var title by rememberSaveable { mutableStateOf("") }
    val rows by remember(subject) { if (subject.isEmpty()) kotlinx.coroutines.flow.flowOf(emptyList()) else r.content.topicRows(subject) }.collectAsStateWithLifecycle(emptyList())
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
            DateField(day, "Tarih") { day = it }
            TimeField(hour, minuteOf, "Saat") { h, m -> hour = h; minuteOf = m }
            Field(minutes, { minutes = it.filter(Char::isDigit).take(3) }, "Süre (dakika)", keyboard = KeyboardOptions(keyboardType = KeyboardType.Number))
            Field(goal, { goal = it.filter(Char::isDigit).take(3) }, "Soru hedefi", keyboard = KeyboardOptions(keyboardType = KeyboardType.Number))
            MainButton("Kaydet", Modifier.fillMaxWidth(), enabled = computed.isNotBlank() && (minutes.toIntOrNull() ?: 0) > 0) {
                scope.launch {
                    r.user.saveTask(StudyTask(date = day.toString(), time = "%02d:%02d".format(java.util.Locale.US, hour, minuteOf), originalDate = day.toString(), updatedAt = System.currentTimeMillis(), subjectId = subject.ifEmpty { null }, topicId = topic.ifEmpty { null }, title = computed, kind = "MANUAL", minutes = minutes.toInt(), questionGoal = goal.toIntOrNull() ?: 0))
                    nav.popBackStack()
                }
            }
        }
    }
}

/** Plan tercihleri: otomatik plan ve yeniden düzenleme bu sınırları aşmaz. */
@Composable
fun PlanSettingsScreen() {
    val r = repo()
    val nav = LocalNav.current
    val scope = rememberUiScope()
    val subjects by r.content.subjects().collectAsStateWithLifecycle(emptyList())
    val settings by r.settings.collectAsStateWithLifecycle(AppSettings())
    var p by remember { mutableStateOf<UserProfile?>(null) }
    var pick by remember { mutableStateOf(false) }
    var weekly by remember(settings.weeklyGoalMinutes) { mutableStateOf(if (settings.weeklyGoalMinutes > 0) "${settings.weeklyGoalMinutes}" else "") }
    LaunchedEffect(Unit) { p = r.user.profileNow() }
    Screen("Plan ayarları", { nav.popBackStack() }) {
        val u = p ?: return@Screen
        fun toggle(csv: String, id: String): String {
            val s = csv.split(',').filter { it.isNotBlank() }.toMutableSet()
            if (!s.add(id)) s.remove(id)
            return s.sorted().joinToString(",")
        }
        ScrollColumn {
            NumberField("${u.dailyMinutes}", { v -> p = u.copy(dailyMinutes = v.toIntOrNull() ?: 0) }, "Günlük çalışma hedefi (dakika)", max = 3)
            NumberField("${u.maxDailyMinutes}", { v -> p = u.copy(maxDailyMinutes = v.toIntOrNull() ?: 0) }, "Günlük en fazla çalışma (dakika)", max = 3)
            NumberField(weekly, { weekly = it }, "Haftalık hedef (dakika)", max = 5)
            NumberField("${u.dailyQuestions}", { v -> p = u.copy(dailyQuestions = v.toIntOrNull() ?: 0) }, "Günlük soru hedefi", max = 3)
            Muted("Müsait günler")
            MultiPills(DAY_NAMES, u.studyDays.split(',').toSet()) { p = u.copy(studyDays = toggle(u.studyDays, it)) }
            TimeField(u.startHour, 0, "Başlangıç saati") { h, _ -> p = u.copy(startHour = h) }
            TimeField(u.endHour, 0, "Bitiş saati") { h, _ -> p = u.copy(endHour = h) }
            NumberField("${u.blockMinutes}", { v -> p = u.copy(blockMinutes = v.toIntOrNull() ?: 0) }, "Tercih ettiğin çalışma bloğu (dakika)", max = 3)
            NumberField("${u.breakMinutes}", { v -> p = u.copy(breakMinutes = v.toIntOrNull() ?: 0) }, "Bloklar arası mola (dakika)", max = 2)
            Muted("Molalar çalışma süresine eklenmez. Her üç bloktan sonra daha uzun mola verilir. Son blok kalan süreye göre kısalır.")
            val fit = remember(u) {
                val blocks = app.ykspusula.domain.PlanGenerator.split(minOf(u.dailyMinutes, u.maxDailyMinutes), u.blockMinutes.coerceIn(15, 120))
                blocks.sum() + blocks.indices.toList().dropLast(1).sumOf { app.ykspusula.domain.PlanGenerator.breakAfter(it, u.breakMinutes) }
            }
            val window = (u.endHour - u.startHour) * 60
            if (window > 0 && fit > window) Text("Günlük hedefin molalarla ${fit / 60} sa ${fit % 60} dk sürüyor ama seçtiğin saat aralığı ${window / 60} saat. Plan sığan kadarını yerleştirir ve nedenini yazar.", color = Yks.c.warning, style = MaterialTheme.typography.bodyMedium)
            val mine = fieldSubjects(subjects, u).map { it.id to "${it.exam} ${it.name}" }
            Muted("Zorlandığın dersler")
            MultiPills(mine, u.weakSubjects.split(',').toSet()) { p = u.copy(weakSubjects = toggle(u.weakSubjects, it), strongSubjects = u.strongSubjects.split(',').filter { s -> s != it }.joinToString(",")) }
            Muted("Güçlü olduğun dersler")
            MultiPills(mine, u.strongSubjects.split(',').toSet()) { p = u.copy(strongSubjects = toggle(u.strongSubjects, it), weakSubjects = u.weakSubjects.split(',').filter { s -> s != it }.joinToString(",")) }
            val count = u.priorityTopics.split(',').count { it.isNotBlank() }
            SoftButton(if (count > 0) "$count öncelikli konu" else "Öncelikli konu seç", Modifier.fillMaxWidth(), icon = Icons.Rounded.Flag) { pick = true }
            val valid = u.studyDays.isNotBlank() && u.dailyMinutes > 0 && u.maxDailyMinutes >= u.dailyMinutes && u.endHour > u.startHour && u.blockMinutes in 15..120
            if (!valid) Text("Günlük en fazla süre hedeften az olamaz; bitiş saati başlangıçtan sonra olmalı; blok 15-120 dakika olmalı.", color = Yks.c.error, style = MaterialTheme.typography.bodyMedium)
            MainButton("Kaydet ve planı güncelle", Modifier.fillMaxWidth(), enabled = valid) {
                scope.launch {
                    r.user.saveProfile(u)
                    r.updateSettings { it[Keys.weekly] = weekly.toIntOrNull() ?: 0 }
                    r.generatePlan()
                    r.rebuildRoute()
                    nav.popBackStack()
                }
            }
        }
        if (pick) TopicPickerDialog(if (u.field == "TYT") "TYT" else if (u.field == "DIL") "YDT" else "AYT", u.priorityTopics.split(',').filter { it.isNotBlank() }.toSet(), { pick = false }) { p = u.copy(priorityTopics = it.joinToString(",")) }
    }
}

/* ---------- POMODORO ---------- */

/** Odaklanma ekranı: reklam gösterilmez. Sayaç zaman damgasıyla çalışır, kapanma ve yeniden başlatmadan etkilenmez. */
@Composable
fun PomodoroScreen(subjectArg: String, topicArg: String, sourceArg: String, minutesArg: Int, labelArg: String, routeArg: String) {
    val r = repo()
    val nav = LocalNav.current
    val context = LocalContext.current
    val scope = rememberUiScope()
    val state by r.pomodoro.collectAsStateWithLifecycle(PomodoroState())
    val settings by r.settings.collectAsStateWithLifecycle(AppSettings())
    val profile by r.user.profile().collectAsStateWithLifecycle(null)
    val subjects by r.content.subjects().collectAsStateWithLifecycle(emptyList())
    val sources by r.user.sources().collectAsStateWithLifecycle(emptyList())
    val sessions by r.user.recentSessions(8).collectAsStateWithLifecycle(emptyList())
    val date = currentDay()
    val todayMin by r.user.minutesBetween(startOf(date), startOf(date.plusDays(1))).collectAsStateWithLifecycle(0)
    var work by rememberSaveable { mutableStateOf(if (minutesArg > 0) "$minutesArg" else "") }
    var pause by rememberSaveable { mutableStateOf("") }
    var subject by rememberSaveable { mutableStateOf(subjectArg) }
    var topic by rememberSaveable { mutableStateOf(topicArg) }
    var source by rememberSaveable { mutableStateOf(sourceArg) }
    var activity by rememberSaveable { mutableStateOf("SORU") }
    var note by rememberSaveable { mutableStateOf("") }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val rows by remember(subject) { if (subject.isEmpty()) kotlinx.coroutines.flow.flowOf(emptyList()) else r.content.topicRows(subject) }.collectAsStateWithLifecycle(emptyList())
    val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    LaunchedEffect(settings.pomodoroWork) { if (work.isEmpty()) work = settings.pomodoroWork.toString(); if (pause.isEmpty()) pause = settings.pomodoroBreak.toString() }
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
    val spentMin = ((state.workMin * 60 - left) / 60).coerceAtLeast(0)

    fun begin(phase: String, sec: Int, s: PomodoroState) {
        val t = System.currentTimeMillis()
        val endsAt = t + sec * 1000L
        scope.launch {
            r.savePomodoro(s.copy(phase = phase, running = true, endsAt = endsAt, remainingSec = sec, startedAt = if (s.startedAt != 0L && phase == s.phase) s.startedAt else t))
            Notifier.schedulePomodoro(context, endsAt)
        }
    }
    /** Gerçek süre kaydedilir; planlanan süre ayrı tutulur. */
    var discard by remember { mutableStateOf(false) }
    if (discard) ConfirmDialog("Oturumdan vazgeçilsin mi?", "Bu oturumun süresi çalışma kaydına eklenmez.", "Vazgeç", true, { discard = false }) {
        discard = false
        scope.launch { Notifier.cancelPomodoro(context); r.savePomodoro(PomodoroState()); note = "" }
    }
    fun finishWork(next: PomodoroState) {
        scope.launch {
            val id = "pomo-${state.startedAt}"
            if (state.phase == "WORK" && spentMin >= 1) r.saveFocus(id, state.copy(note = note), spentMin)
            Notifier.cancelPomodoro(context)
            r.savePomodoro(next.copy(pendingSessionId = if (state.phase == "WORK" && spentMin >= 1) id else state.pendingSessionId))
            note = ""
        }
    }
    Screen("Odaklanma", { nav.popBackStack() }) {
        ScrollColumn {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(Space.s))
                Ring(if (total > 0) left.toFloat() / total else 0f, Timers.clock(left), size = 210.dp, color = if (state.phase == "BREAK") Yks.c.success else Yks.c.accent, description = "Kalan süre ${left / 60} dakika")
                Spacer(Modifier.height(Space.s))
                Muted(when (state.phase) { "WORK" -> "Çalışma • ${state.label.ifBlank { "Odak" }}"; "BREAK" -> "Mola"; else -> labelArg.ifBlank { "Hazır" } })
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
                        val label = labelArg.ifBlank { listOfNotNull(s?.name, tp?.name).joinToString(" / ") }
                        if (routeArg.isNotEmpty()) scope.launch { r.acceptRoute(routeArg) }
                        begin("WORK", w * 60, PomodoroState(workMin = w, breakMin = b, subjectId = subject, topicId = topic, sourceId = source, activity = activity, label = label))
                    }
                    else -> MainButton(if (state.phase == "BREAK" && state.remainingSec == state.breakMin * 60) "Molayı başlat" else "Devam et", Modifier.weight(1f), icon = Icons.Rounded.PlayArrow) {
                        begin(state.phase, state.remainingSec, state)
                    }
                }
                if (state.phase == "WORK") SoftButton("Ara ver", Modifier.weight(1f), icon = Icons.Rounded.Coffee) {
                    finishWork(state.copy(phase = "BREAK", running = false, endsAt = 0, remainingSec = state.breakMin * 60, startedAt = 0))
                }
                if (!isIdle) SoftButton("Bitir", Modifier.weight(1f), icon = Icons.Rounded.Stop, color = Yks.c.error) { finishWork(PomodoroState()) }
            }
            if (state.phase == "WORK") {
                TextButton({ discard = true }, Modifier.fillMaxWidth()) { Text("Oturumdan vazgeç (kaydetme)", color = Yks.c.muted) }
                Field(note, { note = it }, "Kısa not")
            }
            if (state.pendingSessionId.isNotEmpty()) PerformanceCard(state.pendingSessionId) { scope.launch { r.savePomodoro(state.copy(pendingSessionId = "")) } }
            if (isIdle) {
                PillGroup(listOf("25-5" to "25 / 5", "50-10" to "50 / 10", "90-15" to "90 / 15"), "$work-$pause") { v -> work = v.substringBefore('-'); pause = v.substringAfter('-') }
                Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    NumberField(work, { work = it }, "Hedef süre (dk)", Modifier.weight(1f), 3)
                    NumberField(pause, { pause = it }, "Mola (dk)", Modifier.weight(1f), 3)
                }
                Muted("Ders")
                Dropdown(relevant(subjects, profile).map { it.id to "${it.exam} ${it.name}" }, subject, "Ders seçilmedi") { subject = it; topic = "" }
                if (subject.isNotEmpty()) { Muted("Konu"); Dropdown(rows.map { it.id to it.name }, topic, "Konu seçilmedi") { topic = it } }
                Muted("Kaynak")
                Dropdown(sources.map { it.id to it.name }, source, "Kaynak seçilmedi") { source = it }
                Muted("Çalışma türü")
                PillGroup(ACTIVITIES, activity) { activity = it }
            }
            Tile("$todayMin dk", "Bugünkü toplam çalışma")
            Section("Çalışma geçmişi")
            if (sessions.isEmpty()) Muted("Henüz kayıtlı oturum yok.")
            else GroupCard {
                sessions.forEachIndexed { i, s ->
                    if (i > 0) Line()
                    Row(Modifier.fillMaxWidth().padding(Space.m), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(s.label, style = MaterialTheme.typography.bodyMedium, color = Yks.c.text)
                            Muted(shortDate(s.startedAt) + if (s.plannedMinutes > 0 && s.plannedMinutes != s.minutes) " • Plan ${s.plannedMinutes} dk" else "")
                        }
                        Text("${s.minutes} dk", style = MaterialTheme.typography.labelLarge, color = Yks.c.text)
                    }
                }
            }
            Muted("Sayaç zaman damgasıyla çalışır. Uygulama kapansa veya telefon yeniden başlasa da süre doğru hesaplanır.")
        }
    }
}

/** Oturum bitince soru performansı girilir. */
@Composable
private fun PerformanceCard(sessionId: String, onDone: () -> Unit) {
    val r = repo()
    val scope = rememberUiScope()
    var correct by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf("") }
    var blank by remember { mutableStateOf("") }
    YCard {
        Text("Bu çalışmada soru çözdün mü?", style = MaterialTheme.typography.titleMedium, color = Yks.c.text)
        Spacer(Modifier.height(Space.s))
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            NumberField(correct, { correct = it }, "Doğru", Modifier.weight(1f), 3)
            NumberField(wrong, { wrong = it }, "Yanlış", Modifier.weight(1f), 3)
            NumberField(blank, { blank = it }, "Boş", Modifier.weight(1f), 3)
        }
        Spacer(Modifier.height(Space.s))
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            MainButton("Kaydet", Modifier.weight(1f)) {
                scope.launch { r.addPerformance(sessionId, correct.toIntOrNull() ?: 0, wrong.toIntOrNull() ?: 0, blank.toIntOrNull() ?: 0, ""); onDone() }
            }
            SoftButton("Geç", Modifier.weight(1f)) { onDone() }
        }
    }
}

private fun clockMinutes(t: String): Int = (t.substringBefore(':').toIntOrNull() ?: 0) * 60 + (t.substringAfter(':', "0").toIntOrNull() ?: 0)
