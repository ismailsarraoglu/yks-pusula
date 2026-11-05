package app.ykspusula

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.ykspusula.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Script bu iki testi ayrı süreçlerde, araya uygulama durdurma ve cihaz yeniden başlatma koyarak çalıştırır. */
@RunWith(AndroidJUnit4::class)
class PersistenceTest {
    private val app get() = ApplicationProvider.getApplicationContext<Context>() as YksApp

    @Test fun writeData() = runBlocking {
        app.ready.first { it }
        app.repo.clearUserData()
        app.repo.user.saveProfile(UserProfile(onboarded = true, name = "Kalıcı"))
        app.repo.user.saveNote(UserNote(id = "kalici", title = "Silinmemeli", body = "x", createdAt = 1, updatedAt = 1))
        app.repo.updateSettings { it[Keys.theme] = "DARK" }
    }

    @Test fun readData() = runBlocking {
        app.ready.first { it }
        assertEquals("Kalıcı", app.repo.user.profileNow()?.name)
        assertEquals("Silinmemeli", app.repo.user.note("kalici")?.title)
        assertEquals("DARK", app.repo.settings.first().theme)
    }
}
