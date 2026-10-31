package com.just_sri.suvidha.expenses.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.just_sri.suvidha.expenses.ExpenseDatabase
import com.just_sri.suvidha.expenses.NostrIdentity
import com.just_sri.suvidha.expenses.NotificationService
import com.just_sri.suvidha.expenses.PeerSplitLedgerWire
import com.just_sri.suvidha.expenses.PeerSplitRelaySync
import com.just_sri.suvidha.expenses.PeerSplitService
import com.just_sri.suvidha.expenses.PeerSplitSyncCoordinator
import com.just_sri.suvidha.expenses.PeerSplitSyncResult
import com.just_sri.suvidha.expenses.PeerSplitSyncScheduler
import com.just_sri.suvidha.expenses.RelaySettings
import com.just_sri.suvidha.expenses.RelaySyncReport
import com.just_sri.suvidha.expenses.SharedGroupRepository
import com.just_sri.suvidha.expenses.SharedLedgerAction
import com.just_sri.suvidha.expenses.SharedLedgerEvent
import com.just_sri.suvidha.expenses.SharedSplitMoney
import com.just_sri.suvidha.expenses.StagedSyncBatch
import com.just_sri.suvidha.expenses.currencySymbol
import com.just_sri.suvidha.expenses.engine.ActionRegistry
import com.just_sri.suvidha.expenses.engine.ActionResult
import com.just_sri.suvidha.expenses.engine.ConditionRegistry
import com.just_sri.suvidha.expenses.engine.ExecutionResult
import com.just_sri.suvidha.expenses.engine.Rule
import com.just_sri.suvidha.expenses.engine.RuleContext
import com.just_sri.suvidha.expenses.engine.RuleEngine
import com.just_sri.suvidha.expenses.engine.SyncConfig
import com.just_sri.suvidha.expenses.engine.ValidationResult
import com.vitorpamplona.marmot.GroupEventResult
import com.vitorpamplona.marmot.WelcomeResult
import com.vitorpamplona.marmot.client.MarmotManager
import com.vitorpamplona.quartz.marmot.mip02Welcome.WelcomeEvent
import com.vitorpamplona.quartz.marmot.mip03GroupMessages.GroupEvent
import com.vitorpamplona.quartz.nip01Core.core.Event
import com.vitorpamplona.quartz.nip01Core.relay.normalizer.NormalizedRelayUrl
import com.vitorpamplona.quartz.nip59Giftwrap.seals.SealEvent
import com.vitorpamplona.quartz.nip59Giftwrap.wraps.GiftWrapEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException

