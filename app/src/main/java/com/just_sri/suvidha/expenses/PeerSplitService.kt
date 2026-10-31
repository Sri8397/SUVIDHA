package com.just_sri.suvidha.expenses

import android.content.Context
import com.vitorpamplona.marmot.client.MarmotManager
import com.vitorpamplona.marmot.client.MarmotPublisher
import com.vitorpamplona.marmot.mip01Groups.MarmotGroupData
import com.vitorpamplona.quartz.marmot.mip00KeyPackages.KeyPackageEvent
import com.vitorpamplona.quartz.nip01Core.core.Event
import com.vitorpamplona.quartz.nip01Core.relay.normalizer.NormalizedRelayUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import java.util.UUID

/** Serializes MLS mutations independently of screen lifetimes and navigation. */
internal class PeerSplitService(context: Context, private val database: ExpenseDatabase) {
    private val appContext = context.applicationContext
    internal val repository = SharedGroupRepository(database)
    private val identityStore = NostrIdentityStore(context)
    private val mlsStore = EncryptedMlsGroupStateStore(context)
    private val keyPackages = EncryptedKeyPackageBundleStore(context)
    private val relaySettings = RelaySettings(context)
    internal val relay = PeerSplitRelaySync(relays = { relaySettings.urls().map { NormalizedRelayUrl(it) } })
    val operationMutex get() = protocolMutex

    internal suspend fun managerSession(): Pair<NostrIdentity, MarmotManager> = manager()

    private suspend fun manager(): Pair<NostrIdentity, MarmotManager> {
        val identity = identityStore.getOrCreate()
        val manager = MarmotManager(signer = identity.signer, store = mlsStore, keyPackageStore = keyPackages,
            publisher = MarmotPublisher { event, relays -> relay.publish(event, relays) },
            publishObligationStore = mlsStore.transportStore,
            ingestDedupStore = mlsStore.transportStore.ingestDedup)
        manager.restoreAll()
        return identity to manager
    }

    suspend fun publicId(): String = operationMutex.withLock {
        withContext(Dispatchers.IO) { identityStore.getOrCreate().publicKeyHex }
    }

    suspend fun publishInviteKey(): String = operationMutex.withLock { withContext(Dispatchers.IO) {
        val (_, manager) = manager()
        val event = manager.generateKeyPackageEvent(relay.relayUrls(), "primary", false)
        check(relay.publish(event, relay.relayUrls().toSet())) { "Could not publish your invite key. Check your connection and retry." }
        manager.confirmKeyPackagePublished(event)
        "Ready to receive group invitations. Share your public ID with a friend."
    } }

    suspend fun createGroup(name: String, currencyCode: String): String = operationMutex.withLock { withContext(Dispatchers.IO) {
        val (identity, manager) = manager()
        val id = ByteArray(32).also(SecureRandom()::nextBytes).joinToString("") { "%02x".format(it) }
        manager.createGroup(id, MarmotGroupData(nostrGroupId = id, name = name.trim(),
            description = "suvidha.currency=$currencyCode", adminPubkeys = listOf(identity.publicKeyHex),
            relays = relay.relayUrls().map { it.url }))
        try { repository.createGroup(id, name, currencyCode, identity.publicKeyHex, "You") }
        catch (failure: Exception) { mlsStore.delete(id); throw failure }
        id
    } }

    // Permission checks for the membership operations below live in group_membership.yaml
    // (GroupMembershipService); these methods only perform the MLS and ledger work.

