package com.just_sri.suvidha.expenses

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Dao
import androidx.room.ColumnInfo
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.Insert
import androidx.room.migration.Migration
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import java.io.File

private const val EXPENSE_DATABASE_VERSION = 4
private const val EXPENSE_DATABASE_NAME = "suvidha-expenses.db"

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey val id: String,
    val category: String,
    val amount: Double,
    val description: String,
    val date: Long,
)

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY date DESC") fun observeAll(): Flow<List<Expense>>
    @Query("SELECT * FROM expenses ORDER BY date DESC") suspend fun getAll(): List<Expense>
    @Insert suspend fun insert(expense: Expense)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(expenses: List<Expense>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun restoreAll(expenses: List<Expense>)
    @Query("DELETE FROM expenses WHERE id = :id") suspend fun delete(id: String)
    @Query("DELETE FROM expenses") suspend fun deleteAll()
}

@Entity(tableName = "friends")
data class Friend(
    @PrimaryKey val id: String,
    val name: String,
    val updatedAt: Long,
    val phoneNumber: String? = null,
    val contactId: String? = null,
) {
    // Balances come from split transactions, so Room stores friend identity only.
    @Ignore
    var amount: Double = 0.0

    fun withAmount(newAmount: Double): Friend = copy().also { it.amount = newAmount }
}

typealias FriendBalance = Friend

@Entity(tableName = "split_transactions")
data class SplitTransaction(
    @PrimaryKey val id: String,
    val friendId: String,
    val amount: Double,
    val note: String,
    val date: Long,
    val isSettled: Boolean = false,
    val settledAt: Long? = null,
)

