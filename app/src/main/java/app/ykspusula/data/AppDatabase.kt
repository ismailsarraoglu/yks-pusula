package app.ykspusula.data

import android.content.Context
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Upsert
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow

@Dao
interface ContentDao {
    @Upsert suspend fun subjects(v: List<Subject>)
    @Upsert suspend fun topics(v: List<Topic>)
    @Upsert suspend fun questions(v: List<Question>)
    @Upsert suspend fun options(v: List<AnswerOption>)
    @Upsert suspend fun formulas(v: List<Formula>)
    @Upsert suspend fun flashcards(v: List<Flashcard>)
    @Upsert suspend fun pack(v: ImportedContentPack)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun examConfigsIfMissing(v: List<ExamConfiguration>)

    @Query("SELECT * FROM ImportedContentPack ORDER BY importedAt DESC") fun packs(): Flow<List<ImportedContentPack>>
    @Query("SELECT * FROM ImportedContentPack WHERE id = :id") suspend fun packById(id: String): ImportedContentPack?
    @Query("SELECT COUNT(*) FROM Question") fun questionCount(): Flow<Int>
    @Query("SELECT COUNT(*) FROM Question") suspend fun questionCountNow(): Int
    @Query("SELECT id FROM Question WHERE contentHash = :hash LIMIT 1") suspend fun idForHash(hash: String): String?
    @Query("SELECT id FROM Topic") suspend fun allTopicIds(): List<String>
    @Query("SELECT id FROM Subject") suspend fun allSubjectIds(): List<String>

    @Query("SELECT * FROM Subject ORDER BY sortOrder") fun subjects(): Flow<List<Subject>>
    @Query("SELECT * FROM Subject ORDER BY sortOrder") suspend fun subjectsNow(): List<Subject>
    @Query("SELECT * FROM Subject WHERE id = :id") suspend fun subject(id: String): Subject?
    @Query("SELECT * FROM Topic WHERE id = :id") fun topic(id: String): Flow<Topic?>
    @Query("SELECT * FROM Topic WHERE subjectId = :s ORDER BY sortOrder") suspend fun topicsOf(s: String): List<Topic>
    @Query("SELECT * FROM Topic ORDER BY sortOrder") suspend fun allTopics(): List<Topic>
    @Query("SELECT * FROM Question") suspend fun allQuestions(): List<Question>
    @Query("SELECT * FROM Formula ORDER BY subjectName, category, title") fun formulas(): Flow<List<Formula>>
    @Query("SELECT * FROM Formula") suspend fun formulasNow(): List<Formula>
    @Query("SELECT * FROM Flashcard") suspend fun flashcardsNow(): List<Flashcard>

    @Query(
        """SELECT t.id, t.subjectId, t.name, t.summary, t.scope, p.status, p.nextReviewAt, p.lastStudiedAt,
        (SELECT COUNT(*) FROM Question q WHERE q.topicId = t.id) AS questionCount,
        (SELECT COUNT(*) FROM QuestionAttempt a WHERE a.topicId = t.id AND a.selected IS NOT NULL) AS total,
        (SELECT COUNT(*) FROM QuestionAttempt a WHERE a.topicId = t.id AND a.isCorrect = 1) AS `right`
        FROM Topic t LEFT JOIN TopicProgress p ON p.topicId = t.id WHERE t.subjectId = :subjectId ORDER BY t.sortOrder"""
    )
    fun topicRows(subjectId: String): Flow<List<TopicRow>>

    @Query(
        """SELECT t.subjectId AS subjectId, COUNT(*) AS total,
        SUM(CASE WHEN p.status = 3 THEN 1 ELSE 0 END) AS done,
        SUM(CASE WHEN p.status = 1 THEN 1 ELSE 0 END) AS studying,
        SUM(CASE WHEN p.status = 2 THEN 1 ELSE 0 END) AS solving
        FROM Topic t LEFT JOIN TopicProgress p ON p.topicId = t.id WHERE t.scope != 'EXCLUDED' GROUP BY t.subjectId"""
    )
    fun subjectBoards(): Flow<List<SubjectBoard>>

