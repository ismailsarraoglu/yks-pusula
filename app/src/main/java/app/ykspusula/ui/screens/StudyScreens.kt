package app.ykspusula.ui.screens

import app.ykspusula.ui.currentDay
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.StickyNote2
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ykspusula.data.*
import app.ykspusula.domain.Mastery
import app.ykspusula.domain.MasteryResult
import app.ykspusula.domain.Repetition
import app.ykspusula.domain.Scoring
import app.ykspusula.monetize.AdPlacement
import app.ykspusula.ui.LocalNav
import app.ykspusula.ui.components.*
import app.ykspusula.ui.repo
import app.ykspusula.ui.rememberUiScope
import app.ykspusula.ui.shortDate
import app.ykspusula.ui.theme.Yks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

val MANUAL_STATUS = listOf("Başlanmadı", "Çalışılıyor", "Tekrar gerekiyor", "Öğrenildi")
val WRONG_REASONS = listOf("Konuyu bilmiyordum", "Bilgiyi karıştırma", "Dikkatsizlik", "İşlem hatası", "Soruyu yanlış okuma", "Zaman yetersizliği", "İki seçenek arasında kalma", "Bilgiyi unutma", "Diğer")
/** Yanlış kaydının türü. Tahminle doğru da tekrar gerektirir. */
val WRONG_KINDS = listOf("WRONG" to "Yanlış", "BLANK" to "Boş", "GUESS" to "Tahminle doğru")
fun wrongKindLabel(k: String) = WRONG_KINDS.firstOrNull { it.first == k }?.second ?: "Yanlış"
val TOPIC_CHECKS = listOf("KONU" to "Konu çalışıldı", "OZET" to "Özet çıkarıldı", "SORU" to "Soru çözüldü", "TEKRAR" to "Tekrar edildi")

@Composable fun masteryColor(level: Int): Color = when (level) { 4 -> Yks.c.purpleLight; 3 -> Yks.c.success; 2 -> Yks.c.warning; 1 -> Yks.c.accentLight; else -> Yks.c.muted }

/** Öğrencinin alanına ve diline uyan dersleri seçer. */
fun relevant(subjects: List<Subject>, p: UserProfile?): List<Subject> = subjects.filter { s ->
    s.language == null || s.language == (p?.ydtLanguage ?: "İngilizce")
}

/** Öğrencinin alanına giren dersler: TYT herkes için; AYT ve YDT alana göre. */
fun fieldSubjects(subjects: List<Subject>, p: UserProfile): List<Subject> = relevant(subjects, p).filter { s ->
    s.exam == "TYT" || (p.field != "TYT" && s.fields.split(',').any { it == p.field || it == "ALL" })
}

private data class StudyLink(val title: String, val subtitle: String, val icon: ImageVector, val route: String, val purple: Boolean = false)

