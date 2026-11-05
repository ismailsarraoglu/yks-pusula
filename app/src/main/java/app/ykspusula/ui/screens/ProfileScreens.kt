package app.ykspusula.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ykspusula.BuildConfig
import app.ykspusula.data.*
import app.ykspusula.domain.*
import app.ykspusula.notify.Notifier
import app.ykspusula.notify.REMINDER_TYPES
import app.ykspusula.ui.LocalNav
import app.ykspusula.ui.app
import app.ykspusula.ui.components.*
import app.ykspusula.ui.repo
import app.ykspusula.ui.shortDate
import app.ykspusula.ui.theme.Yks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val PRIVACY_TEXT = "Tüm kişisel verilerin bu cihazda saklanır. Uygulama hesabı veya sunucusu bulunmamaktadır."

@Composable
fun ProfileScreen() {
    val r = repo()
    val nav = LocalNav.current
    val profile by r.user.profile().collectAsStateWithLifecycle(null)
    val p = profile ?: return
    ScrollColumn(Modifier.statusBarsPadding()) {
        Text("Profil", style = MaterialTheme.typography.headlineMedium, color = Yks.c.text)
        YCard(onClick = { nav.navigate("profileEdit") }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(56.dp).clip(CircleShape).background(Yks.c.soft), contentAlignment = Alignment.Center) {
                    Text(p.name.take(1).uppercase().ifEmpty { "Ö" }, style = MaterialTheme.typography.titleLarge, color = Yks.c.accent)
                }
                Spacer(Modifier.width(Space.m))
                Column(Modifier.weight(1f)) {
                    Text(p.name.ifBlank { "Öğrenci" }, style = MaterialTheme.typography.titleMedium, color = Yks.c.text)
                    Muted("${GRADES.firstOrNull { it.first == p.grade }?.second} • ${p.examYear} • ${FIELD_LABELS.firstOrNull { it.first == p.field }?.second}")
                    if (p.targetDepartment.isNotBlank()) Muted("Hedef: ${p.targetDepartment}", maxLines = 1)
                }
                Icon(Icons.Rounded.Edit, "Profili düzenle", tint = Yks.c.muted)
            }
        }
        Section("Gelişim")
        GroupCard {
            NavRow("Rozetler ve hedefler", "Çalışma alışkanlığın", Icons.Rounded.EmojiEvents, Yks.c.warning, { nav.navigate("badges") }); Line()
            NavRow("Üniversite ve bölüm hedefleri", "Birden fazla hedef kaydet", Icons.Rounded.School, onClick = { nav.navigate("targets") })
        }
        Section("Ayarlar")
        GroupCard {
            NavRow("Uygulama ayarları", "Tema, yazı boyutu, hedefler", Icons.Rounded.Settings, onClick = { nav.navigate("settings") }); Line()
            NavRow("Bildirimler", "Yerel hatırlatıcılar", Icons.Rounded.Notifications, onClick = { nav.navigate("reminders") }); Line()
            NavRow("Sınav ayarları", "Tarih, süre, net kuralı", Icons.Rounded.Tune, onClick = { nav.navigate("examSettings") }); Line()
            NavRow("Yedekleme ve geri yükleme", "Verilerini dosyaya aktar", Icons.Rounded.Backup, onClick = { nav.navigate("backup") }); Line()
            NavRow("İçerik paketleri", "Sürüm, kapsam ve içe aktarma", Icons.Rounded.Inventory2, onClick = { nav.navigate("packs") })
        }
        YCard {
            Row {
                Icon(Icons.Rounded.Lock, null, tint = Yks.c.success, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(Space.s))
                Text(PRIVACY_TEXT, style = MaterialTheme.typography.bodyMedium, color = Yks.c.text)
            }
        }
        Muted("YKS Pusula ${BuildConfig.VERSION_NAME}")
    }
}