    @RawQuery(observedEntities = [Question::class, QuestionAttempt::class, WrongQuestion::class, Bookmark::class])
    fun pageQuestions(q: SupportSQLiteQuery): PagingSource<Int, Question>

    @RawQuery suspend fun questionIds(q: SupportSQLiteQuery): List<String>

    @Transaction @Query("SELECT * FROM Question WHERE id = :id") suspend fun full(id: String): QuestionFull?
    @Transaction @Query("SELECT * FROM Question WHERE id IN (:ids)") suspend fun fullList(ids: List<String>): List<QuestionFull>
    @Query("SELECT id FROM Question WHERE subjectId = :subjectId") suspend fun idsOfSubject(subjectId: String): List<String>
    @Query("SELECT id FROM Question WHERE topicId IN (:topicIds)") suspend fun idsOfTopics(topicIds: List<String>): List<String>
    @Query("SELECT subjectId AS `key`, COUNT(*) AS total, 0 AS `right` FROM Question GROUP BY subjectId") suspend fun bankBySubject(): List<StatRow>
    @Query("SELECT exam AS `key`, COUNT(*) AS total, 0 AS `right` FROM Question GROUP BY exam") fun bankByExam(): Flow<List<StatRow>>
    @Query("SELECT COUNT(*) FROM Topic WHERE lesson != ''") suspend fun topicsWithLesson(): Int
}

@Dao
interface UserDao {
    /* Profil */
    @Query("SELECT * FROM UserProfile WHERE id = 1") fun profile(): Flow<UserProfile?>
    @Query("SELECT * FROM UserProfile WHERE id = 1") suspend fun profileNow(): UserProfile?
    @Upsert suspend fun saveProfile(p: UserProfile)

    /* Sınav ayarları */
    @Query("SELECT * FROM ExamConfiguration") fun examConfigs(): Flow<List<ExamConfiguration>>
    @Query("SELECT * FROM ExamConfiguration") suspend fun examConfigsNow(): List<ExamConfiguration>
    @Query("SELECT * FROM ExamConfiguration WHERE code = :code") suspend fun examConfig(code: String): ExamConfiguration?
    @Upsert suspend fun saveExamConfig(c: ExamConfiguration)

    /* Soru denemeleri */
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun attempt(a: QuestionAttempt)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun attempts(a: List<QuestionAttempt>)
    @Query("SELECT * FROM QuestionAttempt") suspend fun attemptsNow(): List<QuestionAttempt>
    @Query("SELECT DISTINCT questionId FROM QuestionAttempt WHERE source != 'MOCK'") fun solvedIds(): Flow<List<String>>
    @Query("SELECT COUNT(*) FROM QuestionAttempt WHERE selected IS NOT NULL AND answeredAt >= :from AND answeredAt < :to")
    fun solvedBetween(from: Long, to: Long): Flow<Int>
    @Query("SELECT COUNT(*) FROM QuestionAttempt WHERE selected IS NOT NULL") fun solvedTotal(): Flow<Int>
    @Query("SELECT COUNT(*) AS c FROM QuestionAttempt WHERE selected IS NOT NULL AND answeredAt >= :from AND answeredAt < :to")
    suspend fun solvedBetweenNow(from: Long, to: Long): Int
    @Query("SELECT COUNT(*) FROM QuestionAttempt WHERE isCorrect = 1 AND answeredAt >= :from AND answeredAt < :to")
    suspend fun rightBetweenNow(from: Long, to: Long): Int

