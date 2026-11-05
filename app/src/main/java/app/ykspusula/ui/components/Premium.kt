package app.ykspusula.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.ripple
import app.ykspusula.ui.theme.Gradients
import app.ykspusula.ui.theme.NumberStyle
import app.ykspusula.ui.theme.Yks
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Yazı büyütüldüğünde veya ekran daraldığında iki sütunlu düzenler tek sütuna düşer. */
@Composable
fun rememberCompact(maxWidth: Dp, limit: Dp = 340.dp): Boolean = maxWidth < limit || LocalDensity.current.fontScale > 1.35f

/** Sekme başlığı: büyük başlık, alt metin, sağ düğme ve ince mavi çizgi. */
@Composable
fun PageHeader(title: String, subtitle: String, action: ImageVector? = null, actionLabel: String = "", onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(top = Space.m), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.displayMedium, color = Yks.c.text, modifier = Modifier.semantics { heading() })
            Text(subtitle, style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Normal), color = Yks.c.muted)
        }
        if (action != null && onAction != null) {
            Box(
                Modifier.size(52.dp).clip(CircleShape).background(Yks.c.card).border(1.dp, Yks.c.line.copy(alpha = 0.6f), CircleShape)
                    .clickable(role = Role.Button, onClick = onAction),
                contentAlignment = Alignment.Center,
            ) { Icon(action, actionLabel, tint = Yks.c.text, modifier = Modifier.size(26.dp)) }
        }
    }
    val c = Yks.c.accent
    Canvas(Modifier.padding(top = 4.dp).width(190.dp).height(10.dp)) {
        val p = Path().apply {
            moveTo(0f, size.height)
            quadraticTo(size.width * 0.35f, 0f, size.width, size.height * 0.45f)
            quadraticTo(size.width * 0.35f, size.height * 0.45f, 0f, size.height)
        }
        drawPath(p, Brush.horizontalGradient(listOf(c, c.copy(alpha = 0f))))
    }
}

/** Sayfa bölüm başlığı: solda başlık, sağda "Tümü" gibi mavi bağlantı. */
@Composable
fun SectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(top = Space.s), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = Yks.c.text, modifier = Modifier.weight(1f).semantics { heading() })
        if (action != null && onAction != null) {
            Row(Modifier.clip(RoundedCornerShape(10.dp)).clickable(role = Role.Button, onClick = onAction).padding(horizontal = 6.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(action, style = MaterialTheme.typography.labelLarge, color = Yks.c.accentLight)
                Spacer(Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = Yks.c.accentLight, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun CircleArrow(bg: Color = Color.White.copy(alpha = 0.22f), size: Dp = 40.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
        Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = Color.White, modifier = Modifier.size(size * 0.55f))
    }
}

/** Sayfanın ana eylemi: ikon, başlık, alt açıklama ve ileri düğmesi. */
@Composable
fun PrimaryGradientCard(title: String, subtitle: String, icon: ImageVector, modifier: Modifier = Modifier, brush: Brush = Gradients.hero, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    val source = remember { MutableInteractionSource() }
    Row(
        modifier.fillMaxWidth().press(source).clip(shape).background(brush).border(1.dp, Color.White.copy(alpha = 0.14f), shape)
            .clickable(source, ripple(), role = Role.Button, onClick = onClick).padding(horizontal = 14.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Color(0xFF58B5FF), modifier = Modifier.size(46.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.width(1.dp).height(44.dp).background(Color.White.copy(alpha = 0.25f)))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp), color = Color.White)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFD5DDEC))
        }
        Spacer(Modifier.width(Space.s))
        CircleArrow(Color(0xFF318BFF))
    }
}

/** TYT, AYT ve YDT kartı. */
@Composable
fun ExamTypeCard(code: String, label: String, icon: ImageVector, brush: Brush, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    val source = remember { MutableInteractionSource() }
    Column(
        modifier.press(source).clip(shape).background(brush).border(1.dp, Color.White.copy(alpha = 0.16f), shape)
            .clickable(source, ripple(), role = Role.Button, onClick = onClick).padding(vertical = Space.l, horizontal = Space.s),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(44.dp))
        Spacer(Modifier.height(Space.s))
        Text(code, style = MaterialTheme.typography.headlineMedium, color = Color.White)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFDDE3F0), textAlign = TextAlign.Center, maxLines = 2)
        Spacer(Modifier.height(Space.s))
        CircleArrow(size = 36.dp)
    }
}

