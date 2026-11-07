package app.ykspusula

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import app.ykspusula.data.*
import app.ykspusula.domain.ReminderTime
import app.ykspusula.notify.Notifier
import app.ykspusula.notify.REMINDER_TYPES
import app.ykspusula.notify.ReminderReceiver
import app.ykspusula.ui.DebugNav
import app.ykspusula.ui.screens.OSYM_SORU_ARSIVI
import app.ykspusula.ui.screens.OfficialLink
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.util.regex.Pattern

/**
 * Sistemle konuşan özellikler: bildirim, zamanlanmış hatırlatma, saat dilimi, dosya seçici, fotoğraf seçici,
 * yedeğin dosyaya kaydedilip geri okunması. Seçiciler Android'in kendi ekranlarıdır; UiAutomator ile sürülür.
 * Her adımın sonucu files/sistem/<test>.txt dosyasına yazılır.
 */
@RunWith(AndroidJUnit4::class)
class SystemIntegrationTest {
    @get:Rule val rule = createEmptyComposeRule()
    @get:Rule val testName = org.junit.rules.TestName()
    private val app get() = ApplicationProvider.getApplicationContext<Context>() as YksApp
    private val r get() = app.repo
    private val device: UiDevice get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private var scenario: ActivityScenario<MainActivity>? = null
    private val notes = StringBuilder()
    private val stamp = System.currentTimeMillis().toString().takeLast(6)

    // JUnit @Before yöntemi void olmalı; runBlocking son ifadenin değerini döndürmesin diye gövde bloktur.
    @Before fun setup() {
        runBlocking {
            app.ready.first { it }
            r.clearUserData()
            r.updateSettings { it[Keys.theme] = "DARK" }
            r.user.saveProfile(UserProfile(onboarded = true, name = "Test", field = "SAY"))
        }
    }

    @After fun tearDown() {
        runCatching { scenario?.close() }
        File(app.filesDir, "sistem").apply { mkdirs() }.resolve("${testName.methodName}.txt").writeText(notes.toString())
    }

    private fun note(s: String) { notes.append(s).append('\n'); Log.i("YksSistem", s) }

    private fun launch(route: String? = null) {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        rule.waitUntil("ana ekran", 20_000) { rule.onAllNodesWithText("Bugünkü Rotan").fetchSemanticsNodes().isNotEmpty() }
        if (route != null) { rule.runOnUiThread { DebugNav.nav!!.navigate(route) }; rule.waitForIdle() }
    }

    private fun clickApp(text: String, desc: Boolean = false) {
        val m = if (desc) hasContentDescription(text) else hasText(text)
        rule.waitUntil("'$text'", 15_000) { rule.onAllNodes(m).fetchSemanticsNodes().isNotEmpty() }
        val n = rule.onAllNodes(m).onFirst()
        runCatching { n.performScrollTo() }
        n.performClick()
    }

    private fun evidence(name: String) {
        val dir = File(app.filesDir, "screens").apply { mkdirs() }
        runCatching { device.takeScreenshot(File(dir, "a15-sistem-$name.png")) }
        runCatching { device.dumpWindowHierarchy(File(dir, "a15-sistem-$name.xml")) }
    }

    private fun waitForeignPackage(timeout: Long = 15_000): String {
        val end = System.currentTimeMillis() + timeout
        while (System.currentTimeMillis() < end) {
            val p = device.currentPackageName
            if (p != null && p != app.packageName) return p
            Thread.sleep(250)
        }
        return device.currentPackageName ?: ""
    }

    /** Belge seçicide dosyayı bulur; görünmüyorsa İndirilenler köküne geçer. */
    private fun findInDocumentsUi(fileName: String): UiObject2? {
        // Seçici açılırken ekran yeniden çizilebilir; bayatlayan öğe için arama iki kez daha denenir.
        repeat(2) { runCatching { return findInDocumentsUiOnce(fileName) }.onFailure { if (it !is androidx.test.uiautomator.StaleObjectException) throw it; Thread.sleep(1500) } }
        return findInDocumentsUiOnce(fileName)
    }

