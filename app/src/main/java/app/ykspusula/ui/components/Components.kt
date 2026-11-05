package app.ykspusula.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.ykspusula.ui.theme.Yks

object Space { val xs = 4.dp; val s = 8.dp; val m = 12.dp; val l = 16.dp; val xl = 24.dp; val xxl = 32.dp }

@Composable
fun YCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, padding: Dp = Space.l, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    val base = modifier.fillMaxWidth()
        .then(if (Yks.c.isDark) Modifier else Modifier.shadow(1.dp, shape, ambientColor = Color(0x14000000), spotColor = Color(0x14000000)))
        .clip(shape).background(Yks.c.card)
        .then(if (Yks.c.isDark) Modifier.border(BorderStroke(0.5.dp, Yks.c.line), shape) else Modifier)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick, role = Role.Button) else Modifier)
        .padding(padding)
    Column(base, content = content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Screen(title: String, onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}, content: @Composable ColumnScope.() -> Unit) {
    Scaffold(
        containerColor = Yks.c.bg,
        topBar = {
            TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { heading() }) },
                navigationIcon = { if (onBack != null) IconButton(onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Geri") } },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Yks.c.bg, titleContentColor = Yks.c.text, navigationIconContentColor = Yks.c.text, actionIconContentColor = Yks.c.text),
            )
        },
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize(), content = content)
    }
}

@Composable
fun ScrollColumn(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Box(modifier.fillMaxSize().verticalScroll(rememberScrollState()), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 720.dp).fillMaxWidth().padding(horizontal = Space.l, vertical = Space.s),
            verticalArrangement = Arrangement.spacedBy(Space.m),
        ) {
            content()
            Spacer(Modifier.height(Space.xl))
        }
    }
}

@Composable
fun Section(title: String, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = Space.s), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = Yks.c.text, modifier = Modifier.weight(1f).semantics { heading() })
        trailing?.invoke()
    }
}

@Composable
fun Muted(text: String, modifier: Modifier = Modifier, maxLines: Int = Int.MAX_VALUE) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = Yks.c.muted, modifier = modifier, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
}

@Composable
fun ProgressBar(fraction: Float, modifier: Modifier = Modifier, color: Color = Yks.c.accent, height: Dp = 8.dp) {
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(450), label = "bar")
    Box(modifier.fillMaxWidth().height(height).clip(CircleShape).background(Yks.c.line)) {
        Box(Modifier.fillMaxWidth(f).fillMaxHeight().clip(CircleShape).background(color))
    }
}

@Composable
fun Ring(fraction: Float, label: String, modifier: Modifier = Modifier, size: Dp = 84.dp, color: Color = Yks.c.accent, description: String = label) {
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(500), label = "ring")
    val track = Yks.c.line
    Box(modifier.size(size).semantics(mergeDescendants = true) { contentDescription = description }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = 9.dp.toPx()
            val s = Size(this.size.width - w, this.size.height - w)
            val o = Offset(w / 2, w / 2)
            drawArc(track, 0f, 360f, false, o, s, style = Stroke(w))
            drawArc(color, -90f, 360f * f, false, o, s, style = Stroke(w, cap = StrokeCap.Round))
        }
        Text(label, style = MaterialTheme.typography.titleMedium, color = Yks.c.text)
    }
}

@Composable
fun Pill(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.defaultMinSize(minHeight = 44.dp).clip(CircleShape)
            .background(if (selected) Yks.c.accent else Yks.c.card)
            .then(if (selected) Modifier else Modifier.border(1.dp, Yks.c.line, CircleShape))
            .clickable(role = Role.RadioButton, onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (selected) Icon(Icons.Rounded.Check, null, tint = if (Yks.c.isDark) Color(0xFF0B1A33) else Color.White, modifier = Modifier.size(16.dp))
            Text(text, style = MaterialTheme.typography.labelLarge, color = if (selected) (if (Yks.c.isDark) Color(0xFF0B1A33) else Color.White) else Yks.c.text)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PillGroup(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.s), verticalArrangement = Arrangement.spacedBy(Space.s)) {
        options.forEach { (id, label) -> Pill(label, id == selected) { onSelect(id) } }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MultiPills(options: List<Pair<String, String>>, selected: Set<String>, onToggle: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.s), verticalArrangement = Arrangement.spacedBy(Space.s)) {
        options.forEach { (id, label) -> Pill(label, id in selected) { onToggle(id) } }
    }
}