@Composable
fun StudyScreen() {
    val r = repo()
    val nav = LocalNav.current
    val c = Yks.c
    val profile by r.user.profile().collectAsStateWithLifecycle(null)
    val subjects by r.content.subjects().collectAsStateWithLifecycle(emptyList())
    val rows by r.content.allTopicRows().collectAsStateWithLifecycle(emptyList())
    val date = currentDay()
    val route by remember(date) { r.user.routeOn(date.toString()) }.collectAsStateWithLifecycle(emptyList())
    val last by r.user.lastSource().collectAsStateWithLifecycle(null)
    var exam by rememberSaveable { mutableStateOf("TYT") }
    LaunchedEffect(Unit) { r.ensureRoute() }
    val list = relevant(subjects, profile).filter { it.exam == exam }
    val grouped = list.groupBy { it.groupName }
    val y = profile?.examYear ?: 2027
    val now = remember { System.currentTimeMillis() }
    val levels = remember(rows) { rows.filter { it.scope != "EXCLUDED" }.groupBy { it.subjectId }.mapValues { (_, v) -> v.count { r.mastery(it, now).level >= 3 } to v.size } }

    ScrollColumn(Modifier.statusBarsPadding()) {
        PageHeader("Çalış", "Bugün hedeflerine bir adım daha yaklaş.", Icons.Rounded.Search, "Ara") { nav.navigate("search") }
        PrimaryGradientCard("Çalışmanı kaydet", "Kaynak, süre ve soru sayını ekle.", Icons.Rounded.EditNote) { nav.navigate("log") }
        last?.let { s ->
            if (s.lastPage > 0) InfoRow("Kaldığın yerden devam", "${s.name} • ${s.lastPage}. sayfa", Icons.Rounded.Bookmark) { nav.navigate("log?source=${s.id}") }
        }
        val open = route.filter { it.status == "SUGGESTED" || it.status == "ACCEPTED" }
        if (open.isNotEmpty()) {
            SectionHeader("Günün rotası", "Tümü") { nav.navigate("coach") }
            GroupCard { open.take(2).forEachIndexed { i, x -> if (i > 0) Line(); NavRow(x.title, "${x.minutes} dk • ${x.reason}", Icons.Rounded.Explore, onClick = { nav.navigate("coach") }) } }
        }
        SectionHeader("Çalışma alanın")
        val links = listOf(
            StudyLink("Öncelikli konular", "Önce neyi çalışmalısın", Icons.Rounded.PriorityHigh, "priorities", true),
            StudyLink("Çalışma planı", "Haftalık programın", Icons.Rounded.CalendarMonth, "plan"),
            StudyLink("Konularım", "Konu ustalığı haritası", Icons.Rounded.Map, "mastery", true),
            StudyLink("Kaynaklarım", "Kitap ve dokümanların", Icons.AutoMirrored.Rounded.MenuBook, "sources"),
            StudyLink("Yanlış defterim", "Tekrar bekleyenler", Icons.Rounded.ErrorOutline, "wrongs", true),
            StudyLink("Pomodoro", "Odaklanma sayacı", Icons.Rounded.Timer, "pomodoro"),
            StudyLink("Formüller", "Hızlı erişim", Icons.Rounded.Functions, "formulas", true),
            StudyLink("Bilgi kartları", "Aralıklı tekrar", Icons.Rounded.Style, "cards"),
            StudyLink("Notlarım", "Kişisel notlar", Icons.AutoMirrored.Rounded.StickyNote2, "notes", true),
            StudyLink("PDF kaynaklarım", "Kendi dosyaların", Icons.Rounded.PictureAsPdf, "library"),
            StudyLink("YKS bilgi merkezi", "Sınav yapısı ve takvim", Icons.Rounded.Info, "info", true),
        )
        TwoColumn(links) { l, m -> FeatureShortcutCard(l.title, l.subtitle, l.icon, if (l.purple) c.purpleLight else c.accentLight, m) { nav.navigate(l.route) } }
        AdSlot(AdPlacement.STUDY)
        if (y >= 2028) InfoBanner("Konu listesi 2027 YKS çerçevesine göre hazırlandı. $y sınavı için kapsam ÖSYM ve MEB tarafından açıklandığında yeni içerik paketi gerekir.", Icons.Rounded.Info)
        SectionHeader("Derslerim")
        PillGroup(listOf("TYT" to "TYT", "AYT" to "AYT", "YDT" to "YDT"), exam) { exam = it }
        if (exam == "YDT") Muted("Seçili dil: ${profile?.ydtLanguage ?: "İngilizce"}. " + ydtSupportNote(profile?.ydtLanguage ?: "İngilizce") + " Dili Profil'den değiştirebilirsin.")
        if (list.isEmpty()) YCard { Muted("Bu dil için henüz içerik yok. Bir içerik paketiyle eklenebilir.") }
        grouped.forEach { (group, subs) ->
            if (grouped.size > 1) Text(group, style = MaterialTheme.typography.titleMedium, color = c.muted)
            GroupCard {
                subs.forEachIndexed { i, s ->
                    if (i > 0) Line()
                    val (done, total) = levels[s.id] ?: (0 to 0)
                    val pct = Scoring.percent(done, total)
                    Column(Modifier.clickable { nav.navigate("subject/${s.id}") }.padding(horizontal = Space.l, vertical = Space.m)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(s.name, style = MaterialTheme.typography.bodyLarge, color = c.text, modifier = Modifier.weight(1f))
                            Muted("%$pct")
                        }
                        Spacer(Modifier.height(6.dp))
                        ProgressBar(pct / 100f, height = 5.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun SubjectScreen(id: String) {
    val r = repo()
    val nav = LocalNav.current
    val rows by r.content.topicRows(id).collectAsStateWithLifecycle(emptyList())
    var name by remember { mutableStateOf("") }
    LaunchedEffect(id) { name = r.content.subject(id)?.name.orEmpty() }
    val now = remember { System.currentTimeMillis() }
    val inScope = rows.filter { it.scope != "EXCLUDED" }
    Screen(name, { nav.popBackStack() }) {
        LazyColumn(contentPadding = PaddingValues(Space.l), verticalArrangement = Arrangement.spacedBy(Space.s + 2.dp)) {
            item {
                YCard {
                    val levels = inScope.map { r.mastery(it, now).level }
                    val done = levels.count { it >= 3 }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${inScope.size} konu", style = MaterialTheme.typography.titleMedium, color = Yks.c.text)
                            Muted("Öğrenilen: $done")
                            Muted("Tekrar gereken: ${levels.count { it == 2 }}")
                            Muted("Çalışılan: ${levels.count { it == 1 }}")
                            Muted("Başlanmamış: ${levels.count { it == 0 }}")
                        }
                        val pct = Scoring.percent(done, inScope.size)
                        Ring(pct / 100f, "%$pct", description = "$name ilerlemesi yüzde $pct")
                    }
                    Spacer(Modifier.height(Space.m))
                    SoftButton("Bu ders için çalışma kaydet", Modifier.fillMaxWidth(), icon = Icons.Rounded.EditNote) { nav.navigate("log?subject=$id") }
                }
            }
            items(rows, key = { it.id }) { t -> TopicCard(t, r.mastery(t, now)) { nav.navigate("topic/${t.id}") } }
        }
    }
}

@Composable
private fun TopicCard(t: TopicRow, m: MasteryResult, onClick: () -> Unit) {
    YCard(onClick = onClick, padding = Space.m) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(masteryColor(m.level)))
            Spacer(Modifier.width(Space.m))
            Column(Modifier.weight(1f)) {
                Text(t.name, style = MaterialTheme.typography.bodyLarge, color = if (t.scope == "EXCLUDED") Yks.c.muted else Yks.c.text)
                val success = if (t.total > 0) " • ${t.total} soru • %${Scoring.percent(t.right, t.total)}" else ""
                Muted(Mastery.LABELS[m.level] + success, maxLines = 1)
            }
            when (t.scope) {
                "EXCLUDED" -> Tag("Kapsam dışı", Yks.c.muted)
                "REVIEW" -> Tag("Kontrol", Yks.c.warning)
                else -> {}
            }
        }
    }
}

/** Konu ayrıntısı: ustalık düzeyi, çalışma geçmişi, kaynaklar ve notlar. */
@Composable
fun TopicScreen(id: String) {
    val r = repo()
    val nav = LocalNav.current
    val scope = rememberUiScope()
    val topic by r.content.topic(id).collectAsStateWithLifecycle(null)
    val progress by r.user.progress(id).collectAsStateWithLifecycle(null)
    val logs by remember(id) { r.user.logsOfTopic(id) }.collectAsStateWithLifecycle(emptyList())
    val sessions by remember(id) { r.user.sessionsOfTopic(id) }.collectAsStateWithLifecycle(emptyList())
    val sources by r.user.sources().collectAsStateWithLifecycle(emptyList())
    val notes by r.user.notes().collectAsStateWithLifecycle(emptyList())
    val t = topic
    Screen(t?.name.orEmpty(), { nav.popBackStack() }) {
        if (t == null) return@Screen
        val rows by r.content.topicRows(t.subjectId).collectAsStateWithLifecycle(emptyList())
        val row = rows.firstOrNull { it.id == id }
        val mastery = row?.let { r.mastery(it) }
        ScrollColumn {
            if (t.scope != "IN") InfoBanner(t.scopeNote, Icons.Rounded.WarningAmber)
            if (mastery != null) YCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(12.dp).clip(CircleShape).background(masteryColor(mastery.level)))
                    Spacer(Modifier.width(Space.s))
                    Text(Mastery.LABELS[mastery.level], style = MaterialTheme.typography.titleLarge, color = Yks.c.text)
                }
                Spacer(Modifier.height(4.dp))
                Muted(mastery.explanation)
                Spacer(Modifier.height(Space.m))
                val total = row.total
                KeyValue("Toplam çalışma", "${row.minutes} dk")
                KeyValue("Son çalışma", progress?.lastStudiedAt?.let(::shortDate) ?: "yok")
                KeyValue("Çözülen soru", "$total")
                if (total > 0) { KeyValue("Doğru oranı", "%${Scoring.percent(row.right, total)}"); KeyValue("Yanlış oranı", "%${100 - Scoring.percent(row.right, total)}") }
                KeyValue("Açık yanlış kaydı", "${row.wrongOpen}")
                KeyValue("Tekrar sayısı", "${progress?.reviewCount ?: 0}")
                KeyValue("Sonraki tekrar", progress?.nextReviewAt?.let(::shortDate) ?: "planlanmadı")
            }
            MainButton("Bu konuda çalışma kaydet", Modifier.fillMaxWidth(), icon = Icons.Rounded.EditNote) { nav.navigate("log?topic=$id&subject=${t.subjectId}") }
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                SoftButton("Tekrar yaptım", Modifier.weight(1f), icon = Icons.Rounded.Replay) { scope.launch { r.markTopicReviewed(id) } }
                SoftButton("Yanlış kaydet", Modifier.weight(1f), icon = Icons.Rounded.ErrorOutline) { nav.navigate("wrongEdit/new?topic=$id&subject=${t.subjectId}") }
            }
            YCard {
                Muted("Konu durumu")
                Spacer(Modifier.height(Space.s))
                PillGroup(MANUAL_STATUS.mapIndexed { i, s -> i.toString() to s }, (progress?.status ?: 0).toString()) { v -> scope.launch { r.setTopicStatus(id, v.toInt()) } }
                Spacer(Modifier.height(Space.s))
                Muted("Güçlü düzeyi işaretlenemez; yeterli soru, başarı ve tekrar kaydıyla kendiliğinden oluşur.")
                Spacer(Modifier.height(Space.m))
                Muted("Yaptıkların")
                Spacer(Modifier.height(4.dp))
                val done = progress?.checks.orEmpty().split(',').toSet()
                MultiPills(TOPIC_CHECKS, done) { c -> scope.launch { r.toggleTopicCheck(id, c) } }
                Spacer(Modifier.height(Space.m))
                Muted("Tekrarı planla")
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    SoftButton("Yarın", Modifier.weight(1f)) { scope.launch { r.setTopicReview(id, startOf(today().plusDays(1))) } }
                    SoftButton("7 gün", Modifier.weight(1f)) { scope.launch { r.setTopicReview(id, startOf(today().plusDays(7))) } }
                }
            }
            val usedIds = (logs.mapNotNull { it.sourceId } + sessions.mapNotNull { it.sourceId }).toSet()
            val used = sources.filter { it.id in usedIds || it.topicId == id }
            if (used.isNotEmpty()) {
                Section("Kullanılan kaynaklar")
                GroupCard { used.forEachIndexed { i, s -> if (i > 0) Line(); NavRow(s.name, s.publisher.ifBlank { sourceType(s.type) }, Icons.AutoMirrored.Rounded.MenuBook, onClick = { nav.navigate("source/${s.id}") }) } }
            }
            val mine = notes.filter { it.topicId == id }
            if (mine.isNotEmpty()) {
                Section("Notlarım")
                GroupCard { mine.forEachIndexed { i, n -> if (i > 0) Line(); NavRow(n.title, n.body.take(60), Icons.AutoMirrored.Rounded.StickyNote2, onClick = { nav.navigate("note/${n.id}") }) } }
            }
            if (logs.isNotEmpty()) {
                Section("Çalışma geçmişi")
                GroupCard {
                    logs.take(10).forEachIndexed { i, l ->
                        if (i > 0) Line()
                        Row(Modifier.fillMaxWidth().padding(Space.m)) {
                            Text(shortDate(l.at), style = MaterialTheme.typography.bodyMedium, color = Yks.c.text, modifier = Modifier.weight(1f))
                            Muted("${l.correct} D • ${l.wrong} Y • ${l.blank} B")
                        }
                    }
                }
            }
            if (t.summary.isNotBlank()) Block("Kısa özet", t.summary)
            if (t.lesson.isNotBlank()) Block("Konu notu", t.lesson)
            if (t.keyPoints.isNotBlank()) Block("Formüller ve önemli bilgiler", t.keyPoints)
            if (t.example.isNotBlank()) Block("Örnek", t.example)
            if (t.lesson.isBlank() && t.keyPoints.isBlank()) Muted("Bu konu için uygulamada not yok. Konu listesi ve takip için kullanabilir, kendi kaynağından çalışıp not ekleyebilirsin.")
            else Muted("Buradaki notlar kısa tekrar özetidir; ders kitabı veya konu anlatımı yerine geçmez.")
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

