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

    /** OBP = diploma notu × çarpan. Parametreler yıla göre sınav ayarlarından gelir. */
    fun obp(diploma: Double, multiplier: Double = 5.0): Double {
        require(diploma in 50.0..100.0) { "Diploma notu 50-100 aralığında olmalı" }
        return diploma * multiplier
    }

    fun obpContribution(diploma: Double, placedLastYear: Boolean, multiplier: Double = 5.0, coefficient: Double = 0.12, coefficientPlaced: Double = 0.06): Double =
        obp(diploma, multiplier) * (if (placedLastYear) coefficientPlaced else coefficient)

    /** Hedef nete ulaşmak için gereken en az doğru sayısı; yanlış sayısı sabit tutulur. */
    fun correctNeeded(targetNet: Double, wrong: Int, divisor: Double = 4.0): Int =
        kotlin.math.ceil(targetNet + wrong / divisor).toInt().coerceAtLeast(0)
}

/** 1-3-7-14-30 günlük aralıklı tekrar. */
object Repetition {
    val INTERVALS = listOf(1, 3, 7, 14, 30)
    const val DAY_MS = 86_400_000L

    fun nextAt(stage: Int, from: Long): Long = from + INTERVALS[stage.coerceIn(0, INTERVALS.lastIndex)] * DAY_MS

    data class Outcome(val stage: Int, val nextReviewAt: Long, val resolved: Boolean)

    /** Yanlış ilk kaydedildiğinde 1 gün sonraya planlanır. */
    fun onWrong(now: Long): Outcome = Outcome(0, nextAt(0, now), false)

    /** Hatırladım: bir sonraki aralığa geçer; 30 günlük aşama da geçilirse kayıt tamamlanır. */
    fun onCorrect(stage: Int, now: Long): Outcome {
        val next = stage + 1
        return if (next > INTERVALS.lastIndex) Outcome(INTERVALS.lastIndex, now + INTERVALS.last() * DAY_MS, true)
        else Outcome(next, nextAt(next, now), false)
    }

    /** Sonuç: 0 hatırlayamadım (baştan), 1 kısmen (aynı aralık), 2 hatırladım (sonraki aralık). */
    fun onReview(stage: Int, result: Int, now: Long): Outcome = when (result) {
        2 -> onCorrect(stage, now)
        1 -> Outcome(stage.coerceIn(0, INTERVALS.lastIndex), nextAt(stage, now), false)
        else -> Outcome(0, nextAt(0, now), false)
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

    /** Yer değiştirmiş iki harfi tek hata sayan düzenleme uzaklığı. */
    fun distance(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        val d = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) d[i][0] = i
        for (j in 0..b.length) d[0][j] = j
        for (i in 1..a.length) for (j in 1..b.length) {
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            var v = minOf(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + cost)
            if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) v = min(v, d[i - 2][j - 2] + 1)
            d[i][j] = v
        }
        return d[a.length][b.length]
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
    /** Bugün henüz çalışılmadıysa seri bozulmaz. Planlı dinlenme günleri seriyi kesmez, sayıya da eklenmez. */
    fun current(days: Collection<String>, today: LocalDate, rest: Collection<String> = emptyList()): Int {
        val set = days.toSet()
        val off = rest.toSet()
        var d = if (today.toString() in set) today else today.minusDays(1)
        var n = 0
        repeat(4000) {
            val key = d.toString()
            if (key in set) n++ else if (key !in off) return n
            d = d.minusDays(1)
        }
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
        return if (h > 0) "%d:%02d:%02d".format(java.util.Locale.US, h, m, r) else "%02d:%02d".format(java.util.Locale.US, m, r)
    }
}

/* ---------- PLAN ÜRETİCİ ---------- */

