package app.ykspusula.data

import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

const val BACKUP_FORMAT = "yks-pusula-yedek"
/** 3: ZIP içindeki her ek dosyanın boyutu ve SHA-256 özeti yedek dosyasına yazılır. */
const val BACKUP_VERSION = 3
private const val ZIP_JSON = "yedek.json"
private val ATTACHMENT_DIRS = listOf("photos", "library")

/** Kötü niyetli veya bozuk dosyaya karşı sınırlar. Gerçek bir öğrenci yedeği bunların çok altında kalır. */
data class BackupLimits(
    val maxInputBytes: Long = 700L * 1024 * 1024,
    val maxJsonBytes: Long = 32L * 1024 * 1024,
    val maxFileBytes: Long = 150L * 1024 * 1024,
    val maxTotalBytes: Long = 1024L * 1024 * 1024,
    val maxEntries: Int = 5_000,
)

/** Sınır aşıldığında veya bütünlük bozulduğunda atılır; mesaj kullanıcıya gösterilir. */
class BackupRejected(message: String) : Exception(message)

@Serializable
data class BackupFileEntry(val folder: String, val name: String, val size: Long, val sha256: String)

@Serializable
data class BackupData(
    val profile: UserProfile? = null,
    val examConfigs: List<ExamConfiguration> = emptyList(),
    val logs: List<PracticeLog> = emptyList(),
    val sources: List<StudySource> = emptyList(),
    val plans: List<StudyPlan> = emptyList(),
    val tasks: List<StudyTask> = emptyList(),
    val sessions: List<StudySession> = emptyList(),
    val mocks: List<MockExam> = emptyList(),
    val results: List<MockExamResult> = emptyList(),
    val progress: List<TopicProgress> = emptyList(),
    val wrongs: List<WrongEntry> = emptyList(),
    val wrongReviews: List<WrongReview> = emptyList(),
    val route: List<RouteItem> = emptyList(),
    val revisions: List<PlanRevision> = emptyList(),
    val cardProgress: List<FlashcardProgress> = emptyList(),
    val cardReviews: List<CardReview> = emptyList(),
    val userCards: List<UserCardBackup> = emptyList(),
    val notes: List<UserNote> = emptyList(),
    val itemNotes: List<ItemNote> = emptyList(),
    val bookmarks: List<Bookmark> = emptyList(),
    val reminders: List<Reminder> = emptyList(),
    val universities: List<TargetUniversity> = emptyList(),
    val departments: List<TargetDepartment> = emptyList(),
    val universityRecords: List<UniversityRecord> = emptyList(),
    val netGoals: List<NetGoal> = emptyList(),
    val restDays: List<RestDay> = emptyList(),
    val library: List<LibraryFile> = emptyList(),
    val settings: Map<String, String> = emptyMap(),
)

@Serializable
data class BackupFile(
    val format: String,
    val version: Int,
    val createdAt: Long,
    val appVersion: String,
    val checksum: String,
    val data: BackupData,
    val files: List<BackupFileEntry> = emptyList(),
)

/* Sürüm 1 yedekleri soru bankalı uygulamadan gelir; okunup yeni biçime çevrilir. */
@Serializable
data class LegacyAttempt(val questionId: String, val topicId: String, val subjectId: String, val selected: String? = null, val isCorrect: Boolean, val answeredAt: Long, val source: String = "", val mockExamId: String? = null)
@Serializable
data class LegacyWrong(
    val questionId: String, val topicId: String, val subjectId: String, val givenAnswer: String, val correctAnswer: String, val firstWrongAt: Long, val lastWrongAt: Long,
    val nextReviewAt: Long, val stage: Int = 0, val wrongCount: Int = 1, val reason: String = "", val note: String = "", val resolved: Boolean = false,
)
@Serializable
data class BackupDataV1(
    val profile: UserProfile? = null,
    val examConfigs: List<ExamConfiguration> = emptyList(),
    val attempts: List<LegacyAttempt> = emptyList(),
    val plans: List<StudyPlan> = emptyList(),
    val tasks: List<StudyTask> = emptyList(),
    val sessions: List<StudySession> = emptyList(),
    val mocks: List<MockExam> = emptyList(),
    val results: List<MockExamResult> = emptyList(),
    val progress: List<TopicProgress> = emptyList(),
    val wrongs: List<LegacyWrong> = emptyList(),
    val cardProgress: List<FlashcardProgress> = emptyList(),
    val userCards: List<UserCardBackup> = emptyList(),
    val notes: List<UserNote> = emptyList(),
    val bookmarks: List<Bookmark> = emptyList(),
    val reminders: List<Reminder> = emptyList(),
    val universities: List<TargetUniversity> = emptyList(),
    val departments: List<TargetDepartment> = emptyList(),
    val universityRecords: List<UniversityRecord> = emptyList(),
    val settings: Map<String, String> = emptyMap(),
)