/** Konu ustalığı haritası: sınav, ders ve duruma göre filtrelenir. */
@Composable
fun MasteryScreen() {
    val r = repo()
    val nav = LocalNav.current
    val profile by r.user.profile().collectAsStateWithLifecycle(null)
    val subjects by r.content.subjects().collectAsStateWithLifecycle(emptyList())
    val rows by r.content.allTopicRows().collectAsStateWithLifecycle(emptyList())
    var exam by rememberSaveable { mutableStateOf("TYT") }
    var subject by rememberSaveable { mutableStateOf("") }
    var level by rememberSaveable { mutableStateOf("") }
    var query by rememberSaveable { mutableStateOf("") }
    val now = remember { System.currentTimeMillis() }
    val subs = relevant(subjects, profile).filter { it.exam == exam }
    val ids = subs.map { it.id }.toSet()
    val all = remember(rows) { rows.filter { it.scope != "EXCLUDED" }.map { it to r.mastery(it, now) } }
    val scoped = all.filter { it.first.subjectId in ids && (subject.isEmpty() || it.first.subjectId == subject) }
    val q = app.ykspusula.domain.SearchText.normalize(query)
    val shown = scoped.filter { (t, m) -> (level.isEmpty() || m.level.toString() == level) && (q.isEmpty() || app.ykspusula.domain.SearchText.score(q, app.ykspusula.domain.SearchText.normalize(t.name)) > 0) }
    Screen("Konu ustalığı", { nav.popBackStack() }) {
        LazyColumn(contentPadding = PaddingValues(Space.l), verticalArrangement = Arrangement.spacedBy(Space.s + 2.dp)) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                    PillGroup(listOf("TYT" to "TYT", "AYT" to "AYT", "YDT" to "YDT"), exam) { exam = it; subject = "" }
                    Dropdown(subs.map { it.id to it.name }, subject, "Tüm dersler") { subject = it }
                    PillGroup(listOf("" to "Tümü") + Mastery.LABELS.mapIndexed { i, s -> "$i" to s }, level) { level = it }
                    Field(query, { query = it }, "Konu ara")
                    val counts = (0..4).map { l -> scoped.count { it.second.level == l } }
                    YCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${scoped.size} konu", style = MaterialTheme.typography.titleMedium, color = Yks.c.text, modifier = Modifier.weight(1f))
                            Text("%${Scoring.percent(counts[3] + counts[4], scoped.size)} öğrenildi", style = MaterialTheme.typography.titleMedium, color = Yks.c.accentLight)
                        }
                        Spacer(Modifier.height(Space.s))
                        Row(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Yks.c.line)) {
                            listOf(4 to Yks.c.purpleLight, 3 to Yks.c.success, 2 to Yks.c.warning, 1 to Yks.c.accentLight).forEach { (l, col) ->
                                if (counts[l] > 0) Box(Modifier.weight(counts[l].toFloat()).fillMaxHeight().background(col))
                            }
                            if (counts[0] > 0) Spacer(Modifier.weight(counts[0].toFloat()))
                        }
                        Spacer(Modifier.height(Space.s))
                        Muted(Mastery.LABELS.mapIndexed { i, s -> "$s: ${counts[i]}" }.joinToString(" • "))
                    }
                }
            }
            if (shown.isEmpty()) item { EmptyStateView(Icons.Rounded.Map, "Konu bulunamadı", "Filtreleri değiştirebilirsin.") }
            items(shown, key = { it.first.id }) { (t, m) -> TopicCard(t, m) { nav.navigate("topic/${t.id}") } }
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

