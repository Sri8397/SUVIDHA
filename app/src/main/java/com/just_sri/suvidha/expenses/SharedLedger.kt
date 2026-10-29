package com.just_sri.suvidha.expenses

/** Monetary values in a shared ledger are stored in the currency's minor unit, never Double. */
data class SharedDebtUnit(
    val id: String,
    val creditorMemberId: String,
    val debtorMemberId: String,
    val amountMinor: Long,
)

data class SharedSettlementAllocation(
    val debtUnitId: String,
    val amountMinor: Long,
)

sealed interface SharedLedgerAction {
    /** Creates immutable debt units as the result of a shared expense. */
    data class ExpenseAdded(
        val currencyCode: String,
        val debts: List<SharedDebtUnit>,
    ) : SharedLedgerAction

    /** Settles exact debt units, optionally in part. All allocations apply atomically. */
    data class SettlementRecorded(val allocations: List<SharedSettlementAllocation>) : SharedLedgerAction
}

/** Local domain event. Authentication and encryption fields are intentionally added with their protocol. */
data class SharedLedgerEvent(
    val eventId: String,
    val groupId: String,
    val actorMemberId: String,
    val parentEventIds: Set<String>,
    val occurredAtMillis: Long,
    val action: SharedLedgerAction,
    val description: String = "",
)

data class SharedDebtBalance(
    val debtUnitId: String,
    val creditorMemberId: String,
    val debtorMemberId: String,
    val originalAmountMinor: Long,
    val remainingAmountMinor: Long,
)

data class SharedLedgerProjection(
    /** Positive means the member is owed money; negative means the member owes money. */
    val balanceByMemberId: Map<String, Long>,
    val debtById: Map<String, SharedDebtBalance>,
    val acceptedEventIds: Set<String>,
    val pendingEventIds: Set<String>,
    val rejectedEvents: Map<String, String>,
)

/**
 * Rebuilds one group's balances from immutable events. Causal parents are applied first;
 * concurrent ready events use event ID as a deterministic tie-breaker on every device.
 */
