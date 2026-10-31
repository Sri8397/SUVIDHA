package com.just_sri.suvidha.expenses.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Light palette tokens
val CanvasLight: Color = Color(0xFFF5F7F3)
val CardSurfaceLight: Color = Color(0xFFF7F9F5)
val SurfaceMutedLight: Color = Color(0xFFEFF3EC)
val SurfaceHoverLight: Color = Color(0xFFEDF3E9)
val InkLight: Color = Color(0xFF1A211B)
val MutedLight: Color = Color(0xFF5E6B61)
val GreenLight: Color = Color(0xFF1E5E3A)
val OnGreenLight: Color = Color(0xFFFFFFFF)
val MintLight: Color = Color(0xFFE5F2EA)
val BorderLightLight: Color = Color(0xFFE8EDE5)
val DangerLight: Color = Color(0xFFB64C45)

// Neutral Dark palette tokens
val CanvasDark: Color = Color(0xFF0F1011)
val CardSurfaceDark: Color = Color(0xFF18191B)
val SurfaceMutedDark: Color = Color(0xFF212326)
val SurfaceHoverDark: Color = Color(0xFF292B2F)
val InkDark: Color = Color(0xFFE3E5E8)
val MutedDark: Color = Color(0xFF8C9199)
val GreenDark: Color = Color(0xFF3ECF8E)
// Dark text on the bright dark-mode green; white there glares and has poor contrast.
val OnGreenDark: Color = Color(0xFF0F1011)
val MintDark: Color = Color(0xFF222428)
val BorderLightDark: Color = Color(0xFF26282C)
val DangerDark: Color = Color(0xFFEA5E55)

@Immutable
data class SuvidhaColors(
    val canvas: Color,
    val cardSurface: Color,
    val surfaceMuted: Color,
    val surfaceHover: Color,
    val ink: Color,
    val muted: Color,
    val green: Color,
    val onGreen: Color,
    val mint: Color,
    val borderLight: Color,
    val danger: Color,
    val isDark: Boolean,
)

val LightSuvidhaColors: SuvidhaColors = SuvidhaColors(
    canvas = CanvasLight,
    cardSurface = CardSurfaceLight,
    surfaceMuted = SurfaceMutedLight,
    surfaceHover = SurfaceHoverLight,
    ink = InkLight,
    muted = MutedLight,
    green = GreenLight,
    onGreen = OnGreenLight,
    mint = MintLight,
    borderLight = BorderLightLight,
    danger = DangerLight,
    isDark = false,
)

val DarkSuvidhaColors: SuvidhaColors = SuvidhaColors(
    canvas = CanvasDark,
    cardSurface = CardSurfaceDark,
    surfaceMuted = SurfaceMutedDark,
    surfaceHover = SurfaceHoverDark,
    ink = InkDark,
    muted = MutedDark,
    green = GreenDark,
    onGreen = OnGreenDark,
    mint = MintDark,
    borderLight = BorderLightDark,
    danger = DangerDark,
    isDark = true,
)

val LocalSuvidhaColors = staticCompositionLocalOf { LightSuvidhaColors }