/** İkonlu kısayol: başlık, kısa açıklama ve sağ ok. */
@Composable
fun FeatureShortcutCard(title: String, subtitle: String, icon: ImageVector, tint: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    val c = Yks.c
    val source = remember { MutableInteractionSource() }
    Row(
        modifier.press(source).clip(shape).background(if (c.isDark) Brush.linearGradient(listOf(c.card2, c.card)) else Brush.linearGradient(listOf(c.card, c.card)))
            .border(1.dp, c.line.copy(alpha = 0.6f), shape).clickable(source, ripple(), role = Role.Button, onClick = onClick)
            .padding(start = 10.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(tint.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(Space.s))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp), color = c.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = c.muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = c.muted)
    }
}

/** Ekran genişliğine göre iki veya tek sütuna düşen ızgara. */
@Composable
fun <T> TwoColumn(items: List<T>, item: @Composable (T, Modifier) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val compact = rememberCompact(maxWidth, 360.dp)
        Column(verticalArrangement = Arrangement.spacedBy(Space.s + 2.dp)) {
            items.chunked(if (compact) 1 else 2).forEach { row ->
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Space.s + 2.dp)) {
                    row.forEach { item(it, Modifier.weight(1f).fillMaxHeight()) }
                    if (row.size == 1 && !compact) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/** Açılır-kapanır araç paneli. */
@Composable
fun ExpandableToolsCard(title: String, icon: ImageVector, initiallyOpen: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    var open by rememberSaveableBool(initiallyOpen)
    val rot by animateFloatAsState(if (open) 0f else 180f, tween(220), label = "ok")
    YCard(padding = 0.dp) {
        Row(
            Modifier.fillMaxWidth().clickable(role = Role.Button) { open = !open }.padding(horizontal = Space.l, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, tint = Yks.c.accent, modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(Space.m))
            Text(title, style = MaterialTheme.typography.titleLarge, color = Yks.c.text, modifier = Modifier.weight(1f))
            Icon(Icons.Rounded.ExpandLess, if (open) "Daralt" else "Genişlet", tint = Yks.c.accentLight, modifier = Modifier.graphicsLayer { rotationZ = rot })
        }
        AnimatedVisibility(open) { Column { Line(); content() } }
    }
}

@Composable
private fun rememberSaveableBool(v: Boolean): MutableState<Boolean> = androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(v) }

/** Son deneme satırı. */
@Composable
fun RecentExamCard(title: String, date: String, minutes: Int, net: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    val c = Yks.c
    val source = remember { MutableInteractionSource() }
    Row(
        modifier.fillMaxWidth().press(source).clip(shape).background(if (c.isDark) Brush.linearGradient(listOf(c.card2, c.card)) else Brush.linearGradient(listOf(c.card, c.card)))
            .border(1.dp, c.line.copy(alpha = 0.6f), shape).clickable(source, ripple(), role = Role.Button, onClick = onClick).padding(horizontal = Space.m, vertical = Space.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(c.purple.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.EmojiEvents, null, tint = c.purpleLight, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.width(Space.m))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = c.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CalendarMonth, null, tint = c.muted, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text(date + if (minutes > 0) " • $minutes dk" else "", style = MaterialTheme.typography.labelMedium, color = c.muted, maxLines = 1)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Net", style = MaterialTheme.typography.labelMedium, color = c.muted)
            Text(net, style = MaterialTheme.typography.titleLarge.merge(NumberStyle), color = c.text)
        }
        Spacer(Modifier.width(4.dp))
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = c.muted)
    }
}

/** Başlık, sayısal özet ve ilerleme çubuğu. */
@Composable
fun ProgressCard(title: String, value: String, fraction: Float, detail: String? = null, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    YCard(modifier, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Yks.c.text, modifier = Modifier.weight(1f))
            Text(value, style = MaterialTheme.typography.titleMedium.merge(NumberStyle), color = Yks.c.accentLight)
        }
        Spacer(Modifier.height(Space.s))
        ProgressBar(fraction)
        if (detail != null) { Spacer(Modifier.height(6.dp)); Muted(detail) }
    }
}

