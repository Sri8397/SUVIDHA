package com.just_sri.suvidha.expenses

import org.json.JSONArray
import org.json.JSONObject

/** Canonical app-level envelope carried inside a Marmot MLS application message. */
internal object PeerSplitLedgerWire {
    private const val PROTOCOL = "suvidha.shared-ledger"
    private const val VERSION = 1

    suspend fun encrypt(manager: com.vitorpamplona.marmot.client.MarmotManager, event: SharedLedgerEvent): com.vitorpamplona.quartz.marmot.mip03GroupMessages.GroupEvent {
        val inner = manager.buildTextRumor(encode(event), null, null, emptyList())
        return manager.buildGroupMessage(event.groupId, inner, null).signedEvent
    }

    fun encode(event: SharedLedgerEvent): String {
        val (actionType, actionJson) = SharedLedgerCodec.encodeAction(event.action)
        val parents = JSONArray()
        event.parentEventIds.sorted().forEach(parents::put)
        return JSONObject()
            .put("protocol", PROTOCOL)
            .put("version", VERSION)
            .put("eventId", event.eventId)
            .put("groupId", event.groupId)
            .put("actorMemberId", event.actorMemberId)
            .put("occurredAtMillis", event.occurredAtMillis)
            .put("description", event.description.take(MAX_DESCRIPTION_LENGTH))
            .put("parentEventIds", parents)
            .put("actionType", actionType)
            .put("action", JSONObject(actionJson))
            .toString()
    }

    fun decode(content: String, groupId: String, authenticatedSender: String): SharedLedgerEvent? = runCatching {
        require(content.length <= MAX_MESSAGE_LENGTH) { "Shared ledger message is too large." }
        val json = JSONObject(content)
        require(json.getString("protocol") == PROTOCOL && json.get("version") == VERSION)
        require(json.getString("groupId") == groupId)
        val actor = json.getString("actorMemberId")
        require(actor == authenticatedSender) { "MLS sender identity does not match ledger author." }
        val parentsJson = json.getJSONArray("parentEventIds")
        val (type, payload) = SharedLedgerCodec.encodeAction(
            SharedLedgerCodec.decodeAction(json.getString("actionType"), json.getJSONObject("action").toString()),
        )
        SharedLedgerEvent(
            eventId = json.getString("eventId"),
            groupId = groupId,
            actorMemberId = actor,
            parentEventIds = List(parentsJson.length()) { parentsJson.getString(it) }.toSet(),
            occurredAtMillis = json.getStrictLong("occurredAtMillis"),
            action = SharedLedgerCodec.decodeAction(type, payload),
            description = json.optString("description", "").take(MAX_DESCRIPTION_LENGTH),
        )
    }.getOrNull()

    private const val MAX_MESSAGE_LENGTH = 128 * 1024
    private const val MAX_DESCRIPTION_LENGTH = 200
}
