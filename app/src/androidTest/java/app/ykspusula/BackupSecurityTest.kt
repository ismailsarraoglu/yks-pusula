package app.ykspusula

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.ykspusula.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Yedek dosyasına karşı saldırı ve bozulma senaryoları. Hiçbirinde mevcut veri değişmemeli. */
@RunWith(AndroidJUnit4::class)
class BackupSecurityTest {
    private val app get() = ApplicationProvider.getApplicationContext<Context>() as YksApp
    private val r get() = app.repo
    /** Emülatör diskini doldurmamak için sınırlar küçültülür; denetim kodu uygulamadakiyle aynıdır. */
    private val small = BackupLimits(maxInputBytes = 8L * 1024 * 1024, maxJsonBytes = 1024 * 1024, maxFileBytes = 1024 * 1024, maxTotalBytes = 3L * 1024 * 1024, maxEntries = 50)
    private val strict get() = BackupManager(r, app.filesDir, app.cacheDir, small)

    @Before fun setup() = runBlocking {
        app.ready.first { it }
        r.clearUserData()
        r.user.saveProfile(UserProfile(onboarded = true, name = "Asıl", dailyMinutes = 240))
        r.user.saveNote(UserNote(title = "Kalmalı", body = "x", createdAt = 1, updatedAt = 1))
    }

    private fun bad(bytes: ByteArray): String {
        val l = runBlocking { strict.load(ByteArrayInputStream(bytes)) }
        assertNull("Reddedilen yedeğin geçici dosyası kalmamalı", l.attachments)
        return (l.check as BackupCheck.Bad).message
    }

    private fun zip(block: (ZipOutputStream) -> Unit): ByteArray = ByteArrayOutputStream().also { o -> ZipOutputStream(o).use(block) }.toByteArray()

    private suspend fun validZip(): ByteArray {
        r.photoFile("foto.jpg").writeBytes(ByteArray(64) { it.toByte() })
        r.saveWrong(r.newWrong("tyt-mat", null, null, "Kitap", 3, "4", "Dikkatsizlik", "", "foto.jpg", null))
        return ByteArrayOutputStream().also { app.backup.exportZip(it, "2.1.0", false) }.toByteArray()
    }

    /** ZIP içeriğini değiştirip yeniden paketler. */
    private fun rewrite(src: ByteArray, edit: (String, ByteArray) -> Pair<String, ByteArray>?): ByteArray = zip { z ->
        ZipInputStream(ByteArrayInputStream(src)).use { i ->
            var e = i.nextEntry
            while (e != null) {
                edit(e.name, i.readBytes())?.let { (n, b) -> z.putNextEntry(ZipEntry(n)); z.write(b); z.closeEntry() }
                e = i.nextEntry
            }
        }
    }

    private suspend fun unchanged() {
        assertEquals("Asıl", r.user.profileNow()?.name)
        assertEquals(listOf("Kalmalı"), r.user.notesNow().map { it.title })
    }

    @Test fun pathTraversalRejected() = runBlocking {
        val m = bad(zip { z ->
            z.putNextEntry(ZipEntry("yedek.json")); z.write("{}".toByteArray()); z.closeEntry()
            z.putNextEntry(ZipEntry("files/photos/../../databases/yks.db")); z.write(byteArrayOf(1)); z.closeEntry()
        })
        assertTrue(m, m.contains("yol"))
        assertFalse(File(app.filesDir.parentFile, "databases/yks.db").let { it.exists() && it.length() == 1L })
        unchanged()
    }

    @Test fun tooManyEntriesRejected() = runBlocking {
        val m = bad(zip { z -> repeat(small.maxEntries + 1) { z.putNextEntry(ZipEntry("files/photos/p$it.jpg")); z.closeEntry() } })
        assertTrue(m, m.contains("çok fazla"))
        unchanged()
    }

    /** Sıkıştırılmış hâli küçük, açılmış hâli sınırı aşan dosya (ZIP bombası). */
    @Test fun zipBombRejected() = runBlocking {
        val chunk = ByteArray(1024 * 1024)
        val m = bad(zip { z ->
            z.putNextEntry(ZipEntry("files/photos/bomba.jpg"))
            repeat((small.maxFileBytes / chunk.size).toInt() + 2) { z.write(chunk) }
            z.closeEntry()
        })
        assertTrue(m, m.contains("büyük"))
        unchanged()
    }

