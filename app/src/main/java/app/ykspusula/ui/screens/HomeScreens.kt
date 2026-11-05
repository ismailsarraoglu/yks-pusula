package app.ykspusula.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ykspusula.data.*
import app.ykspusula.domain.Scoring
import app.ykspusula.domain.Streaks
import app.ykspusula.monetize.AdPlacement
import app.ykspusula.ui.LocalNav
import app.ykspusula.ui.components.*
import app.ykspusula.ui.repo
import app.ykspusula.ui.rememberUiScope
import app.ykspusula.ui.shortDate
import app.ykspusula.ui.theme.Gradients
import app.ykspusula.ui.theme.NumberStyle
import app.ykspusula.ui.theme.Yks
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

val FIELD_LABELS = listOf("SAY" to "Sayısal", "EA" to "Eşit Ağırlık", "SOZ" to "Sözel", "DIL" to "Dil", "TYT" to "Yalnızca TYT")
val GRADES = listOf("9" to "9. sınıf", "10" to "10. sınıf", "11" to "11. sınıf", "12" to "12. sınıf", "MEZUN" to "Mezun")
val LANGUAGES = listOf("İngilizce", "Almanca", "Fransızca", "Rusça", "Arapça")
val DAY_NAMES = listOf("1" to "Pzt", "2" to "Sal", "3" to "Çar", "4" to "Per", "5" to "Cum", "6" to "Cmt", "7" to "Paz")

/* ---------- İLK AÇILIŞ ---------- */