/* ---------- ÇALIŞMA KAYDI ---------- */

val ACTIVITIES = listOf("SORU" to "Soru çözümü", "KONU" to "Konu çalışması", "TEKRAR" to "Tekrar", "OKUMA" to "Okuma", "VIDEO" to "Video ders")

/** Öğrencinin kendi kaynağından yaptığı çalışmayı kaydeder. */
@Composable
fun StudyLogScreen(sourceArg: String, topicArg: String, subjectArg: String, taskArg: String, routeArg: String) {
    val r = repo()
    val nav = LocalNav.current
    val scope = rememberUiScope()
    val profile by r.user.profile().collectAsStateWithLifecycle(null)
    val subjects by r.content.subjects().collectAsStateWithLifecycle(emptyList())
    val sources by r.user.sources().collectAsStateWithLifecycle(emptyList())
    var source by rememberSaveable { mutableStateOf(sourceArg) }
    var subject by rememberSaveable { mutableStateOf(subjectArg) }
    var topic by rememberSaveable { mutableStateOf(topicArg) }
    var activity by rememberSaveable { mutableStateOf("SORU") }
    var from by rememberSaveable { mutableStateOf("") }
    var to by rememberSaveable { mutableStateOf("") }
    var correct by rememberSaveable { mutableStateOf("") }
    var wrong by rememberSaveable { mutableStateOf("") }
    var blank by rememberSaveable { mutableStateOf("") }
    var minutes by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var date by remember { mutableStateOf(today()) }
    val rows by remember(subject) { if (subject.isEmpty()) kotlinx.coroutines.flow.flowOf(emptyList()) else r.content.topicRows(subject) }.collectAsStateWithLifecycle(emptyList())
    val s = sources.firstOrNull { it.id == source }
    LaunchedEffect(s?.id) {
        if (s != null) {
            if (subject.isEmpty()) s.subjectId?.let { subject = it }
            if (topic.isEmpty()) s.topicId?.let { topic = it }
            if (from.isEmpty() && s.lastPage > 0) from = (s.lastPage + 1).toString()
        }
    }
    val c = correct.toIntOrNull() ?: 0
    val w = wrong.toIntOrNull() ?: 0
    val b = blank.toIntOrNull() ?: 0
    val m = minutes.toIntOrNull() ?: 0
    val pf = from.toIntOrNull()
    val pt = to.toIntOrNull()
    val pagesOk = pf == null || pt == null || pt >= pf
    Screen("Çalışma kaydı", { nav.popBackStack() }) {
        ScrollColumn {
            Muted("Kaynak")
            Dropdown(sources.map { it.id to it.name }, source, "Kaynak seçilmedi") { source = it }
            if (sources.isEmpty()) TextButton({ nav.navigate("sourceEdit/new") }) { Text("Kaynak ekle") }
            Muted("Ders")
            Dropdown(relevant(subjects, profile).map { it.id to "${it.exam} ${it.name}" }, subject, "Ders seçilmedi") { subject = it; topic = "" }
            if (subject.isNotEmpty()) { Muted("Konu"); Dropdown(rows.map { it.id to it.name }, topic, "Konu seçilmedi") { topic = it } }
            Muted("Çalışma türü")
            PillGroup(ACTIVITIES, activity) { activity = it }
            DateField(date, "Tarih") { date = it }
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                NumberField(from, { from = it }, "İlk sayfa", Modifier.weight(1f))
                NumberField(to, { to = it }, "Son sayfa", Modifier.weight(1f))
            }
            if (!pagesOk) Text("Son sayfa ilk sayfadan küçük olamaz.", color = Yks.c.error, style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                NumberField(correct, { correct = it }, "Doğru", Modifier.weight(1f))
                NumberField(wrong, { wrong = it }, "Yanlış", Modifier.weight(1f))
                NumberField(blank, { blank = it }, "Boş", Modifier.weight(1f))
            }
            if (c + w + b > 0) Muted("Toplam ${c + w + b} soru • Doğru oranı %${Scoring.percent(c, c + w + b)}")
            NumberField(minutes, { minutes = it }, "Çalışma süresi (dakika)", max = 3)
            Field(note, { note = it }, "Kısa not", singleLine = false, minLines = 2)
            MainButton("Kaydet", Modifier.fillMaxWidth(), enabled = pagesOk && (m > 0 || c + w + b > 0 || pt != null)) {
                val at = if (date == today()) System.currentTimeMillis() else startOf(date) + 12 * 3_600_000L
                scope.launch {
                    r.logStudy(StudyEntry(subject.ifEmpty { null }, topic.ifEmpty { null }, source.ifEmpty { null }, pf, pt, c, w, b, m, 0, activity, note.trim(), "", at, taskArg.ifEmpty { null }, routeArg.ifEmpty { null }))
                    nav.popBackStack()
                }
            }
            if (w > 0) SoftButton("Yanlışları deftere ekle", Modifier.fillMaxWidth(), icon = Icons.Rounded.ErrorOutline) { nav.navigate("wrongEdit/new?topic=$topic&subject=$subject&source=$source") }
        }
    }
}