/** gap: hedef net eksi son net; pozitifse ders hedefin altındadır. */
data class PlanSubject(val id: String, val name: String, val weight: Double, val gap: Double? = null, val lastNet: Double? = null, val targetNet: Double? = null)
data class PlanTopic(
    val id: String, val subjectId: String, val name: String, val status: Int, val success: Int?, val minutes: Int, val reviewDue: Boolean,
    /** Son denemelerde bu konuda işaretlenen yanlış ve boş sayısı. */
    val mockMisses: Int = 0,
    /** Konunun son çalışıldığı günden bu yana geçen gün; hiç çalışılmadıysa null. */
    val daysSinceStudy: Int? = null,
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
    val maxDailyMinutes: Int = 720,
    val breakMinutes: Int = 10,
    val endHour: Int = 23,
    val priorityTopics: Set<String> = emptySet(),
    val blockMinutes: Int = PlanGenerator.BLOCK,
    /** Haftalık çalışma hedefi; 0 ise yalnızca günlük hedef uygulanır. */
    val weeklyMinutes: Int = 0,
    /** Öğrencinin işaretlediği dinlenme günleri. */
    val restDates: Set<LocalDate> = emptySet(),
    /** Korunan görevlerin (tamamlanmış veya elle eklenmiş) gün bazında süresi; hedeften düşülür. */
    val keptMinutes: Map<LocalDate, Int> = emptyMap(),
)
/** minutes yalnızca çalışma süresidir; mola ayrı tutulur. */
data class PlannedTask(
    val date: LocalDate, val time: String, val subjectId: String?, val topicId: String?, val title: String,
    val kind: String, val minutes: Int, val questionGoal: Int, val reason: String = "", val priority: Int = 2, val breakAfter: Int = 0,
)
data class PlanDay(val date: LocalDate, val targetMinutes: Int, val plannedMinutes: Int, val breakMinutes: Int, val note: String = "")
data class PlanResult(val tasks: List<PlannedTask>, val days: List<PlanDay>, val notes: List<String>) {
    val studyMinutes: Int get() = tasks.sumOf { it.minutes }
}

object PlanGenerator {
    const val BLOCK = 50
    const val MIN_BLOCK = 15
    /** Sınava bu kadar gün kala yeni konu yerine tekrar öne alınır. */
    const val FINAL_DAYS = 21

