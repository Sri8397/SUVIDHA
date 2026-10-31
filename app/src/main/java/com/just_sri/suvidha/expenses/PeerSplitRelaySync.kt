package com.just_sri.suvidha.expenses

import com.vitorpamplona.marmot.client.MarmotManager
import com.vitorpamplona.marmot.GroupEventResult
import com.vitorpamplona.marmot.WelcomeResult
import com.vitorpamplona.quartz.marmot.mip02Welcome.WelcomeEvent
import com.vitorpamplona.quartz.marmot.mip03GroupMessages.GroupEvent
import com.vitorpamplona.quartz.nip01Core.core.Event
import com.vitorpamplona.quartz.nip01Core.core.hexToByteArray
import com.vitorpamplona.quartz.nip01Core.crypto.EventHasher
import com.vitorpamplona.quartz.nip01Core.crypto.Nip01Crypto
import com.vitorpamplona.quartz.nip01Core.relay.normalizer.NormalizedRelayUrl
import com.vitorpamplona.quartz.nip59Giftwrap.seals.SealEvent
import com.vitorpamplona.quartz.nip59Giftwrap.wraps.GiftWrapEvent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

internal data class RelaySyncReport(
    val relaysResponded: Int,
    val groupEventsReceived: Int,
    val welcomesProcessed: Int,
    val groupEventsProcessed: Int,
    val errors: Int,
    val relayFailures: Int = 0,
)

internal data class StagedSyncBatch(
    val respondedRelays: MutableSet<String> = java.util.Collections.synchronizedSet(mutableSetOf()),
    val incomingEvents: MutableMap<String, Event> = java.util.Collections.synchronizedMap(linkedMapOf()),
    var initialGroupIds: List<String> = emptyList(),
    val newlyJoinedGroupIds: MutableList<String> = mutableListOf(),
    val newlyAppendedEvents: MutableList<SharedLedgerEvent> = mutableListOf(),
    var welcomesProcessed: Int = 0,
    var groupEventsProcessed: Int = 0,
    var errors: Int = 0,
    var relayFailures: Int = 0,
)

