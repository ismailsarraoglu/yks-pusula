package app.ykspusula.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import app.ykspusula.data.*
import app.ykspusula.domain.Scoring
import app.ykspusula.ui.LocalNav
import app.ykspusula.ui.components.*
import app.ykspusula.ui.repo
import app.ykspusula.ui.shortDate
import app.ykspusula.ui.solve
import app.ykspusula.ui.theme.Yks
import kotlinx.coroutines.launch

val STATUS_LABELS = listOf("Henüz başlanmadı", "Çalışılıyor", "Soru çözülüyor", "Tamamlandı")
val WRONG_REASONS = listOf("Konu eksiği", "Dikkatsizlik", "İşlem hatası", "Soruyu yanlış anlama", "Zaman yetersizliği", "Yanlış yöntem")

@Composable fun statusColor(s: Int): Color = when (s) { 3 -> Yks.c.success; 2 -> Yks.c.accent; 1 -> Yks.c.warning; else -> Yks.c.muted }

/** Öğrencinin alanına ve diline uyan dersleri seçer. */
fun relevant(subjects: List<Subject>, p: UserProfile?): List<Subject> = subjects.filter { s ->
    s.language == null || s.language == (p?.ydtLanguage ?: "İngilizce")
}

@Composable
fun StudyScreen() {
    val r = repo()
    val nav = LocalNav.current
    val profile by r.user.profile().collectAsStateWithLifecycle(null)
    val subjects by r.content.subjects().collectAsStateWithLifecycle(emptyList())
    val boards by r.content.subjectBoards().collectAsStateWithLifecycle(emptyList())
    var exam by rememberSaveable { mutableStateOf("TYT") }
    val list = relevant(subjects, profile).filter { it.exam == exam }
    val grouped = list.groupBy { it.groupName }
    val y = profile?.examYear ?: 2027
    val b = boards.associateBy { it.subjectId }

    ScrollColumn(Modifier.statusBarsPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Çalış", style = MaterialTheme.typography.headlineMedium, color = Yks.c.text, modifier = Modifier.weight(1f))
            IconButton({ nav.navigate("search") }) { Icon(Icons.Rounded.Search, "Ara", tint = Yks.c.text) }
        }
        QuickGrid(
            listOf(
                Triple("Soru bankası", Icons.Rounded.EditNote, "bank"), Triple("Yanlışlar", Icons.Rounded.ErrorOutline, "wrongs"),
                Triple("Kartlar", Icons.Rounded.Style, "cards"), Triple("Formüller", Icons.Rounded.Functions, "formulas"),
                Triple("Notlar", Icons.Rounded.StickyNote2, "notes"), Triple("Kaynaklar", Icons.Rounded.PictureAsPdf, "library"),
            )
        ) { nav.navigate(it) }
        if (y >= 2028) {
            YCard {
                Row {
                    Icon(Icons.Rounded.Info, null, tint = Yks.c.warning, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(Space.s))
                    Text("İçerikler 2027 YKS çerçevesine göre hazırlandı. $y sınavı için Maarif Modeli'ne göre güncellenmiş kapsam ÖSYM ve MEB tarafından açıklandığında yeni içerik paketi gerekir.", style = MaterialTheme.typography.bodyMedium, color = Yks.c.text)
                }
            }
        }
        PillGroup(listOf("TYT" to "TYT", "AYT" to "AYT", "YDT" to "YDT"), exam) { exam = it }
        if (exam == "YDT") {
            Muted("Seçili dil: ${profile?.ydtLanguage ?: "İngilizce"}. Dili Profil'den değiştirebilirsin.")
        }
        if (list.isEmpty()) {
            YCard { Muted("Bu dil için henüz içerik yok. Bir içerik paketiyle eklenebilir.") }
        }
        grouped.forEach { (group, subs) ->
            if (grouped.size > 1) Section(group)
            GroupCard {
                subs.forEachIndexed { i, s ->
                    if (i > 0) Line()
                    val st = b[s.id]
                    val pct = st?.let { Scoring.percent(it.done, it.total) } ?: 0
                    Column(Modifier.clickable { nav.navigate("subject/${s.id}") }.padding(horizontal = Space.l, vertical = Space.m)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(s.name, style = MaterialTheme.typography.bodyLarge, color = Yks.c.text, modifier = Modifier.weight(1f))
                            Muted("%$pct")
                        }
                        Spacer(Modifier.height(6.dp))
                        ProgressBar(pct / 100f, height = 5.dp)
                    }
                }
            }
        }
        Section("Konu takibi")
        GroupCard { NavRow("Tüm dersler", "Konu durumlarını ders ders gör", Icons.Rounded.Checklist, onClick = { nav.navigate("tracker") }) }
    }
}