sealed interface BackupCheck {
    data class Ok(val file: BackupFile) : BackupCheck
    data class Bad(val message: String) : BackupCheck
}

object BackupCodec {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private fun sha(text: String) = MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }

    fun encode(data: BackupData, createdAt: Long, appVersion: String, files: List<BackupFileEntry> = emptyList()): String {
        val body = json.encodeToString(BackupData.serializer(), data)
        return json.encodeToString(BackupFile.serializer(), BackupFile(BACKUP_FORMAT, BACKUP_VERSION, createdAt, appVersion, sha(body), data, files))
    }

    /** Biçim, sürüm ve bütünlük denetimi. Geçmezse hiçbir veri değiştirilmez. */
    fun decode(text: String): BackupCheck {
        val root = try {
            json.parseToJsonElement(text).jsonObject
        } catch (e: Exception) {
            return BackupCheck.Bad("Dosya bozuk veya bu uygulamanın yedeği değil.")
        }
        try {
            val format = root["format"]?.jsonPrimitive?.content
            if (format != BACKUP_FORMAT) return BackupCheck.Bad("Bu dosya bir YKS Pusula yedeği değil.")
            val version = root.getValue("version").jsonPrimitive.int
            if (version > BACKUP_VERSION) return BackupCheck.Bad("Yedek daha yeni bir sürümle alınmış (sürüm $version). Önce uygulamayı güncelle.")
            val body = root.getValue("data")
            if (sha(body.toString()) != root.getValue("checksum").jsonPrimitive.content) return BackupCheck.Bad("Yedek bütünlük denetiminden geçemedi; dosya değiştirilmiş veya bozulmuş.")
            val createdAt = root.getValue("createdAt").jsonPrimitive.content.toLong()
            val appVersion = root["appVersion"]?.jsonPrimitive?.content.orEmpty()
            val data = if (version >= 2) json.decodeFromJsonElement(BackupData.serializer(), body)
            else fromV1(json.decodeFromJsonElement(BackupDataV1.serializer(), body))
            val files = root["files"]?.let { json.decodeFromJsonElement(kotlinx.serialization.builtins.ListSerializer(BackupFileEntry.serializer()), it) }.orEmpty()
            if (files.any { it.folder !in ATTACHMENT_DIRS || !safeName(it.name) }) return BackupCheck.Bad("Yedekteki dosya listesi geçersiz.")
            return BackupCheck.Ok(BackupFile(BACKUP_FORMAT, version, createdAt, appVersion, "", data, files))
        } catch (e: Exception) {
            return BackupCheck.Bad("Dosya bozuk veya bu uygulamanın yedeği değil.")
        }
    }

    /** Yalnızca düz dosya adı: klasör ayracı, üst klasör ve gizli ad kabul edilmez. */
    fun safeName(n: String): Boolean =
        n.isNotEmpty() && n.length <= 200 && n != "." && n != ".." && !n.startsWith(".") && n.none { it == '/' || it == '\\' || it == '\u0000' }

    /** Eski soru kayıtları gün ve konu bazında performans kaydına, eski yanlışlar defter kaydına dönüşür. */
    fun fromV1(d: BackupDataV1): BackupData {
        val logs = d.attempts.groupBy { Triple(dateOf(it.answeredAt).toString(), it.topicId, it.mockExamId) }.map { (k, list) ->
            val first = list.first()
            PracticeLog(
                id = "legacy-${k.first}-${k.second}-${k.third.orEmpty()}", at = first.answeredAt, subjectId = first.subjectId, topicId = k.second, mockExamId = k.third,
                correct = list.count { it.selected != null && it.isCorrect }, wrong = list.count { it.selected != null && !it.isCorrect }, blank = list.count { it.selected == null },
                origin = "LEGACY", createdAt = first.answeredAt, updatedAt = first.answeredAt,
            )
        }
        val wrongs = d.wrongs.map {
            WrongEntry(
                id = "legacy-${it.questionId}", subjectId = it.subjectId, topicId = it.topicId, sourceLabel = "Eski soru bankası", reason = it.reason, note = it.note,
                questionText = "Bu kayıt önceki sürümün soru bankasından taşındı.", givenAnswer = it.givenAnswer, correctAnswer = it.correctAnswer, firstWrongAt = it.firstWrongAt,
                lastWrongAt = it.lastWrongAt, nextReviewAt = it.nextReviewAt, stage = it.stage, wrongCount = it.wrongCount, resolved = it.resolved, origin = "LEGACY", updatedAt = it.lastWrongAt,
            )
        }
        return BackupData(
            profile = d.profile, examConfigs = d.examConfigs, logs = logs, plans = d.plans, tasks = d.tasks, sessions = d.sessions,
            mocks = d.mocks.map { if (it.status == "RUNNING") it.copy(status = "ENTRY") else it }, results = d.results,
            progress = d.progress.map { if (it.status == 2) it.copy(status = 1) else it }, wrongs = wrongs, cardProgress = d.cardProgress, userCards = d.userCards, notes = d.notes,
            bookmarks = d.bookmarks.filter { it.kind != "QUESTION" && it.kind != "LATER" }, reminders = d.reminders, universities = d.universities, departments = d.departments,
            universityRecords = d.universityRecords, settings = d.settings,
        )
    }
}