    suspend fun invite(groupId: String, publicId: String): String = operationMutex.withLock { withContext(Dispatchers.IO) {
        val (_, manager) = manager()
        val published = relay.fetchKeyPackageEvents(publicId).asSequence()
            .filter { it.pubKey.equals(publicId, true) }.mapNotNull { it as? KeyPackageEvent }
            .firstOrNull { it.hasValidEncoding() }
            ?: error("No invite key found. Ask this friend to enable group invitations in Profile, then retry.")
        val (_, welcome) = manager.addMember(groupId, published, relay.relayUrls())
        checkNotNull(welcome) { "The invitation was not confirmed. Check your connection and retry." }
        repository.queueWelcome(groupId, publicId, welcome.giftWrapEvent.id, welcome.giftWrapEvent.toJson())
        PeerSplitSyncScheduler.request(appContext, afterChange = true)
        repository.addInvitedMember(groupId, publicId)
        persistRoster(manager, groupId)
        val sent = relay.publish(welcome.giftWrapEvent, relay.relayUrls().toSet())
        if (sent) repository.markWelcomePublished(welcome.giftWrapEvent.id)
        if (sent) "Invitation sent. Your friend will join when their app syncs." else "Invitation saved. Delivery will retry automatically."
    } }

    suspend fun checkInvitationAvailable(publicId: String): Boolean = withContext(Dispatchers.IO) {
        if (!publicId.matches(Regex("[0-9a-f]{64}"))) return@withContext false
        try {
            relay.fetchKeyPackageEvents(publicId).asSequence()
                .filter { it.pubKey.equals(publicId, true) }
                .mapNotNull { it as? KeyPackageEvent }
                .any { it.hasValidEncoding() }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun removeMember(groupId: String, publicId: String) = operationMutex.withLock { withContext(Dispatchers.IO) {
        val (_, manager) = manager()
        val member = manager.memberPubkeys(groupId).firstOrNull { it.pubkey == publicId } ?: error("This member is no longer active.")
        manager.removeMember(groupId, member.leafIndex, relay.relayUrls())
        PeerSplitSyncScheduler.request(appContext, afterChange = true)
        persistRoster(manager, groupId)
    } }

    /** Replaces the admin set with the new owner; the previous owner becomes a regular member. */
    suspend fun transferOwnership(groupId: String, newOwnerId: String) = operationMutex.withLock { withContext(Dispatchers.IO) {
        val (_, manager) = manager()
        manager.setGroupAdmins(groupId, listOf(newOwnerId), relay.relayUrls())
        PeerSplitSyncScheduler.request(appContext, afterChange = true)
        persistRoster(manager, groupId)
    } }

    /** Removes every other member, leaving the caller as the only one before it leaves too. */
    suspend fun removeAllOtherMembers(groupId: String) = operationMutex.withLock { withContext(Dispatchers.IO) {
        val (identity, manager) = manager()
        generateSequence { manager.memberPubkeys(groupId).firstOrNull { it.pubkey != identity.publicKeyHex } }
            .forEach { member -> manager.removeMember(groupId, member.leafIndex, relay.relayUrls()) }
        PeerSplitSyncScheduler.request(appContext, afterChange = true)
        persistRoster(manager, groupId)
    } }

    /** MLS forbids removing yourself with a Remove commit; leave() steps down as admin if needed and proposes SelfRemove. */
    suspend fun leaveGroup(groupId: String) = operationMutex.withLock { withContext(Dispatchers.IO) {
        val (identity, manager) = manager()
        if (manager.memberPubkeys(groupId).none { it.pubkey == identity.publicKeyHex }) return@withContext
        val departure = manager.leave(groupId, relay.relayUrls())
        departure.selfRemove?.let { relay.publish(it.signedEvent, relay.relayUrls().toSet()) }
        database.sharedGroupMembers().markInactive(groupId, identity.publicKeyHex)
        PeerSplitSyncScheduler.request(appContext, afterChange = true)
    } }

    private suspend fun persistRoster(manager: MarmotManager, groupId: String) {
        repository.syncMemberRoster(groupId, manager.memberPubkeys(groupId).mapTo(linkedSetOf()) { it.pubkey },
            manager.groupView(groupId)?.adminPubkeys?.toSet().orEmpty())
    }

    suspend fun reconcileRoster(groupId: String): Unit = operationMutex.withLock { withContext(Dispatchers.IO) {
        val (_, manager) = manager()
        persistRoster(manager, groupId)
    } }

    suspend fun sync(): PeerSplitSyncResult = operationMutex.withLock { syncLocked() }

    /** Foreground polling skips busy protocol operations instead of building up waiting jobs. */
    suspend fun syncIfIdle(): PeerSplitSyncResult? {
        if (!operationMutex.tryLock()) return null
        return try { syncLocked() } finally { operationMutex.unlock() }
    }

    private suspend fun syncLocked(): PeerSplitSyncResult = withContext(Dispatchers.IO) {
        val (identity, manager) = manager()
        repository.pendingWelcomes().forEach { pending ->
            if (relay.publish(Event.fromJson(pending.eventJson), relay.relayUrls().toSet())) repository.markWelcomePublished(pending.id)
        }
        manager.activeGroupIds().forEach { id -> if (repository.findGroup(id) == null) persistJoinedGroup(manager, id, identity.publicKeyHex) }
        database.sharedGroups().getAll().forEach { group ->
            repository.pendingEncryptedEvents(group.id).forEach { pending ->
                pending.transportEventJson?.let { if (relay.publish(Event.fromJson(it), relay.relayUrls().toSet())) repository.markEventPublished(pending.id) }
            }
        }
        val report = relay.sync(manager, identity.signer,
            knownEventIds = repository.storedTransportEvents().mapTo(mutableSetOf()) { Event.fromJson(it).id },
            onLedgerEvent = { event, json ->
                repository.appendRemoteEvent(event, json)
                if (event.actorMemberId != identity.publicKeyHex) {
                    when (val action = event.action) {
                        is SharedLedgerAction.ExpenseAdded -> {
                            val group = repository.findGroup(event.groupId)
                            val groupName = group?.name ?: "Shared group"
                            val currency = group?.currencyCode ?: action.currencyCode
                            val totalMinor = action.debts.sumOf { it.amountMinor }
                            val amountStr = "${currencySymbol(currency)} ${SharedSplitMoney.format(totalMinor, currency)}"
                            NotificationService.notifyGroupExpenseAdded(
                                context = appContext,
                                groupName = groupName,
                                payerName = "A member",
                                amountFormatted = amountStr,
                                note = event.description
                            )
                        }
                        is SharedLedgerAction.SettlementRecorded -> {
                            val group = repository.findGroup(event.groupId)
                            val groupName = group?.name ?: "Shared group"
                            val currency = group?.currencyCode ?: "INR"
                            val totalMinor = action.allocations.sumOf { it.amountMinor }
                            val amountStr = "${currencySymbol(currency)} ${SharedSplitMoney.format(totalMinor, currency)}"
                            NotificationService.notifySettlement(
                                context = appContext,
                                counterpartName = groupName,
                                amountFormatted = amountStr,
                                isGroup = true
                            )
                        }
                        is SharedLedgerAction.ChatMessage -> {
                            val group = repository.findGroup(event.groupId)
                            NotificationService.notifyGroupChatMessage(
                                context = appContext,
                                groupId = event.groupId,
                                groupName = group?.name ?: "Shared group",
                                senderName = memberName(event.groupId, event.actorMemberId),
                                text = action.text
                            )
                        }
                        else -> {}
                    }
                }
            },
            onWelcomeJoined = { id -> persistJoinedGroup(manager, id, identity.publicKeyHex) },
            onMembersChanged = { id, members, admins -> repository.syncMemberRoster(id, members, admins) })
        val pendingUploads = repository.pendingWelcomes().isNotEmpty() || database.sharedGroups().getAll().any {
            repository.pendingEncryptedEvents(it.id).isNotEmpty()
        }
        PeerSplitSyncResult(report, pendingUploads)
    }

    /** Same naming as the UI: linked friend name, then a real roster name, else a neutral fallback. */
    private suspend fun memberName(groupId: String, memberId: String): String {
        val friendId: String? = database.sharedMemberFriendLinks().getAll().find { it.memberId == memberId }?.friendId
        val friendName: String? = friendId?.let { id -> database.friends().getAll().find { it.id == id }?.name }
        return friendName ?: database.sharedGroupMembers().find(groupId, memberId)?.displayName?.takeUnless { it.startsWith("Nostr") } ?: "A member"
    }

    internal suspend fun persistJoinedGroup(manager: MarmotManager, id: String, ownId: String) {
        val view = manager.groupView(id) ?: error("Group details are unavailable.")
        val currency = Regex("(?:^|;)\\s*suvidha\\.currency=([A-Z]{3})(?:;|$)").find(view.description.orEmpty())?.groupValues?.get(1)
            ?: repository.findGroup(id)?.currencyCode ?: error("This invitation is missing its currency.")
        val members = manager.memberPubkeys(id).mapTo(linkedSetOf()) { it.pubkey }.also { it += ownId }
        repository.createJoinedGroup(id, view.name.ifBlank { "Shared group" }, currency, members, view.adminPubkeys.toSet())
    }

    suspend fun addExpense(groupId: String, draft: SharedExpenseDraft) = mutateLedger(groupId) { identity, projection, group ->
        require(draft.amountsMinorByMemberId.values.all { it > 0 } && identity.publicKeyHex !in draft.amountsMinorByMemberId) { "Check the split amounts." }
        newEvent(groupId, identity, projection, SharedLedgerAction.ExpenseAdded(group.currencyCode,
            draft.amountsMinorByMemberId.toSortedMap().map { (id, amount) -> SharedDebtUnit(UUID.randomUUID().toString(), identity.publicKeyHex, id, amount) }),
            draft.note.trim().take(200).ifBlank { "Shared expense" })
    }

    suspend fun settle(groupId: String, debtId: String, amount: Long) = mutateLedger(groupId) { identity, projection, _ ->
        newEvent(groupId, identity, projection, SharedLedgerAction.SettlementRecorded(listOf(SharedSettlementAllocation(debtId, amount))), "Payment recorded")
    }

    suspend fun removeDebt(groupId: String, debtId: String) = mutateLedger(groupId) { identity, projection, _ ->
        newEvent(groupId, identity, projection, SharedLedgerAction.DebtRemoved(debtId), "Outstanding debt removed")
    }

    suspend fun renameGroup(groupId: String, name: String) = mutateLedger(groupId) { identity, projection, _ ->
        newEvent(groupId, identity, projection, SharedLedgerAction.GroupRenamed(name.trim()), "Group renamed")
    }

    suspend fun sendChatMessage(groupId: String, text: String) = mutateLedger(groupId) { identity, projection, _ ->
        newEvent(groupId, identity, projection, SharedLedgerAction.ChatMessage(text.trim()), text.trim().take(100))
    }

    private fun newEvent(id: String, identity: NostrIdentity, projection: SharedLedgerProjection, action: SharedLedgerAction, note: String) =
        SharedLedgerEvent(UUID.randomUUID().toString(), id, identity.publicKeyHex, projection.acceptedEventIds, System.currentTimeMillis(), action, note)

    private suspend fun mutateLedger(id: String, build: suspend (NostrIdentity, SharedLedgerProjection, SharedGroupEntity) -> SharedLedgerEvent) =
        operationMutex.withLock { withContext(Dispatchers.IO) {
            val (identity, manager) = manager()
            val group = repository.findGroup(id) ?: error("This group is no longer available.")
            val event = build(identity, repository.replayGroup(id), group)
            val outer = PeerSplitLedgerWire.encrypt(manager, event)
            repository.appendLocalEvent(event, outer.toJson())
            // Save locally, then hand delivery to a durable job without waiting for the network.
            PeerSplitSyncScheduler.request(appContext, afterChange = true)
            Unit
        } }

    companion object {
        /** UI, workers, backups, identity access and database reset all run in the default process. */
        val protocolMutex = Mutex()

        suspend fun syncInBackground(context: Context): PeerSplitSyncResult = protocolMutex.withLock {
            withContext(Dispatchers.IO) {
                // Resolve the database while locked so a local reset cannot close it mid-sync.
                PeerSplitService(context, ExpenseDatabase.get(context)).syncLocked()
            }
        }
    }
}