    @Query(
        """SELECT a.topicId AS topicId, t.name AS name, s.name AS subjectName, COUNT(*) AS total, SUM(a.isCorrect) AS `right`
        FROM QuestionAttempt a JOIN Topic t ON t.id = a.topicId JOIN Subject s ON s.id = t.subjectId
        WHERE a.selected IS NOT NULL GROUP BY a.topicId HAVING COUNT(*) >= :minCount
        ORDER BY (CAST(SUM(a.isCorrect) AS REAL) / COUNT(*)) ASC, COUNT(*) DESC LIMIT :limit"""
    )
    fun weakTopics(minCount: Int, limit: Int): Flow<List<WeakTopic>>

    @Query(
        """SELECT s.name AS `key`, COUNT(*) AS total, SUM(a.isCorrect) AS `right`
        FROM QuestionAttempt a JOIN Subject s ON s.id = a.subjectId WHERE a.selected IS NOT NULL
        GROUP BY a.subjectId ORDER BY s.sortOrder"""
    )
    fun subjectStats(): Flow<List<StatRow>>

    @Query(
        """SELECT a.subjectId AS `key`, COUNT(*) AS total, SUM(a.isCorrect) AS `right`
        FROM QuestionAttempt a WHERE a.selected IS NOT NULL AND a.answeredAt >= :from GROUP BY a.subjectId"""
    )
    suspend fun subjectStatsSince(from: Long): List<StatRow>

    @Query("SELECT topicId AS `key`, COUNT(*) AS total, SUM(isCorrect) AS `right` FROM QuestionAttempt WHERE selected IS NOT NULL GROUP BY topicId")
    suspend fun topicStatsNow(): List<StatRow>

    @Query(
        """SELECT date(answeredAt / 1000, 'unixepoch', 'localtime') AS day, COUNT(*) AS value FROM QuestionAttempt
        WHERE selected IS NOT NULL AND answeredAt >= :from GROUP BY day ORDER BY day"""
    )
    fun questionsPerDay(from: Long): Flow<List<DayCount>>

    @Query(
        """SELECT date(startedAt / 1000, 'unixepoch', 'localtime') AS day, SUM(minutes) AS value FROM StudySession
        WHERE startedAt >= :from GROUP BY day ORDER BY day"""
    )
    fun minutesPerDay(from: Long): Flow<List<DayCount>>

    @Query(
        """SELECT DISTINCT day FROM (
           SELECT date(answeredAt / 1000, 'unixepoch', 'localtime') AS day FROM QuestionAttempt WHERE selected IS NOT NULL
           UNION SELECT date(startedAt / 1000, 'unixepoch', 'localtime') AS day FROM StudySession
           UNION SELECT date(doneAt / 1000, 'unixepoch', 'localtime') AS day FROM StudyTask WHERE done = 1 AND doneAt IS NOT NULL
        ) ORDER BY day"""
    )
    fun activeDays(): Flow<List<String>>

    /* Görevler ve planlar */
    @Query("SELECT * FROM StudyTask WHERE date = :date ORDER BY time, title") fun tasksOn(date: String): Flow<List<StudyTask>>
    @Query("SELECT * FROM StudyTask WHERE date >= :from AND date <= :to ORDER BY date, time") fun tasksBetween(from: String, to: String): Flow<List<StudyTask>>
    @Query("SELECT COUNT(*) FROM StudyTask WHERE done = 1") fun doneTaskCount(): Flow<Int>
    @Upsert suspend fun saveTask(t: StudyTask)
    @Upsert suspend fun saveTasks(t: List<StudyTask>)
    @Delete suspend fun deleteTask(t: StudyTask)
    @Query("DELETE FROM StudyTask WHERE planId IS NOT NULL AND done = 0 AND date >= :from") suspend fun clearPendingAuto(from: String)
    @Upsert suspend fun savePlan(p: StudyPlan)
    @Query("SELECT * FROM StudyPlan ORDER BY createdAt DESC") suspend fun plansNow(): List<StudyPlan>
    @Query("SELECT * FROM StudyTask") suspend fun allTasks(): List<StudyTask>