/** Okunan yedek, ZIP içindeki dosyaların geçici konumu ve kullanıcıya gösterilecek uyarılar. */
data class LoadedBackup(val check: BackupCheck, val attachments: File?, val warnings: List<String> = emptyList(), val fileCount: Int = 0)

/** Geri yükleme sonucu: kaç kayıt ve dosyanın aktarıldığı. */
data class RestoreReport(val records: Int, val files: Int, val warnings: List<String>, val safetyCopy: File?)

/**
 * Otomatik testlerin geri yüklemenin belirli adımlarında hata üretebilmesi için. Uygulamada boştur.
 * Adımlar: "veritabani-sonrasi", "dosya:<ad>", "ayarlar", "ayarlar-sonrasi".
 */
fun interface RestoreFaults { fun at(stage: String) }

/** Testte süreç ölümünü taklit eder: geri yükleme kendi temizliğini yapamadan durur. */
class SimulatedProcessDeath : RuntimeException("test: süreç öldü")

class BackupManager(private val repo: Repository, private val filesDir: File, private val cacheDir: File, val limits: BackupLimits = BackupLimits()) {
    @Volatile var faults: RestoreFaults = RestoreFaults { }
    /** Geri yükleme günlüğü: yarıda kalan işlem uygulama yeniden açıldığında geri alınır. */
    private val journal get() = filesDir.resolve("geri-yukleme-gunlugu.txt")
    private val u get() = repo.user

    suspend fun snapshot(): BackupData = withContext(Dispatchers.IO) {
        val s = repo.settings.first()
        BackupData(
            profile = u.profileNow(), examConfigs = u.examConfigsNow(), logs = u.logsNow(), sources = u.sourcesNow(),
            plans = u.plansNow(), tasks = u.allTasks(), sessions = u.sessionsNow(), mocks = u.mocksNow(), results = u.resultsNow(),
            progress = u.progressAll(), wrongs = u.wrongsNow(), wrongReviews = u.wrongReviewsNow(), route = u.routeNow(), revisions = u.revisionsNow(),
            cardProgress = u.cardProgressNow(), cardReviews = u.cardReviewsNow(),
            userCards = u.userCardsNow().map { UserCardBackup(it.id, it.deck, it.subjectName, it.front, it.back, it.partOfSpeech, it.example) },
            notes = u.notesNow(), itemNotes = u.itemNotesNow(), bookmarks = u.bookmarksNow(), reminders = u.remindersNow(),
            universities = u.universitiesNow(), departments = u.departmentsNow(), universityRecords = u.universityRecordsNow(),
            netGoals = u.netGoalsNow(), restDays = u.restDaysNow(), library = u.libraryNow(),
            settings = mapOf(
                "theme" to s.theme, "fontScale" to s.fontScale.toString(), "pomodoroWork" to s.pomodoroWork.toString(),
                "pomodoroBreak" to s.pomodoroBreak.toString(), "weeklyGoalMinutes" to s.weeklyGoalMinutes.toString(),
                "monthlyGoalQuestions" to s.monthlyGoalQuestions.toString(), "diploma" to s.diploma, "placedLastYear" to s.placedLastYear.toString(),
            ),
        )
    }

    suspend fun export(appVersion: String): String = BackupCodec.encode(snapshot(), System.currentTimeMillis(), appVersion)

