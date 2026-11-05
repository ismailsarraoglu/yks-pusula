package app.ykspusula.data

import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest

const val BACKUP_FORMAT = "yks-pusula-yedek"
const val BACKUP_VERSION = 1

@Serializable
data class BackupData(
    val profile: UserProfile? = null,
    val examConfigs: List<ExamConfiguration> = emptyList(),
    val attempts: List<QuestionAttempt> = emptyList(),
    val records: List<AnswerRecord> = emptyList(),
    val plans: List<StudyPlan> = emptyList(),
    val tasks: List<StudyTask> = emptyList(),
    val sessions: List<StudySession> = emptyList(),
    val mocks: List<MockExam> = emptyList(),
    val results: List<MockExamResult> = emptyList(),
    val progress: List<TopicProgress> = emptyList(),
    val wrongs: List<WrongQuestion> = emptyList(),
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

@Serializable
data class BackupFile(
    val format: String,
    val version: Int,
    val createdAt: Long,
    val appVersion: String,
    val checksum: String,
    val data: BackupData,
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
        val file = try {
            json.decodeFromString(BackupFile.serializer(), text)
        } catch (e: Exception) {
            return BackupCheck.Bad("Dosya bozuk veya bu uygulamanın yedeği değil.")
        }
        if (file.format != BACKUP_FORMAT) return BackupCheck.Bad("Bu dosya bir YKS Pusula yedeği değil.")
        if (file.version > BACKUP_VERSION) return BackupCheck.Bad("Yedek daha yeni bir sürümle alınmış (sürüm ${file.version}). Önce uygulamayı güncelle.")
        if (sha(json.encodeToString(BackupData.serializer(), file.data)) != file.checksum) return BackupCheck.Bad("Yedek bütünlük denetiminden geçemedi; dosya değiştirilmiş veya bozulmuş.")
        return BackupCheck.Ok(file)
    }
}

class BackupManager(private val repo: Repository, private val filesDir: File) {
    private val u get() = repo.user

    suspend fun snapshot(): BackupData = withContext(Dispatchers.IO) {
        val s = repo.settings.first()
        BackupData(
            profile = u.profileNow(), examConfigs = u.examConfigsNow(), attempts = u.attemptsNow(), records = u.recordsNow(),
            plans = u.plansNow(), tasks = u.allTasks(), sessions = u.sessionsNow(), mocks = u.mocksNow(), results = u.resultsNow(),
            progress = u.progressAll(), wrongs = u.wrongsNow(), cardProgress = u.cardProgressNow(),
            userCards = u.userCardsNow().map { UserCardBackup(it.id, it.deck, it.subjectName, it.front, it.back, it.partOfSpeech, it.example) },
            notes = u.notesNow(), bookmarks = u.bookmarksNow(), reminders = u.remindersNow(),
            universities = u.universitiesNow(), departments = u.departmentsNow(),
            universityRecords = u.universityRecordsNow(),
            settings = mapOf(
                "theme" to s.theme, "fontScale" to s.fontScale.toString(), "pomodoroWork" to s.pomodoroWork.toString(),
                "pomodoroBreak" to s.pomodoroBreak.toString(), "weeklyGoalMinutes" to s.weeklyGoalMinutes.toString(),
                "monthlyGoalQuestions" to s.monthlyGoalQuestions.toString(), "diploma" to s.diploma, "placedLastYear" to s.placedLastYear.toString(),
            ),
        )
    }

    suspend fun export(appVersion: String): String = BackupCodec.encode(snapshot(), System.currentTimeMillis(), appVersion)

    /** Değiştirme öncesinde mevcut veriler cihaz içine güvenlik kopyası olarak yazılır. */
    suspend fun safetyCopy(appVersion: String): File = withContext(Dispatchers.IO) {
        val dir = filesDir.resolve("safety").apply { mkdirs() }
        dir.listFiles()?.sortedBy { it.lastModified() }?.dropLast(2)?.forEach { it.delete() }
        dir.resolve("once-yedek-${System.currentTimeMillis()}.json").apply { writeText(export(appVersion)) }
    }

    suspend fun restore(d: BackupData, replace: Boolean, appVersion: String) = withContext(Dispatchers.IO) {
        if (replace) safetyCopy(appVersion)
        val cards = d.userCards.map { Flashcard(it.id, it.deck, it.subjectName, it.front, it.back, it.partOfSpeech, it.example, true, "user") }
        repo.db.withTransaction {
            if (replace) {
                u.clearAttempts(); u.clearRecords(); u.clearPlans(); u.clearTasks(); u.clearSessions(); u.clearMocks()
                u.clearResults(); u.clearProgress(); u.clearWrongs(); u.clearCardProgress(); u.clearUserCards(); u.clearNotes()
                u.clearBookmarks(); u.clearReminders(); u.clearDepartments(); u.clearUniversities()
                u.clearUniversityRecords()
            }
            val current = u.profileNow()
            if (d.profile != null && (replace || current == null || !current.onboarded)) u.saveProfile(d.profile)
            if (replace) d.examConfigs.forEach { u.saveExamConfig(it) }
            u.attempts(d.attempts); u.mergeRecords(d.records); u.mergePlans(d.plans); u.mergeTasks(d.tasks)
            u.mergeSessions(d.sessions); u.mergeMocks(d.mocks); u.mergeResults(d.results); u.mergeProgress(d.progress)
            u.mergeWrongs(d.wrongs); u.mergeCardProgress(d.cardProgress); u.mergeCards(cards); u.mergeNotes(d.notes)
            u.mergeBookmarks(d.bookmarks); u.mergeReminders(d.reminders); u.mergeUniversities(d.universities)
            u.mergeDepartments(d.departments); u.mergeUniversityRecords(d.universityRecords)
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
