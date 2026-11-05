package app.ykspusula

import android.content.Context
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.ykspusula.data.UserProfile
import app.ykspusula.monetize.AdPlacement
import app.ykspusula.monetize.PurchaseQuery
import app.ykspusula.monetize.REMOVE_ADS_PRODUCT
import app.ykspusula.ui.DebugNav
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
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
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        m.forceEligibleForTest()
        rule.waitUntil("Reklam izni", 60_000) { m.ads.canRequestAds.value }
        val latch = CountDownLatch(1)
        var loaded = false
        rule.runOnUiThread { m.ads.nativeFor(AdPlacement.EXAMS) { loaded = it != null; latch.countDown() } }
        assertTrue("Reklam yanıtı gelmedi", latch.await(90, TimeUnit.SECONDS))
        assertTrue("Test reklamı yüklenemedi: ${m.ads.lastError}", loaded)
        rule.runOnUiThread { DebugNav.nav!!.navigate("exams") }
        rule.waitForIdle()
        Thread.sleep(3000)
        onView(withId(R.id.ad_label)).check { v, e -> assertNull(e); assertNotNull(v) }
        runBlocking { m.entitlement.apply(PurchaseQuery.Purchased("test", "{\"productId\":\"$REMOVE_ADS_PRODUCT\"}", "")) }
        rule.waitForIdle()
        Thread.sleep(1000)
        onView(withId(R.id.ad_label)).check(doesNotExist())
        assertFalse(m.ads.canRequestAds.value)
        runBlocking { m.entitlement.apply(PurchaseQuery.None) }
        scenario.close()
    }
}
