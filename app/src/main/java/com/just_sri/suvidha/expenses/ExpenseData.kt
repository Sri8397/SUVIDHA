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

@Dao
interface FriendBalanceDao {
    @Query("SELECT * FROM friend_balances ORDER BY name COLLATE NOCASE") fun observeAll(): Flow<List<FriendBalance>>
    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE) suspend fun save(balance: FriendBalance)
    @Query("DELETE FROM friend_balances WHERE id = :id") suspend fun delete(id: String)
}

@Database(entities = [Expense::class, FriendBalance::class], version = 2, exportSchema = false)
abstract class ExpenseDatabase : RoomDatabase() {
    abstract fun expenses(): ExpenseDao
    abstract fun friendBalances(): FriendBalanceDao

    companion object {
        @Volatile private var instance: ExpenseDatabase? = null
        private val migration1To2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS friend_balances (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, amount REAL NOT NULL, updatedAt INTEGER NOT NULL)")
            }
        }
        fun get(context: Context): ExpenseDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, ExpenseDatabase::class.java, "suvidha-expenses.db")
                .addMigrations(migration1To2).build().also { instance = it }
        }
    }
}

val defaultExpenseCategories = listOf("Rent", "Utilities", "Food", "Transport", "Health", "Shopping", "Salary", "Other")
