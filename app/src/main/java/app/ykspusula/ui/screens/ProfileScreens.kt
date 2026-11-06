package app.ykspusula.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import app.ykspusula.ui.rememberUiScope
import app.ykspusula.ui.shortDate
import app.ykspusula.ui.theme.Yks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val PRIVACY_TEXT = "Çalışma verilerin bu cihazda saklanır. Uygulamada hesap veya sunucu yoktur. Reklam ve satın alma için Google hizmetleri kullanılır."

@Composable
fun ProfileScreen() {
    val a = app()
    val r = a.repo
    val nav = LocalNav.current
    val context = LocalContext.current
    val c = Yks.c
    val profile by r.user.profile().collectAsStateWithLifecycle(null)
    val adsFree by a.monetize.entitlement.hasAdsFreeEntitlement.collectAsStateWithLifecycle()
    val privacy by a.monetize.ads.privacyOptionsRequired.collectAsStateWithLifecycle()
    val p = profile ?: return
    ScrollColumn(Modifier.statusBarsPadding()) {
        PageHeader("Profil", "Hedeflerin, ayarların ve verilerin.")
        YCard(onClick = { nav.navigate("profileEdit") }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(56.dp).clip(CircleShape).background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(c.accent, c.purple))), contentAlignment = Alignment.Center) {
                    Text(p.name.take(1).uppercase().ifEmpty { "Ö" }, style = MaterialTheme.typography.titleLarge, color = androidx.compose.ui.graphics.Color.White)
                }
                Spacer(Modifier.width(Space.m))
                Column(Modifier.weight(1f)) {
                    Text(p.name.ifBlank { "Öğrenci" }, style = MaterialTheme.typography.titleMedium, color = c.text)
                    Muted("${GRADES.firstOrNull { it.first == p.grade }?.second} • ${p.examYear} • ${FIELD_LABELS.firstOrNull { it.first == p.field }?.second}")
                    if (p.targetDepartment.isNotBlank()) Muted("Hedef: ${p.targetDepartment}", maxLines = 1)
                    Muted("Günlük hedef: ${p.dailyMinutes} dk")
                }
                Icon(Icons.Rounded.Edit, "Profili düzenle", tint = c.muted)
            }
        }
        if (!adsFree) PrimaryGradientCard("Reklamları kaldır", "Tek seferlik ödeme, ömür boyu reklamsız.", Icons.Rounded.WorkspacePremium, brush = app.ykspusula.ui.theme.Gradients.purple) { nav.navigate("removeAds") }
        Section("Gelişim")
        GroupCard {
            NavRow("Üniversite ve bölüm hedefleri", "Hedeflerini karşılaştır", Icons.Rounded.School, onClick = { nav.navigate("targets") }); Line()
            NavRow("Net hedeflerim", "Testlere göre hedef", Icons.Rounded.Flag, c.success, { nav.navigate("netGoals") }); Line()
            NavRow("Raporlar", "Haftalık ve aylık gelişim", Icons.Rounded.Summarize, onClick = { nav.navigate("reports") }); Line()
            NavRow("Rozetler ve seriler", "Çalışma alışkanlığın", Icons.Rounded.EmojiEvents, c.warning, { nav.navigate("badges") }); Line()
            NavRow("Motivasyon sözleri", "Günün sözü ve favoriler", Icons.Rounded.FormatQuote, c.purpleLight, { nav.navigate("motivation") })
        }
        Section("Ayarlar")
        GroupCard {
            NavRow("Uygulama ayarları", "Tema, yazı boyutu, hedefler", Icons.Rounded.Settings, onClick = { nav.navigate("settings") }); Line()
            NavRow("Plan ayarları", "Günlük süre, saatler, öncelikler", Icons.Rounded.Tune, onClick = { nav.navigate("planSettings") }); Line()
            NavRow("Bildirimler", "Hatırlatıcılar ve sıklık", Icons.Rounded.Notifications, onClick = { nav.navigate("reminders") }); Line()
            NavRow("Ana ekran bileşenleri", "Geri sayım, hedef, görevler, odak", Icons.Rounded.Widgets, onClick = { nav.navigate("widgets") }); Line()
            NavRow("Sınav ayarları", "Tarih, süre, net kuralı", Icons.Rounded.EventNote, onClick = { nav.navigate("examSettings") })
        }
        Section("Veri ve gizlilik")
        GroupCard {
            NavRow("Veri yedekleme", "Yedek oluştur ve geri yükle", Icons.Rounded.Backup, onClick = { nav.navigate("backup") }); Line()
            NavRow("İçerik paketleri", "Sürüm ve kapsam", Icons.Rounded.Inventory2, onClick = { nav.navigate("packs") }); Line()
            if (privacy) {
                NavRow("Gizlilik seçenekleri", "Reklam rızanı yönet", Icons.Rounded.PrivacyTip, onClick = {
                    (context as? android.app.Activity)?.let { act -> a.monetize.ads.showPrivacyOptions(act) { } }
                }); Line()
            }
            NavRow("Gizlilik politikası", "Verilerin nasıl işlendiği", Icons.Rounded.Policy, onClick = { nav.navigate("privacy") }); Line()
            NavRow(if (adsFree) "Satın alma durumu" else "Satın almaları geri yükle", if (adsFree) "Reklamlar kaldırıldı" else "Aynı Google hesabıyla", Icons.Rounded.Restore, onClick = { nav.navigate("removeAds") }); Line()
            NavRow("Uygulama hakkında", "Sürüm ve lisanslar", Icons.Rounded.Info, onClick = { nav.navigate("about") })
        }
        Muted("YKS Pusula ${BuildConfig.VERSION_NAME}")
    }
}