@Composable
fun OnboardingScreen() {
    val r = repo()
    val subjects by r.content.subjects().collectAsStateWithLifecycle(emptyList())
    val appScope = app.ykspusula.ui.app().scope
    var step by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }
    var grade by rememberSaveable { mutableStateOf("12") }
    var year by rememberSaveable { mutableStateOf("2027") }
    var field by rememberSaveable { mutableStateOf("SAY") }
    var uni by rememberSaveable { mutableStateOf("") }
    var dept by rememberSaveable { mutableStateOf("") }
    var minutes by rememberSaveable { mutableStateOf("240") }
    var questions by rememberSaveable { mutableStateOf("60") }
    var days by rememberSaveable { mutableStateOf("1,2,3,4,5,6") }
    var strong by rememberSaveable { mutableStateOf("") }
    var weak by rememberSaveable { mutableStateOf("") }
    val tyt = subjects.filter { it.exam == "TYT" }.map { it.id to it.name }
    val last = 5

    fun toggle(csv: String, id: String): String {
        val s = csv.split(',').filter { it.isNotBlank() }.toMutableSet()
        if (!s.add(id)) s.remove(id)
        return s.joinToString(",")
    }

    Column(Modifier.fillMaxSize().background(Yks.c.bg).systemBarsPadding().imePadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
        if (step > 0) {
            ProgressBar(step.toFloat() / last, height = 4.dp)
            Spacer(Modifier.height(Space.l))
        }
        AnimatedContent(step, Modifier.weight(1f), label = "kurulum") { s ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Space.m)) {
                when (s) {
                    0 -> {
                        Spacer(Modifier.height(72.dp))
                        Box(Modifier.size(88.dp).clip(RoundedCornerShape(26.dp)).background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Yks.c.accent, Yks.c.purple))), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Explore, null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(48.dp))
                        }
                        Spacer(Modifier.height(Space.s))
                        Text("YKS Pusula", style = MaterialTheme.typography.displaySmall, color = Yks.c.text)
                        Text("Sınava hazırlığını tek yerden yönet.", style = MaterialTheme.typography.titleLarge, color = Yks.c.text)
                        Text("Planla. Çalış. Takip et. Hedefine yaklaş.", style = MaterialTheme.typography.bodyLarge, color = Yks.c.muted)
                        Spacer(Modifier.height(Space.s))
                        Promise(Icons.Rounded.WifiOff, "İnternetsiz çalışır", "Plan, takip ve analiz cihazında çalışır.")
                        Promise(Icons.Rounded.Lock, "Hesap gerekmez", "Çalışma verilerin bu cihazda kalır.")
                        Promise(Icons.Rounded.Explore, "Pusula Koç", "Kayıtlarına göre günlük rotanı önerir.")
                    }
                    1 -> {
                        Heading("Seni tanıyalım", "Bu bilgileri sonra Profil'den değiştirebilirsin.")
                        Field(name, { name = it }, "Adın (isteğe bağlı)")
                        Muted("Sınıf"); PillGroup(GRADES, grade) { grade = it }
                        Muted("Sınav yılı"); PillGroup(listOf("2027", "2028", "2029", "2030").map { it to it }, year) { year = it }
                    }
                    2 -> {
                        Heading("Hedefin", "Plan ve dersler hedef alanına göre düzenlenir.")
                        Muted("Hedef alan"); PillGroup(FIELD_LABELS, field) { field = it }
                        Field(uni, { uni = it }, "Hedef üniversite")
                        Field(dept, { dept = it }, "Hedef bölüm")
                    }
                    3 -> {
                        Heading("Çalışma düzenin", "Programın bu sürelere göre oluşturulur.")
                        Field(minutes, { minutes = it.filter(Char::isDigit).take(3) }, "Günlük çalışma süresi (dakika)", keyboard = KeyboardOptions(keyboardType = KeyboardType.Number))
                        Field(questions, { questions = it.filter(Char::isDigit).take(3) }, "Günlük soru hedefi", keyboard = KeyboardOptions(keyboardType = KeyboardType.Number))
                        Muted("Haftalık çalışma günleri")
                        MultiPills(DAY_NAMES, days.split(',').toSet()) { days = toggle(days, it) }
                    }
                    4 -> {
                        Heading("Güçlü olduğun dersler", "Birkaç ders seçebilir veya geçebilirsin.")
                        MultiPills(tyt, strong.split(',').toSet()) { strong = toggle(strong, it) }
                    }
                    else -> {
                        Heading("Zorlandığın dersler", "Planında bu derslere daha fazla zaman ayrılır.")
                        MultiPills(tyt, weak.split(',').toSet()) { weak = toggle(weak, it) }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = Space.m), horizontalArrangement = Arrangement.spacedBy(Space.m)) {
            if (step > 0) SoftButton("Geri", Modifier.weight(1f)) { step-- }
            MainButton(
                when (step) { 0 -> "Başla"; last -> "Bitir"; else -> "Devam" }, Modifier.weight(1f),
                enabled = step != 3 || (days.isNotBlank() && (minutes.toIntOrNull() ?: 0) > 0),
            ) {
                // Profil kaydı bu ekranı kapatır; plan üretimi yarıda kalmasın diye uygulama kapsamında çalışır.
                if (step < last) step++ else appScope.launch {
                    r.user.saveProfile(
                        UserProfile(
                            name = name.trim(), grade = grade, examYear = year.toInt(), field = field,
                            targetUniversity = uni.trim(), targetDepartment = dept.trim(),
                            dailyMinutes = minutes.toIntOrNull() ?: 240, studyDays = days, dailyQuestions = questions.toIntOrNull() ?: 60,
                            strongSubjects = strong, weakSubjects = weak, onboarded = true,
                        )
                    )
                    r.generatePlan()
                    r.ensureRoute()
                }
            }
        }
    }
}

@Composable private fun Heading(t: String, d: String) {
    Text(t, style = MaterialTheme.typography.headlineMedium, color = Yks.c.text)
    Muted(d)
    Spacer(Modifier.height(Space.xs))
}

@Composable private fun Promise(icon: ImageVector, t: String, d: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Yks.c.accent, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(Space.m))
        Column { Text(t, style = MaterialTheme.typography.titleMedium, color = Yks.c.text); Muted(d) }
    }
}

/* ---------- ANA SAYFA ---------- */

