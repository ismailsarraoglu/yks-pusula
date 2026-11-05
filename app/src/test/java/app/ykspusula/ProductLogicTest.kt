package app.ykspusula

import app.ykspusula.data.*
import app.ykspusula.domain.*
import app.ykspusula.monetize.CachedPurchase
import app.ykspusula.monetize.EntitlementLogic
import app.ykspusula.monetize.PurchaseQuery
import app.ykspusula.monetize.REMOVE_ADS_PRODUCT
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.security.MessageDigest
import java.time.LocalDate

class ProductLogicTest {
    private val day = 86_400_000L
    private val now = 1_800_000_000_000L
    private fun mastery(status: Int = 0, solved: Int = 0, correct: Int = 0, reviews: Int = 0, last: Long? = null, next: Long? = null, wrong: Int = 0, min: Int = 0) =
        Mastery.compute(MasteryInput(status, solved, correct, reviews, last, next, wrong, min, now))

    @Test fun masteryLevels() {
        assertEquals(0, mastery().level)
        assertEquals(1, mastery(status = 1).level)
        assertEquals(2, mastery(status = 1, solved = 20, correct = 8).level)
        assertEquals(3, mastery(status = 3, last = now - day).level)
    }

    @Test fun oneMarkCannotMakeStrong() {
        assertEquals(3, mastery(status = 3, last = now).level)
        assertEquals(3, mastery(status = 3, solved = 100, correct = 95, reviews = 1, last = now).level)
        assertEquals(4, mastery(status = 3, solved = 50, correct = 45, reviews = 2, last = now - day).level)
        assertEquals(4, mastery(status = 1, solved = 50, correct = 45, reviews = 2, last = now - day).level)
    }

    @Test fun overdueReviewDropsToNeedsReview() {
        assertEquals(2, mastery(status = 3, solved = 50, correct = 45, reviews = 2, last = now - 2 * day, next = now - day).level)
        assertEquals(2, mastery(status = 3, last = now - 60 * day).level)
        assertEquals(2, mastery(status = 1, wrong = 3, last = now).level)
    }

    private fun input(
        topics: List<CoachTopic> = emptyList(), wrongs: Int = 0, cards: Int = 0, sections: List<CoachSection> = emptyList(), rejected: Set<String> = emptySet(),
        missed: List<CoachMissed> = emptyList(), sources: List<CoachSource> = emptyList(), hard: Set<String> = emptySet(), days: Int = 10, rest: Boolean = false, done: Int = 0, daily: Int = 120,
    ) = CoachInput(
        LocalDate.of(2026, 11, 10), now, daily, done, mapOf("m" to 3.0, "t" to 3.0, "f" to 1.0), mapOf("m" to "Matematik", "t" to "Türkçe", "f" to "Fizik"), emptySet(), emptySet(), emptySet(),
        topics, sources, sections, hard, missed, wrongs, cards, rejected, days, rest,
    )
    private fun topic(id: String, s: String = "m", level: Int = 1, solved: Int = 0, correct: Int = 0, due: Boolean = false, last: Long? = null) =
        CoachTopic(id, s, "Konu $id", "Ders $s", level, solved, correct, last, due, 0)

    @Test fun coachExplainsEverySuggestion() {
        val list = PusulaKoc.suggest(input(listOf(topic("a", solved = 20, correct = 8), topic("b", "t", due = true, last = now - 5 * day), topic("c", "f", level = 0)), wrongs = 4))
        assertTrue(list.isNotEmpty())
        assertTrue(list.all { it.reason.isNotBlank() })
        assertTrue(list.any { it.kind == "WRONGS" && "4 yanlış" in it.reason })
        assertTrue(list.any { "%40" in it.reason })
        assertTrue(list.any { "5 gün önce" in it.reason })
    }

    @Test fun coachDoesNotClaimTrendWithFewExams() {
        val few = PusulaKoc.suggest(input(listOf(topic("a")), sections = listOf(CoachSection("TYT", "Matematik", listOf(30.0, 20.0), listOf("m")))))
        assertTrue(few.none { "netin düşüyor" in it.reason })
        val many = PusulaKoc.suggest(input(listOf(topic("a")), sections = listOf(CoachSection("TYT", "Matematik", listOf(30.0, 25.0, 20.0), listOf("m")))))
        assertTrue(many.any { "netin düşüyor" in it.reason })
    }

