package app.ykspusula.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/* ---------- KONU USTALIĞI ---------- */

/** 0 Başlanmadı, 1 Çalışılıyor, 2 Tekrar gerekiyor, 3 Öğrenildi, 4 Güçlü. */
data class MasteryInput(
    val manualStatus: Int,
    val solved: Int,
    val correct: Int,
    val reviewCount: Int,
    val lastStudiedAt: Long?,
    val nextReviewAt: Long?,
    val wrongOpen: Int,
    val minutes: Int,
    val now: Long,
)
data class MasteryResult(val level: Int, val explanation: String)

object Mastery {
    val LABELS = listOf("Başlanmadı", "Çalışılıyor", "Tekrar gerekiyor", "Öğrenildi", "Güçlü")
    const val STRONG_MIN_SOLVED = 40
    const val STRONG_MIN_RATE = 80
    const val STRONG_MIN_REVIEWS = 2
    const val LOW_RATE = 60
    const val MIN_SOLVED_FOR_RATE = 10
    private const val DAY = 86_400_000L

    /** Tek bir işaretleme "Güçlü" yapmaz: bu düzey yalnızca yeterli soru, başarı ve tekrar kaydıyla oluşur. */
    fun compute(i: MasteryInput): MasteryResult {
        val rate = if (i.solved > 0) Scoring.percent(i.correct, i.solved) else null
        val hasActivity = i.solved > 0 || i.minutes > 0 || i.manualStatus > 0 || i.lastStudiedAt != null
        if (!hasActivity) return MasteryResult(0, "Bu konu için henüz kayıt yok.")
        val daysSince = i.lastStudiedAt?.let { ((i.now - it) / DAY).toInt() }
        val dueNow = i.nextReviewAt != null && i.nextReviewAt <= i.now
        val lowRate = rate != null && i.solved >= MIN_SOLVED_FOR_RATE && rate < LOW_RATE
        val fresh = daysSince == null || daysSince <= 45
        if (rate != null && i.solved >= STRONG_MIN_SOLVED && rate >= STRONG_MIN_RATE && i.reviewCount >= STRONG_MIN_REVIEWS && i.wrongOpen <= 1 && fresh && !dueNow) {
            return MasteryResult(4, "${i.solved} soruda %$rate doğru ve ${i.reviewCount} tekrar kaydın var.")
        }
        if (lowRate) return MasteryResult(2, "${i.solved} soruda doğru oranın %$rate. Tekrar iyi gelebilir.")
        if (dueNow) return MasteryResult(2, "Planlanan tekrar zamanı geldi.")
        if (i.wrongOpen >= 3) return MasteryResult(2, "Bu konuda açık ${i.wrongOpen} yanlış kaydın var.")
        if (i.manualStatus == 2) return MasteryResult(2, "Bu konuyu tekrar edilecek olarak işaretledin.")
        if (i.manualStatus >= 3) {
            if (daysSince != null && daysSince > 45) return MasteryResult(2, "Son çalışmanın üzerinden $daysSince gün geçti.")
            val need = listOfNotNull(
                (STRONG_MIN_SOLVED - i.solved).takeIf { it > 0 }?.let { "$it soru daha" },
                (STRONG_MIN_REVIEWS - i.reviewCount).takeIf { it > 0 }?.let { "$it tekrar daha" },
            )
            return MasteryResult(3, if (need.isEmpty()) "Konu öğrenildi olarak işaretli." else "Güçlü düzeyi için ${need.joinToString(" ve ")} gerekiyor.")
        }
        return MasteryResult(1, if (rate != null) "${i.solved} soruda doğru oranın %$rate." else "Çalışma kaydın var, henüz soru girilmedi.")
    }
}

/* ---------- PUSULA KOÇ ---------- */