    private fun attachmentFiles(includePdf: Boolean): List<Pair<String, File>> =
        (if (includePdf) ATTACHMENT_DIRS else listOf("photos")).flatMap { d ->
            filesDir.resolve(d).listFiles()?.filter { it.isFile && BackupCodec.safeName(it.name) }.orEmpty().sortedBy { it.name }.map { d to it }
        }

    /** ZIP yedek: JSON verisi, fotoğraflar ve isteğe bağlı olarak PDF'ler. Her ekin boyutu ve SHA-256 özeti JSON'a yazılır. */
    suspend fun exportZip(out: OutputStream, appVersion: String, includePdf: Boolean) = withContext(Dispatchers.IO) {
        writeZip(out, appVersion, includePdf)
        repo.updateSettings { it[Keys.lastBackupAt] = System.currentTimeMillis() }
    }

    private suspend fun writeZip(out: OutputStream, appVersion: String, includePdf: Boolean) {
        val files = attachmentFiles(includePdf)
        val manifest = files.map { (d, f) -> BackupFileEntry(d, f.name, f.length(), sha256(f)) }
        val text = BackupCodec.encode(snapshot(), System.currentTimeMillis(), appVersion, manifest)
        ZipOutputStream(out.buffered()).use { z ->
            z.putNextEntry(ZipEntry(ZIP_JSON)); z.write(text.toByteArray()); z.closeEntry()
            files.forEach { (d, f) ->
                z.putNextEntry(ZipEntry("files/$d/${f.name}"))
                f.inputStream().use { it.copyTo(z) }
                z.closeEntry()
            }
        }
    }

    /**
     * Hem düz JSON hem ZIP yedeği okur. Boyut, dosya sayısı, yol ve bütünlük denetlenir.
     * Bu aşamada uygulamanın verisine dokunulmaz; dosyalar geçici klasöre açılır.
     */
    suspend fun load(input: InputStream): LoadedBackup = withContext(Dispatchers.IO) {
        val temp = cacheDir.resolve("gelen-yedek-${System.nanoTime()}")
        var dir: File? = null
        try {
            input.use { i -> temp.outputStream().use { o -> copyLimited(i, o, limits.maxInputBytes, "Yedek dosyası çok büyük.") } }
            val zip = temp.inputStream().use { val h = ByteArray(2); it.read(h) == 2 && h[0] == 'P'.code.toByte() && h[1] == 'K'.code.toByte() }
            if (!zip) {
                if (temp.length() > limits.maxJsonBytes) throw BackupRejected("Yedek dosyası çok büyük.")
                return@withContext LoadedBackup(BackupCodec.decode(temp.readText()), null)
            }
            val target = cacheDir.resolve("gelen-ekler-${System.nanoTime()}").apply { mkdirs() }
            dir = target
            var text: String? = null
            var entries = 0
            var total = 0L
            val seen = HashSet<String>()
            ZipInputStream(temp.inputStream().buffered()).use { z ->
                var e = z.nextEntry
                while (e != null) {
                    if (++entries > limits.maxEntries) throw BackupRejected("Yedekte çok fazla dosya var.")
                    val n = e.name
                    if (!seen.add(n)) throw BackupRejected("Yedekte aynı adlı iki dosya var; dosya bozulmuş olabilir.")
                    if (n == ZIP_JSON) {
                        // Belleğe değil dosyaya açılır; sınır aşılırsa okunmadan reddedilir.
                        val jf = target.resolve("yedek.json.tmp")
                        total += jf.outputStream().use { o -> copyLimited(z, o, limits.maxJsonBytes, "Yedek verisi çok büyük.") }
                        text = jf.readText(); jf.delete()
                    } else if (!e.isDirectory) {
                        val (folder, name) = entryPath(n) ?: throw BackupRejected("Yedekte beklenmeyen dosya yolu var: $n")
                        val out = target.resolve(folder).apply { mkdirs() }.resolve(name)
                        if (!out.canonicalPath.startsWith(target.canonicalPath + File.separator)) throw BackupRejected("Yedekte geçersiz dosya yolu var.")
                        total += out.outputStream().use { o -> copyLimited(z, o, limits.maxFileBytes, "Yedekteki bir dosya çok büyük.") }
                    }
                    if (total > limits.maxTotalBytes) throw BackupRejected("Yedeğin açılmış boyutu çok büyük.")
                    e = z.nextEntry
                }
            }
            val t = text ?: throw BackupRejected("ZIP dosyasında yedek bulunamadı.")
            val check = BackupCodec.decode(t)
            if (check !is BackupCheck.Ok) { target.deleteRecursively(); return@withContext LoadedBackup(check, null) }
            val warnings = verifyAttachments(check.file, target)
            LoadedBackup(check, target, warnings, check.file.files.size.takeIf { it > 0 } ?: countFiles(target))
        } catch (e: BackupRejected) {
            dir?.deleteRecursively()
            LoadedBackup(BackupCheck.Bad(e.message ?: "Yedek reddedildi."), null)
        } catch (e: java.util.zip.ZipException) {
            // Android 14 ve sonrası ".." içeren girişleri kendisi reddeder.
            dir?.deleteRecursively()
            val path = e.message.orEmpty().contains("path", ignoreCase = true)
            LoadedBackup(BackupCheck.Bad(if (path) "Yedekte geçersiz dosya yolu var." else "Dosya bozuk veya bu uygulamanın yedeği değil."), null)
        } catch (e: Exception) {
            dir?.deleteRecursively()
            LoadedBackup(BackupCheck.Bad("Dosya bozuk veya bu uygulamanın yedeği değil."), null)
        } finally {
            temp.delete()
        }
    }