@Composable
fun ProfileEditScreen() {
    val r = repo()
    val nav = LocalNav.current
    val scope = rememberCoroutineScope()
    val subjects by r.content.subjects().collectAsStateWithLifecycle(emptyList())
    var p by remember { mutableStateOf<UserProfile?>(null) }
    LaunchedEffect(Unit) { p = r.user.profileNow() }
    Screen("Profili düzenle", { nav.popBackStack() }) {
        val u = p ?: return@Screen
        fun toggle(csv: String, id: String): String {
            val s = csv.split(',').filter { it.isNotBlank() }.toMutableSet()
            if (!s.add(id)) s.remove(id)
            return s.sortedBy { it }.joinToString(",")
        }
        val tyt = subjects.filter { it.exam == "TYT" }.map { it.id to it.name }
        ScrollColumn {
            Field(u.name, { v -> p = u.copy(name = v) }, "Ad")
            Muted("Sınıf"); PillGroup(GRADES, u.grade) { p = u.copy(grade = it) }
            Muted("Sınav yılı"); PillGroup(listOf(2027, 2028, 2029, 2030).map { "$it" to "$it" }, "${u.examYear}") { p = u.copy(examYear = it.toInt()) }
            Muted("Hedef alan"); PillGroup(FIELD_LABELS, u.field) { p = u.copy(field = it) }
            Muted("YDT dili"); PillGroup(LANGUAGES.map { it to it }, u.ydtLanguage) { p = u.copy(ydtLanguage = it) }
            Field(u.targetUniversity, { v -> p = u.copy(targetUniversity = v) }, "Hedef üniversite")
            Field(u.targetDepartment, { v -> p = u.copy(targetDepartment = v) }, "Hedef bölüm")
            Field(u.dailyMinutes.toString(), { v -> p = u.copy(dailyMinutes = v.filter(Char::isDigit).take(3).toIntOrNull() ?: 0) }, "Günlük çalışma (dakika)", keyboard = KeyboardOptions(keyboardType = KeyboardType.Number))
            Field(u.dailyQuestions.toString(), { v -> p = u.copy(dailyQuestions = v.filter(Char::isDigit).take(3).toIntOrNull() ?: 0) }, "Günlük soru hedefi", keyboard = KeyboardOptions(keyboardType = KeyboardType.Number))
            Field(u.startHour.toString(), { v -> p = u.copy(startHour = (v.filter(Char::isDigit).take(2).toIntOrNull() ?: 0).coerceIn(0, 23)) }, "Çalışma başlangıç saati", keyboard = KeyboardOptions(keyboardType = KeyboardType.Number))
            Muted("Çalışma günleri"); MultiPills(DAY_NAMES, u.studyDays.split(',').toSet()) { p = u.copy(studyDays = toggle(u.studyDays, it)) }
            Muted("Güçlü dersler"); MultiPills(tyt, u.strongSubjects.split(',').toSet()) { p = u.copy(strongSubjects = toggle(u.strongSubjects, it)) }
            Muted("Zorlandığın dersler"); MultiPills(tyt, u.weakSubjects.split(',').toSet()) { p = u.copy(weakSubjects = toggle(u.weakSubjects, it)) }
            MainButton("Kaydet", Modifier.fillMaxWidth(), enabled = u.studyDays.isNotBlank() && u.dailyMinutes > 0) {
                scope.launch { r.user.saveProfile(u); nav.popBackStack() }
            }
        }
    }
}

