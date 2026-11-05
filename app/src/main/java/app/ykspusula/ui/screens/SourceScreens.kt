package app.ykspusula.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ykspusula.data.*
import app.ykspusula.domain.Scoring
import app.ykspusula.ui.LocalNav
import app.ykspusula.ui.components.*
import app.ykspusula.ui.repo
import app.ykspusula.ui.rememberUiScope
import app.ykspusula.ui.shortDate
import app.ykspusula.ui.theme.Yks
import kotlinx.coroutines.launch
import java.time.LocalDate

val SOURCE_TYPES = listOf("KITAP" to "Fiziksel kitap", "SORU_BANKASI" to "Soru bankası", "PDF" to "PDF", "NOT" to "Ders notu", "OGRETMEN" to "Öğretmen dokümanı", "VIDEO" to "Video ders", "DOKUMAN" to "Kişisel doküman", "DIGER" to "Diğer")
fun sourceType(id: String) = SOURCE_TYPES.firstOrNull { it.first == id }?.second ?: "Kaynak"

private fun progressOf(s: StudySource): Float = if (s.totalPages > 0) (maxOf(s.pagesDone, s.lastPage).toFloat() / s.totalPages).coerceAtMost(1f) else 0f

@Composable
fun SourceCard(row: SourceRow, onClick: () -> Unit) {
    val s = row.s
    YCard(onClick = onClick, padding = Space.m) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(s.name, style = MaterialTheme.typography.titleMedium, color = Yks.c.text, maxLines = 2)
                Muted(listOf(s.publisher, sourceType(s.type)).filter { it.isNotBlank() }.joinToString(" • "), maxLines = 1)
            }
            if (s.favorite) Icon(Icons.Rounded.Star, "Favori", tint = Yks.c.warning, modifier = Modifier.size(18.dp))
        }
        if (s.totalPages > 0) {
            Spacer(Modifier.height(Space.s))
            ProgressBar(progressOf(s), height = 6.dp)
            Spacer(Modifier.height(4.dp))
            Muted("%${(progressOf(s) * 100).toInt()} • ${s.lastPage}/${s.totalPages}. sayfa")
        }
        if (row.solved > 0 || row.minutes > 0) Muted("${row.solved} soru • ${row.minutes} dk" + if (row.solved > 0) " • %${Scoring.percent(row.correct, row.solved)} doğru" else "")
    }
}

/** Kişisel kaynak merkezi: öğrenci hangi kaynakla çalışırsa çalışsın buradan takip eder. */
@Composable
fun SourcesScreen() {
    val r = repo()
    val nav = LocalNav.current
    val rows by r.user.sourceRows().collectAsStateWithLifecycle(null)
    var filter by rememberSaveable { mutableStateOf("") }
    Screen("Kaynaklarım", { nav.popBackStack() }, actions = {
        IconButton({ nav.navigate("sourceCompare") }) { Icon(Icons.Rounded.CompareArrows, "Karşılaştır") }
        IconButton({ nav.navigate("sourceEdit/new") }) { Icon(Icons.Rounded.Add, "Kaynak ekle") }
    }) {
        val all = rows ?: return@Screen
        val active = all.filter { !it.s.archived && (filter.isEmpty() || it.s.type == filter || (filter == "FAV" && it.s.favorite)) }
        val archived = all.filter { it.s.archived }
        LazyColumn(contentPadding = PaddingValues(Space.l), verticalArrangement = Arrangement.spacedBy(Space.s + 2.dp)) {
            item { PillGroup(listOf("" to "Tümü", "FAV" to "Favoriler") + SOURCE_TYPES.filter { t -> all.any { it.s.type == t.first } }, filter) { filter = it } }
            if (all.isEmpty()) item { EmptyStateView(Icons.AutoMirrored.Rounded.MenuBook, "Henüz kaynak yok", "Kullandığın kitabı, PDF'i veya videoyu ekle. Sayfanı, sürene ve netine göre ilerlemeni takip et.", "Kaynak ekle") { nav.navigate("sourceEdit/new") } }
            items(active, key = { it.s.id }) { x -> SourceCard(x) { nav.navigate("source/${x.s.id}") } }
            if (archived.isNotEmpty()) {
                item { Section("Arşiv") }
                items(archived, key = { it.s.id }) { x -> SourceCard(x) { nav.navigate("source/${x.s.id}") } }
            }
        }
    }
}

