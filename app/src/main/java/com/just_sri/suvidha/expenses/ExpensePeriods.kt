package com.just_sri.suvidha.expenses

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

internal enum class TrendMode(val label: String) {
    Day("Day"),
    Week("Week"),
    Month("Month")
}

internal data class PeriodSpan(val start: LocalDate, val endInclusive: LocalDate) {
    operator fun contains(date: LocalDate): Boolean = !date.isBefore(start) && !date.isAfter(endInclusive)
}

private val dayMonthFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
private val dayMonthYearFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
private val monthYearFullFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
private val monthYearShortFormatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault())
private val monthOnlyFormatter = DateTimeFormatter.ofPattern("MMM", Locale.getDefault())

// Keep list filtering and chart buckets on the same timezone conversion.
internal fun expenseDay(timestamp: Long, zoneId: ZoneId): LocalDate =
    Instant.ofEpochMilli(timestamp).atZone(zoneId).toLocalDate()

/** Start/end (inclusive) of the day, week or month that [anchor] falls into. */
internal fun trendPeriodSpan(mode: TrendMode, anchor: LocalDate): PeriodSpan = when (mode) {
    TrendMode.Day -> PeriodSpan(anchor, anchor)
    TrendMode.Week -> {
        val monday = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        PeriodSpan(monday, monday.plusDays(6))
    }
    TrendMode.Month -> {
        val first = anchor.withDayOfMonth(1)
        PeriodSpan(first, first.withDayOfMonth(first.lengthOfMonth()))
    }
}

private fun shiftPeriod(mode: TrendMode, start: LocalDate, offset: Int): PeriodSpan = when (mode) {
    TrendMode.Day -> {
        val shifted = start.minusDays(offset.toLong())
        PeriodSpan(shifted, shifted)
    }
    TrendMode.Week -> {
        val shifted = start.minusWeeks(offset.toLong())
        PeriodSpan(shifted, shifted.plusDays(6))
    }
    TrendMode.Month -> {
        val shifted = start.minusMonths(offset.toLong())
        PeriodSpan(shifted, shifted.withDayOfMonth(shifted.lengthOfMonth()))
    }
}

private fun trendPointCount(mode: TrendMode): Int = when (mode) {
    TrendMode.Day -> 7
    TrendMode.Week -> 4
    TrendMode.Month -> 6
}

/**
 * Builds the chart window (oldest → newest). It normally ends on the current period; when a
 * selection older than the window exists the window slides so the selection stays visible.
 */
internal fun buildTrendPeriods(mode: TrendMode, today: LocalDate, selectedStart: LocalDate?): List<PeriodSpan> {
    val count = trendPointCount(mode)
    val currentStart = trendPeriodSpan(mode, today).start
    fun window(anchorStart: LocalDate): List<PeriodSpan> =
        (count - 1 downTo 0).map { shiftPeriod(mode, anchorStart, it) }

    val anchoredAtToday = window(currentStart)
    if (selectedStart == null || anchoredAtToday.any { it.start == selectedStart }) return anchoredAtToday
    return window(trendPeriodSpan(mode, selectedStart).start)
}

internal fun trendPointLabel(mode: TrendMode, span: PeriodSpan, index: Int): String = when (mode) {
    TrendMode.Day -> span.start.dayOfMonth.toString()
    TrendMode.Week -> "${span.start.dayOfMonth}–${span.endInclusive.dayOfMonth}"
    TrendMode.Month -> span.start.month.name.take(3).lowercase(Locale.getDefault()).replaceFirstChar { it.uppercase(Locale.getDefault()) }
}

internal fun trendRangeText(mode: TrendMode, periods: List<PeriodSpan>): String {
    if (periods.isEmpty()) return ""
    val first: LocalDate = periods.first().start
    val last: LocalDate = periods.last().endInclusive
    return when (mode) {
        TrendMode.Day, TrendMode.Week -> {
            val monthFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM", Locale.getDefault())
            if (first.month == last.month) {
                "${first.dayOfMonth}–${last.dayOfMonth} ${monthFmt.format(last)}"
            } else {
                val dayMonthFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
                "${dayMonthFmt.format(first)}–${dayMonthFmt.format(last)}"
            }
        }
        TrendMode.Month -> {
            val monthFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM", Locale.getDefault())
            "${monthFmt.format(first)}–${monthFmt.format(last)}"
        }
    }
}

internal fun periodLabel(mode: TrendMode, span: PeriodSpan, compact: Boolean = false): String = when (mode) {
    TrendMode.Day -> (if (compact) dayMonthFormatter else dayMonthYearFormatter).format(span.start)
    TrendMode.Month -> (if (compact) monthYearShortFormatter else monthYearFullFormatter).format(span.start)
    TrendMode.Week -> {
        val start = span.start
        val end = span.endInclusive
        when {
            start == end -> (if (compact) dayMonthFormatter else dayMonthYearFormatter).format(start)
            start.month == end.month -> {
                val endPart = if (compact) monthOnlyFormatter.format(end) else monthYearShortFormatter.format(end)
                "${start.dayOfMonth} – ${end.dayOfMonth} $endPart"
            }
            else -> "${dayMonthFormatter.format(start)} – " + (if (compact) dayMonthFormatter else dayMonthYearFormatter).format(end)
        }
    }
}

internal fun periodScope(mode: TrendMode, span: PeriodSpan, today: LocalDate, hasSelection: Boolean): String {
    if (!hasSelection) return "today"
    return when (mode) {
        TrendMode.Day -> if (span.start == today) "today" else "on this day"
        TrendMode.Week -> if (span.start == trendPeriodSpan(TrendMode.Week, today).start) "this week" else "that week"
        TrendMode.Month -> if (span.start == trendPeriodSpan(TrendMode.Month, today).start) "this month" else "in that month"
    }
}

/** Anchor that lands on the current period for [mode]; a day jumps back to the plain "today" default. */
internal fun currentPeriodAnchor(mode: TrendMode, today: LocalDate): LocalDate? = when (mode) {
    TrendMode.Day -> null
    TrendMode.Week, TrendMode.Month -> today
}

internal fun heroEyebrow(isSpend: Boolean, mode: TrendMode, span: PeriodSpan, today: LocalDate, hasSelection: Boolean): String {
    val verb: String = if (isSpend) "SPENT" else "EARNED"
    if (!hasSelection) return "$verb TODAY"
    return when (mode) {
        TrendMode.Day -> if (span.start == today) "$verb TODAY" else "$verb · ${periodLabel(mode, span, compact = true).uppercase(Locale.getDefault())}"
        TrendMode.Week -> if (span.start == trendPeriodSpan(TrendMode.Week, today).start) "$verb THIS WEEK" else "$verb · ${periodLabel(mode, span, compact = true).uppercase(Locale.getDefault())}"
        TrendMode.Month -> if (span.start == trendPeriodSpan(TrendMode.Month, today).start) "$verb THIS MONTH" else "$verb · ${periodLabel(mode, span, compact = true).uppercase(Locale.getDefault())}"
    }
}

internal fun heroSubtitle(count: Int, isSpend: Boolean, mode: TrendMode, span: PeriodSpan, today: LocalDate, hasSelection: Boolean): String {
    val noun = if (isSpend) "expense" else "earning"
    val scope = periodScope(mode, span, today, hasSelection)
    return when (count) {
        0 -> "No ${noun}s $scope"
        1 -> "1 $noun $scope"
        else -> "$count ${noun}s $scope"
    }
}

/** Pixel positions used by both the drawing code and the tap handler so hits match the dots. */