@Composable
fun ProfileEditScreen() {
    val r = repo()
    val nav = LocalNav.current
    val scope = rememberUiScope()
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
            TimeField(u.startHour, 0, "Çalışma başlangıç saati") { h, _ -> p = u.copy(startHour = h) }
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
    val scope = rememberUiScope()
    val s by r.settings.collectAsStateWithLifecycle(AppSettings())
    var askReset by remember { mutableStateOf(false) }
    var askReset2 by remember { mutableStateOf(false) }
    Screen("Ayarlar", { nav.popBackStack() }) {
        ScrollColumn {
            Section("Görünüm")
            PillGroup(listOf("DARK" to "Premium Koyu", "LIGHT" to "Açık", "SYSTEM" to "Sistem"), s.theme) { v -> scope.launch { r.updateSettings { it[Keys.theme] = v } } }
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
    val scope = rememberUiScope()
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
                        TimeField(cur.hour, cur.minute, "Saat") { h, m -> apply(cur.copy(hour = h, minute = m)) }
                        val freq = cur.frequency.ifBlank { if (t.weekly) "WEEKLY" else "DAILY" }
                        Spacer(Modifier.height(Space.s))
                        PillGroup(listOf("DAILY" to "Her gün", "WEEKDAYS" to "Hafta içi", "WEEKLY" to "Haftada bir"), freq) { apply(cur.copy(frequency = it)) }
                        if (freq == "WEEKLY") { Spacer(Modifier.height(Space.s)); PillGroup(DAY_NAMES, cur.dayOfWeek.toString()) { apply(cur.copy(dayOfWeek = it.toInt())) } }
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
    val scope = rememberUiScope()
    var status by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<LoadedBackup?>(null) }
    var replaceAsk by remember { mutableStateOf(false) }
    var includePdf by remember { mutableStateOf(false) }
    val last by r.lastBackupAt.collectAsStateWithLifecycle(0L)
    var safety by remember { mutableStateOf(a.backup.safetyCopies()) }

    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) scope.launch {
            try {
                withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri, "wt")!!.use { a.backup.exportZip(it, BuildConfig.VERSION_NAME, includePdf) } }
                isError = false; status = "Yedek kaydedildi."
            } catch (e: Exception) { isError = true; status = "Yedek yazılamadı." }
        }
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            try {
                val loaded = a.backup.load(context.contentResolver.openInputStream(uri)!!)
                when (val c = loaded.check) {
                    is BackupCheck.Bad -> { isError = true; status = c.message }
                    is BackupCheck.Ok -> { pending = loaded; status = "" }
                }
            } catch (e: Exception) { isError = true; status = "Dosya okunamadı." }
        }
    }
    fun restore(replace: Boolean) {
        val l = pending ?: return
        val f = (l.check as? BackupCheck.Ok)?.file ?: return
        scope.launch {
            try {
                val rep = a.backup.restore(f.data, replace, BuildConfig.VERSION_NAME, l.attachments, l.warnings)
                Notifier.rescheduleAll(context)
                isError = false
                status = (if (replace) "Yedek geri yüklendi. Önceki verilerin fotoğraf ve PDF'lerle birlikte cihazda güvenlik kopyası olarak saklandı." else "Yedek mevcut verilerle birleştirildi.") +
                    " ${rep.records} kayıt ve ${rep.files} dosya aktarıldı." + rep.warnings.joinToString("") { " $it" }
                pending = null; safety = a.backup.safetyCopies()
            } catch (e: app.ykspusula.data.BackupRejected) { isError = true; status = e.message ?: "Geri yükleme yapılamadı."
            } catch (e: Exception) { isError = true; status = "Geri yükleme tamamlanamadı. Değiştirme seçtiysen önceki verilerin cihazdaki güvenlik kopyasında duruyor." }
        }
    }
    Screen("Veri yedekleme", { nav.popBackStack() }) {
        ScrollColumn {
            Muted("Yedek dosyası profilini, kaynaklarını, çalışma ve deneme geçmişini, konu ilerlemeni, yanlışlarını, fotoğraflarını, notlarını, kartlarını ve ayarlarını içerir. Dosya yalnızca seçtiğin yere kaydedilir; şifresizdir, güvenli bir yerde sakla.")
            if (last > 0) Muted("Son yedek: ${shortDate(last)}")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("PDF dosyalarını da dahil et", color = Yks.c.text, modifier = Modifier.weight(1f))
                Switch(includePdf, { includePdf = it })
            }
            MainButton("Yedek oluştur", Modifier.fillMaxWidth(), icon = Icons.Rounded.Upload) { create.launch("yks-pusula-yedek-${today()}.zip") }
            SoftButton("Yedekten geri yükle", Modifier.fillMaxWidth(), icon = Icons.Rounded.Download) { open.launch(arrayOf("application/zip", "application/json", "text/plain", "application/octet-stream")) }
            if (status.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (isError) Icons.Rounded.ErrorOutline else Icons.Rounded.CheckCircle, null, tint = if (isError) Yks.c.error else Yks.c.success, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(Space.s))
                    Text(status, style = MaterialTheme.typography.bodyMedium, color = Yks.c.text)
                }
            }
            (pending?.check as? BackupCheck.Ok)?.file?.let { f ->
                YCard {
                    Text("Yedek doğrulandı", style = MaterialTheme.typography.titleMedium, color = Yks.c.text)
                    Muted("Tarih: ${shortDate(f.createdAt)} • Sürüm ${f.appVersion}")
                    Muted("${f.data.logs.size} çalışma kaydı, ${f.data.mocks.size} deneme, ${f.data.notes.size} not, ${f.data.wrongs.size} yanlış, ${f.data.sources.size} kaynak")
                    pending?.let { l -> if (l.fileCount > 0) Muted("${l.fileCount} fotoğraf veya PDF dosyası" + if (f.files.isNotEmpty()) ", bütünlüğü doğrulandı" else "") }
                    pending?.warnings?.forEach { w -> Muted(w) }
                    Spacer(Modifier.height(Space.s))
                    MainButton("Mevcut verilerle birleştir", Modifier.fillMaxWidth()) { restore(false) }
                    Spacer(Modifier.height(Space.s))
                    SoftButton("Mevcut verilerin yerine geç", Modifier.fillMaxWidth(), color = Yks.c.error) { replaceAsk = true }
                }
            }
            if (safety.isNotEmpty()) {
                Section("Eski yedeğe dön")
                Muted("Geri yüklemeden önce otomatik alınan kopyalar. Cihazda saklanır.")
                GroupCard {
                    safety.forEachIndexed { i, f ->
                        if (i > 0) Line()
                        NavRow(shortDate(f.lastModified()), "${f.length() / 1024} KB", Icons.Rounded.History, onClick = {
                            scope.launch {
                                val l = a.backup.load(f.inputStream())
                                if (l.check is BackupCheck.Ok) { pending = l; status = "" } else { isError = true; status = (l.check as BackupCheck.Bad).message }
                            }
                        })
                    }
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
    val scope = rememberUiScope()
    val unis by r.user.universities().collectAsStateWithLifecycle(emptyList())
    val depts by r.user.departments().collectAsStateWithLifecycle(emptyList())
    val records by r.user.universityRecords().collectAsStateWithLifecycle(emptyList())
    val mocks by r.user.mockSummaries().collectAsStateWithLifecycle(emptyList())
    val lastTyt = mocks.lastOrNull { it.exam == "TYT" }?.net
    val lastAyt = mocks.lastOrNull { it.exam == "AYT" }?.net
    var addUni by remember { mutableStateOf(false) }
    var addDeptFor by remember { mutableStateOf<TargetUniversity?>(null) }
    var editDept by remember { mutableStateOf<TargetDepartment?>(null) }
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
                                d.targetTytNet?.let { t -> Muted("TYT hedefi ${Scoring.formatNet(t)}" + (lastTyt?.let { " • Son net ${Scoring.formatNet(it)} • Fark ${Scoring.formatNet(t - it)}" } ?: "")) }
                                d.targetAytNet?.let { t -> Muted("AYT hedefi ${Scoring.formatNet(t)}" + (lastAyt?.let { " • Son net ${Scoring.formatNet(it)} • Fark ${Scoring.formatNet(t - it)}" } ?: "")) }
                                if (d.dataSource.isNotBlank()) Muted("Kaynak: ${d.dataSource}" + (d.dataYear?.let { " ($it)" } ?: ""))
                                if (d.note.isNotBlank()) Muted(d.note)
                            }
                            IconButton({ editDept = d }) { Icon(Icons.Rounded.Edit, "Bölümü düzenle", tint = Yks.c.muted) }
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
    if (addDeptFor != null || editDept != null) {
        val base = editDept
        val u = addDeptFor
        fun close() { addDeptFor = null; editDept = null }
        var name by remember { mutableStateOf(base?.name.orEmpty()) }
        var type by remember { mutableStateOf(base?.scoreType ?: "SAY") }
        var rank by remember { mutableStateOf(base?.targetRank?.toString().orEmpty()) }
        var score by remember { mutableStateOf(base?.targetScore?.toString().orEmpty()) }
        var tyt by remember { mutableStateOf(base?.targetTytNet?.toString().orEmpty()) }
        var ayt by remember { mutableStateOf(base?.targetAytNet?.toString().orEmpty()) }
        var source by remember { mutableStateOf(base?.dataSource.orEmpty()) }
        var year by remember { mutableStateOf(base?.dataYear?.toString().orEmpty()) }
        var note by remember { mutableStateOf(base?.note.orEmpty()) }
        AlertDialog(
            onDismissRequest = { close() }, containerColor = Yks.c.card, title = { Text(if (base == null) "Bölüm ekle" else "Bölümü düzenle") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Space.s)) {
                    Field(name, { name = it }, "Bölüm adı")
                    PillGroup(listOf("SAY" to "SAY", "EA" to "EA", "SÖZ" to "SÖZ", "DİL" to "DİL", "TYT" to "TYT"), type) { type = it }
                    Field(rank, { rank = it.filter(Char::isDigit).take(7) }, "Hedef sıralama", keyboard = KeyboardOptions(keyboardType = KeyboardType.Number))
                    Field(score, { score = it.take(7) }, "Hedef puan", keyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                        Field(tyt, { tyt = it.take(6) }, "TYT net", Modifier.weight(1f), keyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                        Field(ayt, { ayt = it.take(6) }, "AYT net", Modifier.weight(1f), keyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    }
                    Field(source, { source = it }, "Veri kaynağı (ör. YÖK Atlas)")
                    NumberField(year, { year = it }, "Veri yılı", max = 4)
                    Field(note, { note = it }, "Notlar")
                }
            },
            confirmButton = {
                TextButton({
                    fun d(x: String) = x.replace(',', '.').toDoubleOrNull()
                    val now = System.currentTimeMillis()
                    scope.launch {
                        val b = base ?: TargetDepartment(universityId = u!!.id, name = "")
                        r.user.saveDepartment(b.copy(name = name.trim(), scoreType = type, targetRank = rank.toIntOrNull(), targetScore = d(score), targetTytNet = d(tyt), targetAytNet = d(ayt), dataSource = source.trim(), dataYear = year.toIntOrNull(), note = note.trim(), updatedAt = now))
                    }
                    close()
                }, enabled = name.isNotBlank()) { Text("Kaydet") }
            },
            dismissButton = { TextButton({ close() }) { Text("Vazgeç") } },
        )
    }
}

