package app.ykspusula.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** YKS Pusula Premium Dark renk jetonları. Ekranlar renkleri yalnızca buradan alır. */
@Immutable
data class YksColors(
    val bg: Color, val bg2: Color, val card: Color, val card2: Color, val text: Color, val muted: Color, val accent: Color, val accentLight: Color,
    val purple: Color, val purpleLight: Color, val success: Color, val warning: Color, val error: Color, val line: Color, val soft: Color,
    val isDark: Boolean,
)

val DarkYks = YksColors(
    bg = Color(0xFF0B0F18), bg2 = Color(0xFF111827), card = Color(0xFF192235), card2 = Color(0xFF202B40),
    text = Color(0xFFF7F8FC), muted = Color(0xFFA7B1C5), accent = Color(0xFF318BFF), accentLight = Color(0xFF58B5FF),
    purple = Color(0xFF8452F5), purpleLight = Color(0xFFA47AFF), success = Color(0xFF36C98F), warning = Color(0xFFFFB84D), error = Color(0xFFFF6475),
    line = Color(0xFF303B50), soft = Color(0xFF1C2C4A), isDark = true,
)
val LightYks = YksColors(
    bg = Color(0xFFF3F5FA), bg2 = Color(0xFFE9EDF6), card = Color(0xFFFFFFFF), card2 = Color(0xFFF2F5FB),
    text = Color(0xFF131A2B), muted = Color(0xFF5B667C), accent = Color(0xFF1F6FE5), accentLight = Color(0xFF2F8BF0),
    purple = Color(0xFF6C3BE6), purpleLight = Color(0xFF8452F5), success = Color(0xFF12996A), warning = Color(0xFFB9770E), error = Color(0xFFD63B4C),
    line = Color(0xFFDDE3EE), soft = Color(0xFFE7EFFD), isDark = false,
)

/** Ortak degradeler. Her karta değil, yalnızca öne çıkan eylemlere uygulanır. */
object Gradients {
    val blue = Brush.linearGradient(listOf(Color(0xFF2B7BF0), Color(0xFF1A4FA8), Color(0xFF12386F)))
    val purple = Brush.linearGradient(listOf(Color(0xFF7A5BE0), Color(0xFF55389F), Color(0xFF3A2878)))
    val navy = Brush.linearGradient(listOf(Color(0xFF3F5478), Color(0xFF2A3A58), Color(0xFF1C2A44)))
    val hero = Brush.linearGradient(listOf(Color(0xFF34496E), Color(0xFF263755), Color(0xFF1D2B46)))
    val motivation = Brush.linearGradient(listOf(Color(0xFF17233F), Color(0xFF241F52), Color(0xFF2C2468)))
    val bar = Brush.horizontalGradient(listOf(Color(0xFF318BFF), Color(0xFF8452F5)))
    val button = Brush.horizontalGradient(listOf(Color(0xFF318BFF), Color(0xFF2468E0)))
}

val LocalYks = staticCompositionLocalOf { DarkYks }

object Yks {
    val c: YksColors @Composable @ReadOnlyComposable get() = LocalYks.current
}

private val AppType = Typography(
    displayMedium = TextStyle(fontSize = 38.sp, lineHeight = 44.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.8).sp),
    displaySmall = TextStyle(fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontSize = 21.sp, lineHeight = 27.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
)

/** Sayısal değerler hizalı kalsın diye sabit genişlikli rakamlar. */
val NumberStyle = TextStyle(fontFeatureSettings = "tnum", fontWeight = FontWeight.Bold)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp), extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun YksTheme(mode: String, fontScale: Float, content: @Composable () -> Unit) {
    val dark = when (mode) { "DARK" -> true; "LIGHT" -> false; else -> isSystemInDarkTheme() }
    val c = if (dark) DarkYks else LightYks
    val scheme = if (dark) {
        darkColorScheme(
            primary = c.accent, onPrimary = Color.White, background = c.bg, onBackground = c.text, surface = c.card, onSurface = c.text,
            surfaceVariant = c.card2, onSurfaceVariant = c.muted, surfaceContainerHigh = c.card, surfaceContainerHighest = c.card2,
            surfaceContainer = c.card, surfaceContainerLow = c.card, error = c.error, outline = c.line, outlineVariant = c.line, secondaryContainer = c.soft, onSecondaryContainer = c.accentLight,
            primaryContainer = c.soft, onPrimaryContainer = c.accentLight, tertiary = c.purpleLight,
        )
    } else {
        lightColorScheme(
            primary = c.accent, onPrimary = Color.White, background = c.bg, onBackground = c.text, surface = c.card, onSurface = c.text,
            surfaceVariant = c.card2, onSurfaceVariant = c.muted, surfaceContainerHigh = c.card, surfaceContainerHighest = c.card2,
            surfaceContainer = c.card, surfaceContainerLow = c.card, error = c.error, outline = c.line, outlineVariant = c.line, secondaryContainer = c.soft, onSecondaryContainer = c.accent,
            primaryContainer = c.soft, onPrimaryContainer = c.accent, tertiary = c.purple,
        )
    }
    val d = LocalDensity.current
    CompositionLocalProvider(LocalYks provides c, LocalDensity provides Density(d.density, d.fontScale * fontScale)) {
        MaterialTheme(colorScheme = scheme, typography = AppType, shapes = AppShapes, content = content)
    }
}
