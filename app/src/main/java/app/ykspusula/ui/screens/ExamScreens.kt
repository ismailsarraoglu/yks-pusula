package app.ykspusula.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ykspusula.data.*
import app.ykspusula.domain.ExamAnalysis
import app.ykspusula.domain.Scoring
import app.ykspusula.domain.Timers
import app.ykspusula.monetize.AdPlacement
import app.ykspusula.ui.LocalNav
import app.ykspusula.ui.components.*
import app.ykspusula.ui.longDate
import app.ykspusula.ui.repo
import app.ykspusula.ui.rememberUiScope
import app.ykspusula.ui.shortDate
import app.ykspusula.ui.theme.Gradients
import app.ykspusula.ui.theme.NumberStyle
import app.ykspusula.ui.theme.Yks
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

private data class Shortcut(val title: String, val subtitle: String, val icon: ImageVector, val kind: String)

/** Denemeler: yalnızca dışarıda çözülen gerçek denemeleri yönetir, uygulama soru üretmez. */
@Composable
fun ExamsScreen() {
    val r = repo()
    val nav = LocalNav.current
    val c = Yks.c
    val mocks by r.user.mockSummaries().collectAsStateWithLifecycle(emptyList())
    val running by r.user.running().collectAsStateWithLifecycle(null)
    val minutesOf = remember { mutableStateMapOf<String, Int>() }
    LaunchedEffect(mocks) { mocks.takeLast(3).forEach { m -> minutesOf[m.id] = r.user.mock(m.id)?.spentMinutes ?: 0 } }
    ScrollColumn(Modifier.statusBarsPadding()) {
        PageHeader("Denemeler", "Kendini test et, gelişimini gör.", Icons.Rounded.History, "Deneme geçmişi") { nav.navigate("examHistory") }
        running?.let { m ->
            InfoRow(if (m.status == "ENTRY") "Sonuç girişi bekliyor" else "Devam eden deneme", m.title, Icons.Rounded.Timer) {
                nav.navigate(if (m.status == "ENTRY") "examEntry/${m.id}" else "examRun/${m.id}")
            }
        }
        PrimaryGradientCard("Hemen deneme oluştur!", "Hedefine uygun bir sınav seç ve başla.", Icons.Rounded.TrackChanges) { nav.navigate("examStart/ALL") }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val cards: List<@Composable (Modifier) -> Unit> = listOf(
                { m -> ExamTypeCard("TYT", "Genel Sınav", Icons.AutoMirrored.Rounded.MenuBook, Gradients.blue, m) { nav.navigate("examStart/TYT") } },
                { m -> ExamTypeCard("AYT", "Alan Sınavı", Icons.Rounded.TrackChanges, Gradients.purple, m) { nav.navigate("examStart/AYT") } },
                { m -> ExamTypeCard("YDT", "Yabancı Dil", Icons.Rounded.Language, Gradients.navy, m) { nav.navigate("examStart/YDT") } },
            )
            if (rememberCompact(maxWidth, 300.dp)) Column(verticalArrangement = Arrangement.spacedBy(Space.s + 2.dp)) { cards.forEach { it(Modifier.fillMaxWidth()) } }
            else Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Space.s + 2.dp)) { cards.forEach { it(Modifier.weight(1f).fillMaxHeight()) } }
        }
        SectionHeader("Diğer Denemeler", "Tümünü Gör") { nav.navigate("examStart/ALL") }
        val list = listOf(
            Shortcut("Branş Denemesi", "Tek ders seç", Icons.Rounded.School, "BRANCH"),
            Shortcut("Konu Denemesi", "Konuya göre kaydet", Icons.Rounded.Folder, "TOPIC"),
            Shortcut("Kişisel Deneme", "Eksiklerini takip et", Icons.Rounded.AutoAwesome, "PERSONAL"),
            Shortcut("Özel Deneme", "Kendi düzenini kur", Icons.Rounded.Tune, "CUSTOM"),
        )
        TwoColumn(list) { s, m ->
            val tint = if (s.kind == "TOPIC" || s.kind == "PERSONAL") c.purpleLight else c.accentLight
            FeatureShortcutCard(s.title, s.subtitle, s.icon, tint, m) { nav.navigate("examStart/${s.kind}") }
        }
        ExpandableToolsCard("Deneme Araçları", Icons.Rounded.HomeRepairService) {
            NavRow("Dışarıda çözdüğüm denemeyi kaydet", "Kitapçık sonuçlarını ekle", Icons.Rounded.EditNote, c.purpleLight, { nav.navigate("external") }); Line()
            NavRow("Deneme analizi", "Ders netleri ve gelişim", Icons.Rounded.Insights, c.accentLight, { nav.navigate("examAnalysis") }); Line()
            NavRow("Net hedeflerim", "Hedefe kalan fark", Icons.Rounded.Flag, c.success, { nav.navigate("netGoals") }); Line()
            NavRow("Sınav Ayarları", "Süre, tarih ve net kuralı", Icons.Rounded.Settings, c.accentLight, { nav.navigate("examSettings") }); Line()
            NavRow("OBP Hesaplama", "Diploma notunun katkısı", Icons.Rounded.Calculate, c.accentLight, { nav.navigate("obp") })
        }
        AdSlot(AdPlacement.EXAMS)
        SectionHeader("Son Denemelerin", "Tüm Geçmiş") { nav.navigate("examHistory") }
        if (mocks.isEmpty()) {
            YCard { EmptyStateView(Icons.Rounded.Assignment, "Henüz deneme kaydın yok", "Çözdüğün ilk denemeyi eklediğinde sonuçların ve gelişimin burada görünür.", "Deneme ekle") { nav.navigate("external") } }
        } else mocks.takeLast(3).reversed().forEach { m ->
            RecentExamCard(m.title, shortDate(m.createdAt), minutesOf[m.id] ?: 0, Scoring.formatNet(m.net)) { nav.navigate("result/${m.id}") }
        }
    }
}

