package app.ykspusula.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.ykspusula.ui.theme.Yks
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

private fun niceMax(v: Double): Double {
    if (v <= 0) return 1.0
    val mag = Math.pow(10.0, Math.floor(Math.log10(v)))
    val n = v / mag
    return (if (n <= 1) 1.0 else if (n <= 2) 2.0 else if (n <= 5) 5.0 else 10.0) * mag
}

/** Grafikte dokunulan değeri metinle gösterir; ekran okuyucu özeti okur. */
@Composable
private fun ChartFrame(title: String, summary: String, readout: String, content: @Composable () -> Unit) {
    YCard(Modifier.semantics(mergeDescendants = true) { contentDescription = "$title. $summary" }) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = Yks.c.text)
        Text(readout.ifEmpty { summary }, style = MaterialTheme.typography.labelMedium, color = Yks.c.muted, maxLines = 2)
        Spacer(Modifier.height(Space.m))
        content()
    }
}

@Composable
fun BarChart(title: String, labels: List<String>, values: List<Double>, unit: String, height: Dp = 150.dp, labelEvery: Int = 1) {
    var sel by remember(labels, values) { mutableIntStateOf(-1) }
    val top = niceMax(values.maxOrNull() ?: 0.0)
    val total = values.sum()
    val bar = Yks.c.accent
    val dim = Yks.c.accent.copy(alpha = 0.55f)
    val grid = Yks.c.line
    val muted = Yks.c.muted
    ChartFrame(title, "Toplam ${total.roundToInt()} $unit, en yüksek ${(values.maxOrNull() ?: 0.0).roundToInt()} $unit.", if (sel >= 0) "${labels[sel]}: ${values[sel].roundToInt()} $unit" else "") {
        Canvas(
            Modifier.fillMaxWidth().height(height).pointerInput(labels, values) {
                detectTapGestures { o ->
                    val slot = size.width.toFloat() / values.size
                    sel = (o.x / slot).toInt().coerceIn(0, values.lastIndex)
                }
            }
        ) {
            val h = size.height
            listOf(0.5f, 1f).forEach { f -> drawLine(grid, Offset(0f, h * (1 - f)), Offset(size.width, h * (1 - f)), 1f) }
            drawLine(muted.copy(alpha = 0.5f), Offset(0f, h), Offset(size.width, h), 1f)
            val slot = size.width / values.size
            val w = (slot * 0.56f).coerceAtMost(14.dp.toPx()).coerceAtLeast(2.dp.toPx())
            values.forEachIndexed { i, v ->
                if (v <= 0) return@forEachIndexed
                val bh = max(((v / top) * h).toFloat(), 2.dp.toPx())
                val x = slot * i + (slot - w) / 2
                val r = CornerRadius(minOf(4.dp.toPx(), w / 2))
                val path = Path().apply {
                    addRoundRect(RoundRect(x, h - bh, x + w, h, topLeftCornerRadius = r, topRightCornerRadius = r))
                }
                drawPath(path, if (sel < 0 || sel == i) bar else dim)
            }
        }
        Spacer(Modifier.height(4.dp))
        AxisLabels(labels, labelEvery)
    }
}

@Composable
private fun AxisLabels(labels: List<String>, every: Int) {
    Row(Modifier.fillMaxWidth()) {
        labels.forEachIndexed { i, l ->
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                if (i % every == 0) Text(l, style = MaterialTheme.typography.labelMedium, color = Yks.c.muted, maxLines = 1, softWrap = false)
            }
        }
    }
}

