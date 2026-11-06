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

    @Test fun testAdLoadsAndPurchaseRemovesIt() {
        runBlocking {
            app.ready.first { it }
            app.repo.clearUserData()
            app.repo.user.saveProfile(UserProfile(onboarded = true, name = "Test"))
        }
        val m = app.monetize
        // Önceki denemeden kalan reklamsız hak bu testi anlamsız kılar: hak varken reklam izni hiç gelmez.
        runBlocking { m.entitlement.apply(PurchaseQuery.None) }
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            adFlow(m)
        } finally {
            runBlocking { m.entitlement.apply(PurchaseQuery.None) }
            scenario.close()
        }
    }

    private fun adFlow(m: Monetize) {
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
        rule.runOnUiThread { DebugNav.nav!!.navigate("exams") }
        rule.waitForIdle()
        Thread.sleep(3000)
        onView(withId(R.id.ad_label)).check { v, e -> assertNull(e); assertNotNull(v) }
        // Yalnızca görsel kayıt içindir. Varsayılan kaydırma dokunma üretmez, reklama basma ihtimali yoktur.
        // "-e kaydirma dokunma" verilirse gerçek parmak hareketi taklit edilir.
        if (InstrumentationRegistry.getArguments().getString("kaydirma") == "dokunma") {
            repeat(3) { InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("input swipe 500 1800 500 500 300").close(); Thread.sleep(700) }
        } else {
            runCatching { rule.onAllNodes(hasScrollAction()).onFirst().performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 900f) } }
        }
        Thread.sleep(1200)
        InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()?.let { b ->
            File(app.filesDir, "reklam-yerel.png").outputStream().use { o -> b.compress(Bitmap.CompressFormat.PNG, 90, o) }
        }
        runBlocking { m.entitlement.apply(PurchaseQuery.Purchased("test", "{\"productId\":\"$REMOVE_ADS_PRODUCT\"}", "")) }
        rule.waitForIdle()
        Thread.sleep(1000)
        onView(withId(R.id.ad_label)).check(doesNotExist())
        assertFalse(m.ads.canRequestAds.value)
    }
}
