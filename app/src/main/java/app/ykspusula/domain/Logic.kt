package app.ykspusula.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Net ve başarı hesapları. Android'e bağımlı değildir, birim testleriyle doğrulanır. */
object Scoring {
    fun net(correct: Int, wrong: Int, divisor: Double = 4.0): Double {
        require(correct >= 0 && wrong >= 0) { "Sayılar negatif olamaz" }
        require(divisor > 0.0) { "Bölen sıfırdan büyük olmalı" }
        return correct - wrong / divisor
    }

    fun percent(part: Int, total: Int): Int = if (total <= 0) 0 else ((part * 100.0) / total).roundToInt()

    /** Doğru + yanlış + boş toplamının test soru sayısını aşmaması gerekir. */
    fun validSection(correct: Int, wrong: Int, blank: Int, max: Int): Boolean =
        correct >= 0 && wrong >= 0 && blank >= 0 && correct + wrong + blank <= max

    fun formatNet(v: Double): String = String.format(java.util.Locale.US, "%.2f", v).replace('.', ',')

    /** OBP = diploma notu × 5. Yerleştirme puanına katkısı OBP × katsayı. */
    fun obp(diploma: Double): Double {
        require(diploma in 50.0..100.0) { "Diploma notu 50-100 aralığında olmalı" }
        return diploma * 5
    }

    fun obpContribution(diploma: Double, placedLastYear: Boolean): Double =
        obp(diploma) * (if (placedLastYear) 0.06 else 0.12)
}

/** 1-3-7-14-30 günlük aralıklı tekrar. */
object Repetition {
    val INTERVALS = listOf(1, 3, 7, 14, 30)
    const val DAY_MS = 86_400_000L

    fun nextAt(stage: Int, from: Long): Long = from + INTERVALS[stage.coerceIn(0, INTERVALS.lastIndex)] * DAY_MS

    data class Outcome(val stage: Int, val nextReviewAt: Long, val resolved: Boolean)

    /** Yanlış ilk kaydedildiğinde 1 gün sonraya planlanır. */
    fun onWrong(now: Long): Outcome = Outcome(0, nextAt(0, now), false)

    /** Tekrar sırasında doğru: bir sonraki aralığa geçer; 30 günlük aşama da doğruysa soru çözülmüş sayılır. */
    fun onCorrect(stage: Int, now: Long): Outcome {
        val next = stage + 1
        return if (next > INTERVALS.lastIndex) Outcome(next, now + INTERVALS.last() * DAY_MS, true)
        else Outcome(next, nextAt(next, now), false)
    }

    /** Kart değerlendirme: 0 Bilmiyorum, 1 Zorlandım, 2 Biliyorum. */
    fun cardOutcome(stage: Int, rating: Int, now: Long): Pair<Int, Long> {
        val next = when (rating) {
            0 -> 0
            1 -> max(0, stage)
            else -> min(stage + 1, INTERVALS.lastIndex)
        }
        val at = if (rating == 0) now + 10 * 60_000L else nextAt(next, now)
        return next to at
    }
}

/** Türkçe karakterlere ve yazım farklarına toleranslı arama. */
object SearchText {
    fun normalize(s: String): String {
        val b = StringBuilder(s.length)
        for (c in s) {
            b.append(
                when (c) {
                    'ı', 'I', 'İ', 'i' -> 'i'
                    'ş', 'Ş' -> 's'
                    'ğ', 'Ğ' -> 'g'
                    'ü', 'Ü' -> 'u'
                    'ö', 'Ö' -> 'o'
                    'ç', 'Ç' -> 'c'
                    'â', 'Â' -> 'a'
                    'î', 'Î' -> 'i'
                    'û', 'Û' -> 'u'
                    else -> if (c.isLetterOrDigit()) c.lowercaseChar() else ' '
                }
            )
        }
        return b.toString().split(' ').filter { it.isNotEmpty() }.joinToString(" ")
    }

    fun distance(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        var prev = IntArray(b.length + 1) { it }
        var cur = IntArray(b.length + 1)
        for (i in a.indices) {
            cur[0] = i + 1
            for (j in b.indices) {
                cur[j + 1] = minOf(cur[j] + 1, prev[j + 1] + 1, prev[j] + if (a[i] == b[j]) 0 else 1)
            }
            val t = prev; prev = cur; cur = t
        }
        return prev[b.length]
    }

