package com.just_sri.suvidha.expenses

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SharedGroupRepositoryTest {
    @Test
    fun remoteSettlementWaitsForItsExpenseAndDuplicateDeliveryKeepsOutboxState() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, ExpenseDatabase::class.java).build()
        try {
            val repository = SharedGroupRepository(database)
            repository.createGroup("g", "Trip", "INR", "alice", "Alice")
            val expense = SharedLedgerEvent("e", "g", "alice", emptySet(), 10,
                SharedLedgerAction.ExpenseAdded("INR", listOf(SharedDebtUnit("d", "alice", "bob", 1000))), "Lunch")
            val settlement = SharedLedgerEvent("s", "g", "bob", setOf("e"), 11,
                SharedLedgerAction.SettlementRecorded(listOf(SharedSettlementAllocation("d", 350))), "Payment")
            assertEquals(setOf("s"), repository.appendRemoteEvent(settlement).pendingEventIds)
            val replayed = repository.appendRemoteEvent(expense)
            assertEquals(setOf("e", "s"), replayed.acceptedEventIds)
            assertEquals(-650L, replayed.balanceByMemberId["bob"])
            assertEquals(replayed, repository.appendRemoteEvent(settlement))

            val outgoing = expense.copy(eventId = "outgoing", parentEventIds = setOf("e", "s"),
                action = SharedLedgerAction.ExpenseAdded("INR", listOf(SharedDebtUnit("d2", "alice", "bob", 200))))
            repository.appendLocalEvent(outgoing, "encrypted-outer-event")
            assertEquals(listOf("outgoing"), repository.pendingEncryptedEvents("g").map { it.id })
            repository.appendRemoteEvent(outgoing)
            assertEquals("pending", database.sharedLedgerEvents().findById("outgoing")?.syncState)
            repository.markEventPublished("outgoing")
            assertTrue(repository.pendingEncryptedEvents("g").isEmpty())
            assertEquals("published", database.sharedLedgerEvents().findById("outgoing")?.syncState)
        } finally { database.close() }
    }

    @Test
    fun invitationAddsMemberToLocalRosterWithoutRoomSchemaChange() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, ExpenseDatabase::class.java)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .allowMainThreadQueries()
            .build()

        try {
            val repository = SharedGroupRepository(database)
            repository.createGroup("invite-group", "Trip", "INR", "${"a".repeat(64)}", "Owner")
            val invitee = "b".repeat(64)
            repository.addInvitedMember("invite-group", invitee, nowMillis = 20L)
            val member = database.sharedGroupMembers().find("invite-group", invitee)

            assertEquals("member", member?.role)
            assertTrue(member?.isActive == true)
            assertEquals(20L, member?.joinedAtMillis)
        } finally {
            database.close()
        }
    }

    @Test
    fun localGroupEventsPersistReplayAndDeduplicate() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, ExpenseDatabase::class.java)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .allowMainThreadQueries()
            .build()

        try {
            val repository = SharedGroupRepository(database)
            repository.createGroup(
                id = "group-1",
                name = "Weekend trip",
                currencyCode = "INR",
                ownerMemberId = "alice",
                ownerDisplayName = "Alice",
                nowMillis = 10L,
            )
            database.sharedGroupMembers().insert(
                SharedGroupMemberEntity(
                    groupId = "group-1",
                    memberId = "bob",
                    displayName = "Bob",
                    role = "member",
                    joinedAtMillis = 11L,
                    isActive = true,
                )
            )

            val expense = SharedLedgerEvent(
                eventId = "expense-event",
                groupId = "group-1",
                actorMemberId = "alice",
                parentEventIds = emptySet(),
                occurredAtMillis = 12L,
                action = SharedLedgerAction.ExpenseAdded(
                    currencyCode = "INR",
                    debts = listOf(SharedDebtUnit("debt-1", "alice", "bob", 1_000L)),
                ),
            )
            val afterExpense = repository.appendLocalEvent(expense)
            val duplicateDelivery = repository.appendLocalEvent(expense)
            val settlement = SharedLedgerEvent(
                eventId = "settlement-event",
                groupId = "group-1",
                actorMemberId = "bob",
                parentEventIds = setOf("expense-event"),
                occurredAtMillis = 13L,
                action = SharedLedgerAction.SettlementRecorded(
                    listOf(SharedSettlementAllocation("debt-1", 250L)),
                ),
            )
            repository.appendLocalEvent(settlement)
            val replayed = repository.replayGroup("group-1")

            assertEquals(afterExpense, duplicateDelivery)
            assertEquals(750L, replayed.balanceByMemberId["alice"])
            assertEquals(-750L, replayed.balanceByMemberId["bob"])
            assertEquals(2, replayed.acceptedEventIds.size)
            assertTrue(replayed.rejectedEvents.isEmpty())
            assertEquals(2, database.sharedLedgerEvents().getByGroup("group-1").size)
        } finally {
            database.close()
        }
    }
}