    @Test fun oversizedJsonRejected() = runBlocking {
        val chunk = ByteArray(1024 * 1024) { ' '.code.toByte() }
        val m = bad(zip { z ->
            z.putNextEntry(ZipEntry("yedek.json"))
            repeat((small.maxJsonBytes / chunk.size).toInt() + 2) { z.write(chunk) }
            z.closeEntry()
        })
        assertTrue(m, m.contains("büyük"))
    }

    /** Tek tek sınırın altında kalan ama toplamı sınırı aşan dosyalar. */
    @Test fun totalUnpackedSizeLimited() = runBlocking {
        val chunk = ByteArray(900 * 1024)
        val m = bad(zip { z -> repeat(5) { z.putNextEntry(ZipEntry("files/library/d$it.pdf")); z.write(chunk); z.closeEntry() } })
        assertTrue(m, m.contains("açılmış boyutu"))
    }

    @Test fun oversizedInputRejected() = runBlocking {
        val m = bad(ByteArray(9 * 1024 * 1024) { (it % 251).toByte() })
        assertTrue(m, m.contains("çok büyük"))
    }

    /** Java'nın ZIP yazıcısı aynı adı iki kez yazmaz; ikinci ad aynı uzunlukta yazılıp baytlarda değiştirilir. */
    @Test fun duplicateEntryRejected() = runBlocking {
        val raw = zip { z ->
            z.putNextEntry(ZipEntry("yedek.json")); z.write("{}".toByteArray()); z.closeEntry()
            z.putNextEntry(ZipEntry("yedek.jsoX")); z.write("{}".toByteArray()); z.closeEntry()
        }
        val from = "yedek.jsoX".toByteArray(); val to = "yedek.json".toByteArray()
        var i = 0
        while (i <= raw.size - from.size) {
            if ((from.indices).all { raw[i + it] == from[it] }) { to.copyInto(raw, i); i += from.size } else i++
        }
        val m = bad(raw)
        assertTrue(m, m.contains("aynı adlı"))
    }

    @Test fun corruptedAttachmentRejected() = runBlocking {
        val src = validZip()
        val m = bad(rewrite(src) { n, b -> n to if (n.endsWith("foto.jpg")) b.reversedArray() else b })
        assertTrue(m, m.contains("bozulmuş"))
    }

    @Test fun missingAttachmentRejected() = runBlocking {
        val src = validZip()
        val m = bad(rewrite(src) { n, b -> if (n.endsWith("foto.jpg")) null else n to b })
        assertTrue(m, m.contains("eksik"))
    }

    @Test fun corruptedJsonRejected() = runBlocking {
        val src = validZip()
        val m = bad(rewrite(src) { n, b -> n to if (n == "yedek.json") String(b).replace("Dikkatsizlik", "Dikkatsizlig").toByteArray() else b })
        assertTrue(m, m.contains("bütünlük"))
    }

    @Test fun oldFormatWithoutManifestWarns() = runBlocking {
        val src = validZip()
        val stripped = rewrite(src) { n, b ->
            if (n != "yedek.json") n to b
            else {
                val root = kotlinx.serialization.json.Json.parseToJsonElement(String(b)) as kotlinx.serialization.json.JsonObject
                n to kotlinx.serialization.json.JsonObject(root - "files").toString().toByteArray()
            }
        }
        val l = app.backup.load(ByteArrayInputStream(stripped))
        assertTrue(l.check is BackupCheck.Ok)
        assertTrue(l.warnings.any { it.contains("doğrulanamadı") })
        l.attachments?.deleteRecursively()
        Unit
    }

    @Test fun missingPhotoReportedNotFatal() = runBlocking {
        r.saveWrong(r.newWrong("tyt-mat", null, null, "Kitap", 3, "4", "Dikkatsizlik", "", "yok.jpg", null))
        val bytes = ByteArrayOutputStream().also { app.backup.exportZip(it, "2.1.0", false) }.toByteArray()
        val l = app.backup.load(ByteArrayInputStream(bytes))
        assertTrue(l.check is BackupCheck.Ok)
        assertTrue(l.warnings.joinToString(), l.warnings.any { it.contains("fotoğrafı yedekte yok") })
        l.attachments?.deleteRecursively()
        Unit
    }

