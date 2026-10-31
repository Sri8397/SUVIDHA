package com.just_sri.suvidha.expenses.constants

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/**
 * Centralized constants, tokens, and formats for the SUVIDHA app.
 */

// ---------------------------------------------------------------------------
// 1. Color Tokens
// ---------------------------------------------------------------------------
val Ink = Color(0xFF1A211B)
val Muted = Color(0xFF5E6B61)
val Green = Color(0xFF1E5E3A)
val Canvas = Color(0xFFF5F7F3)
val CardSurface = Color(0xFFF7F9F5)
val OffWhite = Color(0xFFF4F7F5)
val BorderLight = Color(0xFFE8EDE5)
val Danger = Color(0xFFB64C45)

// ---------------------------------------------------------------------------
// 2. Categories
// ---------------------------------------------------------------------------
val defaultExpenseCategories = listOf(
    "Rent", "Utilities", "Food", "Transport", "Health", "Shopping", "Salary", "Earn", "Other"
)

// ---------------------------------------------------------------------------
// 3. Currency Configuration
// ---------------------------------------------------------------------------
val LocalCurrency = compositionLocalOf { "₹" }

val supportedCurrencies = listOf(
    "₹" to "₹  Rupee",
    "$" to "$  Dollar",
    "€" to "€  Euro",
    "£" to "£  Pound",
    "¥" to "¥  Yen / Yuan",
    "₩" to "₩  Won",
    "CHF" to "CHF  Franc",
    "AED" to "AED  Dirham",
    "฿" to "฿  Baht",
    "₱" to "₱  Peso",
    "₫" to "₫  Dong",
    "kr" to "kr  Krona / Krone"
)

val currencyCodeToSymbol = mapOf(
    "INR" to "₹",
    "USD" to "$",
    "EUR" to "€",
    "GBP" to "£",
    "JPY" to "¥",
    "KRW" to "₩",
    "CHF" to "CHF",
    "AED" to "AED",
    "THB" to "฿",
    "PHP" to "₱",
    "VND" to "₫",
    "SEK" to "kr",
)

val symbolToCurrencyCode = mapOf(
    "₹" to "INR",
    "$" to "USD",
    "€" to "EUR",
    "£" to "GBP",
    "¥" to "JPY",
    "₩" to "KRW",
    "CHF" to "CHF",
    "AED" to "AED",
    "฿" to "THB",
    "₱" to "PHP",
    "₫" to "VND",
    "kr" to "SEK",
)

fun currencySymbol(currencyCodeOrSymbol: String): String {
    symbolToCurrencyCode[currencyCodeOrSymbol]?.let { return currencyCodeOrSymbol }
    currencyCodeToSymbol[currencyCodeOrSymbol]?.let { return it }
    return runCatching { java.util.Currency.getInstance(currencyCodeOrSymbol).symbol }.getOrDefault(currencyCodeOrSymbol)
}

fun currencyCodeFor(symbolOrCode: String): String {
    symbolToCurrencyCode[symbolOrCode]?.let { return it }
    if (symbolOrCode.matches(Regex("[A-Z]{3}"))) return symbolOrCode
    return "INR"
}

// ---------------------------------------------------------------------------
// 4. Timezone Configuration
// ---------------------------------------------------------------------------
val LocalZoneId = compositionLocalOf { ZoneId.systemDefault() }

data class TimezoneOption(
    val id: String,
    val continent: String,
    val city: String,
    val offsetString: String
)

private val timezoneContinents = setOf(
    "Africa", "America", "Antarctica", "Asia", "Atlantic", "Australia", "Europe", "Indian", "Pacific"
)

val timezoneIds: List<String> by lazy {
    ZoneId.getAvailableZoneIds()
        .filter { it.substringBefore('/') in timezoneContinents && '/' in it }
        .sortedWith(compareBy<String>({ it.substringBefore('/') }, { it.substringAfter('/').replace('_', ' ') }))
}

fun timezoneOption(id: String, now: Instant = Instant.now()): TimezoneOption {
    val offset = ZoneId.of(id).rules.getOffset(now)
    val continent = id.substringBefore('/')
    val city = id.substringAfter('/').replace('_', ' ')
    val offsetString = if (offset.totalSeconds == 0) "UTC" else "UTC${offset.id}"
    return TimezoneOption(id, continent, city, offsetString)
}

// ---------------------------------------------------------------------------
// 5. Money Formatting
// ---------------------------------------------------------------------------
private val moneyFmt = ThreadLocal.withInitial {
    DecimalFormat("#,##0.##", DecimalFormatSymbols(Locale.Builder().setLanguage("en").setRegion("IN").build()))
}

fun money(value: Double): String = moneyFmt.get()!!.format(value)
