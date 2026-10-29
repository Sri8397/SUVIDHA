package com.just_sri.suvidha.expenses

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import java.io.File

private const val EXPENSE_DATABASE_VERSION = 1
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
    @Insert suspend fun insert(expense: Expense)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(expenses: List<Expense>)
    @Query("DELETE FROM expenses WHERE id = :id") suspend fun delete(id: String)
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
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun save(friend: Friend)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(friends: List<Friend>)
    @Query("DELETE FROM friends WHERE id = :id") suspend fun delete(id: String)
}

@Dao
interface SplitTransactionDao {
    @Query("SELECT * FROM split_transactions ORDER BY date DESC") fun observeAll(): Flow<List<SplitTransaction>>
    @Query("SELECT * FROM split_transactions WHERE friendId = :friendId ORDER BY date DESC") fun observeByFriend(friendId: String): Flow<List<SplitTransaction>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(transactions: List<SplitTransaction>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun save(transaction: SplitTransaction)
    @Query("DELETE FROM split_transactions WHERE id = :id") suspend fun delete(id: String)
}

@Database(entities = [Expense::class, Friend::class, SplitTransaction::class], version = EXPENSE_DATABASE_VERSION, exportSchema = false)
abstract class ExpenseDatabase : RoomDatabase() {
    abstract fun expenses(): ExpenseDao
    abstract fun friends(): FriendDao
    abstract fun splitTransactions(): SplitTransactionDao

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
            backupDatabaseBeforeDestructiveMigration(context)
            val database = Room.databaseBuilder(
                context.applicationContext,
                ExpenseDatabase::class.java,
                EXPENSE_DATABASE_NAME
            )
                .fallbackToDestructiveMigration(true)
                .build()

            return try {
                database.openHelper.writableDatabase
                database
            } catch (error: Throwable) {
                database.close()
                throw error
            }
        }

        /** Keep a recoverable copy before Room recreates tables for a missing migration. */
        private fun backupDatabaseBeforeDestructiveMigration(context: Context) {
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
