package app.ykspusula

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import app.ykspusula.data.UserProfile
import app.ykspusula.monetize.AdPlacement
import app.ykspusula.monetize.Monetize
import app.ykspusula.monetize.PurchaseQuery
import app.ykspusula.monetize.REMOVE_ADS_PRODUCT
import app.ykspusula.ui.DebugNav
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** İnternet gerektirir, yalnızca bağlantılı emülatörde çalıştırılır. Google test reklam kimliklerini kullanır. */
@RunWith(AndroidJUnit4::class)
class AdOnlineTest {
    @get:Rule val rule = createEmptyComposeRule()
    private val app get() = ApplicationProvider.getApplicationContext<Context>() as YksApp

    @Test fun testAdLoadsAndPurchaseRemovesIt() = withAdOnExams { m ->
        // Yalnızca görsel kayıt içindir. Bu kaydırma dokunma üretmez, reklama basma ihtimali yoktur.
        runCatching { rule.onAllNodes(hasScrollAction()).onFirst().performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 900f) } }
        Thread.sleep(1200)
        InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()?.let { b ->
            File(app.filesDir, "reklam-yerel.png").outputStream().use { o -> b.compress(Bitmap.CompressFormat.PNG, 90, o) }
        }
        runBlocking { m.entitlement.apply(TestPurchase.purchased()) }
        rule.waitForIdle()
        Thread.sleep(1000)
        onView(withId(R.id.ad_label)).check(doesNotExist())
        assertFalse(m.ads.canRequestAds.value)
    }

    /**
     * Sayfayı kaydırırken parmak reklamın üstünden başlasa bile reklam açılmamalı.
     * Yanlışlıkla tıklama AdMob politikası açısından önemlidir. Sürükleme gerçek dokunma olayı üretir.
     */
    @Test fun dragOverAdDoesNotOpenIt() = withAdOnExams { _ ->
        val shell = InstrumentationRegistry.getInstrumentation().uiAutomation
        // Yüklü emülatörde çok kısa sürükleme araya hareket olayı giremeden biter, en zor durum budur.
        val speeds = listOf(40, 120, 300)
        repeat(36) { i ->
            val view = reklamKonumu()
            // Reklam ekranın ortasına alınır, böylece her sürükleme reklamın üstünde başlar.
            runCatching { rule.onAllNodes(hasScrollAction()).onFirst().performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, (view - 1100).toFloat()) } }
            rule.waitForIdle()
            val y = reklamKonumu()
            val (a, b) = if (i % 2 == 0) y to y - 500 else y to y + 500
            shell.executeShellCommand("input swipe 540 $a 540 $b ${speeds[i % 3]}").close()
            Thread.sleep(1200)
            var resumed = false
            rule.runOnUiThread { resumed = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).isNotEmpty() }
            assertTrue("Sürükleme ${i + 1} sonrası uygulama önde değil: reklam açılmış olabilir (y=$a, hız=${speeds[i % 3]})", resumed)
        }
    }

    /** Reklam görselinin ekrandaki dikey ortası. */
    private fun reklamKonumu(): Int {
        var y = 0
        onView(withId(R.id.ad_media)).check { v, e ->
            assertNull(e)
            val p = IntArray(2)
            v.getLocationOnScreen(p)
            y = p[1] + v.height / 2
        }
        return y
    }

    /** Reklamı Denemeler ekranında gösterir, verilen adımı çalıştırır ve hakkı her durumda temizler. */
    private fun withAdOnExams(body: (Monetize) -> Unit) {
        runBlocking {
            app.ready.first { it }
            app.repo.clearUserData()
            app.repo.user.saveProfile(UserProfile(onboarded = true, name = "Test"))
        }
        val m = app.monetize
        // Önceki denemeden kalan reklamsız hak bu testi anlamsız kılar: hak varken reklam izni hiç gelmez.
        runBlocking { m.entitlement.apply(PurchaseQuery.None) }
        // Reklam yalnızca tarafsız yaş ekranında 18 yaşını doldurduğu belirlenen kullanıcıya istenir.
        runBlocking { m.saveBirth(1990, 1); m.verifiedAdult.first { it } }
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            showAd(m)
            body(m)
        } finally {
            runBlocking { m.entitlement.apply(PurchaseQuery.None) }
            scenario.close()
        }
    }

    private fun showAd(m: Monetize) {
        m.forceEligibleForTest()
        try {
            rule.waitUntil("Reklam izni", 120_000) { m.ads.canRequestAds.value }
        } catch (e: ComposeTimeoutException) {
            // Hatanın uygulamadan mı ağdan mı geldiğini ayırmak için gerçek bir HTTPS isteği denenir.
            val net = runCatching {
                (URL("https://fundingchoicesmessages.google.com/").openConnection() as HttpURLConnection).run { connectTimeout = 10_000; readTimeout = 10_000; responseCode }
            }
            throw AssertionError("Reklam izni gelmedi: ${m.ads.lastError}; ${m.ads.debugState()}; ag: $net", e)
        }
        val latch = CountDownLatch(1)
        var loaded = false
        rule.runOnUiThread { m.ads.nativeFor(AdPlacement.EXAMS) { loaded = it != null; latch.countDown() } }
        assertTrue("Reklam yanıtı gelmedi", latch.await(90, TimeUnit.SECONDS))
        assertTrue("Test reklamı yüklenemedi: ${m.ads.lastError}", loaded)
        // Reklam çok hızlı gelirse gezinme henüz hazır olmayabilir.
        rule.waitUntil("Gezinme", 30_000) { DebugNav.nav != null }
        rule.runOnUiThread { DebugNav.nav!!.navigate("exams") }
        rule.waitForIdle()
        Thread.sleep(3000)
        onView(withId(R.id.ad_label)).check { v, e -> assertNull(e); assertNotNull(v) }
    }
}
