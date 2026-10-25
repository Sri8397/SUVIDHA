package com.just_sri.suvidha.expenses

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

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

/** Currency symbol flows down via CompositionLocal – independent of timezone. */
internal val LocalCurrency = compositionLocalOf { "₹" }

/** Active timezone ZoneId flows down via CompositionLocal. */
internal val LocalZoneId = compositionLocalOf { ZoneId.systemDefault() }

/** All currencies the user can pick from. Pair(symbol, label). */
internal val supportedCurrencies = listOf(
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
    "kr" to "kr  Krona / Krone",
)

data class TimezoneOption(
    val id: String,
    val continent: String,
    val city: String,
    val offsetString: String,
    val offsetSeconds: Int
)

private val timezoneContinents = setOf(
    "Africa", "America", "Antarctica", "Asia", "Atlantic", "Australia", "Europe", "Indian", "Pacific"
)

/** Sorted IDs are cheap to retain; rich display options are created only for visible/search-matched rows. */
internal val timezoneIds: List<String> by lazy {
    ZoneId.getAvailableZoneIds()
        .filter { it.substringBefore('/') in timezoneContinents && '/' in it }
        .sortedWith(compareBy<String>({ it.substringBefore('/') }, { it.substringAfter('/').replace('_', ' ') }))
}

internal fun timezoneOption(id: String, now: Instant = Instant.now()): TimezoneOption {
    val offset = ZoneId.of(id).rules.getOffset(now)
    val continent = id.substringBefore('/')
    val city = id.substringAfter('/').replace('_', ' ')
    val offsetString = if (offset.totalSeconds == 0) "UTC" else "UTC${offset.id}"
    return TimezoneOption(id, continent, city, offsetString, offset.totalSeconds)
}