@Composable
fun SubjectScreen(id: String) {
    val r = repo()
    val nav = LocalNav.current
    val rows by r.content.topicRows(id).collectAsStateWithLifecycle(emptyList())
    var name by remember { mutableStateOf("") }
    LaunchedEffect(id) { name = r.content.subject(id)?.name.orEmpty() }
    val inScope = rows.filter { it.scope != "EXCLUDED" }
    Screen(name, { nav.popBackStack() }) {
        LazyColumn(contentPadding = PaddingValues(Space.l), verticalArrangement = Arrangement.spacedBy(Space.m)) {
            item {
                YCard {
                    val done = inScope.count { it.status == 3 }
                    val active = inScope.count { it.status == 1 || it.status == 2 }
                    val idle = inScope.size - done - active
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${inScope.size} konu", style = MaterialTheme.typography.titleMedium, color = Yks.c.text)
                            Muted("Tamamlanan: $done")
                            Muted("Çalışılan: $active")
                            Muted("Başlanmamış: $idle")
                        }
                        val pct = Scoring.percent(done, inScope.size)
                        Ring(pct / 100f, "%$pct", description = "$name ilerlemesi yüzde $pct")
                    }
                    Spacer(Modifier.height(Space.m))
                    SoftButton("Bu dersten soru çöz", Modifier.fillMaxWidth(), icon = Icons.Rounded.EditNote) { nav.solve(subject = id) }
                }
            }
            items(rows, key = { it.id }) { t ->
                YCard(onClick = { nav.navigate("topic/${t.id}") }, padding = Space.m) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(t.name, style = MaterialTheme.typography.bodyLarge, color = if (t.scope == "EXCLUDED") Yks.c.muted else Yks.c.text)
                            val success = if (t.total > 0) " • %${Scoring.percent(t.right, t.total)} başarı" else ""
                            Muted(STATUS_LABELS[t.status ?: 0] + " • ${t.questionCount} soru" + success, maxLines = 1)
                        }
                        when {
                            t.scope == "EXCLUDED" -> Tag("Kapsam dışı", Yks.c.muted)
                            t.scope == "REVIEW" -> Tag("Kontrol", Yks.c.warning)
                            else -> Box(Modifier.size(10.dp).clip(CircleShape).background(statusColor(t.status ?: 0)))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TopicScreen(id: String) {
    val r = repo()
    val nav = LocalNav.current
    val scope = rememberCoroutineScope()
    val topic by r.content.topic(id).collectAsStateWithLifecycle(null)
    val progress by r.user.progress(id).collectAsStateWithLifecycle(null)
    val t = topic
    Screen(t?.name.orEmpty(), { nav.popBackStack() }) {
        if (t == null) return@Screen
        val rows by r.content.topicRows(t.subjectId).collectAsStateWithLifecycle(emptyList())
        val row = rows.firstOrNull { it.id == id }
        ScrollColumn {
            if (t.scope != "IN") {
                YCard {
                    Row {
                        Icon(Icons.Rounded.WarningAmber, null, tint = Yks.c.warning, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(Space.s))
                        Text(t.scopeNote, style = MaterialTheme.typography.bodyMedium, color = Yks.c.text)
                    }
                }
            }
            YCard {
                Muted("Konu durumu")
                Spacer(Modifier.height(Space.s))
                PillGroup(STATUS_LABELS.mapIndexed { i, s -> i.toString() to s }, (progress?.status ?: 0).toString()) { v -> scope.launch { r.setTopicStatus(id, v.toInt()) } }
                Spacer(Modifier.height(Space.m))
                Line()
                Spacer(Modifier.height(Space.s))
                val total = row?.total ?: 0
                Muted(if (total > 0) "Başarı oranı: %${Scoring.percent(row?.right ?: 0, total)} ($total soru)" else "Başarı oranı: henüz soru çözülmedi")
                Muted("Son çalışma: " + (progress?.lastStudiedAt?.let(::shortDate) ?: "yok"))
                Muted("Tekrar tarihi: " + (progress?.nextReviewAt?.let(::shortDate) ?: "planlanmadı"))
                Spacer(Modifier.height(Space.s))
                Muted("Tekrarı planla")
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    SoftButton("Yarın", Modifier.weight(1f)) { scope.launch { r.setTopicReview(id, startOf(today().plusDays(1))) } }
                    SoftButton("7 gün", Modifier.weight(1f)) { scope.launch { r.setTopicReview(id, startOf(today().plusDays(7))) } }
                }
            }
            val count = row?.questionCount ?: 0
            MainButton(if (count > 0) "$count soru çöz" else "Bu konuda henüz soru yok", Modifier.fillMaxWidth(), enabled = count > 0, icon = Icons.Rounded.EditNote) { nav.solve(topic = id) }
            if (t.summary.isNotBlank()) Block("Kısa özet", t.summary)
            if (t.lesson.isNotBlank()) Block("Konu anlatımı", t.lesson)
            if (t.keyPoints.isNotBlank()) Block("Formüller ve önemli bilgiler", t.keyPoints)
            if (t.example.isNotBlank()) Block("Örnek çözüm", t.example)
            if (t.lesson.isBlank()) YCard { Muted("Bu konunun ayrıntılı anlatımı henüz eklenmedi.") }
            if (t.example.isBlank()) YCard { Muted("Bu konu için örnek çözüm henüz eklenmedi.") }
            Muted("Müfredat sürümü: ${t.curriculum} • Geçerlilik: ${t.validFromYear}-${t.validToYear}")
        }
    }
}

@Composable private fun Block(title: String, text: String) {
    YCard {
        Text(title, style = MaterialTheme.typography.titleMedium, color = Yks.c.text)
        Spacer(Modifier.height(Space.s))
        text.split("\n").filter { it.isNotBlank() }.forEach { line ->
            MathText(line, Modifier.padding(bottom = 6.dp), style = MaterialTheme.typography.bodyLarge, color = Yks.c.text)
        }
    }
}

@Composable
fun TrackerScreen() {
    val r = repo()
    val nav = LocalNav.current
    val profile by r.user.profile().collectAsStateWithLifecycle(null)
    val subjects by r.content.subjects().collectAsStateWithLifecycle(emptyList())
    val boards by r.content.subjectBoards().collectAsStateWithLifecycle(emptyList())
    val b = boards.associateBy { it.subjectId }
    Screen("Konu takibi", { nav.popBackStack() }) {
        LazyColumn(contentPadding = PaddingValues(Space.l), verticalArrangement = Arrangement.spacedBy(Space.m)) {
            val list = relevant(subjects, profile)
            items(list, key = { it.id }) { s ->
                val st = b[s.id] ?: SubjectBoard(s.id, 0, 0, 0, 0)
                val active = st.studying + st.solving
                val idle = st.total - st.done - active
                YCard(onClick = { nav.navigate("subject/${s.id}") }, padding = Space.m) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${s.exam} ${s.name}", style = MaterialTheme.typography.titleMedium, color = Yks.c.text, modifier = Modifier.weight(1f))
                        Text("%${Scoring.percent(st.done, st.total)}", style = MaterialTheme.typography.titleMedium, color = Yks.c.accent)
                    }
                    Spacer(Modifier.height(6.dp))
                    SplitBar(st.done, active, idle)
                    Spacer(Modifier.height(6.dp))
                    Muted("${st.total} konu • ${st.done} tamamlandı • $active çalışılıyor • $idle başlanmadı")
                }
            }
        }
    }
}