@Composable
fun InfoRow(title: String, detail: String, icon: ImageVector, onClick: () -> Unit) {
    YCard(onClick = onClick, padding = Space.m) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Yks.c.warning)
            Spacer(Modifier.width(Space.m))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = Yks.c.text)
                Muted(detail, maxLines = 1)
            }
            Text("Devam et", color = Yks.c.accentLight, style = MaterialTheme.typography.labelLarge)
        }
    }
}

val KIND_LABELS = mapOf("TYT" to "TYT denemesi", "AYT" to "AYT denemesi", "YDT" to "YDT denemesi", "BRANCH" to "Branş denemesi", "TOPIC" to "Konu denemesi", "PERSONAL" to "Kişisel deneme", "CUSTOM" to "Özel deneme")

/** Deneme başlatma. Sorular öğrencinin kendi kitapçığındadır; uygulama süreyi ve sonucu tutar. */
@Composable
fun ExamStartScreen(kind: String) {
    val r = repo()
    val nav = LocalNav.current
    val scope = rememberUiScope()
    val profile by r.user.profile().collectAsStateWithLifecycle(null)
    val subjects by r.content.subjects().collectAsStateWithLifecycle(emptyList())
    val configs by r.user.examConfigs().collectAsStateWithLifecycle(emptyList())
    if (kind == "ALL") {
        Screen("Deneme seç", { nav.popBackStack() }) {
            ScrollColumn {
                Muted("Denemeyi kendi kitapçığından veya PDF'inden çözersin. Burada süreni tutar, sonuçlarını kaydeder ve analiz edersin.")
                GroupCard {
                    KIND_LABELS.entries.forEachIndexed { i, (k, v) ->
                        if (i > 0) Line()
                        NavRow(v, when (k) { "TYT", "AYT", "YDT" -> "Genel deneme"; "BRANCH" -> "Tek ders"; "TOPIC" -> "Tek konu"; "PERSONAL" -> "Eksiklerini takip etmek için"; else -> "Kendi test düzenin" }, Icons.Rounded.Assignment, onClick = { nav.navigate("examStart/$k") })
                    }
                }
            }
        }
        return
    }
    val isGeneral = kind in listOf("TYT", "AYT", "YDT")
    val cfg = configs.firstOrNull { it.code == kind }
    var subject by rememberSaveable { mutableStateOf("") }
    var topic by rememberSaveable { mutableStateOf("") }
    var minutes by rememberSaveable(cfg?.durationMin) { mutableStateOf((cfg?.durationMin ?: 40).toString()) }
    var title by rememberSaveable { mutableStateOf("") }
    val p = profile
    val rows by remember(subject) { if (subject.isEmpty()) kotlinx.coroutines.flow.flowOf(emptyList()) else r.content.topicRows(subject) }.collectAsStateWithLifecycle(emptyList())
    val chosen = subjects.firstOrNull { it.id == subject }
    val ok = when (kind) { "BRANCH" -> subject.isNotEmpty(); "TOPIC" -> topic.isNotEmpty(); else -> true }
    fun name(): String = title.trim().ifBlank {
        when (kind) {
            "BRANCH" -> "${chosen?.name.orEmpty()} denemesi"
            "TOPIC" -> "${rows.firstOrNull { it.id == topic }?.name.orEmpty()} denemesi"
            else -> KIND_LABELS[kind].orEmpty()
        }
    }
    val exam = if (isGeneral) kind else chosen?.exam ?: "TYT"
    Screen(KIND_LABELS[kind].orEmpty(), { nav.popBackStack() }) {
        ScrollColumn {
            Muted("Soruları kendi kitapçığından veya PDF'inden çöz. İstersen burada süre tut; bitince sonuçlarını gir.")
            if (isGeneral && cfg != null) YCard {
                Text(cfg.title, style = MaterialTheme.typography.titleMedium, color = Yks.c.text)
                Muted("${cfg.questionCount} soru • ${cfg.durationMin} dakika")
            }
            if (kind == "BRANCH" || kind == "TOPIC") {
                Muted("Ders")
                Dropdown(relevant(subjects, p).map { it.id to "${it.exam} ${it.name}" }, subject, "Ders seç") { subject = it; topic = "" }
            }
            if (kind == "TOPIC" && subject.isNotEmpty()) {
                Muted("Konu")
                Dropdown(rows.map { it.id to it.name }, topic, "Konu seç") { topic = it }
            }
            Field(title, { title = it }, "Deneme adı (isteğe bağlı)")
            NumberField(minutes, { minutes = it }, "Hedef süre (dakika)", max = 3)
            MainButton("Süreyi başlat", Modifier.fillMaxWidth(), enabled = ok && (minutes.toIntOrNull() ?: 0) > 0, icon = Icons.Rounded.PlayArrow) {
                scope.launch {
                    val id = r.startExamSession(name(), if (isGeneral) "GENERAL" else kind, exam, minutes.toInt(), subject.ifEmpty { null })
                    nav.navigate("examRun/$id") { popUpTo("exams") }
                }
            }
            SoftButton("Süre tutmadan sonuçları gir", Modifier.fillMaxWidth(), enabled = ok, icon = Icons.Rounded.EditNote) {
                nav.navigate("external?kind=$kind&exam=$exam&subject=$subject&title=${android.net.Uri.encode(name())}")
            }
        }
    }
}