data class CoachTopic(
    val id: String, val subjectId: String, val name: String, val subjectName: String, val level: Int,
    val solved: Int, val correct: Int, val lastStudiedAt: Long?, val reviewDue: Boolean, val wrongOpen: Int,
)
data class CoachSource(val id: String, val name: String, val subjectId: String?, val topicId: String?, val lastPage: Int, val totalPages: Int)
data class CoachSection(val exam: String, val section: String, val nets: List<Double>, val subjectIds: List<String>)
data class CoachMissed(val taskId: String, val title: String, val subjectId: String?, val topicId: String?, val minutes: Int, val date: LocalDate)
data class CoachInput(
    val date: LocalDate,
    val now: Long,
    val dailyMinutes: Int,
    val minutesDoneToday: Int,
    val subjects: Map<String, Double>,
    val subjectNames: Map<String, String>,
    val weakSubjects: Set<String>,
    val strongSubjects: Set<String>,
    val priorityTopics: Set<String>,
    val topics: List<CoachTopic>,
    val sources: List<CoachSource>,
    val sections: List<CoachSection>,
    val hardTopics: Set<String>,
    val missed: List<CoachMissed>,
    val wrongsDue: Int,
    val cardsDue: Int,
    val rejectedTopics: Set<String>,
    val daysWithData: Int,
    val isRestDay: Boolean = false,
)
data class CoachSuggestion(
    val kind: String, val title: String, val subjectId: String?, val topicId: String?, val sourceId: String?,
    val minutes: Int, val priority: Int, val reason: String, val score: Double,
)

object PusulaKoc {
    const val MIN_EXAMS_FOR_TREND = 3
    const val MAX_ITEMS = 5
    private const val DAY = 86_400_000L

    private fun sourceFor(i: CoachInput, t: CoachTopic): CoachSource? =
        i.sources.firstOrNull { it.topicId == t.id } ?: i.sources.firstOrNull { it.subjectId == t.subjectId }

    private fun sourcePart(s: CoachSource?): String = s?.let { " Kaynak: ${it.name}" + if (it.lastPage > 0) ", ${it.lastPage}. sayfadan devam." else "." }.orEmpty()

