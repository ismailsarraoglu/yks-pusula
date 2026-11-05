package app.ykspusula.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

fun newId(): String = UUID.randomUUID().toString()

/* ---------- KULLANICI PROFİLİ VE SINAV AYARLARI ---------- */

@Serializable
@Entity
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val name: String = "",
    val grade: String = "12",
    val examYear: Int = 2027,
    val field: String = "SAY",
    val ydtLanguage: String = "İngilizce",
    val targetUniversity: String = "",
    val targetDepartment: String = "",
    val dailyMinutes: Int = 240,
    val studyDays: String = "1,2,3,4,5,6",
    val dailyQuestions: Int = 60,
    val strongSubjects: String = "",
    val weakSubjects: String = "",
    val startHour: Int = 9,
    val onboarded: Boolean = false,
    val maxDailyMinutes: Int = 360,
    val breakMinutes: Int = 10,
    val endHour: Int = 22,
    val priorityTopics: String = "",
)

@Serializable
@Entity
data class ExamConfiguration(
    @PrimaryKey val code: String,
    val title: String,
    val examDate: String,
    val examTime: String = "10:15",
    val questionCount: Int,
    val durationMin: Int,
    val wrongDivisor: Double = 4.0,
    val sectionsJson: String,
    val applicationStart: String = "",
    val applicationEnd: String = "",
    val resultDate: String = "",
    val sourceNote: String = "",
    val examYear: Int = 2027,
    val curriculum: String = "",
    val sourceUrl: String = "",
    val lastUpdated: String = "",
    val obpMultiplier: Double = 5.0,
    val obpCoefficient: Double = 0.12,
    val obpCoefficientPlaced: Double = 0.06,
)

/* ---------- BAŞLANGIÇ İÇERİĞİ ---------- */

@Entity(indices = [Index("exam")])
data class Subject(
    @PrimaryKey val id: String,
    val exam: String,
    val name: String,
    val groupName: String,
    val fields: String,
    val sortOrder: Int,
    val language: String? = null,
    val packId: String,
)

@Entity(
    foreignKeys = [ForeignKey(Subject::class, ["id"], ["subjectId"], ForeignKey.CASCADE)],
    indices = [Index("subjectId"), Index("parentId")],
)
data class Topic(
    @PrimaryKey val id: String,
    val subjectId: String,
    val parentId: String? = null,
    val name: String,
    val summary: String,
    val lesson: String,
    val keyPoints: String,
    val example: String,
    val curriculum: String,
    val validFromYear: Int,
    val validToYear: Int,
    val scope: String,
    val scopeNote: String,
    val estimatedMinutes: Int,
    val sortOrder: Int,
    val packId: String,
)

@Entity(indices = [Index("subjectId")])
data class Formula(
    @PrimaryKey val id: String,
    val subjectId: String,
    val subjectName: String,
    val category: String,
    val title: String,
    val body: String,
    val note: String,
    val packId: String,
)

@Entity(indices = [Index("deck")])
data class Flashcard(
    @PrimaryKey val id: String,
    val deck: String,
    val subjectName: String,
    val front: String,
    val back: String,
    val partOfSpeech: String = "",
    val example: String = "",
    val isUser: Boolean = false,
    val packId: String = "user",
)

@Serializable
@Entity
data class ImportedContentPack(
    @PrimaryKey val id: String,
    val title: String,
    val version: String,
    val kind: String,
    val importedAt: Long,
    val questionCount: Int,
    val source: String,
    val license: String,
)

/* ---------- ÖĞRENCİ VERİLERİ (yedeğe girer) ---------- */

