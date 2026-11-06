package app.ykspusula.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.withTransaction
import app.ykspusula.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

val Context.settingsStore: DataStore<Preferences> by preferencesDataStore("settings")

data class AppSettings(
    val theme: String = "DARK",
    val fontScale: Float = 1f,
    val pomodoroWork: Int = 25,
    val pomodoroBreak: Int = 5,
    val weeklyGoalMinutes: Int = 0,
    val monthlyGoalQuestions: Int = 0,
    val diploma: String = "",
    val placedLastYear: Boolean = false,
)

data class PomodoroState(
    val phase: String = "IDLE",
    val running: Boolean = false,
    val endsAt: Long = 0,
    val remainingSec: Int = 0,
    val workMin: Int = 25,
    val breakMin: Int = 5,
    val subjectId: String = "",
    val topicId: String = "",
    val sourceId: String = "",
    val activity: String = "",
    val label: String = "",
    val note: String = "",
    val startedAt: Long = 0,
    val pendingSessionId: String = "",
)

@Serializable data class SectionPart(val subjectId: String, val count: Int)
@Serializable data class SectionConfig(val name: String, val parts: List<SectionPart>)

object Keys {
    val theme = stringPreferencesKey("theme")
    val font = floatPreferencesKey("font_scale")
    val pWork = intPreferencesKey("pomodoro_work")
    val pBreak = intPreferencesKey("pomodoro_break")
    val weekly = intPreferencesKey("weekly_goal_minutes")
    val monthly = intPreferencesKey("monthly_goal_questions")
    val diploma = stringPreferencesKey("diploma")
    val placed = booleanPreferencesKey("placed_last_year")
    val pPhase = stringPreferencesKey("p_phase")
    val pRunning = booleanPreferencesKey("p_running")
    val pEnds = longPreferencesKey("p_ends")
    val pRemain = intPreferencesKey("p_remaining")
    val pW = intPreferencesKey("p_w")
    val pB = intPreferencesKey("p_b")
    val pSubject = stringPreferencesKey("p_subject")
    val pTopic = stringPreferencesKey("p_topic")
    val pSource = stringPreferencesKey("p_source")
    val pActivity = stringPreferencesKey("p_activity")
    val pLabel = stringPreferencesKey("p_label")
    val pNote = stringPreferencesKey("p_note")
    val pStarted = longPreferencesKey("p_started")
    val pPending = stringPreferencesKey("p_pending_session")
    val seeded = stringPreferencesKey("seed_version")
    val quoteOffset = stringPreferencesKey("quote_offset")
    val quoteNotifiedDay = stringPreferencesKey("quote_notified_day")
    val lastBackupAt = longPreferencesKey("last_backup_at")
    val reviewAskedAt = longPreferencesKey("review_asked_at")
}