    /** Her öneri, gerekçesini gerçek kayıtlardan alır. Yeterli veri yoksa bunu açıkça söyler. */
    fun suggest(i: CoachInput): List<CoachSuggestion> {
        if (i.isRestDay) return emptyList()
        val c = ArrayList<CoachSuggestion>()
        val remaining = max(30, i.dailyMinutes - i.minutesDoneToday)

        i.missed.take(3).forEach { m ->
            val days = ChronoUnit.DAYS.between(m.date, i.date).toInt()
            c += CoachSuggestion("MISSED", m.title, m.subjectId, m.topicId, null, min(m.minutes, 50).coerceAtLeast(15), 1,
                (if (days <= 1) "Dün" else "$days gün önce") + " planlanan bu görev tamamlanmadı.", 90.0 - days)
        }
        if (i.wrongsDue > 0) {
            c += CoachSuggestion("WRONGS", "Kişisel yanlış defteri", null, null, null, (i.wrongsDue * 3).coerceIn(10, 30), 1,
                "${i.wrongsDue} yanlış kaydının tekrar zamanı geldi.", 85.0 + min(i.wrongsDue, 10))
        }
        val eligible = i.topics.filter { it.id !in i.rejectedTopics }
        eligible.filter { it.id in i.priorityTopics }.forEach { t ->
            val s = sourceFor(i, t)
            c += CoachSuggestion("STUDY", "${t.subjectName} / ${t.name}", t.subjectId, t.id, s?.id, 40, 1, "Bu konuyu öncelikli olarak seçtin." + sourcePart(s), 82.0)
        }
        eligible.filter { it.id in i.hardTopics }.forEach { t ->
            val s = sourceFor(i, t)
            c += CoachSuggestion("STUDY", "${t.subjectName} / ${t.name}", t.subjectId, t.id, s?.id, 35, 1, "Son denemende bu konuda zorlandığını işaretledin." + sourcePart(s), 80.0)
        }
        eligible.filter { it.reviewDue }.forEach { t ->
            val days = t.lastStudiedAt?.let { ((i.now - it) / DAY).toInt() }
            val s = sourceFor(i, t)
            c += CoachSuggestion("REVIEW", "${t.subjectName} / ${t.name}", t.subjectId, t.id, s?.id, 25, 1,
                (if (days != null && days > 0) "Bu konuya en son $days gün önce çalıştın, tekrar zamanı geldi." else "Bu konunun tekrar zamanı geldi.") + sourcePart(s), 75.0 + min(days ?: 0, 10))
        }
        eligible.filter { it.solved >= Mastery.MIN_SOLVED_FOR_RATE && Scoring.percent(it.correct, it.solved) < Mastery.LOW_RATE && !it.reviewDue }.forEach { t ->
            val pct = Scoring.percent(t.correct, t.solved)
            val s = sourceFor(i, t)
            c += CoachSuggestion("STUDY", "${t.subjectName} / ${t.name}", t.subjectId, t.id, s?.id, 35, 1,
                "Kayıtlı ${t.solved} soruda doğru oranın %$pct. Bu konuya biraz daha zaman ayırmak iyi gelebilir." + sourcePart(s), 70.0 + (Mastery.LOW_RATE - pct) / 4.0)
        }
        // Net düşüşü yalnızca yeterli deneme varsa söylenir.
        i.sections.filter { it.nets.size >= MIN_EXAMS_FOR_TREND }.forEach { sec ->
            val last = sec.nets.takeLast(MIN_EXAMS_FOR_TREND)
            if (last[0] > last[1] && last[1] > last[2]) {
                val subject = sec.subjectIds.firstOrNull()
                val t = eligible.filter { it.subjectId in sec.subjectIds && it.level in 1..2 }.minByOrNull { it.lastStudiedAt ?: 0L }
                    ?: eligible.firstOrNull { it.subjectId in sec.subjectIds && it.level == 0 }
                c += CoachSuggestion("STUDY", t?.let { "${it.subjectName} / ${it.name}" } ?: sec.section, t?.subjectId ?: subject, t?.id, t?.let { sourceFor(i, it)?.id }, 35, 2,
                    "Son ${MIN_EXAMS_FOR_TREND} denemede ${sec.section} netin düşüyor (${Scoring.formatNet(last[0])} → ${Scoring.formatNet(last[2])}).", 68.0)
            }
        }
        i.sources.filter { it.lastPage > 0 && (it.totalPages == 0 || it.lastPage < it.totalPages) }.take(1).forEach { s ->
            val t = s.topicId
            c += CoachSuggestion("SOURCE", s.name, s.subjectId, t, s.id, 30, 2, "En son ${s.lastPage}. sayfada kaldın.", 60.0)
        }
        if (i.cardsDue > 0) {
            c += CoachSuggestion("CARDS", "Bilgi kartları", null, null, null, (i.cardsDue / 2).coerceIn(5, 15), 3, "${i.cardsDue} kartın tekrar zamanı geldi.", 50.0)
        }
        // Yeterli kayıt yoksa ağırlıklı derslerden başlangıç önerisi yapılır ve bu açıkça belirtilir.
        val weightOf: (String) -> Double = { id ->
            (i.subjects[id] ?: 1.0) * (if (id in i.weakSubjects) 1.5 else 1.0) * (if (id in i.strongSubjects) 0.7 else 1.0)
        }
        val fresh = eligible.filter { it.level == 0 }.groupBy { it.subjectId }
            .entries.sortedByDescending { weightOf(it.key) }
        fresh.take(4).forEachIndexed { idx, (sid, list) ->
            val t = list.first()
            val s = sourceFor(i, t)
            val base = if (sid in i.weakSubjects) "Zorlandığını belirttiğin dersten başlamak iyi olabilir." else "Henüz başlamadığın bir konu."
            val note = if (i.daysWithData < 3) " Kayıtların arttıkça öneriler kişiselleşir." else ""
            c += CoachSuggestion("STUDY", "${t.subjectName} / ${t.name}", sid, t.id, s?.id, 35, if (sid in i.weakSubjects) 2 else 3, base + sourcePart(s) + note, 40.0 - idx * 3)
        }
        return pick(c, remaining)
    }

