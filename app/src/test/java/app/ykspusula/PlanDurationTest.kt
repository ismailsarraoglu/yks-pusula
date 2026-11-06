package app.ykspusula

import app.ykspusula.domain.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

/** Planlanan çalışma süresi hedefle karşılaştırılır. 2.0.0'da 240 dk hedef 200 dk, 30 dk hedef 50 dk planlanıyordu. */
class PlanDurationTest {
    private val monday = LocalDate.of(2026, 11, 9)

    private fun input(minutes: Int, start: Int = 9, end: Int = 23, exam: LocalDate = LocalDate.of(2027, 6, 19), wrongs: Int = 0, block: Int = 50, br: Int = 10) = PlanInput(
        start = monday, examDate = exam, dailyMinutes = minutes, studyDays = setOf(1, 2, 3, 4, 5, 6, 7), startHour = start,
        subjects = listOf(PlanSubject("mat", "Matematik", 3.0), PlanSubject("tur", "Türkçe", 3.0), PlanSubject("fiz", "Fizik", 1.0)),
        topics = (1..8).flatMap { n -> listOf(PlanTopic("m$n", "mat", "M$n", n % 3, null, 50, false), PlanTopic("t$n", "tur", "T$n", 1, 70, 50, false), PlanTopic("f$n", "fiz", "F$n", 0, null, 50, false)) },
        weakSubjectIds = emptySet(), strongSubjectIds = emptySet(), wrongsDue = wrongs, endHour = end, blockMinutes = block, breakMinutes = br, maxDailyMinutes = 720,
    )

    private fun minuteOf(t: String) = t.substringBefore(':').toInt() * 60 + t.substringAfter(':').toInt()

    private fun check(target: Int) {
        val r = PlanGenerator.plan(input(target))
        val byDay = r.tasks.groupBy { it.date }
        assertEquals(7, byDay.size)
        byDay.forEach { (d, list) ->
            assertEquals("$target dk hedef, $d", target, list.sumOf { it.minutes })
            assertTrue(list.all { it.minutes >= PlanGenerator.MIN_BLOCK })
            // Molalar çalışma süresine eklenmez, görevler üst üste binmez.
            list.zipWithNext().forEach { (a, b) -> assertEquals(minuteOf(a.time) + a.minutes + a.breakAfter, minuteOf(b.time)) }
            assertEquals(0, list.last().breakAfter)
            assertTrue(list.all { it.reason.isNotBlank() })
        }
        r.days.forEach { assertEquals(target, it.plannedMinutes) }
        assertTrue(r.notes.isEmpty())
    }

    @Test fun minutes30() = check(30)
    @Test fun minutes45() = check(45)
    @Test fun minutes60() = check(60)
    @Test fun minutes90() = check(90)
    @Test fun minutes120() = check(120)
    @Test fun minutes180() = check(180)
    @Test fun minutes240() = check(240)
    @Test fun minutes300() = check(300)

    @Test fun lastBlockUsesRemainder() {
        assertEquals(listOf(50, 50, 50, 50, 40), PlanGenerator.split(240, 50))
        assertEquals(listOf(30), PlanGenerator.split(30, 50))
        assertEquals(listOf(60), PlanGenerator.split(60, 50))
        assertEquals(listOf(50, 40), PlanGenerator.split(90, 50))
        assertEquals(listOf(40, 40, 40), PlanGenerator.split(120, 40))
        assertEquals(emptyList<Int>(), PlanGenerator.split(0, 50))
    }

    @Test fun wrongReviewIsPartOfTarget() {
        val r = PlanGenerator.plan(input(240, wrongs = 40))
        r.tasks.groupBy { it.date }.values.forEach { assertEquals(240, it.sumOf { t -> t.minutes }) }
        assertTrue(r.tasks.any { it.kind == "WRONGS" })
    }

    @Test fun notEnoughHoursIsExplained() {
        val r = PlanGenerator.plan(input(300, start = 18, end = 21))
        r.tasks.groupBy { it.date }.values.forEach { d ->
            assertTrue(d.sumOf { it.minutes } < 300)
            d.forEach { assertTrue(minuteOf(it.time) + it.minutes <= 21 * 60) }
        }
        assertTrue(r.notes.joinToString(), r.notes.any { "sığıyor" in it })
        assertTrue(r.days.all { it.note.isNotBlank() })
    }

    @Test fun restDaysAndStudyDaysRespected() {
        val rest = monday.plusDays(2)
        val r = PlanGenerator.plan(input(120).copy(studyDays = setOf(1, 2, 3, 4, 5), restDates = setOf(rest)))
        val dates = r.tasks.map { it.date }.toSet()
        assertFalse(rest in dates)
        assertTrue(dates.none { it.dayOfWeek.value > 5 })
        assertEquals(4, dates.size)
    }

    @Test fun weeklyGoalCapsTheWeek() {
        val r = PlanGenerator.plan(input(120).copy(weeklyMinutes = 300))
        assertEquals(300, r.studyMinutes)
    }

    @Test fun keptTasksReduceTarget() {
        val r = PlanGenerator.plan(input(240).copy(keptMinutes = mapOf(monday to 100)))
        assertEquals(140, r.tasks.filter { it.date == monday }.sumOf { it.minutes })
    }

    @Test fun examVeryCloseFavoursReview() {
        val i = input(180, exam = monday.plusDays(5)).copy(
            topics = listOf(PlanTopic("new", "mat", "Yeni", 0, null, 50, false), PlanTopic("old", "mat", "Eski", 1, 50, 50, true), PlanTopic("t", "tur", "T", 2, 70, 50, false)),
        )
        val r = PlanGenerator.plan(i)
        assertEquals(5, r.tasks.map { it.date }.toSet().size)
        assertTrue(r.notes.any { "Sınava" in it })
        val firstMat = r.tasks.first { it.subjectId == "mat" }
        assertEquals("old", firstMat.topicId)
    }

    @Test fun noSameTopicTwiceInADay() {
        val r = PlanGenerator.plan(input(300))
        r.tasks.groupBy { it.date }.values.forEach { d -> val ids = d.mapNotNull { it.topicId }; assertEquals(ids.size, ids.toSet().size) }
    }

    @Test fun mockMistakesComeFirstWithReason() {
        val i = input(150).copy(topics = input(150).topics.map { if (it.id == "m5") it.copy(mockMisses = 3) else it })
        val r = PlanGenerator.plan(i)
        val t = r.tasks.first { it.topicId == "m5" }
        assertTrue(t.reason, t.reason.contains("deneme"))
        assertEquals(1, t.priority)
    }

    @Test fun targetGapRaisesSubjectAndExplains() {
        val base = input(240)
        val gapped = base.copy(subjects = base.subjects.map { if (it.id == "fiz") it.copy(gap = 6.0, lastNet = 4.0, targetNet = 10.0) else it })
        val before = PlanGenerator.plan(base).tasks.count { it.subjectId == "fiz" }
        val after = PlanGenerator.plan(gapped).tasks
        assertTrue(after.count { it.subjectId == "fiz" } > before)
        assertTrue(after.first { it.subjectId == "fiz" }.reason.contains("hedefin"))
    }

    @Test fun capAboveMaxIsExplained() {
        val r = PlanGenerator.plan(input(600).copy(maxDailyMinutes = 300))
        r.tasks.groupBy { it.date }.values.forEach { assertEquals(300, it.sumOf { t -> t.minutes }) }
        assertTrue(r.notes.any { "sınır" in it })
    }
}
