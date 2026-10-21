package com.just_sri.suvidha.expenses

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

// Shared UI tokens: change these once to restyle every screen.
internal val Ink = Color(0xFF17231E)
internal val Muted = Color(0xFF728078)
internal val Green = Color(0xFF16845B)
internal val Canvas = Color(0xFFF5F7F4)
internal val Danger = Color(0xFFB64C45)

internal fun money(value: Double): String {
    val symbols = DecimalFormatSymbols(Locale("en", "IN"))
    val fmt = DecimalFormat("#,##0.##", symbols)
    return fmt.format(value)
}

/** Currency symbol flows down via CompositionLocal – no need to thread it as a parameter. */
internal val LocalCurrency = compositionLocalOf { "₹" }

/** All currencies the user can pick from. Pair(symbol, label). */
internal val supportedCurrencies = listOf(
    "₹" to "₹  Indian Rupee (INR)",
    "$" to "$  US Dollar (USD)",
    "€" to "€  Euro (EUR)",
    "£" to "£  British Pound (GBP)",
    "¥" to "¥  Japanese Yen (JPY)",
    "₩" to "₩  South Korean Won (KRW)",
    "A$" to "A$  Australian Dollar (AUD)",
    "C$" to "C$  Canadian Dollar (CAD)",
    "CHF" to "CHF  Swiss Franc",
    "AED" to "AED  UAE Dirham",
    "SGD" to "SGD  Singapore Dollar",
)
