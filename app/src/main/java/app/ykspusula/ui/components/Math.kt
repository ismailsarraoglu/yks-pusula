package app.ykspusula.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.appendInlineContent
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.ykspusula.domain.MathNode
import app.ykspusula.domain.MathParser
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Formül gösterimi: x^{2}, a_{1}, \frac{a}{b}, \sqrt{x} ve **kalın** biçimlerini metin içinde çizer.
 * Ekran okuyucu için formül düz metin olarak okunur.
 */
@Composable
fun MathText(text: String, modifier: Modifier = Modifier, style: TextStyle = LocalTextStyle.current, color: Color = LocalContentColor.current) {
    val nodes = remember(text) { MathParser.parse(text) }
    val spoken = remember(text) { MathParser.spoken(nodes) }
    val size = if (style.fontSize == TextUnit.Unspecified) 16.sp else style.fontSize
    val inline = HashMap<String, InlineTextContent>()
    val annotated = remember(nodes, color) { buildAnnotated(nodes) }
    collectInline(nodes, inline, size, color, style)
    BasicText(annotated, modifier.semantics { contentDescription = spoken }, style = style.copy(color = color), inlineContent = inline)
}

private fun buildAnnotated(nodes: List<MathNode>): AnnotatedString = buildAnnotatedString {
    var i = 0
    fun walk(list: List<MathNode>) {
        list.forEach { n ->
            when (n) {
                is MathNode.Plain -> append(n.text)
                is MathNode.Bold -> withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(n.text) }
                is MathNode.Sup -> withStyle(SpanStyle(baselineShift = BaselineShift(0.4f), fontSize = 0.72.em)) { walk(n.body) }
                is MathNode.Sub -> withStyle(SpanStyle(baselineShift = BaselineShift(-0.18f), fontSize = 0.72.em)) { walk(n.body) }
                is MathNode.Frac, is MathNode.Root -> appendInlineContent("m${i++}", "[formül]")
            }
        }
    }
    walk(nodes)
}

private fun collectInline(nodes: List<MathNode>, map: HashMap<String, InlineTextContent>, size: TextUnit, color: Color, style: TextStyle) {
    var i = 0
    fun walk(list: List<MathNode>) {
        list.forEach { n ->
            when (n) {
                is MathNode.Sup -> walk(n.body)
                is MathNode.Sub -> walk(n.body)
                is MathNode.Frac -> {
                    val w = (maxOf(MathParser.widthUnits(n.top), MathParser.widthUnits(n.bottom)) * 0.56f + 0.5f)
                    map["m${i++}"] = InlineTextContent(Placeholder((size.value * w).sp, (size.value * 2.5f).sp, PlaceholderVerticalAlign.TextCenter)) {
                        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Sub(n.top, size, color, style)
                            Canvas(Modifier.fillMaxWidth().height(1.5.dp).padding(horizontal = 1.dp)) {
                                drawLine(color, Offset(0f, 0f), Offset(this.size.width, 0f), 2f)
                            }
                            Sub(n.bottom, size, color, style)
                        }
                    }
                }
                is MathNode.Root -> {
                    val w = (MathParser.widthUnits(n.body) * 0.56f + 0.95f)
                    map["m${i++}"] = InlineTextContent(Placeholder((size.value * w).sp, (size.value * 1.5f).sp, PlaceholderVerticalAlign.TextCenter)) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
                            Canvas(Modifier.fillMaxSize()) {
                                val h = this.size.height
                                val s = size.toPx()
                                val p = Path().apply {
                                    moveTo(s * 0.04f, h * 0.62f); lineTo(s * 0.17f, h * 0.55f); lineTo(s * 0.33f, h * 0.95f)
                                    lineTo(s * 0.56f, h * 0.06f); lineTo(this@Canvas.size.width, h * 0.06f)
                                }
                                drawPath(p, color, style = Stroke(1.6f * density * 0.7f))
                            }
                            Box(Modifier.padding(end = 1.dp)) { Sub(n.body, size, color, style) }
                        }
                    }
                }
                else -> Unit
            }
        }
    }
    walk(nodes)
}

@Composable
private fun Sub(nodes: List<MathNode>, size: TextUnit, color: Color, style: TextStyle) {
    Text(
        MathParser.plainOf(nodes), style = style.copy(fontSize = (size.value * 0.84f).sp, lineHeight = (size.value * 1.1f).sp, textAlign = TextAlign.Center, color = color),
        softWrap = false, maxLines = 1,
    )
}

/* ---------- ŞEKİL ---------- */

@Serializable data class FigLine(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val dash: Boolean = false)
@Serializable data class FigCircle(val x: Float, val y: Float, val r: Float)
@Serializable data class FigLabel(val x: Float, val y: Float, val t: String)
@Serializable data class FigureSpec(
    val w: Float = 100f, val h: Float = 60f,
    val lines: List<FigLine> = emptyList(), val circles: List<FigCircle> = emptyList(), val labels: List<FigLabel> = emptyList(),
    val alt: String = "Şekil",
)

private val figJson = Json { ignoreUnknownKeys = true }

@OptIn(ExperimentalTextApi::class)
@Composable
fun FigureView(spec: String, modifier: Modifier = Modifier, color: Color = LocalContentColor.current) {
    val f = remember(spec) { runCatching { figJson.decodeFromString(FigureSpec.serializer(), spec) }.getOrNull() } ?: return
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    Canvas(
        modifier.fillMaxWidth().aspectRatio(f.w / f.h).semantics { contentDescription = f.alt }
    ) {
        val sx = size.width / f.w
        val sy = size.height / f.h
        val s = minOf(sx, sy)
        f.lines.forEach { l ->
            drawLine(
                color, Offset(l.x1 * sx, l.y1 * sy), Offset(l.x2 * sx, l.y2 * sy), 1.6f * density,
                pathEffect = if (l.dash) androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 8f)) else null,
            )
        }
        f.circles.forEach { c -> drawCircle(color, c.r * s, Offset(c.x * sx, c.y * sy), style = Stroke(1.6f * density)) }
        f.labels.forEach { t ->
            val r = measurer.measure(t.t, TextStyle(color = color, fontSize = 13.sp))
            drawText(r, topLeft = Offset(t.x * sx - r.size.width / 2f, t.y * sy - r.size.height / 2f))
        }
    }
}