    /** Sürüm 3 yedeklerinde her ek listedeki boyut ve özetle karşılaştırılır; uyuşmayan yedek reddedilir. */
    private fun verifyAttachments(f: BackupFile, dir: File): List<String> {
        val warnings = ArrayList<String>()
        if (f.files.isNotEmpty()) {
            f.files.forEach { e ->
                val file = dir.resolve(e.folder).resolve(e.name)
                if (!file.isFile) throw BackupRejected("Yedekte eksik dosya var: ${e.name}")
                if (file.length() != e.size || sha256(file) != e.sha256) throw BackupRejected("Yedekteki bir dosya bozulmuş: ${e.name}")
            }
            val listed = f.files.map { "${it.folder}/${it.name}" }.toSet()
            ATTACHMENT_DIRS.forEach { d -> dir.resolve(d).listFiles()?.filter { "$d/${it.name}" !in listed }?.forEach { it.delete() } }
        } else if (countFiles(dir) > 0) {
            warnings += "Bu yedek eski biçimde; ek dosyaların bütünlüğü doğrulanamadı."
        }
        val photos = f.data.wrongs.mapNotNull { it.photoFile }.filter { it.isNotBlank() }
        val missing = photos.count { p -> !dir.resolve("photos").resolve(p).isFile && !filesDir.resolve("photos").resolve(p).isFile }
        if (missing > 0) warnings += "$missing yanlış kaydının fotoğrafı yedekte yok. Kayıtlar fotoğrafsız aktarılır."
        val pdfs = f.data.library.count { l -> !dir.resolve("library").resolve(l.fileName).isFile && !filesDir.resolve("library").resolve(l.fileName).isFile }
        if (pdfs > 0) warnings += "$pdfs PDF dosyası yedekte yok; bunlar kitaplığa eklenmez."
        return warnings
    }

    private fun countFiles(dir: File) = ATTACHMENT_DIRS.sumOf { dir.resolve(it).listFiles()?.size ?: 0 }

    private fun entryPath(name: String): Pair<String, String>? {
        val parts = name.split('/')
        if (parts.size != 3 || parts[0] != "files" || parts[1] !in ATTACHMENT_DIRS || !BackupCodec.safeName(parts[2])) return null
        return parts[1] to parts[2]
    }

    private fun copyLimited(i: InputStream, o: OutputStream, limit: Long, message: String): Long {
        val buf = ByteArray(16 * 1024)
        var total = 0L
        while (true) {
            val n = i.read(buf)
            if (n < 0) break
            total += n
            if (total > limit) throw BackupRejected(message)
            o.write(buf, 0, n)
        }
        return total
    }

    /** Değiştirme öncesinde veritabanı, fotoğraflar ve PDF'ler cihaz içine tam güvenlik kopyası olarak yazılır. */
    suspend fun safetyCopy(appVersion: String): File = withContext(Dispatchers.IO) {
        val dir = filesDir.resolve("safety").apply { mkdirs() }
        val target = dir.resolve("once-yedek-${System.currentTimeMillis()}.zip")
        val part = File(target.path + ".yaziliyor")
        part.outputStream().use { writeZip(it, appVersion, includePdf = true) }
        // Yazılan kopya okunup doğrulanmadan eski kopyalar silinmez.
        val check = load(part.inputStream())
        check.attachments?.deleteRecursively()
        if (check.check !is BackupCheck.Ok) { part.delete(); throw BackupRejected("Güvenlik kopyası doğrulanamadı; geri yükleme yapılmadı.") }
        if (!part.renameTo(target)) { part.delete(); throw BackupRejected("Güvenlik kopyası kaydedilemedi; geri yükleme yapılmadı.") }
        dir.listFiles()?.filter { it != target && !it.name.endsWith(".yaziliyor") }?.sortedBy { it.lastModified() }?.dropLast(2)?.forEach { it.delete() }
        dir.listFiles()?.filter { it.name.endsWith(".yaziliyor") }?.forEach { it.delete() }
        target
    }

