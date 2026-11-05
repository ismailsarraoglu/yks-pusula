package app.ykspusula.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.MessageDigest

/** Uygulamaya gömülü başlangıç paketi ile kullanıcının içe aktardığı paketler aynı biçimi kullanır. */
@Serializable data class PackHeader(
    val id: String, val title: String, val version: String, val kind: String = "CONTENT",
    val source: String = "", val license: String = "",
)
@Serializable data class SubjectDto(
    val id: String, val exam: String, val name: String, val groupName: String = "",
    val fields: String = "ALL", val sortOrder: Int = 0, val language: String? = null,
)
@Serializable data class TopicDto(
    val id: String, val subjectId: String, val parentId: String? = null, val name: String,
    val summary: String = "", val lesson: String = "", val keyPoints: String = "", val example: String = "",
    val curriculum: String = "", val validFromYear: Int = 2027, val validToYear: Int = 2027,
    val scope: String = "IN", val scopeNote: String = "", val estimatedMinutes: Int = 120, val sortOrder: Int = 0,
)
@Serializable data class OptionDto(val label: String, val text: String)
@Serializable data class QuestionDto(
    val id: String, val exam: String, val subjectId: String, val topicId: String, val difficulty: Int,
    val stem: String, val figure: String? = null, val options: List<OptionDto>, val correct: String,
    val explanation: String, val steps: List<String> = emptyList(), val examYear: Int = 2027,
    val curriculum: String = "", val source: String = "", val license: String = "", val generated: Boolean = false,
)
@Serializable data class FormulaDto(
    val id: String, val subjectId: String, val subjectName: String, val category: String,
    val title: String, val body: String, val note: String = "",
)
@Serializable data class CardDto(
    val id: String, val deck: String, val subjectName: String, val front: String, val back: String,
    val partOfSpeech: String = "", val example: String = "",
)
@Serializable data class ContentPackFile(
    val pack: PackHeader,
    val examConfigs: List<ExamConfiguration> = emptyList(),
    val subjects: List<SubjectDto> = emptyList(),
    val topics: List<TopicDto> = emptyList(),
    val questions: List<QuestionDto> = emptyList(),
    val formulas: List<FormulaDto> = emptyList(),
    val flashcards: List<CardDto> = emptyList(),
)

val contentJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

object ContentValidator {
    private val LABELS = listOf("A", "B", "C", "D", "E")

    fun hash(q: QuestionDto): String {
        val norm = (listOf(q.stem) + q.options.map { it.text }.sorted())
            .joinToString("|") { it.lowercase().replace(Regex("\\s+"), " ").trim() }
        return MessageDigest.getInstance("SHA-256").digest(norm.toByteArray()).joinToString("") { "%02x".format(it) }.take(40)
    }

    /** Her soru için hataları döndürür. Boş liste: paket geçerli. */
    fun validate(p: ContentPackFile, knownTopics: Set<String> = emptySet(), knownSubjects: Set<String> = emptySet()): List<String> {
        val e = ArrayList<String>()
        if (p.pack.id.isBlank() || p.pack.version.isBlank()) e += "Paket kimliği veya sürümü eksik"
        val subjects = p.subjects.map { it.id }.toSet() + knownSubjects
        val topics = p.topics.map { it.id }.toSet() + knownTopics
        p.topics.forEach { if (it.subjectId !in subjects) e += "${it.id}: ders bulunamadı" }
        val ids = HashSet<String>()
        val hashes = HashSet<String>()
        p.questions.forEach { q ->
            if (!ids.add(q.id)) e += "${q.id}: yinelenen ID"
            if (!hashes.add(hash(q))) e += "${q.id}: aynı soru başka bir ID ile var"
            if (q.stem.isBlank()) e += "${q.id}: soru metni boş"
            if (q.explanation.isBlank()) e += "${q.id}: açıklama boş"
            if (q.topicId !in topics) e += "${q.id}: konu bulunamadı"
            if (q.subjectId !in subjects) e += "${q.id}: ders bulunamadı"
            if (q.difficulty !in 1..3) e += "${q.id}: zorluk 1-3 olmalı"
            val labels = q.options.map { it.label }
            if (labels != LABELS.take(labels.size) || labels.size !in 4..5) e += "${q.id}: seçenekler A-E olmalı"
            if (labels.count { it == q.correct } != 1) e += "${q.id}: tam olarak bir doğru cevap olmalı"
            val texts = q.options.map { it.text.trim().lowercase() }
            if (texts.toSet().size != texts.size) e += "${q.id}: aynı seçenek iki kez var"
            if (texts.any { it.isEmpty() }) e += "${q.id}: boş seçenek"
            if (q.source.isBlank() || q.license.isBlank()) e += "${q.id}: kaynak veya lisans eksik"
        }
        return e
    }

    fun parse(text: String): ContentPackFile = contentJson.decodeFromString(ContentPackFile.serializer(), text)
}