    /** Zayıf ders ve hedefin altındaki ders ağırlığı artar, güçlü dersler azaltılır ama hiçbir zaman sıfırlanmaz. */
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
            s.gap?.let { g -> if (g > 0) w *= 1.0 + min(g / 10.0, 1.0) }
            if (topics.any { it.mockMisses > 0 }) w *= 1.2
            s.id to max(w, 0.2)
        }
    }

    /**
     * Hedef dakika bloklara bölünür; toplam her zaman hedefe eşittir.
     * Son blok kalan dakikadır; 15 dakikadan kısa artık önceki bloğa eklenir.
     */
    fun split(target: Int, block: Int): List<Int> {
        if (target <= 0) return emptyList()
        val b = block.coerceIn(MIN_BLOCK, 120)
        val out = MutableList(target / b) { b }
        val rest = target % b
        if (rest > 0) { if (rest < MIN_BLOCK && out.isNotEmpty()) out[out.lastIndex] += rest else out += rest }
        return out
    }

    /** Üç bloktan sonra uzun mola verilir. Molalar çalışma süresine eklenmez. */
    fun breakAfter(i: Int, base: Int) = if ((i + 1) % 3 == 0) min(base * 3, 30) else base

    private fun topicRank(t: PlanTopic, input: PlanInput, final: Boolean): Int = when {
        t.id in input.priorityTopics -> 0
        t.mockMisses > 0 -> 1
        t.reviewDue -> 2
        t.success != null && t.success < 60 -> 3
        final && t.status == 0 -> 9
        t.status in 1..2 -> 4
        t.status == 0 -> 5
        else -> 6
    }

    private fun reasonFor(t: PlanTopic?, subject: PlanSubject, input: PlanInput, final: Boolean, daysLeft: Long): Pair<String, Int> {
        if (t == null) return "${subject.name} dersinin haftalık payı." to 3
        val gap = subject.gap?.takeIf { it > 0 }?.let { g ->
            " ${subject.name} dersinde son netin ${Scoring.formatNet(subject.lastNet ?: 0.0)}, hedefin ${Scoring.formatNet(subject.targetNet ?: 0.0)}; bu derse ayrılan süre artırıldı."
        }.orEmpty()
        val (text, prio) = when {
            t.id in input.priorityTopics -> "Bu konuyu öncelikli olarak seçtin." to 1
            t.mockMisses > 0 -> "Son denemelerde bu konudan ${t.mockMisses} yanlış veya boş var; tekrar öne alındı." to 1
            t.reviewDue && (t.daysSinceStudy ?: 0) >= 14 -> "Bu konuyu ${t.daysSinceStudy} gündür çalışmadın ve tekrar zamanı geldi." to 1
            t.reviewDue -> "Tekrar zamanı geldi." to 1
            t.success != null && t.success < 60 -> "Kayıtlarında bu konudaki doğru oranın %${t.success}." to 1
            final && t.status > 0 -> "Sınava $daysLeft gün kaldığı için yeni konu yerine bildiğin konuların tekrarı öne alındı." to 2
            subject.id in input.weakSubjectIds -> "Zorlandığını belirttiğin ders." to 2
            t.status == 0 -> "Henüz başlamadığın bir konu." to 2
            else -> "Konuyu güçlendirmek için." to 3
        }
        return (text + gap) to prio
    }

    fun generate(input: PlanInput, days: Int = 7): List<PlannedTask> = plan(input, days).tasks

    fun plan(input: PlanInput, days: Int = 7): PlanResult {
        val daysLeft = ChronoUnit.DAYS.between(input.start, input.examDate)
        val out = ArrayList<PlannedTask>()
        val dayInfo = ArrayList<PlanDay>()
        val notes = ArrayList<String>()
        if (daysLeft <= 0) return PlanResult(out, dayInfo, listOf("Sınav tarihi geçmiş veya bugün. Sınav ayarlarından yeni tarihi gir."))
        if (input.subjects.isEmpty()) return PlanResult(out, dayInfo, listOf("Plan için ders bulunamadı. Profilinde alanını kontrol et."))
        val final = daysLeft <= FINAL_DAYS
        if (final) notes += "Sınava $daysLeft gün kaldı: plan yeni konu yerine tekrar, eksik konu ve yanlışlara ağırlık veriyor."
        val weights = subjectWeights(input)
        val pool = input.topics.filter { it.status < 4 || it.reviewDue || it.mockMisses > 0 }.groupBy { it.subjectId }.mapValues { (_, list) ->
            list.sortedWith(compareBy<PlanTopic>({ topicRank(it, input, final) }, { it.success ?: 101 }, { -(it.daysSinceStudy ?: 0) }))
        }
        val used = HashMap<String, Int>()
        val credit = HashMap<String, Double>()
        var wrongBudget = input.wrongsDue
        var lastSubject: String? = null
        val priority = input.topics.filter { it.id in input.priorityTopics }.toMutableList()
        val daily = min(input.dailyMinutes, input.maxDailyMinutes).coerceAtLeast(0)
        if (input.dailyMinutes > input.maxDailyMinutes) notes += "Günlük hedefin ${input.dailyMinutes} dk, en fazla ${input.maxDailyMinutes} dk olarak belirlediğin sınır uygulandı."
        var weekLeft = if (input.weeklyMinutes > 0) input.weeklyMinutes else Int.MAX_VALUE
        val window = (input.endHour - input.startHour) * 60
        if (window <= 0) return PlanResult(out, dayInfo, listOf("Bitiş saatin başlangıç saatinden sonra olmalı. Profilden çalışma saatlerini düzelt."))

        for (offset in 0 until min(days.toLong(), daysLeft).toInt()) {
            val date = input.start.plusDays(offset.toLong())
            if (date.dayOfWeek.value !in input.studyDays || date in input.restDates) continue
            val kept = input.keptMinutes[date] ?: 0
            var target = (daily - kept).coerceAtLeast(0)
            if (target > weekLeft) target = weekLeft
            if (target <= 0) { dayInfo += PlanDay(date, 0, 0, 0, if (kept > 0) "Bu günün hedefi mevcut görevlerle dolu." else "Haftalık hedefe ulaşıldı."); continue }
            // Pencereye sığdır: bloklar ve aralarındaki molalar başlangıç-bitiş saatleri içinde kalır.
            val blocks = split(target, input.blockMinutes).toMutableList()
            var used0 = 0
            var fitted = 0
            val sized = ArrayList<Int>()
            for ((i, m) in blocks.withIndex()) {
                val room = window - used0
                if (room < MIN_BLOCK) break
                val take = min(m, room)
                if (take < MIN_BLOCK) break
                sized += take; fitted += take
                used0 += take + breakAfter(i, input.breakMinutes)
                if (take < m) break
            }
            var note = ""
            if (fitted < target) {
                note = "Hedefin $target dk; ${clock(input.startHour * 60)}-${clock(input.endHour * 60)} arasında molalarla en fazla $fitted dk sığıyor."
                notes += "${date}: $note"
            }
            weekLeft -= fitted
            var minute = input.startHour * 60
            val review = wrongBudget > 0 && sized.size >= 2
            val today = HashSet<String>()
            var breaks = 0
            sized.forEachIndexed { i, m ->
                val brk = if (i < sized.lastIndex) breakAfter(i, input.breakMinutes) else 0
                if (review && i == sized.lastIndex) {
                    val goal = min(wrongBudget, max(5, m / 3))
                    wrongBudget -= goal
                    out += PlannedTask(date, clock(minute), null, null, "Yanlışların tekrarı", "WRONGS", m, goal, "Tekrar zamanı gelmiş $goal yanlışın var.", 1, brk)
                } else {
                    val first = priority.removeFirstOrNull()
                    val pick = first?.subjectId ?: run {
                        val total = weights.values.sum()
                        weights.forEach { (k, v) -> credit[k] = (credit[k] ?: 0.0) + v / total }
                        val p = credit.entries.filter { it.key != lastSubject || weights.size == 1 }.maxBy { it.value }.key
                        credit[p] = (credit[p] ?: 0.0) - 1.0
                        p
                    }
                    lastSubject = pick
                    val subject = input.subjects.firstOrNull { it.id == pick } ?: input.subjects.first()
                    // Aynı gün aynı konu verilmez; hafta içinde en az kullanılan ve en öncelikli konu seçilir.
                    val topic = first ?: pool[pick].orEmpty().filter { it.id !in today }.minWithOrNull(
                        compareBy<PlanTopic>({ used[it.id] ?: 0 }, { topicRank(it, input, final) }, { it.success ?: 101 }),
                    )
                    topic?.let { today += it.id; used[it.id] = (used[it.id] ?: 0) + 1 }
                    val goal = if (topic != null && topic.status >= 1) max(10, m * 2 / 5) else max(5, m / 5)
                    val (reason, prio) = reasonFor(topic, subject, input, final, daysLeft)
                    out += PlannedTask(
                        date, clock(minute), pick, topic?.id,
                        if (topic != null) "${subject.name} / ${topic.name}" else subject.name,
                        if (topic?.reviewDue == true || topic?.mockMisses?.let { it > 0 } == true) "REVIEW" else "STUDY", m, goal, reason, prio, brk,
                    )
                }
                minute += m + brk
                breaks += brk
            }
            dayInfo += PlanDay(date, target, fitted, breaks, note)
        }
        if (input.weeklyMinutes > 0 && weekLeft == 0) notes += "Haftalık hedefin (${input.weeklyMinutes} dk) dolduğu için kalan günlere görev eklenmedi."
        return PlanResult(out, dayInfo, notes.distinct())
    }

    fun clock(m: Int) = "%02d:%02d".format(java.util.Locale.US, (m / 60) % 24, m % 60)
}

