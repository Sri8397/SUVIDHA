package com.just_sri.suvidha.expenses

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.math.BigDecimal
import java.util.UUID

data class FriendGroupTransaction(
    val debtUnitId: String,
    val eventId: String?,
    val groupId: String,
    val groupName: String,
    val currencyCode: String,
    val description: String,
    val occurredAtMillis: Long,
    val amount: Double,
    val originalAmount: Double,
    val remainingAmount: Double,
    val isSettled: Boolean,
    val remainingAmountMinor: Long,
    val originalAmountMinor: Long,
)

internal fun computeFriendGroupTransactions(
    groups: List<SharedGroupEntity>,
    members: List<SharedGroupMemberEntity>,
    events: List<SharedLedgerEventRecord>,
    links: List<SharedMemberFriendLinkEntity>,
    actorMemberId: String?,
): Map<String, List<FriendGroupTransaction>> {
    if (actorMemberId.isNullOrBlank() || groups.isEmpty() || links.isEmpty()) return emptyMap()

    val memberToFriend = links.associate { it.memberId to it.friendId }
    val membersByGroup = members.groupBy { it.groupId }
    val eventsByGroup = events.groupBy { it.groupId }
    val result = mutableMapOf<String, MutableList<FriendGroupTransaction>>()

    groups.forEach { group ->
        val groupMembers = membersByGroup[group.id].orEmpty()
        val ownerMemberId = groupMembers.find { it.role == "owner" && it.isActive }?.memberId
        val groupEvents = eventsByGroup[group.id].orEmpty()
        val domainEvents = groupEvents.mapNotNull { runCatching { SharedLedgerCodec.toDomainEvent(it) }.getOrNull() }
        val projection = projectSharedLedger(group.id, domainEvents, group.currencyCode, ownerMemberId)
        val digits = runCatching { SharedSplitMoney.digits(group.currencyCode) }.getOrDefault(2)

        projection.debtById.values.forEach { debt ->
            val isCreditor = debt.creditorMemberId == actorMemberId
            val isDebtor = debt.debtorMemberId == actorMemberId
            if (!isCreditor && !isDebtor) return@forEach

            val counterpartMemberId = if (isCreditor) debt.debtorMemberId else debt.creditorMemberId
            val friendId = memberToFriend[counterpartMemberId] ?: return@forEach

            val parentEvent = domainEvents.find { (it.action as? SharedLedgerAction.ExpenseAdded)?.debts?.any { d -> d.id == debt.debtUnitId } == true }
            val rawDescription = parentEvent?.description?.trim().orEmpty().ifBlank { "Shared expense" }
            val occurredAtMillis = parentEvent?.occurredAtMillis ?: System.currentTimeMillis()
            val originalAmount = BigDecimal.valueOf(if (isCreditor) debt.originalAmountMinor else -debt.originalAmountMinor, digits).toDouble()
            val remainingAmount = BigDecimal.valueOf(if (isCreditor) debt.remainingAmountMinor else -debt.remainingAmountMinor, digits).toDouble()
            val isSettled = debt.remainingAmountMinor <= 0L

            result.getOrPut(friendId) { mutableListOf() }.add(
                FriendGroupTransaction(
                    debtUnitId = debt.debtUnitId,
                    eventId = parentEvent?.eventId,
                    groupId = group.id,
                    groupName = group.name,
                    currencyCode = group.currencyCode,
                    description = rawDescription,
                    occurredAtMillis = occurredAtMillis,
                    amount = if (isSettled) originalAmount else remainingAmount,
                    originalAmount = originalAmount,
                    remainingAmount = remainingAmount,
                    isSettled = isSettled,
                    remainingAmountMinor = debt.remainingAmountMinor,
                    originalAmountMinor = debt.originalAmountMinor,
                )
            )
        }
    }
    return result
}

/** Owns ledger writes so the UI cannot leave a split and its settlement record half-saved. */
internal class ExpenseRepository(private val database: ExpenseDatabase) {
    private val expenseDao = database.expenses()
    private val friendDao = database.friends()
    private val splitDao = database.splitTransactions()

    fun observeExpenses(): Flow<List<Expense>> = expenseDao.observeAll()

    fun observeSplits(): Flow<List<SplitTransaction>> = splitDao.observeAll()

    fun observeFriendGroupTransactions(actorMemberId: String? = null): Flow<Map<String, List<FriendGroupTransaction>>> =
        combine(
            database.sharedGroups().observeAll(),
            database.sharedGroupMembers().observeAll(),
            database.sharedLedgerEvents().observeAll(),
            database.sharedMemberFriendLinks().observeAll(),
        ) { groups, members, events, links ->
            computeFriendGroupTransactions(groups, members, events, links, actorMemberId)
        }

    fun observeFriendsWithBalances(actorMemberId: String? = null): Flow<List<Friend>> =
        combine(friendDao.observeAll(), splitDao.observeAll(), observeFriendGroupTransactions(actorMemberId)) { friends, splits, groupTxns ->
            val unsettledByFriend = splits.asSequence()
                .filterNot(SplitTransaction::isSettled)
                .groupBy(SplitTransaction::friendId)
            friends.map { friend ->
                val personalUnsettled = unsettledByFriend[friend.id]?.sumOf(SplitTransaction::amount) ?: 0.0
                val groupUnsettled = groupTxns[friend.id]?.filterNot(FriendGroupTransaction::isSettled)?.sumOf(FriendGroupTransaction::remainingAmount) ?: 0.0
                friend.withAmount(personalUnsettled + groupUnsettled)
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
