package com.just_sri.suvidha.expenses

import androidx.compose.foundation.Canvas as DrawCanvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
private fun buildTrendPeriods(mode: TrendMode, today: LocalDate, selectedStart: LocalDate?): List<PeriodSpan> {
    val count = trendPointCount(mode)
    val currentStart = trendPeriodSpan(mode, today).start
    fun window(anchorStart: LocalDate): List<PeriodSpan> =
        (count - 1 downTo 0).map { shiftPeriod(mode, anchorStart, it) }

    val anchoredAtToday = window(currentStart)
    if (selectedStart == null || anchoredAtToday.any { it.start == selectedStart }) return anchoredAtToday
    return window(trendPeriodSpan(mode, selectedStart).start)
}

private fun trendPointLabel(mode: TrendMode, span: PeriodSpan, index: Int): String = when (mode) {
    TrendMode.Day -> span.start.dayOfWeek.name.take(3).lowercase(Locale.getDefault()).replaceFirstChar { it.uppercase(Locale.getDefault()) }
    TrendMode.Week -> "W${index + 1}"
    TrendMode.Month -> span.start.month.name.take(3).lowercase(Locale.getDefault()).replaceFirstChar { it.uppercase(Locale.getDefault()) }
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
    val verb = if (isSpend) "SPENT" else "EARNED"
    if (!hasSelection) return "$verb TODAY"
    return when (mode) {
        TrendMode.Day ->
            if (span.start == today) "$verb TODAY" else "$verb ON ${periodLabel(mode, span).uppercase(Locale.getDefault())}"
        TrendMode.Week ->
            if (span.start == trendPeriodSpan(TrendMode.Week, today).start) "$verb THIS WEEK"
            else "$verb · ${periodLabel(mode, span).uppercase(Locale.getDefault())}"
        TrendMode.Month ->
            if (span.start == trendPeriodSpan(TrendMode.Month, today).start) "$verb THIS MONTH"
            else "$verb IN ${periodLabel(mode, span).uppercase(Locale.getDefault())}"
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
private fun trendOffsets(values: List<Double>, width: Float, height: Float, horizontalPadding: Float, verticalPadding: Float): List<Offset> {
    val left = horizontalPadding
    val right = (width - horizontalPadding).coerceAtLeast(left + 1f)
    val top = verticalPadding
    val bottom = (height - verticalPadding).coerceAtLeast(top + 1f)
    val max = (values.maxOrNull() ?: 0.0).let { if (it <= 0.0) 1.0 else it }
    return values.mapIndexed { index, value ->
        val x = if (values.size == 1) (left + right) / 2f else left + (right - left) * index / (values.size - 1)
        val y = bottom - ((value / max).toFloat() * (bottom - top))
        Offset(x, y)
    }
}

@Composable
internal fun ExpenseTrend(
    expenses: List<Expense>,
    mode: TrendMode,
    anchor: LocalDate?,
    zoneId: ZoneId,
    onModeChange: (TrendMode) -> Unit,
    onSelectPeriod: (LocalDate) -> Unit,
    title: String = "Spending trend",
    emptyText: String = "Your spending trend will appear here."
) {
    val today = remember(zoneId) { LocalDate.now(zoneId) }
    val selectedStart = anchor?.let { trendPeriodSpan(mode, it).start }
    val periods = remember(mode, today, selectedStart) { buildTrendPeriods(mode, today, selectedStart) }
    val values = remember(periods, expenses, zoneId) {
        periods.map { span ->
            val lastDay = if (span.endInclusive.isAfter(today)) today else span.endInclusive
            expenses.filter { expense ->
                expenseDay(expense.date, zoneId) in span.start..lastDay
            }.sumOf { it.amount }
        }
    }
    val selectedIndex = periods.indexOfFirst { it.start == selectedStart }

    Column(Modifier.fillMaxWidth().background(CardSurface, RoundedCornerShape(24.dp)).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TrendMode.values().forEach { choice ->
                val active = mode == choice
                Text(
                    choice.label,
                    color = if (active) OffWhite else Muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .background(if (active) Green else Canvas, RoundedCornerShape(50))
                        .clickable { onModeChange(choice) }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }
        }
        if (expenses.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                Text(emptyText, color = Muted, fontSize = 13.sp)
            }
        } else {
            DrawCanvas(
                Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .padding(top = 14.dp, bottom = 4.dp)
                    .pointerInput(periods, values) {
                        detectTapGestures { tap ->
                            if (values.isEmpty()) return@detectTapGestures
                            val coords = trendOffsets(values, size.width.toFloat(), size.height.toFloat(), 10.dp.toPx(), 10.dp.toPx())
                            val nearest = coords.indices.minByOrNull { index -> kotlin.math.abs(coords[index].x - tap.x) }
                            if (nearest != null) onSelectPeriod(periods[nearest].start)
                        }
                    }
            ) {
                val hPad = 10.dp.toPx()
                val vPad = 10.dp.toPx()
                val left = hPad
                val right = size.width - hPad
                val top = vPad
                val bottom = size.height - vPad
                for (line in 0..2) {
                    val y = top + (bottom - top) * line / 2f
                    drawLine(Color(0xFFE9EEEA), Offset(left, y), Offset(right, y), 1.dp.toPx())
                }
                val coords = trendOffsets(values, size.width, size.height, hPad, vPad)
                if (coords.size > 1) {
                    val path = Path()
                    coords.forEachIndexed { index, point ->
                        if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
                    }
                    drawPath(path, Green, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
                coords.forEachIndexed { index, point ->
                    if (index == selectedIndex) {
                        drawCircle(Green.copy(alpha = 0.16f), radius = 9.dp.toPx(), center = point)
                        drawCircle(Green, radius = 5.5.dp.toPx(), center = point)
                        drawCircle(CardSurface, radius = 2.dp.toPx(), center = point)
                    } else {
                        drawCircle(Green, radius = 4.dp.toPx(), center = point)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                periods.forEachIndexed { index, span ->
                    Text(
                        trendPointLabel(mode, span, index),
                        color = if (index == selectedIndex) Green else Muted,
                        fontSize = 10.sp,
                        fontWeight = if (index == selectedIndex) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

private val monthShortNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

@Composable
internal fun MonthPickerDialog(
    initial: LocalDate,
    earliest: LocalDate,
    latest: LocalDate,
    onPick: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    var year by remember(initial) { mutableStateOf(initial.year) }
    val minYear = minOf(earliest.year, latest.year)
    val maxYear = maxOf(earliest.year, latest.year)
    val earliestMonth = earliest.withDayOfMonth(1)
    val latestMonth = latest.withDayOfMonth(1)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select month", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (year > minYear) Canvas else Canvas.copy(alpha = 0.5f))
                            .clickable(enabled = year > minYear) { year -= 1 },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Previous year",
                            tint = if (year > minYear) Green else Muted.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        year.toString(),
                        color = Ink,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (year < maxYear) Canvas else Canvas.copy(alpha = 0.5f))
                            .clickable(enabled = year < maxYear) { year += 1 },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = "Next year",
                            tint = if (year < maxYear) Green else Muted.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    (0 until 12).chunked(3).forEach { rowMonths ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            rowMonths.forEach { monthIndex ->
                                val monthStart = LocalDate.of(year, monthIndex + 1, 1)
                                val enabled = !monthStart.isBefore(earliestMonth) && !monthStart.isAfter(latestMonth)
                                val active = year == initial.year && monthIndex + 1 == initial.monthValue
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (active) Green else Canvas)
                                        .clickable(enabled = enabled) { onPick(monthStart) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        monthShortNames[monthIndex],
                                        color = when {
                                            active -> OffWhite
                                            enabled -> Ink
                                            else -> Muted.copy(alpha = 0.35f)
                                        },
                                        fontSize = 13.sp,
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Green, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