/** Manual Nostr relay transport. Relays see event timing, IP, and routing tags; payloads are encrypted. */
internal class PeerSplitRelaySync(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build(),
    private val relays: () -> List<NormalizedRelayUrl> = { DEFAULT_RELAYS },
) {
    fun relayUrls(): List<NormalizedRelayUrl> = relays()

    /** Round-trip time of a minimal query in milliseconds, or null when the relay is unreachable. */
    suspend fun probe(url: String, timeoutMs: Long = 8000L): Long? {
        val started: Long = System.nanoTime()
        return try {
            query(url, JSONObject().put("kinds", JSONArray().put(30443)).put("limit", 1), timeoutMs)
            (System.nanoTime() - started) / 1_000_000L
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
    }

    suspend fun publish(event: Event, relays: Set<NormalizedRelayUrl>): Boolean = coroutineScope {
        if (relays.isEmpty()) return@coroutineScope false
        relays.map { relay -> async { try { publishOne(relay.url, event) } catch (cancelled: CancellationException) { throw cancelled } catch (_: Exception) { false } } }
            .awaitAll().any { it }
    }

    suspend fun fetchParallelEvents(
        signerPubKey: String,
        initialGroupIds: List<String>,
        relayUrls: List<NormalizedRelayUrl> = relayUrls(),
        relayTimeoutMs: Long = 8000L,
    ): StagedSyncBatch {
        val staged = StagedSyncBatch(initialGroupIds = initialGroupIds)
        if (relayUrls.isEmpty()) return staged

        val giftWrapFilter = JSONObject()
            .put("kinds", JSONArray().put(GiftWrapEvent.KIND))
            .put("#p", JSONArray().put(signerPubKey))
            .put("limit", 500)

        val groupFilter = if (initialGroupIds.isNotEmpty()) {
            JSONObject()
                .put("kinds", JSONArray().put(GroupEvent.KIND))
                .put("#h", JSONArray(initialGroupIds))
                .put("limit", MAX_EVENTS_PER_FILTER)
        } else null

        coroutineScope {
            val wrapDeferreds = relayUrls.distinct().map { relay ->
                async(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val events = query(relay.url, giftWrapFilter, relayTimeoutMs)
                        staged.respondedRelays.add(relay.url)
                        events
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (failure: Exception) {
                        android.util.Log.w("PeerSplitSync", "Relay ${relay.url} failed: ${failure.message}")
                        null
                    }
                }
            }
            val groupDeferreds = if (groupFilter != null) {
                relayUrls.distinct().map { relay ->
                    async(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            val events = query(relay.url, groupFilter, relayTimeoutMs)
                            staged.respondedRelays.add(relay.url)
                            events
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (failure: Exception) {
                            android.util.Log.w("PeerSplitSync", "Relay ${relay.url} failed: ${failure.message}")
                            null
                        }
                    }
                }
            } else emptyList()

            wrapDeferreds.awaitAll().forEach { result ->
                if (result == null) {
                    staged.errors++
                    staged.relayFailures++
                } else {
                    result.forEach { event -> staged.incomingEvents.putIfAbsent(event.id, event) }
                }
            }
            groupDeferreds.awaitAll().forEach { result ->
                if (result == null) {
                    staged.errors++
                    staged.relayFailures++
                } else {
                    result.forEach { event -> staged.incomingEvents.putIfAbsent(event.id, event) }
                }
            }
        }
        return staged
    }

    suspend fun fetchAdditionalGroupEvents(
        groupIds: List<String>,
        staged: StagedSyncBatch,
        relayUrls: List<NormalizedRelayUrl> = relayUrls(),
        relayTimeoutMs: Long = 8000L,
    ) {
        if (groupIds.isEmpty() || relayUrls.isEmpty()) return
        val newGroupFilter = JSONObject()
            .put("kinds", JSONArray().put(GroupEvent.KIND))
            .put("#h", JSONArray(groupIds.distinct()))
            .put("limit", MAX_EVENTS_PER_FILTER)

        val results = coroutineScope {
            relayUrls.distinct().map { relay ->
                async(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        query(relay.url, newGroupFilter, relayTimeoutMs)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        null
                    }
                }
            }.awaitAll()
        }
        results.forEach { result ->
            result?.forEach { event -> staged.incomingEvents.putIfAbsent(event.id, event) }
        }
    }

    suspend fun sync(
        manager: MarmotManager,
        signer: com.vitorpamplona.quartz.nip01Core.signers.NostrSigner,
        relayUrls: List<NormalizedRelayUrl> = relayUrls(),
        knownEventIds: Set<String> = emptySet(),
        relayTimeoutMs: Long = 8000L,
        parallelQueries: Boolean = true,
        onLedgerEvent: suspend (SharedLedgerEvent, String) -> Unit = { _, _ -> },
        onWelcomeJoined: suspend (String) -> Unit = {},
        onMembersChanged: suspend (String, Set<String>, Set<String>) -> Unit = { _, _, _ -> },
    ): RelaySyncReport {
        if (relayUrls.isEmpty()) return RelaySyncReport(0, 0, 0, 0, 0)
        val responded = java.util.Collections.synchronizedSet(mutableSetOf<String>())
        var errors = 0
        var relayFailures = 0
        val incoming = java.util.Collections.synchronizedMap(linkedMapOf<String, Event>())

        val initialGroupIds = manager.activeGroupIds().sorted()
        val giftWrapFilter = JSONObject()
            .put("kinds", JSONArray().put(GiftWrapEvent.KIND))
            .put("#p", JSONArray().put(signer.pubKey))
            .put("limit", 500)

        val groupFilter = if (initialGroupIds.isNotEmpty()) {
            JSONObject()
                .put("kinds", JSONArray().put(GroupEvent.KIND))
                .put("#h", JSONArray(initialGroupIds))
                .put("limit", MAX_EVENTS_PER_FILTER)
        } else null

        // 1. Concurrently fetch GiftWraps and GroupEvents across relays
        val (giftWrapResults, groupEventResults) = coroutineScope {
            val wrapDeferreds = relayUrls.distinct().map { relay ->
                async(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val events = query(relay.url, giftWrapFilter, relayTimeoutMs)
                        responded.add(relay.url)
                        events
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        null
                    }
                }
            }
            val groupDeferreds = if (groupFilter != null) {
                relayUrls.distinct().map { relay ->
                    async(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            val events = query(relay.url, groupFilter, relayTimeoutMs)
                            responded.add(relay.url)
                            events
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            null
                        }
                    }
                }
            } else emptyList()

            wrapDeferreds.awaitAll() to groupDeferreds.awaitAll()
        }

        giftWrapResults.forEach { result ->
            if (result == null) {
                errors++
                relayFailures++
            } else {
                result.forEach { event -> incoming.putIfAbsent(event.id, event) }
            }
        }
        groupEventResults.forEach { result ->
            if (result == null) {
                errors++
                relayFailures++
            } else {
                result.forEach { event -> incoming.putIfAbsent(event.id, event) }
            }
        }

        // 2. Process Welcomes in causal order
        var welcomesProcessed = 0
        val newlyJoinedGroupIds = mutableListOf<String>()
        val giftWrapEvents = incoming.values.filterIsInstance<GiftWrapEvent>()
            .sortedWith(compareBy<Event> { it.createdAt }.thenBy { it.id })

        for (event in giftWrapEvents) {
            try {
                if (event.recipientKeys().none { it == signer.pubKey }) continue
                val seal = event.unwrapOrNull(signer) as? SealEvent ?: continue
                if (!isValidNostrEvent(seal)) { errors++; continue }
                val rumor = seal.unsealRumorThrowing(signer)
                if (rumor.pubKey != seal.pubKey) { errors++; continue }
                val welcome = seal.unsealed(rumor) as? WelcomeEvent ?: continue
                val outcome = manager.processWelcome(welcome)
                if (outcome is WelcomeResult.Joined) {
                    onWelcomeJoined(outcome.nostrGroupId)
                    welcomesProcessed++
                    if (outcome.nostrGroupId !in initialGroupIds) {
                        newlyJoinedGroupIds.add(outcome.nostrGroupId)
                    }
                }
                if (outcome is WelcomeResult.Error) errors++
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                errors++
            }
        }

        // 3. Newly received Welcomes may introduce groups requiring an additional event fetch
        if (newlyJoinedGroupIds.isNotEmpty()) {
            val newGroupFilter = JSONObject()
                .put("kinds", JSONArray().put(GroupEvent.KIND))
                .put("#h", JSONArray(newlyJoinedGroupIds.distinct()))
                .put("limit", MAX_EVENTS_PER_FILTER)

            val additionalResults = coroutineScope {
                relayUrls.distinct().map { relay ->
                    async(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            query(relay.url, newGroupFilter, relayTimeoutMs)
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            null
                        }
                    }
                }.awaitAll()
            }
            additionalResults.forEach { result ->
                result?.forEach { event -> incoming.putIfAbsent(event.id, event) }
            }
        }

        // 4. Process MLS group events in causal order
        var groupProcessed = 0
        val sortedGroupEvents = incoming.values.filterIsInstance<GroupEvent>()
            .filter { it.id !in knownEventIds }
            .sortedWith(compareBy<GroupEvent> { it.createdAt }.thenBy { it.id })

        for (event in sortedGroupEvents) {
            try {
                val result = manager.processGroupEvent(event)
                if (result is GroupEventResult.Duplicate) continue
                if (result is GroupEventResult.UndecryptableOuterLayer || result is GroupEventResult.RefusedByLifecycle) {
                    // Old-epoch or pre-join events are refetched on every sync; they are not actionable failures.
                    android.util.Log.w("PeerSplitSync", "Skipped group event ${event.id}: ${result::class.simpleName}")
                    continue
                }
                if (result is GroupEventResult.Error) {
                    android.util.Log.w("PeerSplitSync", "Group event ${event.id} failed: $result")
                    errors++
                    continue
                }
                val groupId = event.groupId() ?: continue
                if (result is GroupEventResult.ApplicationMessage) {
                    val resultGroupId = result.groupId
                    val authenticatedSender = manager.groupManager.memberIdentityHex(resultGroupId, result.senderLeafIndex) ?: continue
                    val innerEvent = Event.fromJson(result.innerEventJson)
                    PeerSplitLedgerWire.decode(innerEvent.content, resultGroupId, authenticatedSender)?.let {
                        onLedgerEvent(it, event.toJson())
                    }
                }
                val members = manager.memberPubkeys(groupId).mapTo(linkedSetOf()) { it.pubkey }
                val admins = manager.groupView(groupId)?.adminPubkeys?.toSet().orEmpty()
                onMembersChanged(groupId, members, admins)
                groupProcessed++
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                errors++
            }
        }
        return RelaySyncReport(
            relaysResponded = responded.size,
            groupEventsReceived = incoming.values.count { it is GroupEvent },
            welcomesProcessed = welcomesProcessed,
            groupEventsProcessed = groupProcessed,
            errors = errors,
            relayFailures = relayFailures,
        )
    }

    suspend fun fetchKeyPackageEvents(pubKeyHex: String, relayUrls: List<NormalizedRelayUrl> = relayUrls()): List<Event> = coroutineScope {
        val author = pubKeyHex.lowercase()
        require(author.matches(Regex("[0-9a-f]{64}"))) { "Enter a 64-character Nostr public key in hex." }
        relayUrls.distinct().map { relay -> async {
            try {
                query(relay.url, JSONObject()
                    .put("kinds", JSONArray().put(30443).put(443))
                    .put("authors", JSONArray().put(author))
                    .put("limit", 20))
            } catch (cancelled: CancellationException) { throw cancelled } catch (_: Exception) { emptyList() }
        } }.awaitAll().flatten()
            .filter(::isValidNostrEvent)
            .distinctBy(Event::id)
            .sortedByDescending(Event::createdAt)
    }

    private suspend fun publishOne(relay: String, event: Event): Boolean = withTimeoutOrNull(12_000) {
        val result = CompletableDeferred<Boolean>()
        val request = Request.Builder().url(validateRelay(relay)).build()
        var socket: WebSocket? = null
        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send(JSONArray().put("EVENT").put(JSONObject(event.toJson())).toString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                runCatching {
                    val message = JSONArray(text)
                    if (message.optString(0) == "OK" && message.optString(1) == event.id) {
                        result.complete(message.optBoolean(2, false))
                        webSocket.close(1000, "publish complete")
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                result.complete(false)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) { result.complete(false) }
        })
        try { result.await() } finally { socket.cancel() }
    } ?: false

    private suspend fun query(relay: String, filter: JSONObject, timeoutMs: Long = 18_000): List<Event> = withTimeoutOrNull(timeoutMs) {
        val subscriptionId = "suvidha-${UUID.randomUUID()}"
        val result = CompletableDeferred<List<Event>>()
        val events = mutableListOf<Event>()
        val request = Request.Builder().url(validateRelay(relay)).build()
        var socket: WebSocket? = null
        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send(JSONArray().put("REQ").put(subscriptionId).put(filter).toString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                runCatching {
                    val message = JSONArray(text)
                    when (message.optString(0)) {
                        "EVENT" -> if (message.optString(1) == subscriptionId && events.size < MAX_EVENTS_PER_FILTER) {
                            val event = Event.fromJson(message.getJSONObject(2).toString())
                            if (isValidNostrEvent(event)) synchronized(events) { events.add(event) }
                        }
                        "EOSE" -> if (message.optString(1) == subscriptionId) {
                            result.complete(synchronized(events) { events.toList() })
                            webSocket.send(JSONArray().put("CLOSE").put(subscriptionId).toString())
                            webSocket.close(1000, "sync complete")
                        }
                        "CLOSED" -> if (message.optString(1) == subscriptionId) result.completeExceptionally(IllegalStateException("Relay closed the subscription."))
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                result.completeExceptionally(t)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (!result.isCompleted) result.complete(synchronized(events) { events.toList() })
            }
        })
        try { result.await() } finally { socket.cancel() }
    } ?: throw java.io.IOException("Relay query timed out")

    private fun validateRelay(url: String): String {
        require(url.startsWith("wss://") || url.startsWith("ws://")) { "Relay URL must use ws:// or wss://." }
        val parsed = java.net.URI(url)
        require(!parsed.host.isNullOrBlank() && parsed.userInfo == null && parsed.fragment == null) { "Relay URL is invalid." }
        return url
    }

    internal companion object {
        const val MAX_EVENTS_PER_FILTER = 2000
        val DEFAULT_RELAYS = RelaySettings.DEFAULT_URLS.map { NormalizedRelayUrl(it) }

        fun isValidNostrEvent(event: Event): Boolean {
            if (event.id.length != 64 || event.pubKey.length != 64 || event.sig.length != 128) return false
            return runCatching {
                EventHasher.hashIdCheck(event.id, event.pubKey, event.createdAt, event.kind, event.tags, event.content) &&
                    Nip01Crypto.verify(event.sig.hexToByteArray(), event.id.hexToByteArray(), event.pubKey.hexToByteArray())
            }.getOrDefault(false)
        }
    }
}