@Composable
fun SourceDetailScreen(id: String) {
    val r = repo()
    val nav = LocalNav.current
    val scope = rememberUiScope()
    val rows by r.user.sourceRows().collectAsStateWithLifecycle(emptyList())
    val logs by remember(id) { r.user.logsOfSource(id) }.collectAsStateWithLifecycle(emptyList())
    val sessions by remember(id) { r.user.sessionsOfSource(id) }.collectAsStateWithLifecycle(emptyList())
    val row = rows.firstOrNull { it.s.id == id }
    var ask by remember { mutableStateOf(false) }
    var pageAsk by remember { mutableStateOf(false) }
    Screen(row?.s?.name.orEmpty(), { nav.popBackStack() }, actions = {
        val s = row?.s
        if (s != null) {
            IconButton({ scope.launch { r.user.saveSource(s.copy(favorite = !s.favorite, updatedAt = System.currentTimeMillis())) } }) { Icon(if (s.favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder, if (s.favorite) "Favorilerden çıkar" else "Favorilere ekle", tint = if (s.favorite) Yks.c.warning else Yks.c.text) }
            IconButton({ nav.navigate("sourceEdit/$id") }) { Icon(Icons.Rounded.Edit, "Düzenle") }
            IconButton({ ask = true }) { Icon(Icons.Rounded.DeleteOutline, "Kaynağı sil") }
        }
    }) {
        val x = row ?: return@Screen
        val s = x.s
        ScrollColumn {
            if (s.lastPage > 0) PrimaryGradientCard("En son ${s.lastPage}. sayfada kaldın.", "Kaldığın yerden devam et.", Icons.Rounded.Bookmark) { nav.navigate("log?source=$id") }
            else MainButton("Çalışma kaydet", Modifier.fillMaxWidth(), icon = Icons.Rounded.EditNote) { nav.navigate("log?source=$id") }
            YCard {
                if (s.totalPages > 0) { ProgressBar(progressOf(s)); Spacer(Modifier.height(Space.s)); Muted("%${(progressOf(s) * 100).toInt()} tamamlandı") }
                KeyValue("Tür", sourceType(s.type))
                if (s.publisher.isNotBlank()) KeyValue("Yayınevi", s.publisher)
                KeyValue("Çalışma süresi", "${x.minutes} dk")
                KeyValue("Çözülen soru", "${x.solved}")
                if (x.solved > 0) KeyValue("Doğru oranı", "%${Scoring.percent(x.correct, x.solved)}")
                x.lastAt?.let { KeyValue("Son çalışma", shortDate(it)) }
                if (s.startDate.isNotBlank()) KeyValue("Başlangıç", s.startDate)
                if (s.endDate.isNotBlank()) KeyValue("Hedef bitiş", s.endDate)
            }
            SoftButton("Son kaldığın sayfayı güncelle", Modifier.fillMaxWidth(), icon = Icons.Rounded.Edit) { pageAsk = true }
            if (s.libraryFileId != null) SoftButton("PDF'i aç", Modifier.fillMaxWidth(), icon = Icons.Rounded.PictureAsPdf) { nav.navigate("pdf/${s.libraryFileId}") }
            if (s.description.isNotBlank()) YCard { Muted("Açıklama"); Text(s.description, color = Yks.c.text) }
            if (s.note.isNotBlank()) YCard { Muted("Kişisel not"); Text(s.note, color = Yks.c.text) }
            SoftButton(if (s.archived) "Arşivden çıkar" else "Arşive taşı", Modifier.fillMaxWidth(), color = Yks.c.muted) { scope.launch { r.user.saveSource(s.copy(archived = !s.archived, updatedAt = System.currentTimeMillis())) } }
            if (sessions.isNotEmpty() || logs.isNotEmpty()) Section("Geçmiş")
            if (logs.isNotEmpty()) GroupCard {
                logs.take(12).forEachIndexed { i, l ->
                    if (i > 0) Line()
                    Row(Modifier.fillMaxWidth().padding(Space.m)) {
                        Column(Modifier.weight(1f)) {
                            Text(shortDate(l.at), style = MaterialTheme.typography.bodyMedium, color = Yks.c.text)
                            if (l.pageFrom != null || l.pageTo != null) Muted("Sayfa ${l.pageFrom ?: "?"}–${l.pageTo ?: "?"}")
                        }
                        Muted("${l.correct} D • ${l.wrong} Y • ${l.blank} B")
                    }
                }
            } else if (sessions.isNotEmpty()) GroupCard {
                sessions.take(12).forEachIndexed { i, l ->
                    if (i > 0) Line()
                    Row(Modifier.fillMaxWidth().padding(Space.m)) {
                        Text(shortDate(l.startedAt), style = MaterialTheme.typography.bodyMedium, color = Yks.c.text, modifier = Modifier.weight(1f))
                        Muted("${l.minutes} dk")
                    }
                }
            }
        }
        if (pageAsk) {
            var page by remember { mutableStateOf(s.lastPage.toString()) }
            AlertDialog(
                onDismissRequest = { pageAsk = false }, containerColor = Yks.c.card, title = { Text("Son kaldığın sayfa") },
                text = { NumberField(page, { page = it }, "Sayfa") },
                confirmButton = {
                    TextButton({
                        val p = page.toIntOrNull() ?: 0
                        scope.launch { r.user.saveSource(s.copy(lastPage = p, pagesDone = maxOf(s.pagesDone, p), lastUsedAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis())) }
                        pageAsk = false
                    }) { Text("Kaydet") }
                },
                dismissButton = { TextButton({ pageAsk = false }) { Text("Vazgeç") } },
            )
        }
        if (ask) ConfirmDialog("Kaynak silinsin mi?", "Kaynak listeden kalkar. Çalışma süren ve soru istatistiklerin korunur.", "Sil", true, { ask = false }) {
            scope.launch { r.user.deleteSource(id); nav.popBackStack() }
        }
    }
}

@Composable
fun SourceEditScreen(id: String) {
    val r = repo()
    val nav = LocalNav.current
    val scope = rememberUiScope()
    val profile by r.user.profile().collectAsStateWithLifecycle(null)
    val subjects by r.content.subjects().collectAsStateWithLifecycle(emptyList())
    val files by r.user.library().collectAsStateWithLifecycle(emptyList())
    var existing by remember { mutableStateOf<StudySource?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var publisher by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf("SORU_BANKASI") }
    var subject by rememberSaveable { mutableStateOf("") }
    var topic by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var pages by rememberSaveable { mutableStateOf("") }
    var lastPage by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var pdf by rememberSaveable { mutableStateOf("") }
    var start by remember { mutableStateOf<LocalDate?>(null) }
    var end by remember { mutableStateOf<LocalDate?>(null) }
    LaunchedEffect(id) {
        if (id != "new") r.user.source(id)?.let { s ->
            existing = s; name = s.name; publisher = s.publisher; type = s.type; subject = s.subjectId.orEmpty(); topic = s.topicId.orEmpty(); description = s.description
            pages = if (s.totalPages > 0) "${s.totalPages}" else ""; lastPage = if (s.lastPage > 0) "${s.lastPage}" else ""; note = s.note; pdf = s.libraryFileId.orEmpty()
            start = s.startDate.takeIf { it.isNotBlank() }?.let(LocalDate::parse); end = s.endDate.takeIf { it.isNotBlank() }?.let(LocalDate::parse)
        }
    }
    val rows by remember(subject) { if (subject.isEmpty()) kotlinx.coroutines.flow.flowOf(emptyList()) else r.content.topicRows(subject) }.collectAsStateWithLifecycle(emptyList())
    Screen(if (id == "new") "Kaynak ekle" else "Kaynağı düzenle", { nav.popBackStack() }) {
        ScrollColumn {
            Field(name, { name = it }, "Kaynak adı")
            Field(publisher, { publisher = it }, "Yayınevi veya hazırlayan")
            Muted("Kaynak türü")
            PillGroup(SOURCE_TYPES, type) { type = it }
            Muted("Ders")
            Dropdown(relevant(subjects, profile).map { it.id to "${it.exam} ${it.name}" }, subject, "Ders seçilmedi") { subject = it; topic = "" }
            if (subject.isNotEmpty()) { Muted("Konu"); Dropdown(rows.map { it.id to it.name }, topic, "Tüm konular") { topic = it } }
            Field(description, { description = it }, "Açıklama", singleLine = false, minLines = 2)
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                NumberField(pages, { pages = it }, "Toplam sayfa", Modifier.weight(1f), 5)
                NumberField(lastPage, { lastPage = it }, "Son sayfa", Modifier.weight(1f), 5)
            }
            DateField(start ?: today(), "Başlangıç") { start = it }
            DateField(end ?: today().plusDays(30), "Hedef bitiş") { end = it }
            if (type == "PDF" && files.isNotEmpty()) { Muted("PDF dosyası"); Dropdown(files.map { it.id to it.name }, pdf, "Bağlanmadı") { pdf = it } }
            Field(note, { note = it }, "Kişisel not", singleLine = false, minLines = 2)
            val total = pages.toIntOrNull() ?: 0
            val last = lastPage.toIntOrNull() ?: 0
            if (total > 0 && last > total) Text("Son sayfa toplam sayfayı aşamaz.", color = Yks.c.error, style = MaterialTheme.typography.bodyMedium)
            MainButton("Kaydet", Modifier.fillMaxWidth(), enabled = name.isNotBlank() && (total == 0 || last <= total)) {
                val now = System.currentTimeMillis()
                scope.launch {
                    val base = existing ?: StudySource(name = "", createdAt = now, updatedAt = now)
                    r.user.saveSource(
                        base.copy(
                            name = name.trim(), publisher = publisher.trim(), type = type, subjectId = subject.ifEmpty { null }, topicId = topic.ifEmpty { null }, description = description.trim(),
                            totalPages = total, lastPage = last, pagesDone = maxOf(base.pagesDone, last), startDate = (start ?: today()).toString(), endDate = end?.toString().orEmpty(), note = note.trim(),
                            libraryFileId = pdf.ifEmpty { null }, lastUsedAt = if (last > 0 && base.lastUsedAt == 0L) now else base.lastUsedAt, updatedAt = now,
                        )
                    )
                    nav.popBackStack()
                }
            }
        }
    }
}