@Composable
fun SplitBar(done: Int, active: Int, idle: Int) {
    Row(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Yks.c.line)) {
        if (done > 0) Box(Modifier.weight(done.toFloat()).fillMaxHeight().background(Yks.c.success))
        if (active > 0) Box(Modifier.weight(active.toFloat()).fillMaxHeight().background(Yks.c.accent))
        if (idle > 0) Spacer(Modifier.weight(idle.toFloat()))
    }
}

/* ---------- SORU BANKASI ---------- */

@Composable
fun BankScreen() {
    val r = repo()
    val nav = LocalNav.current
    val profile by r.user.profile().collectAsStateWithLifecycle(null)
    val subjects by r.content.subjects().collectAsStateWithLifecycle(emptyList())
    var exam by rememberSaveable { mutableStateOf("") }
    var subject by rememberSaveable { mutableStateOf("") }
    var topic by rememberSaveable { mutableStateOf("") }
    var diff by rememberSaveable { mutableStateOf("0") }
    var state by rememberSaveable { mutableStateOf("ALL") }
    var shuffle by rememberSaveable { mutableStateOf(false) }
    val rows by remember(subject) { if (subject.isEmpty()) kotlinx.coroutines.flow.flowOf(emptyList()) else r.content.topicRows(subject) }.collectAsStateWithLifecycle(emptyList())
    val filter = QuestionFilter(exam, subject, topic, diff.toInt(), state)
    val paged = remember(exam, subject, topic, diff, state) { r.pagedQuestions(filter) }.collectAsLazyPagingItems()
    var count by remember { mutableIntStateOf(0) }
    LaunchedEffect(exam, subject, topic, diff, state) { count = r.questionIds(filter, 0, 0).size }
    val subs = relevant(subjects, profile).filter { exam.isEmpty() || it.exam == exam }

    Screen("Soru bankası", { nav.popBackStack() }) {
        LazyColumn(contentPadding = PaddingValues(Space.l), verticalArrangement = Arrangement.spacedBy(Space.m)) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                    Muted("Sınav türü")
                    PillGroup(listOf("" to "Tümü", "TYT" to "TYT", "AYT" to "AYT", "YDT" to "YDT"), exam) { exam = it; subject = ""; topic = "" }
                    Muted("Ders")
                    Dropdown(subs.map { it.id to (if (exam.isEmpty()) "${it.exam} ${it.name}" else it.name) }, subject, "Tüm dersler") { subject = it; topic = "" }
                    if (subject.isNotEmpty()) {
                        Muted("Konu")
                        Dropdown(rows.map { it.id to "${it.name} (${it.questionCount})" }, topic, "Tüm konular") { topic = it }
                    }
                    Muted("Zorluk")
                    PillGroup(listOf("0" to "Tümü", "1" to "Kolay", "2" to "Orta", "3" to "Zor"), diff) { diff = it }
                    Muted("Durum")
                    PillGroup(
                        listOf("ALL" to "Tümü", "UNSOLVED" to "Çözülmeyen", "SOLVED" to "Çözülen", "WRONG" to "Yanlış yapılan", "MARKED" to "İşaretli", "LATER" to "Sonra çöz"),
                        state,
                    ) { state = it }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Soruları ve şıkları karıştır", color = Yks.c.text, modifier = Modifier.weight(1f))
                        Switch(shuffle, { shuffle = it })
                    }
                    MainButton("$count soruyu çöz", Modifier.fillMaxWidth(), enabled = count > 0, icon = Icons.Rounded.PlayArrow) {
                        nav.solve(exam = exam, subject = subject, topic = topic, diff = diff.toInt(), state = state, seed = if (shuffle) System.currentTimeMillis() else 0L, mix = shuffle)
                    }
                    if (count == 0) Muted("Bu filtreyle eşleşen soru yok.")
                }
            }
            items(paged.itemCount, key = { i -> paged.peek(i)?.id ?: i }) { i ->
                val q = paged[i] ?: return@items
                YCard(padding = Space.m) {
                    Text(q.stem.replace(Regex("[\\\\{}^_]"), " ").take(120), style = MaterialTheme.typography.bodyMedium, color = Yks.c.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(4.dp))
                    Muted("${q.exam} • " + listOf("", "Kolay", "Orta", "Zor")[q.difficulty])
                }
            }
        }
    }
}

