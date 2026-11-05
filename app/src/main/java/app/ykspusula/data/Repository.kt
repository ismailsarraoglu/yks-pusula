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
import androidx.paging.Pager
import androidx.paging.PagingConfig
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
    val theme: String = "SYSTEM",
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
    val label: String = "",
    val startedAt: Long = 0,
)

@Serializable data class SectionPart(val subjectId: String, val count: Int)
@Serializable data class SectionConfig(val name: String, val parts: List<SectionPart>)

data class QuestionFilter(
    val exam: String = "",
    val subjectId: String = "",
    val topicId: String = "",
    val difficulty: Int = 0,
    val state: String = "ALL",
)

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
    val pLabel = stringPreferencesKey("p_label")
    val pStarted = longPreferencesKey("p_started")
    val seeded = stringPreferencesKey("seed_version")
}

fun zone(): ZoneId = ZoneId.systemDefault()
fun today(): LocalDate = LocalDate.now(zone())
fun startOf(d: LocalDate): Long = d.atStartOfDay(zone()).toInstant().toEpochMilli()
fun dateOf(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(zone()).toLocalDate()
fun weekStart(d: LocalDate): LocalDate = d.minusDays((d.dayOfWeek.value - 1).toLong())

class Repository(private val context: Context, val db: AppDatabase) {
    val content = db.content()
    val user = db.user()
    private val store get() = context.settingsStore

    /* ---------- AYARLAR ---------- */

    val settings: Flow<AppSettings> = store.data.map { p ->
        AppSettings(
            theme = p[Keys.theme] ?: "SYSTEM",
            fontScale = p[Keys.font] ?: 1f,
            pomodoroWork = p[Keys.pWork] ?: 25,
            pomodoroBreak = p[Keys.pBreak] ?: 5,
            weeklyGoalMinutes = p[Keys.weekly] ?: 0,
            monthlyGoalQuestions = p[Keys.monthly] ?: 0,
            diploma = p[Keys.diploma] ?: "",
            placedLastYear = p[Keys.placed] ?: false,
        )
    }

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
            label = p[Keys.pLabel] ?: "",
            startedAt = p[Keys.pStarted] ?: 0,
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
            p[Keys.pLabel] = s.label
            p[Keys.pStarted] = s.startedAt
        }
    }

    /* ---------- BAŞLANGIÇ İÇERİĞİ ---------- */

    suspend fun ensureSeeded(): Unit = withContext(Dispatchers.IO) {
        val text = context.assets.open("content/base.json").bufferedReader().use { it.readText() }
        val pack = ContentValidator.parse(text)
        val current = store.data.first()[Keys.seeded]
        if (current != pack.pack.version || content.questionCountNow() != pack.questions.size) {
            installPack(pack)
            store.edit { it[Keys.seeded] = pack.pack.version }
        }
    }

    suspend fun installPack(p: ContentPackFile) {
        db.withTransaction {
            content.examConfigsIfMissing(p.examConfigs)
            content.subjects(p.subjects.map { Subject(it.id, it.exam, it.name, it.groupName, it.fields, it.sortOrder, it.language, p.pack.id) })
            content.topics(
                p.topics.map {
                    Topic(
                        it.id, it.subjectId, it.parentId, it.name, it.summary, it.lesson, it.keyPoints, it.example, it.curriculum,
                        it.validFromYear, it.validToYear, it.scope, it.scopeNote, it.estimatedMinutes, it.sortOrder, p.pack.id,
                    )
                }
            )
            content.questions(
                p.questions.map {
                    Question(
                        it.id, it.exam, it.subjectId, it.topicId, it.difficulty, it.stem, it.figure, it.explanation,
                        it.steps.joinToString("\n"), it.examYear, it.curriculum, it.source, it.license, it.generated,
                        ContentValidator.hash(it), p.pack.id,
                    )
                }
            )
            content.options(p.questions.flatMap { q -> q.options.map { AnswerOption(q.id, it.label, it.text, it.label == q.correct) } })
            content.formulas(p.formulas.map { Formula(it.id, it.subjectId, it.subjectName, it.category, it.title, it.body, it.note, p.pack.id) })
            content.flashcards(p.flashcards.map { Flashcard(it.id, it.deck, it.subjectName, it.front, it.back, it.partOfSpeech, it.example, false, p.pack.id) })
            content.pack(ImportedContentPack(p.pack.id, p.pack.title, p.pack.version, p.pack.kind, System.currentTimeMillis(), p.questions.size, p.pack.source, p.pack.license))
        }
    }

    /** Kullanıcının seçtiği içerik paketini doğrular; hatalı veya yinelenen veriyi reddeder. */
    suspend fun importPack(text: String): Pair<Boolean, List<String>> = withContext(Dispatchers.IO) {
        val pack = try {
            ContentValidator.parse(text)
        } catch (e: Exception) {
            return@withContext false to listOf("Dosya geçerli bir içerik paketi değil.")
        }
        val errors = ContentValidator.validate(pack, content.allTopicIds().toSet(), content.allSubjectIds().toSet()).toMutableList()
        pack.questions.forEach { q ->
            val existing = content.idForHash(ContentValidator.hash(q))
            if (existing != null && existing != q.id) errors += "${q.id}: aynı soru zaten ${existing} kimliğiyle kayıtlı"
        }
        if (errors.isNotEmpty()) return@withContext false to errors.take(20)
        installPack(pack)
        true to listOf("${pack.questions.size} soru, ${pack.topics.size} konu içe aktarıldı.")
    }

    /* ---------- SORU ÇÖZME ---------- */

    private fun buildWhere(f: QuestionFilter): Pair<String, List<Any>> {
        val w = ArrayList<String>()
        val a = ArrayList<Any>()
        if (f.exam.isNotEmpty()) { w += "q.exam = ?"; a += f.exam }
        if (f.subjectId.isNotEmpty()) { w += "q.subjectId = ?"; a += f.subjectId }
        if (f.topicId.isNotEmpty()) { w += "q.topicId = ?"; a += f.topicId }
        if (f.difficulty != 0) { w += "q.difficulty = ?"; a += f.difficulty }
        when (f.state) {
            "SOLVED" -> w += "q.id IN (SELECT questionId FROM QuestionAttempt WHERE selected IS NOT NULL)"
            "UNSOLVED" -> w += "q.id NOT IN (SELECT questionId FROM QuestionAttempt WHERE selected IS NOT NULL)"
            "WRONG" -> w += "q.id IN (SELECT questionId FROM WrongQuestion)"
            "MARKED" -> w += "q.id IN (SELECT refId FROM Bookmark WHERE kind = 'QUESTION')"
            "LATER" -> w += "q.id IN (SELECT refId FROM Bookmark WHERE kind = 'LATER')"
        }
        return (if (w.isEmpty()) "" else "WHERE " + w.joinToString(" AND ")) to a
    }

    fun pagedQuestions(f: QuestionFilter) = Pager(PagingConfig(pageSize = 30, enablePlaceholders = false)) {
        val (where, args) = buildWhere(f)
        content.pageQuestions(rawQuestionQuery("SELECT q.* FROM Question q $where ORDER BY q.topicId, q.difficulty, q.id", args))
    }.flow

    suspend fun questionIds(f: QuestionFilter, shuffleSeed: Long, limit: Int): List<String> {
        val (where, args) = buildWhere(f)
        val ids = content.questionIds(rawQuestionQuery("SELECT q.id FROM Question q $where ORDER BY q.topicId, q.difficulty, q.id", args))
        val ordered = if (shuffleSeed != 0L) ids.shuffled(java.util.Random(shuffleSeed)) else ids
        return if (limit > 0) ordered.take(limit) else ordered
    }

    /** Tek bir cevabın etkisi: deneme kaydı, yanlışlar defteri, konu ilerlemesi ve istatistikler aynı işlemde güncellenir. */
    suspend fun answer(q: QuestionFull, selectedLabel: String, source: String, givenText: String, now: Long = System.currentTimeMillis()) {
        val right = q.correctLabel == selectedLabel
        db.withTransaction {
            user.attempt(QuestionAttempt(questionId = q.q.id, topicId = q.q.topicId, subjectId = q.q.subjectId, selected = selectedLabel, isCorrect = right, answeredAt = now, source = source))
            applyWrongBook(q, right, givenText, source, now)
            touchTopic(q.q.topicId, now)
        }
    }

    private suspend fun applyWrongBook(q: QuestionFull, right: Boolean, givenText: String, source: String, now: Long) {
        val existing = user.wrongOf(q.q.id)
        val correctText = q.options.first { it.isCorrect }.text
        if (!right) {
            val o = Repetition.onWrong(now)
            user.saveWrong(
                existing?.copy(
                    givenAnswer = givenText, lastWrongAt = now, nextReviewAt = o.nextReviewAt, stage = o.stage,
                    wrongCount = existing.wrongCount + 1, resolved = false,
                ) ?: WrongQuestion(q.q.id, q.q.topicId, q.q.subjectId, givenText, correctText, now, now, o.nextReviewAt)
            )
        } else if (existing != null && !existing.resolved && source == "REVIEW") {
            val o = Repetition.onCorrect(existing.stage, now)
            user.saveWrong(existing.copy(stage = o.stage, nextReviewAt = o.nextReviewAt, resolved = o.resolved))
        }
    }

    /** Doğru çözülmüş bir soru da istenirse deftere eklenir. */
    suspend fun addToWrongBook(q: QuestionFull, now: Long = System.currentTimeMillis()) {
        if (user.wrongOf(q.q.id) != null) return
        val correct = q.options.first { it.isCorrect }.text
        user.saveWrong(WrongQuestion(q.q.id, q.q.topicId, q.q.subjectId, "Elle eklendi", correct, now, now, Repetition.onWrong(now).nextReviewAt, wrongCount = 0))
    }

    private suspend fun touchTopic(topicId: String, now: Long) {
        val p = user.progressNow(topicId) ?: TopicProgress(topicId)
        user.saveProgress(p.copy(status = if (p.status < 2) 2 else p.status, lastStudiedAt = now))
    }

    suspend fun setTopicStatus(topicId: String, status: Int) {
        val now = System.currentTimeMillis()
        val p = user.progressNow(topicId) ?: TopicProgress(topicId)
        user.saveProgress(
            p.copy(
                status = status,
                lastStudiedAt = if (status > 0) now else p.lastStudiedAt,
                completedAt = if (status == 3) now else null,
                nextReviewAt = if (status == 3) Repetition.nextAt(2, now) else p.nextReviewAt,
            )
        )
    }

    suspend fun setTopicReview(topicId: String, at: Long?) {
        val p = user.progressNow(topicId) ?: TopicProgress(topicId)
        user.saveProgress(p.copy(nextReviewAt = at))
    }

    suspend fun updateWrong(id: String, reason: String, note: String) {
        user.wrongOf(id)?.let { user.saveWrong(it.copy(reason = reason, note = note)) }
    }

    suspend fun toggleBookmark(kind: String, id: String, on: Boolean) {
        if (on) user.saveBookmark(Bookmark(kind, id, System.currentTimeMillis())) else user.deleteBookmark(kind, id)
    }

    /* ---------- DENEMELER ---------- */

    fun sectionsOf(c: ExamConfiguration): List<SectionConfig> =
        contentJson.decodeFromString(ListSerializer(SectionConfig.serializer()), c.sectionsJson)

    data class ExamDraft(val title: String, val kind: String, val exam: String, val sections: List<PickedSection>, val minutes: Int)

    suspend fun availableBySubject(subjectIds: Collection<String>): Map<String, List<String>> =
        subjectIds.toSet().associateWith { content.idsOfSubject(it) }

    suspend fun seenQuestionIds(): Set<String> = user.attemptsNow().map { it.questionId }.toSet()

    suspend fun draftGeneral(code: String, sectionNames: Set<String>, ydtSubject: String?): ExamDraft {
        val c = user.examConfig(code) ?: error("Sınav ayarı yok: $code")
        val sections = sectionsOf(c).filter { sectionNames.isEmpty() || it.name in sectionNames }.map { s ->
            SectionPlan(s.name, s.parts.map { p -> PartPlan(if (code == "YDT" && ydtSubject != null) ydtSubject else p.subjectId, p.count) })
        }
        val avail = availableBySubject(sections.flatMap { it.parts }.map { it.subjectId })
        val picked = ExamBuilder.pick(sections, avail, seenQuestionIds(), System.nanoTime())
        val requested = picked.sumOf { it.requested }
        val actual = picked.sumOf { it.questionIds.size }
        return ExamDraft("${c.title} genel denemesi", "GENERAL", code, picked, ExamBuilder.scaledDuration(c.durationMin, requested, actual))
    }

    suspend fun draftBranch(subject: Subject, count: Int, minutes: Int): ExamDraft {
        val ids = content.idsOfSubject(subject.id)
        val picked = ExamBuilder.pick(listOf(SectionPlan(subject.name, listOf(PartPlan(subject.id, count)))), mapOf(subject.id to ids), seenQuestionIds(), System.nanoTime())
        return ExamDraft("${subject.name} branş denemesi", "BRANCH", subject.exam, picked, ExamBuilder.scaledDuration(minutes, count, picked.sumOf { it.questionIds.size }))
    }

    suspend fun draftTopics(title: String, kind: String, exam: String, topicIds: List<String>, count: Int, minutes: Int): ExamDraft {
        val ids = content.idsOfTopics(topicIds)
        val picked = ExamBuilder.pick(listOf(SectionPlan(title, listOf(PartPlan("x", count)))), mapOf("x" to ids), seenQuestionIds(), System.nanoTime())
        return ExamDraft(title, kind, exam, picked, ExamBuilder.scaledDuration(minutes, count, picked.sumOf { it.questionIds.size }))
    }

    suspend fun weakTopicIds(limit: Int): List<String> = user.topicStatsNow()
        .filter { it.total >= 3 }.sortedBy { it.right.toDouble() / it.total }.take(limit).map { it.key }

    suspend fun startExam(d: ExamDraft, now: Long = System.currentTimeMillis()): String {
        val id = newId()
        val records = ArrayList<AnswerRecord>()
        var i = 0
        d.sections.forEach { s -> s.questionIds.forEach { q -> records += AnswerRecord(id, q, i++, s.name) } }
        require(records.isNotEmpty()) { "Bu deneme için soru yok" }
        db.withTransaction {
            user.saveMock(MockExam(id, d.title, d.kind, d.exam, now, now + d.minutes * 60_000L, d.minutes * 60, "RUNNING"))
            user.saveRecords(records)
        }
        return id
    }

    suspend fun finishExam(id: String, now: Long = System.currentTimeMillis()) {
        db.withTransaction {
            val m = user.mock(id) ?: return@withTransaction
            if (m.status != "RUNNING") return@withTransaction
            val records = user.records(id)
            val questions = content.fullList(records.map { it.questionId }).associateBy { it.q.id }
            val divisor = user.examConfig(m.exam)?.wrongDivisor ?: 4.0
            val rows = LinkedHashMap<String, IntArray>()
            records.forEach { r ->
                val q = questions[r.questionId] ?: return@forEach
                val row = rows.getOrPut(r.sectionName) { IntArray(3) }
                val right = r.selected != null && r.selected == q.correctLabel
                when {
                    r.selected == null -> row[2]++
                    right -> row[0]++
                    else -> row[1]++
                }
                user.attempt(QuestionAttempt(questionId = q.q.id, topicId = q.q.topicId, subjectId = q.q.subjectId, selected = r.selected, isCorrect = right, answeredAt = now, source = "MOCK", mockExamId = id))
                if (r.selected != null) {
                    if (!right) applyWrongBook(q, false, optionText(q, r.selected), "MOCK", now)
                    touchTopic(q.q.topicId, now)
                }
            }
            var i = 0
            user.saveResults(rows.map { (section, v) -> MockExamResult(id, section, v[0], v[1], v[2], Scoring.net(v[0], v[1], divisor), i++) })
            val spent = ((minOf(now, m.endsAt) - m.createdAt) / 60_000L).toInt().coerceAtLeast(0)
            user.saveMock(m.copy(status = "DONE", spentMinutes = spent))
            if (spent > 0) user.saveSession(StudySession(label = m.title, startedAt = m.createdAt, minutes = spent, kind = "MOCK"))
        }
    }

    private fun optionText(q: QuestionFull, label: String) = q.options.firstOrNull { it.label == label }?.text ?: label

    /** Süresi dolmuş denemeler uygulama açıldığında otomatik bitirilir. */
    suspend fun finishExpiredExams(now: Long = System.currentTimeMillis()) {
        user.runningNow().filter { it.endsAt <= now }.forEach { finishExam(it.id, it.endsAt) }
    }

    suspend fun saveExternalExam(
        title: String, publisher: String, date: LocalDate, exam: String, spentMinutes: Int,
        rows: List<Triple<String, IntArray, Int>>,
    ): String {
        val id = newId()
        val divisor = user.examConfig(exam)?.wrongDivisor ?: 4.0
        val at = startOf(date) + 12 * 3_600_000L
        db.withTransaction {
            user.saveMock(MockExam(id, title, "EXTERNAL", exam, at, at, spentMinutes * 60, "DONE", true, publisher, date.toString(), spentMinutes))
            user.saveResults(rows.mapIndexed { i, (name, v, _) -> MockExamResult(id, name, v[0], v[1], v[2], Scoring.net(v[0], v[1], divisor), i) })
            if (spentMinutes > 0) user.saveSession(StudySession(label = title, startedAt = at, minutes = spentMinutes, kind = "MOCK"))
        }
        return id
    }

    suspend fun deleteExam(id: String) {
        db.withTransaction {
            user.deleteResults(id); user.deleteRecords(id); user.deleteMockAttempts(id); user.deleteMock(id)
        }
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
        val plan = StudyPlan(title = "Otomatik plan", startDate = start.toString(), endDate = start.plusDays(6).toString(), isAuto = true, createdAt = System.currentTimeMillis())
        db.withTransaction {
            user.clearPendingAuto(start.toString())
            user.savePlan(plan)
            user.saveTasks(tasks.map { StudyTask(planId = plan.id, date = it.date.toString(), time = it.time, subjectId = it.subjectId, topicId = it.topicId, title = it.title, kind = it.kind, minutes = it.minutes, questionGoal = it.questionGoal) })
        }
        return tasks.size
    }

    suspend fun toggleTask(t: StudyTask, done: Boolean) {
        val now = System.currentTimeMillis()
        db.withTransaction {
            user.saveTask(t.copy(done = done, doneAt = if (done) now else null))
            if (t.topicId != null && done) touchTopic(t.topicId, now)
        }
    }

    /* ---------- POMODORO ---------- */

    /** Süresi uygulama kapalıyken dolmuş olsa bile çalışma oturumunu bir kez kaydeder. */
    suspend fun settlePomodoro(now: Long = System.currentTimeMillis()): PomodoroState {
        val s = pomodoro.first()
        if (!s.running || s.endsAt > now) return s
        if (s.phase == "WORK") {
            user.saveSession(StudySession(id = "pomo-${s.startedAt}", subjectId = s.subjectId.ifEmpty { null }, topicId = s.topicId.ifEmpty { null }, label = s.label.ifEmpty { "Odak" }, startedAt = s.startedAt, minutes = s.workMin))
            if (s.topicId.isNotEmpty()) touchTopic(s.topicId, s.endsAt)
            val next = s.copy(phase = "BREAK", running = false, endsAt = 0, remainingSec = s.breakMin * 60, startedAt = 0)
            savePomodoro(next)
            return next
        }
        val idle = s.copy(phase = "IDLE", running = false, endsAt = 0, remainingSec = 0, startedAt = 0)
        savePomodoro(idle)
        return idle
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

    suspend fun adviceNow(): List<Advice> {
        val mocks = user.mockSummariesNow()
        val points = ArrayList<SectionPoint>()
        mocks.forEach { m -> user.results(m.id).forEach { points += SectionPoint("${m.exam} ${it.section}", m.createdAt, it.net) } }
        val names = content.allTopics().associateBy { it.id }
        val topics = user.topicStatsNow().mapNotNull { s -> names[s.key]?.let { TopicScore(it.name, s.total, s.right) } }
        val now = System.currentTimeMillis()
        val week = weekStart(today())
        val weekly = user.minutesBetweenNow(startOf(week), now + 1)
        val last = user.minutesBetweenNow(startOf(week.minusDays(7)), startOf(week))
        val activeDays = user.activeDays().first()
        val inactive = activeDays.lastOrNull()?.let { java.time.temporal.ChronoUnit.DAYS.between(LocalDate.parse(it), today()).toInt() }
        return Advisor.build(points, topics, user.wrongDueNow(now), user.cardsDueCountNow(now), inactive, weekly, last)
    }

    /* ---------- KART ---------- */

    suspend fun rateCard(cardId: String, rating: Int, now: Long = System.currentTimeMillis()) {
        val p = user.cardProgress(cardId) ?: FlashcardProgress(cardId)
        val (stage, at) = Repetition.cardOutcome(p.stage, rating, now)
        user.saveCardProgress(p.copy(stage = stage, nextReviewAt = at, learned = rating == 2 && stage >= 2 || p.learned && rating != 0, lastRating = rating, reviewCount = p.reviewCount + 1))
    }

    suspend fun setCardLearned(cardId: String, learned: Boolean) {
        val p = user.cardProgress(cardId) ?: FlashcardProgress(cardId)
        user.saveCardProgress(p.copy(learned = learned))
    }

    /* ---------- TOPLU SIFIRLAMA ---------- */

    suspend fun clearUserData() {
        db.withTransaction {
            user.clearAttempts(); user.clearRecords(); user.clearPlans(); user.clearTasks(); user.clearSessions()
            user.clearMocks(); user.clearResults(); user.clearProgress(); user.clearWrongs(); user.clearCardProgress()
            user.clearUserCards(); user.clearNotes(); user.clearBookmarks(); user.clearReminders(); user.clearDepartments()
            user.clearUniversities(); user.clearLibrary(); user.clearUniversityRecords(); user.clearProfile()
        }
        context.filesDir.resolve("library").deleteRecursively()
        savePomodoro(PomodoroState())
    }
}