/* ---------- YANLIŞ DEFTERİ ---------- */

@Composable
fun WrongsScreen() {
    val r = repo()
    val nav = LocalNav.current
    val rows by r.user.wrongRows().collectAsStateWithLifecycle(null)
    val reasons by r.user.wrongReasons().collectAsStateWithLifecycle(emptyList())
    val now = remember { System.currentTimeMillis() }
    var filter by rememberSaveable { mutableStateOf("OPEN") }
    Screen("Yanlış defterim", { nav.popBackStack() }, actions = {
        IconButton({ nav.navigate("wrongEdit/new") }) { Icon(Icons.Rounded.Add, "Yanlış ekle") }
    }) {
        val all = rows ?: return@Screen
        val due = all.count { !it.w.resolved && it.w.nextReviewAt <= now }
        val shown = when (filter) { "DUE" -> all.filter { !it.w.resolved && it.w.nextReviewAt <= now }; "DONE" -> all.filter { it.w.resolved }; else -> all.filter { !it.w.resolved } }
            .sortedWith(compareByDescending<WrongRow> { it.w.wrongCount }.thenBy { it.w.nextReviewAt })
        LazyColumn(contentPadding = PaddingValues(Space.l), verticalArrangement = Arrangement.spacedBy(Space.s + 2.dp)) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
                    MainButton(if (due > 0) "$due yanlışı tekrar et" else "Tekrar zamanı gelen yanlış yok", Modifier.fillMaxWidth(), enabled = due > 0, icon = Icons.Rounded.Replay) { nav.navigate("wrongReview") }
                    PillGroup(listOf("OPEN" to "Açık", "DUE" to "Tekrarı gelen", "DONE" to "Tamamlanan"), filter) { filter = it }
                    if (reasons.isNotEmpty()) {
                        val total = reasons.sumOf { it.value }
                        Muted("En sık neden: ${reasons.first().name} (%${Scoring.percent(reasons.first().value, total)})")
                    }
                }
            }
            if (shown.isEmpty()) item { EmptyStateView(Icons.Rounded.CheckCircle, "Burada kayıt yok", "Kitabında, PDF'inde veya denemende yaptığın yanlışları ekleyebilirsin.", "Yanlış ekle") { nav.navigate("wrongEdit/new") } }
            items(shown, key = { it.w.id }) { row ->
                YCard(onClick = { nav.navigate("wrong/${row.w.id}") }, padding = Space.m) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(listOfNotNull(row.subjectName, row.topicName).joinToString(" • ").ifBlank { "Ders seçilmedi" }, style = MaterialTheme.typography.labelMedium, color = Yks.c.accentLight, modifier = Modifier.weight(1f), maxLines = 1)
                        if (row.w.wrongCount > 1) Tag("${row.w.wrongCount} kez", Yks.c.error)
                    }
                    Spacer(Modifier.height(4.dp))
                    val title = row.w.questionText.ifBlank { listOfNotNull(row.w.sourceLabel.ifBlank { null }, row.w.page?.let { "s. $it" }, row.w.questionNo.ifBlank { null }?.let { "Soru $it" }).joinToString(" • ").ifBlank { "Yanlış kaydı" } }
                    Text(title.replace(Regex("[\\\\{}^_]"), " "), style = MaterialTheme.typography.bodyMedium, color = Yks.c.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(4.dp))
                    Muted("Tekrar: " + shortDate(row.w.nextReviewAt) + if (row.w.reason.isNotBlank()) " • ${row.w.reason}" else "")
                }
            }
        }
    }
}

