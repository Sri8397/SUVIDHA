package com.just_sri.suvidha.expenses

/** One payment that moves money from a debtor to a creditor. Amounts are in the currency's minor unit. */
data class SettlementTransfer(
    val fromMemberId: String,
    val toMemberId: String,
    val amountMinor: Long,
)

/** Transfers that clear every net balance. [isOptimal] is false when the member count exceeded the exact search limit. */
data class MinimumTransferPlan(
    val transfers: List<SettlementTransfer>,
    val isOptimal: Boolean,
)

private data class MemberAmount(val memberId: String, val amountMinor: Long)

/**
 * Reduces net balances to as few payments as possible.
 *
 * A set of members whose balances sum to zero can be cleared with (size - 1) payments. The fewest payments
 * therefore equals (member count - largest number of disjoint zero-sum groups). That number is found with a
 * subset DP, which is exponential, so it runs only up to [EXACT_MEMBER_LIMIT] members. Larger groups fall
 * back to greedy matching, which always clears every balance but may use more payments than necessary.
 */
internal object MinimumTransferSettlement {
    private const val EXACT_MEMBER_LIMIT: Int = 16

    fun planTransfers(balanceByMemberId: Map<String, Long>): MinimumTransferPlan {
        val members: List<MemberAmount> = balanceByMemberId
            .filterValues { it != 0L }
            .toSortedMap()
            .map { (memberId: String, amountMinor: Long) -> MemberAmount(memberId, amountMinor) }
        require(members.fold(0L) { sum: Long, member: MemberAmount -> Math.addExact(sum, member.amountMinor) } == 0L) {
            "Balances must net to zero before a settlement plan can be built."
        }
        return if (members.size <= EXACT_MEMBER_LIMIT) planExact(members) else planGreedy(members)
    }

    private fun planExact(members: List<MemberAmount>): MinimumTransferPlan {
        val transfers: List<SettlementTransfer> = findZeroSumGroups(members).flatMap { group: List<MemberAmount> -> matchGreedily(group) }
        return MinimumTransferPlan(transfers = transfers, isOptimal = true)
    }

    private fun planGreedy(members: List<MemberAmount>): MinimumTransferPlan =
        MinimumTransferPlan(transfers = matchGreedily(members), isOptimal = false)

    /**
     * Partitions members into the largest number of zero-sum groups. The DP walks subsets from the full set
     * down to the empty set, removing one member per step. Each zero-sum subset on the chosen path closes a group.
     */
    private fun findZeroSumGroups(members: List<MemberAmount>): List<List<MemberAmount>> {
        val subsetCount: Int = 1 shl members.size
        val fullMask: Int = subsetCount - 1
        val sumByMask: LongArray = sumEverySubset(members)
        val zeroSumCountByMask: IntArray = IntArray(subsetCount)
        val removedIndexByMask: IntArray = IntArray(subsetCount)
        (1..fullMask).forEach { mask: Int ->
            val bestIndex: Int = occupiedIndices(mask, members.size)
                .maxByOrNull { index: Int -> zeroSumCountByMask[mask xor (1 shl index)] } ?: 0
            removedIndexByMask[mask] = bestIndex
            zeroSumCountByMask[mask] = zeroSumCountByMask[mask xor (1 shl bestIndex)] + zeroSumBonus(sumByMask[mask])
        }
        return collectGroups(fullMask, members, removedIndexByMask, sumByMask)
    }

    private fun sumEverySubset(members: List<MemberAmount>): LongArray {
        val sumByMask: LongArray = LongArray(1 shl members.size)
        (1 until sumByMask.size).forEach { mask: Int ->
            sumByMask[mask] = Math.addExact(
                sumByMask[mask and (mask - 1)],
                members[Integer.numberOfTrailingZeros(mask)].amountMinor,
            )
        }
        return sumByMask
    }

    private fun occupiedIndices(mask: Int, memberCount: Int): List<Int> =
        (0 until memberCount).filter { index: Int -> ((mask shr index) and 1) == 1 }

    private fun zeroSumBonus(sumMinor: Long): Int = if (sumMinor == 0L) 1 else 0

    private fun collectGroups(
        fullMask: Int,
        members: List<MemberAmount>,
        removedIndexByMask: IntArray,
        sumByMask: LongArray,
    ): List<List<MemberAmount>> {
        val groups: MutableList<List<MemberAmount>> = mutableListOf()
        val currentGroup: MutableList<MemberAmount> = mutableListOf()
        var mask: Int = fullMask
        while (mask != 0) {
            val removedIndex: Int = removedIndexByMask[mask]
            currentGroup += members[removedIndex]
            mask = mask xor (1 shl removedIndex)
            if (sumByMask[mask] == 0L) {
                groups += currentGroup.toList()
                currentGroup.clear()
            }
        }
        return groups
    }

    private fun matchGreedily(members: List<MemberAmount>): List<SettlementTransfer> {
        val creditors: List<MemberAmount> = members
            .filter { member: MemberAmount -> member.amountMinor > 0L }
            .sortedWith(compareByDescending<MemberAmount> { it.amountMinor }.thenBy { it.memberId })
        val debtors: List<MemberAmount> = members
            .filter { member: MemberAmount -> member.amountMinor < 0L }
            .map { member: MemberAmount -> MemberAmount(member.memberId, -member.amountMinor) }
            .sortedWith(compareByDescending<MemberAmount> { it.amountMinor }.thenBy { it.memberId })
        return matchRecursively(creditors, debtors)
    }

    private tailrec fun matchRecursively(
        creditors: List<MemberAmount>,
        debtors: List<MemberAmount>,
        transfers: List<SettlementTransfer> = emptyList(),
    ): List<SettlementTransfer> {
        if (creditors.isEmpty() || debtors.isEmpty()) return transfers
        val creditor: MemberAmount = creditors.first()
        val debtor: MemberAmount = debtors.first()
        val amountMinor: Long = minOf(creditor.amountMinor, debtor.amountMinor)
        val transfer = SettlementTransfer(debtor.memberId, creditor.memberId, amountMinor)
        val remainingCreditor: Long = creditor.amountMinor - amountMinor
        val remainingDebtor: Long = debtor.amountMinor - amountMinor
        val nextCreditors: List<MemberAmount> = listOfNotNull(
            creditor.copy(amountMinor = remainingCreditor).takeIf { remainingCreditor > 0L },
        ) + creditors.drop(1)
        val nextDebtors: List<MemberAmount> = listOfNotNull(
            debtor.copy(amountMinor = remainingDebtor).takeIf { remainingDebtor > 0L },
        ) + debtors.drop(1)
        return matchRecursively(nextCreditors, nextDebtors, transfers + transfer)
    }
}