@Composable
fun SettingsScreen() {
    val r = repo()
    val nav = LocalNav.current
    val scope = rememberCoroutineScope()
    val s by r.settings.collectAsStateWithLifecycle(AppSettings())
    var askReset by remember { mutableStateOf(false) }
    var askReset2 by remember { mutableStateOf(false) }
    Screen("Ayarlar", { nav.popBackStack() }) {
        ScrollColumn {
            Section("Görünüm")
            PillGroup(listOf("SYSTEM" to "Sistem", "LIGHT" to "Açık", "DARK" to "Koyu"), s.theme) { v -> scope.launch { r.updateSettings { it[Keys.theme] = v } } }
            Muted("Yazı büyüklüğü")
            PillGroup(listOf("0.9" to "Küçük", "1.0" to "Normal", "1.15" to "Büyük", "1.3" to "Çok büyük"), "%.2f".format(java.util.Locale.US, s.fontScale).trimEnd('0').let { if (it.endsWith(".")) it + "0" else it }) { v -> scope.launch { r.updateSettings { it[Keys.font] = v.toFloat() } } }
            Section("Pomodoro varsayılanı")
            PillGroup(listOf("25-5" to "25 / 5", "50-10" to "50 / 10", "90-15" to "90 / 15"), "${s.pomodoroWork}-${s.pomodoroBreak}") { v ->
                scope.launch { r.updateSettings { it[Keys.pWork] = v.substringBefore('-').toInt(); it[Keys.pBreak] = v.substringAfter('-').toInt() } }
            }
            Section("Haftalık ve aylık hedef")
            var weekly by remember(s.weeklyGoalMinutes) { mutableStateOf(if (s.weeklyGoalMinutes > 0) s.weeklyGoalMinutes.toString() else "") }
            var monthly by remember(s.monthlyGoalQuestions) { mutableStateOf(if (s.monthlyGoalQuestions > 0) s.monthlyGoalQuestions.toString() else "") }
            Field(weekly, { weekly = it.filter(Char::isDigit).take(5) }, "Haftalık çalışma hedefi (dakika)", keyboard = KeyboardOptions(keyboardType = KeyboardType.Number))
            Field(monthly, { monthly = it.filter(Char::isDigit).take(5) }, "Aylık soru hedefi", keyboard = KeyboardOptions(keyboardType = KeyboardType.Number))
            SoftButton("Hedefleri kaydet", Modifier.fillMaxWidth()) {
                scope.launch { r.updateSettings { it[Keys.weekly] = weekly.toIntOrNull() ?: 0; it[Keys.monthly] = monthly.toIntOrNull() ?: 0 } }
            }
            Section("Gizlilik")
            YCard { Text(PRIVACY_TEXT, style = MaterialTheme.typography.bodyMedium, color = Yks.c.text) }
            Section("Veriler")
            GroupCard {
                NavRow("Veri dışa aktarma ve içe aktarma", "Yedek dosyası", Icons.Rounded.Backup, onClick = { nav.navigate("backup") }); Line()
                NavRow("Verileri sıfırla", "Tüm kişisel verilerini siler", Icons.Rounded.DeleteForever, Yks.c.error, { askReset = true })
            }
        }
    }
    if (askReset) ConfirmDialog("Veriler sıfırlansın mı?", "Çalışma geçmişin, denemelerin, notların, yanlışların ve profilin silinir. Önce yedek almanı öneririz.", "Devam", true, { askReset = false }) { askReset = false; askReset2 = true }
    if (askReset2) ConfirmDialog("Emin misin?", "Bu işlem geri alınamaz.", "Hepsini sil", true, { askReset2 = false }) {
        askReset2 = false
        scope.launch { r.clearUserData() }
    }
}

@Composable
fun RemindersScreen() {
    val r = repo()
    val nav = LocalNav.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val rows by r.user.reminders().collectAsStateWithLifecycle(emptyList())
    var pendingEnable by remember { mutableStateOf<Reminder?>(null) }
    var denied by remember { mutableStateOf(false) }
    fun apply(x: Reminder) { scope.launch { r.user.saveReminder(x); Notifier.schedule(context, x) } }
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        denied = !ok
        pendingEnable?.let { if (ok) apply(it) }
        pendingEnable = null
    }
    Screen("Bildirimler", { nav.popBackStack() }) {
        ScrollColumn {
            Muted("Bildirimler cihazda oluşturulur, internet kullanmaz. Android pil tasarrufu nedeniyle birkaç dakika gecikebilir.")
            if (denied) Text("Bildirim izni verilmedi. Android ayarlarından açabilirsin.", color = Yks.c.error, style = MaterialTheme.typography.bodyMedium)
            REMINDER_TYPES.forEach { t ->
                val cur = rows.firstOrNull { it.type == t.type } ?: Reminder(t.type)
                YCard(padding = Space.m) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(t.title, style = MaterialTheme.typography.titleMedium, color = Yks.c.text)
                            Muted(t.detail)
                        }
                        Switch(cur.enabled, { on ->
                            val x = cur.copy(enabled = on)
                            if (on && Build.VERSION.SDK_INT >= 33 && !Notifier.canNotify(context)) { pendingEnable = x; perm.launch(Manifest.permission.POST_NOTIFICATIONS) } else apply(x)
                        })
                    }
                    if (cur.enabled) {
                        Spacer(Modifier.height(Space.s))
                        Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                            Field(cur.hour.toString(), { v -> v.filter(Char::isDigit).take(2).toIntOrNull()?.takeIf { it in 0..23 }?.let { apply(cur.copy(hour = it)) } }, "Saat", Modifier.weight(1f), keyboard = KeyboardOptions(keyboardType = KeyboardType.Number))
                            Field(cur.minute.toString(), { v -> v.filter(Char::isDigit).take(2).toIntOrNull()?.takeIf { it in 0..59 }?.let { apply(cur.copy(minute = it)) } }, "Dakika", Modifier.weight(1f), keyboard = KeyboardOptions(keyboardType = KeyboardType.Number))
                        }
                        if (t.weekly) { Spacer(Modifier.height(Space.s)); PillGroup(DAY_NAMES, cur.dayOfWeek.toString()) { apply(cur.copy(dayOfWeek = it.toInt())) } }
                    }
                }
            }
        }
    }
}

