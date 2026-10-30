package com.just_sri.suvidha.expenses

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.ui.graphics.Color
import com.just_sri.suvidha.expenses.constants.BorderLight as _BorderLight
import com.just_sri.suvidha.expenses.constants.Canvas as _Canvas
import com.just_sri.suvidha.expenses.constants.CardSurface as _CardSurface
import com.just_sri.suvidha.expenses.constants.Danger as _Danger
import com.just_sri.suvidha.expenses.constants.Green as _Green
import com.just_sri.suvidha.expenses.constants.Ink as _Ink
import com.just_sri.suvidha.expenses.constants.LocalCurrency as _LocalCurrency
import com.just_sri.suvidha.expenses.constants.LocalZoneId as _LocalZoneId
import com.just_sri.suvidha.expenses.constants.Muted as _Muted
import com.just_sri.suvidha.expenses.constants.OffWhite as _OffWhite
import com.just_sri.suvidha.expenses.constants.TimezoneOption as _TimezoneOption
import com.just_sri.suvidha.expenses.constants.money as _money
import com.just_sri.suvidha.expenses.constants.supportedCurrencies as _supportedCurrencies
import com.just_sri.suvidha.expenses.constants.timezoneIds as _timezoneIds
import com.just_sri.suvidha.expenses.constants.timezoneOption as _timezoneOption
import java.time.Instant
import java.time.ZoneId

/**
 * Re-exports from [com.just_sri.suvidha.expenses.constants] for backwards compatibility.
 */
internal val Ink: Color get() = _Ink
internal val Muted: Color get() = _Muted
internal val Green: Color get() = _Green
internal val Canvas: Color get() = _Canvas
internal val CardSurface: Color get() = _CardSurface
internal val OffWhite: Color get() = _OffWhite
internal val BorderLight: Color get() = _BorderLight
internal val Danger: Color get() = _Danger

internal fun money(value: Double): String = _money(value)

internal val LocalCurrency: ProvidableCompositionLocal<String> get() = _LocalCurrency
internal val LocalZoneId: ProvidableCompositionLocal<ZoneId> get() = _LocalZoneId
internal val supportedCurrencies: List<Pair<String, String>> get() = _supportedCurrencies
internal fun currencySymbol(currencyCodeOrSymbol: String): String = com.just_sri.suvidha.expenses.constants.currencySymbol(currencyCodeOrSymbol)
internal fun currencyCodeFor(symbolOrCode: String): String = com.just_sri.suvidha.expenses.constants.currencyCodeFor(symbolOrCode)

internal typealias TimezoneOption = _TimezoneOption
internal val timezoneIds: List<String> get() = _timezoneIds
internal fun timezoneOption(id: String, now: Instant = Instant.now()): TimezoneOption = _timezoneOption(id, now)