@Composable
fun Tile(value: String, label: String, modifier: Modifier = Modifier, tint: Color = Yks.c.text) {
    YCard(modifier, padding = Space.m) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = tint, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(label, style = MaterialTheme.typography.labelMedium, color = Yks.c.muted, maxLines = 2)
    }
}

@Composable
fun Row2(left: @Composable (Modifier) -> Unit, right: @Composable (Modifier) -> Unit) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Space.m)) {
        left(Modifier.weight(1f).fillMaxHeight()); right(Modifier.weight(1f).fillMaxHeight())
    }
}

@Composable
fun NavRow(title: String, subtitle: String? = null, icon: ImageVector? = null, tint: Color = Yks.c.accent, onClick: () -> Unit, trailing: @Composable (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp).clickable(role = Role.Button, onClick = onClick).padding(horizontal = Space.l, vertical = Space.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(tint.copy(alpha = 0.13f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(Space.m))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = Yks.c.text)
            if (subtitle != null) Muted(subtitle, maxLines = 2)
        }
        if (trailing != null) trailing() else Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Yks.c.muted)
    }
}

@Composable
fun GroupCard(content: @Composable ColumnScope.() -> Unit) = YCard(padding = 0.dp, content = content)

@Composable
fun Line() = HorizontalDivider(thickness = 0.5.dp, color = Yks.c.line)

@Composable
fun Empty(icon: ImageVector, title: String, detail: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = Space.l), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(64.dp).clip(CircleShape).background(Yks.c.soft), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Yks.c.accent, modifier = Modifier.size(30.dp)) }
        Spacer(Modifier.height(Space.m))
        Text(title, style = MaterialTheme.typography.titleMedium, color = Yks.c.text, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Space.xs))
        Text(detail, style = MaterialTheme.typography.bodyMedium, color = Yks.c.muted, textAlign = TextAlign.Center)
        if (action != null && onAction != null) { Spacer(Modifier.height(Space.m)); MainButton(action, onClick = onAction) }
    }
}

@Composable
fun MainButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, icon: ImageVector? = null, onClick: () -> Unit) {
    Button(onClick, modifier.defaultMinSize(minHeight = 50.dp), enabled = enabled, shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)) {
        if (icon != null) { Icon(icon, null, Modifier.size(20.dp)); Spacer(Modifier.width(Space.s)) }
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SoftButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, icon: ImageVector? = null, color: Color = Yks.c.accent, onClick: () -> Unit) {
    OutlinedButton(onClick, modifier.defaultMinSize(minHeight = 48.dp), enabled = enabled, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Yks.c.line), colors = ButtonDefaults.outlinedButtonColors(contentColor = color)) {
        if (icon != null) { Icon(icon, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)) }
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun Field(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, singleLine: Boolean = true, minLines: Int = 1, keyboard: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default) {
    OutlinedTextField(value, onChange, modifier.fillMaxWidth(), label = { Text(label) }, singleLine = singleLine, minLines = minLines, keyboardOptions = keyboard, shape = RoundedCornerShape(14.dp))
}

@Composable
fun Tag(text: String, color: Color = Yks.c.accent) {
    Box(Modifier.clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.13f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String, danger: Boolean = false, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text(title) }, text = { Text(text) },
        confirmButton = { TextButton(onConfirm) { Text(confirm, color = if (danger) Yks.c.error else Yks.c.accent) } },
        dismissButton = { TextButton(onDismiss) { Text("Vazgeç") } },
        containerColor = Yks.c.card,
    )
}
