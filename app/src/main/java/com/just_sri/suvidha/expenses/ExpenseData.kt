package com.just_sri.suvidha.expenses

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Insert
import androidx.room.migration.Migration
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

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
    @Insert suspend fun insert(expense: Expense)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(expenses: List<Expense>)
    @Query("DELETE FROM expenses WHERE id = :id") suspend fun delete(id: String)
    @Query("DELETE FROM expenses") suspend fun deleteAll()
    @Transaction suspend fun replaceAll(expenses: List<Expense>) { deleteAll(); insertAll(expenses) }
}

@Entity(tableName = "friends")
data class Friend(
    @PrimaryKey val id: String,
    val name: String,
    val updatedAt: Long,
    val phoneNumber: String? = null,
    val contactId: String? = null,
) {
    @Ignore
    var amount: Double = 0.0

    @Ignore
    constructor(
        id: String,
        name: String,
        amount: Double,
        updatedAt: Long,
        phoneNumber: String? = null,
        contactId: String? = null
    ) : this(id, name, updatedAt, phoneNumber, contactId) {
        this.amount = amount
    }

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
    val isShared: Boolean = false,
)

@Dao
interface FriendDao {
    @Query("SELECT * FROM friends ORDER BY name COLLATE NOCASE") fun observeAll(): Flow<List<Friend>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun save(friend: Friend)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(friends: List<Friend>)
    @Query("DELETE FROM friends WHERE id = :id") suspend fun delete(id: String)
    @Query("DELETE FROM friends WHERE id LIKE 'shared_user_%'") suspend fun deleteSharedUsers()
    @Query("DELETE FROM friends") suspend fun deleteAll()
    @Transaction suspend fun replaceAll(friends: List<Friend>) { deleteAll(); insertAll(friends) }
}

typealias FriendBalanceDao = FriendDao

@Dao
interface SplitTransactionDao {
    @Query("SELECT * FROM split_transactions ORDER BY date DESC") fun observeAll(): Flow<List<SplitTransaction>>
    @Query("SELECT * FROM split_transactions WHERE friendId = :friendId ORDER BY date DESC") fun observeByFriend(friendId: String): Flow<List<SplitTransaction>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(transactions: List<SplitTransaction>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun save(transaction: SplitTransaction)
    @Query("DELETE FROM split_transactions WHERE id = :id") suspend fun delete(id: String)
    @Query("DELETE FROM split_transactions WHERE id LIKE 'shared_%'") suspend fun deleteShared()
    @Query("DELETE FROM split_transactions") suspend fun deleteAll()
    @Transaction suspend fun replaceAll(transactions: List<SplitTransaction>) { deleteAll(); insertAll(transactions) }
}

@Database(entities = [Expense::class, Friend::class, SplitTransaction::class], version = 8, exportSchema = false)
abstract class ExpenseDatabase : RoomDatabase() {
    abstract fun expenses(): ExpenseDao
    abstract fun friends(): FriendDao
    fun friendBalances(): FriendDao = friends()
    abstract fun splitTransactions(): SplitTransactionDao

    companion object {
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE friend_balances ADD COLUMN phoneNumber TEXT")
                db.execSQL("ALTER TABLE friend_balances ADD COLUMN contactId TEXT")
            }
        }
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE split_transactions ADD COLUMN isShared INTEGER NOT NULL DEFAULT 0")
            }
        }
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `friends` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `phoneNumber` TEXT,
                        `contactId` TEXT,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT OR IGNORE INTO `friends` (`id`, `name`, `updatedAt`, `phoneNumber`, `contactId`)
                    SELECT `id`, `name`, `updatedAt`, `phoneNumber`, `contactId` FROM `friend_balances`
                """.trimIndent())
                db.execSQL("DROP TABLE IF EXISTS `friend_balances`")
            }
        }
        private val instances = mutableMapOf<String, ExpenseDatabase>()

        /** Keeps guest/offline records in the legacy local database and isolates each account. */
        fun getForAccount(context: Context, uid: String?): ExpenseDatabase {
            val key = uid ?: "offline"
            return synchronized(this) {
                instances[key] ?: run {
                    val dbName = if (uid == null) "suvidha-expenses.db" else "suvidha-expenses-$uid.db"
                    Room.databaseBuilder(context.applicationContext, ExpenseDatabase::class.java, dbName)
                        .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                        .fallbackToDestructiveMigration(true)
                        .build()
                        .also { instances[key] = it }
                }
            }
        }
    }
}

val defaultExpenseCategories = listOf("Rent", "Utilities", "Food", "Transport", "Health", "Shopping", "Salary", "Earn", "Other")

/** Draft rows produced by the full-screen add/split flows before they are persisted. */
internal data class ExpenseDraft(val category: String = "Other", val amount: String = "", val note: String = "")
internal data class SplitDraft(val friendId: String, val amount: String = "", val note: String = "")