    @Test fun coachRespectsBudgetRejectionAndRest() {
        val all = (1..20).map { topic("t$it", if (it % 2 == 0) "m" else "t", level = 0) }
        val s = PusulaKoc.suggest(input(all, wrongs = 3, cards = 5, daily = 60))
        assertTrue(s.sumOf { it.minutes } <= 60)
        assertTrue(s.size <= PusulaKoc.MAX_ITEMS)
        assertEquals(s.size, s.mapNotNull { it.topicId }.toSet().size + s.count { it.topicId == null })
        assertTrue(PusulaKoc.suggest(input(listOf(topic("a", solved = 20, correct = 8)), rejected = setOf("a"))).none { it.topicId == "a" })
        assertTrue(PusulaKoc.suggest(input(all, rest = true)).isEmpty())
    }

    @Test fun coachUsesSourceAndHardTopics() {
        val s = PusulaKoc.suggest(input(listOf(topic("a", level = 2)), hard = setOf("a"), sources = listOf(CoachSource("k", "Mat Bankası", "m", null, 52, 300))))
        assertTrue(s.any { "zorlandığını" in it.reason && "52. sayfadan" in it.reason })
    }

    @Test fun replanRespectsCapacityAndBreaks() {
        val d = LocalDate.of(2026, 11, 10)
        val missed = (1..6).map { SlotTask("m$it", d.minusDays(1), "09:00", 50, if (it <= 2) 1 else 3, "G$it") }
        val up = listOf(SlotTask("u1", d, "09:00", 50, 2, "Var"))
        val r = Replanner.preview(ReplanInput(d, missed, up, setOf(1, 2, 3, 4, 5, 6, 7), 120, 9, 22, 10), false)
        assertEquals(6, r.moves.size)
        val perDay = r.moves.groupBy { it.to }.mapValues { (k, v) -> v.size * 50 + if (k == d) 50 else 0 }
        assertTrue(perDay.values.all { it <= 120 })
        assertTrue(r.moves.first { it.to == d }.time >= "10:00")
        val important = Replanner.preview(ReplanInput(d, missed, up, setOf(1, 2, 3, 4, 5, 6, 7), 120, 9, 22, 10), true)
        assertEquals(setOf("m1", "m2"), important.moves.map { it.id }.toSet())
    }

    @Test fun replanReportsWhatDoesNotFit() {
        val d = LocalDate.of(2026, 11, 10)
        val r = Replanner.preview(ReplanInput(d, (1..30).map { SlotTask("m$it", d.minusDays(1), "09:00", 50, 2, "G") }, emptyList(), setOf(2), 100, 9, 22, 10, 7), false)
        assertEquals(2, r.moves.size)
        assertEquals(28, r.unplaced.size)
    }

    @Test fun planHonoursMaxCapacityAndEndHour() {
        val i = PlanInput(LocalDate.of(2026, 11, 9), LocalDate.of(2027, 6, 19), 600, setOf(1, 2, 3, 4, 5, 6, 7), 9, listOf(PlanSubject("m", "Mat", 3.0), PlanSubject("t", "Tür", 3.0)),
            listOf(PlanTopic("a", "m", "A", 0, null, 100, false), PlanTopic("b", "t", "B", 0, 40, 100, false)), emptySet(), emptySet(), 0, maxDailyMinutes = 200, endHour = 13, priorityTopics = setOf("b"))
        val t = PlanGenerator.generate(i)
        assertEquals("b", t.first().topicId)
        assertTrue(t.first().reason.isNotBlank())
        t.groupBy { it.date }.values.forEach { d -> assertTrue(d.sumOf { it.minutes } <= 200); assertTrue(d.all { it.time < "13:00" }) }
    }

    @Test fun wrongReviewChangesInterval() {
        val a = Repetition.onReview(1, 2, now); assertEquals(2, a.stage); assertEquals(now + 7 * day, a.nextReviewAt)
        val b = Repetition.onReview(2, 1, now); assertEquals(2, b.stage); assertEquals(now + 7 * day, b.nextReviewAt)
        val c = Repetition.onReview(3, 0, now); assertEquals(0, c.stage); assertEquals(now + day, c.nextReviewAt)
        assertTrue(Repetition.onReview(4, 2, now).resolved)
    }

    @Test fun restDayKeepsStreak() {
        val t = LocalDate.of(2026, 11, 10)
        val days = listOf(t.minusDays(3), t.minusDays(1)).map { it.toString() }
        assertEquals(1, Streaks.current(days, t))
        assertEquals(2, Streaks.current(days, t, listOf(t.minusDays(2).toString())))
    }

