package com.just_sri.suvidha.expenses

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedLedgerTest {
    @Test
    fun descendantsOfMissingParentsWaitWithoutBeingRejected() {
        val child = expenseEvent.copy(eventId = "child", parentEventIds = setOf("missing"))
        val grandchild = expenseEvent.copy(eventId = "grandchild", parentEventIds = setOf("child"))
        val projection = projectSharedLedger("group-1", listOf(grandchild, child), "INR")
        assertEquals(setOf("child", "grandchild"), projection.pendingEventIds)
        assertTrue(projection.rejectedEvents.isEmpty())
    }
    private val expenseEvent = SharedLedgerEvent(
        eventId = "expense-event",
        groupId = "group-1",
        actorMemberId = "alice",
        parentEventIds = emptySet(),
        occurredAtMillis = 10L,
        action = SharedLedgerAction.ExpenseAdded(
            currencyCode = "INR",
            debts = listOf(SharedDebtUnit("debt-1", "alice", "bob", 1_000L)),
        ),
    )

    @Test
    fun authorCannotClaimAnotherPayerOrSettleAnUnrelatedDebt() {
        val forgedExpense = expenseEvent.copy(eventId = "forged", actorMemberId = "mallory")
        val unrelatedSettlement = SharedLedgerEvent("settle", "group-1", "mallory", setOf("expense-event"), 20,
            SharedLedgerAction.SettlementRecorded(listOf(SharedSettlementAllocation("debt-1", 100))))
        val projection = projectSharedLedger("group-1", listOf(expenseEvent, forgedExpense, unrelatedSettlement), "INR")
        assertEquals(setOf("expense-event"), projection.acceptedEventIds)
        assertEquals(setOf("forged", "settle"), projection.rejectedEvents.keys)
        assertEquals(1000L, projection.debtById.getValue("debt-1").remainingAmountMinor)
    }

    @Test
    fun partialSettlementReplaysInCausalOrderAndIgnoresDuplicateDelivery() {
        val settlement = SharedLedgerEvent(
            eventId = "settlement-event",
            groupId = "group-1",
            actorMemberId = "bob",
            parentEventIds = setOf("expense-event"),
            occurredAtMillis = 20L,
            action = SharedLedgerAction.SettlementRecorded(
                listOf(SharedSettlementAllocation("debt-1", 350L)),
            ),
        )

        val projection = projectSharedLedger(
            groupId = "group-1",
            events = listOf(settlement, expenseEvent, expenseEvent),
            currencyCode = "INR",
        )

        assertEquals(2, projection.acceptedEventIds.size)
        assertEquals(650L, projection.balanceByMemberId["alice"])
        assertEquals(-650L, projection.balanceByMemberId["bob"])
        assertEquals(650L, projection.debtById.getValue("debt-1").remainingAmountMinor)
        assertTrue(projection.rejectedEvents.isEmpty())
    }

    @Test
    fun competingSettlementsConvergeByEventIdAndCannotOverAllocate() {
        fun settlement(id: String) = SharedLedgerEvent(
            eventId = id,
            groupId = "group-1",
            actorMemberId = "bob",
            parentEventIds = setOf("expense-event"),
            occurredAtMillis = 20L,
            action = SharedLedgerAction.SettlementRecorded(
                listOf(SharedSettlementAllocation("debt-1", 700L)),
            ),
        )

        val firstOrder = listOf(expenseEvent, settlement("settle-a"), settlement("settle-b"))
        val secondOrder = firstOrder.reversed()
        val first = projectSharedLedger("group-1", firstOrder, "INR")
        val second = projectSharedLedger("group-1", secondOrder, "INR")

        assertEquals(first, second)
        assertEquals(300L, first.debtById.getValue("debt-1").remainingAmountMinor)
        assertTrue("settle-a" in first.acceptedEventIds)
        assertFalse("settle-b" in first.acceptedEventIds)
        assertTrue("settle-b" in first.rejectedEvents)
    }

    @Test
    fun multiDebtSettlementIsRejectedAsAWholeWhenOneAllocationIsInvalid() {
        val otherDebtEvent = SharedLedgerEvent(
            eventId = "expense-event-2",
            groupId = "group-1",
            actorMemberId = "alice",
            parentEventIds = emptySet(),
            occurredAtMillis = 11L,
            action = SharedLedgerAction.ExpenseAdded(
                currencyCode = "INR",
                debts = listOf(SharedDebtUnit("debt-2", "alice", "charlie", 500L)),
            ),
        )
        val settlement = SharedLedgerEvent(
            eventId = "settlement-event",
            groupId = "group-1",
            actorMemberId = "bob",
            parentEventIds = setOf("expense-event", "expense-event-2"),
            occurredAtMillis = 20L,
            action = SharedLedgerAction.SettlementRecorded(
                listOf(
                    SharedSettlementAllocation("debt-1", 300L),
                    SharedSettlementAllocation("missing-debt", 1L),
                ),
            ),
        )

        val projection = projectSharedLedger(
            "group-1",
            listOf(expenseEvent, otherDebtEvent, settlement),
            "INR",
        )

        assertEquals(1_000L, projection.debtById.getValue("debt-1").remainingAmountMinor)
        assertEquals(500L, projection.debtById.getValue("debt-2").remainingAmountMinor)
        assertTrue("settlement-event" in projection.rejectedEvents)
    }

    @Test
    fun ownerSoftDeletesOnlyTheOutstandingDebtAndKeepsHistory() {
        val payment = SharedLedgerEvent("payment", "group-1", "bob", setOf("expense-event"), 20,
            SharedLedgerAction.SettlementRecorded(listOf(SharedSettlementAllocation("debt-1", 350))))
        val removal = SharedLedgerEvent("removal", "group-1", "owner", setOf("payment"), 30,
            SharedLedgerAction.DebtRemoved("debt-1"))
        val projection = projectSharedLedger("group-1", listOf(removal, payment, expenseEvent), "INR", "owner")
        assertEquals(setOf("expense-event", "payment", "removal"), projection.acceptedEventIds)
        assertEquals(0L, projection.debtById.getValue("debt-1").remainingAmountMinor)
        assertTrue(projection.balanceByMemberId.isEmpty())
        assertTrue(projection.rejectedEvents.isEmpty())
    }

    @Test
    fun softDeleteRequiresAuthorOrOwnerAndCausalExpense() {
        val removal = SharedLedgerEvent("removal", "group-1", "bob", setOf("expense-event"), 30,
            SharedLedgerAction.DebtRemoved("debt-1"))
        val unauthorized = projectSharedLedger("group-1", listOf(expenseEvent, removal), "INR", "owner")
        assertTrue("removal" in unauthorized.rejectedEvents)
        assertEquals(1000L, unauthorized.debtById.getValue("debt-1").remainingAmountMinor)
        val nonCausal = removal.copy(actorMemberId = "owner", parentEventIds = emptySet())
        val projection = projectSharedLedger("group-1", listOf(expenseEvent, nonCausal), "INR", "owner")
        assertTrue("removal" in projection.rejectedEvents)
    }

    @Test
    fun expenseAuthorCanSettleAndRemoveTheirOwnDebtWithoutBeingOwner() {
        val payment = SharedLedgerEvent("payment", "group-1", "alice", setOf("expense-event"), 20,
            SharedLedgerAction.SettlementRecorded(listOf(SharedSettlementAllocation("debt-1", 350))))
        val removal = SharedLedgerEvent("removal", "group-1", "alice", setOf("payment"), 30,
            SharedLedgerAction.DebtRemoved("debt-1"))
        val projection = projectSharedLedger("group-1", listOf(expenseEvent, payment, removal), "INR", "owner")
        assertEquals(setOf("expense-event", "payment", "removal"), projection.acceptedEventIds)
        assertEquals(0L, projection.debtById.getValue("debt-1").remainingAmountMinor)
        assertTrue(projection.rejectedEvents.isEmpty())
    }

    @Test
    fun mismatchedGroupCurrencyIsRejected() {
        val projection = projectSharedLedger("group-1", listOf(expenseEvent), "USD")

        assertTrue(projection.acceptedEventIds.isEmpty())
        assertTrue("expense-event" in projection.rejectedEvents)
    }

    @Test
    fun chatMessageIsAcceptedAndDoesNotAlterDebtsOrBalances() {
        val chat = SharedLedgerEvent(
            eventId = "chat-1",
            groupId = "group-1",
            actorMemberId = "bob",
            parentEventIds = setOf("expense-event"),
            occurredAtMillis = 25L,
            action = SharedLedgerAction.ChatMessage("Sounds good, see you guys in Goa!")
        )
        val projection = projectSharedLedger("group-1", listOf(expenseEvent, chat), "INR")

        assertEquals(setOf("expense-event", "chat-1"), projection.acceptedEventIds)
        assertTrue(projection.rejectedEvents.isEmpty())
        assertEquals(1000L, projection.balanceByMemberId["alice"])
        assertEquals(-1000L, projection.balanceByMemberId["bob"])
        assertEquals(1000L, projection.debtById.getValue("debt-1").remainingAmountMinor)
    }

    @Test
    fun blankChatMessageIsRejected() {
        val chat = SharedLedgerEvent(
            eventId = "chat-blank",
            groupId = "group-1",
            actorMemberId = "bob",
            parentEventIds = setOf("expense-event"),
            occurredAtMillis = 25L,
            action = SharedLedgerAction.ChatMessage("   ")
        )
        val projection = projectSharedLedger("group-1", listOf(expenseEvent, chat), "INR")

        assertEquals(setOf("expense-event"), projection.acceptedEventIds)
        assertTrue("chat-blank" in projection.rejectedEvents)
    }
}