/** Öğrencinin kendi kaynağından çözdüğü soruların kaydı. İstatistiklerin tek kaynağıdır. */
@Serializable
@Entity(
    foreignKeys = [ForeignKey(StudySource::class, ["id"], ["sourceId"], ForeignKey.SET_NULL)],
    indices = [Index("at"), Index("topicId"), Index("subjectId"), Index("sourceId"), Index("mockExamId"), Index("sessionId")],
)
data class PracticeLog(
    @PrimaryKey val id: String = newId(),
    val at: Long,
    val subjectId: String? = null,
    val topicId: String? = null,
    val sourceId: String? = null,
    val sessionId: String? = null,
    val mockExamId: String? = null,
    val pageFrom: Int? = null,
    val pageTo: Int? = null,
    val correct: Int = 0,
    val wrong: Int = 0,
    val blank: Int = 0,
    val origin: String = "MANUAL",
    val note: String = "",
    val createdAt: Long = at,
    val updatedAt: Long = at,
) {
    val solved: Int get() = correct + wrong + blank
}

/** Fiziksel kitap, PDF, video veya öğretmen dokümanı gibi öğrencinin kendi kaynağı. */
@Serializable
@Entity(indices = [Index("subjectId"), Index("topicId"), Index("libraryFileId")])
data class StudySource(
    @PrimaryKey val id: String = newId(),
    val name: String,
    val publisher: String = "",
    val type: String = "SORU_BANKASI",
    val subjectId: String? = null,
    val topicId: String? = null,
    val description: String = "",
    val totalPages: Int = 0,
    val startDate: String = "",
    val endDate: String = "",
    val lastPage: Int = 0,
    val pagesDone: Int = 0,
    val favorite: Boolean = false,
    val archived: Boolean = false,
    val note: String = "",
    val libraryFileId: String? = null,
    val lastUsedAt: Long = 0,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
@Entity
data class StudyPlan(
    @PrimaryKey val id: String = newId(),
    val title: String,
    val startDate: String,
    val endDate: String,
    val isAuto: Boolean,
    val createdAt: Long,
)

@Serializable
@Entity(indices = [Index("date"), Index("planId")])
data class StudyTask(
    @PrimaryKey val id: String = newId(),
    val planId: String? = null,
    val date: String,
    val time: String,
    val subjectId: String? = null,
    val topicId: String? = null,
    val title: String,
    val kind: String = "STUDY",
    val minutes: Int = 50,
    val questionGoal: Int = 0,
    val done: Boolean = false,
    val doneAt: Long? = null,
    val reason: String = "",
    val priority: Int = 2,
    val sourceId: String? = null,
    val originalDate: String = "",
    val movedCount: Int = 0,
    val skipped: Boolean = false,
    val updatedAt: Long = 0,
)

@Serializable
@Entity(indices = [Index("startedAt")])
data class StudySession(
    @PrimaryKey val id: String = newId(),
    val subjectId: String? = null,
    val topicId: String? = null,
    val label: String,
    val startedAt: Long,
    val minutes: Int,
    val kind: String = "POMODORO",
    val note: String = "",
    val sourceId: String? = null,
    val plannedMinutes: Int = 0,
    val activity: String = "",
)

@Serializable
@Entity(indices = [Index("status")])
data class MockExam(
    @PrimaryKey val id: String = newId(),
    val title: String,
    val kind: String,
    val exam: String,
    val createdAt: Long,
    val endsAt: Long,
    val durationSec: Int,
    val status: String,
    val isExternal: Boolean = false,
    val publisher: String = "",
    val examDate: String = "",
    val spentMinutes: Int = 0,
    val note: String = "",
    val pausedAt: Long = 0,
    val pausedTotalMs: Long = 0,
    val hardTopics: String = "",
    val subjectId: String? = null,
    val updatedAt: Long = 0,
)

@Serializable
@Entity(
    primaryKeys = ["mockExamId", "section"],
    indices = [Index("mockExamId")],
)
data class MockExamResult(
    val mockExamId: String,
    val section: String,
    val correct: Int,
    val wrong: Int,
    val blank: Int,
    val net: Double,
    val sortOrder: Int = 0,
)

@Serializable
@Entity
data class TopicProgress(
    @PrimaryKey val topicId: String,
    val status: Int = 0,
    val lastStudiedAt: Long? = null,
    val nextReviewAt: Long? = null,
    val completedAt: Long? = null,
    val reviewCount: Int = 0,
    val lastReviewAt: Long? = null,
    val checks: String = "",
)

/** Öğrencinin kendi kaynağında, PDF'inde veya denemesinde yaptığı yanlışın kaydı. */
@Serializable
@Entity(
    foreignKeys = [ForeignKey(StudySource::class, ["id"], ["sourceId"], ForeignKey.SET_NULL)],
    indices = [Index("nextReviewAt"), Index("topicId"), Index("subjectId"), Index("sourceId")],
)
data class WrongEntry(
    @PrimaryKey val id: String = newId(),
    val subjectId: String? = null,
    val topicId: String? = null,
    val sourceId: String? = null,
    val sourceLabel: String = "",
    val page: Int? = null,
    val questionNo: String = "",
    val reason: String = "",
    val note: String = "",
    val photoFile: String? = null,
    val questionText: String = "",
    val givenAnswer: String = "",
    val correctAnswer: String = "",
    val firstWrongAt: Long,
    val lastWrongAt: Long,
    val nextReviewAt: Long,
    val stage: Int = 0,
    val wrongCount: Int = 1,
    val resolved: Boolean = false,
    val origin: String = "MANUAL",
    val updatedAt: Long = firstWrongAt,
)

/** Tekrar geçmişi. Sonuç: 0 hatırlayamadım, 1 kısmen, 2 hatırladım. */
@Serializable
@Entity(
    foreignKeys = [ForeignKey(WrongEntry::class, ["id"], ["wrongId"], ForeignKey.CASCADE)],
    indices = [Index("wrongId"), Index("at")],
)
data class WrongReview(
    @PrimaryKey val id: String = newId(),
    val wrongId: String,
    val at: Long,
    val result: Int,
    val stageAfter: Int,
)

/** Pusula Koç'un bir güne ait önerisi. Öğrencinin kararı kalıcıdır. */
@Serializable
@Entity(indices = [Index("date")])
data class RouteItem(
    @PrimaryKey val id: String = newId(),
    val date: String,
    val orderIndex: Int,
    val kind: String,
    val title: String,
    val subjectId: String? = null,
    val topicId: String? = null,
    val sourceId: String? = null,
    val minutes: Int,
    val priority: Int,
    val reason: String,
    val status: String = "SUGGESTED",
    val movedTo: String = "",
    val createdAt: Long,
    val updatedAt: Long,
)

/** Plan değişmeden önceki görevlerin kopyası: eski plan kaybolmaz. */
@Serializable
@Entity
data class PlanRevision(
    @PrimaryKey val id: String = newId(),
    val createdAt: Long,
    val summary: String,
    val snapshotJson: String,
)

@Serializable
@Entity(indices = [Index("cardId"), Index("at")])
data class CardReview(
    @PrimaryKey val id: String = newId(),
    val cardId: String,
    val at: Long,
    val rating: Int,
)

/** Formül veya kart gibi yerleşik içeriğe eklenen kişisel not. */
@Serializable
@Entity(primaryKeys = ["kind", "refId"])
data class ItemNote(
    val kind: String,
    val refId: String,
    val text: String,
    val updatedAt: Long,
)

/** Testlere göre hedef net. */
@Serializable
@Entity(primaryKeys = ["exam", "section"])
data class NetGoal(
    val exam: String,
    val section: String,
    val target: Double,
)

/** Öğrencinin planlı dinlenme günü: çalışma serisini bozmaz. */
@Serializable
@Entity
data class RestDay(
    @PrimaryKey val date: String,
    val createdAt: Long,
)

@Serializable
@Entity
data class FlashcardProgress(
    @PrimaryKey val cardId: String,
    val stage: Int = 0,
    val nextReviewAt: Long = 0,
    val learned: Boolean = false,
    val lastRating: Int = -1,
    val reviewCount: Int = 0,
)

/** Kullanıcının kendi oluşturduğu kartlar: yedekte taşınır. */
@Serializable
data class UserCardBackup(
    val id: String, val deck: String, val subjectName: String, val front: String, val back: String,
    val partOfSpeech: String = "", val example: String = "",
)

@Serializable
@Entity
data class UserNote(
    @PrimaryKey val id: String = newId(),
    val title: String,
    val body: String,
    val subjectId: String? = null,
    val topicId: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val favorite: Boolean = false,
    val tags: String = "",
)

@Serializable
@Entity(primaryKeys = ["kind", "refId"])
data class Bookmark(
    val kind: String,
    val refId: String,
    val createdAt: Long,
)

@Serializable
@Entity
data class Reminder(
    @PrimaryKey val type: String,
    val enabled: Boolean = false,
    val hour: Int = 20,
    val minute: Int = 0,
    val dayOfWeek: Int = 7,
    val frequency: String = "",
)

@Serializable
@Entity
data class TargetUniversity(
    @PrimaryKey val id: String = newId(),
    val name: String,
    val city: String = "",
    val favorite: Boolean = false,
)

@Serializable
@Entity(
    foreignKeys = [ForeignKey(TargetUniversity::class, ["id"], ["universityId"], ForeignKey.CASCADE)],
    indices = [Index("universityId")],
)
data class TargetDepartment(
    @PrimaryKey val id: String = newId(),
    val universityId: String,
    val name: String,
    val scoreType: String = "SAY",
    val targetRank: Int? = null,
    val targetScore: Double? = null,
    val note: String = "",
    val favorite: Boolean = false,
    val targetTytNet: Double? = null,
    val targetAytNet: Double? = null,
    val dataSource: String = "",
    val dataYear: Int? = null,
    val updatedAt: Long = 0,
)

@Serializable
@Entity
data class LibraryFile(
    @PrimaryKey val id: String = newId(),
    val name: String,
    val fileName: String,
    val sizeBytes: Long,
    val addedAt: Long,
    val subjectName: String = "",
    val subjectId: String? = null,
    val topicId: String? = null,
    val lastOpenedAt: Long = 0,
    val lastPage: Int = 0,
    val pageCount: Int = 0,
    val favorite: Boolean = false,
    val note: String = "",
)

/** Kullanıcının içe aktardığı, kaynağı ve sürümü belli üniversite veri paketi satırı. */
@Serializable
@Entity(indices = [Index("university")])
data class UniversityRecord(
    @PrimaryKey val id: String = newId(),
    val packId: String,
    val university: String,
    val city: String = "",
    val department: String,
    val scoreType: String = "",
    val quota: Int? = null,
    val baseScore: Double? = null,
    val baseRank: Int? = null,
    val dataYear: Int? = null,
)

/* ---------- SORGU SONUÇLARI ---------- */

data class StatRow(val key: String, val total: Int, val right: Int)
data class DayCount(val day: String, val value: Int)
data class NamedCount(val name: String, val value: Int)
data class SubjectBoard(val subjectId: String, val total: Int, val done: Int, val studying: Int, val solving: Int)
data class TopicRow(
    val id: String,
    val subjectId: String,
    val name: String,
    val summary: String,
    val scope: String,
    val status: Int?,
    val nextReviewAt: Long?,
    val lastStudiedAt: Long?,
    val reviewCount: Int?,
    val checks: String?,
    val total: Int,
    val right: Int,
    val wrongOpen: Int,
    val minutes: Int,
)
data class WeakTopic(val topicId: String, val name: String, val subjectName: String, val total: Int, val right: Int)
data class WrongRow(
    @Embedded val w: WrongEntry,
    val topicName: String?,
    val subjectName: String?,
)
data class SourceRow(
    @Embedded val s: StudySource,
    val solved: Int,
    val correct: Int,
    val wrong: Int,
    val minutes: Int,
    val lastAt: Long?,
)
data class MockSummary(
    val id: String,
    val title: String,
    val exam: String,
    val kind: String,
    val createdAt: Long,
    val status: String,
    val isExternal: Boolean,
    val publisher: String,
    val examDate: String,
    val correct: Int,
    val wrong: Int,
    val blank: Int,
    val net: Double,
)
data class CardWithProgress(
    @Embedded val card: Flashcard,
    val stage: Int?,
    val nextReviewAt: Long?,
    val learned: Boolean?,
)