/* ---------- TARAFSIZ YAŞ EKRANI ---------- */

/**
 * Google Play hedef kitle politikası: kitle çocukları da içeriyorsa çocuklara uygun olmayan reklamlar yalnızca
 * tarafsız yaş ekranıyla belirlenen büyüklere gösterilir. Türkiye'de 18 yaş altı çocuk sayıldığı ve kullanılan
 * reklam SDK'sı Families listesinde bulunmadığı için reklam yalnızca 18 yaşını doldurduğu kesin olanlara gösterilir.
 * birthYear: 0 girilmedi, -1 belirtmek istemedi.
 */
object AgeGate {
    const val ADULT_AGE = 18
    const val NOT_SET = 0
    const val DECLINED = -1

    /** Ay bilgisi gün içermediği için doğum ayında yaş dolmamış sayılır (ihtiyatlı). */
    fun isVerifiedAdult(birthYear: Int, birthMonth: Int, today: LocalDate): Boolean {
        if (birthYear <= 0 || birthMonth !in 1..12) return false
        val age = today.year - birthYear - if (today.monthValue <= birthMonth) 1 else 0
        return age >= ADULT_AGE
    }

    /** Serbest giriş denetimi; varsayılan değer yoktur. */
    fun validate(month: String, year: String, today: LocalDate): Pair<Int, Int>? {
        val m = month.trim().toIntOrNull() ?: return null
        val y = year.trim().toIntOrNull() ?: return null
        if (m !in 1..12 || y < today.year - 100 || y > today.year) return null
        if (y == today.year && m > today.monthValue) return null
        return y to m
    }
}

