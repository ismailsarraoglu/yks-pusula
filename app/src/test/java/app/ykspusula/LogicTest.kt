package app.ykspusula

import app.ykspusula.domain.*
import org.junit.Assert.*
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class LogicTest {
    @Test fun netMatchesFormula() {
        assertEquals(30.0, Scoring.net(32, 8), 1e-9)
        assertEquals(0.0, Scoring.net(0, 0), 1e-9)
        assertEquals(-2.5, Scoring.net(0, 10), 1e-9)
        assertEquals(35.75, Scoring.net(37, 5), 1e-9)
        assertEquals(30.0, Scoring.net(32, 6, 3.0), 1e-9)
    }

    @Test fun blankNeverChangesNet() {
        for (c in 0..40) for (w in 0..(40 - c)) {
            assertEquals(c - w / 4.0, Scoring.net(c, w), 1e-9)
        }
    }

    @Test(expected = IllegalArgumentException::class) fun negativeRejected() { Scoring.net(-1, 0) }
    @Test(expected = IllegalArgumentException::class) fun zeroDivisorRejected() { Scoring.net(1, 1, 0.0) }

    @Test fun sectionValidation() {
        assertTrue(Scoring.validSection(30, 5, 5, 40))
        assertFalse(Scoring.validSection(30, 6, 5, 40))
        assertFalse(Scoring.validSection(-1, 0, 0, 40))
    }

    @Test fun percentRounds() {
        assertEquals(0, Scoring.percent(0, 0))
        assertEquals(33, Scoring.percent(1, 3))
        assertEquals(67, Scoring.percent(2, 3))
    }

    @Test fun obpRules() {
        assertEquals(425.0, Scoring.obp(85.0), 1e-9)
        assertEquals(51.0, Scoring.obpContribution(85.0, false), 1e-9)
        assertEquals(25.5, Scoring.obpContribution(85.0, true), 1e-9)
    }

    @Test fun repetitionFollowsIntervals() {
        val now = 1_000_000L
        val first = Repetition.onWrong(now)
        assertEquals(now + Repetition.DAY_MS, first.nextReviewAt)
        var stage = first.stage
        val expected = listOf(3, 7, 14, 30)
        for (days in expected) {
            val o = Repetition.onCorrect(stage, now)
            assertEquals(now + days * Repetition.DAY_MS, o.nextReviewAt)
            assertFalse(o.resolved)
            stage = o.stage
        }
        assertTrue(Repetition.onCorrect(stage, now).resolved)
    }

    @Test fun cardRatings() {
        val now = 0L
        assertEquals(0, Repetition.cardOutcome(3, 0, now).first)
        assertEquals(600_000L, Repetition.cardOutcome(3, 0, now).second)
        assertEquals(3, Repetition.cardOutcome(3, 1, now).first)
        assertEquals(4, Repetition.cardOutcome(3, 2, now).first)
        assertEquals(4, Repetition.cardOutcome(4, 2, now).first)
    }

    @Test fun turkishNormalization() {
        assertEquals("sekil cizim igne", SearchText.normalize("ŞEKİL Çizim İĞNE"))
        assertEquals("ikinci dereceden", SearchText.normalize("İkinci  Dereceden!"))
        assertEquals(SearchText.normalize("ığüşöç"), SearchText.normalize("IĞÜŞÖÇ"))
    }

    @Test fun fuzzySearch() {
        val title = SearchText.normalize("Türev ve Uygulamaları")
        assertTrue(SearchText.score(SearchText.normalize("türev"), title) > 0)
        assertTrue(SearchText.score(SearchText.normalize("turev"), title) > 0)
        assertTrue(SearchText.score(SearchText.normalize("tuerv"), title) > 0)
        assertEquals(0, SearchText.score(SearchText.normalize("integral"), title))
        val exact = SearchText.score("turev", "turev")
        val inner = SearchText.score("turev", "matematik turev")
        assertTrue(exact > inner)
    }

    @Test fun streaks() {
        val t = LocalDate.of(2026, 11, 5)
        val days = listOf("2026-11-05", "2026-11-04", "2026-11-03", "2026-11-01")
        assertEquals(3, Streaks.current(days, t))
        assertEquals(2, Streaks.current(days.drop(1), t))
        assertEquals(0, Streaks.current(listOf("2026-11-01"), t))
        assertEquals(3, Streaks.longest(days))
        assertEquals(0, Streaks.longest(emptyList()))
    }

    @Test fun timer() {
        assertEquals(0, Timers.remainingSec(1000, 5000))
        assertEquals(60, Timers.remainingSec(61_000, 1_000))
        assertEquals("01:05", Timers.clock(65))
        assertEquals("2:45:00", Timers.clock(9900))
    }

    private fun input(days: Set<Int> = setOf(1, 2, 3, 4, 5), minutes: Int = 240, wrongs: Int = 0) = PlanInput(
        start = LocalDate.of(2026, 11, 9),
        examDate = LocalDate.of(2027, 6, 19),
        dailyMinutes = minutes,
        studyDays = days,
        startHour = 9,
        subjects = listOf(PlanSubject("mat", "Matematik", 3.0), PlanSubject("tur", "Türkçe", 3.0), PlanSubject("fiz", "Fizik", 1.0)),
        topics = (1..6).flatMap { n ->
            listOf(
                PlanTopic("m$n", "mat", "M$n", 0, if (n == 1) 20 else null, 50, false),
                PlanTopic("t$n", "tur", "T$n", 3, 90, 50, n == 2),
                PlanTopic("f$n", "fiz", "F$n", 1, null, 50, false),
            )
        },
        weakSubjectIds = setOf("mat"),
        strongSubjectIds = setOf("tur"),
        wrongsDue = wrongs,
    )

    @Test fun planRespectsDaysAndLimits() {
        val tasks = PlanGenerator.generate(input())
        assertTrue(tasks.isNotEmpty())
        assertTrue(tasks.all { it.date.dayOfWeek != DayOfWeek.SATURDAY && it.date.dayOfWeek != DayOfWeek.SUNDAY })
        tasks.groupBy { it.date }.forEach { (_, list) ->
            assertTrue(list.sumOf { it.minutes } <= 240)
            assertTrue(list.size <= PlanGenerator.MAX_BLOCKS_PER_DAY)
        }
    }

    @Test fun planFavoursWeakButKeepsStrong() {
        val tasks = PlanGenerator.generate(input(minutes = 350))
        val mat = tasks.count { it.subjectId == "mat" }
        val tur = tasks.count { it.subjectId == "tur" }
        val fiz = tasks.count { it.subjectId == "fiz" }
        assertTrue("mat=$mat tur=$tur", mat > tur)
        assertTrue(tur > 0)
        assertTrue(fiz > 0)
    }

    @Test fun planAddsWrongReview() {
        val tasks = PlanGenerator.generate(input(wrongs = 30))
        assertTrue(tasks.any { it.kind == "WRONGS" })
        assertTrue(PlanGenerator.generate(input(wrongs = 0)).none { it.kind == "WRONGS" })
    }

    @Test fun noPlanAfterExam() {
        val i = input().copy(examDate = LocalDate.of(2026, 11, 9))
        assertTrue(PlanGenerator.generate(i).isEmpty())
    }

    @Test fun advisorUsesRealData() {
        assertTrue(Advisor.build(emptyList(), emptyList(), 0, 0, null, 0, 0).isEmpty())
        val drop = listOf(SectionPoint("TYT Matematik", 1, 30.0), SectionPoint("TYT Matematik", 2, 25.0), SectionPoint("TYT Matematik", 3, 20.0))
        val a = Advisor.build(drop, listOf(TopicScore("Elektrik", 10, 3)), 2, 0, null, 0, 0)
        assertTrue(a.any { it.kind == "NET_DROP" && "TYT Matematik" in it.text })
        assertTrue(a.any { it.kind == "WEAK_TOPIC" && "%30" in it.text })
        assertTrue(a.any { it.kind == "WRONGS" })
    }

    @Test fun examBuilderNoDuplicatesAndFlagsShortage() {
        val avail = mapOf("a" to (1..10).map { "a$it" }, "b" to (1..3).map { "b$it" })
        val picked = ExamBuilder.pick(
            listOf(SectionPlan("A", listOf(PartPlan("a", 8))), SectionPlan("B", listOf(PartPlan("b", 5)))), avail, setOf("a1", "a2"), 7,
        )
        assertEquals(8, picked[0].questionIds.size)
        assertEquals(3, picked[1].questionIds.size)
        assertEquals(5, picked[1].requested)
        val all = picked.flatMap { it.questionIds }
        assertEquals(all.size, all.toSet().size)
        assertEquals(165, ExamBuilder.scaledDuration(165, 120, 120))
        assertEquals(83, ExamBuilder.scaledDuration(165, 120, 60))
    }

    @Test fun badgesFromRealStats() {
        val zero = Badges.all(HabitStats(0, 0, 0, 0, 0, 0, 0))
        assertTrue(zero.none { it.earned })
        val some = Badges.all(HabitStats(12, 0, 0, 0, 0, 0, 0))
        assertEquals(1, some.count { it.earned })
    }
}