    /** 0 eşleşme yok; yüksek puan daha iyi. */
    fun score(query: String, title: String, body: String = ""): Int {
        if (query.isBlank()) return 0
        var total = 0
        val titleWords = title.split(' ')
        val bodyWords = if (query.length >= 3) body.split(' ') else emptyList()
        for (token in query.split(' ')) {
            val s = when {
                title == token -> 100
                title.startsWith(token) -> 80
                titleWords.any { it.startsWith(token) } -> 60
                title.contains(token) -> 45
                body.contains(token) -> 25
                token.length >= 4 && fuzzyHit(token, titleWords) -> 30
                token.length >= 5 && fuzzyHit(token, bodyWords) -> 12
                else -> 0
            }
            if (s == 0) return 0
            total += s
        }
        return total
    }

    private fun fuzzyHit(token: String, words: List<String>): Boolean {
        val tolerance = if (token.length >= 7) 2 else 1
        return words.any { w ->
            val head = if (w.length > token.length) w.substring(0, token.length) else w
            abs(head.length - token.length) <= tolerance && distance(token, head) <= tolerance
        }
    }
}

object Streaks {
    /** Bugün henüz çalışılmadıysa dünkü seri kırılmış sayılmaz. */
    fun current(days: Collection<String>, today: LocalDate): Int {
        val set = days.toSet()
        var d = if (today.toString() in set) today else today.minusDays(1)
        var n = 0
        while (d.toString() in set) { n++; d = d.minusDays(1) }
        return n
    }

    fun longest(days: Collection<String>): Int {
        val sorted = days.toSet().map(LocalDate::parse).sorted()
        var best = 0
        var run = 0
        var prev: LocalDate? = null
        for (d in sorted) {
            run = if (prev != null && ChronoUnit.DAYS.between(prev, d) == 1L) run + 1 else 1
            best = max(best, run)
            prev = d
        }
        return best
    }
}

/** Zaman damgasına dayalı sayaç: uygulama kapansa da doğru kalan süreyi verir. */
object Timers {
    fun remainingSec(endsAt: Long, now: Long): Int = max(0L, (endsAt - now + 999) / 1000).toInt()

    fun clock(sec: Int): String {
        val s = max(0, sec)
        val h = s / 3600
        val m = (s % 3600) / 60
        val r = s % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, r) else "%02d:%02d".format(m, r)
    }
}

/* ---------- PLAN ÜRETİCİ ---------- */

data class PlanSubject(val id: String, val name: String, val weight: Double)
data class PlanTopic(
    val id: String, val subjectId: String, val name: String, val status: Int, val success: Int?, val minutes: Int, val reviewDue: Boolean,
)
data class PlanInput(
    val start: LocalDate,
    val examDate: LocalDate,
    val dailyMinutes: Int,
    val studyDays: Set<Int>,
    val startHour: Int,
    val subjects: List<PlanSubject>,
    val topics: List<PlanTopic>,
    val weakSubjectIds: Set<String>,
    val strongSubjectIds: Set<String>,
    val wrongsDue: Int,
)
data class PlannedTask(
    val date: LocalDate, val time: String, val subjectId: String?, val topicId: String?, val title: String,
    val kind: String, val minutes: Int, val questionGoal: Int,
)

object PlanGenerator {
    const val BLOCK = 50
    const val MAX_BLOCKS_PER_DAY = 7

    /** Zayıf ders ağırlığı artar, güçlü dersler azaltılır ama hiçbir zaman sıfırlanmaz. */
    fun subjectWeights(input: PlanInput): Map<String, Double> {
        val bySubject = input.topics.groupBy { it.subjectId }
        return input.subjects.associate { s ->
            val topics = bySubject[s.id].orEmpty()
            val scored = topics.mapNotNull { it.success }
            val avg = if (scored.isEmpty()) 60.0 else scored.average()
            var w = s.weight * (1.0 + (100.0 - avg) / 100.0)
            val open = topics.count { it.status < 3 }
            if (topics.isNotEmpty()) w *= 0.6 + 0.4 * open.toDouble() / topics.size
            if (s.id in input.weakSubjectIds) w *= 1.5
            if (s.id in input.strongSubjectIds) w *= 0.7
            s.id to max(w, 0.2)
        }
    }