@Composable
fun BackupScreen() {
    val a = app()
    val r = a.repo
    val nav = LocalNav.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<BackupFile?>(null) }
    var replaceAsk by remember { mutableStateOf(false) }

    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            try {
                val text = a.backup.export(BuildConfig.VERSION_NAME)
                withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(text.toByteArray()) } }
                isError = false; status = "Yedek kaydedildi."
            } catch (e: Exception) { isError = true; status = "Yedek yazılamadı." }
        }
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            try {
                val text = withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() } }
                when (val c = BackupCodec.decode(text)) {
                    is BackupCheck.Bad -> { isError = true; status = c.message }
                    is BackupCheck.Ok -> { pending = c.file; status = "" }
                }
            } catch (e: Exception) { isError = true; status = "Dosya okunamadı." }
        }
    }
    fun restore(replace: Boolean) {
        val f = pending ?: return
        scope.launch {
            try {
                a.backup.restore(f.data, replace, BuildConfig.VERSION_NAME)
                Notifier.rescheduleAll(context)
                isError = false; status = if (replace) "Yedek geri yüklendi. Önceki verilerin cihazda güvenlik kopyası olarak saklandı." else "Yedek mevcut verilerle birleştirildi."
                pending = null
            } catch (e: Exception) { isError = true; status = "Geri yükleme başarısız. Verilerin değiştirilmedi." }
        }
    }
    Screen("Yedekleme", { nav.popBackStack() }) {
        ScrollColumn {
            Muted("Yedek dosyası profilini, çalışma geçmişini, denemelerini, konu ilerlemeni, yanlışlarını, notlarını, kartlarını, hedeflerini ve ayarlarını içerir. Dosya yalnızca senin seçtiğin yere kaydedilir. PDF kaynakları dahil değildir.")
            MainButton("Dışa aktar", Modifier.fillMaxWidth(), icon = Icons.Rounded.Upload) { create.launch("yks-pusula-yedek-${today()}.json") }
            SoftButton("Yedekten geri yükle", Modifier.fillMaxWidth(), icon = Icons.Rounded.Download) { open.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }
            if (status.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (isError) Icons.Rounded.ErrorOutline else Icons.Rounded.CheckCircle, null, tint = if (isError) Yks.c.error else Yks.c.success, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(Space.s))
                    Text(status, style = MaterialTheme.typography.bodyMedium, color = Yks.c.text)
                }
            }
            pending?.let { f ->
                YCard {
                    Text("Yedek doğrulandı", style = MaterialTheme.typography.titleMedium, color = Yks.c.text)
                    Muted("Tarih: ${shortDate(f.createdAt)} • Sürüm ${f.appVersion}")
                    Muted("${f.data.attempts.size} cevap, ${f.data.mocks.size} deneme, ${f.data.notes.size} not, ${f.data.wrongs.size} yanlış")
                    Spacer(Modifier.height(Space.s))
                    MainButton("Mevcut verilerle birleştir", Modifier.fillMaxWidth()) { restore(false) }
                    Spacer(Modifier.height(Space.s))
                    SoftButton("Mevcut verilerin yerine geç", Modifier.fillMaxWidth(), color = Yks.c.error) { replaceAsk = true }
                }
            }
        }
    }
    if (replaceAsk) ConfirmDialog("Veriler değiştirilsin mi?", "Mevcut verilerin bu yedekle değiştirilir. İşlemden önce cihaz içine otomatik güvenlik kopyası alınır.", "Değiştir", true, { replaceAsk = false }) { replaceAsk = false; restore(true) }
}

