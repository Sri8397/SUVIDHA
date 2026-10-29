package com.just_sri.suvidha.expenses

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExpenseDatabaseMigrationTest {
    private fun rows(db: androidx.sqlite.db.SupportSQLiteDatabase, table: String): List<List<String?>> =
        db.query("SELECT * FROM `$table` ORDER BY 1, 2").use { cursor ->
            buildList { while (cursor.moveToNext()) add(List(cursor.columnCount) { if (cursor.isNull(it)) null else cursor.getString(it) }) }
        }
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ExpenseDatabase::class.java.canonicalName!!,
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrate1To2KeepsEveryPersonalLedgerRow() {
        val databaseName = "expense-migration-1-2"
        helper.createDatabase(databaseName, 1).apply {
            execSQL(
                "INSERT INTO expenses (id, category, amount, description, date) VALUES (?, ?, ?, ?, ?)",
                arrayOf<Any?>("expense-1", "Food", 14.75, "lunch", 1_735_689_600_000L),
            )
            execSQL(
                "INSERT INTO friends (id, name, updatedAt, phoneNumber, contactId) VALUES (?, ?, ?, ?, ?)",
                arrayOf<Any?>("friend-1", "Asha", 1_735_689_600_000L, "+911234567890", "contact-1"),
            )
            execSQL(
                "INSERT INTO split_transactions (id, friendId, amount, note, date, isSettled, settledAt) VALUES (?, ?, ?, ?, ?, ?, ?)",
                arrayOf<Any?>("split-1", "friend-1", 22.5, "cab", 1_735_689_700_000L, 0, null),
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            databaseName,
            2,
            true,
            MIGRATION_1_2,
        )
        migrated.use { db ->
            db.query("SELECT id, category, amount, description, date FROM expenses").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("expense-1", cursor.getString(0))
                assertEquals("Food", cursor.getString(1))
                assertEquals(14.75, cursor.getDouble(2), 0.0)
                assertEquals("lunch", cursor.getString(3))
                assertEquals(1_735_689_600_000L, cursor.getLong(4))
                assertFalse(cursor.moveToNext())
            }
            db.query("SELECT id, name, updatedAt, phoneNumber, contactId FROM friends").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("friend-1", cursor.getString(0))
                assertEquals("Asha", cursor.getString(1))
                assertEquals(1_735_689_600_000L, cursor.getLong(2))
                assertEquals("+911234567890", cursor.getString(3))
                assertEquals("contact-1", cursor.getString(4))
                assertFalse(cursor.moveToNext())
            }
            db.query("SELECT id, friendId, amount, note, date, isSettled, settledAt FROM split_transactions").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("split-1", cursor.getString(0))
                assertEquals("friend-1", cursor.getString(1))
                assertEquals(22.5, cursor.getDouble(2), 0.0)
                assertEquals("cab", cursor.getString(3))
                assertEquals(1_735_689_700_000L, cursor.getLong(4))
                assertEquals(0, cursor.getInt(5))
                assertTrue(cursor.isNull(6))
                assertFalse(cursor.moveToNext())
            }

            db.execSQL(
                "INSERT INTO shared_groups (id, name, currencyCode, createdAtMillis, updatedAtMillis) VALUES (?, ?, ?, ?, ?)",
                arrayOf<Any?>("group-1", "Trip", "INR", 100L, 100L),
            )
            db.execSQL(
                "INSERT INTO shared_group_members (groupId, memberId, displayName, role, joinedAtMillis, isActive) VALUES (?, ?, ?, ?, ?, ?)",
                arrayOf<Any?>("group-1", "member-1", "Asha", "member", 100L, 1),
            )
            db.query("SELECT memberId FROM shared_group_members WHERE groupId = 'group-1'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("member-1", cursor.getString(0))
            }
        }
    }

    @Test
    fun migrate2To3PreservesEventsAndAddsDurableOutboxDefaults() {
        val databaseName = "expense-migration-2-3"
        helper.createDatabase(databaseName, 2).apply {
            execSQL("INSERT INTO shared_groups (id, name, currencyCode, createdAtMillis, updatedAtMillis) VALUES ('g', 'Trip', 'INR', 10, 10)")
            execSQL("INSERT INTO shared_ledger_events (id, groupId, schemaVersion, actorMemberId, actionType, payloadJson, parentEventIdsJson, createdAtMillis) VALUES ('e', 'g', 1, 'a', 'expense_added', '{\"currencyCode\":\"INR\",\"debts\":[]}', '[]', 11)")
            close()
        }

        helper.runMigrationsAndValidate(databaseName, 3, true, MIGRATION_2_3).use { db ->
            db.query("SELECT id, description, transportEventJson, syncState, groupId, schemaVersion, actorMemberId, actionType, payloadJson, parentEventIdsJson, createdAtMillis FROM shared_ledger_events WHERE id = 'e'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("e", cursor.getString(0))
                assertEquals("", cursor.getString(1))
                assertTrue(cursor.isNull(2))
                assertEquals("local", cursor.getString(3))
                assertEquals("g", cursor.getString(4))
                assertEquals(1, cursor.getInt(5))
                assertEquals("a", cursor.getString(6))
                assertEquals("expense_added", cursor.getString(7))
                assertEquals("{\"currencyCode\":\"INR\",\"debts\":[]}", cursor.getString(8))
                assertEquals("[]", cursor.getString(9))
                assertEquals(11L, cursor.getLong(10))
                assertFalse(cursor.moveToNext())
            }
            db.execSQL("INSERT INTO shared_transport_outbox (id, groupId, purpose, eventJson, createdAtMillis) VALUES ('welcome', 'g', 'welcome', '{}', 12)")
            db.query("SELECT purpose FROM shared_transport_outbox WHERE id = 'welcome'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("welcome", cursor.getString(0))
            }
        }
    }

    @Test
    fun migrate3To4PreservesEverySeededColumnAndAddsOnlyLocalIdentityLinks() {
        val name = "expense-migration-3-4"
        val tables = listOf("expenses", "friends", "split_transactions", "shared_groups", "shared_group_members", "shared_ledger_events", "shared_transport_outbox")
        val before = helper.createDatabase(name, 3).use { db ->
            db.execSQL("INSERT INTO expenses VALUES ('expense', 'Food', 14.75, 'Lunch', 10)")
            db.execSQL("INSERT INTO friends VALUES ('friend', 'Asha', 11, '+911234567890', 'contact-1')")
            db.execSQL("INSERT INTO split_transactions VALUES ('split', 'friend', 12.5, 'Cab', 12, 0, NULL)")
            db.execSQL("INSERT INTO shared_groups VALUES ('group', 'Trip', 'INR', 13, 13)")
            db.execSQL("INSERT INTO shared_group_members VALUES ('group', 'member', 'Peer', 'member', 14, 1)")
            db.execSQL("INSERT INTO shared_ledger_events (id, groupId, schemaVersion, actorMemberId, actionType, payloadJson, parentEventIdsJson, createdAtMillis, description, transportEventJson, syncState) VALUES ('event', 'group', 1, 'member', 'expense_added', '{}', '[]', 15, 'Shared lunch', 'encrypted event', 'pending')")
            db.execSQL("INSERT INTO shared_transport_outbox VALUES ('welcome', 'group', 'welcome', 'encrypted welcome', 16)")
            tables.associateWith { rows(db, it) }
        }
        helper.runMigrationsAndValidate(name, 4, true, MIGRATION_3_4).use { db ->
            tables.forEach { table -> assertEquals("$table changed", before[table], rows(db, table)) }
            assertTrue(rows(db, "shared_member_friend_links").isEmpty())
            db.execSQL("INSERT INTO shared_member_friend_links VALUES (?, 'friend', 17)", arrayOf("b".repeat(64)))
            assertEquals(listOf(listOf("b".repeat(64), "friend", "17")), rows(db, "shared_member_friend_links"))
        }
    }
}