@Composable
fun LineChart(title: String, labels: List<String>, values: List<Double>, format: (Double) -> String, height: Dp = 150.dp) {
    var sel by remember(labels, values) { mutableIntStateOf(-1) }
    val line = Yks.c.accent
    val grid = Yks.c.line
    val surface = Yks.c.card
    val lo = minOf(0.0, values.minOrNull() ?: 0.0)
    val hi = niceMax(values.maxOrNull() ?: 1.0)
    val summary = if (values.size >= 2) "İlk değer ${format(values.first())}, son değer ${format(values.last())}." else "Tek kayıt: ${values.firstOrNull()?.let(format) ?: "-"}."
    ChartFrame(title, summary, if (sel >= 0) "${labels[sel]}: ${format(values[sel])}" else "") {
        Canvas(
            Modifier.fillMaxWidth().height(height).pointerInput(labels, values) {
                detectTapGestures { o ->
                    if (values.size == 1) sel = 0 else {
                        val step = size.width.toFloat() / values.lastIndex
                        sel = ((o.x + step / 2) / step).toInt().coerceIn(0, values.lastIndex)
                    }
                }
            }
        ) {
            val pad = 6.dp.toPx()
            val h = size.height - pad * 2
            val w = size.width - pad * 2
            fun px(i: Int) = pad + if (values.size == 1) w / 2 else w * i / values.lastIndex
            fun py(v: Double) = pad + h * (1f - ((v - lo) / (hi - lo)).toFloat())
            listOf(0f, 0.5f, 1f).forEach { f -> drawLine(grid, Offset(0f, pad + h * f), Offset(size.width, pad + h * f), 1f) }
            val path = Path()
            values.forEachIndexed { i, v -> if (i == 0) path.moveTo(px(i), py(v)) else path.lineTo(px(i), py(v)) }
            drawPath(path, line, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
            values.forEachIndexed { i, v ->
                drawCircle(surface, 6.dp.toPx(), Offset(px(i), py(v)))
                drawCircle(line, if (i == sel) 5.dp.toPx() else 4.dp.toPx(), Offset(px(i), py(v)))
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth()) {
            Text(labels.firstOrNull().orEmpty(), style = MaterialTheme.typography.labelMedium, color = Yks.c.muted, modifier = Modifier.weight(1f))
            if (labels.size > 1) Text(labels.last(), style = MaterialTheme.typography.labelMedium, color = Yks.c.muted)
        }
    }
}

/** Yatay yüzde çubukları: değer her zaman metinle yazılır. */
@Composable
fun PercentBars(title: String, rows: List<Triple<String, Int, String>>) {
    YCard {
        Text(title, style = MaterialTheme.typography.titleMedium, color = Yks.c.text)
        Spacer(Modifier.height(Space.s))
        rows.forEach { (name, pct, detail) ->
            Column(Modifier.padding(vertical = 5.dp).semantics(mergeDescendants = true) { contentDescription = "$name yüzde $pct, $detail" }) {
                Row {
                    Text(name, style = MaterialTheme.typography.bodyMedium, color = Yks.c.text, modifier = Modifier.weight(1f), maxLines = 1)
                    Text("%$pct", style = MaterialTheme.typography.labelLarge, color = Yks.c.text)
                }
                Spacer(Modifier.height(3.dp))
                ProgressBar(pct / 100f, height = 6.dp)
                Muted(detail)
            }
        }
    }
}

/** Doğru-yanlış-boş dağılımı: durum renkleri her zaman etiket ve sayıyla birlikte. */
@Composable
fun StackedBar(title: String, parts: List<Triple<String, Int, Color>>) {
    val total = parts.sumOf { it.second }
    YCard {
        Text(title, style = MaterialTheme.typography.titleMedium, color = Yks.c.text)
        Spacer(Modifier.height(Space.m))
        Row(Modifier.fillMaxWidth().height(12.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            parts.filter { it.second > 0 }.forEach { (_, v, c) ->
                Box(Modifier.weight(v.toFloat()).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(c))
            }
        }
        Spacer(Modifier.height(Space.s))
        parts.forEach { (name, v, c) ->
            Row(Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(c))
                Spacer(Modifier.width(Space.s))
                Text(name, style = MaterialTheme.typography.bodyMedium, color = Yks.c.text, modifier = Modifier.weight(1f))
                Text("$v (%${if (total == 0) 0 else (v * 100.0 / total).roundToInt()})", style = MaterialTheme.typography.bodyMedium, color = Yks.c.muted)
            }
        }
    }
}

/** Alışkanlık takvimi: tek renk tonu, koyuluk çalışma miktarını gösterir. */
@Composable
fun HabitCalendar(title: String, days: List<java.time.LocalDate>, values: Map<String, Int>) {
    var sel by remember { mutableStateOf<java.time.LocalDate?>(null) }
    val top = max(1, values.values.maxOrNull() ?: 1)
    val accent = Yks.c.accent
    val empty = Yks.c.line
    val first = days.first()
    val offset = first.dayOfWeek.value - 1
    val weeks = ceil((days.size + offset) / 7.0).toInt()
    val activeDays = days.count { (values[it.toString()] ?: 0) > 0 }
    ChartFrame(title, "Son ${days.size} günün $activeDays gününde çalışma kaydı var.", sel?.let { "$it: ${values[it.toString()] ?: 0} dakika" } ?: "") {
        Canvas(
            Modifier.fillMaxWidth().aspectRatio(weeks / 7f).pointerInput(days) {
                detectTapGestures { o ->
                    val cell = size.width.toFloat() / weeks
                    val idx = (o.x / cell).toInt() * 7 + (o.y / cell).toInt() - offset
                    sel = days.getOrNull(idx)
                }
            }
        ) {
            val cell = size.width / weeks
            val gap = 2.dp.toPx()
            days.forEachIndexed { i, d ->
                val k = i + offset
                val v = values[d.toString()] ?: 0
                val alpha = if (v == 0) 1f else 0.3f + 0.7f * (v.toFloat() / top)
                drawRoundRect(
                    if (v == 0) empty else accent.copy(alpha = alpha), Offset((k / 7) * cell + gap / 2, (k % 7) * cell + gap / 2),
                    Size(cell - gap, cell - gap), CornerRadius(3.dp.toPx()),
                )
            }
        }
    }
}
