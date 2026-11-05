package app.ykspusula.ui.screens

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ykspusula.data.*
import app.ykspusula.domain.SearchText
import app.ykspusula.ui.LocalNav
import app.ykspusula.ui.components.*
import app.ykspusula.ui.repo
import app.ykspusula.ui.shortDate
import app.ykspusula.ui.solve
import app.ykspusula.ui.theme.Yks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/* ---------- KARTLAR ---------- */

@Composable
fun CardsScreen() {
    val r = repo()
    val nav = LocalNav.current
    val decks by r.user.decks().collectAsStateWithLifecycle(emptyList())
    val due by r.user.cardsDueCount(System.currentTimeMillis()).collectAsStateWithLifecycle(0)
    val learned by r.user.learnedCards().collectAsStateWithLifecycle(0)
    Screen("Kartlar", { nav.popBackStack() }) {
        ScrollColumn {
            Row2({ Tile("$due", "Tekrarı gelen", it) }, { Tile("$learned", "Öğrenilen", it, Yks.c.success) })
            MainButton("Tüm destelerden tekrar", Modifier.fillMaxWidth(), icon = Icons.Rounded.Style) { nav.navigate("review/_all") }
            Section("Desteler") { TextButton({ nav.navigate("cardEdit/Kartlarım") }) { Text("Kart ekle") } }
            GroupCard {
                decks.forEachIndexed { i, d ->
                    if (i > 0) Line()
                    NavRow(d.name, "${d.value} kart", Icons.Rounded.Style, onClick = { nav.navigate("deck/${Uri.encode(d.name)}") })
                }
            }
        }
    }
}

@Composable
fun DeckScreen(deck: String) {
    val r = repo()
    val nav = LocalNav.current
    val scope = rememberCoroutineScope()
    val cards by remember(deck) { r.user.cardsOf(deck) }.collectAsStateWithLifecycle(emptyList())
    var filter by rememberSaveable { mutableStateOf("ALL") }
    val shown = when (filter) { "NEW" -> cards.filter { it.learned != true }; "DONE" -> cards.filter { it.learned == true }; else -> cards }
    Screen(deck, { nav.popBackStack() }, actions = {
        IconButton({ nav.navigate("cardEdit/${Uri.encode(deck)}") }) { Icon(Icons.Rounded.Add, "Kart ekle") }
    }) {
        LazyColumn(contentPadding = PaddingValues(Space.l), verticalArrangement = Arrangement.spacedBy(Space.s)) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
                    MainButton("Tekrara başla", Modifier.fillMaxWidth(), enabled = cards.isNotEmpty()) { nav.navigate("review/${Uri.encode(deck)}") }
                    PillGroup(listOf("ALL" to "Tümü (${cards.size})", "NEW" to "Yeni", "DONE" to "Öğrenilen (${cards.count { it.learned == true }})"), filter) { filter = it }
                }
            }
            items(shown, key = { it.card.id }) { c ->
                YCard(padding = Space.m) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            MathText(c.card.front, style = MaterialTheme.typography.titleMedium, color = Yks.c.text)
                            MathText(c.card.back, style = MaterialTheme.typography.bodyMedium, color = Yks.c.muted)
                            if (c.card.partOfSpeech.isNotBlank()) Muted(c.card.partOfSpeech)
                            if (c.card.example.isNotBlank()) Text(c.card.example, style = MaterialTheme.typography.bodyMedium, color = Yks.c.text)
                            c.nextReviewAt?.takeIf { it > 0 }?.let { Muted("Tekrar: ${shortDate(it)}") }
                        }
                        val l = c.learned == true
                        IconButton({ scope.launch { r.setCardLearned(c.card.id, !l) } }) {
                            Icon(if (l) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked, if (l) "Öğrenildi, geri al" else "Öğrenildi olarak işaretle", tint = if (l) Yks.c.success else Yks.c.muted)
                        }
                        if (c.card.isUser) IconButton({ scope.launch { r.user.deleteUserCard(c.card.id) } }) { Icon(Icons.Rounded.DeleteOutline, "Kartı sil", tint = Yks.c.muted) }
                    }
                }
            }
        }
    }
}

