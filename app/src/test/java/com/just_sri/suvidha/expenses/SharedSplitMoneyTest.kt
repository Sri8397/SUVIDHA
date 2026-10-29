package com.just_sri.suvidha.expenses

import org.junit.Assert.*
import org.junit.Test

class SharedSplitMoneyTest {
    @Test
    fun parsesCurrencyPrecisionWithoutFloatingPointOrSilentRounding() {
        assertEquals(10L, SharedSplitMoney.parseMinor("0.10", "INR"))
        assertEquals(1L, SharedSplitMoney.parseMinor("0.01", "INR"))
        assertNull(SharedSplitMoney.parseMinor("0.001", "INR"))
        assertEquals(123L, SharedSplitMoney.parseMinor("123", "JPY"))
        assertNull(SharedSplitMoney.parseMinor("1.5", "JPY"))
        assertEquals(1234L, SharedSplitMoney.parseMinor("1.234", "KWD"))
        assertEquals(12345L, SharedSplitMoney.parseMinor("1.2345", "CLF"))
        assertNull(SharedSplitMoney.parseMinor("-1.00", "INR"))
        assertNull(SharedSplitMoney.parseMinor("1e2", "INR"))
        assertNull(SharedSplitMoney.parseMinor("9999999999999999999999", "INR"))
    }

    @Test
    fun equalSplitPreservesEveryPaiseAndDoesNotDependOnMemberDisplayOrder() {
        val weights = linkedMapOf("c" to 1, "a" to 1, "b" to 1)
        assertEquals(mapOf("a" to 34L, "b" to 33L, "c" to 33L), SharedSplitMoney.allocate(100, weights))
        assertEquals(SharedSplitMoney.allocate(100, weights), SharedSplitMoney.allocate(100, weights.entries.reversed().associate { it.toPair() }))
        assertEquals(mapOf("a" to 1L, "b" to 0L), SharedSplitMoney.allocate(1, mapOf("a" to 1, "b" to 1)))
    }

    @Test
    fun weightedSharesUseLargestRemainderAndPreserveTheTotal() {
        val allocated = SharedSplitMoney.allocate(1001, mapOf("a" to 2, "b" to 1, "c" to 1))
        assertEquals(mapOf("a" to 501L, "b" to 250L, "c" to 250L), allocated)
        assertEquals(1001L, allocated.values.sum())
    }

    @Test
    fun multiplicationCannotOverflowForLargeValidAmounts() {
        val allocated = SharedSplitMoney.allocate(Long.MAX_VALUE, mapOf("a" to 9999, "b" to 10_000))
        assertEquals(Long.MAX_VALUE, allocated.values.fold(0L) { sum, amount -> Math.addExact(sum, amount) })
        assertTrue(allocated.values.all { it > 0 })
        assertThrows(IllegalArgumentException::class.java) { SharedSplitMoney.allocate(100, mapOf("a" to 0)) }
    }
}
