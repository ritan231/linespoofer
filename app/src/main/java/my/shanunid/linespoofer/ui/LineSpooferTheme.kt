package my.shanunid.linespoofer.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.LocaleList
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.core.view.WindowCompat
import my.shanunid.linespoofer.data.AppSettingsStore

private val MiuiDarkColorScheme: ColorScheme = darkColorScheme(
    primary = MiuiXColors.Blue,
    onPrimary = Color.White,
    primaryContainer = MiuiXColors.BlueContainer,
    onPrimaryContainer = MiuiXColors.BlueLight,
    secondary = MiuiXColors.BlueLight,
    onSecondary = Color.White,
    background = MiuiXColors.DarkBg,
    onBackground = MiuiXColors.TextPrimary,
    surface = MiuiXColors.DarkCard,
    onSurface = MiuiXColors.TextPrimary,
    surfaceVariant = MiuiXColors.DarkCardElevated,
    onSurfaceVariant = MiuiXColors.TextSecondary,
    outline = MiuiXColors.DarkBorder,
    outlineVariant = MiuiXColors.DarkDivider,
    surfaceContainer = MiuiXColors.DarkCard,
    surfaceContainerLow = MiuiXColors.DarkBg,
    surfaceContainerHigh = MiuiXColors.DarkCardElevated,
    surfaceContainerHighest = MiuiXColors.DarkCardElevated,
)

private val MiuiLightColorScheme: ColorScheme = lightColorScheme(
    primary = MiuiXColors.Blue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5F1FF),
    onPrimaryContainer = MiuiXColors.BlueDark,
    secondary = MiuiXColors.BlueDark,
    onSecondary = Color.White,
    background = MiuiXColors.LightBg,
    onBackground = MiuiXColors.LightTextPrimary,
    surface = MiuiXColors.LightCard,
    onSurface = MiuiXColors.LightTextPrimary,
    surfaceVariant = Color(0xFFF0F2F6),
    onSurfaceVariant = MiuiXColors.LightTextSecondary,
    outline = MiuiXColors.LightBorder,
    outlineVariant = Color(0xFFE5E7EB),
    surfaceContainer = MiuiXColors.LightCard,
    surfaceContainerLow = MiuiXColors.LightBg,
    surfaceContainerHigh = Color(0xFFF9FAFB),
    surfaceContainerHighest = Color(0xFFFFFFFF),
)

@Composable
fun LineSpooferTheme(
    themeMode: String? = null,
    languageMode: String? = null,
    useSystemColor: Boolean? = null,
    colorStyle: String? = null,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val systemConfiguration = LocalConfiguration.current
    val selectedThemeMode = themeMode ?: AppSettingsStore.getThemeMode(context)
    val selectedLanguageMode = languageMode ?: AppSettingsStore.getLanguageMode(context)
    val systemIsDark = isSystemInDarkTheme()
    val isDark = when (selectedThemeMode) {
        AppSettingsStore.THEME_LIGHT -> false
        AppSettingsStore.THEME_DARK -> true
        else -> systemIsDark
    }

    val colorScheme = if (isDark) MiuiDarkColorScheme else MiuiLightColorScheme

    val localizedConfiguration = remember(systemConfiguration, selectedLanguageMode) {
        Configuration(systemConfiguration).apply {
            if (selectedLanguageMode != AppSettingsStore.LANGUAGE_DEFAULT) {
                val locales = LocaleList.forLanguageTags(selectedLanguageMode)
                setLocales(locales)
                setLayoutDirection(locales[0])
            }
        }
    }
    val localizedContext = remember(context, localizedConfiguration) {
        val configurationContext = context.createConfigurationContext(localizedConfiguration)
        object : ContextWrapper(context) {
            override fun getResources() = configurationContext.resources
        }
    }

    SideEffect {
        context.findActivity()?.let { activity ->
            WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
                isAppearanceLightStatusBars = !isDark
                isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    val miuiColors = if (isDark) MiuiDarkColors else MiuiLightColors

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedConfiguration,
        LocalResources provides localizedContext.resources,
        LocalMiuiColors provides miuiColors,
    ) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = MotionScheme.expressive(),
            content = content,
        )
    }
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
