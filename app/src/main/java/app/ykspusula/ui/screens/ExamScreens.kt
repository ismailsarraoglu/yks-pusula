package app.ykspusula.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ykspusula.data.*
import app.ykspusula.domain.Scoring
import app.ykspusula.domain.Timers
import app.ykspusula.ui.LocalNav
import app.ykspusula.ui.components.*
import app.ykspusula.ui.longDate
import app.ykspusula.ui.repo
import app.ykspusula.ui.shortDate
import app.ykspusula.ui.theme.Yks
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun ExamsScreen() {
    val r = repo()
    val nav = LocalNav.current
    val mocks by r.user.mockSummaries().collectAsStateWithLifecycle(emptyList())
    val running by r.user.running().collectAsStateWithLifecycle(null)
    ScrollColumn(Modifier.statusBarsPadding()) {
        Text("Denemeler", style = MaterialTheme.typography.headlineMedium, color = Yks.c.text)
        running?.let { m ->
            YCard(onClick = { nav.navigate("exam/${m.id}") }) {
                Text("Devam eden deneme", style = MaterialTheme.typography.titleMedium, color = Yks.c.text)
                Muted("${m.title} • Kalan: ${Timers.clock(Timers.remainingSec(m.endsAt, System.currentTimeMillis()))}")
            }
        }
        Section("Yeni deneme")
        GroupCard {
            NavRow("TYT genel deneme", "Uygulamadaki sorulardan oluşturulur", Icons.Rounded.Assignment, onClick = { nav.navigate("examNew/TYT") }); Line()
            NavRow("AYT genel deneme", "Testleri alanına göre seçebilirsin", Icons.Rounded.Assignment, onClick = { nav.navigate("examNew/AYT") }); Line()
            NavRow("YDT genel deneme", "Seçili yabancı dilde", Icons.Rounded.Translate, onClick = { nav.navigate("examNew/YDT") }); Line()
            NavRow("Branş denemesi", "Tek bir dersten", Icons.Rounded.School, onClick = { nav.navigate("examNew/BRANCH") }); Line()
            NavRow("Konu denemesi", "Seçtiğin konudan", Icons.Rounded.Topic, onClick = { nav.navigate("examNew/TOPIC") }); Line()
            NavRow("Kişiselleştirilmiş deneme", "En zayıf konularından", Icons.Rounded.AutoAwesome, onClick = { nav.navigate("examNew/CUSTOM") })
        }
        GroupCard {
            NavRow("Dışarıda çözdüğüm denemeyi kaydet", "Kitapçık veya yayınevi denemeleri", Icons.Rounded.PostAdd, app.ykspusula.ui.theme.Yks.c.success, { nav.navigate("external") }); Line()
            NavRow("Sınav ayarları", "Tarih, süre, soru sayısı ve net kuralı", Icons.Rounded.Tune, Yks.c.muted, { nav.navigate("examSettings") }); Line()
            NavRow("OBP hesaplama", "Diploma notunun katkısı", Icons.Rounded.Calculate, Yks.c.muted, { nav.navigate("obp") })
        }
        Section("Geçmiş")
        if (mocks.isEmpty()) YCard { Muted("Henüz deneme yok. Çözdüğün denemeler burada listelenir.") }
        mocks.reversed().forEach { m ->
            YCard(onClick = { nav.navigate("result/${m.id}") }, padding = Space.m) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(m.title, style = MaterialTheme.typography.bodyLarge, color = Yks.c.text)
                        Muted("${shortDate(m.createdAt)} • ${m.correct} D, ${m.wrong} Y, ${m.blank} B")
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(Scoring.formatNet(m.net), style = MaterialTheme.typography.titleLarge, color = Yks.c.accent)
                        Muted("net")
                    }
                }
            }
        }
    }
}

