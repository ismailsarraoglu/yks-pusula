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
const val BACKUP_VERSION = 2
private const val ZIP_JSON = "yedek.json"
private const val MAX_FILE_BYTES = 200L * 1024 * 1024

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

    fun encode(data: BackupData, createdAt: Long, appVersion: String): String {
        val body = json.encodeToString(BackupData.serializer(), data)
        return json.encodeToString(BackupFile.serializer(), BackupFile(BACKUP_FORMAT, BACKUP_VERSION, createdAt, appVersion, sha(body), data))
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
            return BackupCheck.Ok(BackupFile(BACKUP_FORMAT, version, createdAt, appVersion, "", data))
        } catch (e: Exception) {
            return BackupCheck.Bad("Dosya bozuk veya bu uygulamanın yedeği değil.")
        }
    }

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

/** Okunan yedek ve varsa ZIP içindeki dosyaların geçici konumu. */
data class LoadedBackup(val check: BackupCheck, val attachments: File?)

class BackupManager(private val repo: Repository, private val filesDir: File, private val cacheDir: File) {
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

    /** ZIP yedek: JSON verisi, fotoğraflar ve isteğe bağlı olarak PDF'ler. */
    suspend fun exportZip(out: OutputStream, appVersion: String, includePdf: Boolean) = withContext(Dispatchers.IO) {
        val text = export(appVersion)
        ZipOutputStream(out.buffered()).use { z ->
            z.putNextEntry(ZipEntry(ZIP_JSON)); z.write(text.toByteArray()); z.closeEntry()
            val dirs = if (includePdf) listOf("photos", "library") else listOf("photos")
            dirs.forEach { d ->
                filesDir.resolve(d).listFiles()?.filter { it.isFile }?.forEach { f ->
                    z.putNextEntry(ZipEntry("files/$d/${f.name}"))
                    f.inputStream().use { it.copyTo(z) }
                    z.closeEntry()
                }
            }
        }
        repo.updateSettings { it[Keys.lastBackupAt] = System.currentTimeMillis() }
    }

    /** Hem düz JSON hem ZIP yedeği okur. Bozuk dosya uygulamayı etkilemez. */
    suspend fun load(input: InputStream): LoadedBackup = withContext(Dispatchers.IO) {
        val temp = cacheDir.resolve("gelen-yedek-${System.nanoTime()}")
        try {
            input.use { i -> temp.outputStream().use { o -> i.copyTo(o) } }
            val zip = temp.inputStream().use { val h = ByteArray(2); it.read(h) == 2 && h[0] == 'P'.code.toByte() && h[1] == 'K'.code.toByte() }
            if (!zip) return@withContext LoadedBackup(BackupCodec.decode(temp.readText()), null)
            val dir = cacheDir.resolve("gelen-ekler-${System.nanoTime()}").apply { mkdirs() }
            var text: String? = null
            ZipInputStream(temp.inputStream().buffered()).use { z ->
                var e = z.nextEntry
                while (e != null) {
                    val n = e.name
                    if (n == ZIP_JSON) text = z.readBytes().toString(Charsets.UTF_8)
                    else if (!e.isDirectory && safeEntry(n)) {
                        val target = dir.resolve(n.removePrefix("files/")).apply { parentFile?.mkdirs() }
                        target.outputStream().use { o -> copyLimited(z, o) }
                    }
                    e = z.nextEntry
                }
            }
            val t = text ?: return@withContext LoadedBackup(BackupCheck.Bad("ZIP dosyasında yedek bulunamadı."), null)
            LoadedBackup(BackupCodec.decode(t), dir)
        } catch (e: Exception) {
            LoadedBackup(BackupCheck.Bad("Dosya bozuk veya bu uygulamanın yedeği değil."), null)
        } finally {
            temp.delete()
        }
    }

    private fun copyLimited(i: InputStream, o: OutputStream) {
        val buf = ByteArray(16 * 1024)
        var total = 0L
        while (true) {
            val n = i.read(buf)
            if (n < 0) break
            total += n
            require(total <= MAX_FILE_BYTES) { "Dosya çok büyük" }
            o.write(buf, 0, n)
        }
    }

    /** Yalnızca beklenen klasörlerdeki düz dosya adları kabul edilir. */
    private fun safeEntry(name: String): Boolean {
        val parts = name.split('/')
        return parts.size == 3 && parts[0] == "files" && parts[1] in setOf("photos", "library") && parts[2].isNotEmpty() && parts[2] != ".." && !parts[2].contains('\\')
    }

    /** Değiştirme öncesinde mevcut veriler cihaz içine güvenlik kopyası olarak yazılır. */
    suspend fun safetyCopy(appVersion: String): File = withContext(Dispatchers.IO) {
        val dir = filesDir.resolve("safety").apply { mkdirs() }
        dir.listFiles()?.sortedBy { it.lastModified() }?.dropLast(2)?.forEach { it.delete() }
        dir.resolve("once-yedek-${System.currentTimeMillis()}.json").apply { writeText(export(appVersion)) }
    }

    fun safetyCopies(): List<File> = filesDir.resolve("safety").listFiles()?.sortedByDescending { it.lastModified() }.orEmpty()

    suspend fun restore(d: BackupData, replace: Boolean, appVersion: String, attachments: File? = null) = withContext(Dispatchers.IO) {
        if (replace) safetyCopy(appVersion)
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
            val hasFile: (LibraryFile) -> Boolean = { f -> filesDir.resolve("library/${f.fileName}").exists() || attachments?.resolve("library/${f.fileName}")?.exists() == true }
            u.mergeLibrary(d.library.filter(hasFile))
        }
        attachments?.let { a ->
            listOf("photos", "library").forEach { sub ->
                val target = filesDir.resolve(sub).apply { mkdirs() }
                a.resolve(sub).listFiles()?.forEach { f -> f.copyTo(target.resolve(f.name), overwrite = true) }
            }
            a.deleteRecursively()
        }
        if (replace) {
            val s = d.settings
            repo.updateSettings { p ->
                s["theme"]?.let { p[Keys.theme] = it }
                s["fontScale"]?.toFloatOrNull()?.let { p[Keys.font] = it }
                s["pomodoroWork"]?.toIntOrNull()?.let { p[Keys.pWork] = it }
                s["pomodoroBreak"]?.toIntOrNull()?.let { p[Keys.pBreak] = it }
                s["weeklyGoalMinutes"]?.toIntOrNull()?.let { p[Keys.weekly] = it }
                s["monthlyGoalQuestions"]?.toIntOrNull()?.let { p[Keys.monthly] = it }
                s["diploma"]?.let { p[Keys.diploma] = it }
                s["placedLastYear"]?.toBooleanStrictOrNull()?.let { p[Keys.placed] = it }
            }
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