fun projectSharedLedger(
    groupId: String,
    events: Iterable<SharedLedgerEvent>,
    currencyCode: String,
    ownerMemberId: String? = null,
): SharedLedgerProjection {
    val rejected = linkedMapOf<String, String>()
    val byId = linkedMapOf<String, SharedLedgerEvent>()
    val conflicts = mutableSetOf<String>()

    events.forEach { event ->
        if (event.eventId.isBlank() || event.actorMemberId.isBlank()) {
            rejected[event.eventId] = "Event ID and actor member ID are required."
            return@forEach
        }
        if (event.groupId != groupId) {
            rejected[event.eventId] = "Event belongs to a different group."
            return@forEach
        }
        val prior = byId[event.eventId]
        if (prior == null) {
            byId[event.eventId] = event
        } else if (prior != event) {
            conflicts += event.eventId
        }
    }
    conflicts.forEach { id ->
        byId.remove(id)
        rejected[id] = "Conflicting events use the same event ID."
    }

    val pending = byId.values
        .filter { event -> event.parentEventIds.any { it !in byId && it !in rejected } }
        .mapTo(mutableSetOf(), SharedLedgerEvent::eventId)

    // Every descendant of a missing ancestor must wait too, rather than being
    // rejected merely because its immediate parent is also waiting.
    val descendants = mutableMapOf<String, MutableList<String>>()
    byId.values.forEach { event -> event.parentEventIds.forEach { parent -> descendants.getOrPut(parent, ::mutableListOf).add(event.eventId) } }
    val waiting = java.util.ArrayDeque(pending)
    while (waiting.isNotEmpty()) {
        descendants[waiting.removeFirst()].orEmpty().forEach { child -> if (pending.add(child)) waiting.addLast(child) }
    }

    val candidates = byId.values.filterNot { it.eventId in pending }
    val indegree = candidates.associate { event ->
        event.eventId to event.parentEventIds.count { parent -> candidates.any { it.eventId == parent } }
    }.toMutableMap()
    val children = mutableMapOf<String, MutableList<String>>()
    candidates.forEach { event ->
        event.parentEventIds.filter { it in indegree }.forEach { parent ->
            children.getOrPut(parent, ::mutableListOf).add(event.eventId)
        }
    }
    val ready = java.util.PriorityQueue<String>()
    indegree.filterValues { it == 0 }.keys.forEach(ready::add)
    val orderedIds = mutableListOf<String>()
    while (ready.isNotEmpty()) {
        val id = ready.remove()
        orderedIds += id
        children[id].orEmpty().forEach { child ->
            val nextDegree = indegree.getValue(child) - 1
            indegree[child] = nextDegree
            if (nextDegree == 0) ready.add(child)
        }
    }
    val cyclicIds = candidates.map(SharedLedgerEvent::eventId).toSet() - orderedIds.toSet()
    cyclicIds.forEach { rejected[it] = "Event ancestry contains a cycle." }

    val accepted = linkedSetOf<String>()
    val ancestorIds = mutableMapOf<String, Set<String>>()
    val debts = linkedMapOf<String, SharedDebtBalance>()

    orderedIds.forEach { id ->
        val event = byId.getValue(id)
        val invalidParent = event.parentEventIds.firstOrNull { it !in accepted }
        if (invalidParent != null) {
            rejected[id] = "Required parent event '$invalidParent' was not accepted."
            return@forEach
        }
        val ancestors = event.parentEventIds + event.parentEventIds.flatMap { ancestorIds[it].orEmpty() }

        when (val action = event.action) {
            is SharedLedgerAction.ExpenseAdded -> {
                val duplicateDebtIds = action.debts.groupBy(SharedDebtUnit::id).filterValues { it.size > 1 }.keys
                val invalidDebt = action.debts.firstOrNull { debt ->
                    debt.id.isBlank() || debt.creditorMemberId.isBlank() || debt.debtorMemberId.isBlank() ||
                        debt.creditorMemberId != event.actorMemberId || debt.creditorMemberId == debt.debtorMemberId || debt.amountMinor <= 0 ||
                        debt.id in debts || debt.id in duplicateDebtIds
                }
                if (currencyCode.isBlank() || action.currencyCode != currencyCode || invalidDebt != null) {
                    rejected[id] = if (invalidDebt != null) {
                        "Expense contains an invalid or duplicate debt unit."
                    } else if (action.currencyCode != currencyCode) {
                        "Expense currency does not match the group's currency."
                    } else {
                        "Group currency code is required."
                    }
                    return@forEach
                }
                action.debts.forEach { debt ->
                    debts[debt.id] = SharedDebtBalance(
                        debtUnitId = debt.id,
                        creditorMemberId = debt.creditorMemberId,
                        debtorMemberId = debt.debtorMemberId,
                        originalAmountMinor = debt.amountMinor,
                        remainingAmountMinor = debt.amountMinor,
                    )
                }
            }

            is SharedLedgerAction.SettlementRecorded -> {
                val allocationsByDebt = action.allocations.groupBy(SharedSettlementAllocation::debtUnitId)
                val invalidAllocation = action.allocations.firstOrNull { allocation ->
                    allocation.debtUnitId.isBlank() || allocation.amountMinor <= 0 ||
                        allocationsByDebt[allocation.debtUnitId]?.size != 1
                }
                val unavailableDebt = action.allocations.firstOrNull { allocation ->
                    val debt = debts[allocation.debtUnitId]
                    val isAuthorized = debt != null && (
                        event.actorMemberId in setOf(debt.creditorMemberId, debt.debtorMemberId) ||
                        (ownerMemberId != null && event.actorMemberId == ownerMemberId)
                    )
                    debt == null || !isAuthorized || debt.remainingAmountMinor < allocation.amountMinor ||
                        debt.debtUnitId.let { debtId ->
                            val creator = orderedIds.firstOrNull { parentId ->
                                (byId[parentId]?.action as? SharedLedgerAction.ExpenseAdded)
                                    ?.debts?.any { it.id == debtId } == true
                            }
                            creator == null || creator !in ancestors
                        }
                }
                if (action.allocations.isEmpty() || invalidAllocation != null || unavailableDebt != null) {
                    rejected[id] = when {
                        action.allocations.isEmpty() -> "Settlement must allocate at least one debt unit."
                        invalidAllocation != null -> "Settlement contains an invalid or duplicate allocation."
                        else -> "Settlement references an unavailable, non-causal, or over-allocated debt unit."
                    }
                    return@forEach
                }
                action.allocations.forEach { allocation ->
                    val debt = debts.getValue(allocation.debtUnitId)
                    debts[allocation.debtUnitId] = debt.copy(
                        remainingAmountMinor = debt.remainingAmountMinor - allocation.amountMinor,
                    )
                }
            }
        }
        accepted += id
        ancestorIds[id] = ancestors
    }

    val balances = mutableMapOf<String, Long>()
    debts.values.forEach { debt ->
        if (debt.remainingAmountMinor > 0) {
            balances[debt.creditorMemberId] = Math.addExact(
                balances[debt.creditorMemberId] ?: 0L,
                debt.remainingAmountMinor,
            )
            balances[debt.debtorMemberId] = Math.subtractExact(
                balances[debt.debtorMemberId] ?: 0L,
                debt.remainingAmountMinor,
            )
        }
    }

    return SharedLedgerProjection(
        balanceByMemberId = balances.filterValues { it != 0L },
        debtById = debts,
        acceptedEventIds = accepted,
        pendingEventIds = pending,
        rejectedEvents = rejected,
    )
}
