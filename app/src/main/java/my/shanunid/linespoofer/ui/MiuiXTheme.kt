package my.shanunid.linespoofer.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Xiaomi HyperOS / MIUIX Design System Palette.
 * Full dual-theme architecture:
 * - Obsidian Dark (#0C0D10 / #171920)
 * - Clean Light (#F4F5F8 / #FFFFFF)
 * with signature HyperOS Blue (#007DFF) accents.
 */
object MiuiXColors {
    // HyperOS / MIUI Official Primary Blue
    val Blue = Color(0xFF007DFF)
    val Primary = Blue
    val BlueDark = Color(0xFF0066D6)
    val BlueLight = Color(0xFF3397FF)
    val BlueContainer = Color(0x1F007DFF)
    val BlueGlow = Color(0x35007DFF)

    // Dark Mode Surfaces (HyperOS Obsidian Palette)
    val DarkBg = Color(0xFF0C0D10)
    val DarkCard = Color(0xFF171920)
    val DarkCardElevated = Color(0xFF20232C)
    val DarkBorder = Color(0xFF262934)
    val Border = DarkBorder
    val DarkDivider = Color(0xFF1E212A)

    // Dark Mode Text
    val TextPrimary = Color(0xFFF7F8FA)
    val TextSecondary = Color(0xFF8A909F)
    val TextTertiary = Color(0xFF5D6373)

    // Light Mode Surfaces
    val LightBg = Color(0xFFF4F5F8)
    val LightCard = Color(0xFFFFFFFF)
    val LightCardElevated = Color(0xFFF9FAFB)
    val LightBorder = Color(0xFFE5E7EB)
    val LightDivider = Color(0xFFEEEEEE)
    val LightTextPrimary = Color(0xFF121418)
    val LightTextSecondary = Color(0xFF6B7280)
    val LightTextTertiary = Color(0xFF9CA3AF)

    // Status Accents
    val Green = Color(0xFF00C853)
    val GreenContainer = Color(0x1F00C853)
    val Orange = Color(0xFFFF9100)
    val Red = Color(0xFFFF3B30)
}

data class MiuiColors(
    val isDark: Boolean,
    val primary: Color = MiuiXColors.Blue,
    val background: Color,
    val card: Color,
    val cardElevated: Color,
    val border: Color,
    val divider: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val navBarBg: Color,
    val navBarBorder: Color,
    val pillBackground: Color,
    val iconBadgeBg: Color,
    val sliderInactiveTrack: Color,
    val switchInactiveTrack: Color,
    val switchInactiveBorder: Color,
)

val MiuiDarkColors = MiuiColors(
    isDark = true,
    primary = MiuiXColors.Blue,
    background = MiuiXColors.DarkBg,
    card = MiuiXColors.DarkCard,
    cardElevated = MiuiXColors.DarkCardElevated,
    border = MiuiXColors.DarkBorder,
    divider = MiuiXColors.DarkDivider,
    textPrimary = MiuiXColors.TextPrimary,
    textSecondary = MiuiXColors.TextSecondary,
    textTertiary = MiuiXColors.TextTertiary,
    navBarBg = Color(0xFF111318),
    navBarBorder = MiuiXColors.DarkBorder,
    pillBackground = Color(0xFF1E212B),
    iconBadgeBg = Color(0xFF222631),
    sliderInactiveTrack = Color(0xFF2A2E3B),
    switchInactiveTrack = Color(0xFF232732),
    switchInactiveBorder = Color(0xFF383E4E),
)

val MiuiLightColors = MiuiColors(
    isDark = false,
    primary = MiuiXColors.Blue,
    background = MiuiXColors.LightBg,
    card = MiuiXColors.LightCard,
    cardElevated = MiuiXColors.LightCardElevated,
    border = MiuiXColors.LightBorder,
    divider = MiuiXColors.LightDivider,
    textPrimary = MiuiXColors.LightTextPrimary,
    textSecondary = MiuiXColors.LightTextSecondary,
    textTertiary = MiuiXColors.LightTextTertiary,
    navBarBg = Color(0xFFFFFFFF),
    navBarBorder = MiuiXColors.LightBorder,
    pillBackground = Color(0xFFECEFF4),
    iconBadgeBg = Color(0xFFF0F2F6),
    sliderInactiveTrack = Color(0xFFE0E3EB),
    switchInactiveTrack = Color(0xFFD1D5DB),
    switchInactiveBorder = Color(0xFFBBC1CB),
)

val LocalMiuiColors = staticCompositionLocalOf { MiuiDarkColors }

object MiuiTheme {
    val colors: MiuiColors
        @Composable
        get() = LocalMiuiColors.current
}