@Composable
fun Dropdown(options: List<Pair<String, String>>, selected: String, allLabel: String, onSelect: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val label = options.firstOrNull { it.first == selected }?.second ?: allLabel
    Box {
        SoftButton(label, Modifier.fillMaxWidth(), color = Yks.c.text, icon = Icons.Rounded.ExpandMore) { open = true }
        DropdownMenu(open, { open = false }, containerColor = Yks.c.card) {
            DropdownMenuItem({ Text(allLabel) }, { onSelect(""); open = false })
            options.forEach { (id, name) -> DropdownMenuItem({ Text(name) }, { onSelect(id); open = false }) }
        }
    }
}

/* ---------- SORU EKRANI ---------- */

data class Picked(val label: String, val checked: Boolean)

@Composable
fun SolveScreen(mode: String, exam: String, subject: String, topic: String, diff: Int, state: String, seed: Long, limit: Int, mix: Boolean) {
    val r = repo()
    val nav = LocalNav.current
    val scope = rememberCoroutineScope()
    var ids by remember { mutableStateOf<List<String>?>(null) }
    LaunchedEffect(Unit) {
        ids = if (mode == "REVIEW") r.user.wrongDueIds(System.currentTimeMillis())
        else r.questionIds(QuestionFilter(exam, subject, topic, diff, state), seed, limit)
    }
    val list = ids
    var index by rememberSaveable { mutableIntStateOf(0) }
    val answers = remember { mutableStateMapOf<String, Picked>() }
    var showSolution by remember { mutableStateOf(false) }
    var noteOpen by remember { mutableStateOf(false) }
    var q by remember { mutableStateOf<QuestionFull?>(null) }
    val marked by r.user.bookmarkIds("QUESTION").collectAsStateWithLifecycle(emptyList())
    val later by r.user.bookmarkIds("LATER").collectAsStateWithLifecycle(emptyList())
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(list, index) {
        val id = list?.getOrNull(index) ?: return@LaunchedEffect
        q = r.content.full(id)
        showSolution = answers[id]?.checked == true
    }

    Scaffold(
        containerColor = Yks.c.bg,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Row(Modifier.statusBarsPadding().fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton({ nav.popBackStack() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Geri", tint = Yks.c.text) }
                Text(if (list.isNullOrEmpty()) "Soru" else "Soru ${index + 1} / ${list.size}", style = MaterialTheme.typography.titleMedium, color = Yks.c.text, modifier = Modifier.weight(1f))
                val cur = q
                if (cur != null) {
                    val m = cur.q.id in marked
                    IconButton({ scope.launch { r.toggleBookmark("QUESTION", cur.q.id, !m) } }) {
                        Icon(if (m) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder, if (m) "İşareti kaldır" else "Soruyu işaretle", tint = if (m) Yks.c.accent else Yks.c.text)
                    }
                    val l = cur.q.id in later
                    IconButton({ scope.launch { r.toggleBookmark("LATER", cur.q.id, !l); snackbar.showSnackbar(if (l) "Sonra çöz listesinden çıkarıldı" else "Sonra çöz listesine eklendi") } }) {
                        Icon(Icons.Rounded.Schedule, if (l) "Sonra çöz listesinden çıkar" else "Daha sonra çöz", tint = if (l) Yks.c.accent else Yks.c.text)
                    }
                }
            }
        },
        bottomBar = {
            if (!list.isNullOrEmpty()) {
                Row(Modifier.navigationBarsPadding().fillMaxWidth().background(Yks.c.card).padding(Space.m), horizontalArrangement = Arrangement.spacedBy(Space.m)) {
                    SoftButton("Önceki", Modifier.weight(1f), enabled = index > 0, icon = Icons.AutoMirrored.Rounded.ArrowBack) { index-- }
                    SoftButton("Sonraki", Modifier.weight(1f), enabled = index < list.lastIndex, icon = Icons.AutoMirrored.Rounded.ArrowForward) { index++ }
                }
            }
        },
    ) { inner ->
        val cur = q
        when {
            list == null -> Box(Modifier.padding(inner).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            list.isEmpty() -> Box(Modifier.padding(inner)) {
                Empty(Icons.Rounded.CheckCircle, if (mode == "REVIEW") "Tekrar edilecek yanlış yok" else "Soru bulunamadı", if (mode == "REVIEW") "Tekrar zamanı gelen yanlışların burada görünür." else "Filtreleri değiştirerek yeniden deneyebilirsin.")
            }
            cur != null && cur.q.id == list.getOrNull(index) -> {
                val pick = answers[cur.q.id]
                val options = remember(cur.q.id, mix, seed) {
                    val sorted = cur.options.sortedBy { it.label }
                    if (mix) sorted.shuffled(java.util.Random(seed xor cur.q.id.hashCode().toLong())) else sorted
                }
                val letters = listOf("A", "B", "C", "D", "E")
                Column(Modifier.padding(inner).fillMaxSize().verticalScroll(rememberScrollState()).padding(Space.l), verticalArrangement = Arrangement.spacedBy(Space.m)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Tag(cur.q.exam)
                        Tag(listOf("", "Kolay", "Orta", "Zor")[cur.q.difficulty], Yks.c.muted)
                    }
                    MathText(cur.q.stem, style = MaterialTheme.typography.bodyLarge, color = Yks.c.text)
                    cur.q.figure?.let { FigureView(it, Modifier.padding(vertical = Space.s).widthIn(max = 420.dp), color = Yks.c.text) }
                    options.forEachIndexed { i, o ->
                        OptionRow(letters[i], o.text, pick?.label == o.label, pick?.checked == true, o.isCorrect, enabled = pick?.checked != true) {
                            answers[cur.q.id] = Picked(o.label, false)
                        }
                    }
                    if (pick?.checked != true) {
                        MainButton("Cevabı kontrol et", Modifier.fillMaxWidth(), enabled = pick != null, icon = Icons.Rounded.Check) {
                            val p = pick ?: return@MainButton
                            answers[cur.q.id] = p.copy(checked = true)
                            showSolution = true
                            scope.launch {
                                val src = if (mode == "REVIEW") "REVIEW" else "BANK"
                                r.answer(cur, p.label, src, cur.options.first { it.label == p.label }.text)
                            }
                        }
                    } else {
                        val right = pick.label == cur.correctLabel
                        Verdict(right)
                        if (!showSolution) SoftButton("Açıklamayı göster", Modifier.fillMaxWidth()) { showSolution = true }
                        else SolutionCard(cur, onTopic = { nav.navigate("topic/${cur.q.topicId}") })
                        if (!right) ReasonPicker(cur.q.id)
                        Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                            SoftButton("Çözüme ekle", Modifier.weight(1f), icon = Icons.Rounded.NoteAdd) { noteOpen = true }
                            if (right) SoftButton("Yanlışlara ekle", Modifier.weight(1f), icon = Icons.Rounded.ErrorOutline) {
                                scope.launch { r.addToWrongBook(cur); snackbar.showSnackbar("Yanlışlar defterine eklendi") }
                            }
                        }
                    }
                    Muted("Kaynak: ${cur.q.source}")
                }
                if (noteOpen) {
                    var text by remember { mutableStateOf("") }
                    AlertDialog(
                        onDismissRequest = { noteOpen = false }, containerColor = Yks.c.card, title = { Text("Çözüm notu") },
                        text = { Field(text, { text = it }, "Kendi çözümün", singleLine = false, minLines = 4) },
                        confirmButton = {
                            TextButton({
                                val now = System.currentTimeMillis()
                                scope.launch {
                                    r.user.saveNote(UserNote(title = "Çözüm: " + cur.q.stem.take(50), body = text, subjectId = cur.q.subjectId, topicId = cur.q.topicId, createdAt = now, updatedAt = now))
                                    snackbar.showSnackbar("Notlara eklendi")
                                }
                                noteOpen = false
                            }, enabled = text.isNotBlank()) { Text("Kaydet") }
                        },
                        dismissButton = { TextButton({ noteOpen = false }) { Text("Vazgeç") } },
                    )
                }
            }
            else -> Box(Modifier.padding(inner).fillMaxSize())
        }
    }
}