    fun safetyCopies(): List<File> = filesDir.resolve("safety").listFiles()?.filter { !it.name.endsWith(".yaziliyor") }?.sortedByDescending { it.lastModified() }.orEmpty()

    /**
     * Aşamalı ve geri alınabilir geri yükleme (birleştirme ve değiştirme için aynı):
     * 1. Ekler uygulama klasöründe geçici alana kopyalanır; mevcut verilere dokunulmaz.
     * 2. Fotoğraf, PDF ve ayarlar dahil tam güvenlik kopyası alınır ve doğrulanır.
     * 3. Diske geri yükleme günlüğü yazılır.
     * 4. Veritabanı tek işlemde yazılır.
     * 5. Ekler tek tek yerine konur: aynı adlı eski dosya silinmez, önce kenara alınır.
     * 6. Değiştirmede ayarlar uygulanır.
     * Herhangi bir adımda hata olursa dosyalar günlükten, veritabanı ve ayarlar güvenlik kopyasından eski hâline döner.
     * Süreç yarıda ölürse uygulama açılışında [recoverIfInterrupted] aynısını yapar.
     */
    suspend fun restore(d: BackupData, replace: Boolean, appVersion: String, attachments: File? = null, warnings: List<String> = emptyList()): RestoreReport = withContext(Dispatchers.IO) {
        recoverIfInterrupted()
        val staging = filesDir.resolve("geri-yukleme-${System.nanoTime()}")
        var completed = false
        try {
            var staged = 0
            attachments?.let { a ->
                ATTACHMENT_DIRS.forEach { sub ->
                    a.resolve(sub).listFiles()?.filter { it.isFile && BackupCodec.safeName(it.name) }?.forEach { f ->
                        val t = staging.resolve(sub).apply { mkdirs() }.resolve(f.name)
                        f.copyTo(t, overwrite = true)
                        if (t.length() != f.length()) throw BackupRejected("Dosya kopyalanamadı: ${f.name}")
                        staged++
                    }
                }
            }
            val safety = safetyCopy(appVersion)
            journal.writeText("BASLA\t${safety.absolutePath}\n")
            try {
                writeDatabase(d, replace, staging)
                journal.appendText("VERITABANI\n")
                faults.at("veritabani-sonrasi")
                moveStaged(staging)
                if (replace) applySettings(d.settings, fault = true)
                faults.at("ayarlar-sonrasi")
                commitJournal()
            } catch (e: SimulatedProcessDeath) {
                throw e
            } catch (e: Exception) {
                val back = runCatching { revertFromJournal() }.isSuccess
                throw BackupRejected(
                    if (back) "Geri yükleme tamamlanamadı. Verilerin, dosyaların ve ayarların geri yükleme öncesindeki hâline döndürüldü."
                    else "Geri yükleme tamamlanamadı ve otomatik geri dönüş de başarısız oldu. Önceki verilerin şu güvenlik kopyasında: ${safety.name}",
                )
            }
            completed = true
            val records = d.logs.size + d.sessions.size + d.mocks.size + d.wrongs.size + d.notes.size + d.tasks.size + d.sources.size + d.progress.size
            val after = u.wrongsNow().size
            val extra = if (after < d.wrongs.size) listOf("Bazı yanlış kayıtları aktarılamadı (${d.wrongs.size - after}).") else emptyList()
            RestoreReport(records, staged, warnings + extra, safety)
        } finally {
            if (completed || !journal.exists()) staging.deleteRecursively()
            attachments?.deleteRecursively()
        }
    }

