package com.just_sri.suvidha.expenses

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.just_sri.suvidha.expenses.ui.theme.LocalSuvidhaColors
import com.just_sri.suvidha.expenses.ui.theme.ThemePreference
import com.just_sri.suvidha.expenses.ui.theme.ThemeConfig
import com.just_sri.suvidha.expenses.constants.LocalCurrency as _LocalCurrency
import com.just_sri.suvidha.expenses.constants.LocalZoneId as _LocalZoneId
import com.just_sri.suvidha.expenses.constants.TimezoneOption as _TimezoneOption
import com.just_sri.suvidha.expenses.constants.money as _money
import com.just_sri.suvidha.expenses.constants.supportedCurrencies as _supportedCurrencies
import com.just_sri.suvidha.expenses.constants.timezoneIds as _timezoneIds
import com.just_sri.suvidha.expenses.constants.timezoneOption as _timezoneOption
import java.time.Instant
import java.time.ZoneId

/**
 * Dynamic theme tokens matching LocalSuvidhaColors for dark and light modes.
 */
internal val Ink: Color @Composable @ReadOnlyComposable get() = LocalSuvidhaColors.current.ink
internal val Muted: Color @Composable @ReadOnlyComposable get() = LocalSuvidhaColors.current.muted
internal val Green: Color @Composable @ReadOnlyComposable get() = LocalSuvidhaColors.current.green
internal val OnGreen: Color @Composable @ReadOnlyComposable get() = LocalSuvidhaColors.current.onGreen
internal val Canvas: Color @Composable @ReadOnlyComposable get() = LocalSuvidhaColors.current.canvas
internal val CardSurface: Color @Composable @ReadOnlyComposable get() = LocalSuvidhaColors.current.cardSurface
internal val OffWhite: Color @Composable @ReadOnlyComposable get() = LocalSuvidhaColors.current.cardSurface
internal val BorderLight: Color @Composable @ReadOnlyComposable get() = LocalSuvidhaColors.current.borderLight
internal val Danger: Color @Composable @ReadOnlyComposable get() = LocalSuvidhaColors.current.danger

internal fun money(value: Double): String = _money(value)

internal val LocalCurrency: ProvidableCompositionLocal<String> get() = _LocalCurrency
internal val LocalZoneId: ProvidableCompositionLocal<ZoneId> get() = _LocalZoneId
internal val supportedCurrencies: List<Pair<String, String>> get() = _supportedCurrencies
internal fun currencySymbol(currencyCodeOrSymbol: String): String = com.just_sri.suvidha.expenses.constants.currencySymbol(currencyCodeOrSymbol)
internal fun currencyCodeFor(symbolOrCode: String): String = com.just_sri.suvidha.expenses.constants.currencyCodeFor(symbolOrCode)

internal typealias TimezoneOption = _TimezoneOption
internal val timezoneIds: List<String> get() = _timezoneIds
internal fun timezoneOption(id: String, now: Instant = Instant.now()): TimezoneOption = _timezoneOption(id, now)