/** Kronometre: uygulama kapansa da zaman damgasıyla doğru kalır. */
@Composable
fun ExamRunScreen(id: String) {
    val r = repo()
    val nav = LocalNav.current
    val scope = rememberUiScope()
    var exam by remember { mutableStateOf<MockExam?>(null) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var leave by remember { mutableStateOf(false) }
    var finish by remember { mutableStateOf(false) }
    LaunchedEffect(id) {
        val m = r.user.mock(id)
        exam = m
        if (m == null || m.status == "DONE") { nav.navigate("result/$id") { popUpTo("examRun/{id}") { inclusive = true } }; return@LaunchedEffect }
        if (m.status == "ENTRY") { nav.navigate("examEntry/$id") { popUpTo("examRun/{id}") { inclusive = true } }; return@LaunchedEffect }
        while (true) { now = System.currentTimeMillis(); delay(500) }
    }
    BackHandler { leave = true }
    Screen("Deneme oturumu", { leave = true }) {
        val m = exam ?: return@Screen
        val elapsed = r.elapsedSec(m, now)
        val limit = m.durationSec
        val paused = m.pausedAt > 0
        val left = limit - elapsed
        ScrollColumn {
            Text(m.title, style = MaterialTheme.typography.titleLarge, color = Yks.c.text, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Column(Modifier.fillMaxWidth().padding(vertical = Space.m), horizontalAlignment = Alignment.CenterHorizontally) {
                Ring(if (limit > 0) (elapsed.toFloat() / limit).coerceAtMost(1f) else 0f, Timers.clock(elapsed), size = 220.dp, description = "Geçen süre ${elapsed / 60} dakika")
                Spacer(Modifier.height(Space.m))
                Text(
                    if (left >= 0) "Hedef süreye ${Timers.clock(left)} kaldı" else "Hedef süreyi ${Timers.clock(-left)} geçtin",
                    style = MaterialTheme.typography.bodyLarge, color = if (left >= 0) Yks.c.muted else Yks.c.warning,
                    modifier = Modifier.semantics { contentDescription = "Hedef süre durumu" },
                )
                if (paused) Muted("Duraklatıldı")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                if (paused) MainButton("Devam et", Modifier.weight(1f), icon = Icons.Rounded.PlayArrow) { scope.launch { r.resumeExam(id); exam = r.user.mock(id) } }
                else SoftButton("Duraklat", Modifier.weight(1f), icon = Icons.Rounded.Pause) { scope.launch { r.pauseExam(id); exam = r.user.mock(id) } }
                MainButton("Bitir", Modifier.weight(1f), icon = Icons.Rounded.Stop) { finish = true }
            }
            Muted("Bu ekranda reklam gösterilmez. Uygulamadan çıksan da süre doğru hesaplanır.")
        }
    }
    if (finish) ConfirmDialog("Deneme bitirilsin mi?", "Süre durur ve sonuç girişine geçersin.", "Bitir", false, { finish = false }) {
        finish = false
        scope.launch { r.stopExam(id); nav.navigate("examEntry/$id") { popUpTo("examRun/{id}") { inclusive = true } } }
    }
    if (leave) ConfirmDialog("Oturumdan çıkılsın mı?", "Süre işlemeye devam eder. Denemeler bölümünden geri dönebilirsin.", "Çık", false, { leave = false }) {
        leave = false
        nav.popBackStack()
    }
}

data class FormSection(val key: String, val name: String, val max: Int, val subjectId: String?)

/** Dışarıda çözülen denemenin sonuç formu. Boş sayısı yazılmazsa kalan sorular boş sayılır. */
@Composable
private fun ExamResultForm(
    exam: String, kind: String, subjectId: String?, initialTitle: String, initialPublisher: String, initialDate: LocalDate, initialMinutes: Int,
    initialRows: List<MockExamResult>, initialHard: String, initialNote: String, submit: String,
    onSave: (title: String, publisher: String, date: LocalDate, minutes: Int, rows: List<SectionEntry>, hard: String, note: String) -> Unit,
) {
    val r = repo()
    val configs by r.user.examConfigs().collectAsStateWithLifecycle(emptyList())
    val profile by r.user.profile().collectAsStateWithLifecycle(null)
    val subjects by r.content.subjects().collectAsStateWithLifecycle(emptyList())
    var title by rememberSaveable { mutableStateOf(initialTitle) }
    var publisher by rememberSaveable { mutableStateOf(initialPublisher) }
    var date by remember { mutableStateOf(initialDate) }
    var spent by rememberSaveable { mutableStateOf(if (initialMinutes > 0) initialMinutes.toString() else "") }
    var note by rememberSaveable { mutableStateOf(initialNote) }
    var hard by rememberSaveable { mutableStateOf(initialHard) }
    var pick by remember { mutableStateOf(false) }
    val values = remember { mutableStateMapOf<String, String>() }
    LaunchedEffect(initialRows) { initialRows.forEach { x -> values["${x.section}|c"] = "${x.correct}"; values["${x.section}|w"] = "${x.wrong}"; values["${x.section}|b"] = "${x.blank}" } }
    val cfg = configs.firstOrNull { it.code == exam }
    val all = cfg?.let { r.sectionsOf(it) }.orEmpty()
    val general = kind == "GENERAL" || kind == "EXTERNAL" || kind in listOf("TYT", "AYT", "YDT")
    var chosen by remember(cfg?.code, profile?.field) {
        mutableStateOf(
            if (initialRows.isNotEmpty()) initialRows.map { it.section }.toSet()
            else when {
                exam != "AYT" -> all.map { it.name }.toSet()
                profile?.field == "SAY" -> setOf("Matematik", "Fen Bilimleri")
                profile?.field == "EA" -> setOf("Matematik", "Türk Dili ve Edebiyatı-Sosyal Bilimler-1")
                profile?.field == "SOZ" -> setOf("Türk Dili ve Edebiyatı-Sosyal Bilimler-1", "Sosyal Bilimler-2")
                else -> all.map { it.name }.toSet()
            }
        )
    }
    val subjectName = subjects.firstOrNull { it.id == subjectId }?.name
    var extra by rememberSaveable { mutableStateOf(initialRows.filter { !general }.joinToString("|") { it.section }.ifBlank { subjectName ?: "Genel" }) }
    val sections: List<FormSection> = if (general) all.filter { it.name in chosen }.map { s -> FormSection(s.name, s.name, s.parts.sumOf { it.count }, s.parts.singleOrNull()?.subjectId) }
    else extra.split('|').filter { it.isNotBlank() }.map { FormSection(it, it, 0, subjectId) }
    val divisor = cfg?.wrongDivisor ?: 4.0
    fun v(s: String, k: String) = values["$s|$k"]?.toIntOrNull() ?: 0
    fun blank(s: FormSection): Int = values["${s.key}|b"]?.toIntOrNull() ?: if (s.max > 0) (s.max - v(s.key, "c") - v(s.key, "w")).coerceAtLeast(0) else 0
    val problems = sections.filter { s -> s.max > 0 && !Scoring.validSection(v(s.key, "c"), v(s.key, "w"), blank(s), s.max) }
    val answered = sections.sumOf { v(it.key, "c") + v(it.key, "w") + blank(it) }
    val total = sections.sumOf { Scoring.net(v(it.key, "c"), v(it.key, "w"), divisor) }
    ScrollColumn {
        Field(title, { title = it }, "Deneme adı")
        Field(publisher, { publisher = it }, "Yayınevi veya kaynak")
        DateField(date, "Tarih") { date = it }
        if (general && all.size > 1) {
            Muted("Testler")
            MultiPills(all.map { it.name to it.name }, chosen) { n -> chosen = if (n in chosen) chosen - n else chosen + n }
        }
        if (!general) {
            Field(extra.replace("|", ", "), { extra = it.replace(", ", "|").replace(",", "|") }, "Testler (virgülle ayır)")
        }
        sections.forEach { s ->
            YCard(padding = Space.m) {
                Text(if (s.max > 0) "${s.name} (${s.max} soru)" else s.name, style = MaterialTheme.typography.titleMedium, color = Yks.c.text)
                Spacer(Modifier.height(Space.s))
                Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    listOf("c" to "Doğru", "w" to "Yanlış", "b" to "Boş").forEach { (k, label) ->
                        NumberField(values["${s.key}|$k"].orEmpty(), { values["${s.key}|$k"] = it }, label, Modifier.weight(1f), max = 3)
                    }
                }
                Spacer(Modifier.height(4.dp))
                if (s in problems) Text("Toplam ${s.max} soruyu aşamaz.", color = Yks.c.error, style = MaterialTheme.typography.bodyMedium)
                else Muted("Net: ${Scoring.formatNet(Scoring.net(v(s.key, "c"), v(s.key, "w"), divisor))}" + if (values["${s.key}|b"].isNullOrEmpty() && s.max > 0) " • ${blank(s)} boş" else "")
            }
        }
        NumberField(spent, { spent = it }, "Süre (dakika)", max = 3)
        Field(note, { note = it }, "Kişisel not", singleLine = false, minLines = 2)
        val hardCount = hard.split(',').count { it.isNotBlank() }
        SoftButton(if (hardCount > 0) "$hardCount zorlandığın konu seçildi" else "Zorlandığın konuları seç", Modifier.fillMaxWidth(), icon = Icons.Rounded.Flag) { pick = true }
        YCard { Row { Text("Toplam net", color = Yks.c.text, modifier = Modifier.weight(1f)); Text(Scoring.formatNet(total), style = MaterialTheme.typography.titleLarge.merge(NumberStyle), color = Yks.c.accentLight) } }
        MainButton(submit, Modifier.fillMaxWidth(), enabled = title.isNotBlank() && problems.isEmpty() && sections.isNotEmpty() && answered > 0) {
            onSave(title.trim(), publisher.trim(), date, spent.toIntOrNull() ?: 0, sections.map { SectionEntry(it.name, v(it.key, "c"), v(it.key, "w"), blank(it), it.subjectId) }, hard, note.trim())
        }
    }
    if (pick) TopicPickerDialog(exam, hard.split(',').filter { it.isNotBlank() }.toSet(), { pick = false }) { hard = it.joinToString(",") }
}

/** Birden çok konu seçmek için aranabilir liste. */
@Composable
fun TopicPickerDialog(exam: String, selected: Set<String>, onDismiss: () -> Unit, onChange: (Set<String>) -> Unit) {
    val r = repo()
    var all by remember { mutableStateOf<List<Pair<Topic, String>>>(emptyList()) }
    var q by remember { mutableStateOf("") }
    var cur by remember { mutableStateOf(selected) }
    LaunchedEffect(exam) {
        val subs = r.content.subjectsNow().associateBy { it.id }
        all = r.content.allTopics().filter { subs[it.subjectId]?.exam == exam && it.scope != "EXCLUDED" }.map { it to subs[it.subjectId]?.name.orEmpty() }
    }
    val n = app.ykspusula.domain.SearchText.normalize(q)
    val shown = all.filter { n.isEmpty() || app.ykspusula.domain.SearchText.score(n, app.ykspusula.domain.SearchText.normalize(it.first.name + " " + it.second)) > 0 }.take(60)
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = Yks.c.card, title = { Text("Konu seç") },
        text = {
            Column {
                Field(q, { q = it }, "Konu ara")
                Spacer(Modifier.height(Space.s))
                LazyColumn(Modifier.heightIn(max = 340.dp)) {
                    items(shown, key = { it.first.id }) { (t, s) ->
                        Row(Modifier.fillMaxWidth().clickable { cur = if (t.id in cur) cur - t.id else cur + t.id }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(t.id in cur, null)
                            Spacer(Modifier.width(Space.s))
                            Column { Text(t.name, color = Yks.c.text, style = MaterialTheme.typography.bodyMedium); Muted(s) }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton({ onChange(cur); onDismiss() }) { Text("Tamam") } },
    )
}

/** Süreli oturumdan veya düzenlemeden gelen sonuç girişi. */
@Composable
fun ExamEntryScreen(id: String) {
    val r = repo()
    val nav = LocalNav.current
    val scope = rememberUiScope()
    var m by remember { mutableStateOf<MockExam?>(null) }
    var rows by remember { mutableStateOf<List<MockExamResult>?>(null) }
    LaunchedEffect(id) { m = r.user.mock(id); rows = r.user.results(id) }
    Screen("Sonuçları gir", { nav.popBackStack() }) {
        val e = m ?: return@Screen
        val list = rows ?: return@Screen
        ExamResultForm(
            e.exam, e.kind, e.subjectId, e.title, e.publisher, e.examDate.takeIf { it.isNotBlank() }?.let(LocalDate::parse) ?: dateOf(e.createdAt), e.spentMinutes, list, e.hardTopics, e.note, "Sonucu kaydet",
        ) { title, publisher, date, minutes, entries, hard, note ->
            scope.launch {
                r.saveExamResults(id, title, publisher, date, minutes, entries, hard, note)
                // Geri tuşu bitmiş denemenin formuna dönmesin.
                nav.popBackStack()
                nav.navigate("result/$id")
            }
        }
    }
}

/** Süre tutmadan sonuç ekleme. */
@Composable
fun ExternalExamScreen(kind: String, exam: String, subject: String, title: String) {
    val r = repo()
    val nav = LocalNav.current
    val scope = rememberUiScope()
    var e by rememberSaveable { mutableStateOf(exam.ifEmpty { "TYT" }) }
    Screen("Deneme sonucu ekle", { nav.popBackStack() }) {
        if (kind.isEmpty() || kind in listOf("TYT", "AYT", "YDT")) {
            Box(Modifier.padding(horizontal = Space.l)) { PillGroup(listOf("TYT" to "TYT", "AYT" to "AYT", "YDT" to "YDT"), e) { e = it } }
        }
        key(e) {
            ExamResultForm(e, kind.ifEmpty { "EXTERNAL" }, subject.ifEmpty { null }, title, "", today(), 0, emptyList(), "", "", "Kaydet") { t, publisher, date, minutes, rows, hard, note ->
                scope.launch {
                    val k = if (kind in listOf("BRANCH", "TOPIC", "PERSONAL", "CUSTOM")) kind else "EXTERNAL"
                    val id = r.saveExternalExam(t, publisher, date, e, minutes, rows, k, hard, note, subject.ifEmpty { null })
                    nav.popBackStack()
                    nav.navigate("result/$id")
                }
            }
        }
    }
}

@Composable
fun ResultScreen(id: String) {
    val r = repo()
    val nav = LocalNav.current
    val scope = rememberUiScope()
    var exam by remember { mutableStateOf<MockExam?>(null) }
    var results by remember { mutableStateOf<List<MockExamResult>>(emptyList()) }
    var previous by remember { mutableStateOf<MockSummary?>(null) }
    var hardNames by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var ask by remember { mutableStateOf(false) }
    val goals by r.user.netGoals().collectAsStateWithLifecycle(emptyList())
    LaunchedEffect(id) {
        val m = r.user.mock(id)
        exam = m
        results = r.user.results(id)
        val all = r.user.mockSummariesNow().filter { it.exam == m?.exam }
        val i = all.indexOfFirst { it.id == id }
        previous = if (i > 0) all[i - 1] else null
        val ids = m?.hardTopics.orEmpty().split(',').filter { it.isNotBlank() }
        if (ids.isNotEmpty()) hardNames = r.content.allTopics().filter { it.id in ids }.map { it.id to it.name }
    }
    Screen("Deneme sonucu", { if (!nav.popBackStack()) nav.navigate("exams") }, actions = {
        IconButton({ nav.navigate("examEntry/$id") }) { Icon(Icons.Rounded.Edit, "Düzenle") }
        IconButton({ ask = true }) { Icon(Icons.Rounded.DeleteOutline, "Denemeyi sil") }
    }) {
        val m = exam ?: return@Screen
        val c = results.sumOf { it.correct }
        val w = results.sumOf { it.wrong }
        val b = results.sumOf { it.blank }
        val net = results.sumOf { it.net }
        ScrollColumn {
            YCard {
                Text(m.title, style = MaterialTheme.typography.titleMedium, color = Yks.c.text)
                Muted(listOf(if (m.examDate.isNotBlank()) longDate(LocalDate.parse(m.examDate)) else shortDate(m.createdAt), m.publisher, if (m.spentMinutes > 0) "${m.spentMinutes} dk" else "").filter { it.isNotBlank() }.joinToString(" • "))
                Spacer(Modifier.height(Space.m))
                Text(Scoring.formatNet(net), style = MaterialTheme.typography.displaySmall.merge(NumberStyle), color = Yks.c.accentLight)
                Muted("Toplam net")
                previous?.let { p ->
                    val diff = net - p.net
                    val up = diff >= 0
                    Spacer(Modifier.height(Space.s))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (up) Icons.AutoMirrored.Rounded.TrendingUp else Icons.AutoMirrored.Rounded.TrendingDown, null, tint = if (up) Yks.c.success else Yks.c.warning, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Önceki denemeye göre ${if (up) "+" else ""}${Scoring.formatNet(diff)} net", style = MaterialTheme.typography.bodyMedium, color = if (up) Yks.c.success else Yks.c.warning)
                    }
                }
            }
            Row2({ Tile("$c", "Doğru", it, Yks.c.success) }, { Tile("$w", "Yanlış", it, Yks.c.error) })
            Tile("$b", "Boş")
            Section("Testlere göre")
            GroupCard {
                results.forEachIndexed { i, s ->
                    if (i > 0) Line()
                    val goal = goals.firstOrNull { it.exam == m.exam && it.section == s.section }
                    Row(Modifier.fillMaxWidth().padding(Space.l), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(s.section, style = MaterialTheme.typography.bodyLarge, color = Yks.c.text)
                            Muted("${s.correct} D • ${s.wrong} Y • ${s.blank} B" + (goal?.let { " • Hedef ${Scoring.formatNet(it.target)}" } ?: ""))
                        }
                        Text(Scoring.formatNet(s.net), style = MaterialTheme.typography.titleMedium.merge(NumberStyle), color = Yks.c.text)
                    }
                }
            }
            if (hardNames.isNotEmpty()) {
                Section("Zorlandığın konular")
                GroupCard { hardNames.forEachIndexed { i, (tid, n) -> if (i > 0) Line(); NavRow(n, "Pusula Koç bu konuyu önerilerine ekler", Icons.Rounded.Flag, Yks.c.warning, { nav.navigate("topic/$tid") }) } }
            }
            if (m.note.isNotBlank()) YCard { Muted("Notun"); Text(m.note, color = Yks.c.text) }
            MainButton("Yanlış kaydet", Modifier.fillMaxWidth(), icon = Icons.Rounded.AddCircleOutline) { nav.navigate("wrongEdit/new") }
            SoftButton("Deneme analizine git", Modifier.fillMaxWidth(), icon = Icons.Rounded.Insights) { nav.navigate("examAnalysis") }
        }
        if (ask) ConfirmDialog("Deneme silinsin mi?", "Bu deneme ve sonuçları istatistiklerinden kaldırılır.", "Sil", true, { ask = false }) {
            scope.launch { r.deleteExam(id); nav.popBackStack() }
        }
    }
}

/** Geçmiş: tarih, sınav türü, ders ve net aralığı filtreleri; iki deneme karşılaştırılabilir. */
@Composable
fun ExamHistoryScreen() {
    val r = repo()
    val nav = LocalNav.current
    val mocks by r.user.mockSummaries().collectAsStateWithLifecycle(emptyList())
    var exam by rememberSaveable { mutableStateOf("") }
    var period by rememberSaveable { mutableStateOf("ALL") }
    var minNet by rememberSaveable { mutableStateOf("") }
    var maxNet by rememberSaveable { mutableStateOf("") }
    var section by rememberSaveable { mutableStateOf("") }
    var sections by remember { mutableStateOf<Map<String, Set<String>>>(emptyMap()) }
    var picked by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(mocks) { sections = r.user.resultsNow().groupBy { it.mockExamId }.mapValues { e -> e.value.map { it.section }.toSet() } }
    val now = remember { System.currentTimeMillis() }
    val shown = mocks.filter { m ->
        (exam.isEmpty() || m.exam == exam) &&
            (period == "ALL" || now - m.createdAt <= period.toLong() * 86_400_000L) &&
            (minNet.toDoubleOrNull()?.let { m.net >= it } ?: true) && (maxNet.toDoubleOrNull()?.let { m.net <= it } ?: true) &&
            (section.isEmpty() || sections[m.id]?.contains(section) == true)
    }.reversed()
    Screen("Deneme geçmişi", { nav.popBackStack() }) {
        LazyColumn(contentPadding = PaddingValues(Space.l), verticalArrangement = Arrangement.spacedBy(Space.s)) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                    PillGroup(listOf("" to "Tümü", "TYT" to "TYT", "AYT" to "AYT", "YDT" to "YDT"), exam) { exam = it }
                    PillGroup(listOf("ALL" to "Tüm zamanlar", "30" to "30 gün", "90" to "90 gün"), period) { period = it }
                    Dropdown(sections.values.flatten().distinct().sorted().map { it to it }, section, "Tüm dersler") { section = it }
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                        NumberField(minNet, { minNet = it }, "En az net", Modifier.weight(1f), 3)
                        NumberField(maxNet, { maxNet = it }, "En çok net", Modifier.weight(1f), 3)
                    }
                    Muted("Karşılaştırmak için iki deneme seç.")
                    if (picked.size == 2) MainButton("Seçilenleri karşılaştır", Modifier.fillMaxWidth(), icon = Icons.Rounded.CompareArrows) { nav.navigate("examCompare/${picked[0]}/${picked[1]}") }
                }
            }
            if (shown.isEmpty()) item { EmptyStateView(Icons.Rounded.History, "Kayıt bulunamadı", "Filtreleri değiştirebilir veya yeni bir deneme ekleyebilirsin.") }
            items(shown, key = { it.id }) { m ->
                val on = m.id in picked
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(on, { picked = if (on) picked - m.id else (picked + m.id).takeLast(2) })
                    RecentExamCard(m.title, shortDate(m.createdAt), 0, Scoring.formatNet(m.net), Modifier.weight(1f)) { nav.navigate("result/${m.id}") }
                }
            }
        }
    }
}

