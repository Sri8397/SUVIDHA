package com.just_sri.suvidha.expenses

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.UUID

/** Owns ledger writes so the UI cannot leave a split and its settlement record half-saved. */
internal class ExpenseRepository(private val database: ExpenseDatabase) {
    private val expenseDao = database.expenses()
    private val friendDao = database.friends()
    private val splitDao = database.splitTransactions()

    fun observeExpenses(): Flow<List<Expense>> = expenseDao.observeAll()

    fun observeSplits(): Flow<List<SplitTransaction>> = splitDao.observeAll()

    fun observeFriendsWithBalances(): Flow<List<Friend>> =
        combine(friendDao.observeAll(), splitDao.observeAll()) { friends, splits ->
            val unsettledByFriend = splits.asSequence()
                .filterNot(SplitTransaction::isSettled)
                .groupBy(SplitTransaction::friendId)
            friends.map { friend ->
                friend.withAmount(unsettledByFriend[friend.id]?.sumOf(SplitTransaction::amount) ?: 0.0)
            }
        }

    suspend fun replaceFriends(previous: List<Friend>, updated: List<Friend>) = database.withTransaction {
        val updatedIds = updated.mapTo(mutableSetOf(), Friend::id)
        previous.filterNot { it.id in updatedIds }.forEach { friendDao.delete(it.id) }
        updated.forEach { friend -> friendDao.save(friend) }
    }

    suspend fun saveFriend(friend: Friend) = friendDao.save(friend)

    suspend fun deleteFriend(id: String) = friendDao.delete(id)

    suspend fun addExpense(draft: ExpenseDraft) {
        expenseDao.insert(
            Expense(
                id = UUID.randomUUID().toString(),
                category = draft.category,
                amount = draft.amount.toDouble(),
                description = draft.note.ifBlank { draft.category },
                date = System.currentTimeMillis(),
            )
        )
    }

    suspend fun deleteExpense(id: String) = expenseDao.delete(id)

    suspend fun saveSplitTransactions(
        entries: List<SplitDraft>,
        friendsById: Map<String, Friend>,
        currentFriends: List<Friend>,
    ) = database.withTransaction {
        val now = System.currentTimeMillis()
        splitDao.insertAll(
            entries.map { entry ->
                SplitTransaction(
                    id = UUID.randomUUID().toString(),
                    friendId = entry.friendId,
                    amount = entry.amount.toDouble(),
                    note = entry.note.trim(),
                    date = now,
                )
            }
        )
        entries.asSequence().map(SplitDraft::friendId).distinct().forEach { friendId ->
            val friend = friendsById[friendId] ?: currentFriends.firstOrNull { it.id == friendId }
            if (friend != null) friendDao.save(friend.copy(updatedAt = now))
        }
    }

    suspend fun deleteSplitTransaction(transaction: SplitTransaction, friends: List<Friend>) =
        database.withTransaction {
            splitDao.delete(transaction.id)
            friends.firstOrNull { it.id == transaction.friendId }?.let { friend ->
                friendDao.save(friend.copy(updatedAt = System.currentTimeMillis()))
            }
        }

    suspend fun settleSplitTransaction(
        transaction: SplitTransaction,
        friendName: String,
        friends: List<Friend>,
    ) = database.withTransaction {
        val current = splitDao.findById(transaction.id) ?: return@withTransaction
        if (current.isSettled) return@withTransaction

        val now = System.currentTimeMillis()
        splitDao.save(current.copy(isSettled = true, settledAt = now))
        friends.firstOrNull { it.id == current.friendId }?.let { friend ->
            friendDao.save(friend.copy(updatedAt = now))
        }

        if (current.amount != 0.0) {
            val isOwed = current.amount > 0
            expenseDao.insert(
                Expense(
                    id = UUID.randomUUID().toString(),
                    category = if (isOwed) "Earn" else "Other",
                    amount = kotlin.math.abs(current.amount),
                    description = "Settled ${if (isOwed) "by" else "to"} $friendName: ${current.note.ifBlank { "Split" }}",
                    date = now,
                )
            )
        }
    }
}