@Composable
fun CardReviewScreen(deck: String) {
    val r = repo()
    val nav = LocalNav.current
    val scope = rememberCoroutineScope()
    var queue by remember { mutableStateOf<List<CardWithProgress>?>(null) }
    var index by remember { mutableIntStateOf(0) }
    var flipped by remember { mutableStateOf(false) }
    LaunchedEffect(deck) { queue = r.user.cardsDue(if (deck == "_all") "" else deck, System.currentTimeMillis(), 50) }
    val rotation by animateFloatAsState(if (flipped) 180f else 0f, label = "kart")
    Screen("Kart tekrarı", { nav.popBackStack() }) {
        val q = queue ?: return@Screen
        val c = q.getOrNull(index)
        if (c == null) {
            Empty(Icons.Rounded.CheckCircle, if (q.isEmpty()) "Tekrar zamanı gelen kart yok" else "Tekrar bitti", if (q.isEmpty()) "Yeni kartlar ve tekrar zamanı gelen kartlar burada görünür." else "${q.size} kart değerlendirildi.", "Kapat") { nav.popBackStack() }
            return@Screen
        }
        Column(Modifier.fillMaxSize().padding(Space.l), verticalArrangement = Arrangement.spacedBy(Space.m)) {
            Muted("${index + 1} / ${q.size} • ${c.card.deck}")
            ProgressBar((index + 1f) / q.size, height = 4.dp)
            YCard(
                Modifier.weight(1f).graphicsLayer { rotationY = rotation; cameraDistance = 24f * density }
                    .semantics { contentDescription = if (flipped) "Kartın arka yüzü" else "Kartın ön yüzü, çevirmek için dokun" },
                onClick = { flipped = !flipped },
            ) {
                Box(Modifier.fillMaxSize().graphicsLayer { rotationY = if (rotation > 90f) 180f else 0f }, contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (rotation <= 90f) {
                            MathText(c.card.front, style = MaterialTheme.typography.headlineMedium.copy(textAlign = TextAlign.Center), color = Yks.c.text)
                            Spacer(Modifier.height(Space.m))
                            Muted("Cevabı görmek için dokun")
                        } else {
                            MathText(c.card.back, style = MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.Center), color = Yks.c.text)
                            if (c.card.example.isNotBlank()) { Spacer(Modifier.height(Space.m)); Muted(c.card.example) }
                        }
                    }
                }
            }
            if (flipped) {
                Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    listOf(Triple("Bilmiyorum", 0, Yks.c.error), Triple("Zorlandım", 1, Yks.c.warning), Triple("Biliyorum", 2, Yks.c.success)).forEach { (t, v, col) ->
                        SoftButton(t, Modifier.weight(1f), color = col) {
                            scope.launch { r.rateCard(c.card.id, v) }
                            flipped = false
                            index++
                        }
                    }
                }
            } else MainButton("Kartı çevir", Modifier.fillMaxWidth()) { flipped = true }
        }
    }
}

@Composable
fun CardEditScreen(deck: String) {
    val r = repo()
    val nav = LocalNav.current
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf(deck) }
    var subject by rememberSaveable { mutableStateOf("") }
    var front by rememberSaveable { mutableStateOf("") }
    var back by rememberSaveable { mutableStateOf("") }
    Screen("Yeni kart", { nav.popBackStack() }) {
        ScrollColumn {
            Field(name, { name = it }, "Deste")
            Field(subject, { subject = it }, "Ders (isteğe bağlı)")
            Field(front, { front = it }, "Ön yüz: soru veya kavram", singleLine = false, minLines = 2)
            Field(back, { back = it }, "Arka yüz: cevap", singleLine = false, minLines = 3)
            MainButton("Kaydet", Modifier.fillMaxWidth(), enabled = name.isNotBlank() && front.isNotBlank() && back.isNotBlank()) {
                scope.launch {
                    r.user.saveCard(Flashcard(newId(), name.trim(), subject.trim(), front.trim(), back.trim(), isUser = true))
                    nav.popBackStack()
                }
            }
        }
    }
}