@Composable
fun TargetsScreen() {
    val r = repo()
    val nav = LocalNav.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val unis by r.user.universities().collectAsStateWithLifecycle(emptyList())
    val depts by r.user.departments().collectAsStateWithLifecycle(emptyList())
    val records by r.user.universityRecords().collectAsStateWithLifecycle(emptyList())
    var addUni by remember { mutableStateOf(false) }
    var addDeptFor by remember { mutableStateOf<TargetUniversity?>(null) }
    var message by remember { mutableStateOf("") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            try {
                val name = uri.lastPathSegment.orEmpty()
                val text = withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() } }
                val p = UniversityImport.parse(name, text)
                val errors = UniversityImport.errors(p)
                if (errors.isNotEmpty()) { message = "Paket reddedildi: " + errors.take(3).joinToString("; ") }
                else {
                    val packId = "uni-${p.version}"
                    r.user.deleteUniversityPack(packId)
                    r.user.saveUniversityRecords(p.records.map { UniversityRecord(packId = packId, university = it.university, city = it.city, department = it.department, scoreType = it.scoreType, quota = it.quota, baseScore = it.baseScore, baseRank = it.baseRank, dataYear = p.dataYear) })
                    r.content.pack(ImportedContentPack(packId, "Üniversite verisi", p.version, "UNIVERSITY", System.currentTimeMillis(), 0, p.source, ""))
                    message = "${p.records.size} kayıt içe aktarıldı. Kaynak: ${p.source}"
                }
            } catch (e: Exception) { message = "Dosya okunamadı veya biçimi hatalı." }
        }
    }
    Screen("Hedeflerim", { nav.popBackStack() }, actions = {
        IconButton({ addUni = true }) { Icon(Icons.Rounded.Add, "Üniversite ekle") }
    }) {
        ScrollColumn {
            if (unis.isEmpty()) Empty(Icons.Rounded.School, "Henüz hedef yok", "Hedeflediğin üniversite ve bölümleri ekleyebilirsin.", "Üniversite ekle") { addUni = true }
            unis.forEach { u ->
                YCard(padding = Space.m) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(u.name, style = MaterialTheme.typography.titleMedium, color = Yks.c.text)
                            if (u.city.isNotBlank()) Muted(u.city)
                        }
                        IconButton({ scope.launch { r.user.saveUniversity(u.copy(favorite = !u.favorite)) } }) { Icon(if (u.favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder, "Favori", tint = if (u.favorite) Yks.c.warning else Yks.c.muted) }
                        IconButton({ scope.launch { r.user.deleteUniversity(u) } }) { Icon(Icons.Rounded.DeleteOutline, "Üniversiteyi sil", tint = Yks.c.muted) }
                    }
                    depts.filter { it.universityId == u.id }.forEach { d ->
                        Line()
                        Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(d.name, style = MaterialTheme.typography.bodyLarge, color = Yks.c.text)
                                Muted(listOfNotNull(d.scoreType.ifBlank { null }, d.targetRank?.let { "Hedef sıra: $it" }, d.targetScore?.let { "Hedef puan: $it" }).joinToString(" • "))
                                if (d.note.isNotBlank()) Muted(d.note)
                            }
                            IconButton({ scope.launch { r.user.saveDepartment(d.copy(favorite = !d.favorite)) } }) { Icon(if (d.favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder, "Favori bölüm", tint = if (d.favorite) Yks.c.warning else Yks.c.muted) }
                            IconButton({ scope.launch { r.user.deleteDepartment(d) } }) { Icon(Icons.Rounded.DeleteOutline, "Bölümü sil", tint = Yks.c.muted) }
                        }
                    }
                    TextButton({ addDeptFor = u }) { Text("Bölüm ekle") }
                }
            }
            Section("Üniversite veri paketi")
            YCard {
                Muted("Uygulamada canlı YÖK Atlas verisi yoktur; kontenjan, taban puan ve sıralama gösterilmez. Sürümü ve kaynağı belli bir veri setin varsa JSON veya CSV dosyasıyla ekleyebilirsin.")
                Spacer(Modifier.height(Space.s))
                SoftButton("Veri paketi içe aktar", Modifier.fillMaxWidth(), icon = Icons.Rounded.UploadFile) { picker.launch(arrayOf("application/json", "text/csv", "text/comma-separated-values", "text/plain")) }
                if (message.isNotEmpty()) { Spacer(Modifier.height(Space.s)); Muted(message) }
            }
            records.take(50).forEach { u ->
                YCard(padding = Space.m) {
                    Text("${u.university} • ${u.department}", style = MaterialTheme.typography.bodyMedium, color = Yks.c.text)
                    Muted(listOfNotNull(u.dataYear?.toString(), u.scoreType.ifBlank { null }, u.quota?.let { "Kontenjan: $it" }, u.baseScore?.let { "Taban: $it" }, u.baseRank?.let { "Sıra: $it" }).joinToString(" • "))
                }
            }
        }
    }
    if (addUni) {
        var name by remember { mutableStateOf("") }
        var city by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { addUni = false }, containerColor = Yks.c.card, title = { Text("Üniversite ekle") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(Space.s)) { Field(name, { name = it }, "Üniversite adı"); Field(city, { city = it }, "Şehir") } },
            confirmButton = { TextButton({ scope.launch { r.user.saveUniversity(TargetUniversity(name = name.trim(), city = city.trim())) }; addUni = false }, enabled = name.isNotBlank()) { Text("Ekle") } },
            dismissButton = { TextButton({ addUni = false }) { Text("Vazgeç") } },
        )
    }
    addDeptFor?.let { u ->
        var name by remember { mutableStateOf("") }
        var type by remember { mutableStateOf("SAY") }
        var rank by remember { mutableStateOf("") }
        var score by remember { mutableStateOf("") }
        var note by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { addDeptFor = null }, containerColor = Yks.c.card, title = { Text("Bölüm ekle") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                    Field(name, { name = it }, "Bölüm adı")
                    PillGroup(listOf("SAY" to "SAY", "EA" to "EA", "SÖZ" to "SÖZ", "DİL" to "DİL", "TYT" to "TYT"), type) { type = it }
                    Field(rank, { rank = it.filter(Char::isDigit).take(7) }, "Hedef sıralama", keyboard = KeyboardOptions(keyboardType = KeyboardType.Number))
                    Field(score, { score = it.take(7) }, "Hedef puan", keyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    Field(note, { note = it }, "Notlar")
                }
            },
            confirmButton = {
                TextButton({
                    scope.launch { r.user.saveDepartment(TargetDepartment(universityId = u.id, name = name.trim(), scoreType = type, targetRank = rank.toIntOrNull(), targetScore = score.replace(',', '.').toDoubleOrNull(), note = note.trim())) }
                    addDeptFor = null
                }, enabled = name.isNotBlank()) { Text("Ekle") }
            },
            dismissButton = { TextButton({ addDeptFor = null }) { Text("Vazgeç") } },
        )
    }
}