    /* Oturumlar */
    @Upsert suspend fun saveSession(s: StudySession)
    @Query("SELECT * FROM StudySession ORDER BY startedAt DESC LIMIT :limit") fun recentSessions(limit: Int): Flow<List<StudySession>>
    @Query("SELECT * FROM StudySession") suspend fun sessionsNow(): List<StudySession>
    @Query("SELECT COALESCE(SUM(minutes), 0) FROM StudySession WHERE startedAt >= :from AND startedAt < :to") fun minutesBetween(from: Long, to: Long): Flow<Int>
    @Query("SELECT COALESCE(SUM(minutes), 0) FROM StudySession") fun minutesTotal(): Flow<Int>
    @Query("SELECT COALESCE(SUM(minutes), 0) FROM StudySession WHERE startedAt >= :from AND startedAt < :to") suspend fun minutesBetweenNow(from: Long, to: Long): Int

    /* Konu ilerlemesi */
    @Query("SELECT * FROM TopicProgress WHERE topicId = :id") fun progress(id: String): Flow<TopicProgress?>
    @Query("SELECT * FROM TopicProgress WHERE topicId = :id") suspend fun progressNow(id: String): TopicProgress?
    @Query("SELECT * FROM TopicProgress") suspend fun progressAll(): List<TopicProgress>
    @Upsert suspend fun saveProgress(p: TopicProgress)
    @Query("SELECT COUNT(*) FROM TopicProgress WHERE status = 3") fun completedTopics(): Flow<Int>
    @Query("SELECT COUNT(*) FROM TopicProgress WHERE completedAt >= :from AND completedAt < :to") fun completedBetween(from: Long, to: Long): Flow<Int>
    @Query("SELECT COUNT(*) FROM TopicProgress WHERE nextReviewAt IS NOT NULL AND nextReviewAt <= :now") fun topicsDue(now: Long): Flow<Int>

    /* Yanlışlar */
    @Query("SELECT * FROM WrongQuestion WHERE questionId = :id") suspend fun wrongOf(id: String): WrongQuestion?
    @Upsert suspend fun saveWrong(w: WrongQuestion)
    @Query("DELETE FROM WrongQuestion WHERE questionId = :id") suspend fun deleteWrong(id: String)
    @Query("SELECT * FROM WrongQuestion") suspend fun wrongsNow(): List<WrongQuestion>
    @Query(
        """SELECT w.*, q.stem AS stem, t.name AS topicName, s.name AS subjectName FROM WrongQuestion w
        JOIN Question q ON q.id = w.questionId JOIN Topic t ON t.id = w.topicId JOIN Subject s ON s.id = w.subjectId
        ORDER BY w.resolved ASC, w.nextReviewAt ASC"""
    )
    fun wrongRows(): Flow<List<WrongRow>>
    @Query("SELECT COUNT(*) FROM WrongQuestion WHERE resolved = 0 AND nextReviewAt <= :now") fun wrongDue(now: Long): Flow<Int>
    @Query("SELECT COUNT(*) FROM WrongQuestion WHERE resolved = 0 AND nextReviewAt <= :now") suspend fun wrongDueNow(now: Long): Int
    @Query("SELECT questionId FROM WrongQuestion WHERE resolved = 0 AND nextReviewAt <= :now ORDER BY wrongCount DESC, nextReviewAt ASC")
    suspend fun wrongDueIds(now: Long): List<String>
    @Query("SELECT reason AS name, COUNT(*) AS value FROM WrongQuestion WHERE reason != '' GROUP BY reason ORDER BY value DESC") fun wrongReasons(): Flow<List<NamedCount>>

