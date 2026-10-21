package com.just_sri.suvidha.expenses

import android.content.Context
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import androidx.room.PrimaryKey
import androidx.room.OnConflictStrategy

@Entity(tableName = "expenses")
data class Expense(
    @androidx.room.PrimaryKey val id: String,
    val category: String,
    val amount: Double,
    val description: String,
    val date: Long,
)

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY date DESC") fun observeAll(): Flow<List<Expense>>
    @Insert suspend fun insert(expense: Expense)
    @Insert suspend fun insertAll(expenses: List<Expense>)
    @Query("DELETE FROM expenses WHERE id = :id") suspend fun delete(id: String)
}

@Entity(tableName = "friend_balances")
data class FriendBalance(
    @androidx.room.PrimaryKey val id: String,
    val name: String,
    val amount: Double,
    val updatedAt: Long,
)

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
interface FriendBalanceDao {
    @Query("SELECT * FROM friend_balances ORDER BY name COLLATE NOCASE") fun observeAll(): Flow<List<FriendBalance>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun save(balance: FriendBalance)
    @Query("DELETE FROM friend_balances WHERE id = :id") suspend fun delete(id: String)
}

@Dao
interface SplitTransactionDao {
    @Query("SELECT * FROM split_transactions WHERE friendId = :friendId ORDER BY date DESC") fun observeByFriend(friendId: String): Flow<List<SplitTransaction>>
    @Insert suspend fun insert(transaction: SplitTransaction)
    @Insert suspend fun insertAll(transactions: List<SplitTransaction>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun save(transaction: SplitTransaction)
    @Query("DELETE FROM split_transactions WHERE id = :id") suspend fun delete(id: String)
    @Query("SELECT * FROM split_transactions WHERE id = :id") suspend fun getById(id: String): SplitTransaction?
}

@Database(entities = [Expense::class, FriendBalance::class, SplitTransaction::class], version = 5, exportSchema = false)
abstract class ExpenseDatabase : RoomDatabase() {
    abstract fun expenses(): ExpenseDao
    abstract fun friendBalances(): FriendBalanceDao
    abstract fun splitTransactions(): SplitTransactionDao

    companion object {
        @Volatile private var instance: ExpenseDatabase? = null
        private val migration1To2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS friend_balances (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, amount REAL NOT NULL, updatedAt INTEGER NOT NULL)")
            }
        }
        private val migration2To3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS split_transactions (id TEXT NOT NULL PRIMARY KEY, friendId TEXT NOT NULL, amount REAL NOT NULL, note TEXT NOT NULL, date INTEGER NOT NULL)")
            }
        }
        private val migration3To4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE split_transactions ADD COLUMN isSettled INTEGER NOT NULL DEFAULT 0")
            }
        }
        private val migration4To5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE split_transactions ADD COLUMN settledAt INTEGER")
            }
        }
        fun get(context: Context): ExpenseDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, ExpenseDatabase::class.java, "suvidha-expenses.db")
                .addMigrations(migration1To2, migration2To3, migration3To4, migration4To5).build().also { instance = it }
        }
    }
}

val defaultExpenseCategories = listOf("Rent", "Utilities", "Food", "Transport", "Health", "Shopping", "Salary", "Earn", "Other")
