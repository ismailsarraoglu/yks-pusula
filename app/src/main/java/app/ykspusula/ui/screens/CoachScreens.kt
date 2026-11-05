package app.ykspusula.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ykspusula.data.*
import app.ykspusula.domain.ReplanResult
import app.ykspusula.ui.LocalNav
import app.ykspusula.ui.components.*
import app.ykspusula.ui.longDate
import app.ykspusula.ui.repo
import app.ykspusula.ui.rememberUiScope
import app.ykspusula.ui.shortDate
import app.ykspusula.ui.theme.Yks
import kotlinx.coroutines.launch
import java.time.format.TextStyle
import java.util.Locale

/** Pusula Koç: cihazdaki kayıtlardan çalışan, açıklanabilir öneri sistemi. */
@Composable
fun CoachScreen() {
    val r = repo()
    val nav = LocalNav.current
    val scope = rememberUiScope()
    val date = remember { today() }
    val route by remember { r.user.routeOn(date.toString()) }.collectAsStateWithLifecycle(emptyList())
    var editing by remember { mutableStateOf<RouteItem?>(null) }
    var moving by remember { mutableStateOf<RouteItem?>(null) }
    LaunchedEffect(Unit) { r.ensureRoute(date) }
    Screen("Pusula Koç", { nav.popBackStack() }, actions = {
        IconButton({ scope.launch { r.rebuildRoute(date) } }) { Icon(Icons.Rounded.Refresh, "Rotayı yenile") }
    }) {
        ScrollColumn {
            Muted("Öneriler cihazındaki çalışma, deneme ve yanlış kayıtlarından hesaplanır. Yapay zekâ servisi değildir, internet kullanmaz. Son karar her zaman senin.")
            val visible = route.filter { it.status != "REJECTED" && it.status != "MOVED" }
            if (visible.isEmpty()) EmptyStateView(Icons.Rounded.Explore, "Bugün için öneri yok", "Yeni rota oluşturabilir veya çalışma kaydı ekleyebilirsin.", "Yeni rota") { scope.launch { r.rebuildRoute(date) } }
            visible.forEach { x ->
                YCard(padding = Space.m) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Tag(when (x.priority) { 1 -> "Öncelikli"; 2 -> "Önemli"; else -> "İsteğe bağlı" }, if (x.priority == 1) Yks.c.purpleLight else Yks.c.accentLight)
                        Spacer(Modifier.width(Space.s))
                        Muted("${x.minutes} dk")
                        Spacer(Modifier.weight(1f))
                        when (x.status) { "ACCEPTED" -> Tag("Programda", Yks.c.success); "DONE" -> Tag("Tamamlandı", Yks.c.success); else -> {} }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(x.title, style = MaterialTheme.typography.titleMedium, color = Yks.c.text)
                    Muted(x.reason)
                    if (x.status != "DONE") {
                        Spacer(Modifier.height(Space.s))
                        Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                            MainButton("Başlat", Modifier.weight(1f), icon = Icons.Rounded.PlayArrow) { startRoute(nav, x) }
                            if (x.status == "SUGGESTED") SoftButton("Kabul et", Modifier.weight(1f), icon = Icons.Rounded.Check) { scope.launch { r.acceptRoute(x.id) } }
                        }
                        Row {
                            TextButton({ editing = x }) { Text("Düzenle") }
                            TextButton({ moving = x }) { Text("Taşı") }
                            TextButton({ scope.launch { r.rejectRoute(x.id) } }) { Text("Reddet", color = Yks.c.muted) }
                            TextButton({ scope.launch { r.deleteRoute(x.id) } }) { Text("Sil", color = Yks.c.muted) }
                        }
                    }
                }
            }
            SoftButton("Haftalık değerlendirme", Modifier.fillMaxWidth(), icon = Icons.Rounded.Insights) { nav.navigate("reports") }
        }
    }
    editing?.let { x ->
        var title by remember { mutableStateOf(x.title) }
        var minutes by remember { mutableStateOf("${x.minutes}") }
        AlertDialog(
            onDismissRequest = { editing = null }, containerColor = Yks.c.card, title = { Text("Öneriyi düzenle") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(Space.s)) { Field(title, { title = it }, "Başlık"); NumberField(minutes, { minutes = it }, "Süre (dakika)", max = 3) } },
            confirmButton = { TextButton({ scope.launch { r.editRoute(x.id, title.trim(), minutes.toIntOrNull() ?: x.minutes) }; editing = null }, enabled = title.isNotBlank()) { Text("Kaydet") } },
            dismissButton = { TextButton({ editing = null }) { Text("Vazgeç") } },
        )
    }
    moving?.let { x ->
        AlertDialog(
            onDismissRequest = { moving = null }, containerColor = Yks.c.card, title = { Text("Hangi güne taşınsın?") },
            text = {
                Column {
                    (1..6).forEach { i ->
                        val d = date.plusDays(i.toLong())
                        TextButton({ scope.launch { r.moveRoute(x.id, d) }; moving = null }, Modifier.fillMaxWidth()) {
                            Text("${d.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("tr", "TR"))} (${d.dayOfMonth})", color = Yks.c.text)
                        }
                    }
                }
            },
            confirmButton = { TextButton({ moving = null }) { Text("Vazgeç") } },
        )
    }
}

