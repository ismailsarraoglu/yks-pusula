package app.ykspusula

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.ykspusula.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** 2.1.0 akışları gerçek veritabanıyla: deneme yanlışlarından deftere, plan süresi, öncelik sırası, gün değişimi. */
@RunWith(AndroidJUnit4::class)
class LearningFlowTest {
    private val app get() = ApplicationProvider.getApplicationContext<Context>() as YksApp
    private val r get() = app.repo
    private val topic = "tyt-mat-kumeler"

    @Before fun setup() = runBlocking {
        app.ready.first { it }
        r.clearUserData()
        r.updateSettings { it[Keys.weekly] = 0 }
        r.user.saveProfile(UserProfile(onboarded = true, name = "Test", field = "SAY", dailyMinutes = 240, maxDailyMinutes = 360, studyDays = "1,2,3,4,5,6,7", startHour = 9, endHour = 22))
    }

    @Test fun mockTopicMissesGoToWrongBook() = runBlocking {
        val id = r.saveExternalExam("Deneme 1", "Yayın", today(), "TYT", 160, listOf(SectionEntry("Temel Matematik", 25, 10, 5)), hardTopics = "$topic:3:2")
        val w = r.user.wrongsNow().filter { it.mockExamId == id }
        assertEquals(2, w.size)
        assertEquals(3, w.single { it.kind == "WRONG" }.wrongCount)
        assertEquals(2, w.single { it.kind == "BLANK" }.wrongCount)
        assertTrue(w.all { it.origin == "MOCK" && it.topicId == topic && it.subjectId == "tyt-mat" && it.sourceLabel == "Deneme 1" })
        assertNotNull(r.user.progressNow(topic)?.nextReviewAt)

        // Tekrar aşaması ilerlemişken sonuç düzeltilirse aşama korunur, boş kaldırılırsa kaydı silinir.
        val wrongId = w.single { it.kind == "WRONG" }.id
        r.reviewWrong(wrongId, 2)
        val stage = r.user.wrong(wrongId)!!.stage
        r.saveExamResults(id, "Deneme 1", "Yayın", today(), 160, listOf(SectionEntry("Temel Matematik", 25, 10, 5)), "$topic:1:0", "")
        val after = r.user.wrongsNow().filter { it.mockExamId == id }
        assertEquals(1, after.size)
        assertEquals(1, after.single().wrongCount)
        assertEquals(stage, after.single().stage)

        r.deleteExam(id)
        assertTrue(r.user.wrongsNow().none { it.mockExamId == id })
    }

    @Test fun oldHardTopicFormatStillWorks() = runBlocking {
        val id = r.saveExternalExam("Eski", "", today(), "TYT", 0, listOf(SectionEntry("Türkçe", 30, 5, 5)), hardTopics = topic)
        assertTrue(r.user.wrongsNow().none { it.mockExamId == id })
        assertNotNull(r.user.progressNow(topic)?.nextReviewAt)
    }

    @Test fun planMatchesDailyTargetAndKeepsDoneTasks() = runBlocking {
        val day = LocalDate.of(2027, 1, 4)
        val res = r.generatePlanDetailed(day)
        val byDay = r.user.tasksBetweenNow(day.toString(), day.plusDays(6).toString()).groupBy { it.date }
        assertEquals(7, byDay.size)
        byDay.values.forEach { assertEquals(240, it.sumOf { t -> t.minutes }) }
        assertEquals(7 * 240, res.studyMinutes)
        val done = byDay.getValue(day.toString()).first()
        r.toggleTask(done, true)
        r.generatePlanDetailed(day)
        val first = r.user.tasksBetweenNow(day.toString(), day.toString())
        assertTrue(first.any { it.id == done.id && it.done })
        assertEquals("Tamamlanan görev korunup hedef aşılmamalı", 240, first.sumOf { it.minutes })
    }

    @Test fun priorityListStartsWithMockMistake() = runBlocking {
        r.saveExternalExam("Deneme", "", today(), "TYT", 0, listOf(SectionEntry("Temel Matematik", 20, 10, 10)), hardTopics = "$topic:4:0")
        val (list, _) = r.priorityTopics()
        assertEquals(topic, list.first().topic.id)
        assertTrue(list.first().reasons.joinToString(), list.first().reasons.any { "deneme" in it })
    }

    @Test fun emptyUserHasNoFakePriorities() = runBlocking {
        val (list, _) = r.priorityTopics()
        assertTrue(list.all { it.reasons.all { x -> x == "Henüz başlanmadı" } })
    }

    @Test fun dayRefreshFollowsClock() {
        app.day.value = today().minusDays(1)
        app.refreshDay()
        assertEquals(today(), app.day.value)
    }
}