/** Yayınevlerine puan verilmez: yalnızca öğrencinin kendi kayıtları yan yana gösterilir. */
@Composable
fun SourceCompareScreen() {
    val r = repo()
    val nav = LocalNav.current
    val rows by r.user.sourceRows().collectAsStateWithLifecycle(emptyList())
    Screen("Kaynak karşılaştırma", { nav.popBackStack() }) {
        ScrollColumn {
            Muted("Bu tablo yalnızca senin kayıtlarını gösterir. Kaynakların kalitesi hakkında bir değerlendirme değildir.")
            val list = rows.filter { !it.s.archived && (it.solved > 0 || it.minutes > 0) }
            if (list.isEmpty()) EmptyStateView(Icons.Rounded.CompareArrows, "Karşılaştıracak kayıt yok", "Kaynaklarına çalışma ekledikçe burada yan yana görünür.")
            list.sortedByDescending { it.solved }.forEach { x ->
                YCard(onClick = { nav.navigate("source/${x.s.id}") }) {
                    Text(x.s.name, style = MaterialTheme.typography.titleMedium, color = Yks.c.text)
                    Spacer(Modifier.height(4.dp))
                    KeyValue("Süre", "${x.minutes} dk")
                    KeyValue("Soru", "${x.solved}")
                    KeyValue("Doğru oranı", if (x.solved > 0) "%${Scoring.percent(x.correct, x.solved)}" else "–")
                    KeyValue("İlerleme", if (x.s.totalPages > 0) "%${(progressOf(x.s) * 100).toInt()}" else "–")
                }
            }
        }
    }
}