/* ---------- EKSİK KONU ÖNCELİK SIRASI ---------- */

data class GapTopic(
    val id: String, val name: String, val subjectId: String, val subjectName: String,
    val status: Int, val success: Int?, val reviewDue: Boolean, val mockMisses: Int, val wrongOpen: Int, val daysSinceStudy: Int?,
    val subjectGap: Double? = null,
)
data class RankedTopic(val topic: GapTopic, val score: Double, val reasons: List<String>)

/**
 * "Önce hangi konu?" sorusunun cevabı. Puan yalnızca öğrencinin kendi kayıtlarından gelir:
 * deneme yanlışları, açık yanlışlar, tekrar tarihi, konudaki doğru oranı, hedef net farkı ve konu seviyesi.
 * Ağırlıklar başlangıç değeridir, bilimsel kesinlik iddia etmez.
 */
object TopicPriority {
    fun rank(list: List<GapTopic>, final: Boolean = false): List<RankedTopic> = list.mapNotNull { t ->
        var score = 0.0
        val why = ArrayList<String>()
        if (t.mockMisses > 0) { score += 3.0 * min(t.mockMisses, 5); why += "Son denemelerde ${t.mockMisses} yanlış veya boş" }
        if (t.wrongOpen > 0) { score += 2.0 * min(t.wrongOpen, 5); why += "Defterde ${t.wrongOpen} açık yanlış" }
        if (t.reviewDue) { score += 4.0; why += if ((t.daysSinceStudy ?: 0) >= 14) "Tekrar zamanı geçti (${t.daysSinceStudy} gündür çalışılmadı)" else "Tekrar zamanı geldi" }
        if (t.success != null && t.success < 60) { score += (60 - t.success) / 10.0 + 1; why += "Doğru oranın %${t.success}" }
        if (t.subjectGap != null && t.subjectGap > 0) { score += min(t.subjectGap, 10.0) / 2; why += "Ders hedef netinin ${Scoring.formatNet(t.subjectGap)} altında" }
        if (t.status == 3 && !t.reviewDue && t.mockMisses == 0 && t.wrongOpen == 0) score -= 2.0
        // Yalnızca "başlanmadı" olan konu listeye girmez; aksi hâlde liste bütün konuların alfabetik dökümüne döner.
        if (score <= 0 || why.isEmpty()) return@mapNotNull null
        if (t.status == 0 && !final) { score += 1.0; why += "Henüz başlanmadı" }
        RankedTopic(t, score, why)
    }.sortedWith(compareByDescending<RankedTopic> { it.score }.thenBy { it.topic.name })
}

/* ---------- DENEMEDEKİ KONU EKSİKLERİ ---------- */

/** Denemede bir konudan yapılan yanlış ve boş sayısı. Saklama biçimi: "konu:yanlış:boş" virgülle ayrılır; eski kayıtlarda yalnızca konu kimliği bulunur. */
data class TopicMiss(val topicId: String, val wrong: Int = 0, val blank: Int = 0)

