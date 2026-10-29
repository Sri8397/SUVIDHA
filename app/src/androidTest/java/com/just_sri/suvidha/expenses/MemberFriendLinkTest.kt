package com.just_sri.suvidha.expenses

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MemberFriendLinkTest {
    @Test
    fun friendUpdatesAndContactReuseKeepTheLinkWithoutChangingEitherLedger() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, ExpenseDatabase::class.java).build()
        try {
            val repo = SharedGroupRepository(db)
            val owner = "a".repeat(64)
            val member = "b".repeat(64)
            repo.createGroup("g", "Trip", "INR", owner, "Owner")
            repo.addInvitedMember("g", member)
            val friend = Friend("f", "Asha", 1, "+911234567890", "contact-1")
            db.friends().save(friend)
            db.splitTransactions().insertAll(listOf(SplitTransaction("personal", "f", 12.5, "Existing", 1)))
            repo.appendLocalEvent(SharedLedgerEvent("e", "g", owner, emptySet(), 2,
                SharedLedgerAction.ExpenseAdded("INR", listOf(SharedDebtUnit("d", owner, member, 1000)))))
            val personalBefore = db.splitTransactions().getAll()
            val sharedBefore = db.sharedLedgerEvents().getAll()

            repo.linkMemberToFriend("g", member, "f")
            db.friends().save(friend.copy(name = "Asha renamed", updatedAt = 3))
            assertEquals("f", db.sharedMemberFriendLinks().getAll().single().friendId)
            repo.linkMemberToContact("g", member, "contact-1", "Phone name", "+911234567890")
            assertEquals(1, db.friends().getAll().size)
            assertEquals("f", db.sharedMemberFriendLinks().getAll().single().friendId)
            assertEquals(personalBefore, db.splitTransactions().getAll())
            assertEquals(sharedBefore, db.sharedLedgerEvents().getAll())

            val snapshot = ExpenseBackupRepository(db).createSnapshot()
            val recovered = BackupJsonCodec.decode(BackupJsonCodec.encode(snapshot))
            assertEquals(snapshot, recovered)
            val beforeFriends = db.friends().getAll()
            assertTrue(runCatching { repo.createFriendForMember("g", "c".repeat(64), "Should roll back") }.isFailure)
            assertEquals(beforeFriends, db.friends().getAll())

            db.friends().delete("f")
            assertTrue(db.sharedMemberFriendLinks().getAll().isEmpty())
            assertEquals(sharedBefore, db.sharedLedgerEvents().getAll())
            assertEquals(2, db.sharedGroupMembers().getAll().size)
            ExpenseBackupRepository(db).replaceWith(recovered)
            assertEquals("f", db.sharedMemberFriendLinks().getAll().single().friendId)
            assertEquals("Asha renamed", db.friends().getAll().single().name)
            assertEquals(sharedBefore, db.sharedLedgerEvents().getAll())
        } finally { db.close() }
    }
}