@Composable
fun ExamCompareScreen(a: String, b: String) {
    val r = repo()
    val nav = LocalNav.current
    var ma by remember { mutableStateOf<MockExam?>(null) }
    var mb by remember { mutableStateOf<MockExam?>(null) }
    var ra by remember { mutableStateOf<List<MockExamResult>>(emptyList()) }
    var rb by remember { mutableStateOf<List<MockExamResult>>(emptyList()) }
    LaunchedEffect(a, b) { ma = r.user.mock(a); mb = r.user.mock(b); ra = r.user.results(a); rb = r.user.results(b) }
    Screen("Karşılaştırma", { nav.popBackStack() }) {
        val x = ma ?: return@Screen
        val y = mb ?: return@Screen
        ScrollColumn {
            Row2({ Tile(Scoring.formatNet(ra.sumOf { it.net }), x.title, it) }, { Tile(Scoring.formatNet(rb.sumOf { it.net }), y.title, it) })
            GroupCard {
                (ra.map { it.section } + rb.map { it.section }).distinct().forEachIndexed { i, s ->
                    if (i > 0) Line()
                    val p = ra.firstOrNull { it.section == s }
                    val q = rb.firstOrNull { it.section == s }
                    Row(Modifier.fillMaxWidth().padding(Space.m), verticalAlignment = Alignment.CenterVertically) {
                        Text(s, style = MaterialTheme.typography.bodyMedium, color = Yks.c.text, modifier = Modifier.weight(1f))
                        Text(p?.let { Scoring.formatNet(it.net) } ?: "–", style = MaterialTheme.typography.titleMedium.merge(NumberStyle), color = Yks.c.text)
                        Spacer(Modifier.width(Space.m))
                        Text(q?.let { Scoring.formatNet(it.net) } ?: "–", style = MaterialTheme.typography.titleMedium.merge(NumberStyle), color = Yks.c.accentLight)
                    }
                }
            }
        }
    }
}

