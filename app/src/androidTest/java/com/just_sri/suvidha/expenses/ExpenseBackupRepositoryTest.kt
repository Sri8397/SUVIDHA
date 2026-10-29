package com.just_sri.suvidha.expenses

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExpenseBackupRepositoryTest {
    @Test
    fun jsonSnapshotRoundTripsAndRestoreIsAtomic() = runBlocking {
        val target = AppBackupSnapshot(
            100L,
            listOf(Expense("e1", "Food", 9.5, "Lunch", 10L)),
            listOf(Friend("f1", "Ari", 11L)),
            listOf(SplitTransaction("s1", "f1", 3.0, "Fare", 12L)),
            listOf(SharedGroupEntity("g1", "Trip", "INR", 13L, 13L)),
            listOf(SharedGroupMemberEntity("g1", "m1", "Ari", "owner", 13L, true)),
            emptyList(),
        )
        assertEquals(target, BackupJsonCodec.decode(BackupJsonCodec.encode(target)))
        val legacy = org.json.JSONObject(String(BackupJsonCodec.encode(target), Charsets.UTF_8))
            .put("formatVersion", 2).put("roomSchemaVersion", 2)
        legacy.remove("transportOutbox")
        legacy.remove("transportStateBase64")
        legacy.remove("keyPackageBundleBase64")
        assertEquals(target, BackupJsonCodec.decode(legacy.toString().toByteArray()))
        val legacy3 = org.json.JSONObject(String(BackupJsonCodec.encode(target), Charsets.UTF_8))
            .put("formatVersion", 3).put("roomSchemaVersion", 3)
        legacy3.remove("memberFriendLinks")
        assertEquals(target, BackupJsonCodec.decode(legacy3.toString().toByteArray()))

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, ExpenseDatabase::class.java)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).allowMainThreadQueries().build()
        try {
            val repo = ExpenseBackupRepository(db)
            db.expenses().insert(Expense("old", "Food", 1.0, "Existing", 1L))
            val invalid = target.copy(members = listOf(SharedGroupMemberEntity("absent", "x", "X", "owner", 1L, true)))
            assertThrows(BackupFormatException::class.java) { runBlocking { repo.replaceWith(invalid) } }
            assertEquals(listOf("old"), db.expenses().getAll().map(Expense::id))
            repo.replaceWith(target)
            assertEquals(listOf("e1"), db.expenses().getAll().map(Expense::id))
            assertEquals(listOf("g1"), db.sharedGroups().getAll().map(SharedGroupEntity::id))
        } finally { db.close() }
    }
}