/** Seçilen fotoğraf küçültülüp uygulamanın özel klasörüne yazılır; internete gönderilmez. */
private suspend fun savePhoto(c: android.content.Context, uri: Uri, target: File): Boolean = withContext(Dispatchers.IO) {
    try {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        c.contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, o) }
        var sample = 1
        while (o.outWidth / sample > 1600 || o.outHeight / sample > 1600) sample *= 2
        val bmp = c.contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) } ?: return@withContext false
        target.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        bmp.recycle()
        true
    } catch (e: Exception) { false }
}

@Composable
private fun PhotoView(file: File, modifier: Modifier = Modifier) {
    var bmp by remember(file.path) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(file.path) { bmp = withContext(Dispatchers.IO) { BitmapFactory.decodeFile(file.path) } }
    bmp?.let { Image(it.asImageBitmap(), "Soru fotoğrafı", modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.FillWidth) }
}

@Composable
fun WrongEditScreen(id: String, topicArg: String, subjectArg: String, sourceArg: String) {
    val r = repo()
    val nav = LocalNav.current
    val scope = rememberUiScope()
    val context = LocalContext.current
    val profile by r.user.profile().collectAsStateWithLifecycle(null)
    val subjects by r.content.subjects().collectAsStateWithLifecycle(emptyList())
    val sources by r.user.sources().collectAsStateWithLifecycle(emptyList())
    var existing by remember { mutableStateOf<WrongEntry?>(null) }
    var subject by rememberSaveable { mutableStateOf(subjectArg) }
    var topic by rememberSaveable { mutableStateOf(topicArg) }
    var source by rememberSaveable { mutableStateOf(sourceArg) }
    var label by rememberSaveable { mutableStateOf("") }
    var page by rememberSaveable { mutableStateOf("") }
    var number by rememberSaveable { mutableStateOf("") }
    var reason by rememberSaveable { mutableStateOf("") }
    var kind by rememberSaveable { mutableStateOf("WRONG") }
    var note by rememberSaveable { mutableStateOf("") }
    var photo by rememberSaveable { mutableStateOf<String?>(null) }
    var review by remember { mutableStateOf(today().plusDays(1)) }
    var error by remember { mutableStateOf("") }
    LaunchedEffect(id) {
        if (id != "new") r.user.wrong(id)?.let { w ->
            existing = w; subject = w.subjectId.orEmpty(); topic = w.topicId.orEmpty(); source = w.sourceId.orEmpty(); label = w.sourceLabel
            page = w.page?.toString().orEmpty(); number = w.questionNo; reason = w.reason; kind = w.kind; note = w.note; photo = w.photoFile; review = dateOf(w.nextReviewAt)
        }
    }
    val rows by remember(subject) { if (subject.isEmpty()) kotlinx.coroutines.flow.flowOf(emptyList()) else r.content.topicRows(subject) }.collectAsStateWithLifecycle(emptyList())
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val name = newId() + ".jpg"
            if (savePhoto(context, uri, r.photoFile(name))) { photo?.let { old -> r.photoFile(old).delete() }; photo = name; error = "" } else error = "Fotoğraf eklenemedi."
        }
    }
    Screen(if (id == "new") "Yanlış ekle" else "Yanlışı düzenle", { nav.popBackStack() }) {
        ScrollColumn {
            Muted("Ders")
            Dropdown(relevant(subjects, profile).map { it.id to "${it.exam} ${it.name}" }, subject, "Ders seçilmedi") { subject = it; topic = "" }
            if (subject.isNotEmpty()) { Muted("Konu"); Dropdown(rows.map { it.id to it.name }, topic, "Konu seçilmedi") { topic = it } }
            Muted("Kaynak")
            Dropdown(sources.map { it.id to it.name }, source, "Kaynak seçilmedi") { source = it }
            if (source.isEmpty()) Field(label, { label = it }, "Kaynak adı (kitap, PDF veya deneme)")
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                NumberField(page, { page = it }, "Sayfa", Modifier.weight(1f))
                Field(number, { number = it.take(8) }, "Soru no", Modifier.weight(1f))
            }
            Muted("Soru durumu")
            PillGroup(WRONG_KINDS, kind) { kind = it }
            Muted("Neden")
            PillGroup(WRONG_REASONS.map { it to it }, reason) { reason = it }
            Field(note, { note = it }, "Kişisel açıklama", singleLine = false, minLines = 3)
            photo?.let { PhotoView(r.photoFile(it)) }
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                SoftButton(if (photo == null) "Fotoğraf ekle" else "Fotoğrafı değiştir", Modifier.weight(1f), icon = Icons.Rounded.AddPhotoAlternate) { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                if (photo != null) SoftButton("Kaldır", Modifier.weight(1f), color = Yks.c.error) { photo?.let { r.photoFile(it).delete() }; photo = null }
            }
            if (error.isNotEmpty()) Text(error, color = Yks.c.error)
            DateField(review, "Tekrar tarihi") { review = it }
            MainButton("Kaydet", Modifier.fillMaxWidth(), enabled = subject.isNotEmpty() || label.isNotBlank() || source.isNotEmpty() || photo != null) {
                val now = System.currentTimeMillis()
                val srcName = sources.firstOrNull { it.id == source }?.name ?: label.trim()
                scope.launch {
                    val e = existing
                    val at = startOf(review) + 8 * 3_600_000L
                    if (e == null) r.saveWrong(r.newWrong(subject.ifEmpty { null }, topic.ifEmpty { null }, source.ifEmpty { null }, srcName, page.toIntOrNull(), number, reason, note.trim(), photo, at, now).copy(kind = kind))
                    else r.saveWrong(e.copy(subjectId = subject.ifEmpty { null }, topicId = topic.ifEmpty { null }, sourceId = source.ifEmpty { null }, sourceLabel = srcName, page = page.toIntOrNull(), questionNo = number, reason = reason, kind = kind, note = note.trim(), photoFile = photo, nextReviewAt = at, updatedAt = now))
                    nav.popBackStack()
                }
            }
        }
    }
}