fun zone(): ZoneId = ZoneId.systemDefault()
fun today(): LocalDate = LocalDate.now(zone())
fun startOf(d: LocalDate): Long = d.atStartOfDay(zone()).toInstant().toEpochMilli()
fun dateOf(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(zone()).toLocalDate()
fun weekStart(d: LocalDate): LocalDate = d.minusDays((d.dayOfWeek.value - 1).toLong())

/** Çalışma kaydı: kaynak, süre ve soru performansı tek işlemde bütün istatistiklere yansır. */
data class StudyEntry(
    val subjectId: String? = null,
    val topicId: String? = null,
    val sourceId: String? = null,
    val pageFrom: Int? = null,
    val pageTo: Int? = null,
    val correct: Int = 0,
    val wrong: Int = 0,
    val blank: Int = 0,
    val minutes: Int = 0,
    val plannedMinutes: Int = 0,
    val activity: String = "SORU",
    val note: String = "",
    val label: String = "",
    val at: Long = System.currentTimeMillis(),
    val taskId: String? = null,
    val routeId: String? = null,
)

data class SectionEntry(val name: String, val correct: Int, val wrong: Int, val blank: Int, val subjectId: String? = null)

class Repository(private val context: Context, val db: AppDatabase) {
    val content = db.content()
    val user = db.user()
    private val store get() = context.settingsStore

    /** Uygulama içi veriler değiştiğinde ana ekran bileşenlerini tazelemek için. */
    var onDataChanged: (() -> Unit)? = null
    private fun changed() { onDataChanged?.invoke() }

    /* ---------- AYARLAR ---------- */

    val settings: Flow<AppSettings> = store.data.map { p ->
        AppSettings(
            theme = p[Keys.theme] ?: "DARK",
            fontScale = p[Keys.font] ?: 1f,
            pomodoroWork = p[Keys.pWork] ?: 25,
            pomodoroBreak = p[Keys.pBreak] ?: 5,
            weeklyGoalMinutes = p[Keys.weekly] ?: 0,
            monthlyGoalQuestions = p[Keys.monthly] ?: 0,
            diploma = p[Keys.diploma] ?: "",
            placedLastYear = p[Keys.placed] ?: false,
        )
    }

    val lastBackupAt: Flow<Long> = store.data.map { it[Keys.lastBackupAt] ?: 0L }

    suspend fun updateSettings(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        store.edit { block(it) }
    }

    val pomodoro: Flow<PomodoroState> = store.data.map { p ->
        PomodoroState(
            phase = p[Keys.pPhase] ?: "IDLE",
            running = p[Keys.pRunning] ?: false,
            endsAt = p[Keys.pEnds] ?: 0,
            remainingSec = p[Keys.pRemain] ?: 0,
            workMin = p[Keys.pW] ?: (p[Keys.pWork] ?: 25),
            breakMin = p[Keys.pB] ?: (p[Keys.pBreak] ?: 5),
            subjectId = p[Keys.pSubject] ?: "",
            topicId = p[Keys.pTopic] ?: "",
            sourceId = p[Keys.pSource] ?: "",
            activity = p[Keys.pActivity] ?: "",
            label = p[Keys.pLabel] ?: "",
            note = p[Keys.pNote] ?: "",
            startedAt = p[Keys.pStarted] ?: 0,
            pendingSessionId = p[Keys.pPending] ?: "",
        )
    }

    suspend fun savePomodoro(s: PomodoroState) {
        store.edit { p ->
            p[Keys.pPhase] = s.phase
            p[Keys.pRunning] = s.running
            p[Keys.pEnds] = s.endsAt
            p[Keys.pRemain] = s.remainingSec
            p[Keys.pW] = s.workMin
            p[Keys.pB] = s.breakMin
            p[Keys.pSubject] = s.subjectId
            p[Keys.pTopic] = s.topicId
            p[Keys.pSource] = s.sourceId
            p[Keys.pActivity] = s.activity
            p[Keys.pLabel] = s.label
            p[Keys.pNote] = s.note
            p[Keys.pStarted] = s.startedAt
            p[Keys.pPending] = s.pendingSessionId
        }
    }

    /* ---------- BAŞLANGIÇ İÇERİĞİ ---------- */

    suspend fun ensureSeeded(): Unit = withContext(Dispatchers.IO) {
        val text = context.assets.open("content/base.json").bufferedReader().use { it.readText() }
        val pack = ContentValidator.parse(text)
        val current = store.data.first()[Keys.seeded]
        if (current != pack.pack.version || content.topicCountNow() < pack.topics.size) {
            installPack(pack)
            store.edit { it[Keys.seeded] = pack.pack.version }
        }
    }

    suspend fun installPack(p: ContentPackFile) {
        db.withTransaction {
            content.examConfigsIfMissing(p.examConfigs)
            // Yeni yılın paketi eski yılın sınav ayarlarının yerine geçer; aynı yılın ayarlarını öğrenci değiştirmiş olabilir.
            p.examConfigs.forEach { c ->
                val cur = user.examConfig(c.code)
                if (cur != null && c.examYear > cur.examYear) user.saveExamConfig(c)
                else if (cur != null && cur.lastUpdated.isBlank()) {
                    user.saveExamConfig(cur.copy(examYear = c.examYear, curriculum = c.curriculum, sourceUrl = c.sourceUrl, lastUpdated = c.lastUpdated, obpMultiplier = c.obpMultiplier, obpCoefficient = c.obpCoefficient, obpCoefficientPlaced = c.obpCoefficientPlaced))
                }
            }
            content.subjects(p.subjects.map { Subject(it.id, it.exam, it.name, it.groupName, it.fields, it.sortOrder, it.language, p.pack.id) })
            content.topics(
                p.topics.map {
                    Topic(
                        it.id, it.subjectId, it.parentId, it.name, it.summary, it.lesson, it.keyPoints, it.example, it.curriculum,
                        it.validFromYear, it.validToYear, it.scope, it.scopeNote, it.estimatedMinutes, it.sortOrder, p.pack.id,
                    )
                }
            )
            content.formulas(p.formulas.map { Formula(it.id, it.subjectId, it.subjectName, it.category, it.title, it.body, it.note, p.pack.id) })
            content.flashcards(p.flashcards.map { Flashcard(it.id, it.deck, it.subjectName, it.front, it.back, it.partOfSpeech, it.example, false, p.pack.id) })
            content.pack(ImportedContentPack(p.pack.id, p.pack.title, p.pack.version, p.pack.kind, System.currentTimeMillis(), 0, p.pack.source, p.pack.license))
        }
    }

    /** Kullanıcının seçtiği içerik paketini doğrular; hatalı veriyi reddeder. */
    suspend fun importPack(text: String): Pair<Boolean, List<String>> = withContext(Dispatchers.IO) {
        val pack = try {
            ContentValidator.parse(text)
        } catch (e: Exception) {
            return@withContext false to listOf("Dosya geçerli bir içerik paketi değil.")
        }
        val errors = ContentValidator.validate(pack, content.allTopicIds().toSet(), content.allSubjectIds().toSet())
        if (errors.isNotEmpty()) return@withContext false to errors.take(20)
        installPack(pack)
        true to listOf("${pack.topics.size} konu, ${pack.formulas.size} formül, ${pack.flashcards.size} kart içe aktarıldı.")
    }

    /* ---------- ÇALIŞMA KAYDI ---------- */

    /** Süre, soru performansı, kaynak ilerlemesi, konu durumu ve görev aynı işlemde güncellenir. */
    suspend fun logStudy(e: StudyEntry): String {
        val sessionId = newId()
        val now = System.currentTimeMillis()
        db.withTransaction {
            val subject = e.subjectId ?: e.topicId?.let { content.subjectOfTopic(it) }
            val source = e.sourceId?.let { user.source(it) }
            val label = e.label.ifBlank { source?.name ?: "Çalışma" }
            if (e.minutes > 0) {
                user.saveSession(
                    StudySession(
                        id = sessionId, subjectId = subject, topicId = e.topicId, label = label, startedAt = e.at, minutes = e.minutes,
                        kind = "MANUAL", note = e.note, sourceId = e.sourceId, plannedMinutes = e.plannedMinutes, activity = e.activity,
                    )
                )
            }
            if (e.correct + e.wrong + e.blank > 0) {
                user.saveLog(
                    PracticeLog(
                        at = e.at, subjectId = subject, topicId = e.topicId, sourceId = e.sourceId, sessionId = sessionId,
                        pageFrom = e.pageFrom, pageTo = e.pageTo, correct = e.correct, wrong = e.wrong, blank = e.blank, note = e.note, createdAt = now, updatedAt = now,
                    )
                )
            }
            if (source != null) {
                val pages = if (e.pageFrom != null && e.pageTo != null && e.pageTo >= e.pageFrom) e.pageTo - e.pageFrom + 1 else 0
                val last = maxOf(source.lastPage, e.pageTo ?: 0)
                val total = source.totalPages
                val done = (source.pagesDone + pages).let { if (total > 0) it.coerceAtMost(total) else it }
                user.saveSource(source.copy(lastPage = last, pagesDone = done, lastUsedAt = e.at, updatedAt = now))
            }
            e.topicId?.let { touchTopic(it, e.at) }
            e.taskId?.let { id -> user.task(id)?.let { user.saveTask(it.copy(done = true, doneAt = now, updatedAt = now)) } }
            e.routeId?.let { id -> user.routeItem(id)?.let { user.saveRouteItem(it.copy(status = "DONE", updatedAt = now)) } }
        }
        changed()
        return sessionId
    }

    /** Çalışma oturumunun sonunda girilen soru performansını mevcut oturuma bağlar. */
    suspend fun addPerformance(sessionId: String, correct: Int, wrong: Int, blank: Int, note: String) {
        val s = user.sessionsNow().firstOrNull { it.id == sessionId } ?: return
        if (correct + wrong + blank == 0) return
        val now = System.currentTimeMillis()
        user.saveLog(PracticeLog(at = s.startedAt, subjectId = s.subjectId, topicId = s.topicId, sourceId = s.sourceId, sessionId = sessionId, correct = correct, wrong = wrong, blank = blank, note = note, createdAt = now, updatedAt = now))
        changed()
    }

    private suspend fun touchTopic(topicId: String, now: Long) {
        val p = user.progressNow(topicId) ?: TopicProgress(topicId)
        user.saveProgress(p.copy(status = if (p.status < 1) 1 else p.status, lastStudiedAt = now))
    }

    suspend fun setTopicStatus(topicId: String, status: Int) {
        val now = System.currentTimeMillis()
        val p = user.progressNow(topicId) ?: TopicProgress(topicId)
        user.saveProgress(
            p.copy(
                status = status.coerceIn(0, 3),
                lastStudiedAt = if (status > 0) now else p.lastStudiedAt,
                completedAt = if (status == 3) (p.completedAt ?: now) else null,
                nextReviewAt = if (status == 3 && p.nextReviewAt == null) Repetition.nextAt(2, now) else p.nextReviewAt,
            )
        )
        changed()
    }

    suspend fun setTopicReview(topicId: String, at: Long?) {
        val p = user.progressNow(topicId) ?: TopicProgress(topicId)
        user.saveProgress(p.copy(nextReviewAt = at))
    }

    /** Konu tekrarı yapıldı: sayaç artar ve sonraki tekrar aralığı planlanır. */
    suspend fun markTopicReviewed(topicId: String, now: Long = System.currentTimeMillis()) {
        val p = user.progressNow(topicId) ?: TopicProgress(topicId)
        val stage = (p.reviewCount + 1).coerceAtMost(Repetition.INTERVALS.lastIndex)
        user.saveProgress(p.copy(status = maxOf(p.status, 1), reviewCount = p.reviewCount + 1, lastReviewAt = now, lastStudiedAt = now, nextReviewAt = Repetition.nextAt(stage, now)))
        changed()
    }

    suspend fun toggleTopicCheck(topicId: String, check: String) {
        val p = user.progressNow(topicId) ?: TopicProgress(topicId)
        val set = p.checks.split(',').filter { it.isNotBlank() }.toMutableSet()
        if (!set.add(check)) set.remove(check)
        user.saveProgress(p.copy(checks = set.sorted().joinToString(",")))
    }

    fun mastery(t: TopicRow, now: Long = System.currentTimeMillis()): MasteryResult = Mastery.compute(
        MasteryInput(t.status ?: 0, t.total, t.right, t.reviewCount ?: 0, t.lastStudiedAt, t.nextReviewAt, t.wrongOpen, t.minutes, now)
    )

    suspend fun toggleBookmark(kind: String, id: String, on: Boolean) {
        if (on) user.saveBookmark(Bookmark(kind, id, System.currentTimeMillis())) else user.deleteBookmark(kind, id)
    }

    /* ---------- YANLIŞ DEFTERİ ---------- */

    suspend fun saveWrong(w: WrongEntry) {
        user.saveWrong(w)
        w.topicId?.let { t -> if (user.progressNow(t) == null) user.saveProgress(TopicProgress(t, status = 1, lastStudiedAt = w.lastWrongAt)) }
        changed()
    }

    fun newWrong(subjectId: String?, topicId: String?, sourceId: String?, sourceLabel: String, page: Int?, questionNo: String, reason: String, note: String, photoFile: String?, reviewAt: Long?, now: Long = System.currentTimeMillis()) = WrongEntry(
        subjectId = subjectId, topicId = topicId, sourceId = sourceId, sourceLabel = sourceLabel, page = page, questionNo = questionNo,
        reason = reason, note = note, photoFile = photoFile, firstWrongAt = now, lastWrongAt = now, nextReviewAt = reviewAt ?: Repetition.onWrong(now).nextReviewAt, updatedAt = now,
    )

    /** Tekrar sonucuna göre aralık değişir ve geçmiş kaydedilir. */
    suspend fun reviewWrong(id: String, result: Int, now: Long = System.currentTimeMillis()) {
        db.withTransaction {
            val w = user.wrong(id) ?: return@withTransaction
            val o = Repetition.onReview(w.stage, result, now)
            user.saveWrong(w.copy(stage = o.stage, nextReviewAt = o.nextReviewAt, resolved = o.resolved, wrongCount = if (result == 0) w.wrongCount + 1 else w.wrongCount, lastWrongAt = if (result == 0) now else w.lastWrongAt, updatedAt = now))
            user.saveWrongReview(WrongReview(wrongId = id, at = now, result = result, stageAfter = o.stage))
        }
        changed()
    }

    suspend fun deleteWrong(id: String) {
        user.wrong(id)?.photoFile?.let { photoFile(it).delete() }
        user.deleteWrong(id)
        changed()
    }

    fun photoFile(name: String) = context.filesDir.resolve("photos").apply { mkdirs() }.resolve(name)

    /* ---------- DENEMELER ---------- */

    fun sectionsOf(c: ExamConfiguration): List<SectionConfig> =
        contentJson.decodeFromString(ListSerializer(SectionConfig.serializer()), c.sectionsJson)

    /** Dışarıda çözülen denemenin süresini tutar. Soru üretmez. */
    suspend fun startExamSession(title: String, kind: String, exam: String, minutes: Int, subjectId: String? = null, now: Long = System.currentTimeMillis()): String {
        val id = newId()
        user.saveMock(MockExam(id, title, kind, exam, now, now + minutes * 60_000L, minutes * 60, "RUNNING", isExternal = true, examDate = dateOf(now).toString(), subjectId = subjectId, updatedAt = now))
        changed()
        return id
    }

    fun elapsedSec(m: MockExam, now: Long): Int {
        val end = if (m.pausedAt > 0) m.pausedAt else now
        return ((end - m.createdAt - m.pausedTotalMs) / 1000L).toInt().coerceAtLeast(0)
    }

    suspend fun pauseExam(id: String, now: Long = System.currentTimeMillis()) {
        user.mock(id)?.takeIf { it.pausedAt == 0L }?.let { user.saveMock(it.copy(pausedAt = now, updatedAt = now)) }
    }

    suspend fun resumeExam(id: String, now: Long = System.currentTimeMillis()) {
        user.mock(id)?.takeIf { it.pausedAt > 0L }?.let { user.saveMock(it.copy(pausedAt = 0, pausedTotalMs = it.pausedTotalMs + (now - it.pausedAt), updatedAt = now)) }
    }

    /** Süre durur; deneme sonuç girişini bekler. */
    suspend fun stopExam(id: String, now: Long = System.currentTimeMillis()) {
        val m = user.mock(id) ?: return
        user.saveMock(m.copy(status = "ENTRY", spentMinutes = (elapsedSec(m, now) + 59) / 60, pausedAt = 0, updatedAt = now))
    }

    suspend fun saveExamResults(
        id: String, title: String, publisher: String, date: LocalDate, spentMinutes: Int, rows: List<SectionEntry>, hardTopics: String, note: String,
    ) {
        val m = user.mock(id) ?: return
        val divisor = user.examConfig(m.exam)?.wrongDivisor ?: 4.0
        val now = System.currentTimeMillis()
        val at = if (dateOf(m.createdAt) == date) m.createdAt else startOf(date) + 12 * 3_600_000L
        db.withTransaction {
            user.saveMock(m.copy(title = title, publisher = publisher, createdAt = at, examDate = date.toString(), status = "DONE", spentMinutes = spentMinutes, note = note, hardTopics = hardTopics, updatedAt = now))
            user.deleteResults(id)
            user.deleteMockLogs(id)
            user.saveResults(rows.mapIndexed { i, r -> MockExamResult(id, r.name, r.correct, r.wrong, r.blank, Scoring.net(r.correct, r.wrong, divisor), i) })
            user.saveLogs(rows.map { r -> PracticeLog(id = "mock-$id-${r.name.hashCode()}", at = at, subjectId = r.subjectId, mockExamId = id, correct = r.correct, wrong = r.wrong, blank = r.blank, origin = "MOCK", createdAt = now, updatedAt = now) })
            if (spentMinutes > 0) user.saveSession(StudySession(id = "mock-$id", label = title, startedAt = at, minutes = spentMinutes, kind = "MOCK", activity = "DENEME"))
            hardTopics.split(',').filter { it.isNotBlank() }.forEach { t -> setReviewIfMissing(t, now) }
        }
        changed()
    }

    private suspend fun setReviewIfMissing(topicId: String, now: Long) {
        val p = user.progressNow(topicId) ?: TopicProgress(topicId)
        if (p.nextReviewAt == null || p.nextReviewAt > now + Repetition.DAY_MS) user.saveProgress(p.copy(status = maxOf(p.status, 1), nextReviewAt = now + Repetition.DAY_MS))
    }

    suspend fun saveExternalExam(
        title: String, publisher: String, date: LocalDate, exam: String, spentMinutes: Int, rows: List<SectionEntry>,
        kind: String = "EXTERNAL", hardTopics: String = "", note: String = "", subjectId: String? = null,
    ): String {
        val id = newId()
        val at = startOf(date) + 12 * 3_600_000L
        user.saveMock(MockExam(id, title, kind, exam, at, at, spentMinutes * 60, "ENTRY", true, publisher, date.toString(), spentMinutes, subjectId = subjectId, updatedAt = at))
        saveExamResults(id, title, publisher, date, spentMinutes, rows, hardTopics, note)
        return id
    }

    suspend fun deleteExam(id: String) {
        db.withTransaction {
            user.deleteResults(id); user.deleteMockLogs(id); user.deleteMock(id); user.deleteSession("mock-$id")
        }
        changed()
    }

    suspend fun examAnalysis(exam: String): ExamAnalysis {
        val mocks = user.mockSummariesNow().filter { exam.isEmpty() || it.exam == exam }
        val rows = ArrayList<SectionResultRow>()
        mocks.forEach { m -> user.results(m.id).forEach { rows += SectionResultRow(m.id, m.createdAt, it.section, it.correct, it.wrong, it.blank, it.net) } }
        val minutes = mocks.mapNotNull { user.mock(it.id)?.spentMinutes }
        return ExamAnalyzer.analyze(rows, mocks.map { it.createdAt to it.net }, minutes)
    }

    /* ---------- PLAN ---------- */

    suspend fun planInput(start: LocalDate): PlanInput {
        val profile = user.profileNow() ?: UserProfile()
        val cfg = user.examConfigsNow()
        val examCode = if (profile.field == "DIL") "YDT" else if (profile.field == "TYT") "TYT" else "AYT"
        val examDate = LocalDate.parse((cfg.firstOrNull { it.code == examCode } ?: cfg.first()).examDate)
        val subjects = planSubjects(profile)
        val topics = content.allTopics().filter { t -> t.scope != "EXCLUDED" && subjects.any { it.id == t.subjectId } }
        val progress = user.progressAll().associateBy { it.topicId }
        val stats = user.topicStatsNow().associateBy { it.key }
        val now = System.currentTimeMillis()
        val plan = topics.map { t ->
            val p = progress[t.id]
            val s = stats[t.id]
            PlanTopic(t.id, t.subjectId, t.name, p?.status ?: 0, s?.takeIf { it.total > 0 }?.let { Scoring.percent(it.right, it.total) }, t.estimatedMinutes, p?.nextReviewAt?.let { it <= now } ?: false)
        }
        val days = profile.studyDays.split(',').mapNotNull { it.toIntOrNull() }.toSet().ifEmpty { setOf(1, 2, 3, 4, 5, 6, 7) }
        return PlanInput(
            start, examDate, profile.dailyMinutes, days, profile.startHour, subjects, plan,
            profile.weakSubjects.split(',').filter { it.isNotBlank() }.toSet(),
            profile.strongSubjects.split(',').filter { it.isNotBlank() }.toSet(),
            user.wrongDueNow(now),
            profile.maxDailyMinutes, profile.breakMinutes, profile.endHour,
            profile.priorityTopics.split(',').filter { it.isNotBlank() }.toSet(),
        )
    }

    suspend fun planSubjects(p: UserProfile): List<PlanSubject> {
        val all = content.subjectsNow().filter { it.exam == "TYT" || (p.field != "TYT" && it.fields.split(',').any { f -> f == p.field || f == "ALL" }) }
        val relevant = all.filter { it.language == null || it.language == p.ydtLanguage }
        return relevant.map { s ->
            val w = when {
                s.groupName == "Türkçe" || s.groupName == "Matematik" -> 3.0
                s.exam == "TYT" -> 1.0
                else -> 2.5
            }
            PlanSubject(s.id, s.name, w)
        }
    }

    suspend fun generatePlan(start: LocalDate = today()): Int {
        val tasks = PlanGenerator.generate(planInput(start))
        if (tasks.isEmpty()) return 0
        val now = System.currentTimeMillis()
        val plan = StudyPlan(title = "Otomatik plan", startDate = start.toString(), endDate = start.plusDays(6).toString(), isAuto = true, createdAt = now)
        db.withTransaction {
            snapshotPlan("Yeni plan oluşturulmadan önce", start)
            user.clearPendingAuto(start.toString())
            user.savePlan(plan)
            user.saveTasks(tasks.map { StudyTask(planId = plan.id, date = it.date.toString(), time = it.time, subjectId = it.subjectId, topicId = it.topicId, title = it.title, kind = it.kind, minutes = it.minutes, questionGoal = it.questionGoal, reason = it.reason, priority = it.priority, originalDate = it.date.toString(), updatedAt = now) })
        }
        changed()
        return tasks.size
    }

    private suspend fun snapshotPlan(summary: String, from: LocalDate) {
        val tasks = user.tasksBetweenNow(from.toString(), from.plusDays(30).toString())
        if (tasks.isEmpty()) return
        user.saveRevision(PlanRevision(createdAt = System.currentTimeMillis(), summary = summary, snapshotJson = contentJson.encodeToString(ListSerializer(StudyTask.serializer()), tasks)))
    }

    /** Geçmiş 14 gündeki tamamlanmamış görevler. */
    fun missedTasks(date: LocalDate = today()): Flow<List<StudyTask>> = user.missedTasks(date.minusDays(14).toString(), date.toString())

    suspend fun previewReplan(importantOnly: Boolean, date: LocalDate = today()): ReplanResult {
        val p = user.profileNow() ?: UserProfile()
        val toSlot: (StudyTask) -> SlotTask = { SlotTask(it.id, LocalDate.parse(it.date), it.time, it.minutes, it.priority, it.title) }
        val missed = user.missedTasksNow(date.minusDays(14).toString(), date.toString())
        val upcoming = user.tasksBetweenNow(date.toString(), date.plusDays(13).toString()).filter { !it.done }
        return Replanner.preview(
            ReplanInput(
                date, missed.map(toSlot), upcoming.map(toSlot),
                p.studyDays.split(',').mapNotNull { it.toIntOrNull() }.toSet().ifEmpty { setOf(1, 2, 3, 4, 5, 6, 7) },
                minOf(p.dailyMinutes, p.maxDailyMinutes), p.startHour, p.endHour, p.breakMinutes,
            ),
            importantOnly,
        )
    }

    /** Onaylanan düzenleme uygulanır; önceki durum geçmişte kalır, tamamlanmış görevlere dokunulmaz. */
    suspend fun applyReplan(moves: List<ReplanMove>, skipRemaining: Boolean, date: LocalDate = today()) {
        val now = System.currentTimeMillis()
        db.withTransaction {
            snapshotPlan("Yeniden düzenlemeden önce", date.minusDays(14))
            moves.forEach { m ->
                user.task(m.id)?.let { t ->
                    user.saveTask(t.copy(date = m.to.toString(), time = m.time, originalDate = t.originalDate.ifBlank { t.date }, movedCount = t.movedCount + 1, updatedAt = now))
                }
            }
            if (skipRemaining) {
                val moved = moves.map { it.id }.toSet()
                user.missedTasksNow(date.minusDays(14).toString(), date.toString()).filter { it.id !in moved }.forEach { user.saveTask(it.copy(skipped = true, updatedAt = now)) }
            }
        }
        changed()
    }

    suspend fun skipMissed(date: LocalDate = today()) {
        val now = System.currentTimeMillis()
        user.missedTasksNow(date.minusDays(14).toString(), date.toString()).forEach { user.saveTask(it.copy(skipped = true, updatedAt = now)) }
    }

    suspend fun toggleTask(t: StudyTask, done: Boolean) {
        val now = System.currentTimeMillis()
        db.withTransaction {
            user.saveTask(t.copy(done = done, doneAt = if (done) now else null, updatedAt = now))
            if (t.topicId != null && done) touchTopic(t.topicId, now)
        }
        changed()
    }

    /* ---------- PUSULA KOÇ ---------- */

    private suspend fun coachInput(date: LocalDate): CoachInput {
        val p = user.profileNow() ?: UserProfile()
        val now = System.currentTimeMillis()
        val subjects = planSubjects(p)
        val names = content.subjectsNow().associateBy { it.id }
        val ids = subjects.map { it.id }.toSet()
        val rows = content.allTopicRowsNow().filter { it.subjectId in ids && it.scope != "EXCLUDED" }
        val topics = rows.map { t ->
            CoachTopic(t.id, t.subjectId, t.name, names[t.subjectId]?.name.orEmpty(), mastery(t, now).level, t.total, t.right, t.lastStudiedAt, t.nextReviewAt?.let { it <= now } ?: false, t.wrongOpen)
        }
        val mocks = user.mockSummariesNow().takeLast(8)
        val cfgs = user.examConfigsNow().associateBy { it.code }
        val nets = LinkedHashMap<Pair<String, String>, ArrayList<Double>>()
        mocks.forEach { m -> user.results(m.id).forEach { nets.getOrPut(m.exam to it.section) { ArrayList() } += it.net } }
        val sections = nets.map { (k, v) ->
            val sub = cfgs[k.first]?.let { c -> sectionsOf(c).firstOrNull { it.name == k.second }?.parts?.map { it.subjectId } }.orEmpty()
            CoachSection(k.first, k.second, v, sub)
        }
        val hard = mocks.takeLast(1).flatMap { m -> user.mock(m.id)?.hardTopics.orEmpty().split(',').filter { it.isNotBlank() } }.toSet()
        val missed = user.missedTasksNow(date.minusDays(7).toString(), date.toString()).map { CoachMissed(it.id, it.title, it.subjectId, it.topicId, it.minutes, LocalDate.parse(it.date)) }
        val from = startOf(date)
        val done = user.minutesBetweenNow(from, startOf(date.plusDays(1)))
        val rest = user.restDaysNow().map { it.date }
        return CoachInput(
            date, now, minOf(p.dailyMinutes, p.maxDailyMinutes), done, subjects.associate { it.id to it.weight }, subjects.associate { it.id to it.name },
            p.weakSubjects.split(',').filter { it.isNotBlank() }.toSet(), p.strongSubjects.split(',').filter { it.isNotBlank() }.toSet(),
            p.priorityTopics.split(',').filter { it.isNotBlank() }.toSet(), topics,
            user.sourcesNow().filter { !it.archived }.sortedByDescending { it.lastUsedAt }.map { CoachSource(it.id, it.name, it.subjectId, it.topicId, it.lastPage, it.totalPages) },
            sections, hard, missed, user.wrongDueNow(now), user.cardsDueCountNow(now),
            user.rejectedTopics(date.minusDays(3).toString()).toSet(), user.activeDays().first().size, date.toString() in rest,
        )
    }

    /** Günün rotası bir kez oluşturulur. Öğrencinin kararları kalıcıdır. */
    suspend fun ensureRoute(date: LocalDate = today()) {
        if (user.routeOnNow(date.toString()).isEmpty()) buildRoute(date)
    }

    suspend fun rebuildRoute(date: LocalDate = today()) {
        user.clearSuggested(date.toString())
        buildRoute(date)
    }

    private suspend fun buildRoute(date: LocalDate) {
        val keep = user.routeOnNow(date.toString())
        val keptTopics = keep.mapNotNull { it.topicId }.toSet()
        val now = System.currentTimeMillis()
        val list = PusulaKoc.suggest(coachInput(date)).filter { it.topicId == null || it.topicId !in keptTopics }
        user.saveRoute(list.mapIndexed { i, s -> RouteItem(date = date.toString(), orderIndex = keep.size + i, kind = s.kind, title = s.title, subjectId = s.subjectId, topicId = s.topicId, sourceId = s.sourceId, minutes = s.minutes, priority = s.priority, reason = s.reason, createdAt = now, updatedAt = now) })
        user.pruneRoute(date.minusDays(30).toString())
    }

    /** Kabul edilen öneri bugünkü programa görev olarak eklenir. */
    suspend fun acceptRoute(id: String) {
        val r = user.routeItem(id) ?: return
        val now = System.currentTimeMillis()
        val p = user.profileNow() ?: UserProfile()
        val tasks = user.tasksBetweenNow(r.date, r.date)
        val last = tasks.maxOfOrNull { t -> t.time.take(2).toInt() * 60 + t.time.takeLast(2).toInt() + t.minutes }
        val start = maxOf(last?.plus(p.breakMinutes) ?: 0, p.startHour * 60).coerceAtMost(23 * 60)
        db.withTransaction {
            user.saveRouteItem(r.copy(status = "ACCEPTED", updatedAt = now))
            user.saveTask(StudyTask(id = "route-$id", date = r.date, time = PlanGenerator.clock(start), subjectId = r.subjectId, topicId = r.topicId, title = r.title, kind = if (r.kind == "WRONGS") "WRONGS" else "STUDY", minutes = r.minutes, reason = r.reason, priority = r.priority, sourceId = r.sourceId, originalDate = r.date, updatedAt = now))
        }
        changed()
    }

    suspend fun rejectRoute(id: String) {
        user.routeItem(id)?.let { user.saveRouteItem(it.copy(status = "REJECTED", updatedAt = System.currentTimeMillis())) }
        changed()
    }

    suspend fun moveRoute(id: String, to: LocalDate) {
        val r = user.routeItem(id) ?: return
        val now = System.currentTimeMillis()
        val next = user.routeOnNow(to.toString()).size
        db.withTransaction {
            user.saveRouteItem(r.copy(status = "MOVED", movedTo = to.toString(), updatedAt = now))
            user.saveRouteItem(r.copy(id = newId(), date = to.toString(), orderIndex = next, status = "SUGGESTED", updatedAt = now, createdAt = now))
        }
        changed()
    }

    suspend fun editRoute(id: String, title: String, minutes: Int) {
        user.routeItem(id)?.let { user.saveRouteItem(it.copy(title = title, minutes = minutes, updatedAt = System.currentTimeMillis())) }
    }

    suspend fun deleteRoute(id: String) { user.deleteRouteItem(id); changed() }

    /* ---------- POMODORO ---------- */

    /** Süresi uygulama kapalıyken dolmuş olsa bile çalışma oturumunu bir kez kaydeder. */
    suspend fun settlePomodoro(now: Long = System.currentTimeMillis()): PomodoroState {
        val s = pomodoro.first()
        if (!s.running || s.endsAt > now) return s
        if (s.phase == "WORK") {
            val id = "pomo-${s.startedAt}"
            saveFocus(id, s, s.workMin)
            val next = s.copy(phase = "BREAK", running = false, endsAt = 0, remainingSec = s.breakMin * 60, startedAt = 0, pendingSessionId = id, note = "")
            savePomodoro(next)
            return next
        }
        val idle = s.copy(phase = "IDLE", running = false, endsAt = 0, remainingSec = 0, startedAt = 0)
        savePomodoro(idle)
        return idle
    }

    /** Planlanan süre ve gerçek süre ayrı tutulur. */
    suspend fun saveFocus(id: String, s: PomodoroState, minutes: Int) {
        user.saveSession(StudySession(id = id, subjectId = s.subjectId.ifEmpty { null }, topicId = s.topicId.ifEmpty { null }, label = s.label.ifEmpty { "Odak" }, startedAt = s.startedAt, minutes = minutes, note = s.note, sourceId = s.sourceId.ifEmpty { null }, plannedMinutes = s.workMin, activity = s.activity))
        if (s.topicId.isNotEmpty()) touchTopic(s.topicId, s.startedAt + minutes * 60_000L)
        if (s.sourceId.isNotEmpty()) user.source(s.sourceId)?.let { user.saveSource(it.copy(lastUsedAt = s.startedAt + minutes * 60_000L)) }
        changed()
    }

    /* ---------- İSTATİSTİK ---------- */

    data class PeriodCompare(val days: Int, val solved: Int, val success: Int, val minutes: Int, val prevSuccess: Int, val prevSolved: Int)

    suspend fun comparePeriod(days: Int): PeriodCompare {
        val t = today().plusDays(1)
        val from = startOf(t.minusDays(days.toLong()))
        val prev = startOf(t.minusDays(days * 2L))
        val end = startOf(t)
        val s = user.solvedBetweenNow(from, end)
        val r = user.rightBetweenNow(from, end)
        val ps = user.solvedBetweenNow(prev, from)
        val pr = user.rightBetweenNow(prev, from)
        return PeriodCompare(days, s, Scoring.percent(r, s), user.minutesBetweenNow(from, end), Scoring.percent(pr, ps), ps)
    }

    /* ---------- RAPORLAR ---------- */

    data class Report(
        val title: String, val from: LocalDate, val to: LocalDate, val minutes: Int, val prevMinutes: Int, val avgMinutes: Int, val activeDays: Int,
        val doneTasks: Int, val openTasks: List<String>, val subjects: List<Pair<String, Int>>, val topics: List<String>, val solved: Int, val correct: Int,
        val wrong: Int, val blank: Int, val sources: List<Pair<String, Int>>, val finishedSources: List<String>, val wrongReviews: Int, val exams: List<MockSummary>,
        val review: WeekReview,
    ) { val hasData: Boolean get() = minutes > 0 || solved > 0 || doneTasks > 0 || exams.isNotEmpty() }

    suspend fun report(from: LocalDate, to: LocalDate, title: String): Report = withContext(Dispatchers.IO) {
        val a = startOf(from)
        val b = startOf(to.plusDays(1))
        val span = java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1
        val sessions = user.sessionsBetween(a, b)
        val prev = user.minutesBetweenNow(startOf(from.minusDays(span)), a)
        val logs = user.logsBetween(a, b)
        val tasks = user.tasksBetweenNow(from.toString(), to.toString())
        val names = content.subjectsNow().associateBy { it.id }
        val topicNames = content.allTopics().associateBy { it.id }
        val bySubject = sessions.groupBy { it.subjectId }.mapNotNull { (k, v) -> k?.let { names[it]?.name }?.let { n -> n to v.sumOf { s -> s.minutes } } }.sortedByDescending { it.second }
        val topicIds = (sessions.mapNotNull { it.topicId } + logs.mapNotNull { it.topicId }).distinct()
        val sources = user.sourcesNow()
        val srcMin = sessions.groupBy { it.sourceId }.mapNotNull { (k, v) -> sources.firstOrNull { it.id == k }?.let { s -> s.name to v.sumOf { x -> x.minutes } } }.sortedByDescending { it.second }
        val finished = sources.filter { it.totalPages > 0 && it.pagesDone >= it.totalPages && it.updatedAt in a until b }.map { it.name }
        val exams = user.mockSummariesNow().filter { it.createdAt in a until b }
        val days = (sessions.map { dateOf(it.startedAt) } + logs.map { dateOf(it.at) }).toSet().size
        val practice = logs.filter { it.origin != "MOCK" }
        val topics = topicIds.mapNotNull { id ->
            topicNames[id]?.let { t ->
                val l = practice.filter { it.topicId == id }
                TopicWeek(t.name, names[t.subjectId]?.name.orEmpty(), l.sumOf { it.solved }, l.sumOf { it.correct }, sessions.filter { it.topicId == id }.sumOf { it.minutes })
            }
        }
        val solvedBySubject = practice.groupBy { it.subjectId }.mapNotNull { (k, v) -> k?.let { names[it]?.name }?.let { n -> n to (v.sumOf { x -> x.solved } to v.sumOf { x -> x.correct }) } }.toMap()
        val now = System.currentTimeMillis()
        val due = content.allTopicRowsNow().filter { it.nextReviewAt != null && it.nextReviewAt <= now }.map { it.name }
        val open = tasks.filter { !it.done && LocalDate.parse(it.date) <= today() }.map { it.title }
        Report(
            title, from, to, sessions.sumOf { it.minutes }, prev, sessions.sumOf { it.minutes } / span.toInt().coerceAtLeast(1), days, tasks.count { it.done }, open,
            bySubject, topics.map { "${it.subjectName} / ${it.name}" }, logs.sumOf { it.solved }, logs.sumOf { it.correct }, logs.sumOf { it.wrong }, logs.sumOf { it.blank },
            srcMin, finished, user.wrongReviewsBetween(a, b), exams,
            WeeklyCoach.build(WeekFacts(topics, bySubject.toMap(), solvedBySubject, due, open, days)),
        )
    }

    /* ---------- KART ---------- */

    suspend fun rateCard(cardId: String, rating: Int, now: Long = System.currentTimeMillis()) {
        val p = user.cardProgress(cardId) ?: FlashcardProgress(cardId)
        val (stage, at) = Repetition.cardOutcome(p.stage, rating, now)
        user.saveCardProgress(p.copy(stage = stage, nextReviewAt = at, learned = rating == 2 && stage >= 2 || p.learned && rating != 0, lastRating = rating, reviewCount = p.reviewCount + 1))
        user.saveCardReview(CardReview(cardId = cardId, at = now, rating = rating))
    }

    suspend fun setCardLearned(cardId: String, learned: Boolean) {
        val p = user.cardProgress(cardId) ?: FlashcardProgress(cardId)
        user.saveCardProgress(p.copy(learned = learned))
    }

    /* ---------- MOTİVASYON ---------- */

    val quotes: List<Quote> by lazy {
        context.assets.open("content/motivation.json").bufferedReader().use { contentJson.decodeFromString(ListSerializer(Quote.serializer()), it.readText()) }
    }

    fun quoteOfDay(date: LocalDate = today()): Quote = quotes[DailyQuote.indexFor(date.toEpochDay(), quotes.size)]

    /** Bugünden geriye giden günlük sözler. */
    fun pastQuotes(days: Int, date: LocalDate = today()): List<Pair<LocalDate, Quote>> =
        (0 until days).map { date.minusDays(it.toLong()) }.map { it to quoteOfDay(it) }

    /* ---------- TOPLU SIFIRLAMA ---------- */

    suspend fun clearUserData() {
        db.withTransaction {
            user.clearLogs(); user.clearWrongReviews(); user.clearPlans(); user.clearTasks(); user.clearSessions()
            user.clearMocks(); user.clearResults(); user.clearProgress(); user.clearWrongs(); user.clearCardProgress()
            user.clearUserCards(); user.clearNotes(); user.clearBookmarks(); user.clearReminders(); user.clearDepartments()
            user.clearUniversities(); user.clearLibrary(); user.clearUniversityRecords(); user.clearProfile()
            user.clearSources(); user.clearRoute(); user.clearRevisions(); user.clearCardReviews(); user.clearItemNotes()
            user.clearNetGoals(); user.clearRestDays()
        }
        context.filesDir.resolve("library").deleteRecursively()
        context.filesDir.resolve("photos").deleteRecursively()
        context.cacheDir.resolve("paylasim").deleteRecursively()
        savePomodoro(PomodoroState())
        // Kişisel değerler de silinir. Tema, yazı boyutu ve süre tercihleri ayar olduğu için kalır.
        updateSettings { it.remove(Keys.diploma); it.remove(Keys.placed); it.remove(Keys.weekly); it.remove(Keys.monthly); it.remove(Keys.lastBackupAt) }
        changed()
    }
}

@Serializable data class Quote(val id: String, val category: String, val text: String)