@Composable
fun HomeScreen() {
    val r = repo()
    val nav = LocalNav.current
    val scope = rememberUiScope()
    val date = remember { today() }
    val from = remember { startOf(date) }
    val to = remember { startOf(date.plusDays(1)) }
    val profile by r.user.profile().collectAsStateWithLifecycle(null)
    val configs by r.user.examConfigs().collectAsStateWithLifecycle(emptyList())
    val tasks by r.user.tasksOn(date.toString()).collectAsStateWithLifecycle(emptyList())
    val route by remember { r.user.routeOn(date.toString()) }.collectAsStateWithLifecycle(emptyList())
    val solved by r.user.solvedBetween(from, to).collectAsStateWithLifecycle(0)
    val minutes by r.user.minutesBetween(from, to).collectAsStateWithLifecycle(0)
    val week = remember { weekStart(date) }
    val weekMinutes by r.user.minutesBetween(startOf(week), to).collectAsStateWithLifecycle(0)
    val running by r.user.running().collectAsStateWithLifecycle(null)
    val missed by remember { r.missedTasks(date) }.collectAsStateWithLifecycle(emptyList())
    val mocks by r.user.mockSummaries().collectAsStateWithLifecycle(emptyList())
    val recent by r.user.recentSessions(1).collectAsStateWithLifecycle(emptyList())
    val last by r.user.lastSource().collectAsStateWithLifecycle(null)
    val active by r.user.activeDays().collectAsStateWithLifecycle(emptyList())
    val rest by r.user.restDays().collectAsStateWithLifecycle(emptyList())
    val favQuotes by r.user.bookmarkIds("QUOTE").collectAsStateWithLifecycle(emptyList())
    LaunchedEffect(Unit) { r.ensureRoute(date) }

    val p = profile ?: return
    val c = Yks.c
    val examCode = if (p.field == "DIL") "YDT" else "TYT"
    val exam = configs.firstOrNull { it.code == examCode }
    val daysLeft = exam?.let { ChronoUnit.DAYS.between(date, LocalDate.parse(it.examDate)) }
    val taskMinutes = tasks.filter { it.done }.sumOf { it.minutes }
    val studied = maxOf(minutes, taskMinutes)
    val minFrac = if (p.dailyMinutes > 0) studied.toFloat() / p.dailyMinutes else 0f
    val greeting = when (LocalTime.now().hour) { in 5..11 -> "Günaydın"; in 12..17 -> "Hoş geldin"; in 18..22 -> "İyi akşamlar"; else -> "İyi geceler" }
    val quote = remember(date) { r.quoteOfDay(date) }
    val isRest = date.toString() in rest
    val open = route.filter { it.status == "SUGGESTED" || it.status == "ACCEPTED" }

    ScrollColumn(Modifier.statusBarsPadding()) {
        Row(Modifier.padding(top = Space.s), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(if (p.name.isBlank()) greeting else "$greeting, ${p.name}", style = MaterialTheme.typography.headlineMedium, color = c.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val target = listOf(p.targetDepartment, p.targetUniversity).filter { it.isNotBlank() }.joinToString(" • ")
                Muted(target.ifBlank { "Hedefini Profil'den ekleyebilirsin" }, maxLines = 1)
            }
            IconButton({ nav.navigate("search") }) { Icon(Icons.Rounded.Search, "Ara", tint = c.text) }
            IconButton({ nav.navigate("reminders") }) { Icon(Icons.Rounded.NotificationsNone, "Bildirimler", tint = c.text) }
        }

        running?.let { m ->
            InfoRow(if (m.status == "ENTRY") "Sonuç girişi bekliyor" else "Devam eden deneme", m.title, Icons.Rounded.Timer) { nav.navigate(if (m.status == "ENTRY") "examEntry/${m.id}" else "examRun/${m.id}") }
        }

        val yearMismatch = exam != null && LocalDate.parse(exam.examDate).year != p.examYear
        if (yearMismatch) {
            YCard(onClick = { nav.navigate("examSettings") }) {
                Text("${p.examYear} sınav tarihi", style = MaterialTheme.typography.titleMedium, color = c.text)
                Muted("Bu yılın resmî takvimi uygulamada yok. Tarih açıklandığında Sınav ayarlarından girebilirsin.")
            }
        } else if (daysLeft != null && exam != null) {
            val shape = RoundedCornerShape(24.dp)
            Row(
                Modifier.fillMaxWidth().clip(shape).background(Gradients.blue).semantics(mergeDescendants = true) {}.padding(horizontal = Space.l, vertical = Space.m + 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Sınava kalan", style = MaterialTheme.typography.bodyMedium, color = androidx.compose.ui.graphics.Color(0xFFDDE7F8))
                    Text(if (daysLeft >= 0) "$daysLeft gün" else "Sınav tarihi geçti", style = MaterialTheme.typography.displaySmall.merge(NumberStyle), color = androidx.compose.ui.graphics.Color.White)
                    Text("${p.examYear} $examCode: ${app.ykspusula.ui.longDate(LocalDate.parse(exam.examDate))}", style = MaterialTheme.typography.bodyMedium, color = androidx.compose.ui.graphics.Color(0xFFDDE7F8))
                }
                Icon(Icons.Rounded.Explore, null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(44.dp))
            }
        }

        if (missed.isNotEmpty()) {
            InfoBanner("Tamamlanmayan ${missed.size} görevin var. Programını yeniden düzenleyelim mi?", Icons.Rounded.EventRepeat, c.accentLight)
            SoftButton("Programı düzenle", Modifier.fillMaxWidth(), icon = Icons.Rounded.EventRepeat) { nav.navigate("replan") }
        }

        SectionHeader("Bugünkü Rotan", "Tümü") { nav.navigate("coach") }
        when {
            isRest -> YCard { Text("Bugün dinlenme günün", style = MaterialTheme.typography.titleMedium, color = c.text); Muted("Çalışma serin bozulmaz. Dinlenmek de hazırlığın bir parçası.") }
            open.isEmpty() -> YCard {
                Muted(if (route.isEmpty()) "Rotan hazırlanıyor. Çalışma kayıtların arttıkça öneriler kişiselleşir." else "Bugünkü önerileri tamamladın veya kapattın.")
                Spacer(Modifier.height(Space.s))
                SoftButton("Yeni rota oluştur", Modifier.fillMaxWidth(), icon = Icons.Rounded.Explore) { scope.launch { r.rebuildRoute(date) } }
            }
            else -> GroupCard { open.take(4).forEachIndexed { i, x -> if (i > 0) Line(); RouteRow(x) { startRoute(nav, x) } } }
        }

        YCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Günlük hedef", style = MaterialTheme.typography.titleMedium, color = c.text)
                    Muted("Süre: $studied / ${p.dailyMinutes} dk")
                    Muted("Soru: $solved / ${p.dailyQuestions}")
                    Muted("Görev: ${tasks.count { it.done }} / ${tasks.size}")
                }
                val pct = (minFrac.coerceAtMost(1f) * 100).toInt()
                Ring(minFrac, "%$pct", description = "Günlük hedefin yüzde $pct kadarı tamamlandı")
            }
        }

        SectionHeader("Hızlı işlemler")
        TwoColumn(
            listOf(
                Triple("Çalışmaya başla", Icons.Rounded.PlayCircle, "pomodoro"), Triple("Deneme ekle", Icons.Rounded.Assignment, "external"),
                Triple("Yanlış kaydet", Icons.Rounded.ErrorOutline, "wrongEdit/new"), Triple("Kaynak ekle", Icons.AutoMirrored.Rounded.MenuBook, "sourceEdit/new"),
            )
        ) { (t, ic, route), m ->
            FeatureShortcutCard(t, when (route) { "pomodoro" -> "Odak sayacını aç"; "external" -> "Sonucunu gir"; "wrongEdit/new" -> "Deftere ekle"; else -> "Kitap veya PDF" }, ic, if (route == "external" || route == "wrongEdit/new") c.purpleLight else c.accentLight, m) { nav.navigate(route) }
        }

        last?.let { s -> if (s.lastPage > 0) InfoRow("En son ${s.lastPage}. sayfada kaldın", s.name, Icons.Rounded.Bookmark) { nav.navigate("log?source=${s.id}") } }

        SectionHeader("Son gelişim", "İstatistik") { nav.navigate("stats") { launchSingleTop = true } }
        Row2(
            { Tile(mocks.lastOrNull()?.let { m -> Scoring.formatNet(m.net) } ?: "–", "Son deneme neti", it) },
            { Tile("${weekMinutes / 60} sa ${weekMinutes % 60} dk", "Bu hafta çalışma", it) },
        )
        Row2(
            { Tile(recent.firstOrNull()?.label ?: "–", "Son çalışma", it) },
            { Tile("${Streaks.current(active, date, rest)} gün", "Çalışma serisi", it) },
        )

        MotivationCard(quote.text, quote.category, quote.id in favQuotes, onFavorite = { scope.launch { r.toggleBookmark("QUOTE", quote.id, quote.id !in favQuotes) } }, onOpen = { nav.navigate("motivation") })
        AdSlot(AdPlacement.HOME)
        TextButton({ scope.launch { if (isRest) r.user.deleteRestDay(date.toString()) else r.user.saveRestDay(RestDay(date.toString(), System.currentTimeMillis())) } }, Modifier.fillMaxWidth()) {
            Text(if (isRest) "Dinlenme gününü kaldır" else "Bugünü dinlenme günü yap", color = c.muted)
        }
    }
}

