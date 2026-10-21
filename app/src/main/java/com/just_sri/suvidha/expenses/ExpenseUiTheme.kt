package com.just_sri.suvidha.expenses

import androidx.compose.ui.graphics.Color
import java.util.Locale

// Shared UI tokens: change these once to restyle every screen.
internal val Ink = Color(0xFF17231E)
internal val Muted = Color(0xFF728078)
internal val Green = Color(0xFF16845B)
internal val Canvas = Color(0xFFF5F7F4)
internal val Danger = Color(0xFFB64C45)

internal fun money(value: Double): String = String.format(Locale("en", "IN"), "%,.0f", value)