@Composable
fun PacksScreen() {
    val r = repo()
    val nav = LocalNav.current
    val context = LocalContext.current
    val scope = rememberUiScope()
    val packs by r.content.packs().collectAsStateWithLifecycle(emptyList())
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
            Muted("İçerik paketleri konu listesi, formüller, bilgi kartları ve sınav yılı bilgilerini güncellemek içindir. Uygulama soru içermez.")
            GroupCard {
                packs.forEachIndexed { i, p ->
                    if (i > 0) Line()
                    Column(Modifier.padding(Space.l)) {
                        Text(p.title, style = MaterialTheme.typography.bodyLarge, color = Yks.c.text)
                        Muted("Sürüm ${p.version} • ${shortDate(p.importedAt)}")
                        if (p.source.isNotBlank()) Muted("Kaynak: ${p.source}")
                        if (p.license.isNotBlank()) Muted("Lisans: ${p.license}")
                    }
                }
            }
            SoftButton("JSON içerik paketi içe aktar", Modifier.fillMaxWidth(), icon = Icons.Rounded.UploadFile) { picker.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }
            result.forEach { Text(it, style = MaterialTheme.typography.bodyMedium, color = if (ok) Yks.c.success else Yks.c.error) }
            Muted("Paket içeriği doğrulanır: kaynak, sürüm, ders bağlantıları ve tarih denetimi yapılır. Hatalı paket reddedilir ve hiçbir içerik eklenmez.")
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
    val rest by r.user.restDays().collectAsStateWithLifecycle(emptyList())
    val badges = Badges.all(HabitStats(solved, minutes, mocks.size, completed, Streaks.longest(active), tasks, cards))
    Screen("Rozetler ve hedefler", { nav.popBackStack() }) {
        ScrollColumn {
            Row2({ Tile("${Streaks.current(active, date, rest)} gün", "Çalışma serisi", it) }, { Tile("$tasks", "Tamamlanan görev", it) })
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