@Composable
fun PacksScreen() {
    val r = repo()
    val nav = LocalNav.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val packs by r.content.packs().collectAsStateWithLifecycle(emptyList())
    val byExam by r.content.bankByExam().collectAsStateWithLifecycle(emptyList())
    var result by remember { mutableStateOf<List<String>>(emptyList()) }
    var ok by remember { mutableStateOf(true) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val text = withContext(Dispatchers.IO) { runCatching { context.contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() } }.getOrDefault("") }
            val (success, lines) = r.importPack(text)
            ok = success; result = lines
        }
    }
    Screen("İçerik paketleri", { nav.popBackStack() }) {
        ScrollColumn {
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                byExam.sortedBy { it.key }.forEach { Tile("${it.total}", "${it.key} sorusu", Modifier.weight(1f)) }
            }
            GroupCard {
                packs.forEachIndexed { i, p ->
                    if (i > 0) Line()
                    Column(Modifier.padding(Space.l)) {
                        Text(p.title, style = MaterialTheme.typography.bodyLarge, color = Yks.c.text)
                        Muted("Sürüm ${p.version} • ${p.questionCount} soru • ${shortDate(p.importedAt)}")
                        if (p.source.isNotBlank()) Muted("Kaynak: ${p.source}")
                        if (p.license.isNotBlank()) Muted("Lisans: ${p.license}")
                    }
                }
            }
            SoftButton("JSON içerik paketi içe aktar", Modifier.fillMaxWidth(), icon = Icons.Rounded.UploadFile) { picker.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }
            result.forEach { Text(it, style = MaterialTheme.typography.bodyMedium, color = if (ok) Yks.c.success else Yks.c.error) }
            Muted("Paketteki her soru doğrulanır: tek doğru cevap, açıklama, kaynak, lisans ve yinelenme denetimi. Hatalı paket reddedilir ve hiçbir içerik eklenmez.")
        }
    }
}