    /* Denemeler */
    @Upsert suspend fun saveMock(m: MockExam)
    @Upsert suspend fun saveRecords(r: List<AnswerRecord>)
    @Upsert suspend fun saveRecord(r: AnswerRecord)
    @Upsert suspend fun saveResults(r: List<MockExamResult>)
    @Query("SELECT * FROM MockExam WHERE id = :id") suspend fun mock(id: String): MockExam?
    @Query("SELECT * FROM MockExam WHERE status = 'RUNNING' ORDER BY createdAt DESC LIMIT 1") fun running(): Flow<MockExam?>
    @Query("SELECT * FROM MockExam WHERE status = 'RUNNING'") suspend fun runningNow(): List<MockExam>
    @Query("SELECT * FROM AnswerRecord WHERE mockExamId = :id ORDER BY orderIndex") suspend fun records(id: String): List<AnswerRecord>
    @Query("SELECT * FROM MockExamResult WHERE mockExamId = :id ORDER BY sortOrder") suspend fun results(id: String): List<MockExamResult>
    @Query("DELETE FROM MockExam WHERE id = :id") suspend fun deleteMock(id: String)
    @Query("DELETE FROM MockExamResult WHERE mockExamId = :id") suspend fun deleteResults(id: String)
    @Query("DELETE FROM AnswerRecord WHERE mockExamId = :id") suspend fun deleteRecords(id: String)
    @Query("DELETE FROM QuestionAttempt WHERE mockExamId = :id") suspend fun deleteMockAttempts(id: String)
    @Query("SELECT * FROM MockExam") suspend fun mocksNow(): List<MockExam>
    @Query("SELECT * FROM MockExamResult") suspend fun resultsNow(): List<MockExamResult>
    @Query("SELECT * FROM AnswerRecord") suspend fun recordsNow(): List<AnswerRecord>
    @Query(
        """SELECT m.id, m.title, m.exam, m.kind, m.createdAt, m.status, m.isExternal, m.publisher, m.examDate,
        COALESCE(SUM(r.correct),0) AS correct, COALESCE(SUM(r.wrong),0) AS wrong, COALESCE(SUM(r.blank),0) AS blank, COALESCE(SUM(r.net),0) AS net
        FROM MockExam m LEFT JOIN MockExamResult r ON r.mockExamId = m.id WHERE m.status = 'DONE'
        GROUP BY m.id ORDER BY m.createdAt ASC"""
    )
    fun mockSummaries(): Flow<List<MockSummary>>
    @Query(
        """SELECT m.id, m.title, m.exam, m.kind, m.createdAt, m.status, m.isExternal, m.publisher, m.examDate,
        COALESCE(SUM(r.correct),0) AS correct, COALESCE(SUM(r.wrong),0) AS wrong, COALESCE(SUM(r.blank),0) AS blank, COALESCE(SUM(r.net),0) AS net
        FROM MockExam m LEFT JOIN MockExamResult r ON r.mockExamId = m.id WHERE m.status = 'DONE'
        GROUP BY m.id ORDER BY m.createdAt ASC"""
    )
    suspend fun mockSummariesNow(): List<MockSummary>

    /* Kartlar */
    @Query(
        """SELECT c.*, p.stage AS stage, p.nextReviewAt AS nextReviewAt, p.learned AS learned FROM Flashcard c
        LEFT JOIN FlashcardProgress p ON p.cardId = c.id WHERE c.deck = :deck ORDER BY c.subjectName, c.front"""
    )
    fun cardsOf(deck: String): Flow<List<CardWithProgress>>
    @Query(
        """SELECT c.*, p.stage AS stage, p.nextReviewAt AS nextReviewAt, p.learned AS learned FROM Flashcard c
        LEFT JOIN FlashcardProgress p ON p.cardId = c.id
        WHERE (c.deck = :deck OR :deck = '') AND (p.nextReviewAt IS NULL OR p.nextReviewAt <= :now)
        ORDER BY p.nextReviewAt IS NULL, p.nextReviewAt LIMIT :limit"""
    )
    suspend fun cardsDue(deck: String, now: Long, limit: Int): List<CardWithProgress>
    @Query("SELECT deck AS name, COUNT(*) AS value FROM Flashcard GROUP BY deck ORDER BY deck") fun decks(): Flow<List<NamedCount>>
    @Query("SELECT COUNT(*) FROM Flashcard c LEFT JOIN FlashcardProgress p ON p.cardId = c.id WHERE p.nextReviewAt IS NOT NULL AND p.nextReviewAt <= :now")
    fun cardsDueCount(now: Long): Flow<Int>
    @Query("SELECT COUNT(*) FROM Flashcard c LEFT JOIN FlashcardProgress p ON p.cardId = c.id WHERE p.nextReviewAt IS NOT NULL AND p.nextReviewAt <= :now")
    suspend fun cardsDueCountNow(now: Long): Int
    @Query("SELECT * FROM FlashcardProgress WHERE cardId = :id") suspend fun cardProgress(id: String): FlashcardProgress?
    @Upsert suspend fun saveCardProgress(p: FlashcardProgress)
    @Upsert suspend fun saveCard(c: Flashcard)
    @Query("DELETE FROM Flashcard WHERE id = :id AND isUser = 1") suspend fun deleteUserCard(id: String)
    @Query("SELECT * FROM FlashcardProgress") suspend fun cardProgressNow(): List<FlashcardProgress>
    @Query("SELECT * FROM Flashcard WHERE isUser = 1") suspend fun userCardsNow(): List<Flashcard>
    @Query("SELECT COUNT(*) FROM FlashcardProgress WHERE learned = 1") fun learnedCards(): Flow<Int>