@Composable
private fun WrongBody(w: WrongEntry, subject: String?, topic: String?) {
    val r = repo()
    if (w.questionText.isNotBlank()) MathText(w.questionText, style = MaterialTheme.typography.bodyLarge, color = Yks.c.text)
    w.photoFile?.let { PhotoView(r.photoFile(it)) }
    YCard {
        listOfNotNull(subject, topic).joinToString(" • ").takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = Yks.c.accentLight) }
        if (w.sourceLabel.isNotBlank()) KeyValue("Kaynak", w.sourceLabel)
        w.page?.let { KeyValue("Sayfa", "$it") }
        if (w.questionNo.isNotBlank()) KeyValue("Soru no", w.questionNo)
        KeyValue("Durum", wrongKindLabel(w.kind) + if (w.wrongCount > 1) " • ${w.wrongCount} soru" else "")
        if (w.reason.isNotBlank()) KeyValue("Neden", w.reason)
        if (w.givenAnswer.isNotBlank()) KeyValue("Verdiğin cevap", w.givenAnswer)
        if (w.correctAnswer.isNotBlank()) KeyValue("Doğru cevap", w.correctAnswer, Yks.c.success)
        KeyValue("İlk kayıt", shortDate(w.firstWrongAt))
        KeyValue("Sonraki tekrar", shortDate(w.nextReviewAt))
        KeyValue("Öğrenme düzeyi", if (w.resolved) "Öğrenildi" else "Tekrar ${w.stage + 1} / ${Repetition.INTERVALS.size}")
    }
    if (w.note.isNotBlank()) YCard { Muted("Açıklaman"); Text(w.note, color = Yks.c.text) }
}