    /** Değiştirerek geri yükleme: güvenlik kopyası fotoğrafı da içermeli ve ondan geri dönülebilmeli. */
    @Test fun safetyCopyKeepsPhotosAndCanRestore() = runBlocking {
        val src = validZip()
        val loaded = app.backup.load(ByteArrayInputStream(src))
        val f = (loaded.check as BackupCheck.Ok).file
        r.user.saveNote(UserNote(title = "Sonradan", body = "y", createdAt = 2, updatedAt = 2))
        val rep = app.backup.restore(f.data, true, "2.1.0", loaded.attachments, loaded.warnings)
        assertEquals(1, rep.files)
        val safety = rep.safetyCopy!!
        assertTrue(safety.name.endsWith(".zip"))
        val names = ZipInputStream(safety.inputStream()).use { z -> generateSequence { z.nextEntry?.name }.toList() }
        assertTrue(names.toString(), "files/photos/foto.jpg" in names)
        assertFalse(r.user.notesNow().any { it.title == "Sonradan" })
        val back = app.backup.load(safety.inputStream())
        app.backup.restore((back.check as BackupCheck.Ok).file.data, true, "2.1.0", back.attachments)
        assertTrue(r.user.notesNow().any { it.title == "Sonradan" })
        assertTrue(r.photoFile("foto.jpg").exists())
    }

    @Test fun noTempFilesLeftAfterRestore() = runBlocking {
        val src = validZip()
        val loaded = app.backup.load(ByteArrayInputStream(src))
        app.backup.restore((loaded.check as BackupCheck.Ok).file.data, false, "2.1.0", loaded.attachments)
        assertTrue(app.filesDir.listFiles()!!.none { it.name.startsWith("geri-yukleme-") })
        assertTrue(app.cacheDir.listFiles()!!.none { it.name.startsWith("gelen-") })
    }

    /* ---------- Geri alınabilir geri yükleme ---------- */

    @After fun clearFaults() { app.backup.faults = RestoreFaults { } }

    private val newBytes = "YENI-FOTO".toByteArray()
    private val oldBytes = "ESKI-FOTO".toByteArray()

    /** Yedekte: "Yedekteki" notu, foto.jpg = YENI, tema koyu. Cihazda: "Yerel" notu, foto.jpg = ESKI, tema açık. */
    private suspend fun prepared(): LoadedBackup {
        r.updateSettings { it[Keys.theme] = "DARK" }
        r.photoFile("foto.jpg").writeBytes(newBytes)
        r.saveWrong(r.newWrong("tyt-mat", null, null, "Kitap", 3, "4", "Dikkatsizlik", "", "foto.jpg", null))
        r.user.saveNote(UserNote(id = "yedekteki", title = "Yedekteki", body = "y", createdAt = 3, updatedAt = 3))
        val bytes = ByteArrayOutputStream().also { app.backup.exportZip(it, "2.1.1", false) }.toByteArray()
        r.user.deleteNote("yedekteki")
        r.user.saveNote(UserNote(id = "yerel", title = "Yerel", body = "z", createdAt = 4, updatedAt = 4))
        r.photoFile("foto.jpg").writeBytes(oldBytes)
        r.updateSettings { it[Keys.theme] = "LIGHT" }
        return app.backup.load(ByteArrayInputStream(bytes)).also { assertTrue(it.check is BackupCheck.Ok) }
    }

    private fun data(l: LoadedBackup) = (l.check as BackupCheck.Ok).file.data

    private suspend fun assertUntouched() {
        val titles = r.user.notesNow().map { it.title }.toSet()
        assertTrue("Yerel not kaybolmamalı: $titles", "Yerel" in titles)
        assertFalse("Yedekteki not geri dönmemeli: $titles", "Yedekteki" in titles)
        assertArrayEquals("Eski fotoğraf korunmalı", oldBytes, r.photoFile("foto.jpg").readBytes())
        assertEquals("LIGHT", r.settings.first().theme)
        assertNoLeftovers()
    }