    private fun findInDocumentsUiOnce(fileName: String): UiObject2? {
        device.wait(Until.findObject(By.textContains(fileName)), 8_000)?.let { return it }
        val roots = device.findObject(By.desc(Pattern.compile("(?i)show roots|kökleri göster")))
        if (roots != null) {
            roots.click()
            device.wait(Until.findObject(By.text(Pattern.compile("(?i)downloads|indirilenler"))), 5_000)?.click()
        }
        return device.wait(Until.findObject(By.textContains(fileName)), 10_000)
    }

    private fun insertDownload(name: String, mime: String, bytes: ByteArray): Uri {
        val cv = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/")
        }
        val uri = app.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv)!!
        app.contentResolver.openOutputStream(uri)!!.use { it.write(bytes) }
        return uri
    }

    private fun samplePdf(): ByteArray {
        val doc = PdfDocument()
        repeat(2) { i ->
            val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, i + 1).create())
            page.canvas.drawText("YKS Pusula deneme PDF sayfa ${i + 1}", 60f, 100f, Paint().apply { textSize = 24f })
            doc.finishPage(page)
        }
        val out = java.io.ByteArrayOutputStream()
        doc.writeTo(out); doc.close()
        return out.toByteArray()
    }

    /* ---------- Bildirimler ---------- */

    @Test fun notificationShownWhenAllowed() = runBlocking<Unit> {
        assertTrue("Bildirim izni bu testte verilmiş olmalı", Notifier.canNotify(app))
        Notifier.createChannels(app)
        Notifier.show(app, 4242, "Deneme bildirimi", "Android 15 testi")
        val nm = app.getSystemService(NotificationManager::class.java)
        var ok = false
        repeat(20) { if (nm.activeNotifications.any { it.id == 4242 }) { ok = true; return@repeat }; Thread.sleep(250) }
        note("bildirim-gosterildi=$ok kanal-onemi=${nm.getNotificationChannel("reminders")?.importance}")
        assertTrue(ok)
        nm.cancel(4242)
    }

    /** Gerçek bekleme: hatırlatma 2 dakika sonrasına kurulur, Android'in tetiklemesi beklenir (en çok 8 dakika). */
    @Test fun scheduledReminderActuallyFires() = runBlocking<Unit> {
        val nm = app.getSystemService(NotificationManager::class.java)
        nm.cancelAll()
        val zone = ZoneId.systemDefault()
        val target = Instant.ofEpochMilli(System.currentTimeMillis() + 120_000).atZone(zone)
        val rem = Reminder("DAILY", enabled = true, hour = target.hour, minute = target.minute, frequency = "DAILY")
        r.user.saveReminder(rem)
        Notifier.createChannels(app)
        Notifier.schedule(app, rem)
        val at = Notifier.scheduledAt["DAILY"]!!
        val pi = PendingIntent.getBroadcast(app, 1, Intent(app, ReminderReceiver::class.java).setAction("app.ykspusula.REMINDER"), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        note("hatirlatma-kuruldu=${Instant.ofEpochMilli(at)} alarm-kaydi=${pi != null}")
        assertNotNull("Alarm kaydı bulunamadı", pi)
        val code = 100 + REMINDER_TYPES.indexOfFirst { it.type == "DAILY" }
        val deadline = at + 6 * 60_000
        var firedAt = 0L
        while (System.currentTimeMillis() < deadline) {
            if (nm.activeNotifications.any { it.id == code }) { firedAt = System.currentTimeMillis(); break }
            Thread.sleep(2_000)
        }
        note("hatirlatma-geldi=${firedAt > 0} gecikme-sn=${if (firedAt > 0) (firedAt - at) / 1000 else -1}")
        assertTrue("Hatırlatma ${Instant.ofEpochMilli(at)} için kuruldu ama 6 dakika içinde gelmedi", firedAt > 0)
        r.user.saveReminder(rem.copy(enabled = false)); Notifier.schedule(app, rem.copy(enabled = false))
        nm.cancel(code)
    }

    /** Saat dilimi değişince BootReceiver hatırlatmayı yeni yerel saate göre yeniden kurar. */
    @Test fun timeZoneChangeReschedules() = runBlocking<Unit> {
        val ua = InstrumentationRegistry.getInstrumentation().uiAutomation
        val am = app.getSystemService(android.app.AlarmManager::class.java)
        val original = java.util.TimeZone.getDefault().id
        val rem = Reminder("DAILY", enabled = true, hour = 20, minute = 0, frequency = "DAILY")
        r.user.saveReminder(rem)
        Notifier.schedule(app, rem)
        val before = Notifier.scheduledAt["DAILY"]!!
        val other = if (original == "Asia/Tokyo") "America/New_York" else "Asia/Tokyo"
        ua.adoptShellPermissionIdentity(android.Manifest.permission.SET_TIME_ZONE)
        try {
            am.setTimeZone(other)
            val expected = ReminderTime.nextFor(System.currentTimeMillis(), 20, 0, "DAILY", 7, ZoneId.of(other))
            var now = before
            repeat(40) { now = Notifier.scheduledAt["DAILY"] ?: 0; if (now == expected) return@repeat; Thread.sleep(500) }
            note("saat-dilimi $original -> $other: once=${Instant.ofEpochMilli(before)} sonra=${Instant.ofEpochMilli(now)} beklenen=${Instant.ofEpochMilli(expected)} gun=${app.day.value}")
            assertEquals("Hatırlatma yeni saat dilimine göre kurulmadı", expected, now)
            assertEquals(java.time.LocalDate.now(ZoneId.of(other)), app.day.value)
        } finally {
            am.setTimeZone(original)
            ua.dropShellPermissionIdentity()
            r.user.saveReminder(rem.copy(enabled = false)); Notifier.schedule(app, rem.copy(enabled = false))
        }
    }

    /* ---------- Dosya ve fotoğraf seçici ---------- */

    @Test fun pdfPickerImportsAndRenders() = runBlocking<Unit> {
        val fileName = "yks-ornek-$stamp.pdf"
        insertDownload(fileName, "application/pdf", samplePdf())
        launch("library")
        clickApp("PDF ekle", desc = true)
        val pkg = waitForeignPackage()
        note("pdf-secici-paketi=$pkg")
        val item = findInDocumentsUi(fileName)
        evidence("pdf-secici")
        assertNotNull("Seçicide $fileName bulunamadı (paket $pkg)", item)
        item!!.click()
        rule.waitUntil("PDF kaydı", 20_000) { runBlocking { r.user.libraryNow().any { it.name.contains("yks-ornek-$stamp") } } }
        val f = r.user.libraryNow().first { it.name.contains("yks-ornek-$stamp") }
        val copied = File(File(app.filesDir, "library"), f.fileName)
        note("pdf-kopyalandi=${copied.length()} bayt ad=${f.name}")
        assertTrue(copied.length() > 500)
        rule.runOnUiThread { DebugNav.nav!!.navigate("pdf/${f.id}") }
        rule.waitUntil("PDF sayfası", 15_000) { rule.onAllNodesWithText("1 / 2").fetchSemanticsNodes().isNotEmpty() }
        // Sayfa görüntüsü arka planda çizilir; sayfa sayısı yazısından biraz sonra gelebilir.
        rule.waitUntil("PDF sayfa görüntüsü", 15_000) { rule.onAllNodes(hasContentDescription("Sayfa 1")).fetchSemanticsNodes().isNotEmpty() }
        evidence("pdf-goruntu")
        note("pdf-acildi=1/2")
    }

    @Test fun photoPickerAttachesToWrong() = runBlocking<Unit> {
        val bmp = Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.rgb(40, 120, 220)) }
        val cv = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "yks-soru-$stamp.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/")
        }
        val uri = app.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv)!!
        app.contentResolver.openOutputStream(uri)!!.use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        launch("wrongEdit/new")
        clickApp("Fotoğraf ekle")
        val pkg = waitForeignPackage()
        note("foto-secici-paketi=$pkg")
        Thread.sleep(2_000)
        evidence("foto-secici")
        val thumb = device.wait(Until.findObject(By.res(Pattern.compile(".*:id/icon_thumbnail"))), 10_000)
            ?: device.findObject(By.desc(Pattern.compile("(?i)(photo|fotoğraf|image|görüntü).*")))
        assertNotNull("Fotoğraf seçicide görsel bulunamadı (paket $pkg)", thumb)
        thumb!!.click()
        // Bazı sürümlerde tek seçimden sonra "Ekle/Done" onayı istenir.
        device.wait(Until.findObject(By.text(Pattern.compile("(?i)add|done|ekle|bitti"))), 2_500)?.takeIf { device.currentPackageName != app.packageName }?.click()
        rule.waitUntil("fotoğraf önizleme", 20_000) { rule.onAllNodesWithText("Fotoğrafı değiştir").fetchSemanticsNodes().isNotEmpty() }
        val inputs = rule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes()
        note("form-alani=${inputs.size}")
        rule.onAllNodes(hasSetTextAction()).onFirst().performTextInput("Seçici denemesi")
        clickApp("Dikkatsizlik")
        clickApp("Kaydet")
        rule.waitUntil("yanlış kaydı", 15_000) { runBlocking { r.user.wrongsNow().any { it.photoFile != null } } }
        val w = r.user.wrongsNow().first { it.photoFile != null }
        val photo = r.photoFile(w.photoFile!!)
        note("foto-kaydedildi=${photo.length()} bayt")
        assertTrue(photo.length() > 1000)
        app.contentResolver.delete(uri, null, null)
    }

    /** Yedek Android'in kaydet ekranıyla dosyaya yazılır, sonra açma ekranıyla geri okunur. */
    @Test fun backupSaveAndRestoreThroughPicker() = runBlocking<Unit> {
        r.user.saveNote(UserNote(id = "yedek-notu", title = "Yedekten dönmeli", body = "x", createdAt = 1, updatedAt = 1))
        launch("backup")
        clickApp("Yedek oluştur")
        val pkg = waitForeignPackage()
        note("kaydet-ekrani-paketi=$pkg")
        Thread.sleep(1_500)
        evidence("yedek-kaydet")
        var save = device.wait(Until.findObject(By.text(Pattern.compile("(?i)save|kaydet")).enabled(true)), 6_000)
        if (save == null) {
            device.findObject(By.desc(Pattern.compile("(?i)show roots|kökleri göster")))?.click()
            device.wait(Until.findObject(By.text(Pattern.compile("(?i)downloads|indirilenler"))), 5_000)?.click()
            save = device.wait(Until.findObject(By.text(Pattern.compile("(?i)save|kaydet")).enabled(true)), 6_000)
        }
        assertNotNull("Kaydet düğmesi bulunamadı (paket $pkg)", save)
        save!!.click()
        rule.waitUntil("yedek yazıldı", 30_000) { rule.onAllNodesWithText("Yedek kaydedildi.").fetchSemanticsNodes().isNotEmpty() }
        note("yedek-kaydedildi=evet")
        r.user.deleteNote("yedek-notu")
        clickApp("Yedekten geri yükle")
        waitForeignPackage()
        val name = "yks-pusula-yedek-${today()}"
        val item = findInDocumentsUi(name)
        evidence("yedek-ac")
        assertNotNull("Açma ekranında $name bulunamadı", item)
        item!!.click()
        rule.waitUntil("yedek doğrulandı", 30_000) { rule.onAllNodesWithText("Yedek doğrulandı").fetchSemanticsNodes().isNotEmpty() }
        clickApp("Mevcut verilerle birleştir")
        rule.waitUntil("not döndü", 30_000) { runBlocking { r.user.notesNow().any { it.id == "yedek-notu" } } }
        note("yedek-geri-yuklendi=evet")
    }

    /* ---------- YKS bilgi merkezi: ÖSYM soru ve cevap arşivi bağlantısı ---------- */

    /** İnternet yokken ÖSYM kartı anlaşılır bir uyarı gösterir, tarayıcı açmaz ve uygulama çökmez (uçak modunda). */
    @Test fun osymLinkOfflineShowsWarning() = runBlocking<Unit> {
        launch("info")
        clickApp("ÖSYM")
        rule.waitUntil("internet uyarısı", 10_000) { rule.onAllNodesWithText(OfficialLink.NO_INTERNET).fetchSemanticsNodes().isNotEmpty() }
        assertTrue("Uygulama önde kalmalı", device.currentPackageName == app.packageName)
        note("cevrimdisi-uyari=gosterildi")
    }

    /** Çevrimiçiyken ÖSYM kartı telefonun tarayıcısını resmî arşiv adresiyle açar (uygulama içinde WebView yok). */
    @Test fun osymLinkOpensBrowser() = runBlocking<Unit> {
        val intent = OfficialLink.browserIntent(OSYM_SORU_ARSIVI)
        assertEquals(OSYM_SORU_ARSIVI, intent.dataString)
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertTrue(intent.selector!!.hasCategory(Intent.CATEGORY_BROWSABLE))
        val browser = app.packageManager.resolveActivity(intent.selector!!, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
        note("varsayilan-tarayici=$browser")
        launch("info")
        clickApp("ÖSYM")
        // Açılan etkinlik ve görevin isteği sistemden okunur: tarayıcı önde mi, istek ÖSYM adresini taşıyor mu?
        var top = ""; var task = ""
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < 30_000) {
            val dump = shell("dumpsys activity activities")
            top = dump.lineSequence().firstOrNull { it.contains("topResumedActivity") || it.contains("mResumedActivity") }?.trim() ?: ""
            // Chrome isteği dağıtıp ilk açılış ekranına geçebilir; görevin kök isteği son görevler kaydında kalır.
            val all = dump + "\n" + shell("dumpsys activity recents")
            if (task.isEmpty()) task = all.lineSequence().firstOrNull { it.contains("osym.gov.tr") }?.trim() ?: ""
            if (task.isNotEmpty() && top.isNotEmpty() && !top.contains(" ${app.packageName}/")) break
            Thread.sleep(1000)
        }
        note("on-plan=$top")
        note("istek=$task")
        assertFalse("Uygulama yerine tarayıcı önde olmalı: $top", top.isEmpty() || top.contains(" ${app.packageName}/"))
        assertTrue("Tarayıcıya giden istek ÖSYM adresini taşımalı: $task", task.contains("https://www.osym.gov.tr"))
        device.pressBack()
    }

    private fun shell(cmd: String): String =
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(cmd).use { pfd ->
            android.os.ParcelFileDescriptor.AutoCloseInputStream(pfd).bufferedReader().readText()
        }

    /* ---------- Play Billing (çevrimiçi Play imajında çalıştırılır) ---------- */

    /**
     * Play Billing kitaplığının başlatılması ve güvenli davranış. Emülatörde Google hesabı yoktur; Play Store bu
     * durumda satın almanın desteklenmediğini bildirir ve kurulum yanıtı hiç gelmeyebilir. Test bu yüzden gerçek
     * satın alma bağlantısını değil şunları doğrular: kitaplık Play Store'a bağlanmayı dener, yanıt gelirse doğru
     * işlenir, yanıt gelmezse satın alma düğmesi kapalı kalır, doğrulanmış satın alma olmadan hak açılmaz, uygulama çökmez.
     */
    @Test fun billingClientStarts() = runBlocking<Unit> {
        shell("logcat -c")
        val b = app.monetize.billing
        b.connect()
        val start = System.currentTimeMillis()
        while (b.lastSetupCode == null && System.currentTimeMillis() - start < 60_000) Thread.sleep(500)
        val code = b.lastSetupCode
        Thread.sleep(5_000)
        val ui = b.ui.value
        val log = shell("logcat -d -s BillingClient:V").lines().filter { it.contains("BillingClient") }
        note("billing-yanit-kodu=$code sure-ms=${System.currentTimeMillis() - start} bagli=${ui.connected} fiyat=${ui.price} mesaj='${ui.message}' reklamsiz=${app.monetize.entitlement.hasAdsFreeEntitlement.value}")
        log.takeLast(5).forEach { note("billing-log: ${it.substringAfter("BillingClient").take(160)}") }
        assertFalse("Doğrulanmış satın alma olmadan reklamsız hak açılmamalı", app.monetize.entitlement.hasAdsFreeEntitlement.value)
        when {
            code == 0 -> assertTrue(ui.connected)
            code != null -> assertEquals("Google Play satın alma servisine ulaşılamadı.", ui.message)
            else -> {
                assertTrue("Billing kitaplığı Play Store'a bağlanmayı denemedi", log.isNotEmpty())
                assertNull("Yanıt yokken fiyat görünmemeli (satın alma düğmesi kapalı)", ui.price)
                assertFalse(ui.connected)
            }
        }
    }
}
