package com.just_sri.suvidha.expenses

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ExpensePeriodsTest {
    private val today: LocalDate = LocalDate.of(2026, 10, 31)
    private val pickedDate: LocalDate = LocalDate.of(2025, 12, 1)

    @Test
    fun buildTrendPeriodsForDayEndsOnPickedDateAndIncludesPreviousDays() {
        val periods: List<PeriodSpan> = buildTrendPeriods(TrendMode.Day, today, pickedDate)
        assertEquals(7, periods.size)
        assertEquals(LocalDate.of(2025, 11, 25), periods.first().start)
        assertEquals(LocalDate.of(2025, 12, 1), periods.last().endInclusive)
    }

    @Test
    fun buildTrendPeriodsForWeekEndsOnWeekOfPickedDateAndIncludesPreviousWeeks() {
        val periods: List<PeriodSpan> = buildTrendPeriods(TrendMode.Week, today, pickedDate)
        assertEquals(4, periods.size)
        assertEquals(LocalDate.of(2025, 11, 10), periods.first().start)
        assertEquals(LocalDate.of(2025, 12, 7), periods.last().endInclusive)
    }

    @Test
    fun buildTrendPeriodsForMonthEndsOnMonthOfPickedDateAndIncludesPreviousMonths() {
        val periods: List<PeriodSpan> = buildTrendPeriods(TrendMode.Month, today, pickedDate)
        assertEquals(6, periods.size)
        assertEquals(LocalDate.of(2025, 7, 1), periods.first().start)
        assertEquals(LocalDate.of(2025, 12, 31), periods.last().endInclusive)
    }

    @Test
    fun buildTrendPeriodsWithoutAnchorUsesToday() {
        val dayPeriods: List<PeriodSpan> = buildTrendPeriods(TrendMode.Day, today, null)
        val weekPeriods: List<PeriodSpan> = buildTrendPeriods(TrendMode.Week, today, null)
        val monthPeriods: List<PeriodSpan> = buildTrendPeriods(TrendMode.Month, today, null)
        assertEquals(today, dayPeriods.last().endInclusive)
        assertEquals(LocalDate.of(2026, 11, 1), weekPeriods.last().endInclusive)
        assertEquals(LocalDate.of(2026, 10, 31), monthPeriods.last().endInclusive)
    }
}