object MockMisses {
    fun parse(text: String): List<TopicMiss> = text.split(',').map { it.trim() }.filter { it.isNotEmpty() }.mapNotNull { part ->
        val p = part.split(':')
        val id = p[0].trim()
        if (id.isEmpty()) null else TopicMiss(id, p.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 200) ?: 0, p.getOrNull(2)?.toIntOrNull()?.coerceIn(0, 200) ?: 0)
    }.distinctBy { it.topicId }

    fun format(list: List<TopicMiss>): String = list.distinctBy { it.topicId }.joinToString(",") { "${it.topicId}:${it.wrong}:${it.blank}" }

    fun ids(text: String): List<String> = parse(text).map { it.topicId }
}

/* ---------- KAÇIRILAN PROGRAMI YENİDEN DÜZENLEME ---------- */

data class SlotTask(val id: String, val date: LocalDate, val time: String, val minutes: Int, val priority: Int, val title: String)
data class ReplanInput(
    val today: LocalDate,
    val missed: List<SlotTask>,
    val upcoming: List<SlotTask>,
    val studyDays: Set<Int>,
    val dailyCapacity: Int,
    val startHour: Int,
    val endHour: Int,
    val breakMinutes: Int,
    val horizonDays: Int = 7,
)
data class ReplanMove(val id: String, val title: String, val from: LocalDate, val to: LocalDate, val time: String)
data class ReplanResult(val moves: List<ReplanMove>, val unplaced: List<SlotTask>)

object Replanner {
    /** ALL: tüm kaçırılanlar. IMPORTANT: yalnızca öncelik 1 ve 2 olan görevler. Günlük kapasite ve molalar aşılmaz. */
    fun preview(input: ReplanInput, importantOnly: Boolean): ReplanResult {
        val chosen = input.missed
            .filter { !importantOnly || it.priority <= 2 }
            .sortedWith(compareBy({ it.priority }, { it.date }, { it.time }))
        val used = HashMap<LocalDate, Int>()
        val nextFree = HashMap<LocalDate, Int>()
        input.upcoming.forEach { t ->
            used[t.date] = (used[t.date] ?: 0) + t.minutes
            val end = minuteOf(t.time) + t.minutes + input.breakMinutes
            nextFree[t.date] = max(nextFree[t.date] ?: 0, end)
        }
        val days = (0 until input.horizonDays).map { input.today.plusDays(it.toLong()) }.filter { it.dayOfWeek.value in input.studyDays }
        val moves = ArrayList<ReplanMove>()
        val unplaced = ArrayList<SlotTask>()
        for (t in chosen) {
            // Boş kapasitesi en yüksek gün seçilir; böylece görevler dengeli dağılır.
            val day = days.filter { d ->
                val start = max(nextFree[d] ?: 0, input.startHour * 60)
                (used[d] ?: 0) + t.minutes <= input.dailyCapacity && start + t.minutes <= input.endHour * 60
            }.minByOrNull { (used[it] ?: 0) * 10_000L + ChronoUnit.DAYS.between(input.today, it) }
            if (day == null) { unplaced += t; continue }
            val start = max(nextFree[day] ?: 0, input.startHour * 60)
            moves += ReplanMove(t.id, t.title, t.date, day, PlanGenerator.clock(start))
            used[day] = (used[day] ?: 0) + t.minutes
            nextFree[day] = start + t.minutes + input.breakMinutes
        }
        return ReplanResult(moves, unplaced)
    }

    private fun minuteOf(t: String): Int {
        val h = t.substringBefore(':').toIntOrNull() ?: 0
        val m = t.substringAfter(':', "0").toIntOrNull() ?: 0
        return h * 60 + m
    }
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
    /** Sıklık: DAILY her gün, WEEKDAYS hafta içi, WEEKLY seçilen gün. */
    fun nextFor(now: Long, hour: Int, minute: Int, frequency: String, dayOfWeek: Int, zone: java.time.ZoneId = java.time.ZoneId.systemDefault()): Long {
        if (frequency == "WEEKLY") return next(now, hour, minute, dayOfWeek, zone)
        var at = next(now, hour, minute, null, zone)
        if (frequency == "WEEKDAYS") {
            while (java.time.Instant.ofEpochMilli(at).atZone(zone).dayOfWeek.value > 5) at = next(at, hour, minute, null, zone)
        }
        return at
    }

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
