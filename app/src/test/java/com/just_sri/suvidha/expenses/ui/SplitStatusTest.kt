package com.just_sri.suvidha.expenses.ui

import com.just_sri.suvidha.expenses.SharedDebtUnit
import com.just_sri.suvidha.expenses.SharedLedgerAction
import com.just_sri.suvidha.expenses.SharedLedgerCodec
import com.just_sri.suvidha.expenses.SharedLedgerEvent
import com.just_sri.suvidha.expenses.SharedLedgerEventRecord
import org.junit.Assert.assertEquals
import org.junit.Test

class SplitStatusTest {
    @Test
    fun returnsActiveWhenNoCancellationOccurred() {
        val debtUnit = SharedDebtUnit("debt-1", "user-1", "user-2", 5000L)
        val event = SharedLedgerEvent(
            eventId = "event-1",
            groupId = "group-1",
            actorMemberId = "user-1",
            parentEventIds = emptySet(),
            occurredAtMillis = 1000L,
            action = SharedLedgerAction.ExpenseAdded("INR", listOf(debtUnit)),
            description = "Dinner"
        )
        val (actionType, payload) = SharedLedgerCodec.encodeAction(event.action)
        val inputRecord = SharedLedgerEventRecord(
            id = event.eventId,
            groupId = event.groupId,
            schemaVersion = 1,
            actorMemberId = event.actorMemberId,
            actionType = actionType,
            payloadJson = payload,
            parentEventIdsJson = "[]",
            createdAtMillis = event.occurredAtMillis,
            description = event.description
        )
        val actualStatus: SplitStatus = resolveSplitStatus(inputRecord, listOf(inputRecord))
        val expectedStatus: SplitStatus = SplitStatus.ACTIVE
        assertEquals(expectedStatus, actualStatus)
    }

    @Test
    fun returnsCancelledWhenDescriptionContainsCancelledMarker() {
        val inputRecord = SharedLedgerEventRecord(
            id = "event-cancel-sample",
            groupId = "group-1",
            schemaVersion = 1,
            actorMemberId = "user-1",
            actionType = "expense_added",
            payloadJson = "{}",
            parentEventIdsJson = "[]",
            createdAtMillis = 1000L,
            description = "Taxi to Airport (Cancelled)"
        )
        val actualStatus: SplitStatus = resolveSplitStatus(inputRecord, listOf(inputRecord))
        val expectedStatus: SplitStatus = SplitStatus.CANCELLED
        assertEquals(expectedStatus, actualStatus)
    }

    @Test
    fun returnsCancelledWhenAllDebtsAreRemoved() {
        val debtUnit = SharedDebtUnit("debt-10", "user-1", "user-2", 2000L)
        val addEvent = SharedLedgerEvent(
            eventId = "event-add",
            groupId = "group-1",
            actorMemberId = "user-1",
            parentEventIds = emptySet(),
            occurredAtMillis = 1000L,
            action = SharedLedgerAction.ExpenseAdded("INR", listOf(debtUnit)),
            description = "Lunch"
        )
        val (addAction, addPayload) = SharedLedgerCodec.encodeAction(addEvent.action)
        val addRecord = SharedLedgerEventRecord(
            id = addEvent.eventId,
            groupId = addEvent.groupId,
            schemaVersion = 1,
            actorMemberId = addEvent.actorMemberId,
            actionType = addAction,
            payloadJson = addPayload,
            parentEventIdsJson = "[]",
            createdAtMillis = addEvent.occurredAtMillis,
            description = addEvent.description
        )
        val removeEvent = SharedLedgerEvent(
            eventId = "event-remove",
            groupId = "group-1",
            actorMemberId = "user-1",
            parentEventIds = setOf("event-add"),
            occurredAtMillis = 1200L,
            action = SharedLedgerAction.DebtRemoved("debt-10"),
            description = "Debt removed"
        )
        val (removeAction, removePayload) = SharedLedgerCodec.encodeAction(removeEvent.action)
        val removeRecord = SharedLedgerEventRecord(
            id = removeEvent.eventId,
            groupId = removeEvent.groupId,
            schemaVersion = 1,
            actorMemberId = removeEvent.actorMemberId,
            actionType = removeAction,
            payloadJson = removePayload,
            parentEventIdsJson = "[\"event-add\"]",
            createdAtMillis = removeEvent.occurredAtMillis,
            description = removeEvent.description
        )
        val actualStatus: SplitStatus = resolveSplitStatus(addRecord, listOf(addRecord, removeRecord))
        val expectedStatus: SplitStatus = SplitStatus.CANCELLED
        assertEquals(expectedStatus, actualStatus)
    }
}