    /* Notlar */
    @Query("SELECT * FROM UserNote ORDER BY favorite DESC, updatedAt DESC") fun notes(): Flow<List<UserNote>>
    @Query("SELECT * FROM UserNote") suspend fun notesNow(): List<UserNote>
    @Query("SELECT * FROM UserNote WHERE id = :id") suspend fun note(id: String): UserNote?
    @Upsert suspend fun saveNote(n: UserNote)
    @Query("DELETE FROM UserNote WHERE id = :id") suspend fun deleteNote(id: String)

    /* İşaretler */
    @Query("SELECT refId FROM Bookmark WHERE kind = :kind") fun bookmarkIds(kind: String): Flow<List<String>>
    @Query("SELECT * FROM Bookmark") suspend fun bookmarksNow(): List<Bookmark>
    @Upsert suspend fun saveBookmark(b: Bookmark)
    @Query("DELETE FROM Bookmark WHERE kind = :kind AND refId = :refId") suspend fun deleteBookmark(kind: String, refId: String)

    /* Hatırlatıcılar */
    @Query("SELECT * FROM Reminder") fun reminders(): Flow<List<Reminder>>
    @Query("SELECT * FROM Reminder") suspend fun remindersNow(): List<Reminder>
    @Upsert suspend fun saveReminder(r: Reminder)

    /* Hedefler */
    @Query("SELECT * FROM TargetUniversity ORDER BY favorite DESC, name") fun universities(): Flow<List<TargetUniversity>>
    @Query("SELECT * FROM TargetDepartment ORDER BY favorite DESC, name") fun departments(): Flow<List<TargetDepartment>>
    @Query("SELECT * FROM TargetUniversity") suspend fun universitiesNow(): List<TargetUniversity>
    @Query("SELECT * FROM TargetDepartment") suspend fun departmentsNow(): List<TargetDepartment>
    @Upsert suspend fun saveUniversity(u: TargetUniversity)
    @Upsert suspend fun saveDepartment(d: TargetDepartment)
    @Delete suspend fun deleteUniversity(u: TargetUniversity)
    @Delete suspend fun deleteDepartment(d: TargetDepartment)

    /* Kütüphane */
    @Query("SELECT * FROM LibraryFile ORDER BY addedAt DESC") fun library(): Flow<List<LibraryFile>>
    @Query("SELECT * FROM LibraryFile") suspend fun libraryNow(): List<LibraryFile>
    @Upsert suspend fun saveLibrary(f: LibraryFile)
    @Delete suspend fun deleteLibrary(f: LibraryFile)