    fun generate(input: PlanInput, days: Int = 7): List<PlannedTask> {
        val daysLeft = ChronoUnit.DAYS.between(input.start, input.examDate)
        val out = ArrayList<PlannedTask>()
        if (daysLeft <= 0 || input.subjects.isEmpty()) return out
        val weights = subjectWeights(input)
        val pool = input.topics.groupBy { it.subjectId }.mapValues { (_, list) ->
            ArrayDeque(list.sortedWith(compareBy<PlanTopic>({ !it.reviewDue }, { it.status >= 3 }, { it.success ?: 101 }, { it.status })))
        }
        val credit = HashMap<String, Double>()
        var wrongBudget = input.wrongsDue
        var lastSubject: String? = null
        for (offset in 0 until min(days.toLong(), daysLeft).toInt()) {
            val date = input.start.plusDays(offset.toLong())
            if (date.dayOfWeek.value !in input.studyDays) continue
            val blocks = (input.dailyMinutes / BLOCK).coerceIn(1, MAX_BLOCKS_PER_DAY)
            var minute = input.startHour * 60
            val review = wrongBudget > 0 && blocks >= 2
            val studyBlocks = if (review) blocks - 1 else blocks
            repeat(studyBlocks) { i ->
                val total = weights.values.sum()
                weights.forEach { (k, v) -> credit[k] = (credit[k] ?: 0.0) + v / total }
                val pick = credit.entries.filter { it.key != lastSubject || weights.size == 1 }.maxBy { it.value }.key
                credit[pick] = (credit[pick] ?: 0.0) - 1.0
                lastSubject = pick
                val subject = input.subjects.first { it.id == pick }
                val queue = pool[pick]
                val topic = queue?.removeFirstOrNull()
                if (topic != null) queue.addLast(topic)
                val goal = if (topic != null && topic.status >= 2) 20 else 10
                out += PlannedTask(
                    date, clock(minute), pick, topic?.id,
                    if (topic != null) "${subject.name} / ${topic.name}" else subject.name,
                    if (topic?.reviewDue == true) "REVIEW" else "STUDY", BLOCK, goal,
                ).also { minute += BLOCK + breakAfter(i) }
            }
            if (review) {
                val goal = min(wrongBudget, 20)
                wrongBudget -= goal
                out += PlannedTask(date, clock(minute), null, null, "Yanlışların tekrarı", "WRONGS", BLOCK, goal)
            }
        }
        return out
    }

    private fun breakAfter(i: Int) = if ((i + 1) % 3 == 0) 40 else 10

    private fun clock(m: Int) = "%02d:%02d".format((m / 60) % 24, m % 60)
}

/* ---------- ÖNERİ MOTORU (kural tabanlı, gerçek veriden) ---------- */

data class NetPoint(val exam: String, val at: Long, val net: Double)
data class SectionPoint(val section: String, val at: Long, val net: Double)
data class TopicScore(val name: String, val total: Int, val right: Int)
data class Advice(val text: String, val kind: String)