    @Test fun reminderFrequency() {
        val zone = java.time.ZoneId.of("Europe/Istanbul")
        val friEvening = LocalDate.of(2026, 11, 6).atTime(21, 0).atZone(zone).toInstant().toEpochMilli()
        val next = ReminderTime.nextFor(friEvening, 20, 0, "WEEKDAYS", 7, zone)
        assertEquals(1, java.time.Instant.ofEpochMilli(next).atZone(zone).dayOfWeek.value)
    }

    @Test fun netHelpers() {
        assertEquals(32, Scoring.correctNeeded(30.0, 8))
        assertEquals(51.0, Scoring.obpContribution(85.0, false), 1e-9)
    }

    @Test fun examAnalysisNeedsEnoughData() {
        fun row(e: String, at: Long, c: Int, w: Int, b: Int) = SectionResultRow(e, at, "Mat", c, w, b, c - w / 4.0)
        val one = ExamAnalyzer.analyze(listOf(row("1", 1, 20, 8, 12)), listOf(1L to 18.0), listOf(120))
        assertNull(one.trendNote); assertNull(one.sections.first().trend)
        val three = ExamAnalyzer.analyze(listOf(row("1", 1, 20, 8, 12), row("2", 2, 24, 8, 8), row("3", 3, 28, 8, 4)), listOf(1L to 18.0, 2L to 22.0, 3L to 26.0), listOf(120, 0, 130))
        assertTrue(three.trendNote!!.contains("yükseliyor"))
        assertEquals(125, three.avgMinutes)
        assertEquals(8.0, three.blankAverage, 1e-9)
    }

    @Test fun weeklyReviewAvoidsConclusionsWithoutData() {
        assertFalse(WeeklyCoach.build(WeekFacts(emptyList(), emptyMap(), emptyMap(), emptyList(), emptyList(), 1)).hasEnoughData)
        val w = WeeklyCoach.build(WeekFacts(listOf(TopicWeek("Üslü", "Mat", 20, 18, 60)), mapOf("Mat" to 100), mapOf("Mat" to (20 to 18), "Fizik" to (20 to 8)), listOf("Limit"), listOf("Görev"), 3))
        assertEquals("Mat / Üslü", w.bestTopic); assertEquals("Fizik", w.hardestSubject); assertEquals(3, w.nextWeek.size)
    }

    /* ---------- Motivasyon ---------- */

    private val quotes: List<Quote> by lazy {
        Json.decodeFromString(ListSerializer(Quote.serializer()), File("src/main/assets/content/motivation.json").readText())
    }

    @Test fun exactlyHundredUniqueQuotes() {
        assertEquals(100, quotes.size)
        assertEquals(100, quotes.map { it.id }.toSet().size)
        assertEquals(100, quotes.map { it.text.trim().lowercase() }.toSet().size)
        assertEquals(10, quotes.map { it.category }.toSet().size)
        assertTrue(quotes.groupBy { it.category }.values.all { it.size == 10 })
    }

    @Test fun dailyQuoteStableAndNoRepeatInCycle() {
        val start = 20_500L * 100
        val seen = (0 until 100).map { DailyQuote.indexFor(start + it, 100) }
        assertEquals(100, seen.toSet().size)
        assertEquals(DailyQuote.indexFor(start + 5, 100), DailyQuote.indexFor(start + 5, 100))
        (1..50).forEach { c -> assertNotEquals(DailyQuote.indexFor(start + 100L * c - 1, 100), DailyQuote.indexFor(start + 100L * c, 100)) }
    }

    /* ---------- Reklamsız hak ---------- */

    private val json = "{\"productId\":\"$REMOVE_ADS_PRODUCT\"}"

    @Test fun entitlementOnlyFromVerifiedPurchase() {
        assertFalse(EntitlementLogic.isValid(null, ""))
        assertFalse(EntitlementLogic.isValid(EntitlementLogic.next(null, PurchaseQuery.Pending, now), ""))
        assertFalse(EntitlementLogic.isValid(EntitlementLogic.next(null, PurchaseQuery.None, now), ""))
        val c = EntitlementLogic.next(null, PurchaseQuery.Purchased("tok", json, "sig"), now)
        assertTrue(EntitlementLogic.isValid(c, ""))
        assertFalse(EntitlementLogic.isValid(CachedPurchase("x", "{}", "", 1), ""))
    }