@Composable
fun ExamNewScreen(kind: String) {
    val r = repo()
    val nav = LocalNav.current
    val scope = rememberCoroutineScope()
    val profile by r.user.profile().collectAsStateWithLifecycle(null)
    val subjects by r.content.subjects().collectAsStateWithLifecycle(emptyList())
    val configs by r.user.examConfigs().collectAsStateWithLifecycle(emptyList())
    val isGeneral = kind in listOf("TYT", "AYT", "YDT")
    val cfg = configs.firstOrNull { it.code == kind }
    var chosen by remember { mutableStateOf<Set<String>>(emptySet()) }
    var subject by rememberSaveable { mutableStateOf("") }
    var topic by rememberSaveable { mutableStateOf("") }
    var count by rememberSaveable { mutableStateOf("20") }
    var minutes by rememberSaveable { mutableStateOf("30") }
    var draft by remember { mutableStateOf<Repository.ExamDraft?>(null) }
    var message by remember { mutableStateOf("") }
    val p = profile
    LaunchedEffect(cfg?.code, p?.field) {
        if (kind == "AYT" && cfg != null && p != null) {
            chosen = when (p.field) {
                "SAY" -> setOf("Matematik", "Fen Bilimleri")
                "EA" -> setOf("Matematik", "Türk Dili ve Edebiyatı-Sosyal Bilimler-1")
                "SOZ" -> setOf("Türk Dili ve Edebiyatı-Sosyal Bilimler-1", "Sosyal Bilimler-2")
                else -> r.sectionsOf(cfg).map { it.name }.toSet()
            }
        }
    }
    val rows by remember(subject) { if (subject.isEmpty()) kotlinx.coroutines.flow.flowOf(emptyList()) else r.content.topicRows(subject) }.collectAsStateWithLifecycle(emptyList())
    val title = when (kind) { "BRANCH" -> "Branş denemesi"; "TOPIC" -> "Konu denemesi"; "CUSTOM" -> "Kişiselleştirilmiş deneme"; else -> "$kind genel denemesi" }

    fun prepare() {
        scope.launch {
            message = ""
            val c = count.toIntOrNull() ?: 20
            val m = minutes.toIntOrNull() ?: 30
            val d = when (kind) {
                "TYT", "AYT", "YDT" -> {
                    val ydt = if (kind == "YDT") subjects.firstOrNull { it.exam == "YDT" && it.language == p?.ydtLanguage && it.groupName == "Genel" }?.id else null
                    r.draftGeneral(kind, chosen, ydt)
                }
                "BRANCH" -> r.draftBranch(r.content.subject(subject) ?: return@launch, c, m)
                "TOPIC" -> r.draftTopics(rows.firstOrNull { it.id == topic }?.name.orEmpty() + " denemesi", "TOPIC", r.content.subject(subject)?.exam ?: "TYT", listOf(topic), c, m)
                else -> {
                    val weak = r.weakTopicIds(8)
                    if (weak.isEmpty()) { message = "Kişiselleştirilmiş deneme için önce en az 3 soru çözülmüş konu gerekir."; return@launch }
                    r.draftTopics("Kişiselleştirilmiş deneme", "CUSTOM", "TYT", weak, c, m)
                }
            }
            if (d.sections.sumOf { it.questionIds.size } == 0) { message = "Bu seçim için soru bankasında soru yok."; return@launch }
            draft = d
        }
    }

    Screen(title, { nav.popBackStack() }) {
        ScrollColumn {
            val d = draft
            if (d == null) {
                if (isGeneral && cfg != null) {
                    YCard {
                        Text(cfg.title, style = MaterialTheme.typography.titleMedium, color = Yks.c.text)
                        Muted("${cfg.questionCount} soru • ${cfg.durationMin} dakika")
                    }
                    if (kind == "AYT") {
                        Section("Testler")
                        val names = r.sectionsOf(cfg).map { it.name }
                        Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                            names.forEach { n ->
                                Pill(n, n in chosen) { chosen = if (n in chosen) chosen - n else chosen + n }
                            }
                        }
                    }
                }
                if (kind == "BRANCH" || kind == "TOPIC") {
                    Muted("Ders")
                    Dropdown(relevant(subjects, p).map { it.id to "${it.exam} ${it.name}" }, subject, "Ders seç") { subject = it; topic = "" }
                }
                if (kind == "TOPIC" && subject.isNotEmpty()) {
                    Muted("Konu")
                    Dropdown(rows.filter { it.questionCount > 0 }.map { it.id to "${it.name} (${it.questionCount})" }, topic, "Konu seç") { topic = it }
                }
                if (!isGeneral) {
                    Field(count, { count = it.filter(Char::isDigit).take(3) }, "Soru sayısı", keyboard = KeyboardOptions(keyboardType = KeyboardType.Number))
                    Field(minutes, { minutes = it.filter(Char::isDigit).take(3) }, "Süre (dakika)", keyboard = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
                if (message.isNotEmpty()) Text(message, color = Yks.c.error, style = MaterialTheme.typography.bodyMedium)
                val ok = when (kind) {
                    "BRANCH" -> subject.isNotEmpty(); "TOPIC" -> topic.isNotEmpty()
                    "AYT" -> chosen.isNotEmpty(); else -> true
                }
                MainButton("Devam", Modifier.fillMaxWidth(), enabled = ok) { prepare() }
            } else {
                val actual = d.sections.sumOf { it.questionIds.size }
                val requested = d.sections.sumOf { it.requested }
                YCard {
                    Text(d.title, style = MaterialTheme.typography.titleMedium, color = Yks.c.text)
                    Muted("$actual soru • ${d.minutes} dakika")
                    Spacer(Modifier.height(Space.s))
                    d.sections.forEach { s -> Muted("${s.name}: ${s.questionIds.size} / ${s.requested}") }
                }
                if (actual < requested) {
                    YCard {
                        Row {
                            Icon(Icons.Rounded.WarningAmber, null, tint = Yks.c.warning, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(Space.s))
                            Text("Soru bankasında bu deneme için yeterli soru yok. Deneme $actual soruyla oluşturuldu ve süre orantılı olarak ${d.minutes} dakikaya kısaltıldı.", style = MaterialTheme.typography.bodyMedium, color = Yks.c.text)
                        }
                    }
                }
                Muted("Süre başladıktan sonra uygulamadan çıksan da sayaç işlemeye devam eder; cevapların otomatik kaydedilir.")
                MainButton("Denemeyi başlat", Modifier.fillMaxWidth(), icon = Icons.Rounded.PlayArrow) {
                    scope.launch {
                        val id = r.startExam(d)
                        nav.navigate("exam/$id") { popUpTo("exams") }
                    }
                }
                SoftButton("Geri dön", Modifier.fillMaxWidth()) { draft = null }
            }
        }
    }
}

@Composable
fun ExamRunScreen(id: String) {
    val r = repo()
    val nav = LocalNav.current
    val scope = rememberCoroutineScope()
    var exam by remember { mutableStateOf<MockExam?>(null) }
    val records = remember { mutableStateListOf<AnswerRecord>() }
    var questions by remember { mutableStateOf<Map<String, QuestionFull>>(emptyMap()) }
    var index by rememberSaveable { mutableIntStateOf(0) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var gridOpen by remember { mutableStateOf(false) }
    var ask by remember { mutableStateOf(false) }
    var leave by remember { mutableStateOf(false) }
    var finishing by remember { mutableStateOf(false) }

    LaunchedEffect(id) {
        val m = r.user.mock(id)
        exam = m
        if (m == null || m.status != "RUNNING") { nav.navigate("result/$id") { popUpTo("exams") }; return@LaunchedEffect }
        val rec = r.user.records(id)
        records.clear(); records.addAll(rec)
        questions = rec.map { it.questionId }.chunked(400).flatMap { r.content.fullList(it) }.associateBy { it.q.id }
    }
    fun finish() {
        if (finishing) return
        finishing = true
        scope.launch {
            r.finishExam(id)
            nav.navigate("result/$id") { popUpTo("exams") }
        }
    }
    LaunchedEffect(exam) {
        val m = exam ?: return@LaunchedEffect
        while (true) {
            now = System.currentTimeMillis()
            if (now >= m.endsAt) { finish(); break }
            delay(1000)
        }
    }
    BackHandler { leave = true }

    val m = exam
    val rec = records.getOrNull(index)
    val q = rec?.let { questions[it.questionId] }
    val left = m?.let { Timers.remainingSec(it.endsAt, now) } ?: 0
    fun update(i: Int, v: AnswerRecord) {
        records[i] = v
        scope.launch { r.user.saveRecord(v) }
    }

    Scaffold(
        containerColor = Yks.c.bg,
        topBar = {
            Row(Modifier.statusBarsPadding().fillMaxWidth().padding(horizontal = Space.s, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton({ leave = true }) { Icon(Icons.Rounded.Close, "Denemeden çık") }
                Column(Modifier.weight(1f)) {
                    Text(Timers.clock(left), style = MaterialTheme.typography.titleLarge, color = if (left < 300) Yks.c.error else Yks.c.text, modifier = Modifier.semantics { contentDescription = "Kalan süre ${left / 60} dakika" })
                    Muted(rec?.sectionName.orEmpty(), maxLines = 1)
                }
                TextButton({ ask = true }) { Text("Sınavı bitir") }
            }
        },
        bottomBar = {
            if (records.isNotEmpty()) {
                Row(Modifier.navigationBarsPadding().fillMaxWidth().background(Yks.c.card).padding(Space.s), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    SoftButton("Önceki", Modifier.weight(1f), enabled = index > 0, icon = Icons.AutoMirrored.Rounded.ArrowBack) { index-- }
                    SoftButton("${index + 1}/${records.size}", icon = Icons.Rounded.GridView) { gridOpen = true }
                    SoftButton("Sonraki", Modifier.weight(1f), enabled = index < records.lastIndex, icon = Icons.AutoMirrored.Rounded.ArrowForward) { index++ }
                }
            }
        },
    ) { inner ->
        if (rec == null || q == null) {
            Box(Modifier.padding(inner).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            Column(Modifier.padding(inner).fillMaxSize().verticalScroll(rememberScrollState()).padding(Space.l), verticalArrangement = Arrangement.spacedBy(Space.m)) {
                Text("Soru ${index + 1}", style = MaterialTheme.typography.titleMedium, color = Yks.c.accent)
                MathText(q.q.stem, style = MaterialTheme.typography.bodyLarge, color = Yks.c.text)
                q.q.figure?.let { FigureView(it, Modifier.widthIn(max = 420.dp), color = Yks.c.text) }
                q.options.sortedBy { it.label }.forEach { o ->
                    OptionRow(o.label, o.text, rec.selected == o.label, false, false, true) {
                        update(index, rec.copy(selected = o.label, answeredAt = System.currentTimeMillis()))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    SoftButton("Boş bırak", Modifier.weight(1f), enabled = rec.selected != null, icon = Icons.Rounded.RadioButtonUnchecked) { update(index, rec.copy(selected = null, answeredAt = null)) }
                    SoftButton(if (rec.flagged) "İşareti kaldır" else "Soruyu işaretle", Modifier.weight(1f), icon = if (rec.flagged) Icons.Rounded.Flag else Icons.Rounded.OutlinedFlag) { update(index, rec.copy(flagged = !rec.flagged)) }
                }
            }
        }
    }

    if (gridOpen) {
        AlertDialog(
            onDismissRequest = { gridOpen = false }, containerColor = Yks.c.card, title = { Text("Soru tablosu") },
            text = {
                Column {
                    Muted("Dolu: cevaplandı • Bayrak: işaretli")
                    Spacer(Modifier.height(Space.s))
                    LazyVerticalGrid(GridCells.Adaptive(44.dp), Modifier.heightIn(max = 360.dp), verticalArrangement = Arrangement.spacedBy(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        itemsIndexed(records) { i, x ->
                            val tone = if (x.selected != null) Yks.c.accent else Yks.c.card
                            Box(
                                Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(tone)
                                    .border(if (i == index) 2.dp else 1.dp, if (x.flagged) Yks.c.warning else if (i == index) Yks.c.text else Yks.c.line, RoundedCornerShape(10.dp))
                                    .clickable { index = i; gridOpen = false }
                                    .semantics { contentDescription = "Soru ${i + 1}, " + (if (x.selected != null) "cevaplandı" else "boş") + if (x.flagged) ", işaretli" else "" },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("${i + 1}", color = if (x.selected != null) Color.White else Yks.c.text, style = MaterialTheme.typography.labelMedium)
                                if (x.flagged) Icon(Icons.Rounded.Flag, null, tint = Yks.c.warning, modifier = Modifier.size(12.dp).align(Alignment.TopEnd))
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton({ gridOpen = false }) { Text("Kapat") } },
        )
    }
    if (ask) {
        val blank = records.count { it.selected == null }
        ConfirmDialog("Sınav bitirilsin mi?", if (blank > 0) "$blank soru boş. Bitirdikten sonra cevapları değiştiremezsin." else "Bitirdikten sonra cevapları değiştiremezsin.", "Bitir", false, { ask = false }) { ask = false; finish() }
    }
    if (leave) {
        ConfirmDialog("Denemeden çıkılsın mı?", "Cevapların kayıtlı. Sayaç işlemeye devam eder; süre bitmeden Ana Sayfa veya Denemeler bölümünden geri dönebilirsin.", "Çık", false, { leave = false }) {
            leave = false
            nav.popBackStack()
        }
    }
}

@Composable
fun ResultScreen(id: String) {
    val r = repo()
    val nav = LocalNav.current
    val scope = rememberCoroutineScope()
    var exam by remember { mutableStateOf<MockExam?>(null) }
    var results by remember { mutableStateOf<List<MockExamResult>>(emptyList()) }
    var previous by remember { mutableStateOf<MockSummary?>(null) }
    var ask by remember { mutableStateOf(false) }
    LaunchedEffect(id) {
        val m = r.user.mock(id)
        exam = m
        results = r.user.results(id)
        val all = r.user.mockSummariesNow().filter { it.exam == m?.exam }
        val i = all.indexOfFirst { it.id == id }
        previous = if (i > 0) all[i - 1] else null
    }
    Screen("Deneme sonucu", { if (!nav.popBackStack()) nav.navigate("exams") }, actions = {
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
                Muted(listOf(if (m.examDate.isNotBlank()) longDate(LocalDate.parse(m.examDate)) else shortDate(m.createdAt), m.publisher).filter { it.isNotBlank() }.joinToString(" • "))
                Spacer(Modifier.height(Space.m))
                Text(Scoring.formatNet(net), style = MaterialTheme.typography.displaySmall, color = Yks.c.accent)
                Muted("Toplam net")
                previous?.let { p ->
                    val diff = net - p.net
                    val up = diff >= 0
                    Spacer(Modifier.height(Space.s))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (up) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown, null, tint = if (up) Yks.c.success else Yks.c.error, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Önceki denemeye göre ${if (up) "+" else ""}${Scoring.formatNet(diff)} net", style = MaterialTheme.typography.bodyMedium, color = if (up) Yks.c.success else Yks.c.error)
                    }
                }
            }
            Row2({ Tile("$c", "Doğru", it, Yks.c.success) }, { Tile("$w", "Yanlış", it, Yks.c.error) })
            Tile("$b", "Boş")
            if (results.size > 1) {
                val best = results.maxBy { it.net }
                val worst = results.minBy { it.net }
                Row2({ Tile(best.section, "En başarılı: ${Scoring.formatNet(best.net)} net", it) }, { Tile(worst.section, "En zayıf: ${Scoring.formatNet(worst.net)} net", it) })
            }
            Section("Testlere göre")
            GroupCard {
                results.forEachIndexed { i, s ->
                    if (i > 0) Line()
                    Row(Modifier.fillMaxWidth().padding(Space.l), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(s.section, style = MaterialTheme.typography.bodyLarge, color = Yks.c.text)
                            Muted("${s.correct} D • ${s.wrong} Y • ${s.blank} B")
                        }
                        Text(Scoring.formatNet(s.net), style = MaterialTheme.typography.titleMedium, color = Yks.c.text)
                    }
                }
            }
            if (!m.isExternal) MainButton("Cevapları incele", Modifier.fillMaxWidth(), icon = Icons.Rounded.Visibility) { nav.navigate("examReview/$id") }
        }
        if (ask) ConfirmDialog("Deneme silinsin mi?", "Bu deneme ve sonuçları istatistiklerinden kaldırılır.", "Sil", true, { ask = false }) {
            scope.launch { r.deleteExam(id); nav.popBackStack() }
        }
    }
}

@Composable
fun ExamReviewScreen(id: String) {
    val r = repo()
    val nav = LocalNav.current
    var records by remember { mutableStateOf<List<AnswerRecord>>(emptyList()) }
    var questions by remember { mutableStateOf<Map<String, QuestionFull>>(emptyMap()) }
    var filter by rememberSaveable { mutableStateOf("ALL") }
    LaunchedEffect(id) {
        records = r.user.records(id)
        questions = records.map { it.questionId }.chunked(400).flatMap { r.content.fullList(it) }.associateBy { it.q.id }
    }
    Screen("Cevaplar", { nav.popBackStack() }) {
        ScrollColumn {
            PillGroup(listOf("ALL" to "Tümü", "WRONG" to "Yanlış", "BLANK" to "Boş"), filter) { filter = it }
            records.forEachIndexed { i, rec ->
                val q = questions[rec.questionId] ?: return@forEachIndexed
                val right = rec.selected == q.correctLabel
                val show = filter == "ALL" || (filter == "BLANK" && rec.selected == null) || (filter == "WRONG" && rec.selected != null && !right)
                if (show) {
                    var open by remember { mutableStateOf(false) }
                    YCard(onClick = { open = !open }, padding = Space.m) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                when { rec.selected == null -> Icons.Rounded.RadioButtonUnchecked; right -> Icons.Rounded.CheckCircle; else -> Icons.Rounded.Cancel }, null,
                                tint = when { rec.selected == null -> Yks.c.muted; right -> Yks.c.success; else -> Yks.c.error }, modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(Space.s))
                            Text("Soru ${i + 1} • " + when { rec.selected == null -> "Boş"; right -> "Doğru"; else -> "Yanlış" } + " • ${rec.sectionName}", style = MaterialTheme.typography.labelLarge, color = Yks.c.text)
                        }
                        Spacer(Modifier.height(6.dp))
                        MathText(q.q.stem, style = MaterialTheme.typography.bodyMedium, color = Yks.c.text)
                        if (open) {
                            Spacer(Modifier.height(Space.s))
                            q.q.figure?.let { FigureView(it, Modifier.widthIn(max = 420.dp), color = Yks.c.text) }
                            q.options.sortedBy { it.label }.forEach { o ->
                                OptionRow(o.label, o.text, rec.selected == o.label, true, o.isCorrect, false) {}
                                Spacer(Modifier.height(6.dp))
                            }
                            SolutionCard(q) { nav.navigate("topic/${q.q.topicId}") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExternalExamScreen() {
    val r = repo()
    val nav = LocalNav.current
    val scope = rememberCoroutineScope()
    val configs by r.user.examConfigs().collectAsStateWithLifecycle(emptyList())
    var title by rememberSaveable { mutableStateOf("") }
    var publisher by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf(today().toString()) }
    var exam by rememberSaveable { mutableStateOf("TYT") }
    var spent by rememberSaveable { mutableStateOf("") }
    val values = remember(exam) { mutableStateMapOf<String, String>() }
    val cfg = configs.firstOrNull { it.code == exam }
    val sections = cfg?.let { r.sectionsOf(it) }.orEmpty()
    val divisor = cfg?.wrongDivisor ?: 4.0
    fun v(s: String, k: String) = values["$s|$k"]?.toIntOrNull() ?: 0
    val problems = sections.filter { s -> !Scoring.validSection(v(s.name, "c"), v(s.name, "w"), v(s.name, "b"), s.parts.sumOf { it.count }) }
    val totalNet = sections.sumOf { Scoring.net(v(it.name, "c"), v(it.name, "w"), divisor) }
    val parsedDate = runCatching { LocalDate.parse(date) }.getOrNull()
    Screen("Dış deneme", { nav.popBackStack() }) {
        ScrollColumn {
            Field(title, { title = it }, "Deneme adı")
            Field(publisher, { publisher = it }, "Yayınevi veya kaynak")
            Field(date, { date = it }, "Tarih (YYYY-AA-GG)")
            if (parsedDate == null) Text("Tarihi 2026-11-05 biçiminde yaz.", color = Yks.c.error, style = MaterialTheme.typography.bodyMedium)
            PillGroup(listOf("TYT" to "TYT", "AYT" to "AYT", "YDT" to "YDT"), exam) { exam = it }
            sections.forEach { s ->
                val max = s.parts.sumOf { it.count }
                YCard(padding = Space.m) {
                    Text("${s.name} ($max soru)", style = MaterialTheme.typography.titleMedium, color = Yks.c.text)
                    Spacer(Modifier.height(Space.s))
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                        listOf("c" to "Doğru", "w" to "Yanlış", "b" to "Boş").forEach { (k, label) ->
                            Field(values["${s.name}|$k"].orEmpty(), { values["${s.name}|$k"] = it.filter(Char::isDigit).take(3) }, label, Modifier.weight(1f), keyboard = KeyboardOptions(keyboardType = KeyboardType.Number))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    if (s in problems) Text("Toplam $max soruyu aşamaz.", color = Yks.c.error, style = MaterialTheme.typography.bodyMedium)
                    else Muted("Net: ${Scoring.formatNet(Scoring.net(v(s.name, "c"), v(s.name, "w"), divisor))}")
                }
            }
            Field(spent, { spent = it.filter(Char::isDigit).take(3) }, "Çalışma süresi (dakika)", keyboard = KeyboardOptions(keyboardType = KeyboardType.Number))
            YCard { Row { Text("Toplam net", color = Yks.c.text, modifier = Modifier.weight(1f)); Text(Scoring.formatNet(totalNet), style = MaterialTheme.typography.titleLarge, color = Yks.c.accent) } }
            MainButton("Kaydet", Modifier.fillMaxWidth(), enabled = title.isNotBlank() && problems.isEmpty() && parsedDate != null) {
                scope.launch {
                    val rows = sections.map { s -> Triple(s.name, intArrayOf(v(s.name, "c"), v(s.name, "w"), v(s.name, "b")), s.parts.sumOf { it.count }) }
                    val id = r.saveExternalExam(title.trim(), publisher.trim(), parsedDate!!, exam, spent.toIntOrNull() ?: 0, rows)
                    nav.navigate("result/$id") { popUpTo("exams") }
                }
            }
        }
    }
}

@Composable
fun ExamSettingsScreen() {
    val r = repo()
    val nav = LocalNav.current
    val scope = rememberCoroutineScope()
    val configs by r.user.examConfigs().collectAsStateWithLifecycle(emptyList())
    Screen("Sınav ayarları", { nav.popBackStack() }) {
        ScrollColumn {
            configs.sortedBy { it.code }.forEach { c ->
                var date by remember(c) { mutableStateOf(c.examDate) }
                var minutes by remember(c) { mutableStateOf(c.durationMin.toString()) }
                var divisor by remember(c) { mutableStateOf(c.wrongDivisor.toString()) }
                val valid = runCatching { LocalDate.parse(date) }.isSuccess && (minutes.toIntOrNull() ?: 0) > 0 && (divisor.toDoubleOrNull() ?: 0.0) > 0.0
                YCard {
                    Text(c.title, style = MaterialTheme.typography.titleMedium, color = Yks.c.text)
                    Muted("${c.questionCount} soru. ${c.sourceNote}")
                    if (c.applicationStart.isNotBlank()) Muted("Başvuru: ${c.applicationStart} - ${c.applicationEnd} • Sonuç: ${c.resultDate}")
                    Spacer(Modifier.height(Space.s))
                    Field(date, { date = it }, "Sınav tarihi (YYYY-AA-GG)")
                    Spacer(Modifier.height(Space.s))
                    Field(minutes, { minutes = it.filter(Char::isDigit).take(3) }, "Süre (dakika)", keyboard = KeyboardOptions(keyboardType = KeyboardType.Number))
                    Spacer(Modifier.height(Space.s))
                    Field(divisor, { divisor = it.filter { ch -> ch.isDigit() || ch == '.' }.take(4) }, "Kaç yanlış bir doğruyu götürür", keyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    Spacer(Modifier.height(Space.s))
                    MainButton("Kaydet", Modifier.fillMaxWidth(), enabled = valid) {
                        scope.launch { r.user.saveExamConfig(c.copy(examDate = date, durationMin = minutes.toInt(), wrongDivisor = divisor.toDouble())) }
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
    val scope = rememberCoroutineScope()
    val s by r.settings.collectAsStateWithLifecycle(AppSettings())
    var diploma by remember(s.diploma) { mutableStateOf(s.diploma) }
    var placed by remember(s.placedLastYear) { mutableStateOf(s.placedLastYear) }
    val d = diploma.replace(',', '.').toDoubleOrNull()
    val ok = d != null && d in 50.0..100.0
    Screen("OBP hesaplama", { nav.popBackStack() }) {
        ScrollColumn {
            Field(diploma, { diploma = it.take(6) }, "Diploma notu (50-100)", keyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Geçen yıl bir programa yerleştim", color = Yks.c.text, modifier = Modifier.weight(1f))
                Switch(placed, { placed = it })
            }
            if (ok) {
                Row2({ Tile("%.1f".format(Scoring.obp(d!!)), "OBP", it) }, { Tile("%.2f".format(Scoring.obpContribution(d!!, placed)), "Puana katkı", it, Yks.c.accent) })
            }
            MainButton("Kaydet", Modifier.fillMaxWidth(), enabled = ok) {
                scope.launch { r.updateSettings { p -> p[Keys.diploma] = diploma; p[Keys.placed] = placed } }
            }
            YCard {
                Text("Kullanılan kural", style = MaterialTheme.typography.titleMedium, color = Yks.c.text)
                Muted("OBP = diploma notu × 5. Katkı = OBP × 0,12. Önceki yıl bir programa yerleşenlerde katsayı 0,06 olarak alınır. Sınav yılının kılavuzuyla karşılaştırmalısın.")
                Spacer(Modifier.height(Space.s))
                Muted("Uygulama puan veya sıralama tahmini yapmaz. Bunun için her yılın resmî standart puan verileri gerekir ve bu veriler uygulamada yoktur.")
            }
        }
    }
}