/** Kaçırılan görevleri kapasiteyi aşmadan dağıtır; öğrenci önizleyip onaylar. */
@Composable
fun ReplanScreen() {
    val r = repo()
    val nav = LocalNav.current
    val scope = rememberUiScope()
    val missed by remember { r.missedTasks() }.collectAsStateWithLifecycle(emptyList())
    var mode by remember { mutableStateOf("") }
    var preview by remember { mutableStateOf<ReplanResult?>(null) }
    LaunchedEffect(mode, missed.size) { if (mode.isNotEmpty()) preview = r.previewReplan(mode == "IMPORTANT") }
    Screen("Programı düzenle", { nav.popBackStack() }) {
        ScrollColumn {
            if (missed.isEmpty()) { EmptyStateView(Icons.Rounded.CheckCircle, "Kaçırılan görev yok", "Programın yolunda görünüyor."); return@ScrollColumn }
            val firstDay = missed.minOf { it.date }
            val name = java.time.LocalDate.parse(firstDay).dayOfWeek.getDisplayName(TextStyle.FULL, Locale("tr", "TR"))
            YCard {
                Text("$name gününden bu yana ${missed.size} görevi tamamlayamadın.", style = MaterialTheme.typography.titleMedium, color = Yks.c.text)
                Muted("Bu çok normal. Haftanın kalanını yeniden düzenleyelim mi?")
            }
            GroupCard {
                NavRow("Kalan günlere dağıt", "Bütün görevler boş zamanlara yerleşir", Icons.Rounded.EventRepeat, onClick = { mode = "ALL" }); Line()
                NavRow("Yalnızca önemli görevleri taşı", "Öncelikli olanlar taşınır, diğerleri geçilir", Icons.Rounded.Flag, onClick = { mode = "IMPORTANT" }); Line()
                NavRow("Kendim düzenleyeceğim", "Plan ekranını aç", Icons.Rounded.Edit, onClick = { nav.navigate("plan") })
            }
            preview?.let { p ->
                Section("Önizleme")
                if (p.moves.isEmpty()) Muted("Taşınacak uygun boş zaman bulunamadı. Plan ayarlarından kapasiteni artırabilirsin.")
                else GroupCard {
                    p.moves.forEachIndexed { i, m ->
                        if (i > 0) Line()
                        Column(Modifier.padding(Space.m)) {
                            Text(m.title, style = MaterialTheme.typography.bodyMedium, color = Yks.c.text)
                            Muted("${longDate(m.from)} → ${longDate(m.to)} ${m.time}")
                        }
                    }
                }
                if (p.unplaced.isNotEmpty()) Muted("${p.unplaced.size} görev günlük kapasiteye sığmadı.")
                MainButton("Onayla", Modifier.fillMaxWidth(), enabled = p.moves.isNotEmpty() || mode == "IMPORTANT") {
                    scope.launch { r.applyReplan(p.moves, mode == "IMPORTANT"); nav.popBackStack() }
                }
            }
            SoftButton("Kaçırılanları geç", Modifier.fillMaxWidth(), color = Yks.c.muted) { scope.launch { r.skipMissed(); nav.popBackStack() } }
        }
    }
}

@Composable
fun PlanHistoryScreen() {
    val r = repo()
    val nav = LocalNav.current
    val revisions by r.user.revisions().collectAsStateWithLifecycle(emptyList())
    Screen("Plan geçmişi", { nav.popBackStack() }) {
        ScrollColumn {
            if (revisions.isEmpty()) EmptyStateView(Icons.Rounded.History, "Geçmiş yok", "Plan değiştiğinde önceki hâli burada saklanır.")
            else GroupCard {
                revisions.forEachIndexed { i, v ->
                    if (i > 0) Line()
                    val count = Regex("\"id\"").findAll(v.snapshotJson).count()
                    Column(Modifier.padding(Space.m)) {
                        Text(v.summary, style = MaterialTheme.typography.bodyMedium, color = Yks.c.text)
                        Muted("${shortDate(v.createdAt)} • $count görev saklandı")
                    }
                }
            }
        }
    }
}