    private suspend fun writeDatabase(d: BackupData, replace: Boolean, staging: File) {
        val cards = d.userCards.map { Flashcard(it.id, it.deck, it.subjectName, it.front, it.back, it.partOfSpeech, it.example, true, "user") }
        repo.db.withTransaction {
            if (replace) {
                u.clearLogs(); u.clearWrongReviews(); u.clearPlans(); u.clearTasks(); u.clearSessions(); u.clearMocks()
                u.clearResults(); u.clearProgress(); u.clearWrongs(); u.clearCardProgress(); u.clearUserCards(); u.clearNotes()
                u.clearBookmarks(); u.clearReminders(); u.clearDepartments(); u.clearUniversities()
                u.clearUniversityRecords(); u.clearSources(); u.clearRoute(); u.clearRevisions(); u.clearCardReviews()
                u.clearItemNotes(); u.clearNetGoals(); u.clearRestDays(); u.clearLibrary()
            }
            val current = u.profileNow()
            if (d.profile != null && (replace || current == null || !current.onboarded)) u.saveProfile(d.profile)
            if (replace) d.examConfigs.forEach { u.saveExamConfig(it) }
            u.mergeSources(d.sources); u.mergeLogs(d.logs); u.mergePlans(d.plans); u.mergeTasks(d.tasks)
            u.mergeSessions(d.sessions); u.mergeMocks(d.mocks); u.mergeResults(d.results); u.mergeProgress(d.progress)
            u.mergeWrongs(d.wrongs); u.mergeWrongReviews(d.wrongReviews); u.mergeRoute(d.route); u.mergeRevisions(d.revisions)
            u.mergeCardProgress(d.cardProgress); u.mergeCardReviews(d.cardReviews); u.mergeCards(cards); u.mergeNotes(d.notes); u.mergeItemNotes(d.itemNotes)
            u.mergeBookmarks(d.bookmarks); u.mergeReminders(d.reminders); u.mergeUniversities(d.universities)
            u.mergeDepartments(d.departments); u.mergeUniversityRecords(d.universityRecords); u.mergeNetGoals(d.netGoals); u.mergeRestDays(d.restDays)
            val hasFile: (LibraryFile) -> Boolean = { f -> filesDir.resolve("library/${f.fileName}").exists() || staging.resolve("library/${f.fileName}").exists() }
            u.mergeLibrary(d.library.filter(hasFile))
        }
    }

    /**
     * Ekler yerine konur. Aynı adlı mevcut dosya silinmez: önce gizli bir ada taşınır ve günlüğe yazılır,
     * yeni dosya yerine oturduktan ve bütün geri yükleme bittikten sonra silinir.
     */
    private fun moveStaged(staging: File) {
        ATTACHMENT_DIRS.forEach { sub ->
            val target = filesDir.resolve(sub).apply { mkdirs() }
            staging.resolve(sub).listFiles()?.sortedBy { it.name }?.forEach { f ->
                val dest = target.resolve(f.name)
                val old = if (dest.exists()) target.resolve(".eski-${System.nanoTime()}-${f.name}") else null
                journal.appendText("DOSYA\t${dest.absolutePath}\t${old?.absolutePath ?: "-"}\n")
                if (old != null && !dest.renameTo(old)) throw java.io.IOException("Eski dosya kenara alınamadı: ${f.name}")
                faults.at("dosya:${f.name}")
                if (!f.renameTo(dest)) {
                    f.copyTo(dest, overwrite = false)
                    if (dest.length() != f.length()) throw java.io.IOException("Dosya taşınamadı: ${f.name}")
                    f.delete()
                }
            }
        }
    }

    /** Başarılı bitişte kenara alınan eski dosyalar ve günlük silinir. */
    private fun commitJournal() {
        journalEntries().forEach { (_, old) -> old?.delete() }
        journal.delete()
    }

    private fun journalEntries(): List<Pair<File, File?>> = journal.takeIf { it.exists() }?.readLines().orEmpty()
        .filter { it.startsWith("DOSYA\t") }.map { l -> val p = l.split('\t'); File(p[1]) to p[2].takeIf { it != "-" }?.let(::File) }

    /**
     * Günlükteki dosya taşımaları ters sırayla geri alınır; veritabanı ve ayarlar güvenlik kopyasından yüklenir.
     * Güvenlik kopyası işlem başlamadan hemen önce alındığı için tekrar çalıştırılması güvenlidir.
     */
    private suspend fun revertFromJournal() {
        val lines = journal.takeIf { it.exists() }?.readLines().orEmpty()
        journalEntries().asReversed().forEach { (dest, old) ->
            if (old != null) {
                if (old.exists()) { dest.delete(); if (!old.renameTo(dest)) { old.copyTo(dest, true); old.delete() } }
            } else dest.delete()
        }
        val safety = lines.firstOrNull { it.startsWith("BASLA\t") }?.substringAfter('\t')?.let(::File)
        if (safety != null && safety.exists()) rollback(safety)
        journal.delete()
    }

