package app.ykspusula.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ContentDao {
    @Upsert suspend fun subjects(v: List<Subject>)
    @Upsert suspend fun topics(v: List<Topic>)
    @Upsert suspend fun formulas(v: List<Formula>)
    @Upsert suspend fun flashcards(v: List<Flashcard>)
    @Upsert suspend fun pack(v: ImportedContentPack)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun examConfigsIfMissing(v: List<ExamConfiguration>)

    @Query("SELECT * FROM ImportedContentPack ORDER BY importedAt DESC") fun packs(): Flow<List<ImportedContentPack>>
    @Query("SELECT * FROM ImportedContentPack WHERE id = :id") suspend fun packById(id: String): ImportedContentPack?
    @Query("SELECT COUNT(*) FROM Topic") suspend fun topicCountNow(): Int
    @Query("SELECT COUNT(*) FROM Formula") suspend fun formulaCountNow(): Int
    @Query("SELECT COUNT(*) FROM Flashcard WHERE isUser = 0") suspend fun cardCountNow(): Int
    @Query("SELECT id FROM Topic") suspend fun allTopicIds(): List<String>
    @Query("SELECT id FROM Subject") suspend fun allSubjectIds(): List<String>

    @Query("SELECT * FROM Subject ORDER BY sortOrder") fun subjects(): Flow<List<Subject>>
    @Query("SELECT * FROM Subject ORDER BY sortOrder") suspend fun subjectsNow(): List<Subject>
    @Query("SELECT * FROM Subject WHERE id = :id") suspend fun subject(id: String): Subject?
    @Query("SELECT * FROM Topic WHERE id = :id") fun topic(id: String): Flow<Topic?>
    @Query("SELECT subjectId FROM Topic WHERE id = :id") suspend fun subjectOfTopic(id: String): String?
    @Query("SELECT * FROM Topic WHERE subjectId = :s ORDER BY sortOrder") suspend fun topicsOf(s: String): List<Topic>
    @Query("SELECT * FROM Topic ORDER BY sortOrder") suspend fun allTopics(): List<Topic>
    @Query("SELECT * FROM Formula ORDER BY subjectName, category, title") fun formulas(): Flow<List<Formula>>
    @Query("SELECT * FROM Formula") suspend fun formulasNow(): List<Formula>
    @Query("SELECT * FROM Flashcard") suspend fun flashcardsNow(): List<Flashcard>

    @Query(
        """SELECT t.id, t.subjectId, t.name, t.summary, t.scope, p.status, p.nextReviewAt, p.lastStudiedAt, p.reviewCount, p.checks,
        COALESCE((SELECT SUM(l.correct + l.wrong + l.blank) FROM PracticeLog l WHERE l.topicId = t.id), 0) AS total,
        COALESCE((SELECT SUM(l.correct) FROM PracticeLog l WHERE l.topicId = t.id), 0) AS `right`,
        (SELECT COUNT(*) FROM WrongEntry w WHERE w.topicId = t.id AND w.resolved = 0) AS wrongOpen,
        COALESCE((SELECT SUM(x.minutes) FROM StudySession x WHERE x.topicId = t.id), 0) AS minutes
        FROM Topic t LEFT JOIN TopicProgress p ON p.topicId = t.id WHERE t.subjectId = :subjectId ORDER BY t.sortOrder"""
    )
    fun topicRows(subjectId: String): Flow<List<TopicRow>>

    @Query(
        """SELECT t.id, t.subjectId, t.name, t.summary, t.scope, p.status, p.nextReviewAt, p.lastStudiedAt, p.reviewCount, p.checks,
        COALESCE((SELECT SUM(l.correct + l.wrong + l.blank) FROM PracticeLog l WHERE l.topicId = t.id), 0) AS total,
        COALESCE((SELECT SUM(l.correct) FROM PracticeLog l WHERE l.topicId = t.id), 0) AS `right`,
        (SELECT COUNT(*) FROM WrongEntry w WHERE w.topicId = t.id AND w.resolved = 0) AS wrongOpen,
        COALESCE((SELECT SUM(x.minutes) FROM StudySession x WHERE x.topicId = t.id), 0) AS minutes
        FROM Topic t LEFT JOIN TopicProgress p ON p.topicId = t.id ORDER BY t.sortOrder"""
    )
    fun allTopicRows(): Flow<List<TopicRow>>

    @Query(
        """SELECT t.id, t.subjectId, t.name, t.summary, t.scope, p.status, p.nextReviewAt, p.lastStudiedAt, p.reviewCount, p.checks,
        COALESCE((SELECT SUM(l.correct + l.wrong + l.blank) FROM PracticeLog l WHERE l.topicId = t.id), 0) AS total,
        COALESCE((SELECT SUM(l.correct) FROM PracticeLog l WHERE l.topicId = t.id), 0) AS `right`,
        (SELECT COUNT(*) FROM WrongEntry w WHERE w.topicId = t.id AND w.resolved = 0) AS wrongOpen,
        COALESCE((SELECT SUM(x.minutes) FROM StudySession x WHERE x.topicId = t.id), 0) AS minutes
        FROM Topic t LEFT JOIN TopicProgress p ON p.topicId = t.id ORDER BY t.sortOrder"""
    )
    suspend fun allTopicRowsNow(): List<TopicRow>

    @Query(
        """SELECT t.subjectId AS subjectId, COUNT(*) AS total,
        SUM(CASE WHEN p.status = 3 THEN 1 ELSE 0 END) AS done,
        SUM(CASE WHEN p.status = 1 THEN 1 ELSE 0 END) AS studying,
        SUM(CASE WHEN p.status = 2 THEN 1 ELSE 0 END) AS solving
        FROM Topic t LEFT JOIN TopicProgress p ON p.topicId = t.id WHERE t.scope != 'EXCLUDED' GROUP BY t.subjectId"""
    )
    fun subjectBoards(): Flow<List<SubjectBoard>>

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

    /* Soru performansı */
    @Upsert suspend fun saveLog(l: PracticeLog)
    @Upsert suspend fun saveLogs(l: List<PracticeLog>)
    @Query("DELETE FROM PracticeLog WHERE id = :id") suspend fun deleteLog(id: String)
    @Query("DELETE FROM PracticeLog WHERE sessionId = :id") suspend fun deleteLogsOfSession(id: String)
    @Query("SELECT * FROM PracticeLog") suspend fun logsNow(): List<PracticeLog>
    @Query("SELECT * FROM PracticeLog ORDER BY at DESC LIMIT :limit") fun recentLogs(limit: Int): Flow<List<PracticeLog>>
    @Query("SELECT * FROM PracticeLog WHERE sourceId = :id ORDER BY at DESC") fun logsOfSource(id: String): Flow<List<PracticeLog>>
    @Query("SELECT * FROM PracticeLog WHERE topicId = :id ORDER BY at DESC") fun logsOfTopic(id: String): Flow<List<PracticeLog>>
    @Query("SELECT * FROM PracticeLog WHERE at >= :from AND at < :to") suspend fun logsBetween(from: Long, to: Long): List<PracticeLog>
    @Query("SELECT COALESCE(SUM(correct + wrong + blank), 0) FROM PracticeLog WHERE at >= :from AND at < :to") fun solvedBetween(from: Long, to: Long): Flow<Int>
    @Query("SELECT COALESCE(SUM(correct + wrong + blank), 0) FROM PracticeLog") fun solvedTotal(): Flow<Int>
    @Query("SELECT COALESCE(SUM(correct + wrong + blank), 0) FROM PracticeLog WHERE at >= :from AND at < :to") suspend fun solvedBetweenNow(from: Long, to: Long): Int
    @Query("SELECT COALESCE(SUM(correct), 0) FROM PracticeLog WHERE at >= :from AND at < :to") suspend fun rightBetweenNow(from: Long, to: Long): Int

    @Query(
        """SELECT l.topicId AS topicId, t.name AS name, s.name AS subjectName, SUM(l.correct + l.wrong + l.blank) AS total, SUM(l.correct) AS `right`
        FROM PracticeLog l JOIN Topic t ON t.id = l.topicId JOIN Subject s ON s.id = t.subjectId
        GROUP BY l.topicId HAVING SUM(l.correct + l.wrong + l.blank) >= :minCount
        ORDER BY (CAST(SUM(l.correct) AS REAL) / SUM(l.correct + l.wrong + l.blank)) ASC, total DESC LIMIT :limit"""
    )
    fun weakTopics(minCount: Int, limit: Int): Flow<List<WeakTopic>>

    @Query(
        """SELECT s.name AS `key`, SUM(l.correct + l.wrong + l.blank) AS total, SUM(l.correct) AS `right`
        FROM PracticeLog l JOIN Subject s ON s.id = l.subjectId WHERE l.origin != 'MOCK'
        GROUP BY l.subjectId ORDER BY s.sortOrder"""
    )
    fun subjectStats(): Flow<List<StatRow>>

    @Query(
        """SELECT l.subjectId AS `key`, SUM(l.correct + l.wrong + l.blank) AS total, SUM(l.correct) AS `right`
        FROM PracticeLog l WHERE l.subjectId IS NOT NULL AND l.at >= :from GROUP BY l.subjectId"""
    )
    suspend fun subjectStatsSince(from: Long): List<StatRow>

    @Query("SELECT topicId AS `key`, SUM(correct + wrong + blank) AS total, SUM(correct) AS `right` FROM PracticeLog WHERE topicId IS NOT NULL GROUP BY topicId")
    suspend fun topicStatsNow(): List<StatRow>

    @Query(
        """SELECT date(at / 1000, 'unixepoch', 'localtime') AS day, SUM(correct + wrong + blank) AS value FROM PracticeLog
        WHERE at >= :from GROUP BY day ORDER BY day"""
    )
    fun questionsPerDay(from: Long): Flow<List<DayCount>>

    @Query(
        """SELECT date(startedAt / 1000, 'unixepoch', 'localtime') AS day, SUM(minutes) AS value FROM StudySession
        WHERE startedAt >= :from GROUP BY day ORDER BY day"""
    )
    fun minutesPerDay(from: Long): Flow<List<DayCount>>

    @Query(
        """SELECT DISTINCT day FROM (
           SELECT date(at / 1000, 'unixepoch', 'localtime') AS day FROM PracticeLog
           UNION SELECT date(startedAt / 1000, 'unixepoch', 'localtime') AS day FROM StudySession
           UNION SELECT date(doneAt / 1000, 'unixepoch', 'localtime') AS day FROM StudyTask WHERE done = 1 AND doneAt IS NOT NULL
        ) ORDER BY day"""
    )
    fun activeDays(): Flow<List<String>>

    /* Kaynaklar */
    @Upsert suspend fun saveSource(s: StudySource)
    @Query("SELECT * FROM StudySource WHERE id = :id") suspend fun source(id: String): StudySource?
    @Query("SELECT * FROM StudySource") suspend fun sourcesNow(): List<StudySource>
    @Query("DELETE FROM StudySource WHERE id = :id") suspend fun deleteSource(id: String)
    @Query("SELECT * FROM StudySource WHERE archived = 0 ORDER BY favorite DESC, lastUsedAt DESC, name") fun sources(): Flow<List<StudySource>>
    @Query(
        """SELECT s.*,
        COALESCE((SELECT SUM(l.correct + l.wrong + l.blank) FROM PracticeLog l WHERE l.sourceId = s.id), 0) AS solved,
        COALESCE((SELECT SUM(l.correct) FROM PracticeLog l WHERE l.sourceId = s.id), 0) AS correct,
        COALESCE((SELECT SUM(l.wrong) FROM PracticeLog l WHERE l.sourceId = s.id), 0) AS wrong,
        COALESCE((SELECT SUM(x.minutes) FROM StudySession x WHERE x.sourceId = s.id), 0) AS minutes,
        (SELECT MAX(x.startedAt) FROM StudySession x WHERE x.sourceId = s.id) AS lastAt
        FROM StudySource s ORDER BY s.archived ASC, s.favorite DESC, s.lastUsedAt DESC, s.name"""
    )
    fun sourceRows(): Flow<List<SourceRow>>
    @Query("SELECT * FROM StudySource WHERE archived = 0 AND lastUsedAt > 0 ORDER BY lastUsedAt DESC LIMIT 1") fun lastSource(): Flow<StudySource?>

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
    @Query("SELECT * FROM StudyTask WHERE date >= :from AND date <= :to ORDER BY date, time") suspend fun tasksBetweenNow(from: String, to: String): List<StudyTask>
    @Query("SELECT * FROM StudyTask WHERE date < :before AND date >= :since AND done = 0 AND skipped = 0 ORDER BY date, time") fun missedTasks(since: String, before: String): Flow<List<StudyTask>>
    @Query("SELECT * FROM StudyTask WHERE date < :before AND date >= :since AND done = 0 AND skipped = 0 ORDER BY date, time") suspend fun missedTasksNow(since: String, before: String): List<StudyTask>
    @Query("SELECT * FROM StudyTask WHERE id = :id") suspend fun task(id: String): StudyTask?

    /* Pusula Koç */
    @Upsert suspend fun saveRoute(r: List<RouteItem>)
    @Upsert suspend fun saveRouteItem(r: RouteItem)
    @Query("SELECT * FROM RouteItem WHERE date = :date ORDER BY orderIndex") fun routeOn(date: String): Flow<List<RouteItem>>
    @Query("SELECT * FROM RouteItem WHERE date = :date ORDER BY orderIndex") suspend fun routeOnNow(date: String): List<RouteItem>
    @Query("SELECT * FROM RouteItem WHERE id = :id") suspend fun routeItem(id: String): RouteItem?
    @Query("DELETE FROM RouteItem WHERE date = :date AND status = 'SUGGESTED'") suspend fun clearSuggested(date: String)
    @Query("DELETE FROM RouteItem WHERE id = :id") suspend fun deleteRouteItem(id: String)
    @Query("DELETE FROM RouteItem WHERE date < :before") suspend fun pruneRoute(before: String)
    @Query("SELECT topicId FROM RouteItem WHERE status = 'REJECTED' AND topicId IS NOT NULL AND date >= :since") suspend fun rejectedTopics(since: String): List<String>
    @Query("SELECT * FROM RouteItem") suspend fun routeNow(): List<RouteItem>
    @Upsert suspend fun saveRevision(r: PlanRevision)
    @Query("SELECT * FROM PlanRevision ORDER BY createdAt DESC") fun revisions(): Flow<List<PlanRevision>>
    @Query("SELECT * FROM PlanRevision") suspend fun revisionsNow(): List<PlanRevision>

    /* Oturumlar */
    @Upsert suspend fun saveSession(s: StudySession)
    @Query("SELECT * FROM StudySession WHERE sourceId = :id ORDER BY startedAt DESC") fun sessionsOfSource(id: String): Flow<List<StudySession>>
    @Query("SELECT * FROM StudySession WHERE topicId = :id ORDER BY startedAt DESC") fun sessionsOfTopic(id: String): Flow<List<StudySession>>
    @Query("SELECT * FROM StudySession WHERE startedAt >= :from AND startedAt < :to") suspend fun sessionsBetween(from: Long, to: Long): List<StudySession>
    @Query("DELETE FROM StudySession WHERE id = :id") suspend fun deleteSession(id: String)
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
    @Query("SELECT * FROM WrongEntry WHERE id = :id") suspend fun wrong(id: String): WrongEntry?
    @Upsert suspend fun saveWrong(w: WrongEntry)
    @Query("DELETE FROM WrongEntry WHERE id = :id") suspend fun deleteWrong(id: String)
    @Query("SELECT * FROM WrongEntry") suspend fun wrongsNow(): List<WrongEntry>
    @Query(
        """SELECT w.*, t.name AS topicName, s.name AS subjectName FROM WrongEntry w
        LEFT JOIN Topic t ON t.id = w.topicId LEFT JOIN Subject s ON s.id = w.subjectId
        ORDER BY w.resolved ASC, w.nextReviewAt ASC"""
    )
    fun wrongRows(): Flow<List<WrongRow>>
    @Query("SELECT COUNT(*) FROM WrongEntry WHERE resolved = 0 AND nextReviewAt <= :now") fun wrongDue(now: Long): Flow<Int>
    @Query("SELECT COUNT(*) FROM WrongEntry WHERE resolved = 0 AND nextReviewAt <= :now") suspend fun wrongDueNow(now: Long): Int
    @Query("SELECT COUNT(*) FROM WrongEntry WHERE resolved = 0") fun wrongOpen(): Flow<Int>
    @Query("SELECT id FROM WrongEntry WHERE resolved = 0 AND nextReviewAt <= :now ORDER BY wrongCount DESC, nextReviewAt ASC")
    suspend fun wrongDueIds(now: Long): List<String>
    @Query("SELECT reason AS name, COUNT(*) AS value FROM WrongEntry WHERE reason != '' GROUP BY reason ORDER BY value DESC") fun wrongReasons(): Flow<List<NamedCount>>
    @Upsert suspend fun saveWrongReview(r: WrongReview)
    @Query("SELECT * FROM WrongReview WHERE wrongId = :id ORDER BY at DESC") fun reviewsOf(id: String): Flow<List<WrongReview>>
    @Query("SELECT * FROM WrongReview") suspend fun wrongReviewsNow(): List<WrongReview>
    @Query("SELECT COUNT(*) FROM WrongReview WHERE at >= :from AND at < :to") suspend fun wrongReviewsBetween(from: Long, to: Long): Int

    /* Denemeler */
    @Upsert suspend fun saveMock(m: MockExam)
    @Upsert suspend fun saveResults(r: List<MockExamResult>)
    @Query("SELECT * FROM MockExam WHERE id = :id") suspend fun mock(id: String): MockExam?
    @Query("SELECT * FROM MockExam WHERE status IN ('RUNNING', 'ENTRY') ORDER BY createdAt DESC LIMIT 1") fun running(): Flow<MockExam?>
    @Query("SELECT * FROM MockExamResult WHERE mockExamId = :id ORDER BY sortOrder") suspend fun results(id: String): List<MockExamResult>
    @Query("DELETE FROM MockExam WHERE id = :id") suspend fun deleteMock(id: String)
    @Query("DELETE FROM MockExamResult WHERE mockExamId = :id") suspend fun deleteResults(id: String)
    @Query("DELETE FROM PracticeLog WHERE mockExamId = :id") suspend fun deleteMockLogs(id: String)
    @Query("SELECT * FROM MockExam") suspend fun mocksNow(): List<MockExam>
    @Query("SELECT * FROM MockExamResult") suspend fun resultsNow(): List<MockExamResult>
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
    @Upsert suspend fun saveCardReview(r: CardReview)
    @Query("SELECT * FROM CardReview") suspend fun cardReviewsNow(): List<CardReview>
    @Query("SELECT COUNT(*) FROM CardReview WHERE cardId = :id") fun cardReviewCount(id: String): Flow<Int>

    /* Kişisel içerik notları, net hedefleri, dinlenme günleri */
    @Upsert suspend fun saveItemNote(n: ItemNote)
    @Query("DELETE FROM ItemNote WHERE kind = :kind AND refId = :refId") suspend fun deleteItemNote(kind: String, refId: String)
    @Query("SELECT * FROM ItemNote WHERE kind = :kind") fun itemNotes(kind: String): Flow<List<ItemNote>>
    @Query("SELECT * FROM ItemNote") suspend fun itemNotesNow(): List<ItemNote>
    @Upsert suspend fun saveNetGoal(g: NetGoal)
    @Query("DELETE FROM NetGoal WHERE exam = :exam AND section = :section") suspend fun deleteNetGoal(exam: String, section: String)
    @Query("SELECT * FROM NetGoal") fun netGoals(): Flow<List<NetGoal>>
    @Query("SELECT * FROM NetGoal") suspend fun netGoalsNow(): List<NetGoal>
    @Upsert suspend fun saveRestDay(d: RestDay)
    @Query("DELETE FROM RestDay WHERE date = :date") suspend fun deleteRestDay(date: String)
    @Query("SELECT date FROM RestDay") fun restDays(): Flow<List<String>>
    @Query("SELECT * FROM RestDay") suspend fun restDaysNow(): List<RestDay>

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
    @Query("DELETE FROM PracticeLog") suspend fun clearLogs()
    @Query("DELETE FROM WrongReview") suspend fun clearWrongReviews()
    @Query("DELETE FROM StudySource") suspend fun clearSources()
    @Query("DELETE FROM RouteItem") suspend fun clearRoute()
    @Query("DELETE FROM PlanRevision") suspend fun clearRevisions()
    @Query("DELETE FROM CardReview") suspend fun clearCardReviews()
    @Query("DELETE FROM ItemNote") suspend fun clearItemNotes()
    @Query("DELETE FROM NetGoal") suspend fun clearNetGoals()
    @Query("DELETE FROM RestDay") suspend fun clearRestDays()
    @Query("DELETE FROM StudyPlan") suspend fun clearPlans()
    @Query("DELETE FROM StudyTask") suspend fun clearTasks()
    @Query("DELETE FROM StudySession") suspend fun clearSessions()
    @Query("DELETE FROM MockExam") suspend fun clearMocks()
    @Query("DELETE FROM MockExamResult") suspend fun clearResults()
    @Query("DELETE FROM TopicProgress") suspend fun clearProgress()
    @Query("DELETE FROM WrongEntry") suspend fun clearWrongs()
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

    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun mergePlans(v: List<StudyPlan>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun mergeTasks(v: List<StudyTask>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun mergeSessions(v: List<StudySession>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun mergeMocks(v: List<MockExam>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun mergeResults(v: List<MockExamResult>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun mergeProgress(v: List<TopicProgress>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun mergeWrongs(v: List<WrongEntry>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun mergeLogs(v: List<PracticeLog>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun mergeSources(v: List<StudySource>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun mergeWrongReviews(v: List<WrongReview>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun mergeRoute(v: List<RouteItem>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun mergeRevisions(v: List<PlanRevision>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun mergeCardReviews(v: List<CardReview>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun mergeItemNotes(v: List<ItemNote>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun mergeNetGoals(v: List<NetGoal>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun mergeRestDays(v: List<RestDay>)
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
    version = 2,
    exportSchema = true,
    entities = [
        UserProfile::class, ExamConfiguration::class, Subject::class, Topic::class,
        Formula::class, Flashcard::class, ImportedContentPack::class, PracticeLog::class, StudySource::class,
        StudyPlan::class, StudyTask::class, StudySession::class, MockExam::class, MockExamResult::class, TopicProgress::class,
        WrongEntry::class, WrongReview::class, RouteItem::class, PlanRevision::class, CardReview::class, ItemNote::class, NetGoal::class, RestDay::class,
        FlashcardProgress::class, UserNote::class, Bookmark::class, Reminder::class,
        TargetUniversity::class, TargetDepartment::class, LibraryFile::class, UniversityRecord::class,
    ],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun content(): ContentDao
    abstract fun user(): UserDao

    companion object {
        /** Şema değişiklikleri Migration ile yapılır; yıkıcı geçiş hiçbir zaman açılmaz. */
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "yks_pusula.db").addMigrations(MIGRATION_1_2).build()
    }
}
