package app.ykspusula.report

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import app.ykspusula.data.Repository
import app.ykspusula.domain.Scoring
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Rapor gerçek verilerden çizilir ve Android paylaşım menüsüyle gönderilir. Dosya yalnızca cihazda oluşturulur. */
object ReportExporter {
    private const val W = 1080
    private val fmt = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("tr", "TR"))

    private fun lines(r: Repository.Report): List<Pair<String, String>> {
        val out = ArrayList<Pair<String, String>>()
        val h = "H"
        out += h to "Özet"
        out += "" to "Toplam çalışma: ${r.minutes / 60} sa ${r.minutes % 60} dk"
        out += "" to "Günlük ortalama: ${r.avgMinutes} dk • Aktif gün: ${r.activeDays}"
        if (r.prevMinutes > 0) out += "" to "Önceki döneme göre: ${r.minutes - r.prevMinutes} dk"
        out += "" to "Tamamlanan görev: ${r.doneTasks} • Bekleyen: ${r.openTasks.size}"
        out += "" to "Çözülen soru: ${r.solved} (${r.correct} doğru, ${r.wrong} yanlış, ${r.blank} boş)"
        out += "" to "Tekrar edilen yanlış: ${r.wrongReviews}"
        if (r.subjects.isNotEmpty()) { out += h to "Derslere göre süre"; r.subjects.take(8).forEach { out += "" to "${it.first}: ${it.second} dk" } }
        if (r.topics.isNotEmpty()) { out += h to "Çalışılan konular"; r.topics.take(8).forEach { out += "" to it } }
        if (r.sources.isNotEmpty()) { out += h to "Kaynaklar"; r.sources.take(5).forEach { out += "" to "${it.first}: ${it.second} dk" } }
        if (r.finishedSources.isNotEmpty()) { out += h to "Tamamlanan kaynaklar"; r.finishedSources.forEach { out += "" to it } }
        if (r.exams.isNotEmpty()) { out += h to "Denemeler"; r.exams.forEach { out += "" to "${it.title}: ${Scoring.formatNet(it.net)} net" } }
        if (r.review.hasEnoughData) {
            out += h to "Değerlendirme"
            r.review.bestTopic?.let { out += "" to "En iyi ilerleme: $it" }
            r.review.hardestSubject?.let { out += "" to "Geliştirilebilecek alan: $it" }
            r.review.nextWeek.forEach { out += "" to it }
        }
        return out
    }

    private fun draw(canvas: Canvas, r: Repository.Report, width: Int, height: Int) {
        val bg = Paint().apply { shader = LinearGradient(0f, 0f, 0f, height.toFloat(), Color.parseColor("#0B0F18"), Color.parseColor("#111827"), Shader.TileMode.CLAMP) }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bg)
        val title = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F7F8FC"); textSize = 56f; typeface = Typeface.DEFAULT_BOLD }
        val sub = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#A7B1C5"); textSize = 30f }
        val head = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#58B5FF"); textSize = 36f; typeface = Typeface.DEFAULT_BOLD }
        val body = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F7F8FC"); textSize = 30f }
        var y = 100f
        canvas.drawText("YKS Pusula", 60f, y, sub); y += 70
        canvas.drawText(r.title, 60f, y, title); y += 50
        canvas.drawText("${r.from.format(fmt)} - ${r.to.format(fmt)}", 60f, y, sub); y += 30
        val line = Paint().apply { shader = LinearGradient(60f, 0f, 500f, 0f, Color.parseColor("#318BFF"), Color.TRANSPARENT, Shader.TileMode.CLAMP) }
        canvas.drawRoundRect(RectF(60f, y, 500f, y + 5), 3f, 3f, line); y += 50
        lines(r).forEach { (k, t) ->
            if (k.isNotEmpty()) {
                y += 24
                canvas.drawText(t, 60f, y + 30, head); y += 56
            } else {
                val sl = StaticLayout.Builder.obtain(t, 0, t.length, body, width - 120).setAlignment(Layout.Alignment.ALIGN_NORMAL).build()
                canvas.save(); canvas.translate(60f, y); sl.draw(canvas); canvas.restore()
                y += sl.height + 10
            }
        }
    }

    private fun height(r: Repository.Report): Int = 340 + lines(r).sumOf { (k, t) -> if (k.isNotEmpty()) 80 else (if (t.length > 60) 92 else 46) }

    private fun dir(c: Context) = File(c.cacheDir, "paylasim").apply { mkdirs() }

    suspend fun png(c: Context, r: Repository.Report): File = withContext(Dispatchers.Default) {
        val h = height(r)
        val bmp = Bitmap.createBitmap(W, h, Bitmap.Config.ARGB_8888)
        draw(Canvas(bmp), r, W, h)
        File(dir(c), "yks-pusula-rapor.png").also { f -> f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 95, it) }; bmp.recycle() }
    }

    suspend fun pdf(c: Context, r: Repository.Report): File = withContext(Dispatchers.Default) {
        val h = height(r)
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(W, h, 1).create())
        draw(page.canvas, r, W, h)
        doc.finishPage(page)
        File(dir(c), "yks-pusula-rapor.pdf").also { f -> f.outputStream().use { doc.writeTo(it) }; doc.close() }
    }

    fun share(c: Context, f: File, mime: String) {
        val uri = FileProvider.getUriForFile(c, "${c.packageName}.files", f)
        val i = Intent(Intent.ACTION_SEND).setType(mime).putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        c.startActivity(Intent.createChooser(i, "Raporu paylaş").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /** Metin paylaşımı: motivasyon sözü gibi. */
    fun shareText(c: Context, text: String) {
        val i = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        c.startActivity(Intent.createChooser(i, "Paylaş").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
