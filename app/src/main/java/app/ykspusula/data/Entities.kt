package app.ykspusula.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
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

@Entity(
    foreignKeys = [ForeignKey(Topic::class, ["id"], ["topicId"], ForeignKey.CASCADE)],
    indices = [Index("topicId"), Index("subjectId"), Index("exam"), Index("difficulty"), Index(value = ["contentHash"], unique = true)],
)
data class Question(
    @PrimaryKey val id: String,
    val exam: String,
    val subjectId: String,
    val topicId: String,
    val difficulty: Int,
    val stem: String,
    val figure: String?,
    val explanation: String,
    val steps: String,
    val examYear: Int,
    val curriculum: String,
    val source: String,
    val license: String,
    val generated: Boolean,
    val contentHash: String,
    val packId: String,
)

@Entity(
    primaryKeys = ["questionId", "label"],
    foreignKeys = [ForeignKey(Question::class, ["id"], ["questionId"], ForeignKey.CASCADE)],
)
data class AnswerOption(
    val questionId: String,
    val label: String,
    val text: String,
    val isCorrect: Boolean,
)

data class QuestionFull(
    @Embedded val q: Question,
    @Relation(parentColumn = "id", entityColumn = "questionId") val options: List<AnswerOption>,
) {
    val correctLabel: String get() = options.first { it.isCorrect }.label
}

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

@Serializable
@Entity(indices = [Index("questionId"), Index("topicId"), Index("subjectId"), Index("answeredAt")])
data class QuestionAttempt(
    @PrimaryKey val id: String = newId(),
    val questionId: String,
    val topicId: String,
    val subjectId: String,
    val selected: String?,
    val isCorrect: Boolean,
    val answeredAt: Long,
    val source: String,
    val mockExamId: String? = null,
)

@Serializable
@Entity(primaryKeys = ["mockExamId", "questionId"], indices = [Index("mockExamId")])
data class AnswerRecord(
    val mockExamId: String,
    val questionId: String,
    val orderIndex: Int,
    val sectionName: String,
    val selected: String? = null,
    val flagged: Boolean = false,
    val answeredAt: Long? = null,
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
)

@Serializable
@Entity(indices = [Index("nextReviewAt")])
data class WrongQuestion(
    @PrimaryKey val questionId: String,
    val topicId: String,
    val subjectId: String,
    val givenAnswer: String,
    val correctAnswer: String,
    val firstWrongAt: Long,
    val lastWrongAt: Long,
    val nextReviewAt: Long,
    val stage: Int = 0,
    val wrongCount: Int = 1,
    val reason: String = "",
    val note: String = "",
    val resolved: Boolean = false,
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
    val questionCount: Int,
    val total: Int,
    val right: Int,
)
data class WeakTopic(val topicId: String, val name: String, val subjectName: String, val total: Int, val right: Int)
data class WrongRow(
    @Embedded val w: WrongQuestion,
    val stem: String,
    val topicName: String,
    val subjectName: String,
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
