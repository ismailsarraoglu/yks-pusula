package app.ykspusula

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.ykspusula.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
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

    @Test fun duplicateEntryRejected() = runBlocking {
        val m = bad(zip { z ->
            z.putNextEntry(ZipEntry("yedek.json")); z.write("{}".toByteArray()); z.closeEntry()
            z.putNextEntry(ZipEntry("yedek.json")); z.write("{}".toByteArray()); z.closeEntry()
        })
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
    }

    @Test fun missingPhotoReportedNotFatal() = runBlocking {
        r.saveWrong(r.newWrong("tyt-mat", null, null, "Kitap", 3, "4", "Dikkatsizlik", "", "yok.jpg", null))
        val bytes = ByteArrayOutputStream().also { app.backup.exportZip(it, "2.1.0", false) }.toByteArray()
        val l = app.backup.load(ByteArrayInputStream(bytes))
        assertTrue(l.check is BackupCheck.Ok)
        assertTrue(l.warnings.joinToString(), l.warnings.any { it.contains("fotoğrafı yedekte yok") })
        l.attachments?.deleteRecursively()
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
}
