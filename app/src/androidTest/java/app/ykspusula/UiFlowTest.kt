package app.ykspusula

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.ykspusula.data.*
import app.ykspusula.domain.PickedSection
import app.ykspusula.ui.DebugNav
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class UiFlowTest {
    @get:Rule val rule = createEmptyComposeRule()
    private val app get() = ApplicationProvider.getApplicationContext<Context>() as YksApp
    private val r get() = app.repo
    private var scenario: ActivityScenario<MainActivity>? = null

    private val prefix: String get() {
        val m = app.resources.displayMetrics
        return "${(m.widthPixels / m.density).toInt()}dp"
    }

    @Before fun setup() = runBlocking {
        app.ready.first { it }
        r.clearUserData()
        r.updateSettings { it[Keys.theme] = "LIGHT" }
    }

    @After fun tearDown() { scenario?.close() }

    private fun launch() { scenario = ActivityScenario.launch(MainActivity::class.java); rule.waitForIdle() }

    private fun shot(name: String) {
        rule.waitForIdle()
        Thread.sleep(250)
        val dir = File(app.filesDir, "screens").apply { mkdirs() }
        val bmp = rule.onAllNodes(isRoot()).onFirst().captureToImage().asAndroidBitmapCompat()
        File(dir, "$prefix-$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 90, it) }
    }

    private fun androidx.compose.ui.graphics.ImageBitmap.asAndroidBitmapCompat(): Bitmap {
        val px = IntArray(width * height)
        readPixels(px)
        return Bitmap.createBitmap(px, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun waitText(t: String, ms: Long = 15000, sub: Boolean = false) {
        rule.waitUntil(ms) { rule.onAllNodesWithText(t, substring = sub).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun clickText(t: String) { waitText(t); rule.onAllNodesWithText(t).onFirst().performScrollTo().performClick(); rule.waitForIdle() }

    private fun seedProfile() = runBlocking {
        r.user.saveProfile(UserProfile(onboarded = true, name = "Elif", targetDepartment = "Bilgisayar Mühendisliği", targetUniversity = "ODTÜ", weakSubjects = "tyt-mat"))
        r.generatePlan()
    }

    private fun seedActivity() = runBlocking {
        val now = System.currentTimeMillis()
        listOf("tyt-mat", "tyt-tur", "tyt-fiz", "ayt-mat").forEach { s ->
            r.content.idsOfSubject(s).take(30).forEachIndexed { i, id ->
                val q = r.content.full(id)!!
                val label = if (i % 3 == 0) q.options.first { !it.isCorrect }.label else q.correctLabel
                r.answer(q, label, "BANK", "x", now - i * 86_400_000L / 3)
            }
        }
        (0..9).forEach { d -> r.user.saveSession(StudySession(label = "Odak", startedAt = now - d * 86_400_000L, minutes = 30 + d * 12)) }
        listOf(62.0, 70.0, 75.0).forEachIndexed { i, _ ->
            r.saveExternalExam("Deneme ${i + 1}", "Örnek", today().minusDays(20L - i * 7), "TYT", 165, listOf(Triple("Türkçe", intArrayOf(28 + i, 8, 4 - i), 40), Triple("Temel Matematik", intArrayOf(20 + 2 * i, 10, 10 - 2 * i), 40)))
        }
    }

    @Test fun onboardingAndTabs() {
        launch()
        waitText("Başla"); shot("01-karsilama")
        clickText("Başla")
        waitText("Seni tanıyalım"); shot("02-tanisma")
        rule.onNodeWithText("Adın (isteğe bağlı)").performTextInput("Elif")
        clickText("Devam")
        waitText("Hedefin"); rule.onNodeWithText("Hedef bölüm").performTextInput("Tıp")
        shot("03-hedef")
        clickText("Devam"); waitText("Çalışma düzenin"); shot("04-duzen")
        clickText("Devam"); waitText("Güçlü olduğun dersler"); shot("05-guclu")
        clickText("Matematik".let { "Temel Matematik" })
        clickText("Devam"); waitText("Zorlandığın dersler")
        clickText("Fizik"); clickText("Bitir")
        waitText("Bugünkü ilerleme"); shot("06-ana-sayfa")
        assertEquals("Elif", runBlocking { r.user.profileNow()!!.name })
        assertTrue(runBlocking { r.user.tasksOn(today().toString()).first().isNotEmpty() || r.user.allTasks().isNotEmpty() })
        clickText("Çalış"); waitText("Soru bankası"); shot("07-calis")
        clickText("AYT"); waitText("Fen Bilimleri"); shot("08-ayt")
        clickText("YDT"); waitText("Seçili dil", sub = true); shot("09-ydt")
        clickText("Denemeler"); waitText("TYT genel deneme"); shot("10-denemeler")
        clickText("İstatistik"); waitText("Henüz veri yok"); shot("11-bos-istatistik")
        clickText("Profil"); waitText("Yedekleme ve geri yükleme"); shot("12-profil")
    }

    @Test fun solveQuestionUpdatesStats() {
        seedProfile()
        val topic = "tyt-mat-kumeler"
        launch()
        waitText("Bugünkü ilerleme")
        rule.runOnUiThread { DebugNav.nav!!.navigate("topic/$topic") }
        waitText("Kısa özet", sub = false); shot("20-konu")
        rule.runOnUiThread { DebugNav.nav!!.navigate("solve?topic=$topic") }
        waitText("Cevabı kontrol et"); shot("21-soru")
        val first = runBlocking { r.content.full(r.questionIds(QuestionFilter(topicId = topic), 0, 0).first())!! }
        val bad = first.options.first { !it.isCorrect }
        rule.onAllNodesWithContentDescription(bad.text.replace("−", "−"), substring = true).onFirst().performScrollTo().performClick()
        clickText("Cevabı kontrol et")
        waitText("Yanlış cevap", sub = true); shot("22-yanlis")
        clickText("Dikkatsizlik")
        rule.waitUntil(5000) { runBlocking { r.user.wrongOf(first.q.id)?.reason == "Dikkatsizlik" } }
        assertEquals(1, runBlocking { r.user.attemptsNow().size })
        Espresso.pressBack()
        waitText("Kısa özet")
        Espresso.pressBack()
        waitText("Bugünkü ilerleme")
    }

    @Test fun examResumesAfterRecreate() {
        seedProfile()
        val id = runBlocking {
            val ids = r.content.idsOfSubject("tyt-mat").take(5)
            r.startExam(Repository.ExamDraft("TYT Test", "BRANCH", "TYT", listOf(PickedSection("Temel Matematik", 5, ids)), 30))
        }
        launch()
        waitText("Devam eden deneme", sub = true); shot("30-devam")
        rule.runOnUiThread { DebugNav.nav!!.navigate("exam/$id") }
        waitText("Soru 1"); shot("31-deneme")
        val rec = runBlocking { r.user.records(id).first() }
        val q = runBlocking { r.content.full(rec.questionId)!! }
        rule.onAllNodesWithContentDescription(q.options.first().text, substring = true).onFirst().performScrollTo().performClick()
        rule.waitUntil(5000) { runBlocking { r.user.records(id).first().selected != null } }
        clickText("Sonraki"); waitText("Soru 2")
        scenario!!.recreate()
        waitText("Soru", sub = true, ms = 20000)
        scenario!!.close(); scenario = null
        assertEquals("RUNNING", runBlocking { r.user.mock(id)!!.status })
        launch()
        waitText("Devam eden deneme", sub = true)
        rule.runOnUiThread { DebugNav.nav!!.navigate("exam/$id") }
        waitText("Sınavı bitir")
        clickText("Sınavı bitir")
        waitText("Sınav bitirilsin mi?"); rule.onNodeWithText("Bitir").performClick()
        waitText("Toplam net"); shot("32-sonuc")
        assertEquals("DONE", runBlocking { r.user.mock(id)!!.status })
    }

    @Test fun allScreensOpenBothThemes() {
        seedProfile(); seedActivity()
        runBlocking {
            r.user.saveNote(UserNote(id = "n1", title = "Limit notu", body = "x→2", createdAt = 1, updatedAt = 1))
            val u = TargetUniversity(id = "u1", name = "ODTÜ", city = "Ankara"); r.user.saveUniversity(u)
            r.user.saveDepartment(TargetDepartment(universityId = "u1", name = "Bilgisayar Mühendisliği", targetRank = 2000))
        }
        val exam = runBlocking { r.user.mockSummariesNow().first().id }
        val wrong = runBlocking { r.user.wrongsNow().first().questionId }
        val routes = listOf(
            "home", "study", "exams", "stats", "profile", "search", "subject/tyt-mat", "topic/tyt-mat-uslu-sayilar", "topic/ayt-mat-turev", "tracker", "bank",
            "solve?subject=tyt-geo", "wrongs", "wrong/$wrong", "cards", "deck/Tarih", "review/_all", "cardEdit/Kartlarım", "formulas", "notes", "note/n1", "note/new",
            "library", "pomodoro", "plan", "taskEdit/${today()}", "examNew/TYT", "examNew/AYT", "examNew/YDT", "examNew/BRANCH", "examNew/TOPIC", "examNew/CUSTOM",
            "result/$exam", "external", "examSettings", "obp", "profileEdit", "settings", "reminders", "backup", "targets", "packs", "badges", "topic/ayt-biy-evrim",
        )
        listOf("LIGHT", "DARK").forEach { theme ->
            runBlocking { r.updateSettings { it[Keys.theme] = theme } }
            launch()
            waitText("Bugünkü ilerleme")
            routes.forEachIndexed { i, route ->
                rule.runOnUiThread { DebugNav.nav!!.navigate(route) }
                rule.waitForIdle()
                Thread.sleep(350)
                rule.onAllNodes(isRoot()).fetchSemanticsNodes()
                shot("s-${theme.lowercase()}-${"%02d".format(i)}-${route.substringBefore('?').replace('/', '_').replace("Kartlarım", "k")}")
            }
            scenario!!.close(); scenario = null
        }
    }
}