/* ---------- FORMÜLLER ---------- */

@Composable
fun FormulasScreen() {
    val r = repo()
    val nav = LocalNav.current
    val scope = rememberCoroutineScope()
    val all by r.content.formulas().collectAsStateWithLifecycle(emptyList())
    val favs by r.user.bookmarkIds("FORMULA").collectAsStateWithLifecycle(emptyList())
    var query by rememberSaveable { mutableStateOf("") }
    var subject by rememberSaveable { mutableStateOf("") }
    var onlyFav by rememberSaveable { mutableStateOf(false) }
    val q = SearchText.normalize(query)
    val shown = remember(all, q, subject, onlyFav, favs) {
        all.filter { (subject.isEmpty() || it.subjectName == subject) && (!onlyFav || it.id in favs) && (q.isEmpty() || SearchText.score(q, SearchText.normalize(it.title), SearchText.normalize(it.body + " " + it.category)) > 0) }
    }
    Screen("Formüller ve bilgiler", { nav.popBackStack() }) {
        LazyColumn(contentPadding = PaddingValues(Space.l), verticalArrangement = Arrangement.spacedBy(Space.s)) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                    Field(query, { query = it }, "Ara")
                    Dropdown(all.map { it.subjectName }.distinct().map { it to it }, subject, "Tüm dersler") { subject = it }
                    Pill("Yalnızca favoriler", onlyFav) { onlyFav = !onlyFav }
                    if (all.isEmpty()) Muted("Henüz içerik yok.")
                }
            }
            items(shown, key = { it.id }) { f ->
                val fav = f.id in favs
                YCard(padding = Space.m) {
                    Row(verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Text("${f.subjectName} • ${f.category}", style = MaterialTheme.typography.labelMedium, color = Yks.c.accent)
                            Text(f.title, style = MaterialTheme.typography.titleMedium, color = Yks.c.text)
                            Spacer(Modifier.height(4.dp))
                            f.body.split("\n").forEach { MathText(it, style = MaterialTheme.typography.bodyLarge, color = Yks.c.text) }
                            if (f.note.isNotBlank()) Muted(f.note)
                        }
                        IconButton({ scope.launch { r.toggleBookmark("FORMULA", f.id, !fav) } }) {
                            Icon(if (fav) Icons.Rounded.Star else Icons.Rounded.StarBorder, if (fav) "Favorilerden çıkar" else "Favorilere ekle", tint = if (fav) Yks.c.warning else Yks.c.muted)
                        }
                    }
                }
            }
        }
    }
}

/* ---------- NOTLAR ---------- */

@Composable
fun NotesScreen() {
    val r = repo()
    val nav = LocalNav.current
    val notes by r.user.notes().collectAsStateWithLifecycle(emptyList())
    val subjects by r.content.subjects().collectAsStateWithLifecycle(emptyList())
    var query by rememberSaveable { mutableStateOf("") }
    val q = SearchText.normalize(query)
    val names = subjects.associate { it.id to "${it.exam} ${it.name}" }
    val shown = notes.filter { q.isEmpty() || SearchText.score(q, SearchText.normalize(it.title), SearchText.normalize(it.body)) > 0 }
    Screen("Notlarım", { nav.popBackStack() }, actions = {
        IconButton({ nav.navigate("note/new") }) { Icon(Icons.Rounded.Add, "Yeni not") }
    }) {
        LazyColumn(contentPadding = PaddingValues(Space.l), verticalArrangement = Arrangement.spacedBy(Space.s)) {
            item { Field(query, { query = it }, "Notlarda ara") }
            if (shown.isEmpty()) item { Empty(Icons.Rounded.StickyNote2, "Not yok", "Ders notlarını buraya ekleyebilirsin.", "Yeni not") { nav.navigate("note/new") } }
            items(shown, key = { it.id }) { n ->
                YCard(onClick = { nav.navigate("note/${n.id}") }, padding = Space.m) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(n.title, style = MaterialTheme.typography.titleMedium, color = Yks.c.text, modifier = Modifier.weight(1f), maxLines = 1)
                        if (n.favorite) Icon(Icons.Rounded.Star, "Favori", tint = Yks.c.warning, modifier = Modifier.size(18.dp))
                    }
                    Muted(n.body, maxLines = 2)
                    Muted(listOfNotNull(n.subjectId?.let { names[it] }, shortDate(n.updatedAt)).joinToString(" • "))
                }
            }
        }
    }
}

