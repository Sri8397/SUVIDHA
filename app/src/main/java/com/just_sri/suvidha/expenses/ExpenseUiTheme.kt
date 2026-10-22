package com.just_sri.suvidha.expenses

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

import java.time.ZoneId

// Shared UI tokens: change these once to restyle every screen.
internal val Ink = Color(0xFF17231E)
internal val Muted = Color(0xFF728078)
internal val Green = Color(0xFF16845B)
internal val Canvas = Color(0xFFEFF2EE)
internal val CardSurface = Color(0xFFFAFBF8)
internal val OffWhite = Color(0xFFF4F7F5)
internal val BorderLight = Color(0xFFE2E7E2)
internal val Danger = Color(0xFFB64C45)

private val moneyFmt = ThreadLocal.withInitial {
    DecimalFormat("#,##0.##", DecimalFormatSymbols(Locale("en", "IN")))
}

internal fun money(value: Double): String = moneyFmt.get()!!.format(value)

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

private val currencyZones = mapOf(
    "₹" to ZoneId.of("Asia/Kolkata"),
    "$" to ZoneId.of("America/New_York"),
    "€" to ZoneId.of("Europe/Paris"),
    "£" to ZoneId.of("Europe/London"),
    "¥" to ZoneId.of("Asia/Tokyo"),
    "₩" to ZoneId.of("Asia/Seoul"),
    "A$" to ZoneId.of("Australia/Sydney"),
    "C$" to ZoneId.of("America/Toronto"),
    "CHF" to ZoneId.of("Europe/Zurich"),
    "AED" to ZoneId.of("Asia/Dubai"),
    "SGD" to ZoneId.of("Asia/Singapore")
)

internal fun getZoneIdForCurrency(currency: String): ZoneId =
    currencyZones[currency] ?: ZoneId.systemDefault()

