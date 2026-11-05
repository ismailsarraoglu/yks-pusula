package app.ykspusula

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import app.ykspusula.data.*
import app.ykspusula.domain.PickedSection
import app.ykspusula.domain.Repetition
import app.ykspusula.domain.Scoring
import app.ykspusula.notify.Notifier
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class DataFlowTest {
    @get:Rule val perm: GrantPermissionRule =
        if (android.os.Build.VERSION.SDK_INT >= 33) GrantPermissionRule.grant(android.Manifest.permission.POST_NOTIFICATIONS) else GrantPermissionRule.grant()
    private val app get() = ApplicationProvider.getApplicationContext<Context>() as YksApp
    private val r get() = app.repo

    @Before fun setup() = runBlocking {
        app.ready.first { it }
        r.clearUserData()
        r.user.saveProfile(UserProfile(onboarded = true, name = "Test", dailyMinutes = 240))
    }

    private suspend fun firstQ(subject: String): QuestionFull = r.content.full(r.content.idsOfSubject(subject).first())!!
    private fun wrongLabel(q: QuestionFull) = q.options.first { !it.isCorrect }.label

    @Test fun seedContentValid() = runBlocking {
        val questions = r.content.allQuestions()
        assertTrue("soru sayısı ${questions.size}", questions.size >= 500)
        assertEquals(questions.size, r.content.questionCountNow())
        questions.chunked(300).forEach { part ->
            r.content.fullList(part.map { it.id }).forEach { q ->
                assertEquals(q.q.id, 1, q.options.count { it.isCorrect })
                assertEquals(q.q.id, 5, q.options.size)
            }
        }
        val subjects = r.content.subjectsNow()
        listOf("TYT", "AYT", "YDT").forEach { e -> assertTrue(subjects.any { it.exam == e }) }
        assertEquals(setOf("TYT", "AYT", "YDT"), r.user.examConfigsNow().map { it.code }.toSet())
        assertEquals("2027-06-19", r.user.examConfig("TYT")!!.examDate)
    }

    @Test fun wrongAnswerUpdatesEverything() = runBlocking {
        val q = firstQ("tyt-mat")
        val now = System.currentTimeMillis()
        r.answer(q, wrongLabel(q), "BANK", "x", now)
        val w = r.user.wrongOf(q.q.id)!!
        assertEquals(now + Repetition.DAY_MS, w.nextReviewAt)
        assertEquals(1, r.user.attemptsNow().size)
        assertEquals(2, r.user.progressNow(q.q.topicId)!!.status)
        assertEquals(1, r.user.solvedTotal().first())
        val stat = r.user.topicStatsNow().first { it.key == q.q.topicId }
        assertEquals(1, stat.total); assertEquals(0, stat.right)
        r.answer(q, q.correctLabel, "BANK", "y", now + 1)
        assertEquals(50, r.user.topicStatsNow().first { it.key == q.q.topicId }.let { Scoring.percent(it.right, it.total) })
        assertTrue(r.user.wrongOf(q.q.id)!!.stage == 0)
    }

    @Test fun repetitionThroughReview() = runBlocking {
        val q = firstQ("tyt-tur")
        val t0 = 1_000_000_000L
        r.answer(q, wrongLabel(q), "BANK", "x", t0)
        var t = t0
        listOf(3, 7, 14, 30).forEach { days ->
            t += Repetition.DAY_MS
            r.answer(q, q.correctLabel, "REVIEW", "y", t)
            assertEquals(t + days * Repetition.DAY_MS, r.user.wrongOf(q.q.id)!!.nextReviewAt)
        }
        r.answer(q, q.correctLabel, "REVIEW", "y", t + 1)
        assertTrue(r.user.wrongOf(q.q.id)!!.resolved)
    }

    @Test fun examAutosaveAndNetMath() = runBlocking {
        val mat = r.content.idsOfSubject("tyt-mat").take(8)
        val id = r.startExam(Repository.ExamDraft("Test", "BRANCH", "TYT", listOf(PickedSection("Temel Matematik", 8, mat)), 10))
        val rec = r.user.records(id)
        assertEquals(8, rec.size)
        val qs = r.content.fullList(mat).associateBy { it.q.id }
        // 4 doğru, 2 yanlış, 2 boş
        rec.forEachIndexed { i, x ->
            val q = qs[x.questionId]!!
            when { i < 4 -> r.user.saveRecord(x.copy(selected = q.correctLabel)); i < 6 -> r.user.saveRecord(x.copy(selected = wrongLabel(q))) }
        }
        assertEquals(6, r.user.records(id).count { it.selected != null })
        assertEquals("RUNNING", r.user.mock(id)!!.status)
        r.finishExam(id)
        val res = r.user.results(id).single()
        assertEquals(4, res.correct); assertEquals(2, res.wrong); assertEquals(2, res.blank)
        assertEquals(3.5, res.net, 1e-9)
        assertEquals("DONE", r.user.mock(id)!!.status)
        assertEquals(2, r.user.wrongsNow().size)
        r.finishExam(id)
        assertEquals("İkinci bitirme etkisiz olmalı", 8, r.user.attemptsNow().size)
    }

    @Test fun expiredExamFinishesItself() = runBlocking {
        val ids = r.content.idsOfSubject("tyt-geo").take(3)
        val past = System.currentTimeMillis() - 3_600_000
        val id = r.startExam(Repository.ExamDraft("Eski", "BRANCH", "TYT", listOf(PickedSection("Geometri", 3, ids)), 10), past)
        r.finishExpiredExams()
        assertEquals("DONE", r.user.mock(id)!!.status)
        assertEquals(3, r.user.results(id).single().blank)
    }

    @Test fun fullTytDraftFillsAllSections() = runBlocking {
        val d = r.draftGeneral("TYT", emptySet(), null)
        assertEquals(120, d.sections.sumOf { it.questionIds.size })
        assertEquals(165, d.minutes)
        assertEquals(120, d.sections.flatMap { it.questionIds }.toSet().size)
    }

    @Test fun externalExam() = runBlocking {
        val id = r.saveExternalExam("Yayın 1", "Yayınevi", LocalDate.of(2026, 11, 1), "TYT", 165, listOf(Triple("Türkçe", intArrayOf(30, 8, 2), 40)))
        assertEquals(28.0, r.user.results(id).single().net, 1e-9)
        assertEquals(1, r.user.mockSummariesNow().size)
        assertEquals(165, r.user.minutesTotal().first())
    }

    @Test fun planGeneratesAndKeepsDone() = runBlocking {
        val n = r.generatePlan(LocalDate.of(2026, 11, 9))
        assertTrue(n > 0)
        val all = r.user.allTasks()
        assertTrue(all.groupBy { it.date }.values.all { it.sumOf { t -> t.minutes } <= 240 })
        val first = all.first()
        r.toggleTask(first, true)
        r.generatePlan(LocalDate.of(2026, 11, 9))
        assertTrue(r.user.allTasks().any { it.id == first.id && it.done })
    }

    @Test fun pomodoroSurvivesClosedApp() = runBlocking {
        val start = System.currentTimeMillis() - 30 * 60_000
        r.savePomodoro(PomodoroState(phase = "WORK", running = true, endsAt = start + 25 * 60_000, remainingSec = 1500, workMin = 25, breakMin = 5, label = "Test", startedAt = start))
        r.settlePomodoro()
        r.settlePomodoro()
        assertEquals(1, r.user.sessionsNow().size)
        assertEquals(25, r.user.minutesTotal().first())
        assertEquals("BREAK", r.pomodoro.first().phase)
    }

    @Test fun backupRoundTrip() = runBlocking {
        val q = firstQ("tyt-mat")
        r.answer(q, wrongLabel(q), "BANK", "x")
        r.user.saveNote(UserNote(title = "Not", body = "Gövde", createdAt = 1, updatedAt = 1))
        r.user.saveCard(Flashcard("u1", "Kartlarım", "", "ön", "arka", isUser = true))
        val text = app.backup.export("t")
        r.clearUserData()
        assertEquals(0, r.user.notesNow().size)
        val ok = BackupCodec.decode(text) as BackupCheck.Ok
        app.backup.restore(ok.file.data, true, "t")
        assertEquals(1, r.user.notesNow().size)
        assertEquals(1, r.user.wrongsNow().size)
        assertEquals(1, r.user.attemptsNow().size)
        assertEquals(1, r.user.userCardsNow().size)
        assertEquals("Test", r.user.profileNow()!!.name)
        app.backup.restore(ok.file.data, false, "t")
        assertEquals("Birleştirme yineleme üretmemeli", 1, r.user.notesNow().size)
    }

    @Test fun backupRejectsBadFiles() {
        assertTrue(BackupCodec.decode("bozuk") is BackupCheck.Bad)
        assertTrue(BackupCodec.decode("{\"a\":1}") is BackupCheck.Bad)
        val good = BackupCodec.encode(BackupData(notes = listOf(UserNote(id = "n", title = "A", body = "B", createdAt = 1, updatedAt = 1))), 1, "t")
        assertTrue(BackupCodec.decode(good) is BackupCheck.Ok)
        assertTrue("Değiştirilmiş dosya", BackupCodec.decode(good.replace("\"title\":\"A\"", "\"title\":\"C\"")) is BackupCheck.Bad)
        assertTrue("Yeni sürüm", BackupCodec.decode(good.replace("\"version\":1", "\"version\":99")) is BackupCheck.Bad)
    }

    @Test fun importRejectsInvalidPack() = runBlocking {
        assertFalse(r.importPack("bozuk").first)
        val base = app.assets.open("content/base.json").bufferedReader().use { it.readText() }
        val q = ContentValidator.parse(base).questions.first()
        val dup = ContentPackFile(PackHeader("p1", "Test", "1"), questions = listOf(q.copy(id = "yeni-id")))
        val text = contentJson.encodeToString(ContentPackFile.serializer(), dup)
        val res = r.importPack(text)
        assertFalse(res.first)
        assertTrue(res.second.joinToString(), res.second.any { "aynı soru" in it })
        val twoRight = ContentPackFile(PackHeader("p2", "Test", "1"), questions = listOf(q.copy(id = "z", stem = "Yeni soru", correct = "Z")))
        assertFalse(r.importPack(contentJson.encodeToString(ContentPackFile.serializer(), twoRight)).first)
    }

    @Test fun universityPackNeedsSource() {
        assertTrue(UniversityImport.errors(UniversityImport.parse("a.csv", "universite;sehir;bolum\nX;Y;Z")).isNotEmpty())
        val ok = UniversityImport.parse("a.csv", "#kaynak=ÖSYM\n#surum=2026\nuniversite;sehir;bolum\nX;Y;Z")
        assertTrue(UniversityImport.errors(ok).isEmpty())
    }

    @Test fun notificationWorksOffline() = runBlocking {
        val m = app.getSystemService(NotificationManager::class.java)
        m.cancelAll()
        Notifier.show(app, 777, "Test", "Yerel bildirim")
        Thread.sleep(800)
        assertTrue(m.activeNotifications.any { it.id == 777 })
        val rem = Reminder("DAILY", enabled = true, hour = 20)
        r.user.saveReminder(rem)
        Notifier.schedule(app, rem)
        Notifier.fire(app, "DAILY")
        Thread.sleep(800)
        assertTrue(m.activeNotifications.any { it.id == 100 })
    }

    @Test fun resetClearsUserDataOnly() = runBlocking {
        val before = r.content.questionCountNow()
        val q = firstQ("tyt-mat")
        r.answer(q, wrongLabel(q), "BANK", "x")
        r.clearUserData()
        assertEquals(before, r.content.questionCountNow())
        assertEquals(0, r.user.attemptsNow().size)
        assertNull(r.user.profileNow())
    }

    @Test fun manifestHasNoNetworkPermission() {
        val info = app.packageManager.getPackageInfo(app.packageName, android.content.pm.PackageManager.GET_PERMISSIONS)
        val perms = info.requestedPermissions.orEmpty().toList()
        assertFalse(perms.toString(), perms.any { it.endsWith(".INTERNET") || it.endsWith("ACCESS_NETWORK_STATE") })
    }
}
