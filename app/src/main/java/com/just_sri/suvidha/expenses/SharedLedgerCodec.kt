package com.just_sri.suvidha.expenses

import org.json.JSONArray
import org.json.JSONObject

/** Versioned local payload encoding. It is storage format only, not a signing/canonical wire format. */
internal object SharedLedgerCodec {
    fun encodeAction(action: SharedLedgerAction): Pair<String, String> = when (action) {
        is SharedLedgerAction.ExpenseAdded -> {
            val debts = JSONArray()
            action.debts.forEach { debt ->
                debts.put(
                    JSONObject()
                        .put("id", debt.id)
                        .put("creditorMemberId", debt.creditorMemberId)
                        .put("debtorMemberId", debt.debtorMemberId)
                        .put("amountMinor", debt.amountMinor)
                )
            }
            "expense_added" to JSONObject()
                .put("currencyCode", action.currencyCode)
                .put("debts", debts)
                .toString()
        }

        is SharedLedgerAction.DebtRemoved -> "debt_removed" to JSONObject().put("debtUnitId", action.debtUnitId).toString()

        is SharedLedgerAction.SettlementRecorded -> {
            val allocations = JSONArray()
            action.allocations.forEach { allocation ->
                allocations.put(
                    JSONObject()
                        .put("debtUnitId", allocation.debtUnitId)
                        .put("amountMinor", allocation.amountMinor)
                )
            }
            "settlement_recorded" to JSONObject()
                .put("allocations", allocations)
                .toString()
        }

        is SharedLedgerAction.GroupRenamed -> "group_renamed" to JSONObject().put("name", action.name.take(MAX_GROUP_NAME_LENGTH)).toString()

        is SharedLedgerAction.ChatMessage -> "chat_message" to JSONObject().put("text", action.text.take(MAX_CHAT_LENGTH)).toString()
    }

    const val MAX_CHAT_LENGTH = 2000
    const val MAX_GROUP_NAME_LENGTH = 80

    fun decodeAction(actionType: String, payloadJson: String): SharedLedgerAction {
        val payload = JSONObject(payloadJson)
        return when (actionType) {
            "expense_added" -> {
                val debts = payload.getJSONArray("debts")
                SharedLedgerAction.ExpenseAdded(
                    currencyCode = payload.getString("currencyCode"),
                    debts = List(debts.length()) { index ->
                        val debt = debts.getJSONObject(index)
                        SharedDebtUnit(
                            id = debt.getString("id"),
                            creditorMemberId = debt.getString("creditorMemberId"),
                            debtorMemberId = debt.getString("debtorMemberId"),
                            amountMinor = debt.getStrictLong("amountMinor"),
                        )
                    },
                )
            }

            "debt_removed" -> SharedLedgerAction.DebtRemoved(payload.getString("debtUnitId"))

            "settlement_recorded" -> {
                val allocations = payload.getJSONArray("allocations")
                SharedLedgerAction.SettlementRecorded(
                    allocations = List(allocations.length()) { index ->
                        val allocation = allocations.getJSONObject(index)
                        SharedSettlementAllocation(
                            debtUnitId = allocation.getString("debtUnitId"),
                            amountMinor = allocation.getStrictLong("amountMinor"),
                        )
                    },
                )
            }

            "chat_message" -> SharedLedgerAction.ChatMessage(payload.getString("text").take(MAX_CHAT_LENGTH))

            "group_renamed" -> SharedLedgerAction.GroupRenamed(payload.getString("name").take(MAX_GROUP_NAME_LENGTH))

            else -> throw IllegalArgumentException("Unsupported shared ledger action: $actionType")
        }
    }

    fun encodeParents(parentEventIds: Set<String>): String =
        JSONArray().also { array -> parentEventIds.sorted().forEach(array::put) }.toString()

    fun decodeParents(parentEventIdsJson: String): Set<String> {
        val array = JSONArray(parentEventIdsJson)
        return List(array.length()) { index -> array.getString(index) }.toSet()
    }

    fun toDomainEvent(record: SharedLedgerEventRecord): SharedLedgerEvent = SharedLedgerEvent(
        eventId = record.id,
        groupId = record.groupId,
        actorMemberId = record.actorMemberId,
        parentEventIds = decodeParents(record.parentEventIdsJson),
        occurredAtMillis = record.createdAtMillis,
        action = decodeAction(record.actionType, record.payloadJson),
        description = record.description,
    )
}

/** JSONObject.getLong coerces fractional numbers and numeric strings; money must be an integer. */
internal fun JSONObject.getStrictLong(name: String): Long {
    val value = get(name)
    require(value is Int || value is Long) { "$name must be an integer." }
    return (value as Number).toLong()
}