@Composable
fun NoteEditScreen(id: String) {
    val r = repo()
    val nav = LocalNav.current
    val scope = rememberCoroutineScope()
    val profile by r.user.profile().collectAsStateWithLifecycle(null)
    val subjects by r.content.subjects().collectAsStateWithLifecycle(emptyList())
    var title by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }
    var subject by rememberSaveable { mutableStateOf("") }
    var topic by rememberSaveable { mutableStateOf("") }
    var favorite by rememberSaveable { mutableStateOf(false) }
    var existing by remember { mutableStateOf<UserNote?>(null) }
    var ask by remember { mutableStateOf(false) }
    val rows by remember(subject) { if (subject.isEmpty()) kotlinx.coroutines.flow.flowOf(emptyList()) else r.content.topicRows(subject) }.collectAsStateWithLifecycle(emptyList())
    LaunchedEffect(id) {
        if (id != "new") r.user.note(id)?.let {
            existing = it; title = it.title; body = it.body; subject = it.subjectId.orEmpty(); topic = it.topicId.orEmpty(); favorite = it.favorite
        }
    }
    Screen(if (id == "new") "Yeni not" else "Notu düzenle", { nav.popBackStack() }, actions = {
        IconButton({ favorite = !favorite }) { Icon(if (favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder, if (favorite) "Favorilerden çıkar" else "Favorilere ekle", tint = if (favorite) Yks.c.warning else Yks.c.text) }
        if (id != "new") IconButton({ ask = true }) { Icon(Icons.Rounded.DeleteOutline, "Notu sil") }
    }) {
        ScrollColumn {
            Field(title, { title = it }, "Başlık")
            Field(body, { body = it }, "Açıklama", singleLine = false, minLines = 8)
            Muted("Ders")
            Dropdown(relevant(subjects, profile).map { it.id to "${it.exam} ${it.name}" }, subject, "Ders seçilmedi") { subject = it; topic = "" }
            if (subject.isNotEmpty()) { Muted("Konu"); Dropdown(rows.map { it.id to it.name }, topic, "Konu seçilmedi") { topic = it } }
            MainButton("Kaydet", Modifier.fillMaxWidth(), enabled = title.isNotBlank()) {
                val now = System.currentTimeMillis()
                scope.launch {
                    r.user.saveNote((existing ?: UserNote(title = "", body = "", createdAt = now, updatedAt = now)).copy(title = title.trim(), body = body, subjectId = subject.ifEmpty { null }, topicId = topic.ifEmpty { null }, updatedAt = now, favorite = favorite))
                    nav.popBackStack()
                }
            }
        }
    }
    if (ask) ConfirmDialog("Not silinsin mi?", "Bu işlem geri alınamaz.", "Sil", true, { ask = false }) { scope.launch { r.user.deleteNote(id); nav.popBackStack() } }
}

/* ---------- PDF KAYNAKLARI ---------- */

private fun displayName(c: android.content.Context, uri: Uri): String =
    c.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { if (it.moveToFirst()) it.getString(0) else null } ?: "Belge.pdf"