@Composable
fun OptionRow(letter: String, text: String, selected: Boolean, reveal: Boolean, correct: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val c = Yks.c
    val tone = when {
        reveal && correct -> c.success
        reveal && selected -> c.error
        selected -> c.accent
        else -> null
    }
    val state = when {
        reveal && correct -> "Doğru cevap"
        reveal && selected -> "Senin cevabın, yanlış"
        selected -> "Seçildi"
        else -> "Seçilmedi"
    }
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(tone?.copy(alpha = 0.1f) ?: c.card).border(if (tone != null) 1.5.dp else 1.dp, tone ?: c.line, shape)
            .selectable(selected, enabled = enabled, role = Role.RadioButton, onClick = onClick).defaultMinSize(minHeight = 52.dp).padding(horizontal = Space.m, vertical = 10.dp)
            .semantics { stateDescription = state },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(30.dp).clip(CircleShape).background(tone ?: c.line), contentAlignment = Alignment.Center) {
            Text(letter, style = MaterialTheme.typography.labelLarge, color = if (tone != null) Color.White else c.text)
        }
        Spacer(Modifier.width(Space.m))
        MathText(text, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = c.text)
        if (reveal && (correct || selected)) {
            Icon(if (correct) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel, null, tint = tone ?: c.text, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
fun Verdict(right: Boolean) {
    val color = if (right) Yks.c.success else Yks.c.error
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(color.copy(alpha = 0.12f)).padding(Space.m), verticalAlignment = Alignment.CenterVertically) {
        Icon(if (right) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel, null, tint = color)
        Spacer(Modifier.width(Space.s))
        Text(if (right) "Doğru cevap" else "Yanlış cevap • Yanlışlar defterine eklendi", style = MaterialTheme.typography.titleMedium, color = color)
    }
}

@Composable
fun SolutionCard(q: QuestionFull, onTopic: (() -> Unit)?) {
    val correct = q.options.first { it.isCorrect }
    YCard {
        Text("Doğru cevap", style = MaterialTheme.typography.labelMedium, color = Yks.c.muted)
        MathText(correct.text, style = MaterialTheme.typography.titleMedium, color = Yks.c.success)
        Spacer(Modifier.height(Space.s))
        Text("Açıklama", style = MaterialTheme.typography.labelMedium, color = Yks.c.muted)
        MathText(q.q.explanation, style = MaterialTheme.typography.bodyLarge, color = Yks.c.text)
        val steps = q.q.steps.split("\n").filter { it.isNotBlank() }
        if (steps.isNotEmpty()) {
            Spacer(Modifier.height(Space.s))
            Text("Adım adım çözüm", style = MaterialTheme.typography.labelMedium, color = Yks.c.muted)
            steps.forEachIndexed { i, s ->
                Row(Modifier.padding(top = 6.dp)) {
                    Text("${i + 1}.", style = MaterialTheme.typography.bodyLarge, color = Yks.c.accent)
                    Spacer(Modifier.width(Space.s))
                    MathText(s, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = Yks.c.text)
                }
            }
        }
        if (onTopic != null) { Spacer(Modifier.height(Space.s)); TextButton(onTopic) { Text("İlgili konuya git") } }
    }
}

@Composable
fun ReasonPicker(questionId: String) {
    val r = repo()
    val scope = rememberCoroutineScope()
    var reason by remember(questionId) { mutableStateOf("") }
    YCard {
        Text("Neden yanlış oldu?", style = MaterialTheme.typography.titleMedium, color = Yks.c.text)
        Spacer(Modifier.height(Space.s))
        PillGroup(WRONG_REASONS.map { it to it }, reason) { v ->
            reason = v
            scope.launch { val w = r.user.wrongOf(questionId); if (w != null) r.updateWrong(questionId, v, w.note) }
        }
    }
}

/* ---------- YANLIŞLAR DEFTERİ ---------- */

@Composable
fun WrongsScreen() {
    val r = repo()
    val nav = LocalNav.current
    val rows by r.user.wrongRows().collectAsStateWithLifecycle(null)
    val now = remember { System.currentTimeMillis() }
    var filter by rememberSaveable { mutableStateOf("OPEN") }
    Screen("Yanlışlar defteri", { nav.popBackStack() }) {
        val all = rows ?: return@Screen
        val due = all.count { !it.w.resolved && it.w.nextReviewAt <= now }
        val shown = when (filter) { "DUE" -> all.filter { !it.w.resolved && it.w.nextReviewAt <= now }; "DONE" -> all.filter { it.w.resolved }; else -> all.filter { !it.w.resolved } }
            .sortedWith(compareByDescending<WrongRow> { it.w.wrongCount }.thenBy { it.w.nextReviewAt })
        LazyColumn(contentPadding = PaddingValues(Space.l), verticalArrangement = Arrangement.spacedBy(Space.m)) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
                    MainButton(if (due > 0) "$due yanlışı tekrar et" else "Tekrar zamanı gelen yanlış yok", Modifier.fillMaxWidth(), enabled = due > 0, icon = Icons.Rounded.Replay) { nav.navigate("solve?mode=REVIEW") }
                    PillGroup(listOf("OPEN" to "Açık", "DUE" to "Tekrarı gelen", "DONE" to "Çözülen"), filter) { filter = it }
                }
            }
            if (shown.isEmpty()) item { Empty(Icons.Rounded.CheckCircle, "Burada kayıt yok", "Yanlış yaptığın sorular otomatik olarak buraya eklenir.") }
            items(shown, key = { it.w.questionId }) { row ->
                YCard(onClick = { nav.navigate("wrong/${row.w.questionId}") }, padding = Space.m) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${row.subjectName} • ${row.topicName}", style = MaterialTheme.typography.labelMedium, color = Yks.c.accent, modifier = Modifier.weight(1f), maxLines = 1)
                        if (row.w.wrongCount > 1) Tag("${row.w.wrongCount} kez", Yks.c.error)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(row.stem.replace(Regex("[\\\\{}^_]"), " "), style = MaterialTheme.typography.bodyMedium, color = Yks.c.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(4.dp))
                    Muted("Tekrar: " + shortDate(row.w.nextReviewAt) + if (row.w.reason.isNotBlank()) " • ${row.w.reason}" else "")
                }
            }
        }
    }
}