@Composable
fun ExamAnalysisScreen() {
    val r = repo()
    val nav = LocalNav.current
    var exam by rememberSaveable { mutableStateOf("TYT") }
    val mocks by r.user.mockSummaries().collectAsStateWithLifecycle(emptyList())
    var a by remember { mutableStateOf<ExamAnalysis?>(null) }
    LaunchedEffect(exam, mocks) { a = r.examAnalysis(exam) }
    val list = mocks.filter { it.exam == exam }
    Screen("Deneme analizi", { nav.popBackStack() }) {
        ScrollColumn {
            PillGroup(listOf("TYT" to "TYT", "AYT" to "AYT", "YDT" to "YDT"), exam) { exam = it }
            val x = a
            if (x == null || x.examCount == 0) {
                EmptyStateView(Icons.Rounded.Insights, "Henüz analiz yok", "Bu sınav türünde deneme kaydettiğinde analiz burada oluşur.", "Deneme ekle") { nav.navigate("external") }
                return@ScrollColumn
            }
            Row2({ Tile("${x.examCount}", "Deneme", it) }, { Tile(x.avgMinutes?.let { m -> "$m dk" } ?: "–", "Ortalama süre", it) })
            Tile("%.1f".format(java.util.Locale("tr", "TR"), x.blankAverage), "Deneme başına boş")
            if (list.size >= 2) LineChart("Net gelişimi", list.map { shortDate(it.createdAt) }, list.map { it.net }, { Scoring.formatNet(it) })
            x.trendNote?.let { InfoBanner(it, Icons.AutoMirrored.Rounded.TrendingUp, Yks.c.accentLight) }
                ?: Muted("Eğilim yorumu için en az 3 deneme gerekir.")
            if (x.strong.isNotEmpty()) InfoBanner("Güçlü olduğun testler: ${x.strong.joinToString(", ")}", Icons.Rounded.CheckCircle, Yks.c.success)
            if (x.weak.isNotEmpty()) InfoBanner("Geliştirebileceğin testler: ${x.weak.joinToString(", ")}", Icons.Rounded.Flag, Yks.c.warning)
            Section("Testlere göre")
            GroupCard {
                x.sections.forEachIndexed { i, s ->
                    if (i > 0) Line()
                    Column(Modifier.padding(Space.l)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(s.section, style = MaterialTheme.typography.bodyLarge, color = Yks.c.text, modifier = Modifier.weight(1f))
                            Text(Scoring.formatNet(s.lastNet), style = MaterialTheme.typography.titleMedium.merge(NumberStyle), color = Yks.c.accentLight)
                        }
                        Muted("Ortalama ${Scoring.formatNet(s.avgNet)} • En iyi ${Scoring.formatNet(s.best)} • Doğruluk %${s.accuracy} • Boş %${s.blankRate}")
                    }
                }
            }
        }
    }
}