fun startRoute(nav: androidx.navigation.NavController, x: RouteItem) {
    when (x.kind) {
        "WRONGS" -> nav.navigate("wrongReview")
        "CARDS" -> nav.navigate("review/_all")
        else -> nav.navigate("pomodoro?subject=${x.subjectId.orEmpty()}&topic=${x.topicId.orEmpty()}&source=${x.sourceId.orEmpty()}&minutes=${x.minutes}&label=${android.net.Uri.encode(x.title)}&routeId=${x.id}")
    }
}

@Composable
fun RouteRow(x: RouteItem, onStart: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = Space.l, vertical = Space.m), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(when (x.priority) { 1 -> Yks.c.purpleLight; 2 -> Yks.c.accentLight; else -> Yks.c.muted }))
                Spacer(Modifier.width(6.dp))
                Text("${x.minutes} dk", style = MaterialTheme.typography.labelMedium, color = Yks.c.muted)
            }
            Text(x.title, style = MaterialTheme.typography.bodyLarge, color = Yks.c.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Muted(x.reason, maxLines = 2)
        }
        Spacer(Modifier.width(Space.s))
        IconButton(onStart) { Icon(Icons.Rounded.PlayCircle, "Başlat", tint = Yks.c.accentLight, modifier = Modifier.size(32.dp)) }
    }
}

@Composable
fun StudyTaskCard(t: StudyTask, onToggle: (Boolean) -> Unit, onOpen: () -> Unit) = TaskRow(t, onToggle, onOpen)

@Composable
fun TaskRow(t: StudyTask, onToggle: (Boolean) -> Unit, onOpen: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(start = 4.dp, end = Space.l, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            { onToggle(!t.done) },
            Modifier.semantics { stateDescription = if (t.done) "Tamamlandı" else "Tamamlanmadı" },
        ) {
            Icon(if (t.done) Icons.Outlined.CheckCircle else Icons.Outlined.Circle, if (t.done) "Tamamlandı, geri al" else "Tamamlandı olarak işaretle", tint = if (t.done) Yks.c.success else Yks.c.muted)
        }
        Column(Modifier.weight(1f)) {
            Text(t.title, style = MaterialTheme.typography.bodyLarge, color = if (t.done) Yks.c.muted else Yks.c.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Muted("${t.time} • ${t.minutes} dk" + (if (t.questionGoal > 0) " • ${t.questionGoal} soru" else "") + if (t.movedCount > 0) " • Taşındı" else "")
            if (t.reason.isNotBlank()) Muted(t.reason, maxLines = 1)
        }
    }
}