internal class RefreshService(
    yamlContent: String,
    private val appContext: Context,
    private val database: ExpenseDatabase,
    private val repository: SharedGroupRepository,
    private val peers: PeerSplitService,
    private val relay: PeerSplitRelaySync,
    private val coordinator: PeerSplitSyncCoordinator = PeerSplitSyncCoordinator(peers),
    private val config: SyncConfig = SyncConfig(),
) {
    constructor(
        context: Context,
        database: ExpenseDatabase,
        peers: PeerSplitService,
    ) : this(
        yamlContent = readRulesYaml(context),
        appContext = context.applicationContext,
        database = database,
        repository = SharedGroupRepository(database),
        peers = peers,
        relay = RelaySettings(context).let { settings -> PeerSplitRelaySync(relays = { settings.urls().map { NormalizedRelayUrl(it) } }) },
        coordinator = PeerSplitSyncCoordinator(peers),
    )

    val conditionRegistry: ConditionRegistry = ConditionRegistry()
    val actionRegistry: ActionRegistry = ActionRegistry()
    val engine: RuleEngine = RuleEngine(conditionRegistry, actionRegistry)

    init {
        registerConditions()
        registerActions()
        engine.loadYaml(yamlContent, validateRegistries = true)
    }

    suspend fun manualPullRefresh(
        setBusy: (Boolean) -> Unit,
        setMessage: (String) -> Unit,
        isBusy: () -> Boolean,
    ): ExecutionResult {
        val context = RuleContext(
            attributes = mutableMapOf(
                "set_busy" to setBusy,
                "set_message" to setMessage,
                "is_busy" to isBusy,
                "rule_message" to engine.getRule("manual_pull_refresh")?.message,
            )
        )
        // A rejected pull (already busy) runs no actions, so it must not queue behind the protocol lock.
        if (isBusy()) return engine.execute("manual_pull_refresh", context)
        // Fetch, MLS processing and roster updates must not interleave with other group operations.
        return PeerSplitService.protocolMutex.withLock { engine.execute("manual_pull_refresh", context) }
    }

    /** Runs the background sync rule. The caller must hold [PeerSplitService.protocolMutex]. */
    private suspend fun runBackgroundSync(): PeerSplitSyncResult {
        val context = RuleContext()
        return when (val result = engine.execute("background_idle_sync", context)) {
            is ExecutionResult.Success -> syncResult(context.attributes["staged_batch"] as? StagedSyncBatch)
            is ExecutionResult.ValidationFailed -> throw IllegalStateException(result.violation.reason)
            is ExecutionResult.ActionFailed -> throw IOException(result.reason)
        }
    }

    suspend fun scheduledRetrySync(): ExecutionResult {
        val context = RuleContext()
        return engine.execute("scheduled_retry_sync", context)
    }

    fun getRule(ruleId: String): Rule? = engine.getRule(ruleId)

    private suspend fun hasPendingUploads(): Boolean = repository.pendingWelcomes().isNotEmpty() || database.sharedGroups().getAll().any {
        repository.pendingEncryptedEvents(it.id).isNotEmpty()
    }

    private suspend fun syncResult(staged: StagedSyncBatch?): PeerSplitSyncResult = PeerSplitSyncResult(
        RelaySyncReport(
            relaysResponded = staged?.respondedRelays?.size ?: 0,
            groupEventsReceived = staged?.incomingEvents?.values?.count { it is GroupEvent } ?: 0,
            welcomesProcessed = staged?.welcomesProcessed ?: 0,
            groupEventsProcessed = staged?.groupEventsProcessed ?: 0,
            errors = staged?.errors ?: 0,
            relayFailures = staged?.relayFailures ?: 0,
        ),
        hasPendingUploads(),
    )

    private fun registerConditions() {
        conditionRegistry.register("is_not_busy") { context ->
            @Suppress("UNCHECKED_CAST")
            val isBusyFunc = context.attributes["is_busy"] as? (() -> Boolean)
            val busy = isBusyFunc?.invoke() ?: false
            if (!busy) ValidationResult.Allowed else ValidationResult.Denied("is_not_busy", "A sync operation is already in progress.")
        }

        conditionRegistry.register("background_sync_allowed") {
            ValidationResult.Allowed
        }

        conditionRegistry.register("has_pending_uploads_or_relay_failures") {
            val hasPending = withContext(Dispatchers.IO) {
                repository.pendingWelcomes().isNotEmpty() || database.sharedGroups().getAll().any {
                    repository.pendingEncryptedEvents(it.id).isNotEmpty()
                }
            }
            if (hasPending) ValidationResult.Allowed else ValidationResult.Denied("has_pending_uploads_or_relay_failures", "No pending uploads.")
        }

        conditionRegistry.register("network_connected") {
            val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = connectivityManager?.activeNetwork
            val caps = connectivityManager?.getNetworkCapabilities(network)
            val connected = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            if (connected) ValidationResult.Allowed else ValidationResult.Denied("network_connected", "Network unavailable.")
        }

        conditionRegistry.register("event_is_newly_inserted") { context ->
            val isNew = (context.attributes["is_new_event"] as? Boolean) == true
            if (isNew) ValidationResult.Allowed else ValidationResult.Denied("event_is_newly_inserted", "Event is not newly inserted.")
        }

        conditionRegistry.register("event_is_expense_added") { context ->
            val event = context.attributes["ledger_event"] as? SharedLedgerEvent
            if (event?.action is SharedLedgerAction.ExpenseAdded) ValidationResult.Allowed else ValidationResult.Denied("event_is_expense_added", "Not an ExpenseAdded event.")
        }

        conditionRegistry.register("event_is_settlement_recorded") { context ->
            val event = context.attributes["ledger_event"] as? SharedLedgerEvent
            if (event?.action is SharedLedgerAction.SettlementRecorded) ValidationResult.Allowed else ValidationResult.Denied("event_is_settlement_recorded", "Not a SettlementRecorded event.")
        }

        conditionRegistry.register("actor_is_not_current_user") { context ->
            val event = context.attributes["ledger_event"] as? SharedLedgerEvent
            val ownId = context.attributes["own_public_id"] as? String
            if (event != null && event.actorMemberId != ownId) ValidationResult.Allowed else ValidationResult.Denied("actor_is_not_current_user", "Event was authored by current user.")
        }

        conditionRegistry.register("notifications_enabled") {
            if (NotificationService.hasNotificationPermission(appContext)) ValidationResult.Allowed
            else ValidationResult.Denied("notifications_enabled", "Notifications are turned off for this app.")
        }
    }

    private fun registerActions() {
        actionRegistry.registerUnit("set_busy_indicator_active") { context ->
            @Suppress("UNCHECKED_CAST")
            val setBusy = context.attributes["set_busy"] as? ((Boolean) -> Unit)
            setBusy?.invoke(true)
        }

        actionRegistry.registerUnit("clear_busy_indicator") { context ->
            @Suppress("UNCHECKED_CAST")
            val setBusy = context.attributes["set_busy"] as? ((Boolean) -> Unit)
            setBusy?.invoke(false)
        }

        actionRegistry.registerUnit("publish_pending_welcomes") {
            withContext(Dispatchers.IO) {
                repository.pendingWelcomes().forEach { pending ->
                    if (relay.publish(Event.fromJson(pending.eventJson), relay.relayUrls().toSet())) {
                        repository.markWelcomePublished(pending.id)
                    }
                }
            }
        }

        actionRegistry.registerUnit("publish_pending_encrypted_events") {
            withContext(Dispatchers.IO) {
                database.sharedGroups().getAll().forEach { group ->
                    repository.pendingEncryptedEvents(group.id).forEach { pending ->
                        pending.transportEventJson?.let { json ->
                            if (relay.publish(Event.fromJson(json), relay.relayUrls().toSet())) {
                                repository.markEventPublished(pending.id)
                            }
                        }
                    }
                }
            }
        }

        actionRegistry.register("execute_parallel_relay_fetches") { context ->
            try {
                withContext(Dispatchers.IO) {
                    val (identity, manager) = peers.managerSession()
                    context.attributes["identity"] = identity
                    context.attributes["manager"] = manager
                    val initialGroupIds = manager.activeGroupIds().sorted()
                    val staged = relay.fetchParallelEvents(
                        signerPubKey = identity.publicKeyHex,
                        initialGroupIds = initialGroupIds,
                        relayUrls = relay.relayUrls(),
                        relayTimeoutMs = config.relayTimeoutMs
                    )
                    context.attributes["staged_batch"] = staged
                }
                ActionResult.Success()
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (failure: Exception) {
                ActionResult.Failure("Relay fetch failed: ${failure.message}", failure)
            }
        }

        actionRegistry.register("process_inbound_welcomes") { context ->
            val staged = context.attributes["staged_batch"] as? StagedSyncBatch
                ?: return@register ActionResult.Failure("Missing staged sync batch")
            val identity = context.attributes["identity"] as? NostrIdentity
                ?: return@register ActionResult.Failure("Missing identity session")
            val manager = context.attributes["manager"] as? MarmotManager
                ?: return@register ActionResult.Failure("Missing manager session")

            withContext(Dispatchers.IO) {
                val giftWrapEvents = staged.incomingEvents.values.filterIsInstance<GiftWrapEvent>()
                    .sortedWith(compareBy<Event> { it.createdAt }.thenBy { it.id })

                for (event in giftWrapEvents) {
                    try {
                        if (event.recipientKeys().none { it == identity.publicKeyHex }) continue
                        val seal = event.unwrapOrNull(identity.signer) as? SealEvent ?: continue
                        if (!PeerSplitRelaySync.isValidNostrEvent(seal)) { staged.errors++; continue }
                        val rumor = seal.unsealRumorThrowing(identity.signer)
                        if (rumor.pubKey != seal.pubKey) { staged.errors++; continue }
                        val welcome = seal.unsealed(rumor) as? WelcomeEvent ?: continue
                        val outcome = manager.processWelcome(welcome)
                        if (outcome is WelcomeResult.Joined) {
                            peers.persistJoinedGroup(manager, outcome.nostrGroupId, identity.publicKeyHex)
                            staged.welcomesProcessed++
                            if (outcome.nostrGroupId !in staged.initialGroupIds) {
                                staged.newlyJoinedGroupIds.add(outcome.nostrGroupId)
                            }
                        }
                        if (outcome is WelcomeResult.Error) staged.errors++
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        staged.errors++
                    }
                }

                // If newly joined groups were discovered, fetch their group events concurrently
                if (staged.newlyJoinedGroupIds.isNotEmpty()) {
                    relay.fetchAdditionalGroupEvents(
                        groupIds = staged.newlyJoinedGroupIds,
                        staged = staged,
                        relayUrls = relay.relayUrls(),
                        relayTimeoutMs = config.relayTimeoutMs
                    )
                }
            }
            ActionResult.Success()
        }

        actionRegistry.register("decrypt_and_append_remote_ledger_events") { context ->
            val staged = context.attributes["staged_batch"] as? StagedSyncBatch
                ?: return@register ActionResult.Failure("Missing staged sync batch")
            val identity = context.attributes["identity"] as? NostrIdentity
                ?: return@register ActionResult.Failure("Missing identity session")
            val manager = context.attributes["manager"] as? MarmotManager
                ?: return@register ActionResult.Failure("Missing manager session")

            withContext(Dispatchers.IO) {
                val knownEventIds = repository.storedTransportEvents().mapTo(mutableSetOf()) { Event.fromJson(it).id }
                val sortedGroupEvents = staged.incomingEvents.values.filterIsInstance<GroupEvent>()
                    .filter { it.id !in knownEventIds }
                    .sortedWith(compareBy<GroupEvent> { it.createdAt }.thenBy { it.id })

                for (event in sortedGroupEvents) {
                    try {
                        val result = manager.processGroupEvent(event)
                        if (result is GroupEventResult.Duplicate) continue
                        if (result is GroupEventResult.UndecryptableOuterLayer || result is GroupEventResult.RefusedByLifecycle) {
                            // Old-epoch or pre-join events are refetched on every sync; they are not actionable failures.
                            Log.w("RefreshService", "Skipped group event ${event.id}: ${result::class.simpleName}")
                            continue
                        }
                        if (result is GroupEventResult.Error) {
                            Log.w("RefreshService", "Group event ${event.id} failed: $result")
                            staged.errors++
                            continue
                        }
                        val groupId = event.groupId() ?: continue
                        if (result is GroupEventResult.ApplicationMessage) {
                            val resultGroupId = result.groupId
                            val authenticatedSender = manager.groupManager.memberIdentityHex(resultGroupId, result.senderLeafIndex) ?: continue
                            val innerEvent = Event.fromJson(result.innerEventJson)
                            val decoded = PeerSplitLedgerWire.decode(innerEvent.content, resultGroupId, authenticatedSender)
                            if (decoded != null) {
                                repository.appendRemoteEvent(decoded, event.toJson())
                                if (authenticatedSender != identity.publicKeyHex) {
                                    staged.newlyAppendedEvents.add(decoded)
                                }
                            }
                        }
                        val members = manager.memberPubkeys(groupId).mapTo(linkedSetOf()) { it.pubkey }
                        val admins = manager.groupView(groupId)?.adminPubkeys?.toSet().orEmpty()
                        repository.syncMemberRoster(groupId, members, admins)
                        staged.groupEventsProcessed++
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        staged.errors++
                    }
                }
            }
            ActionResult.Success()
        }

        actionRegistry.register("notify_newly_inserted_events") { context ->
            val staged = context.attributes["staged_batch"] as? StagedSyncBatch
            val identity = context.attributes["identity"] as? NostrIdentity
            val newEvents = staged?.newlyAppendedEvents.orEmpty()

            for (event in newEvents) {
                val subContext = RuleContext(
                    groupId = event.groupId,
                    actorMemberId = event.actorMemberId,
                    attributes = mutableMapOf(
                        "ledger_event" to event,
                        "is_new_event" to true,
                        "own_public_id" to (identity?.publicKeyHex ?: ""),
                    )
                )
                when (event.action) {
                    is SharedLedgerAction.ExpenseAdded -> {
                        engine.execute("inbound_expense_notification", subContext)
                    }
                    is SharedLedgerAction.SettlementRecorded -> {
                        engine.execute("inbound_settlement_notification", subContext)
                    }
                    else -> {}
                }
            }
            ActionResult.Success()
        }

        // Reuses the manager session from the fetch step; peers.reconcileRoster would deadlock on the held protocol lock.
        actionRegistry.registerUnit("reconcile_active_group_rosters") { context ->
            val manager = context.attributes["manager"] as? MarmotManager ?: error("Missing manager session")
            withContext(Dispatchers.IO) {
                val active = manager.activeGroupIds().toSet()
                database.sharedGroups().getAll().filter { it.id in active }.forEach { group ->
                    repository.syncMemberRoster(
                        group.id,
                        manager.memberPubkeys(group.id).mapTo(linkedSetOf()) { it.pubkey },
                        manager.groupView(group.id)?.adminPubkeys?.toSet().orEmpty(),
                    )
                }
            }
        }

        actionRegistry.registerUnit("display_sync_status_summary") { context ->
            @Suppress("UNCHECKED_CAST")
            val setMessage = context.attributes["set_message"] as? ((String) -> Unit)
            val result = syncResult(context.attributes["staged_batch"] as? StagedSyncBatch)
            val ruleMessage = context.attributes["rule_message"] as? String
            setMessage?.invoke(result.summary(ruleMessage ?: "Up to date"))
            if (result.needsRetry) {
                PeerSplitSyncScheduler.request(appContext)
            }
        }

        actionRegistry.registerUnit("display_sync_error_summary") { context ->
            @Suppress("UNCHECKED_CAST")
            val setMessage = context.attributes["set_message"] as? ((String) -> Unit)
            setMessage?.invoke("Could not reach relays. Retrying in background.")
        }

        actionRegistry.registerUnit("schedule_background_retry_if_needed") {
            PeerSplitSyncScheduler.request(appContext)
        }

        actionRegistry.registerUnit("execute_background_sync") {
            withContext(Dispatchers.IO) {
                syncInBackground(appContext)
            }
        }

        actionRegistry.registerUnit("persist_retry_state") {
            PeerSplitSyncScheduler.request(appContext)
        }

        actionRegistry.registerUnit("format_expense_amount") { context ->
            val event = context.attributes["ledger_event"] as? SharedLedgerEvent
            val action = event?.action as? SharedLedgerAction.ExpenseAdded ?: return@registerUnit
            val totalMinor = action.debts.sumOf { it.amountMinor }
            val formatted = "${currencySymbol(action.currencyCode)} ${SharedSplitMoney.format(totalMinor, action.currencyCode)}"
            context.attributes["formatted_amount"] = formatted
        }

        actionRegistry.registerUnit("post_system_notification") { context ->
            val event = context.attributes["ledger_event"] as? SharedLedgerEvent ?: return@registerUnit
            val group = database.sharedGroups().findById(event.groupId)
            val groupName = group?.name ?: "Shared group"
            val formattedAmount = (context.attributes["formatted_amount"] as? String).orEmpty()
            when (event.action) {
                is SharedLedgerAction.ExpenseAdded -> {
                    NotificationService.notifyGroupExpenseAdded(
                        context = appContext,
                        groupName = groupName,
                        payerName = "A member",
                        amountFormatted = formattedAmount,
                        note = event.description
                    )
                }
                is SharedLedgerAction.SettlementRecorded -> {
                    NotificationService.notifySettlement(
                        context = appContext,
                        counterpartName = groupName,
                        amountFormatted = formattedAmount,
                        isGroup = true
                    )
                }
                else -> {}
            }
        }

        actionRegistry.registerUnit("format_settlement_amount") { context ->
            val event = context.attributes["ledger_event"] as? SharedLedgerEvent
            val action = event?.action as? SharedLedgerAction.SettlementRecorded ?: return@registerUnit
            val totalMinor = action.allocations.sumOf { it.amountMinor }
            val group = database.sharedGroups().findById(event.groupId)
            val currency = group?.currencyCode ?: "INR"
            val formatted = "${currencySymbol(currency)} ${SharedSplitMoney.format(totalMinor, currency)}"
            context.attributes["formatted_amount"] = formatted
        }
    }

    companion object {
        /** Entry point for WorkManager: resolves the database while locked so a local reset cannot close it mid-sync. */
        suspend fun syncInBackground(context: Context): PeerSplitSyncResult = PeerSplitService.protocolMutex.withLock {
            withContext(Dispatchers.IO) {
                val database = ExpenseDatabase.get(context)
                RefreshService(context, database, PeerSplitService(context, database)).runBackgroundSync()
            }
        }

        fun readRulesYaml(context: Context): String {
            return context.assets.open("refresh.yaml").bufferedReader().use { it.readText() }
        }
    }
}