@Composable
fun WrongDetailScreen(id: String) {
    val r = repo()
    val nav = LocalNav.current
    val scope = rememberUiScope()
    val rows by r.user.wrongRows().collectAsStateWithLifecycle(emptyList())
    val history by remember(id) { r.user.reviewsOf(id) }.collectAsStateWithLifecycle(emptyList())
    var ask by remember { mutableStateOf(false) }
    val row = rows.firstOrNull { it.w.id == id }
    Screen("Yanlış ayrıntısı", { nav.popBackStack() }, actions = {
        IconButton({ nav.navigate("wrongEdit/$id") }) { Icon(Icons.Rounded.Edit, "Düzenle") }
        IconButton({ ask = true }) { Icon(Icons.Rounded.DeleteOutline, "Defterden sil") }
    }) {
        val w = row?.w ?: return@Screen
        ScrollColumn {
            WrongBody(w, row.subjectName, row.topicName)
            if (!w.resolved) RatingButtons { v -> scope.launch { r.reviewWrong(id, v) } }
            if (history.isNotEmpty()) {
                Section("Tekrar geçmişi")
                GroupCard {
                    history.take(12).forEachIndexed { i, h ->
                        if (i > 0) Line()
                        Row(Modifier.fillMaxWidth().padding(Space.m)) {
                            Text(shortDate(h.at), style = MaterialTheme.typography.bodyMedium, color = Yks.c.text, modifier = Modifier.weight(1f))
                            Muted(listOf("Hatırlayamadım", "Kısmen", "Hatırladım")[h.result.coerceIn(0, 2)])
                        }
                    }
                }
            }
            w.topicId?.let { t -> SoftButton("İlgili konuya git", Modifier.fillMaxWidth()) { nav.navigate("topic/$t") } }
        }
        if (ask) ConfirmDialog("Defterden silinsin mi?", "Bu kayıt ve fotoğrafı silinir. İstatistiklerin değişmez.", "Sil", true, { ask = false }) {
            scope.launch { r.deleteWrong(id); nav.popBackStack() }
        }
    }
}

@Composable
private fun RatingButtons(onRate: (Int) -> Unit) {
    val items = listOf(Triple("Hatırlayamadım", 0, Yks.c.error), Triple("Kısmen hatırladım", 1, Yks.c.warning), Triple("Hatırladım", 2, Yks.c.success))
    BoxWithConstraints {
        if (rememberCompact(maxWidth, 400.dp)) Column(verticalArrangement = Arrangement.spacedBy(Space.s)) { items.forEach { (t, v, col) -> SoftButton(t, Modifier.fillMaxWidth(), color = col) { onRate(v) } } }
        else Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) { items.forEach { (t, v, col) -> SoftButton(t, Modifier.weight(1f), color = col) { onRate(v) } } }
    }
}

/** Tekrar zamanı gelen yanlışlar sırayla gösterilir. */
@Composable
fun WrongReviewScreen() {
    val r = repo()
    val nav = LocalNav.current
    val scope = rememberUiScope()
    var ids by remember { mutableStateOf<List<String>?>(null) }
    var index by rememberSaveable { mutableIntStateOf(0) }
    val rows by r.user.wrongRows().collectAsStateWithLifecycle(emptyList())
    LaunchedEffect(Unit) { ids = r.user.wrongDueIds(System.currentTimeMillis()) }
    Screen("Yanlış tekrarı", { nav.popBackStack() }) {
        val list = ids ?: return@Screen
        val id = list.getOrNull(index)
        if (id == null) { Empty(Icons.Rounded.CheckCircle, if (list.isEmpty()) "Tekrar edilecek yanlış yok" else "Tekrar bitti", if (list.isEmpty()) "Tekrar zamanı gelen yanlışların burada görünür." else "${list.size} kayıt değerlendirildi.", "Kapat") { nav.popBackStack() }; return@Screen }
        val row = rows.firstOrNull { it.w.id == id } ?: return@Screen
        ScrollColumn {
            Muted("${index + 1} / ${list.size}")
            ProgressBar((index + 1f) / list.size, height = 4.dp)
            WrongBody(row.w, row.subjectName, row.topicName)
            Text("Bu soruyu yeniden çözdün mü? Nasıl geçti?", style = MaterialTheme.typography.titleMedium, color = Yks.c.text)
            RatingButtons { v -> scope.launch { r.reviewWrong(id, v) }; index++ }
        }
    }
}

/** Eksik konu öncelik sırası ve sınav öncesi tekrar görünümü. */
@Composable
fun PrioritiesScreen() {
    val r = repo()
    val nav = LocalNav.current
    val date = app.ykspusula.ui.currentDay()
    var data by remember { mutableStateOf<Pair<List<app.ykspusula.domain.RankedTopic>, Long>?>(null) }
    LaunchedEffect(date) { data = r.priorityTopics() }
    Screen("Öncelikli konular", { nav.popBackStack() }) {
        ScrollColumn {
            val d = data
            if (d == null) { Muted("Hazırlanıyor…"); return@ScrollColumn }
            val (list, daysLeft) = d
            if (daysLeft in 1..app.ykspusula.domain.PlanGenerator.FINAL_DAYS) {
                InfoBanner("Sınav öncesi modu: sınava $daysLeft gün kaldı. Yeni konu yerine deneme yanlışların, tekrar zamanı gelen konular ve düşük doğru oranlı konular öne alındı.", Icons.Rounded.Flag, Yks.c.warning)
            }
            Muted("Sıra yalnızca senin kayıtlarından çıkar: deneme yanlışları, açık yanlışlar, tekrar tarihleri, doğru oranları ve hedef netler. Kayıt girdikçe değişir.")
            if (list.isEmpty()) {
                YCard { Text("Henüz öncelik çıkaracak kayıt yok", style = MaterialTheme.typography.titleMedium, color = Yks.c.text); Muted("Deneme sonucu, yanlış veya soru çözümü kaydettiğinde burada çalışman gereken konular sıralanır.") }
            } else GroupCard {
                list.forEachIndexed { i, x ->
                    if (i > 0) Line()
                    NavRow("${i + 1}. ${x.topic.name}", "${x.topic.subjectName} • " + x.reasons.joinToString(" • "), Icons.Rounded.PriorityHigh, if (i < 3) Yks.c.warning else Yks.c.accent, onClick = { nav.navigate("topic/${x.topic.id}") })
                }
            }
        }
    }
}