    private fun assertNoLeftovers() {
        assertFalse("Günlük kalmamalı", File(app.filesDir, "geri-yukleme-gunlugu.txt").exists())
        assertTrue(File(app.filesDir, "photos").listFiles()!!.none { it.name.startsWith(".eski-") })
        assertTrue(app.filesDir.listFiles()!!.none { it.name.startsWith("geri-yukleme-") })
    }

    @Test fun mergeReplacesSameNameFileSafely() = runBlocking {
        val l = prepared()
        app.backup.restore(data(l), false, "2.1.1", l.attachments, l.warnings)
        assertArrayEquals(newBytes, r.photoFile("foto.jpg").readBytes())
        assertTrue(r.user.notesNow().any { it.title == "Yedekteki" })
        assertTrue(r.user.notesNow().any { it.title == "Yerel" })
        assertNoLeftovers()
    }

    @Test fun fileMoveFailureKeepsOldFileAndData() = runBlocking {
        val l = prepared()
        app.backup.faults = RestoreFaults { if (it == "dosya:foto.jpg") throw java.io.IOException("test: taşıma hatası") }
        val e = runCatching { app.backup.restore(data(l), false, "2.1.1", l.attachments, l.warnings) }.exceptionOrNull()
        assertTrue("$e", e is BackupRejected && e.message!!.contains("döndürüldü"))
        assertUntouched()
    }

    @Test fun databaseStepFailureRollsBack() = runBlocking {
        val l = prepared()
        app.backup.faults = RestoreFaults { if (it == "veritabani-sonrasi") throw IllegalStateException("test") }
        assertTrue(runCatching { app.backup.restore(data(l), true, "2.1.1", l.attachments, l.warnings) }.exceptionOrNull() is BackupRejected)
        assertUntouched()
    }

    /** DataStore yazımı hata verirse (değiştirme modu) veritabanı ve dosyalar da eski hâline döner. */
    @Test fun settingsWriteFailureRollsBackEverything() = runBlocking {
        val l = prepared()
        app.backup.faults = RestoreFaults { if (it == "ayarlar") throw java.io.IOException("test: DataStore yazılamadı") }
        assertTrue(runCatching { app.backup.restore(data(l), true, "2.1.1", l.attachments, l.warnings) }.exceptionOrNull() is BackupRejected)
        assertUntouched()
    }

    /** Ayarlar yazıldıktan sonra hata olursa geri dönüş ayarları da eski hâline getirir. */
    @Test fun failureAfterSettingsRestoresSettings() = runBlocking {
        val l = prepared()
        app.backup.faults = RestoreFaults { if (it == "ayarlar-sonrasi") throw IllegalStateException("test") }
        assertTrue(runCatching { app.backup.restore(data(l), true, "2.1.1", l.attachments, l.warnings) }.exceptionOrNull() is BackupRejected)
        assertUntouched()
    }

    /** Süreç geri yükleme ortasında ölürse açılıştaki kurtarma önceki hâli getirir. */
    @Test fun interruptedRestoreRecoveredOnNextStart() = runBlocking {
        val l = prepared()
        app.backup.faults = RestoreFaults { if (it == "dosya:foto.jpg") throw SimulatedProcessDeath() }
        assertTrue(runCatching { app.backup.restore(data(l), true, "2.1.1", l.attachments, l.warnings) }.exceptionOrNull() is SimulatedProcessDeath)
        assertTrue("Günlük yarım işlemi göstermeli", File(app.filesDir, "geri-yukleme-gunlugu.txt").exists())
        app.backup.faults = RestoreFaults { }
        assertTrue(app.backup.recoverIfInterrupted())
        assertUntouched()
        assertFalse(app.backup.recoverIfInterrupted())
    }

    @Test fun successfulReplaceAppliesEverything() = runBlocking {
        val l = prepared()
        app.backup.restore(data(l), true, "2.1.1", l.attachments, l.warnings)
        assertEquals("DARK", r.settings.first().theme)
        assertArrayEquals(newBytes, r.photoFile("foto.jpg").readBytes())
        assertTrue(r.user.notesNow().any { it.title == "Yedekteki" })
        assertFalse(r.user.notesNow().any { it.title == "Yerel" })
        assertNoLeftovers()
    }
}
