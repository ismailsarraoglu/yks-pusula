package app.ykspusula

import android.app.NotificationManager
import android.content.Context
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.ykspusula.data.*
import app.ykspusula.notify.Notifier
import app.ykspusula.ui.DebugNav
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Bildirim izni reddedilmişken çalıştırılır (betik önce `pm revoke` uygular).
 * Uygulama çökmeden çalışmalı, hatırlatma kurulabilmeli, bildirim gösterilmemeli.
 */
@RunWith(AndroidJUnit4::class)
class NotificationDeniedTest {
    @get:Rule val rule = createEmptyComposeRule()
    private val app get() = ApplicationProvider.getApplicationContext<Context>() as YksApp

    @Test fun appWorksWithoutNotificationPermission() = runBlocking {
        app.ready.first { it }
        val r = app.repo
        r.clearUserData()
        r.user.saveProfile(UserProfile(onboarded = true, name = "İzinsiz", field = "EA"))
        assertFalse("Bu test izin kapalıyken çalışmalı", Notifier.canNotify(app))
        Notifier.createChannels(app)
        Notifier.show(app, 4343, "Görünmemeli", "İzin yok")
        Notifier.fire(app, "DAILY")
        val rem = Reminder("DAILY", enabled = true, hour = 21, minute = 0, frequency = "DAILY")
        r.user.saveReminder(rem); Notifier.schedule(app, rem)
        val nm = app.getSystemService(NotificationManager::class.java)
        val shown = nm.activeNotifications.count { it.packageName == app.packageName }
        val s = ActivityScenario.launch(MainActivity::class.java)
        rule.waitUntil("ana ekran", 20_000) { rule.onAllNodesWithText("Bugünkü Rotan").fetchSemanticsNodes().isNotEmpty() }
        rule.runOnUiThread { DebugNav.nav!!.navigate("reminders") }
        rule.waitForIdle()
        rule.runOnUiThread { DebugNav.nav!!.navigate("pomodoro") }
        rule.waitForIdle()
        val alive = rule.onAllNodesWithText("Başlat").fetchSemanticsNodes().isNotEmpty()
        File(app.filesDir, "sistem").apply { mkdirs() }.resolve("appWorksWithoutNotificationPermission.txt")
            .writeText("izin=${Notifier.canNotify(app)} gosterilen-bildirim=$shown hatirlatma-kuruldu=${Notifier.scheduledAt["DAILY"] != null} ekranlar-acildi=$alive\n")
        assertEquals(0, shown)
        assertNotNull(Notifier.scheduledAt["DAILY"])
        assertTrue(alive)
        r.user.saveReminder(rem.copy(enabled = false)); Notifier.schedule(app, rem.copy(enabled = false))
        s.close()
    }
}