@Composable
fun LibraryScreen() {
    val r = repo()
    val nav = LocalNav.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val files by r.user.library().collectAsStateWithLifecycle(emptyList())
    var message by remember { mutableStateOf("") }
    var removing by remember { mutableStateOf<LibraryFile?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            try {
                val saved = withContext(Dispatchers.IO) {
                    val dir = File(context.filesDir, "library").apply { mkdirs() }
                    val fileName = newId() + ".pdf"
                    val out = File(dir, fileName)
                    context.contentResolver.openInputStream(uri)!!.use { i -> out.outputStream().use { o -> i.copyTo(o) } }
                    LibraryFile(name = displayName(context, uri), fileName = fileName, sizeBytes = out.length(), addedAt = System.currentTimeMillis())
                }
                r.user.saveLibrary(saved)
            } catch (e: Exception) {
                message = "Dosya eklenemedi."
            }
        }
    }
    Screen("Kaynaklarım", { nav.popBackStack() }, actions = {
        IconButton({ picker.launch(arrayOf("application/pdf")) }) { Icon(Icons.Rounded.Add, "PDF ekle") }
    }) {
        ScrollColumn {
            Muted("Eklediğin PDF dosyaları uygulamanın özel klasörüne kopyalanır. Uygulama cihazını taramaz; yalnızca seçtiğin dosyaya erişir. PDF'ler yedek dosyasına dahil değildir.")
            if (message.isNotEmpty()) Text(message, color = Yks.c.error)
            if (files.isEmpty()) Empty(Icons.Rounded.PictureAsPdf, "Henüz kaynak yok", "Kendi PDF dosyalarını ekleyebilirsin.", "PDF ekle") { picker.launch(arrayOf("application/pdf")) }
            else GroupCard {
                files.forEachIndexed { i, f ->
                    if (i > 0) Line()
                    NavRow(f.name, "${f.sizeBytes / 1024} KB • ${shortDate(f.addedAt)}", Icons.Rounded.PictureAsPdf, Yks.c.error, { nav.navigate("pdf/${f.id}") }) {
                        IconButton({ removing = f }) { Icon(Icons.Rounded.DeleteOutline, "Kaynağı sil", tint = Yks.c.muted) }
                    }
                }
            }
        }
    }
    removing?.let { f ->
        ConfirmDialog("Kaynak silinsin mi?", f.name, "Sil", true, { removing = null }) {
            scope.launch { File(File(context.filesDir, "library"), f.fileName).delete(); r.user.deleteLibrary(f) }
            removing = null
        }
    }
}

@Composable
fun PdfScreen(id: String) {
    val r = repo()
    val nav = LocalNav.current
    val context = LocalContext.current
    val files by r.user.library().collectAsStateWithLifecycle(emptyList())
    val f = files.firstOrNull { it.id == id }
    var page by rememberSaveable { mutableIntStateOf(0) }
    var pages by remember { mutableIntStateOf(0) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(f?.fileName, page) {
        val file = f?.let { File(File(context.filesDir, "library"), it.fileName) } ?: return@LaunchedEffect
        bitmap = withContext(Dispatchers.IO) {
            try {
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                    PdfRenderer(fd).use { pdf ->
                        pages = pdf.pageCount
                        pdf.openPage(page.coerceIn(0, pdf.pageCount - 1)).use { p ->
                            val scale = 1600f / p.width
                            val bmp = Bitmap.createBitmap(1600, (p.height * scale).toInt(), Bitmap.Config.ARGB_8888)
                            bmp.eraseColor(android.graphics.Color.WHITE)
                            p.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            bmp
                        }
                    }
                }
            } catch (e: Exception) { failed = true; null }
        }
    }
    Screen(f?.name.orEmpty(), { nav.popBackStack() }) {
        if (failed) { Empty(Icons.Rounded.ErrorOutline, "Dosya açılamadı", "PDF bozuk veya parolalı olabilir."); return@Screen }
        ScrollColumn {
            bitmap?.let { Image(it.asImageBitmap(), "Sayfa ${page + 1}", Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth) }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                SoftButton("Önceki", Modifier.weight(1f), enabled = page > 0) { page-- }
                Muted("${page + 1} / $pages")
                SoftButton("Sonraki", Modifier.weight(1f), enabled = page < pages - 1) { page++ }
            }
        }
    }
}

/* ---------- ARAMA ---------- */

