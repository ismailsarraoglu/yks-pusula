package app.ykspusula

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.ykspusula.data.*
import app.ykspusula.ui.DebugNav
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import java.io.File

@android.annotation.SuppressLint("RestrictedApi")
@RunWith(AndroidJUnit4::class)
class UiFlowTest {
    /** Hata mesajına test iş parçacığını yazar: Compose eşzamanlılık hatasının kaynağını ayırt etmek için. */
    @get:Rule(order = -2) val threadNote = TestRule { base, _ -> object : Statement() {
        override fun evaluate() {
            try { base.evaluate() } catch (t: Throwable) {
                runCatching {
                    InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()?.let { b ->
                        File(app.filesDir, "hata.png").outputStream().use { o -> b.compress(Bitmap.CompressFormat.PNG, 90, o) }
                    }
                }
                throw AssertionError("testThread=${Thread.currentThread().id}", t)
            }
        }
    } }
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
        r.updateSettings { it[Keys.theme] = "DARK" }
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
        try {
            rule.waitUntil(ms) { rule.onAllNodesWithText(t, substring = sub).fetchSemanticsNodes().isNotEmpty() }
        } catch (e: ComposeTimeoutException) {
            val seen = runCatching {
                rule.onAllNodes(hasText("", substring = true)).fetchSemanticsNodes().mapNotNull { it.config.getOrNull(SemanticsProperties.Text)?.joinToString() }.take(40)
            }.getOrDefault(emptyList())
            throw AssertionError("'$t' bulunamadi. Ekrandakiler: $seen", e)
        }
    }

    /** Klavye kapanırken düğmeler yer değiştirir; dokunma yanlış yere düşmesin. */
    private fun closeKeyboard() { runCatching { Espresso.closeSoftKeyboard() }; rule.waitForIdle() }

    private fun clickText(t: String) {
        waitText(t)
        val n = rule.onAllNodesWithText(t).onFirst()
        runCatching { n.performScrollTo() }
        n.performClick()
        rule.waitForIdle()
    }

    private fun seedProfile() = runBlocking {
        r.user.saveProfile(UserProfile(onboarded = true, name = "Elif", targetDepartment = "Bilgisayar Mühendisliği", targetUniversity = "ODTÜ", weakSubjects = "tyt-mat"))
        r.generatePlan()
    }

    /** Örnek veri: ekran denetimi ve mağaza görselleri için. Uygulamada değil, yalnızca testte bulunur. */
    private fun seedActivity() = runBlocking {
        val now = System.currentTimeMillis()
        val src = StudySource(id = "s1", name = "TYT Matematik Soru Bankası", publisher = "Örnek Yayın", subjectId = "tyt-mat", totalPages = 320, lastPage = 52, pagesDone = 52, lastUsedAt = now, createdAt = now, updatedAt = now)
        r.user.saveSource(src)
        r.user.saveSource(StudySource(id = "s2", name = "Paragraf Denemeleri", publisher = "Örnek Yayın", type = "KITAP", subjectId = "tyt-tur", totalPages = 180, lastPage = 96, pagesDone = 96, favorite = true, lastUsedAt = now - 86_400_000L, createdAt = now, updatedAt = now))
        r.user.saveSource(StudySource(id = "s3", name = "Biyoloji Ders Notlarım", type = "NOT", subjectId = "tyt-biy", totalPages = 60, lastPage = 21, pagesDone = 21, lastUsedAt = now - 2 * 86_400_000L, createdAt = now, updatedAt = now))
        val topics = listOf("tyt-mat-kumeler" to "tyt-mat", "tyt-mat-uslu-sayilar" to "tyt-mat", "tyt-tur-paragraf" to "tyt-tur")
        (0..9).forEach { d ->
            val (t, s) = topics[d % 3]
            r.logStudy(StudyEntry(s, t, if (s == "tyt-mat") "s1" else "s2", 40 + d, 41 + d, 8 + d, 3, 1, 30 + d * 12, at = now - d * 86_400_000L))
        }
        r.setTopicStatus("tyt-mat-kumeler", 3); r.setTopicStatus("tyt-tur-paragraf", 2)
        listOf(Triple("Kümelerde birleşim", "Dikkatsizlik", "12"), Triple("Üslü ifade işlemi", "İşlem hatası", "7"), Triple("Paragrafta ana düşünce", "İki seçenek arasında kalma", "21")).forEachIndexed { i, (n, why, q) ->
            r.saveWrong(r.newWrong(if (i == 2) "tyt-tur" else "tyt-mat", topics[i].first, if (i == 2) "s2" else "s1", if (i == 2) "Paragraf Denemeleri" else src.name, 45 + i, q, why, n, null, now - 1000 + i * 86_400_000L))
        }
        listOf(0, 1, 2, 3).forEach { i ->
            r.saveExternalExam("TYT Deneme ${i + 1}", "Örnek Yayın", today().minusDays(21L - i * 7), "TYT", 150 + i * 5, listOf(SectionEntry("Türkçe", 26 + i * 2, 8, 6 - i * 2), SectionEntry("Sosyal Bilimler", 11 + i, 5, 4 - i), SectionEntry("Temel Matematik", 18 + 3 * i, 10, 12 - 3 * i), SectionEntry("Fen Bilimleri", 9 + 2 * i, 5, 6 - 2 * i)))
        }
        r.generatePlan(weekStart(today()))
        r.user.tasksBetweenNow(weekStart(today()).toString(), today().toString()).take(5).forEach { r.toggleTask(it, true) }
        r.user.saveRoute(emptyList())
        r.rebuildRoute()
    }

    @Test fun onboardingAndTabs() {
        launch()
        waitText("Başla"); shot("01-karsilama")
        clickText("Başla")
        waitText("Seni tanıyalım"); shot("02-tanisma")
        rule.onNodeWithText("Adın (isteğe bağlı)").performTextInput("Elif"); closeKeyboard()
        clickText("Devam")
        waitText("Hedefin"); rule.onNodeWithText("Hedef bölüm").performTextInput("Tıp"); closeKeyboard()
        shot("03-hedef")
        clickText("Devam"); waitText("Çalışma düzenin"); shot("04-duzen")
        clickText("Devam"); waitText("Güçlü olduğun dersler"); shot("05-guclu")
        clickText("Temel Matematik")
        clickText("Devam"); waitText("Zorlandığın dersler")
        clickText("Fizik"); clickText("Bitir")
        waitText("Bugünkü Rotan"); shot("06-ana-sayfa")
        assertEquals("Elif", runBlocking { r.user.profileNow()!!.name })
        assertTrue(runBlocking { r.user.allTasks().isNotEmpty() })
        rule.waitUntil(10000) { runBlocking { r.user.routeOnNow(today().toString()).isNotEmpty() } }
        clickText("Çalış"); waitText("Kaynaklarım"); shot("07-calis")
        assertTrue("Soru bankası menüsü olmamalı", rule.onAllNodesWithText("Soru bankası").fetchSemanticsNodes().isEmpty())
        clickText("Denemeler"); waitText("Hemen deneme oluştur!"); shot("10-denemeler")
        clickText("İstatistik"); waitText("Henüz veri yok"); shot("11-bos-istatistik")
        clickText("Profil"); waitText("Veri yedekleme", sub = false); shot("12-profil")
    }

    @Test fun studyLogUpdatesStats() {
        seedProfile()
        val topic = "tyt-mat-kumeler"
        runBlocking { r.user.saveSource(StudySource(id = "s1", name = "Soru Bankası", subjectId = "tyt-mat", totalPages = 300, createdAt = 1, updatedAt = 1)) }
        launch()
        waitText("Bugünkü Rotan")
        go("topic/$topic"); waitText("Kısa özet", sub = false); shot("20-konu")
        go("log?source=s1&topic=$topic&subject=tyt-mat"); waitText("Çalışma kaydı")
        type("İlk sayfa", "45"); type("Son sayfa", "52")
        type("Doğru", "30"); type("Yanlış", "8"); type("Çalışma süresi (dakika)", "55")
        waitText("Toplam 38 soru", sub = true); shot("21-kayit")
        clickText("Kaydet")
        until("kayıt") { r.user.solvedTotal().first() == 38 }
        assertEquals(52, runBlocking { r.user.source("s1")!!.lastPage })
        Espresso.pressBack()
        waitText("Bugünkü Rotan")
    }

    @Test fun examSessionSurvivesRecreate() {
        seedProfile()
        launch()
        waitText("Bugünkü Rotan")
        go("examStart/TYT"); waitText("Süreyi başlat"); shot("30-baslat")
        clickText("Süreyi başlat")
        waitText("Deneme oturumu"); shot("31-oturum")
        val id = runBlocking { r.user.running().first()!!.id }
        scenario!!.recreate()
        rule.waitForIdle()
        scenario!!.close(); scenario = null
        assertEquals("RUNNING", runBlocking { r.user.mock(id)!!.status })
        launch()
        waitText("Devam eden deneme", sub = true)
        go("examRun/$id")
        clickText("Bitir")
        waitText("Deneme bitirilsin mi?")
        rule.onAllNodesWithText("Bitir").onLast().performClick()
        waitText("Sonuçları gir"); shot("32-giris")
        rule.onAllNodesWithText("Doğru").onFirst().performTextInput("30"); rule.onAllNodesWithText("Yanlış").onFirst().performTextInput("8"); closeKeyboard()
        clickText("Sonucu kaydet")
        waitText("Toplam net"); shot("33-sonuc")
        assertEquals("DONE", runBlocking { r.user.mock(id)!!.status })
        rule.runOnUiThread { assertEquals("Oturum ekranı geri yığınında kalmamalı", 0, DebugNav.nav!!.currentBackStack.value.count { it.destination.route == "examRun/{id}" }) }
        Espresso.pressBack()
        waitText("Bugünkü Rotan")
    }

    /** Reklamsız durumda ve reklam izni yokken hiçbir ekranda reklam alanı çizilmez. */
    @Test fun noAdSurfacesWithoutConsentOrWhenAdsFree() {
        seedProfile()
        launch()
        waitText("Bugünkü Rotan")
        listOf("home", "study", "exams", "stats").forEach { go(it); assertTrue(rule.onAllNodesWithText("Reklam").fetchSemanticsNodes().isEmpty()) }
    }

    @Test fun allScreensOpenBothThemes() {
        seedProfile(); seedActivity()
        runBlocking {
            r.user.saveNote(UserNote(id = "n1", title = "Limit notu", body = "x→2", createdAt = 1, updatedAt = 1))
            val u = TargetUniversity(id = "u1", name = "ODTÜ", city = "Ankara"); r.user.saveUniversity(u)
            r.user.saveDepartment(TargetDepartment(universityId = "u1", name = "Bilgisayar Mühendisliği", targetRank = 2000, targetTytNet = 95.0))
        }
        val mocks = runBlocking { r.user.mockSummariesNow() }
        val exam = mocks.first().id
        val wrong = runBlocking { r.user.wrongsNow().first().id }
        val routes = listOf(
            "home", "study", "exams", "stats", "profile", "search", "subject/tyt-mat", "topic/tyt-mat-uslu-sayilar", "topic/ayt-mat-turev", "mastery", "coach", "replan",
            "sources", "source/s1", "sourceEdit/new", "sourceEdit/s1", "sourceCompare", "log", "wrongs", "wrong/$wrong", "wrongEdit/new", "wrongEdit/$wrong", "wrongReview",
            "cards", "deck/Tarih", "review/_all", "cardEdit/Kartlarım", "formulas", "notes", "note/n1", "note/new", "library", "pomodoro", "plan", "planSettings", "planHistory", "taskEdit/${today()}",
            "examStart/ALL", "examStart/TYT", "examStart/AYT", "examStart/YDT", "examStart/BRANCH", "examStart/TOPIC", "examStart/PERSONAL", "examStart/CUSTOM", "examEntry/$exam", "result/$exam",
            "examHistory", "examCompare/${mocks[0].id}/${mocks[3].id}", "examAnalysis", "netGoals", "external", "examSettings", "obp", "reports", "motivation", "removeAds", "privacy", "terms", "about", "info",
            "profileEdit", "settings", "reminders", "backup", "targets", "packs", "badges", "topic/ayt-biy-evrim",
        )
        listOf("DARK", "LIGHT").forEach { theme ->
            runBlocking { r.updateSettings { it[Keys.theme] = theme } }
            launch()
            waitText("Bugünkü Rotan")
            routes.forEachIndexed { i, route ->
                rule.runOnUiThread { DebugNav.nav!!.navigate(route) }
                rule.waitForIdle()
                Thread.sleep(350)
                rule.onAllNodes(isRoot()).fetchSemanticsNodes()
                shot("s-${theme.lowercase()}-${"%02d".format(i)}-${route.substringBefore('?').replace('/', '_').replace("Kartlarım", "k").take(24)}")
            }
            scenario!!.close(); scenario = null
        }
    }

    private fun go(route: String) { rule.runOnUiThread { DebugNav.nav!!.navigate(route) }; rule.waitForIdle() }
    private fun type(label: String, text: String) { waitText(label); val n = rule.onAllNodesWithText(label).onFirst(); runCatching { n.performScrollTo() }; n.performTextInput(text); closeKeyboard() }
    private fun until(what: String, block: suspend () -> Boolean) { rule.waitUntil(what, 10000) { runBlocking { block() } } }

    @Test fun mainButtonsDoTheirJob() {
        seedProfile()
        launch()
        waitText("Bugünkü Rotan")

        go("pomodoro"); clickText("Başlat"); waitText("Duraklat")
        until("sayaç") { r.pomodoro.first().running }
        clickText("Duraklat"); waitText("Devam et")
        until("duraklatma") { !r.pomodoro.first().running }
        clickText("Devam et"); waitText("Duraklat"); clickText("Bitir"); waitText("Başlat")
        until("bitirme") { r.pomodoro.first().phase == "IDLE" }

        go("sourceEdit/new"); type("Kaynak adı", "Paragraf Bankası"); type("Toplam sayfa", "200"); clickText("Kaydet")
        until("kaynak") { r.user.sourcesNow().any { it.name == "Paragraf Bankası" } }
        go("sources"); waitText("Paragraf Bankası")

        go("wrongEdit/new"); type("Kaynak adı (kitap, PDF veya deneme)", "Deneme 3"); type("Sayfa", "12"); clickText("Dikkatsizlik"); clickText("Kaydet")
        until("yanlış") { r.user.wrongsNow().any { it.sourceLabel == "Deneme 3" && it.reason == "Dikkatsizlik" } }
        runBlocking { val w = r.user.wrongsNow().first(); r.saveWrong(w.copy(nextReviewAt = 1)) }
        go("wrongReview"); clickText("Hatırladım")
        until("tekrar") { r.user.wrongReviewsNow().size == 1 }

        go("note/new"); type("Başlık", "Limit özeti"); type("Açıklama", "Sağdan ve soldan limit eşit olmalı."); clickText("Kaydet")
        until("not") { r.user.notesNow().any { it.title == "Limit özeti" } }
        go("notes"); waitText("Limit özeti")

        go("cardEdit/Kartlarım"); type("Ön yüz: soru veya kavram", "Hücre zarı"); type("Arka yüz: cevap", "Seçici geçirgen"); clickText("Kaydet")
        until("kart") { r.user.userCardsNow().size == 1 }
        go("review/Kartlarım"); clickText("Kartı çevir"); clickText("Biliyorum")
        until("kart değerlendirme") { r.user.cardProgressNow().any { it.lastRating == 2 } && r.user.cardReviewsNow().isNotEmpty() }

        go("taskEdit/${today()}"); type("Görev adı (isteğe bağlı)", "Paragraf tekrarı"); clickText("Kaydet")
        until("görev") { r.user.allTasks().any { it.title == "Paragraf tekrarı" } }
        val task = runBlocking { r.user.allTasks().first { it.title == "Paragraf tekrarı" } }
        go("plan"); clickText("Otomatik program oluştur")
        waitText("görev oluşturuldu", sub = true)
        assertTrue(runBlocking { r.user.allTasks().any { it.id == task.id } })

        go("external"); type("Deneme adı", "Yayın Denemesi"); type("Yayınevi veya kaynak", "Örnek Yayın")
        rule.onAllNodesWithText("Doğru").onFirst().performTextInput("30"); rule.onAllNodesWithText("Yanlış").onFirst().performTextInput("8"); closeKeyboard()
        clickText("Kaydet")
        // "Toplam net" formda da yazar; kaydın bittiğini yalnızca sonuç sayfasındaki düğme gösterir.
        waitText("Deneme analizine git"); waitText("28,00", sub = true)

        go("targets"); clickText("Üniversite ekle"); type("Üniversite adı", "Boğaziçi"); type("Şehir", "İstanbul"); clickText("Ekle")
        until("üniversite") { r.user.universitiesNow().any { it.name == "Boğaziçi" } }

        go("obp"); type("Diploma notu (50-100)", "85"); waitText("51,00"); clickText("Kaydet")
        until("diploma") { r.settings.first().diploma == "85" }

        go("settings"); clickText("Açık")
        until("tema") { r.settings.first().theme == "LIGHT" }
        clickText("Çok büyük")
        until("yazı") { r.settings.first().fontScale > 1.2f }
        runBlocking { r.updateSettings { it[Keys.theme] = "DARK"; it[Keys.font] = 1f } }

        go("search"); type("Ders, konu, kaynak, formül, not...", "turev"); waitText("Türev")
        go("backup"); waitText("Yedek oluştur")
        go("motivation"); clickText("Başka söz göster")
        go("reports"); waitText("Haftalık", sub = false)
    }
}