    /* Üniversite veri paketi */
    @Upsert suspend fun saveUniversityRecords(r: List<UniversityRecord>)
    @Query("SELECT * FROM UniversityRecord ORDER BY university, department LIMIT 500") fun universityRecords(): Flow<List<UniversityRecord>>
    @Query("SELECT * FROM UniversityRecord") suspend fun universityRecordsNow(): List<UniversityRecord>
    @Query("DELETE FROM UniversityRecord WHERE packId = :packId") suspend fun deleteUniversityPack(packId: String)

    /* Toplu temizlik */
    @Query("DELETE FROM QuestionAttempt") suspend fun clearAttempts()
    @Query("DELETE FROM AnswerRecord") suspend fun clearRecords()
    @Query("DELETE FROM StudyPlan") suspend fun clearPlans()
    @Query("DELETE FROM StudyTask") suspend fun clearTasks()
    @Query("DELETE FROM StudySession") suspend fun clearSessions()
    @Query("DELETE FROM MockExam") suspend fun clearMocks()
    @Query("DELETE FROM MockExamResult") suspend fun clearResults()
    @Query("DELETE FROM TopicProgress") suspend fun clearProgress()
    @Query("DELETE FROM WrongQuestion") suspend fun clearWrongs()
    @Query("DELETE FROM FlashcardProgress") suspend fun clearCardProgress()
    @Query("DELETE FROM Flashcard WHERE isUser = 1") suspend fun clearUserCards()
    @Query("DELETE FROM UserNote") suspend fun clearNotes()
    @Query("DELETE FROM Bookmark") suspend fun clearBookmarks()
    @Query("DELETE FROM Reminder") suspend fun clearReminders()
    @Query("DELETE FROM TargetDepartment") suspend fun clearDepartments()
    @Query("DELETE FROM TargetUniversity") suspend fun clearUniversities()
    @Query("DELETE FROM LibraryFile") suspend fun clearLibrary()
    @Query("DELETE FROM UniversityRecord") suspend fun clearUniversityRecords()
    @Query("DELETE FROM UserProfile") suspend fun clearProfile()

    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun mergeRecords(v: List<AnswerRecord>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun mergePlans(v: List<StudyPlan>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun mergeTasks(v: List<StudyTask>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun mergeSessions(v: List<StudySession>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun mergeMocks(v: List<MockExam>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun mergeResults(v: List<MockExamResult>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun mergeProgress(v: List<TopicProgress>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun mergeWrongs(v: List<WrongQuestion>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun mergeCardProgress(v: List<FlashcardProgress>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun mergeCards(v: List<Flashcard>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun mergeNotes(v: List<UserNote>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun mergeBookmarks(v: List<Bookmark>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun mergeReminders(v: List<Reminder>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun mergeUniversities(v: List<TargetUniversity>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun mergeDepartments(v: List<TargetDepartment>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun mergeLibrary(v: List<LibraryFile>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun mergeUniversityRecords(v: List<UniversityRecord>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun mergeExamConfigs(v: List<ExamConfiguration>)
}

@Database(
    version = 1,
    exportSchema = true,
    entities = [
        UserProfile::class, ExamConfiguration::class, Subject::class, Topic::class, Question::class, AnswerOption::class,
        Formula::class, Flashcard::class, ImportedContentPack::class, QuestionAttempt::class, AnswerRecord::class,
        StudyPlan::class, StudyTask::class, StudySession::class, MockExam::class, MockExamResult::class, TopicProgress::class,
        WrongQuestion::class, FlashcardProgress::class, UserNote::class, Bookmark::class, Reminder::class,
        TargetUniversity::class, TargetDepartment::class, LibraryFile::class, UniversityRecord::class,
    ],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun content(): ContentDao
    abstract fun user(): UserDao

    companion object {
        /** Yeni şema sürümlerinde buraya Migration eklenir; yıkıcı geçiş hiçbir zaman açılmaz. */
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "yks_pusula.db").build()
    }
}

fun rawQuestionQuery(sql: String, args: List<Any>) = SimpleSQLiteQuery(sql, args.toTypedArray())