/** Günün sözü kartı: lacivert-mor degrade, zarif ikon. */
@Composable
fun MotivationCard(text: String, category: String, favorite: Boolean, modifier: Modifier = Modifier, onFavorite: () -> Unit, onOpen: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    val source = remember { MutableInteractionSource() }
    Box(
        modifier.fillMaxWidth().press(source).clip(shape).background(Gradients.motivation).border(1.dp, Color(0xFF8452F5).copy(alpha = 0.35f), shape)
            .clickable(source, ripple(), role = Role.Button, onClick = onOpen),
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawCircle(Brush.radialGradient(listOf(Color(0x338452F5), Color.Transparent), center = Offset(size.width, 0f), radius = size.width * 0.7f), radius = size.width * 0.7f, center = Offset(size.width, 0f))
        }
        Column(Modifier.padding(Space.l)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.FormatQuote, null, tint = Color(0xFFA47AFF), modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(6.dp))
                Text("Günün Motivasyon Sözü", style = MaterialTheme.typography.labelLarge, color = Color(0xFFC9BBFF), modifier = Modifier.weight(1f))
                IconButton(onFavorite, Modifier.size(36.dp)) {
                    Icon(if (favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder, if (favorite) "Favorilerden çıkar" else "Favorilere ekle", tint = if (favorite) Color(0xFFFFB84D) else Color(0xFFC9BBFF))
                }
            }
            Spacer(Modifier.height(Space.xs))
            Text(text, style = MaterialTheme.typography.titleMedium, color = Color.White)
            Spacer(Modifier.height(Space.xs))
            Text(category, style = MaterialTheme.typography.labelMedium, color = Color(0xFFA7B1C5))
        }
    }
}

/** Sayı alanı. */
@Composable
fun NumberField(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, max: Int = 4) {
    Field(value, { onChange(it.filter(Char::isDigit).take(max)) }, label, modifier, keyboard = KeyboardOptions(keyboardType = KeyboardType.Number))
}

/** Tarih alanı: sistem takvimiyle seçilir. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(value: LocalDate, label: String, modifier: Modifier = Modifier, onChange: (LocalDate) -> Unit) {
    var open by remember { mutableStateOf(false) }
    SoftButton("$label: ${value.dayOfMonth}.${"%02d".format(value.monthValue)}.${value.year}", modifier.fillMaxWidth(), color = Yks.c.text, icon = Icons.Rounded.CalendarMonth) { open = true }
    if (open) {
        val state = rememberDatePickerState(value.toEpochDay() * 86_400_000L)
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton({
                    state.selectedDateMillis?.let { onChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    open = false
                }) { Text("Seç") }
            },
            dismissButton = { TextButton({ open = false }) { Text("Vazgeç") } },
        ) { DatePicker(state, showModeToggle = false) }
    }
}

/** Saat alanı. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeField(hour: Int, minute: Int, label: String, modifier: Modifier = Modifier, onChange: (Int, Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    SoftButton("$label: %02d:%02d".format(java.util.Locale.US, hour, minute), modifier.fillMaxWidth(), color = Yks.c.text) { open = true }
    if (open) {
        val state = rememberTimePickerState(hour, minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { open = false }, containerColor = Yks.c.card, title = { Text(label) },
            text = { TimeInput(state) },
            confirmButton = { TextButton({ onChange(state.hour, state.minute); open = false }) { Text("Seç") } },
            dismissButton = { TextButton({ open = false }) { Text("Vazgeç") } },
        )
    }
}

@Composable
fun InfoBanner(text: String, icon: ImageVector, tint: Color = Yks.c.warning) {
    YCard(padding = Space.m) {
        Row {
            Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(Space.s))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = Yks.c.text)
        }
    }
}

/** İki sayı yan yana: etiket ve değer satırı. */
@Composable
fun KeyValue(key: String, value: String, valueColor: Color = Yks.c.text) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(key, style = MaterialTheme.typography.bodyMedium, color = Yks.c.muted, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium.merge(NumberStyle.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)), color = valueColor)
    }
}