    /** Aynı konu tekrar edilmez, aynı dersten en çok iki öneri gelir, günlük süre aşılmaz. */
    private fun pick(all: List<CoachSuggestion>, budget: Int): List<CoachSuggestion> {
        val out = ArrayList<CoachSuggestion>()
        val topics = HashSet<String>()
        val perSubject = HashMap<String, Int>()
        val keys = HashSet<String>()
        var left = budget
        for (s in all.sortedByDescending { it.score }) {
            if (out.size >= MAX_ITEMS || left < 10) break
            if (s.topicId != null && !topics.add(s.topicId)) continue
            if (s.kind in setOf("WRONGS", "CARDS") && !keys.add(s.kind)) continue
            if (s.subjectId != null && (perSubject[s.subjectId] ?: 0) >= 2) continue
            val m = min(s.minutes, left)
            out += s.copy(minutes = m)
            left -= m
            if (s.subjectId != null) perSubject[s.subjectId] = (perSubject[s.subjectId] ?: 0) + 1
        }
        return out
    }
}

/* ---------- HAFTALIK DEĞERLENDİRME ---------- */

data class TopicWeek(val name: String, val subjectName: String, val solved: Int, val correct: Int, val minutes: Int)
data class WeekFacts(
    val topics: List<TopicWeek>,
    val subjectMinutes: Map<String, Int>,
    val subjectSolved: Map<String, Pair<Int, Int>>,
    val reviewTopics: List<String>,
    val openTasks: List<String>,
    val daysActive: Int,
)
data class WeekReview(
    val hasEnoughData: Boolean,
    val bestTopic: String?,
    val hardestSubject: String?,
    val mostStudied: String?,
    val reviewTopics: List<String>,
    val openTasks: List<String>,
    val nextWeek: List<String>,
)

object WeeklyCoach {
    const val MIN_ACTIVE_DAYS = 2

    fun build(f: WeekFacts): WeekReview {
        if (f.daysActive < MIN_ACTIVE_DAYS) return WeekReview(false, null, null, null, f.reviewTopics.take(5), f.openTasks.take(5), emptyList())
        val best = f.topics.filter { it.solved >= 10 }.maxByOrNull { Scoring.percent(it.correct, it.solved) }
            ?: f.topics.maxByOrNull { it.minutes }?.takeIf { it.minutes > 0 }
        val hard = f.subjectSolved.filter { it.value.first >= 10 }.minByOrNull { Scoring.percent(it.value.second, it.value.first) }
        val most = f.subjectMinutes.filter { it.value > 0 }.maxByOrNull { it.value }
        val next = ArrayList<String>()
        hard?.let { next += "${it.key} dersine haftada bir blok daha ayırabilirsin." }
        if (f.reviewTopics.isNotEmpty()) next += "${f.reviewTopics.first()} konusunu haftanın başında tekrar edebilirsin."
        if (f.openTasks.isNotEmpty()) next += "Tamamlanmayan ${f.openTasks.size} görevi yeniden planlayabilirsin."
        return WeekReview(true, best?.let { "${it.subjectName} / ${it.name}" }, hard?.key, most?.key, f.reviewTopics.take(5), f.openTasks.take(5), next)
    }
}

/* ---------- DENEME ANALİZİ ---------- */