data class Hit(val kind: String, val title: String, val detail: String, val route: String, val score: Int)
private data class Entry(val kind: String, val title: String, val detail: String, val route: String, val nt: String, val nb: String, val boost: Int)

@Composable
fun SearchScreen() {
    val r = repo()
    val nav = LocalNav.current
    var query by rememberSaveable { mutableStateOf("") }
    var index by remember { mutableStateOf<List<Entry>?>(null) }
    var hits by remember { mutableStateOf<List<Hit>>(emptyList()) }
    LaunchedEffect(Unit) {
        index = withContext(Dispatchers.Default) {
            val n = SearchText::normalize
            val out = ArrayList<Entry>()
            val subjects = r.content.subjectsNow()
            val names = subjects.associateBy { it.id }
            subjects.forEach { out += Entry("Ders", it.name, it.exam, "subject/${it.id}", n(it.name), "", 8) }
            r.content.allTopics().forEach { t ->
                val s = names[t.subjectId]
                val isSub = t.parentId != null
                out += Entry(if (isSub) "Alt konu" else "Konu", t.name, "${s?.exam} ${s?.name}", "topic/${t.id}", n(t.name), n(t.summary), 6)
            }
            r.content.allQuestions().forEach { q ->
                val s = names[q.subjectId]
                val clean = q.stem.replace(Regex("[\\\\{}^_]"), " ")
                out += Entry("Soru", clean.take(80), "${s?.exam} ${s?.name}", "solveOne:${q.topicId}", "", n(clean), 0)
            }
            r.content.formulasNow().forEach { f -> out += Entry("Formül", f.title, "${f.subjectName} • ${f.category}", "formulas", n(f.title), n(f.body), 4) }
            r.user.notesNow().forEach { x -> out += Entry("Not", x.title, x.body.take(60), "note/${x.id}", n(x.title), n(x.body), 5) }
            r.content.flashcardsNow().forEach { c -> out += Entry("Kart", c.front, c.back.take(60), "deck/${Uri.encode(c.deck)}", n(c.front), n(c.back), 2) }
            r.user.universitiesNow().forEach { u -> out += Entry("Üniversite", u.name, u.city, "targets", n(u.name), n(u.city), 3) }
            r.user.departmentsNow().forEach { d -> out += Entry("Bölüm", d.name, d.scoreType, "targets", n(d.name), n(d.note), 3) }
            r.user.universityRecordsNow().forEach { u -> out += Entry("Bölüm", "${u.university} ${u.department}", u.city, "targets", n("${u.university} ${u.department}"), "", 1) }
            out
        }
    }
    LaunchedEffect(query, index) {
        val idx = index ?: return@LaunchedEffect
        val q = SearchText.normalize(query)
        hits = if (q.isEmpty()) emptyList() else withContext(Dispatchers.Default) {
            idx.mapNotNull { e ->
                val s = SearchText.score(q, e.nt, e.nb)
                if (s > 0) Hit(e.kind, e.title, e.detail, e.route, s + e.boost) else null
            }.sortedByDescending { it.score }.take(60)
        }
    }
    Screen("Ara", { nav.popBackStack() }) {
        LazyColumn(contentPadding = PaddingValues(Space.l), verticalArrangement = Arrangement.spacedBy(Space.s)) {
            item { Field(query, { query = it }, "Ders, konu, soru, formül, not...") }
            if (query.isNotBlank() && hits.isEmpty() && index != null) item { Muted("Sonuç bulunamadı.") }
            items(hits) { h ->
                YCard(padding = Space.m, onClick = {
                    if (h.route.startsWith("solveOne:")) nav.solve(topic = h.route.removePrefix("solveOne:")) else nav.navigate(h.route)
                }) {
                    Text(h.kind, style = MaterialTheme.typography.labelMedium, color = Yks.c.accent)
                    Text(h.title, style = MaterialTheme.typography.bodyLarge, color = Yks.c.text, maxLines = 2)
                    Muted(h.detail, maxLines = 1)
                }
            }
        }
    }
}