object Advisor {
    fun build(
        mocks: List<SectionPoint>,
        topics: List<TopicScore>,
        wrongsDue: Int,
        cardsDue: Int,
        daysInactive: Int?,
        weeklyMinutes: Int,
        lastWeekMinutes: Int,
    ): List<Advice> {
        val out = ArrayList<Advice>()
        mocks.groupBy { it.section }.forEach { (section, points) ->
            val last = points.sortedBy { it.at }.takeLast(3).map { it.net }
            if (last.size == 3 && last[0] > last[1] && last[1] > last[2]) {
                out += Advice("Son üç denemede $section netin düşmüş. Bu hafta bu derse biraz daha fazla zaman ayırabilirsin.", "NET_DROP")
            } else if (last.size == 3 && last[0] < last[1] && last[1] < last[2]) {
                out += Advice("Son üç denemede $section netin yükseliyor. Bu tempoyu koruyabilirsin.", "NET_RISE")
            }
        }
        topics.filter { it.total >= 5 && Scoring.percent(it.right, it.total) < 50 }
            .sortedBy { it.right.toDouble() / it.total }.take(2).forEach {
                out += Advice("${it.name} konusunda başarı oranın %${Scoring.percent(it.right, it.total)}. Bugün 20 soru ve kısa bir konu tekrarı öneriliyor.", "WEAK_TOPIC")
            }
        if (wrongsDue > 0) out += Advice("Tekrar zamanı gelen $wrongsDue yanlışın var.", "WRONGS")
        if (cardsDue > 0) out += Advice("Tekrar zamanı gelen $cardsDue kartın var.", "CARDS")
        if (lastWeekMinutes > 0 && weeklyMinutes >= lastWeekMinutes * 1.2) {
            out += Advice("Bu hafta geçen haftadan daha fazla çalıştın: $weeklyMinutes dakika.", "RISE")
        }
        if (daysInactive != null && daysInactive >= 3) {
            out += Advice("Birkaç gündür kayıt görünmüyor. Kısa bir tekrarla yeniden başlayabilirsin.", "RESTART")
        }
        return out
    }
}

/* ---------- DENEME OLUŞTURMA ---------- */

data class PartPlan(val subjectId: String, val count: Int)
data class SectionPlan(val name: String, val parts: List<PartPlan>)
data class PickedSection(val name: String, val requested: Int, val questionIds: List<String>)

object ExamBuilder {
    /** Görülmemiş soruları öne alır; aynı soru bir denemede iki kez yer almaz. */
    fun pick(
        sections: List<SectionPlan>,
        available: Map<String, List<String>>,
        seen: Set<String>,
        seed: Long,
    ): List<PickedSection> {
        val rnd = java.util.Random(seed)
        val used = HashSet<String>()
        return sections.map { sec ->
            val picked = ArrayList<String>()
            sec.parts.forEach { part ->
                val pool = available[part.subjectId].orEmpty().filter { it !in used }.shuffled(rnd)
                val ordered = pool.filter { it !in seen } + pool.filter { it in seen }
                val take = ordered.take(part.count)
                picked += take
                used += take
            }
            PickedSection(sec.name, sec.parts.sumOf { it.count }, picked)
        }
    }

    /** Eksik soruda süre orantılı kısalır; en az 1 dakika. */
    fun scaledDuration(fullMinutes: Int, requested: Int, actual: Int): Int =
        if (requested <= 0 || actual >= requested) fullMinutes
        else max(1, (fullMinutes * actual.toDouble() / requested).roundToInt())
}

/* ---------- BAŞARI ROZETLERİ ---------- */

data class HabitStats(
    val questions: Int, val minutes: Int, val mocks: Int, val completedTopics: Int, val longestStreak: Int, val doneTasks: Int, val learnedCards: Int,
)
data class Badge(val title: String, val detail: String, val current: Int, val target: Int) {
    val earned: Boolean get() = current >= target
}

object Badges {
    fun all(s: HabitStats): List<Badge> = listOf(
        Badge("İlk Adım", "İlk 10 soruyu çöz", s.questions, 10),
        Badge("Soru Avcısı", "500 soru çöz", s.questions, 500),
        Badge("Bin Soru", "1.000 soru çöz", s.questions, 1000),
        Badge("Odak", "Toplam 10 saat çalış", s.minutes, 600),
        Badge("Maratoncu", "Toplam 100 saat çalış", s.minutes, 6000),
        Badge("Deneme Yolcusu", "5 deneme kaydet", s.mocks, 5),
        Badge("Konu Hâkimi", "20 konuyu tamamla", s.completedTopics, 20),
        Badge("Düzenli", "7 günlük seri yap", s.longestStreak, 7),
        Badge("Kararlı", "30 günlük seri yap", s.longestStreak, 30),
        Badge("Planlı", "50 görev tamamla", s.doneTasks, 50),
        Badge("Kelime Ustası", "100 kart öğren", s.learnedCards, 100),
    )
}

