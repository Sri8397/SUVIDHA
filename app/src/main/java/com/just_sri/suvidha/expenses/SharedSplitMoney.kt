package com.just_sri.suvidha.expenses

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.util.Currency

internal data class SharedExpenseParticipant(val memberId: String, val name: String)
internal data class SharedExpenseDraft(val note: String, val amountsMinorByMemberId: Map<String, Long>)

/** Currency parsing and deterministic allocation shared by the group flow and its checks. */
internal object SharedSplitMoney {
    fun digits(currencyCode: String): Int = Currency.getInstance(currencyCode).defaultFractionDigits.also {
        require(it in 0..9) { "This currency is not supported for shared splits." }
    }

    fun parseMinor(input: String, currencyCode: String): Long? = runCatching {
        val text = input.trim()
        require(text.length in 1..32 && text.matches(Regex("[0-9]+(?:\\.[0-9]*)?")))
        BigDecimal(text).movePointRight(digits(currencyCode)).setScale(0, RoundingMode.UNNECESSARY).longValueExact().also { require(it >= 0) }
    }.getOrNull()

    fun format(amountMinor: Long, currencyCode: String): String = BigDecimal.valueOf(amountMinor, digits(currencyCode)).toPlainString()

    /** Largest remainder allocation; ties use identity order so input/display order cannot change paise. */
    fun allocate(totalMinor: Long, weights: Map<String, Int>): Map<String, Long> {
        require(totalMinor > 0 && weights.isNotEmpty() && weights.size <= 200)
        require(weights.keys.all { it.isNotBlank() } && weights.values.all { it in 1..10_000 })
        val denominator = weights.values.sumOf { it.toLong() }.toBigInteger()
        val parts = weights.toSortedMap().map { (id, weight) ->
            val division = BigInteger.valueOf(totalMinor).multiply(weight.toBigInteger()).divideAndRemainder(denominator)
            Triple(id, division[0].longValueExact(), division[1])
        }
        val remainder = totalMinor - parts.sumOf { it.second }
        val extra = parts.sortedWith(compareByDescending<Triple<String, Long, BigInteger>> { it.third }.thenBy { it.first })
            .take(remainder.toInt()).mapTo(mutableSetOf()) { it.first }
        return parts.associate { it.first to (it.second + if (it.first in extra) 1L else 0L) }
    }
}
