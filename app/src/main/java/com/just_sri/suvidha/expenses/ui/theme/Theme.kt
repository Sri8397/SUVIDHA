package com.just_sri.suvidha.expenses.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.just_sri.suvidha.expenses.ui.Inter

enum class ThemePreference(val title: String, val subtitle: String) {
    SYSTEM("System default", "Follow device appearance"),
    LIGHT("Light", "Always use light theme"),
    DARK("Dark", "Always use dark theme");

    companion object {
        fun from(value: String?): ThemePreference = when (value?.uppercase()) {
            "LIGHT" -> LIGHT
            "DARK" -> DARK
            else -> SYSTEM
        }
    }
}

typealias ThemeConfig = ThemePreference

private val LightColorScheme = lightColorScheme(
    primary = GreenLight,
    onPrimary = OnGreenLight,
    primaryContainer = MintLight,
    onPrimaryContainer = GreenLight,
    background = CanvasLight,
    onBackground = InkLight,
    surface = CardSurfaceLight,
    onSurface = InkLight,
    surfaceVariant = SurfaceMutedLight,
    onSurfaceVariant = MutedLight,
    surfaceContainer = CardSurfaceLight,
    surfaceContainerLow = CardSurfaceLight,
    surfaceContainerHigh = CardSurfaceLight,
    surfaceContainerHighest = CardSurfaceLight,
    outlineVariant = BorderLightLight,
    outline = MutedLight,
    error = DangerLight,
    onError = Color.White,
)

private val DarkColorScheme = darkColorScheme(
    primary = GreenDark,
    onPrimary = OnGreenDark,
    primaryContainer = MintDark,
    onPrimaryContainer = GreenDark,
    background = CanvasDark,
    onBackground = InkDark,
    surface = CardSurfaceDark,
    onSurface = InkDark,
    surfaceVariant = SurfaceMutedDark,
    onSurfaceVariant = MutedDark,
    surfaceContainer = CardSurfaceDark,
    surfaceContainerLow = CardSurfaceDark,
    surfaceContainerHigh = CardSurfaceDark,
    surfaceContainerHighest = SurfaceHoverDark,
    outlineVariant = BorderLightDark,
    outline = MutedDark,
    error = DangerDark,
    onError = Color(0xFF0F1011),
)

@Composable
fun SuvidhaTheme(
    themePref: ThemePreference = ThemePreference.SYSTEM,
    darkTheme: Boolean = when (themePref) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    },
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkSuvidhaColors else LightSuvidhaColors
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalSuvidhaColors provides colors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography(
                bodyLarge = TextStyle(fontFamily = Inter, fontSize = 15.sp, lineHeight = 22.sp),
                bodyMedium = TextStyle(fontFamily = Inter, fontSize = 14.sp, lineHeight = 20.sp),
                labelLarge = TextStyle(fontFamily = Inter, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            ),
            content = content
        )
    }
}

