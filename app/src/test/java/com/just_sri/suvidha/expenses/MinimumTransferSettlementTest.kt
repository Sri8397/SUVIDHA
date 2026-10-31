package com.just_sri.suvidha.expenses

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class MinimumTransferSettlementTest {
    @Test
    fun emptyOrZeroBalancesNeedNoTransfers() {
        // Arrange
        val inputBalances: Map<String, Long> = mapOf("a" to 0L, "b" to 0L)

        // Act
        val actualPlan: MinimumTransferPlan = MinimumTransferSettlement.planTransfers(inputBalances)

        // Assert
        assertEquals(emptyList<SettlementTransfer>(), actualPlan.transfers)
        assertTrue(actualPlan.isOptimal)
    }

    @Test
    fun twoMembersNeedOneTransferOfTheExactAmount() {
        // Arrange
        val inputBalances: Map<String, Long> = mapOf("ravi" to -50_000L, "priya" to 50_000L)
        val expectedTransfers: List<SettlementTransfer> = listOf(SettlementTransfer("ravi", "priya", 50_000L))

        // Act
        val actualPlan: MinimumTransferPlan = MinimumTransferSettlement.planTransfers(inputBalances)

        // Assert
        assertEquals(expectedTransfers, actualPlan.transfers)
    }

    @Test
    fun findsFewerTransfersThanGreedyMatchingWhenASubsetClearsItself() {
        // Arrange
        // Greedy largest-first matching needs 4 payments here; {b, c} and {a, d, e} are zero-sum, so 3 suffice.
        val inputBalances: Map<String, Long> = mapOf(
            "a" to 300L,
            "b" to 200L,
            "c" to -200L,
            "d" to -100L,
            "e" to -200L,
        )

        // Act
        val actualPlan: MinimumTransferPlan = MinimumTransferSettlement.planTransfers(inputBalances)

        // Assert
        assertEquals(3, actualPlan.transfers.size)
        assertTrue(actualPlan.isOptimal)
        assertEquals(emptyMap<String, Long>(), residualAfter(inputBalances, actualPlan.transfers))
    }

    @Test
    fun everyPlanClearsEveryBalanceAndNeverSendsToSelf() {
        // Arrange
        val inputBalances: Map<String, Long> = mapOf(
            "a" to 1_250L,
            "b" to -400L,
            "c" to 75L,
            "d" to -925L,
            "e" to 0L,
        )

        // Act
        val actualPlan: MinimumTransferPlan = MinimumTransferSettlement.planTransfers(inputBalances)

        // Assert
        assertEquals(emptyMap<String, Long>(), residualAfter(inputBalances, actualPlan.transfers))
        assertTrue(actualPlan.transfers.all { transfer: SettlementTransfer -> transfer.fromMemberId != transfer.toMemberId })
        assertTrue(actualPlan.transfers.all { transfer: SettlementTransfer -> transfer.amountMinor > 0L })
    }

    @Test
    fun rejectsBalancesThatDoNotNetToZero() {
        // Arrange
        val inputBalances: Map<String, Long> = mapOf("a" to 100L, "b" to -99L)

        // Act
        val actualFailure: Throwable = assertThrows(IllegalArgumentException::class.java) {
            MinimumTransferSettlement.planTransfers(inputBalances)
        }

        // Assert
        assertEquals("Balances must net to zero before a settlement plan can be built.", actualFailure.message)
    }

    @Test
    fun largeGroupsFallBackToGreedyButStillClearEveryBalance() {
        // Arrange
        val inputBalances: Map<String, Long> = (1..18).associate { index: Int ->
            "member%02d".format(index) to if (index % 2 == 0) -100L else 100L
        }
        val inputNetTotal: Long = inputBalances.values.sum()

        // Act
        val actualPlan: MinimumTransferPlan = MinimumTransferSettlement.planTransfers(inputBalances)

        // Assert
        assertEquals(0L, inputNetTotal)
        assertFalse(actualPlan.isOptimal)
        assertEquals(emptyMap<String, Long>(), residualAfter(inputBalances, actualPlan.transfers))
    }

    private fun residualAfter(
        balanceByMemberId: Map<String, Long>,
        transfers: List<SettlementTransfer>,
    ): Map<String, Long> {
        val remaining: MutableMap<String, Long> = balanceByMemberId.toMutableMap()
        transfers.forEach { transfer: SettlementTransfer ->
            remaining[transfer.fromMemberId] = remaining.getValue(transfer.fromMemberId) + transfer.amountMinor
            remaining[transfer.toMemberId] = remaining.getValue(transfer.toMemberId) - transfer.amountMinor
        }
        return remaining.filterValues { it != 0L }
    }
}
