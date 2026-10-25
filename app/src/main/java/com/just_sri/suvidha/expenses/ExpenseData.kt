package com.just_sri.suvidha.expenses

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
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
    @Insert suspend fun insertAll(expenses: List<Expense>)
    @Query("DELETE FROM expenses WHERE id = :id") suspend fun delete(id: String)
}

@Entity(tableName = "friend_balances")
data class FriendBalance(
    @PrimaryKey val id: String,
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
    @Insert suspend fun insertAll(transactions: List<SplitTransaction>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun save(transaction: SplitTransaction)
    @Query("DELETE FROM split_transactions WHERE id = :id") suspend fun delete(id: String)
}

@Database(entities = [Expense::class, FriendBalance::class, SplitTransaction::class], version = 5, exportSchema = false)
abstract class ExpenseDatabase : RoomDatabase() {
    abstract fun expenses(): ExpenseDao
    abstract fun friendBalances(): FriendBalanceDao
    abstract fun splitTransactions(): SplitTransactionDao

    companion object {
        @Volatile private var instance: ExpenseDatabase? = null
        fun get(context: Context): ExpenseDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, ExpenseDatabase::class.java, "suvidha-expenses.db")
                .fallbackToDestructiveMigration(true)
                .build().also { instance = it }
        }
    }
}

val defaultExpenseCategories = listOf("Rent", "Utilities", "Food", "Transport", "Health", "Shopping", "Salary", "Earn", "Other")

/** Draft rows produced by the full-screen add/split flows before they are persisted. */
internal data class ExpenseDraft(val category: String = "Other", val amount: String = "", val note: String = "")
internal data class SplitDraft(val friendId: String, val amount: String = "", val note: String = "")