@Dao
interface FriendDao {
    @Query("SELECT * FROM friends ORDER BY name COLLATE NOCASE") fun observeAll(): Flow<List<Friend>>
    @Query("SELECT * FROM friends ORDER BY name COLLATE NOCASE") suspend fun getAll(): List<Friend>
    @Upsert suspend fun save(friend: Friend)
    @Upsert suspend fun insertAll(friends: List<Friend>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun restoreAll(friends: List<Friend>)
    @Query("DELETE FROM friends WHERE id = :id") suspend fun delete(id: String)
    @Query("DELETE FROM friends") suspend fun deleteAll()
}

@Dao
interface SplitTransactionDao {
    @Query("SELECT * FROM split_transactions ORDER BY date DESC") fun observeAll(): Flow<List<SplitTransaction>>
    @Query("SELECT * FROM split_transactions ORDER BY date DESC") suspend fun getAll(): List<SplitTransaction>
    @Query("SELECT * FROM split_transactions WHERE friendId = :friendId ORDER BY date DESC") fun observeByFriend(friendId: String): Flow<List<SplitTransaction>>
    @Query("SELECT * FROM split_transactions WHERE id = :id LIMIT 1") suspend fun findById(id: String): SplitTransaction?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(transactions: List<SplitTransaction>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun save(transaction: SplitTransaction)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun restoreAll(transactions: List<SplitTransaction>)
    @Query("DELETE FROM split_transactions WHERE id = :id") suspend fun delete(id: String)
    @Query("DELETE FROM split_transactions") suspend fun deleteAll()
}

@Entity(tableName = "shared_groups")
data class SharedGroupEntity(
    @PrimaryKey val id: String,
    val name: String,
    val currencyCode: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

@Entity(
    tableName = "shared_group_members",
    primaryKeys = ["groupId", "memberId"],
    foreignKeys = [
        ForeignKey(
            entity = SharedGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("groupId"), Index("memberId")],
)
data class SharedGroupMemberEntity(
    val groupId: String,
    val memberId: String,
    val displayName: String,
    val role: String,
    val joinedAtMillis: Long,
    val isActive: Boolean,
)

@Entity(
    tableName = "shared_ledger_events",
    foreignKeys = [
        ForeignKey(
            entity = SharedGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("groupId"), Index("createdAtMillis")],
)
data class SharedLedgerEventRecord(
    @PrimaryKey val id: String,
    val groupId: String,
    val schemaVersion: Int,
    val actorMemberId: String,
    val actionType: String,
    val payloadJson: String,
    val parentEventIdsJson: String,
    val createdAtMillis: Long,
    @ColumnInfo(defaultValue = "''") val description: String = "",
    @ColumnInfo(defaultValue = "NULL") val transportEventJson: String? = null,
    @ColumnInfo(defaultValue = "'local'") val syncState: String = "local",
)

/** Signed encrypted transport events awaiting at least one relay acknowledgement. */
@Entity(tableName = "shared_transport_outbox", indices = [Index("groupId")], foreignKeys = [
    ForeignKey(entity = SharedGroupEntity::class, parentColumns = ["id"], childColumns = ["groupId"], onDelete = ForeignKey.CASCADE),
])
data class SharedTransportOutboxEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val purpose: String,
    val eventJson: String,
    val createdAtMillis: Long,
)

/** A local address-book label for a cryptographic identity; never part of a relay payload. */
@Entity(tableName = "shared_member_friend_links", indices = [Index("friendId")], foreignKeys = [
    ForeignKey(entity = Friend::class, parentColumns = ["id"], childColumns = ["friendId"], onDelete = ForeignKey.CASCADE),
])
data class SharedMemberFriendLinkEntity(
    @PrimaryKey val memberId: String,
    val friendId: String,
    val linkedAtMillis: Long,
)

@Dao
interface SharedMemberFriendLinkDao {
    @Query("SELECT * FROM shared_member_friend_links ORDER BY memberId")
    fun observeAll(): Flow<List<SharedMemberFriendLinkEntity>>
    @Query("SELECT * FROM shared_member_friend_links ORDER BY memberId")
    suspend fun getAll(): List<SharedMemberFriendLinkEntity>
    @Upsert suspend fun save(link: SharedMemberFriendLinkEntity)
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun restoreAll(links: List<SharedMemberFriendLinkEntity>)
    @Query("DELETE FROM shared_member_friend_links WHERE memberId = :memberId")
    suspend fun delete(memberId: String)
    @Query("DELETE FROM shared_member_friend_links")
    suspend fun deleteAll()
}

@Dao
interface SharedTransportOutboxDao {
    @Query("SELECT * FROM shared_transport_outbox ORDER BY createdAtMillis, id")
    fun observeAll(): Flow<List<SharedTransportOutboxEntity>>
    @Query("SELECT * FROM shared_transport_outbox ORDER BY createdAtMillis, id")
    suspend fun getAll(): List<SharedTransportOutboxEntity>
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(event: SharedTransportOutboxEntity)
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun restoreAll(events: List<SharedTransportOutboxEntity>)
    @Query("DELETE FROM shared_transport_outbox WHERE id = :id")
    suspend fun delete(id: String)
    @Query("DELETE FROM shared_transport_outbox")
    suspend fun deleteAll()
}

@Dao
interface SharedGroupDao {
    @Query("SELECT * FROM shared_groups ORDER BY updatedAtMillis DESC")
    fun observeAll(): Flow<List<SharedGroupEntity>>

    @Query("SELECT * FROM shared_groups ORDER BY updatedAtMillis DESC")
    suspend fun getAll(): List<SharedGroupEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(group: SharedGroupEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun restoreAll(groups: List<SharedGroupEntity>)

    @Query("SELECT * FROM shared_groups WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): SharedGroupEntity?

    @Query("UPDATE shared_groups SET updatedAtMillis = :updatedAtMillis WHERE id = :id")
    suspend fun updateTimestamp(id: String, updatedAtMillis: Long)

    @Query("DELETE FROM shared_groups WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM shared_groups")
    suspend fun deleteAll()
}

@Dao
interface SharedGroupMemberDao {
    @Query("SELECT * FROM shared_group_members WHERE groupId = :groupId ORDER BY displayName COLLATE NOCASE")
    fun observeByGroup(groupId: String): Flow<List<SharedGroupMemberEntity>>

    @Query("SELECT * FROM shared_group_members ORDER BY groupId, displayName COLLATE NOCASE")
    suspend fun getAll(): List<SharedGroupMemberEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(member: SharedGroupMemberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(member: SharedGroupMemberEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun restoreAll(members: List<SharedGroupMemberEntity>)

    @Query("DELETE FROM shared_group_members")
    suspend fun deleteAll()

    @Query("SELECT * FROM shared_group_members WHERE groupId = :groupId AND memberId = :memberId AND isActive = 1 LIMIT 1")
    suspend fun findActive(groupId: String, memberId: String): SharedGroupMemberEntity?

    @Query("SELECT * FROM shared_group_members WHERE groupId = :groupId AND memberId = :memberId LIMIT 1")
    suspend fun find(groupId: String, memberId: String): SharedGroupMemberEntity?

    @Query("SELECT * FROM shared_group_members WHERE groupId = :groupId")
    suspend fun getByGroup(groupId: String): List<SharedGroupMemberEntity>

    @Query("UPDATE shared_group_members SET isActive = 0 WHERE groupId = :groupId AND memberId = :memberId")
    suspend fun markInactive(groupId: String, memberId: String)
}

@Dao
interface SharedLedgerEventDao {
    @Query("SELECT * FROM shared_ledger_events WHERE groupId = :groupId ORDER BY createdAtMillis, id")
    fun observeByGroup(groupId: String): Flow<List<SharedLedgerEventRecord>>

    @Query("SELECT * FROM shared_ledger_events WHERE groupId = :groupId ORDER BY createdAtMillis, id")
    suspend fun getByGroup(groupId: String): List<SharedLedgerEventRecord>

    @Query("SELECT * FROM shared_ledger_events ORDER BY groupId, createdAtMillis, id")
    suspend fun getAll(): List<SharedLedgerEventRecord>

    @Query("SELECT * FROM shared_ledger_events WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): SharedLedgerEventRecord?

    @Query("UPDATE shared_ledger_events SET transportEventJson = :eventJson, syncState = 'pending' WHERE id = :id")
    suspend fun setTransportEvent(id: String, eventJson: String)

    @Query("UPDATE shared_ledger_events SET syncState = 'published' WHERE id = :id AND syncState = 'pending'")
    suspend fun markPublished(id: String)

    @Query("SELECT * FROM shared_ledger_events WHERE groupId = :groupId AND syncState = 'pending' AND transportEventJson IS NOT NULL ORDER BY createdAtMillis, id")
    suspend fun getPendingTransportEvents(groupId: String): List<SharedLedgerEventRecord>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(event: SharedLedgerEventRecord)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun restoreAll(events: List<SharedLedgerEventRecord>)

    @Query("DELETE FROM shared_ledger_events WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM shared_ledger_events")
    suspend fun deleteAll()
}

internal val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `shared_groups` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `currencyCode` TEXT NOT NULL,
                `createdAtMillis` INTEGER NOT NULL,
                `updatedAtMillis` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )""".trimIndent()
        )
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `shared_group_members` (
                `groupId` TEXT NOT NULL,
                `memberId` TEXT NOT NULL,
                `displayName` TEXT NOT NULL,
                `role` TEXT NOT NULL,
                `joinedAtMillis` INTEGER NOT NULL,
                `isActive` INTEGER NOT NULL,
                PRIMARY KEY(`groupId`, `memberId`),
                FOREIGN KEY(`groupId`) REFERENCES `shared_groups`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )""".trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_shared_group_members_groupId` ON `shared_group_members` (`groupId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_shared_group_members_memberId` ON `shared_group_members` (`memberId`)")
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `shared_ledger_events` (
                `id` TEXT NOT NULL,
                `groupId` TEXT NOT NULL,
                `schemaVersion` INTEGER NOT NULL,
                `actorMemberId` TEXT NOT NULL,
                `actionType` TEXT NOT NULL,
                `payloadJson` TEXT NOT NULL,
                `parentEventIdsJson` TEXT NOT NULL,
                `createdAtMillis` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`groupId`) REFERENCES `shared_groups`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )""".trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_shared_ledger_events_groupId` ON `shared_ledger_events` (`groupId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_shared_ledger_events_createdAtMillis` ON `shared_ledger_events` (`createdAtMillis`)")
    }
}

internal val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `shared_ledger_events` ADD COLUMN `description` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `shared_ledger_events` ADD COLUMN `transportEventJson` TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE `shared_ledger_events` ADD COLUMN `syncState` TEXT NOT NULL DEFAULT 'local'")
        db.execSQL("CREATE TABLE IF NOT EXISTS `shared_transport_outbox` (`id` TEXT NOT NULL, `groupId` TEXT NOT NULL, `purpose` TEXT NOT NULL, `eventJson` TEXT NOT NULL, `createdAtMillis` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`groupId`) REFERENCES `shared_groups`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_shared_transport_outbox_groupId` ON `shared_transport_outbox` (`groupId`)")
    }
}

internal val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `shared_member_friend_links` (`memberId` TEXT NOT NULL, `friendId` TEXT NOT NULL, `linkedAtMillis` INTEGER NOT NULL, PRIMARY KEY(`memberId`), FOREIGN KEY(`friendId`) REFERENCES `friends`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_shared_member_friend_links_friendId` ON `shared_member_friend_links` (`friendId`)")
    }
}

@Database(
    entities = [
        Expense::class,
        Friend::class,
        SplitTransaction::class,
        SharedGroupEntity::class,
        SharedGroupMemberEntity::class,
        SharedLedgerEventRecord::class,
        SharedTransportOutboxEntity::class,
        SharedMemberFriendLinkEntity::class,
    ],
    version = EXPENSE_DATABASE_VERSION,
    exportSchema = true,
)
abstract class ExpenseDatabase : RoomDatabase() {
    abstract fun expenses(): ExpenseDao
    abstract fun friends(): FriendDao
    abstract fun splitTransactions(): SplitTransactionDao
    abstract fun sharedGroups(): SharedGroupDao
    abstract fun sharedGroupMembers(): SharedGroupMemberDao
    abstract fun sharedLedgerEvents(): SharedLedgerEventDao
    abstract fun sharedTransportOutbox(): SharedTransportOutboxDao
    abstract fun sharedMemberFriendLinks(): SharedMemberFriendLinkDao

    companion object {
        @Volatile
        private var INSTANCE: ExpenseDatabase? = null

        fun get(context: Context): ExpenseDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: openDatabase(context).also { INSTANCE = it }
            }
        }

        fun deleteAndCreateFresh(context: Context): ExpenseDatabase {
            return synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
                context.deleteDatabase(EXPENSE_DATABASE_NAME)
                check(!context.getDatabasePath(EXPENSE_DATABASE_NAME).exists()) {
                    "Could not delete the existing database."
                }
                openDatabase(context).also { INSTANCE = it }
            }
        }

        private fun openDatabase(context: Context): ExpenseDatabase {
            backupDatabaseBeforeMigration(context)
            val database = Room.databaseBuilder(
                context.applicationContext,
                ExpenseDatabase::class.java,
                EXPENSE_DATABASE_NAME
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()

            return try {
                database.openHelper.writableDatabase
                database
            } catch (error: Throwable) {
                database.close()
                throw error
            }
        }

        /** Keep a recoverable copy before Room applies a schema migration. */
        private fun backupDatabaseBeforeMigration(context: Context) {
            val databaseFile = context.getDatabasePath(EXPENSE_DATABASE_NAME)
            if (!databaseFile.isFile) return

            val oldVersion = SQLiteDatabase.openDatabase(
                databaseFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READWRITE
            ).use { database ->
                val version = database.rawQuery("PRAGMA user_version", null).use { cursor ->
                    check(cursor.moveToFirst()) { "Could not read the existing database version." }
                    cursor.getInt(0)
                }
                if (version == EXPENSE_DATABASE_VERSION) return

                val checkpointed = database.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null).use { cursor ->
                    check(cursor.moveToFirst()) { "Could not checkpoint the existing database." }
                    cursor.getInt(0) == 0
                }
                check(checkpointed) { "Could not safely checkpoint the existing database before backup." }
                version
            }

            val backupDirectory = File(context.filesDir, "database-backups")
            check(backupDirectory.exists() || backupDirectory.mkdirs()) {
                "Could not create the database backup directory."
            }
            val backupFile = File(
                backupDirectory,
                "suvidha-expenses-v${oldVersion}-to-v${EXPENSE_DATABASE_VERSION}.db"
            )
            val temporaryFile = File(backupDirectory, "${backupFile.name}.tmp")
            databaseFile.copyTo(temporaryFile, overwrite = true)
            if (!temporaryFile.renameTo(backupFile)) {
                temporaryFile.copyTo(backupFile, overwrite = true)
                temporaryFile.delete()
            }
        }
    }
}

val defaultExpenseCategories get() = com.just_sri.suvidha.expenses.constants.defaultExpenseCategories

// Earn and Salary share the same income behavior across entry, list, and detail screens.
internal fun isIncomeCategory(category: String): Boolean =
    category.equals("Earn", ignoreCase = true) || category.equals("Salary", ignoreCase = true)

/** Draft rows produced by the full-screen add/split flows before they are persisted. */
internal data class ExpenseDraft(val category: String = "Other", val amount: String = "", val note: String = "")
internal data class SplitDraft(val friendId: String, val amount: String = "", val note: String = "")