@Composable
fun BadgesScreen() {
    val r = repo()
    val nav = LocalNav.current
    val solved by r.user.solvedTotal().collectAsStateWithLifecycle(0)
    val minutes by r.user.minutesTotal().collectAsStateWithLifecycle(0)
    val mocks by r.user.mockSummaries().collectAsStateWithLifecycle(emptyList())
    val completed by r.user.completedTopics().collectAsStateWithLifecycle(0)
    val active by r.user.activeDays().collectAsStateWithLifecycle(emptyList())
    val tasks by r.user.doneTaskCount().collectAsStateWithLifecycle(0)
    val cards by r.user.learnedCards().collectAsStateWithLifecycle(0)
    val s by r.settings.collectAsStateWithLifecycle(AppSettings())
    val date = remember { today() }
    val week = remember { weekStart(date) }
    val month = remember { date.withDayOfMonth(1) }
    val weekMin by r.user.minutesBetween(startOf(week), startOf(date.plusDays(1))).collectAsStateWithLifecycle(0)
    val monthQ by r.user.solvedBetween(startOf(month), startOf(date.plusDays(1))).collectAsStateWithLifecycle(0)
    val badges = Badges.all(HabitStats(solved, minutes, mocks.size, completed, Streaks.longest(active), tasks, cards))
    Screen("Rozetler ve hedefler", { nav.popBackStack() }) {
        ScrollColumn {
            Row2({ Tile("${Streaks.current(active, date)} gün", "Çalışma serisi", it) }, { Tile("$tasks", "Tamamlanan görev", it) })
            YCard {
                Text("Haftalık hedef", style = MaterialTheme.typography.titleMedium, color = Yks.c.text)
                if (s.weeklyGoalMinutes > 0) { Muted("$weekMin / ${s.weeklyGoalMinutes} dakika • %${Scoring.percent(weekMin, s.weeklyGoalMinutes)}"); Spacer(Modifier.height(6.dp)); ProgressBar(weekMin.toFloat() / s.weeklyGoalMinutes) }
                else Muted("Hedefi Ayarlar'dan belirleyebilirsin.")
                Spacer(Modifier.height(Space.m))
                Text("Aylık hedef", style = MaterialTheme.typography.titleMedium, color = Yks.c.text)
                if (s.monthlyGoalQuestions > 0) { Muted("$monthQ / ${s.monthlyGoalQuestions} soru • %${Scoring.percent(monthQ, s.monthlyGoalQuestions)}"); Spacer(Modifier.height(6.dp)); ProgressBar(monthQ.toFloat() / s.monthlyGoalQuestions) }
                else Muted("Hedefi Ayarlar'dan belirleyebilirsin.")
            }
            Section("Rozetler")
            badges.forEach { b ->
                YCard(padding = Space.m) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (b.earned) Icons.Rounded.EmojiEvents else Icons.Rounded.Lock, null, tint = if (b.earned) Yks.c.warning else Yks.c.muted)
                        Spacer(Modifier.width(Space.m))
                        Column(Modifier.weight(1f)) {
                            Text(b.title + if (b.earned) " • Kazanıldı" else "", style = MaterialTheme.typography.bodyLarge, color = Yks.c.text)
                            Muted(b.detail)
                            if (!b.earned) { Spacer(Modifier.height(4.dp)); ProgressBar(b.current.toFloat() / b.target, height = 4.dp) }
                        }
                    }
                }
            }
        }
    }
}