    /** Uygulama açılışında çağrılır: yarıda kalan bir geri yükleme varsa önceki hâle döner. */
    suspend fun recoverIfInterrupted(): Boolean = withContext(Dispatchers.IO) {
        if (!journal.exists()) return@withContext false
        revertFromJournal()
        filesDir.listFiles()?.filter { it.name.startsWith("geri-yukleme-") && it.isDirectory }?.forEach { it.deleteRecursively() }
        true
    }

    /** Güvenlik kopyasından veritabanını ve ayarları eski hâline döndürür (ekler günlükten geri alınır). */
    private suspend fun rollback(safety: File) {
        val l = load(safety.inputStream())
        val ok = l.check as? BackupCheck.Ok ?: throw BackupRejected("Güvenlik kopyası okunamadı.")
        try {
            writeDatabase(ok.file.data, replace = true, filesDir.resolve("yok"))
            applySettings(ok.file.data.settings, fault = false)
        } finally {
            l.attachments?.deleteRecursively()
        }
    }

    private suspend fun applySettings(s: Map<String, String>, fault: Boolean) {
        repo.updateSettings { p ->
            s["theme"]?.let { p[Keys.theme] = it }
            if (fault) faults.at("ayarlar")
            s["fontScale"]?.toFloatOrNull()?.let { p[Keys.font] = it }
            s["pomodoroWork"]?.toIntOrNull()?.let { p[Keys.pWork] = it }
            s["pomodoroBreak"]?.toIntOrNull()?.let { p[Keys.pBreak] = it }
            s["weeklyGoalMinutes"]?.toIntOrNull()?.let { p[Keys.weekly] = it }
            s["monthlyGoalQuestions"]?.toIntOrNull()?.let { p[Keys.monthly] = it }
            s["diploma"]?.let { p[Keys.diploma] = it }
            s["placedLastYear"]?.toBooleanStrictOrNull()?.let { p[Keys.placed] = it }
        }
    }

    companion object {
        fun sha256(f: File): String {
            val md = MessageDigest.getInstance("SHA-256")
            f.inputStream().use { i -> val b = ByteArray(16 * 1024); while (true) { val n = i.read(b); if (n < 0) break; md.update(b, 0, n) } }
            return md.digest().joinToString("") { "%02x".format(it) }
        }
    }
}

/** Üniversite veri paketi: sürümü ve kaynağı olmayan dosya kabul edilmez. */
@Serializable
data class UniversityPackFile(
    val source: String,
    val version: String,
    val dataYear: Int? = null,
    val records: List<UniversityDto>,
)

@Serializable
data class UniversityDto(
    val university: String, val city: String = "", val department: String, val scoreType: String = "",
    val quota: Int? = null, val baseScore: Double? = null, val baseRank: Int? = null,
)

object UniversityImport {
    private val json = Json { ignoreUnknownKeys = true }

    /** CSV başlığı: universite;sehir;bolum;puan_turu;kontenjan;taban_puan;basari_sirasi — ilk iki satır: #kaynak=... ve #surum=... */
    fun parse(name: String, text: String): UniversityPackFile {
        if (name.endsWith(".json", ignoreCase = true) || text.trimStart().startsWith("{")) {
            return json.decodeFromString(UniversityPackFile.serializer(), text)
        }
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val meta = lines.filter { it.startsWith("#") }.associate { it.drop(1).substringBefore('=').trim() to it.substringAfter('=').trim() }
        val source = meta["kaynak"].orEmpty()
        val version = meta["surum"].orEmpty()
        val rows = lines.filter { !it.startsWith("#") }.drop(1).map { line ->
            val c = line.split(';').map { it.trim() }
            require(c.size >= 3) { "Eksik sütunlu satır: $line" }
            UniversityDto(
                c[0], c.getOrElse(1) { "" }, c[2], c.getOrElse(3) { "" }, c.getOrNull(4)?.toIntOrNull(),
                c.getOrNull(5)?.replace(',', '.')?.toDoubleOrNull(), c.getOrNull(6)?.toIntOrNull(),
            )
        }
        return UniversityPackFile(source, version, meta["yil"]?.toIntOrNull(), rows)
    }

    fun errors(p: UniversityPackFile): List<String> {
        val e = ArrayList<String>()
        if (p.source.isBlank()) e += "Kaynak bilgisi yok"
        if (p.version.isBlank()) e += "Sürüm bilgisi yok"
        if (p.records.isEmpty()) e += "Kayıt bulunamadı"
        p.records.forEachIndexed { i, r -> if (r.university.isBlank() || r.department.isBlank()) e += "Satır ${i + 1}: üniversite veya bölüm boş" }
        return e
    }
}
