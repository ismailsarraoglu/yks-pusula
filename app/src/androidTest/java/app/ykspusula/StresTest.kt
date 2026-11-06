package app.ykspusula

import android.content.Context
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.ykspusula.data.UserProfile
import app.ykspusula.ui.DebugNav
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Sayfa geçişi sürerken etkinliğin kapanması. Kullanıcı karşılığı: düğmeye basıp hemen telefonu döndürmek
 * veya sistemin uygulamayı o anda kapatması. Bir ekran hatası görülürse süreç düşer ve test başarısız olur.
 */
@RunWith(AndroidJUnit4::class)
class StresTest {
    @get:Rule val rule = createEmptyComposeRule()
    private val app get() = ApplicationProvider.getApplicationContext<Context>() as YksApp

    @Test fun navigationThenImmediateRecreate() {
        runBlocking {
            app.ready.first { it }
            app.repo.clearUserData()
            app.repo.user.saveProfile(UserProfile(onboarded = true, name = "Test"))
        }
        val routes = listOf("reports", "notes", "sources", "targets", "motivation", "backup", "search")
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        rule.waitUntil(15_000) { rule.onAllNodesWithText("Bugünkü Rotan").fetchSemanticsNodes().isNotEmpty() }
        repeat(35) { i ->
            rule.runOnUiThread { DebugNav.nav?.navigate(routes[i % routes.size]) }
            // Kare beklenmeden etkinlik yeniden oluşturulur.
            scenario.recreate()
            rule.waitForIdle()
        }
        Thread.sleep(800)
        scenario.close()
    }
}
