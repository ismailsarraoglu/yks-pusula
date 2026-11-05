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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Immutable
data class YksColors(
    val bg: Color, val card: Color, val text: Color, val muted: Color, val accent: Color,
    val success: Color, val warning: Color, val error: Color, val line: Color, val soft: Color,
    val isDark: Boolean,
)

val LightYks = YksColors(
    bg = Color(0xFFF5F5F7), card = Color(0xFFFFFFFF), text = Color(0xFF1D1D1F), muted = Color(0xFF777780),
    accent = Color(0xFF3478F6), success = Color(0xFF28A568), warning = Color(0xFFE8A23A), error = Color(0xFFE35454),
    line = Color(0xFFE4E4E9), soft = Color(0xFFEDF3FF), isDark = false,
)
val DarkYks = YksColors(
    bg = Color(0xFF111114), card = Color(0xFF1C1C22), text = Color(0xFFF5F5F7), muted = Color(0xFFA1A1AA),
    accent = Color(0xFF6A9EFF), success = Color(0xFF45C283), warning = Color(0xFFF0B45A), error = Color(0xFFF07070),
    line = Color(0xFF2E2E36), soft = Color(0xFF232C40), isDark = true,
)

val LocalYks = staticCompositionLocalOf { LightYks }

object Yks {
    val c: YksColors @Composable @ReadOnlyComposable get() = LocalYks.current
}

private val AppType = Typography(
    displaySmall = TextStyle(fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp), extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun YksTheme(mode: String, fontScale: Float, content: @Composable () -> Unit) {
    val dark = when (mode) { "DARK" -> true; "LIGHT" -> false; else -> isSystemInDarkTheme() }
    val c = if (dark) DarkYks else LightYks
    val scheme = if (dark) {
        darkColorScheme(
            primary = c.accent, onPrimary = Color(0xFF0B1A33), background = c.bg, onBackground = c.text, surface = c.card, onSurface = c.text,
            surfaceVariant = c.line, onSurfaceVariant = c.muted, surfaceContainerHigh = c.card, surfaceContainerHighest = c.card,
            surfaceContainer = c.card, surfaceContainerLow = c.card, error = c.error, outline = c.line, outlineVariant = c.line, secondaryContainer = c.soft, onSecondaryContainer = c.accent,
        )
    } else {
        lightColorScheme(
            primary = c.accent, onPrimary = Color.White, background = c.bg, onBackground = c.text, surface = c.card, onSurface = c.text,
            surfaceVariant = c.line, onSurfaceVariant = c.muted, surfaceContainerHigh = c.card, surfaceContainerHighest = c.card,
            surfaceContainer = c.card, surfaceContainerLow = c.card, error = c.error, outline = c.line, outlineVariant = c.line, secondaryContainer = c.soft, onSecondaryContainer = c.accent,
        )
    }
    val d = LocalDensity.current
    CompositionLocalProvider(LocalYks provides c, LocalDensity provides Density(d.density, d.fontScale * fontScale)) {
        MaterialTheme(colorScheme = scheme, typography = AppType, shapes = AppShapes, content = content)
    }
}