data class SectionResultRow(val examId: String, val at: Long, val section: String, val correct: Int, val wrong: Int, val blank: Int, val net: Double)
data class SectionAnalysis(
    val section: String, val lastNet: Double, val avgNet: Double, val best: Double, val accuracy: Int, val blankRate: Int,
    val trend: Double?, val count: Int,
)
data class ExamAnalysis(
    val examCount: Int, val sections: List<SectionAnalysis>, val strong: List<String>, val weak: List<String>, val trendNote: String?,
    val avgMinutes: Int?, val blankAverage: Double,
)

object ExamAnalyzer {
    fun analyze(rows: List<SectionResultRow>, totalsByExam: List<Pair<Long, Double>>, minutes: List<Int>): ExamAnalysis {
        val exams = rows.map { it.examId }.toSet().size
        val sections = rows.groupBy { it.section }.map { (name, list) ->
            val ordered = list.sortedBy { it.at }
            val nets = ordered.map { it.net }
            val answered = ordered.sumOf { it.correct + it.wrong }
            val all = ordered.sumOf { it.correct + it.wrong + it.blank }
            SectionAnalysis(
                name, nets.last(), nets.average(), nets.max(), Scoring.percent(ordered.sumOf { it.correct }, answered), Scoring.percent(ordered.sumOf { it.blank }, all),
                if (nets.size >= PusulaKoc.MIN_EXAMS_FOR_TREND) slope(nets) else null, nets.size,
            )
        }.sortedByDescending { it.lastNet }
        val rated = sections.filter { it.count >= 2 }
        val strong = rated.sortedByDescending { it.accuracy }.take(2).filter { it.accuracy >= 70 }.map { it.section }
        val weak = rated.sortedBy { it.accuracy }.take(2).filter { it.accuracy < 60 && it.section !in strong }.map { it.section }
        val totals = totalsByExam.sortedBy { it.first }.map { it.second }
        val note = if (totals.size < PusulaKoc.MIN_EXAMS_FOR_TREND) null else {
            val s = slope(totals)
            when {
                s > 0.5 -> "Son denemelerinde toplam netin yükseliyor."
                s < -0.5 -> "Son denemelerinde toplam netin biraz düşmüş. Bunun nedeni deneme zorluğu da olabilir."
                else -> "Son denemelerinde toplam netin yatay ilerliyor."
            }
        }
        val blank = if (exams == 0) 0.0 else rows.sumOf { it.blank }.toDouble() / exams
        return ExamAnalysis(exams, sections, strong, weak, note, minutes.filter { it > 0 }.takeIf { it.isNotEmpty() }?.average()?.roundToInt(), blank)
    }

    /** En küçük kareler eğimi: deneme başına net değişimi. */
    fun slope(v: List<Double>): Double {
        if (v.size < 2) return 0.0
        val n = v.size
        val mx = (n - 1) / 2.0
        val my = v.average()
        var num = 0.0
        var den = 0.0
        v.forEachIndexed { i, y -> num += (i - mx) * (y - my); den += (i - mx) * (i - mx) }
        return if (den == 0.0) 0.0 else num / den
    }
}

/* ---------- MOTİVASYON ---------- */

object DailyQuote {
    /** 100 günlük döngüde her söz bir kez gelir; her döngünün sırası farklıdır ve cihazdan bağımsızdır. */
    fun indexFor(epochDay: Long, size: Int): Int {
        require(size > 0)
        val cycle = Math.floorDiv(epochDay, size.toLong())
        val pos = Math.floorMod(epochDay, size.toLong()).toInt()
        return order(cycle, size)[pos]
    }

    private fun raw(cycle: Long, size: Int): List<Int> = (0 until size).toList().shuffled(java.util.Random(cycle * 7_919L + 104_729L))

    fun order(cycle: Long, size: Int): List<Int> {
        val cur = raw(cycle, size)
        // Döngü geçişinde aynı söz art arda gelmesin.
        if (size < 3 || cur.first() != raw(cycle - 1, size).last()) return cur
        return listOf(cur[1], cur[0]) + cur.drop(2)
    }
}
