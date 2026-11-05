package app.ykspusula.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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
    val formulas: List<FormulaDto> = emptyList(),
    val flashcards: List<CardDto> = emptyList(),
)

val contentJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

object ContentValidator {
    /** Boş liste: paket geçerli. Hatalı paket hiç uygulanmaz. */
    fun validate(p: ContentPackFile, knownTopics: Set<String> = emptySet(), knownSubjects: Set<String> = emptySet()): List<String> {
        val e = ArrayList<String>()
        if (p.pack.id.isBlank() || p.pack.version.isBlank()) e += "Paket kimliği veya sürümü eksik"
        if (p.pack.source.isBlank()) e += "Paket kaynağı eksik"
        val subjects = p.subjects.map { it.id }.toSet() + knownSubjects
        p.topics.forEach { if (it.subjectId !in subjects) e += "${it.id}: ders bulunamadı" }
        val ids = HashSet<String>()
        p.topics.forEach { if (!ids.add(it.id)) e += "${it.id}: yinelenen konu" }
        p.formulas.forEach { if (it.title.isBlank() || it.body.isBlank()) e += "${it.id}: formül boş" }
        p.flashcards.forEach { if (it.front.isBlank() || it.back.isBlank()) e += "${it.id}: kart boş" }
        p.examConfigs.forEach { c ->
            if (runCatching { java.time.LocalDate.parse(c.examDate) }.isFailure) e += "${c.code}: sınav tarihi geçersiz"
            if (c.wrongDivisor <= 0.0) e += "${c.code}: net kuralı geçersiz"
        }
        return e
    }

    fun parse(text: String): ContentPackFile = contentJson.decodeFromString(ContentPackFile.serializer(), text)
}
