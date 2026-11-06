package app.ykspusula

import android.app.NotificationManager
import android.content.Context
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import app.ykspusula.data.*
import app.ykspusula.domain.Repetition
import app.ykspusula.monetize.AdPlacement
import app.ykspusula.monetize.PurchaseQuery
import app.ykspusula.monetize.REMOVE_ADS_PRODUCT
import app.ykspusula.notify.Notifier
import app.ykspusula.report.ReportExporter
import app.ykspusula.widget.Widgets
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
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

    private suspend fun source(pages: Int = 300): StudySource {
        val s = StudySource(name = "Matematik Soru Bankası", subjectId = "tyt-mat", totalPages = pages, createdAt = 1, updatedAt = 1)
        r.user.saveSource(s)
        return s
    }

    @Test fun seedContentKept() = runBlocking {
        assertEquals(26, r.content.subjectsNow().size)
        assertEquals(261, r.content.topicCountNow())
        assertEquals(236, r.content.formulaCountNow())
        assertEquals(411, r.content.cardCountNow())
        assertEquals(3, r.user.examConfigsNow().size)
        assertEquals(2027, r.user.examConfig("TYT")!!.examYear)
    }

    /** Spesifikasyondaki örnek: 38 soru, 30 doğru, 8 yanlış, 55 dakika, sayfa 45-52. */
    @Test fun studyLogFeedsEverything() = runBlocking {
        val s = source()
        val topic = "tyt-mat-kumeler"
        r.logStudy(StudyEntry("tyt-mat", topic, s.id, 45, 52, 30, 8, 0, 55))
        val d = today()
        val from = startOf(d); val to = startOf(d.plusDays(1))
        assertEquals(38, r.user.solvedBetween(from, to).first())
        assertEquals(55, r.user.minutesBetween(from, to).first())
        val src = r.user.source(s.id)!!
        assertEquals(52, src.lastPage); assertEquals(8, src.pagesDone)
        val row = r.user.sourceRows().first().single()
        assertEquals(38, row.solved); assertEquals(30, row.correct); assertEquals(55, row.minutes)
        val t = r.content.topicRows("tyt-mat").first().first { it.id == topic }
        assertEquals(38, t.total); assertEquals(30, t.right); assertEquals(55, t.minutes)
        assertEquals(1, r.user.progressNow(topic)!!.status)
        assertTrue(r.user.subjectStatsSince(from).any { it.key == "tyt-mat" && it.total == 38 })
        val rep = r.report(d.minusDays(1), d, "Rapor")
        assertEquals(55, rep.minutes); assertEquals(38, rep.solved)
        assertTrue(rep.sources.single().first == s.name)
    }

    @Test fun deletingSourceKeepsStatistics() = runBlocking {
        val s = source()
        r.logStudy(StudyEntry("tyt-mat", null, s.id, null, null, 5, 5, 0, 20))
        r.user.deleteSource(s.id)
        assertEquals(10, r.user.solvedTotal().first())
        assertNull(r.user.logsNow().single().sourceId)
    }

    @Test fun manualWrongAndRepetition() = runBlocking {
        val t0 = 1_700_000_000_000L
        val w = r.newWrong("tyt-tur", "tyt-tur-paragraf", null, "Paragraf Kitabı", 34, "12", "Dikkatsizlik", "Not", null, null, t0)
        r.saveWrong(w)
        assertEquals(t0 + Repetition.DAY_MS, r.user.wrong(w.id)!!.nextReviewAt)
        r.reviewWrong(w.id, 2, t0 + Repetition.DAY_MS)
        assertEquals(1, r.user.wrong(w.id)!!.stage)
        r.reviewWrong(w.id, 0, t0 + 2 * Repetition.DAY_MS)
        assertEquals(0, r.user.wrong(w.id)!!.stage)
        assertEquals(2, r.user.wrong(w.id)!!.wrongCount)
        repeat(5) { r.reviewWrong(w.id, 2, t0 + (3 + it) * Repetition.DAY_MS) }
        assertTrue(r.user.wrong(w.id)!!.resolved)
        assertEquals(7, r.user.wrongReviewsNow().size)
        r.deleteWrong(w.id)
        assertEquals(0, r.user.wrongReviewsNow().size)
    }

    @Test fun examSessionStopwatchAndResults() = runBlocking {
        val t0 = 1_800_000_000_000L
        val id = r.startExamSession("TYT 1", "GENERAL", "TYT", 165, null, t0)
        r.pauseExam(id, t0 + 600_000)
        r.resumeExam(id, t0 + 900_000)
        assertEquals(1200, r.elapsedSec(r.user.mock(id)!!, t0 + 1_500_000))
        r.stopExam(id, t0 + 1_500_000)
        assertEquals("ENTRY", r.user.mock(id)!!.status)
        assertEquals(20, r.user.mock(id)!!.spentMinutes)
        r.saveExamResults(id, "TYT 1", "Yayın", today(), 20, listOf(SectionEntry("Türkçe", 30, 8, 2), SectionEntry("Temel Matematik", 4, 2, 34)), "tyt-mat-kumeler", "not")
        val res = r.user.results(id)
        assertEquals(28.0, res.first { it.section == "Türkçe" }.net, 1e-9)
        assertEquals(3.5, res.first { it.section == "Temel Matematik" }.net, 1e-9)
        assertEquals("DONE", r.user.mock(id)!!.status)
        assertEquals(80, r.user.solvedTotal().first())
        assertTrue(r.user.progressNow("tyt-mat-kumeler")!!.nextReviewAt != null)
        assertEquals(1, r.examAnalysis("TYT").examCount)
        r.deleteExam(id)
        assertEquals(0, r.user.solvedTotal().first())
        assertTrue(r.user.resultsNow().isEmpty())
    }

    @Test fun planKeepsDoneAndSnapshots() = runBlocking {
        val day = LocalDate.of(2027, 1, 4)
        assertTrue(r.generatePlan(day) > 0)
        val t = r.user.tasksBetweenNow(day.toString(), day.plusDays(6).toString()).first()
        r.toggleTask(t, true)
        r.generatePlan(day)
        assertTrue(r.user.allTasks().any { it.id == t.id && it.done })
        assertTrue(r.user.revisionsNow().isNotEmpty())
    }

    @Test fun replanMovesAndKeepsHistory() = runBlocking {
        val d = today()
        r.user.saveProfile(UserProfile(onboarded = true, dailyMinutes = 240, maxDailyMinutes = 240, studyDays = "1,2,3,4,5,6,7"))
        val done = StudyTask(date = d.minusDays(1).toString(), time = "09:00", title = "Bitti", done = true, doneAt = 1)
        val miss = StudyTask(date = d.minusDays(1).toString(), time = "10:00", title = "Kaçan", priority = 1)
        r.user.saveTasks(listOf(done, miss))
        val p = r.previewReplan(false, d)
        assertEquals(miss.id, p.moves.single().id)
        assertEquals(d.minusDays(1).toString(), r.user.task(miss.id)!!.date)
        r.applyReplan(p.moves, false, d)
        val moved = r.user.task(miss.id)!!
        assertTrue(LocalDate.parse(moved.date) >= d)
        assertEquals(1, moved.movedCount); assertEquals(d.minusDays(1).toString(), moved.originalDate)
        assertTrue(r.user.task(done.id)!!.done)
        assertTrue(r.user.revisionsNow().isNotEmpty())
        assertTrue(r.user.missedTasksNow(d.minusDays(14).toString(), d.toString()).isEmpty())
    }

    @Test fun routeDecisionsPersist() = runBlocking {
        val d = today()
        r.ensureRoute(d)
        val list = r.user.routeOnNow(d.toString())
        assertTrue(list.isNotEmpty())
        assertTrue(list.all { it.reason.isNotBlank() })
        r.ensureRoute(d)
        assertEquals(list.map { it.id }, r.user.routeOnNow(d.toString()).map { it.id })
        val a = list[0]
        r.acceptRoute(a.id)
        assertEquals("ACCEPTED", r.user.routeItem(a.id)!!.status)
        assertTrue(r.user.tasksBetweenNow(d.toString(), d.toString()).any { it.id == "route-${a.id}" })
        if (list.size > 1) {
            r.rejectRoute(list[1].id)
            r.rebuildRoute(d)
            assertEquals("REJECTED", r.user.routeItem(list[1].id)!!.status)
            assertTrue(r.user.routeOnNow(d.toString()).filter { it.status == "SUGGESTED" }.none { it.topicId == list[1].topicId && list[1].topicId != null })
        }
        r.moveRoute(a.id, d.plusDays(1))
        assertEquals(1, r.user.routeOnNow(d.plusDays(1).toString()).size)
    }

    @Test fun pomodoroSurvivesClosedAppAndRecordsPlan() = runBlocking {
        val start = System.currentTimeMillis() - 30 * 60_000
        r.savePomodoro(PomodoroState(phase = "WORK", running = true, endsAt = start + 25 * 60_000, remainingSec = 1500, workMin = 25, breakMin = 5, label = "Test", startedAt = start))
        val s = r.settlePomodoro()
        assertEquals("BREAK", s.phase)
        assertEquals(s.pendingSessionId, "pomo-$start")
        r.settlePomodoro()
        val session = r.user.sessionsNow().single()
        assertEquals(25, session.minutes); assertEquals(25, session.plannedMinutes)
        r.addPerformance(session.id, 10, 2, 1, "")
        assertEquals(13, r.user.solvedTotal().first())
    }

    @Test fun zipBackupRoundTripWithPhoto() = runBlocking {
        val s = source()
        r.logStudy(StudyEntry("tyt-mat", null, s.id, 1, 2, 3, 1, 0, 10))
        r.photoFile("a.jpg").writeBytes(byteArrayOf(1, 2, 3))
        r.saveWrong(r.newWrong("tyt-mat", null, s.id, s.name, 3, "4", "Diğer", "", "a.jpg", null))
        val out = ByteArrayOutputStream()
        app.backup.exportZip(out, "2.0.0", false)
        val loaded = app.backup.load(ByteArrayInputStream(out.toByteArray()))
        val f = (loaded.check as BackupCheck.Ok).file
        r.clearUserData()
        assertFalse(r.photoFile("a.jpg").exists())
        app.backup.restore(f.data, true, "2.0.0", loaded.attachments)
        assertEquals("Test", r.user.profileNow()?.name)
        assertEquals(4, r.user.solvedTotal().first())
        assertEquals(1, r.user.wrongsNow().size)
        assertEquals(s.name, r.user.sourcesNow().single().name)
        assertTrue(r.photoFile("a.jpg").exists())
        assertTrue(app.backup.safetyCopies().isNotEmpty())
    }

    @Test fun badBackupFilesAreRejected() = runBlocking {
        assertTrue(app.backup.load(ByteArrayInputStream("bozuk".toByteArray())).check is BackupCheck.Bad)
        assertTrue(app.backup.load(ByteArrayInputStream(byteArrayOf('P'.code.toByte(), 'K'.code.toByte(), 3, 4, 9, 9))).check is BackupCheck.Bad)
    }

    @Test fun importRejectsInvalidPack() = runBlocking {
        assertFalse(r.importPack("bozuk").first)
        assertFalse(r.importPack("""{"pack":{"id":"x","title":"t","version":"1"},"topics":[{"id":"t","subjectId":"yok","name":"k"}]}""").first)
    }

    @Test fun universityPackNeedsSource() {
        assertTrue(UniversityImport.errors(UniversityImport.parse("a.csv", "universite;sehir;bolum\nX;Y;Z")).isNotEmpty())
        assertTrue(UniversityImport.errors(UniversityImport.parse("a.csv", "#kaynak=ÖSYM\n#surum=2026\nuniversite;sehir;bolum\nX;Y;Z")).isEmpty())
    }

    @Test fun notificationsWorkOffline() = runBlocking {
        val m = app.getSystemService(NotificationManager::class.java)
        m.cancelAll()
        Notifier.show(app, 777, "Test", "Yerel bildirim")
        Thread.sleep(600)
        assertTrue(m.activeNotifications.any { it.id == 777 })
        Notifier.fire(app, "MOTIVATION")
        Thread.sleep(600)
        val first = m.activeNotifications.count { it.notification.extras.getString("android.title") == "Günün sözü" }
        assertEquals(1, first)
        m.cancelAll()
        Notifier.fire(app, "MOTIVATION")
        Thread.sleep(600)
        assertEquals("Aynı gün ikinci kez gönderilmemeli", 0, m.activeNotifications.count { it.notification.extras.getString("android.title") == "Günün sözü" })
        r.updateSettings { it.remove(Keys.quoteNotifiedDay) }
        Notifier.fire(app, "GOAL")
        Thread.sleep(600)
        assertTrue(m.activeNotifications.any { it.notification.extras.getString("android.title") == "Günlük hedefin" })
    }

    /** Ana ekran bileşenleri gerçek RemoteViews olarak çizilir. */
    @Test fun widgetsRenderRealData() = runBlocking {
        val root = FrameLayout(app)
        val t = StudyTask(date = today().toString(), time = "09:00", title = "Paragraf tekrarı")
        r.user.saveTask(t)
        r.logStudy(StudyEntry(minutes = 120))
        val goal = Widgets.goal(app).apply(app, root)
        assertEquals("%50", goal.findViewById<TextView>(R.id.w_value).text.toString())
        assertTrue(Widgets.tasks(app).apply(app, root).findViewById<TextView>(R.id.w_task1).text.contains("Paragraf"))
        assertTrue(Widgets.countdown(app).apply(app, root).findViewById<TextView>(R.id.w_value).text.isNotBlank())
        assertNotNull(Widgets.focus(app).apply(app, root))
    }

    @Test fun reportFilesAreCreated() = runBlocking {
        r.logStudy(StudyEntry("tyt-mat", null, null, null, null, 8, 2, 0, 50))
        val rep = r.report(today(), today(), "Haftalık rapor")
        assertTrue(ReportExporter.pdf(app, rep).length() > 500)
        assertTrue(ReportExporter.png(app, rep).length() > 500)
    }

    @Test fun emptyReportSaysSo() = runBlocking {
        assertFalse(r.report(today(), today(), "Rapor").hasData)
    }

    @Test fun motivationDataAndFavorites() = runBlocking {
        assertEquals(100, r.quotes.size)
        val a = r.quoteOfDay(); assertEquals(a, r.quoteOfDay())
        assertNotEquals(a, r.quoteOfDay(today().plusDays(1)))
        r.toggleBookmark("QUOTE", a.id, true)
        assertEquals(listOf(a.id), r.user.bookmarkIds("QUOTE").first())
    }

    /** Satın alma Play olmadan yapılamaz; merkezi hak ve kalıcılığı gerçek depoyla denenir. */
    @Test fun entitlementFlowAndAdsOff() = runBlocking {
        val m = app.monetize
        m.entitlement.apply(PurchaseQuery.None)
        assertFalse(m.entitlement.hasAdsFreeEntitlement.value)
        m.entitlement.apply(PurchaseQuery.Pending)
        assertFalse("Bekleyen ödeme hak vermemeli", m.entitlement.hasAdsFreeEntitlement.value)
        m.entitlement.apply(PurchaseQuery.Purchased("tok", "{\"productId\":\"$REMOVE_ADS_PRODUCT\"}", ""))
        assertFalse("İmzasız kayıt hak vermemeli", m.entitlement.hasAdsFreeEntitlement.value)
        m.entitlement.apply(TestPurchase.purchased("baska_urun"))
        assertFalse("Başka ürün hak vermemeli", m.entitlement.hasAdsFreeEntitlement.value)
        m.entitlement.apply(TestPurchase.purchased(state = 4))
        assertFalse("Tamamlanmamış ödeme hak vermemeli", m.entitlement.hasAdsFreeEntitlement.value)
        m.entitlement.apply(TestPurchase.purchased())
        assertTrue(m.entitlement.hasAdsFreeEntitlement.value)
        m.entitlement.apply(PurchaseQuery.Unavailable)
        assertTrue("İnternetsiz hak korunmalı", m.entitlement.hasAdsFreeEntitlement.value)
        Thread.sleep(200)
        assertFalse(m.ads.canRequestAds.value)
        var got = false
        m.ads.nativeFor(AdPlacement.HOME) { got = it != null }
        assertFalse(got)
        m.entitlement.apply(PurchaseQuery.None)
        assertFalse(m.entitlement.hasAdsFreeEntitlement.value)
    }

    @Test fun resetClearsUserDataOnly() = runBlocking {
        val s = source()
        r.logStudy(StudyEntry("tyt-mat", null, s.id, null, null, 1, 0, 0, 5))
        r.updateSettings { it[Keys.diploma] = "85"; it[Keys.theme] = "LIGHT" }
        r.clearUserData()
        assertEquals("", r.settings.first().diploma)
        assertEquals("LIGHT", r.settings.first().theme)
        r.updateSettings { it[Keys.theme] = "DARK" }
        assertEquals(0, r.user.solvedTotal().first())
        assertTrue(r.user.sourcesNow().isEmpty())
        assertEquals(261, r.content.topicCountNow())
    }
}