object ReminderTime {
    /** Bir sonraki bildirim zamanı; dayOfWeek null ise her gün, doluysa haftalık (1 Pazartesi ... 7 Pazar). */
    fun next(now: Long, hour: Int, minute: Int, dayOfWeek: Int?, zone: java.time.ZoneId = java.time.ZoneId.systemDefault()): Long {
        val base = java.time.Instant.ofEpochMilli(now).atZone(zone)
        var t = base.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (dayOfWeek != null) {
            while (t.dayOfWeek.value != dayOfWeek || !t.isAfter(base)) t = t.plusDays(1)
        } else if (!t.isAfter(base)) t = t.plusDays(1)
        return t.toInstant().toEpochMilli()
    }
}

/* ---------- FORMÜL ÇÖZÜMLEYİCİ ---------- */

sealed interface MathNode {
    data class Plain(val text: String) : MathNode
    data class Bold(val text: String) : MathNode
    data class Sup(val body: List<MathNode>) : MathNode
    data class Sub(val body: List<MathNode>) : MathNode
    data class Frac(val top: List<MathNode>, val bottom: List<MathNode>) : MathNode
    data class Root(val body: List<MathNode>) : MathNode
}

object MathParser {
    fun parse(s: String): List<MathNode> {
        val out = ArrayList<MathNode>()
        val plain = StringBuilder()
        fun flush() { if (plain.isNotEmpty()) { out += MathNode.Plain(plain.toString()); plain.clear() } }
        var i = 0
        while (i < s.length) {
            when {
                s.startsWith("\\frac{", i) -> {
                    val a = group(s, i + 5)
                    if (a != null && a.second < s.length && s[a.second] == '{') {
                        val b = group(s, a.second)
                        if (b != null) { flush(); out += MathNode.Frac(parse(a.first), parse(b.first)); i = b.second; continue }
                    }
                    plain.append(s[i]); i++
                }
                s.startsWith("\\sqrt{", i) -> {
                    val a = group(s, i + 5)
                    if (a != null) { flush(); out += MathNode.Root(parse(a.first)); i = a.second } else { plain.append(s[i]); i++ }
                }
                s.startsWith("^{", i) || s.startsWith("_{", i) -> {
                    val a = group(s, i + 1)
                    if (a != null) {
                        flush()
                        out += if (s[i] == '^') MathNode.Sup(parse(a.first)) else MathNode.Sub(parse(a.first))
                        i = a.second
                    } else { plain.append(s[i]); i++ }
                }
                s.startsWith("**", i) -> {
                    val e = s.indexOf("**", i + 2)
                    if (e > 0) { flush(); out += MathNode.Bold(s.substring(i + 2, e)); i = e + 2 } else { plain.append(s[i]); i++ }
                }
                else -> { plain.append(s[i]); i++ }
            }
        }
        flush()
        return out
    }

    /** s[start] '{' olmalı; içerik ve kapanıştan sonraki indeksi döndürür. */
    private fun group(s: String, start: Int): Pair<String, Int>? {
        if (start >= s.length || s[start] != '{') return null
        var depth = 0
        for (i in start until s.length) {
            if (s[i] == '{') depth++
            if (s[i] == '}') { depth--; if (depth == 0) return s.substring(start + 1, i) to i + 1 }
        }
        return null
    }

    fun plainOf(n: List<MathNode>): String = n.joinToString("") {
        when (it) {
            is MathNode.Plain -> it.text
            is MathNode.Bold -> it.text
            is MathNode.Sup -> "^" + plainOf(it.body)
            is MathNode.Sub -> plainOf(it.body)
            is MathNode.Frac -> "(" + plainOf(it.top) + ")/(" + plainOf(it.bottom) + ")"
            is MathNode.Root -> "√(" + plainOf(it.body) + ")"
        }
    }

    fun spoken(n: List<MathNode>): String = n.joinToString("") {
        when (it) {
            is MathNode.Plain -> it.text
            is MathNode.Bold -> it.text
            is MathNode.Sup -> " üssü " + spoken(it.body) + " "
            is MathNode.Sub -> " alt " + spoken(it.body) + " "
            is MathNode.Frac -> " " + spoken(it.top) + " bölü " + spoken(it.bottom) + " "
            is MathNode.Root -> " karekök " + spoken(it.body) + " "
        }
    }

    fun widthUnits(n: List<MathNode>): Float = plainOf(n).length.toFloat()
}