    @Test fun entitlementSurvivesOfflineAndRevokesOnNone() {
        val c = EntitlementLogic.next(null, PurchaseQuery.Purchased("tok", json, "sig"), now)
        assertEquals(c, EntitlementLogic.next(c, PurchaseQuery.Unavailable, now + day))
        assertEquals(c, EntitlementLogic.next(c, PurchaseQuery.Pending, now + day))
        assertNull(EntitlementLogic.next(c, PurchaseQuery.None, now + day))
    }

    @Test fun signatureMustMatchWhenKeySet() {
        val kp = java.security.KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val pub = java.util.Base64.getEncoder().encodeToString(kp.public.encoded)
        val sig = java.security.Signature.getInstance("SHA1withRSA").run { initSign(kp.private); update(json.toByteArray()); java.util.Base64.getEncoder().encodeToString(sign()) }
        assertTrue(EntitlementLogic.isValid(CachedPurchase("h", json, sig, 1), pub))
        assertFalse(EntitlementLogic.isValid(CachedPurchase("h", json.replace("remove", "x_remove"), sig, 1), pub))
    }

    /* ---------- Yedek ---------- */

    @Test fun backupRoundTripAndTamper() {
        val text = BackupCodec.encode(BackupData(sources = listOf(StudySource(id = "s", name = "Kitap", createdAt = 1, updatedAt = 1))), 5, "2.0.0")
        val ok = BackupCodec.decode(text) as BackupCheck.Ok
        assertEquals("Kitap", ok.file.data.sources.single().name)
        assertTrue(BackupCodec.decode(text.replace("Kitap", "Kitab")) is BackupCheck.Bad)
        assertTrue(BackupCodec.decode("bozuk") is BackupCheck.Bad)
        assertTrue(BackupCodec.decode("{\"a\":1}") is BackupCheck.Bad)
    }

    @Test fun oldBackupIsConverted() {
        val body = """{"profile":null,"attempts":[{"questionId":"q1","topicId":"t","subjectId":"s","selected":"A","isCorrect":true,"answeredAt":1000,"source":"BANK"},{"questionId":"q2","topicId":"t","subjectId":"s","selected":"B","isCorrect":false,"answeredAt":1001,"source":"BANK"}],"wrongs":[{"questionId":"q2","topicId":"t","subjectId":"s","givenAnswer":"5","correctAnswer":"6","firstWrongAt":1,"lastWrongAt":2,"nextReviewAt":3}],"progress":[{"topicId":"t","status":2}],"bookmarks":[{"kind":"QUESTION","refId":"q1","createdAt":1},{"kind":"FORMULA","refId":"f","createdAt":1}]}"""
        val sha = MessageDigest.getInstance("SHA-256").digest(Json.parseToJsonElement(body).toString().toByteArray()).joinToString("") { "%02x".format(it) }
        val text = """{"format":"$BACKUP_FORMAT","version":1,"createdAt":9,"appVersion":"1.0.0","checksum":"$sha","data":$body}"""
        val d = (BackupCodec.decode(text) as BackupCheck.Ok).file.data
        assertEquals(1, d.logs.single().correct); assertEquals(1, d.logs.single().wrong)
        assertEquals("6", d.wrongs.single().correctAnswer)
        assertEquals(1, d.progress.single().status)
        assertEquals(listOf("FORMULA"), d.bookmarks.map { it.kind })
    }

    @Test fun contentPackValidation() {
        val base = ContentPackFile(PackHeader("p", "T", "1", source = "ÖSYM"), subjects = listOf(SubjectDto("s", "TYT", "Mat")), topics = listOf(TopicDto("t", "s", name = "K")))
        assertTrue(ContentValidator.validate(base).isEmpty())
        assertFalse(ContentValidator.validate(base.copy(pack = PackHeader("p", "T", "1"))).isEmpty())
        assertFalse(ContentValidator.validate(base.copy(topics = listOf(TopicDto("t", "yok", name = "K")))).isEmpty())
    }

    @Test fun seedHasNoQuestions() {
        val text = File("src/main/assets/content/base.json").readText()
        val p = ContentValidator.parse(text)
        assertEquals(26, p.subjects.size); assertEquals(261, p.topics.size); assertEquals(236, p.formulas.size); assertEquals(411, p.flashcards.size)
        assertFalse(text.contains("\"questions\""))
        assertTrue(ContentValidator.validate(p).isEmpty())
    }
}