/** Testlere göre hedef net ve hedefe ulaşmak için gereken doğru sayısı. */
@Composable
fun NetGoalsScreen() {
    val r = repo()
    val nav = LocalNav.current
    val scope = rememberUiScope()
    val configs by r.user.examConfigs().collectAsStateWithLifecycle(emptyList())
    val goals by r.user.netGoals().collectAsStateWithLifecycle(emptyList())
    val mocks by r.user.mockSummaries().collectAsStateWithLifecycle(emptyList())
    var last by remember { mutableStateOf<Map<Pair<String, String>, MockExamResult>>(emptyMap()) }
    LaunchedEffect(mocks) {
        val m = HashMap<Pair<String, String>, MockExamResult>()
        mocks.forEach { x -> r.user.results(x.id).forEach { m[x.exam to it.section] = it } }
        last = m
    }
    Screen("Net hedeflerim", { nav.popBackStack() }) {
        ScrollColumn {
            Muted("Her test için hedef netini yaz. Son denemenle arasındaki farkı ve gereken doğru sayısını görürsün.")
            configs.sortedBy { it.code }.forEach { c ->
                Section(c.title)
                r.sectionsOf(c).forEach { s ->
                    val g = goals.firstOrNull { it.exam == c.code && it.section == s.name }
                    var text by remember(g) { mutableStateOf(g?.target?.let { Scoring.formatNet(it) }.orEmpty()) }
                    val cur = last[c.code to s.name]
                    val max = s.parts.sumOf { it.count }
                    val target = text.replace(',', '.').toDoubleOrNull()
                    YCard(padding = Space.m) {
                        Text(s.name, style = MaterialTheme.typography.titleMedium, color = Yks.c.text)
                        Spacer(Modifier.height(Space.s))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                            Field(text, { text = it.take(6) }, "Hedef net", Modifier.weight(1f), keyboard = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal))
                            SoftButton("Kaydet", enabled = target != null && target in 0.0..max.toDouble()) {
                                scope.launch { r.user.saveNetGoal(NetGoal(c.code, s.name, target!!)) }
                            }
                        }
                        if (cur != null && target != null) {
                            val need = Scoring.correctNeeded(target, cur.wrong, c.wrongDivisor)
                            Spacer(Modifier.height(4.dp))
                            Muted("Son net ${Scoring.formatNet(cur.net)}. ${cur.wrong} yanlışla hedef için en az $need doğru gerekir.")
                            ProgressBar(if (target > 0) (cur.net / target).toFloat() else 0f, height = 5.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExamSettingsScreen() {
    val r = repo()
    val nav = LocalNav.current
    val scope = rememberUiScope()
    val configs by r.user.examConfigs().collectAsStateWithLifecycle(emptyList())
    Screen("Sınav ayarları", { nav.popBackStack() }) {
        ScrollColumn {
            configs.sortedBy { it.code }.forEach { c ->
                var date by remember(c) { mutableStateOf(LocalDate.parse(c.examDate)) }
                var minutes by remember(c) { mutableStateOf(c.durationMin.toString()) }
                var divisor by remember(c) { mutableStateOf(c.wrongDivisor.toString()) }
                val valid = (minutes.toIntOrNull() ?: 0) > 0 && (divisor.toDoubleOrNull() ?: 0.0) > 0.0
                YCard {
                    Text("${c.title} • ${c.examYear}", style = MaterialTheme.typography.titleMedium, color = Yks.c.text)
                    Muted("${c.questionCount} soru. ${c.sourceNote}")
                    if (c.applicationStart.isNotBlank()) Muted("Başvuru: ${c.applicationStart} - ${c.applicationEnd} • Sonuç: ${c.resultDate}")
                    if (c.lastUpdated.isNotBlank()) Muted("Son güncelleme: ${c.lastUpdated}")
                    Spacer(Modifier.height(Space.s))
                    DateField(date, "Sınav tarihi") { date = it }
                    Spacer(Modifier.height(Space.s))
                    NumberField(minutes, { minutes = it }, "Süre (dakika)", max = 3)
                    Spacer(Modifier.height(Space.s))
                    Field(divisor, { divisor = it.filter { ch -> ch.isDigit() || ch == '.' }.take(4) }, "Kaç yanlış bir doğruyu götürür", keyboard = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal))
                    Spacer(Modifier.height(Space.s))
                    MainButton("Kaydet", Modifier.fillMaxWidth(), enabled = valid) {
                        scope.launch { r.user.saveExamConfig(c.copy(examDate = date.toString(), durationMin = minutes.toInt(), wrongDivisor = divisor.toDouble())) }
                    }
                }
            }
            Muted("Yeni yılın resmî kılavuzu yayımlandığında bu değerleri buradan veya bir içerik paketiyle güncelleyebilirsin.")
        }
    }
}

@Composable
fun ObpScreen() {
    val r = repo()
    val nav = LocalNav.current
    val scope = rememberUiScope()
    val s by r.settings.collectAsStateWithLifecycle(AppSettings())
    val configs by r.user.examConfigs().collectAsStateWithLifecycle(emptyList())
    val cfg = configs.firstOrNull { it.code == "TYT" }
    val mult = cfg?.obpMultiplier ?: 5.0
    val coef = cfg?.obpCoefficient ?: 0.12
    val placedCoef = cfg?.obpCoefficientPlaced ?: 0.06
    var diploma by remember(s.diploma) { mutableStateOf(s.diploma) }
    var placed by remember(s.placedLastYear) { mutableStateOf(s.placedLastYear) }
    val d = diploma.replace(',', '.').toDoubleOrNull()
    val ok = d != null && d in 50.0..100.0
    Screen("OBP hesaplama", { nav.popBackStack() }) {
        ScrollColumn {
            Field(diploma, { diploma = it.take(6) }, "Diploma notu (50-100)", keyboard = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Geçen yıl bir programa yerleştim", color = Yks.c.text, modifier = Modifier.weight(1f))
                Switch(placed, { placed = it })
            }
            if (ok) Row2({ Tile(Scoring.formatNet(Scoring.obp(d!!, mult)), "OBP", it) }, { Tile(Scoring.formatNet(Scoring.obpContribution(d!!, placed, mult, coef, placedCoef)), "Puana katkı", it, Yks.c.accentLight) })
            MainButton("Kaydet", Modifier.fillMaxWidth(), enabled = ok) { scope.launch { r.updateSettings { p -> p[Keys.diploma] = diploma; p[Keys.placed] = placed } } }
            YCard {
                Text("Kullanılan kural (${cfg?.examYear ?: 2027})", style = MaterialTheme.typography.titleMedium, color = Yks.c.text)
                Muted("OBP = diploma notu × ${Scoring.formatNet(mult)}. Katkı = OBP × ${Scoring.formatNet(coef)}. Önceki yıl bir programa yerleşenlerde katsayı ${Scoring.formatNet(placedCoef)} olarak alınır. Değerleri sınav yılının kılavuzuyla karşılaştırmalısın.")
                Spacer(Modifier.height(Space.s))
                Muted("Uygulama puan veya sıralama tahmini yapmaz. Bunun için her yılın resmî verileri gerekir.")
            }
        }
    }
}
