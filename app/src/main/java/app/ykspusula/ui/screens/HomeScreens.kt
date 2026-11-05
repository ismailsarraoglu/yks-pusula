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
import app.ykspusula.domain.Advice
import app.ykspusula.domain.Scoring
import app.ykspusula.ui.LocalNav
import app.ykspusula.ui.components.*
import app.ykspusula.ui.repo
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
                        Box(Modifier.size(84.dp).clip(RoundedCornerShape(24.dp)).background(Yks.c.accent), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Explore, null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(48.dp))
                        }
                        Spacer(Modifier.height(Space.s))
                        Text("YKS Pusula", style = MaterialTheme.typography.displaySmall, color = Yks.c.text)
                        Text("Ne çalışacağını gösteren, gelişimini ölçen kişisel çalışma merkezin.", style = MaterialTheme.typography.bodyLarge, color = Yks.c.muted)
                        Spacer(Modifier.height(Space.s))
                        Promise(Icons.Rounded.WifiOff, "İnternet gerekmez", "Tüm özellikler cihazında çalışır.")
                        Promise(Icons.Rounded.Lock, "Hesap yok", "Verilerin bu cihazda kalır.")
                        Promise(Icons.Rounded.Block, "Reklam yok", "Takip ve üçüncü taraf analitik de yok.")
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
    val scope = app.ykspusula.ui.rememberUiScope()
    val date = remember { today() }
    val from = remember { startOf(date) }
    val to = remember { startOf(date.plusDays(1)) }
    val profile by r.user.profile().collectAsStateWithLifecycle(null)
    val configs by r.user.examConfigs().collectAsStateWithLifecycle(emptyList())
    val tasks by r.user.tasksOn(date.toString()).collectAsStateWithLifecycle(emptyList())
    val solved by r.user.solvedBetween(from, to).collectAsStateWithLifecycle(0)
    val minutes by r.user.minutesBetween(from, to).collectAsStateWithLifecycle(0)
    val completed by r.user.completedBetween(from, to).collectAsStateWithLifecycle(0)
    val weak by r.user.weakTopics(3, 3).collectAsStateWithLifecycle(emptyList())
    val wrongsDue by r.user.wrongDue(System.currentTimeMillis()).collectAsStateWithLifecycle(0)
    val running by r.user.running().collectAsStateWithLifecycle(null)
    var advice by remember { mutableStateOf<List<Advice>>(emptyList()) }
    LaunchedEffect(solved, minutes) { advice = r.adviceNow() }

    val p = profile ?: return
    val exam = configs.firstOrNull { it.code == "TYT" }
    val daysLeft = exam?.let { ChronoUnit.DAYS.between(date, LocalDate.parse(it.examDate)) }
    val taskMinutes = tasks.filter { it.done }.sumOf { it.minutes }
    val studied = maxOf(minutes, taskMinutes)
    val minFrac = if (p.dailyMinutes > 0) studied.toFloat() / p.dailyMinutes else 0f
    val qFrac = if (p.dailyQuestions > 0) solved.toFloat() / p.dailyQuestions else 0f
    val total = ((minFrac.coerceAtMost(1f) + qFrac.coerceAtMost(1f)) / 2f)
    val greeting = when (LocalTime.now().hour) { in 5..11 -> "Günaydın"; in 12..17 -> "İyi çalışmalar"; in 18..22 -> "İyi akşamlar"; else -> "İyi geceler" }

    ScrollColumn(Modifier.statusBarsPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(if (p.name.isBlank()) greeting else "$greeting, ${p.name}", style = MaterialTheme.typography.titleLarge, color = Yks.c.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val target = listOf(p.targetDepartment, p.targetUniversity).filter { it.isNotBlank() }.joinToString(" • ")
                Muted(target.ifBlank { "Hedefini Profil'den ekleyebilirsin" }, maxLines = 1)
            }
            IconButton({ nav.navigate("search") }) { Icon(Icons.Rounded.Search, "Ara", tint = Yks.c.text) }
            IconButton({ nav.navigate("reminders") }) { Icon(Icons.Rounded.NotificationsNone, "Hatırlatıcılar", tint = Yks.c.text) }
            IconButton({ nav.navigate("profile") { launchSingleTop = true } }) { Icon(Icons.Rounded.AccountCircle, "Profil", tint = Yks.c.text) }
        }

        running?.let { m ->
            YCard(onClick = { nav.navigate("exam/${m.id}") }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Timer, null, tint = Yks.c.warning)
                    Spacer(Modifier.width(Space.m))
                    Column(Modifier.weight(1f)) {
                        Text("Devam eden deneme", style = MaterialTheme.typography.titleMedium, color = Yks.c.text)
                        Muted(m.title)
                    }
                    Text("Devam et", color = Yks.c.accent, style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        val yearMismatch = exam != null && LocalDate.parse(exam.examDate).year != p.examYear
        if (yearMismatch) {
            YCard(onClick = { nav.navigate("examSettings") }) {
                Text("${p.examYear} sınav tarihi", style = MaterialTheme.typography.titleMedium, color = Yks.c.text)
                Muted("Bu yılın resmî takvimi uygulamada yok. Tarih açıklandığında Sınav ayarlarından girebilirsin.")
            }
        } else if (daysLeft != null && exam != null) {
            YCard(Modifier.semantics(mergeDescendants = true) {}) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Muted("Sınava kalan")
                        Text(if (daysLeft >= 0) "$daysLeft gün" else "Sınav tarihi geçti", style = MaterialTheme.typography.displaySmall, color = Yks.c.text)
                        Muted("${p.examYear} TYT: ${app.ykspusula.ui.longDate(LocalDate.parse(exam.examDate))}")
                    }
                    Icon(Icons.Rounded.Explore, null, tint = Yks.c.accent, modifier = Modifier.size(44.dp))
                }
            }
        }

        YCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Bugünkü ilerleme", style = MaterialTheme.typography.titleMedium, color = Yks.c.text)
                    Muted("Çalışma: $studied / ${p.dailyMinutes} dk")
                    Muted("Soru: $solved / ${p.dailyQuestions}")
                    Muted("Tamamlanan konu: $completed")
                }
                Ring(total, "%${(total * 100).toInt()}", description = "Günlük hedefin yüzde ${(total * 100).toInt()} kadarı tamamlandı")
            }
        }

        Section("Bugünkü program") { TextButton({ nav.navigate("plan") }) { Text("Tümü") } }
        if (tasks.isEmpty()) {
            YCard {
                Muted("Bugün için görev yok.")
                Spacer(Modifier.height(Space.s))
                SoftButton("Otomatik program oluştur", icon = Icons.Rounded.AutoAwesome) { scope.launch { r.generatePlan() } }
            }
        } else GroupCard {
            tasks.forEachIndexed { i, t ->
                if (i > 0) Line()
                TaskRow(t, { d -> scope.launch { r.toggleTask(t, d) } }) {
                    if (t.kind == "WRONGS") nav.navigate("wrongs") else if (t.topicId != null) nav.navigate("topic/${t.topicId}")
                }
            }
        }

        Section("Hızlı erişim")
        QuickGrid(
            listOf(
                Triple("Soru çöz", Icons.Rounded.EditNote, "bank"), Triple("Deneme başlat", Icons.Rounded.Assignment, "exams"),
                Triple("Pomodoro", Icons.Rounded.Timer, "pomodoro"), Triple("Yanlışlarım", Icons.Rounded.ErrorOutline, "wrongs"),
                Triple("Konu takibi", Icons.Rounded.Checklist, "tracker"), Triple("Program", Icons.Rounded.CalendarMonth, "plan"),
            )
        ) { route -> if (route == "exams") nav.navigate("exams") { launchSingleTop = true } else nav.navigate(route) }

        Section("Dikkat etmen gerekenler")
        if (weak.isEmpty()) {
            YCard { Muted("Soru çözdükçe en zayıf üç konun burada görünür. Bir konunun sayılması için en az 3 soru gerekir.") }
        } else GroupCard {
            weak.forEachIndexed { i, w ->
                if (i > 0) Line()
                val pct = Scoring.percent(w.right, w.total)
                NavRow(w.name, "${w.subjectName} • ${w.total} soruda %$pct başarı", onClick = { nav.navigate("topic/${w.topicId}") })
            }
        }

        if (advice.isNotEmpty()) {
            Section("Öneriler")
            advice.take(4).forEach { a ->
                YCard {
                    Row {
                        Icon(Icons.Rounded.Lightbulb, null, tint = Yks.c.warning, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(Space.s))
                        Text(a.text, style = MaterialTheme.typography.bodyMedium, color = Yks.c.text)
                    }
                }
            }
        }
        if (wrongsDue > 0) {
            MainButton("$wrongsDue yanlışı tekrar et", Modifier.fillMaxWidth(), icon = Icons.Rounded.Replay) { nav.navigate("solve?mode=REVIEW") }
        }
    }
}

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
            Muted("${t.time} • ${t.minutes} dk" + if (t.questionGoal > 0) " • ${t.questionGoal} soru" else "")
        }
    }
}

@Composable
fun QuickGrid(items: List<Triple<String, ImageVector, String>>, onOpen: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
        items.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s), modifier = Modifier.height(IntrinsicSize.Min)) {
                row.forEach { (label, icon, route) ->
                    YCard(Modifier.weight(1f).fillMaxHeight(), onClick = { onOpen(route) }, padding = Space.m) {
                        Icon(icon, null, tint = Yks.c.accent, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.height(6.dp))
                        Text(label, style = MaterialTheme.typography.labelMedium, color = Yks.c.text, maxLines = 2)
                    }
                }
            }
        }
    }
}