@Composable
fun WrongDetailScreen(id: String) {
    val r = repo()
    val nav = LocalNav.current
    val scope = rememberCoroutineScope()
    var w by remember { mutableStateOf<WrongQuestion?>(null) }
    var q by remember { mutableStateOf<QuestionFull?>(null) }
    var note by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var ask by remember { mutableStateOf(false) }
    LaunchedEffect(id) {
        w = r.user.wrongOf(id); q = r.content.full(id)
        note = w?.note.orEmpty(); reason = w?.reason.orEmpty()
    }
    Screen("Yanlış ayrıntısı", { nav.popBackStack() }, actions = {
        IconButton({ ask = true }) { Icon(Icons.Rounded.DeleteOutline, "Defterden sil") }
    }) {
        val wq = w
        val qq = q
        if (wq == null || qq == null) return@Screen
        ScrollColumn {
            MathText(qq.q.stem, style = MaterialTheme.typography.bodyLarge, color = Yks.c.text)
            qq.q.figure?.let { FigureView(it, Modifier.widthIn(max = 420.dp), color = Yks.c.text) }
            YCard {
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.Cancel, null, tint = Yks.c.error, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Muted("Verdiğin cevap") }
                MathText(wq.givenAnswer, style = MaterialTheme.typography.bodyLarge, color = Yks.c.text)
                Spacer(Modifier.height(Space.s))
                Muted("Yanlış tarihi: ${shortDate(wq.lastWrongAt)} • Tekrar: ${shortDate(wq.nextReviewAt)} • ${wq.wrongCount} kez")
            }
            SolutionCard(qq) { nav.navigate("topic/${qq.q.topicId}") }
            YCard {
                Text("Yanlış nedeni", style = MaterialTheme.typography.titleMedium, color = Yks.c.text)
                Spacer(Modifier.height(Space.s))
                PillGroup(WRONG_REASONS.map { it to it }, reason) { reason = it }
                Spacer(Modifier.height(Space.m))
                Field(note, { note = it }, "Kişisel not", singleLine = false, minLines = 3)
                Spacer(Modifier.height(Space.s))
                MainButton("Kaydet", Modifier.fillMaxWidth()) { scope.launch { r.updateWrong(id, reason, note); nav.popBackStack() } }
            }
        }
        if (ask) ConfirmDialog("Defterden silinsin mi?", "Bu soru yanlışlar defterinden çıkarılır. İstatistiklerin değişmez.", "Sil", true, { ask = false }) {
            scope.launch { r.user.deleteWrong(id); nav.popBackStack() }
        }
    }
}

