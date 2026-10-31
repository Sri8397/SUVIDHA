package com.just_sri.suvidha.expenses.service

import android.content.Context
import android.util.Log
import com.just_sri.suvidha.expenses.ExpenseDatabase
import com.just_sri.suvidha.expenses.PeerSplitRelaySync
import com.just_sri.suvidha.expenses.PeerSplitService
import com.just_sri.suvidha.expenses.SharedGroupMemberEntity
import com.just_sri.suvidha.expenses.SharedGroupRepository
import com.just_sri.suvidha.expenses.engine.ActionRegistry
import com.just_sri.suvidha.expenses.engine.ActionResult
import com.just_sri.suvidha.expenses.engine.ConditionRegistry
import com.just_sri.suvidha.expenses.engine.ExecutionResult
import com.just_sri.suvidha.expenses.engine.Rule
import com.just_sri.suvidha.expenses.engine.RuleContext
import com.just_sri.suvidha.expenses.engine.RuleEngine
import com.just_sri.suvidha.expenses.engine.ValidationResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class GroupMembershipService(
    yamlContent: String,
    private val database: ExpenseDatabase,
    private val repository: SharedGroupRepository,
    private val peers: PeerSplitService,
    private val relay: PeerSplitRelaySync,
) {
    constructor(
        context: Context,
        database: ExpenseDatabase,
        repository: SharedGroupRepository,
        peers: PeerSplitService,
        relay: PeerSplitRelaySync,
    ) : this(
        yamlContent = readRulesYaml(context),
        database = database,
        repository = repository,
        peers = peers,
        relay = relay,
    )

    val conditionRegistry: ConditionRegistry = ConditionRegistry()
    val actionRegistry: ActionRegistry = ActionRegistry()
    val engine: RuleEngine = RuleEngine(conditionRegistry, actionRegistry)

    init {
        registerConditions()
        registerActions()
        engine.loadYaml(yamlContent, validateRegistries = true)
    }

    suspend fun executeRule(ruleId: String, context: RuleContext): ExecutionResult {
        return engine.execute(ruleId, context)
    }

    suspend fun canExecuteRule(ruleId: String, context: RuleContext): ValidationResult {
        return engine.canExecute(ruleId, context)
    }

    fun getRule(ruleId: String): Rule? {
        return engine.getRule(ruleId)
    }

    suspend fun reconcileMembership(groupId: String): Unit = withContext(Dispatchers.IO) {
        peers.reconcileRoster(groupId)
    }

    private fun registerConditions(): Unit {
        conditionRegistry.register("owner_must_transfer_or_close") { context: RuleContext ->
            val members: List<SharedGroupMemberEntity> = database.sharedGroupMembers().getByGroup(context.groupId).filter { it.isActive }
            val isOwner: Boolean = members.any { it.memberId == context.actorMemberId && it.role == "owner" }
            if (isOwner && members.count { it.memberId != context.actorMemberId } > 0) {
                ValidationResult.Denied("owner_must_transfer_or_close", "Transfer group ownership or close the group before leaving.")
            } else {
                ValidationResult.Allowed
            }
        }
        conditionRegistry.register("net_balance_zero") { context: RuleContext ->
            val targetId: String = context.targetMemberId ?: context.actorMemberId
            val balance: Long = repository.replayGroup(context.groupId).balanceByMemberId[targetId] ?: 0L
            if (balance == 0L) ValidationResult.Allowed else ValidationResult.Denied("net_balance_zero", "Member balance must be zero.")
        }
        conditionRegistry.register("no_outstanding_pairwise_debts") { context: RuleContext ->
            val targetId: String = context.targetMemberId ?: context.actorMemberId
            val projection = repository.replayGroup(context.groupId)
            val hasDebts: Boolean = projection.debtById.values.any { debt ->
                (debt.debtorMemberId == targetId || debt.creditorMemberId == targetId) && debt.remainingAmountMinor > 0L
            }
            if (!hasDebts) ValidationResult.Allowed else ValidationResult.Denied("no_outstanding_pairwise_debts", "Member has outstanding debts.")
        }
        conditionRegistry.register("recipient_is_friend") { context: RuleContext ->
            val targetId: String = context.targetMemberId ?: ""
            val isFriend: Boolean = database.sharedMemberFriendLinks().getAll().any { it.memberId == targetId }
            if (isFriend) ValidationResult.Allowed else ValidationResult.Denied("recipient_is_friend", "Recipient is not a friend.")
        }
        conditionRegistry.register("recipient_has_nostr_id") { context: RuleContext ->
            val targetId: String = context.targetMemberId ?: ""
            val hasNostrKey: Boolean = targetId.matches(Regex("[0-9a-f]{64}"))
            if (hasNostrKey) ValidationResult.Allowed else ValidationResult.Denied("recipient_has_nostr_id", "Missing valid Nostr public key.")
        }
        conditionRegistry.register("recipient_invitation_status_enabled") {
            ValidationResult.Allowed
        }
        conditionRegistry.register("recipient_has_valid_keypackage") { context: RuleContext ->
            val targetId: String = context.targetMemberId ?: ""
            val isAvailable: Boolean = peers.checkInvitationAvailable(targetId)
            if (isAvailable) ValidationResult.Allowed else ValidationResult.Denied("recipient_has_valid_keypackage", "No active key package found on relays.")
        }
        conditionRegistry.register("has_nostr_identity") {
            if (peers.publicId().matches(Regex("[0-9a-f]{64}"))) ValidationResult.Allowed
            else ValidationResult.Denied("has_nostr_identity", "A Nostr identity is required.")
        }
    }

    private fun registerActions(): Unit {
        actionRegistry.registerUnit("cancel_authored_active_splits") { context: RuleContext ->
            val authored: List<String> = repository.replayGroup(context.groupId).debtById.values
                .filter { it.creditorMemberId == context.actorMemberId && it.remainingAmountMinor > 0L }
                .map { it.debtUnitId }
            authored.forEach { debtUnitId: String -> peers.removeDebt(context.groupId, debtUnitId) }
        }
        actionRegistry.register("request_mls_self_removal") { context: RuleContext ->
            try {
                peers.leaveGroup(context.groupId)
                peers.reconcileRoster(context.groupId)
                ActionResult.Success()
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (failure: Exception) {
                try {
                    peers.reconcileRoster(context.groupId)
                } catch (recoveryFailure: Exception) {
                    Log.w("GroupMembershipService", "Reconciliation after self-removal failed", recoveryFailure)
                }
                ActionResult.Failure("MLS self-removal failed: ${failure.message}", failure)
            }
        }
        actionRegistry.register("mls_remove_member") { context: RuleContext ->
            val targetId: String = context.targetMemberId ?: return@register ActionResult.Failure("Target member missing")
            try {
                peers.removeMember(context.groupId, targetId)
                peers.reconcileRoster(context.groupId)
                ActionResult.Success()
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (failure: Exception) {
                try {
                    peers.reconcileRoster(context.groupId)
                } catch (recoveryFailure: Exception) {
                    Log.w("GroupMembershipService", "Reconciliation after member removal failed", recoveryFailure)
                }
                ActionResult.Failure("MLS removal failed: ${failure.message}", failure)
            }
        }
        actionRegistry.registerUnit("deactivate_member") { context: RuleContext ->
            val targetId: String = context.targetMemberId ?: context.actorMemberId
            database.sharedGroupMembers().markInactive(context.groupId, targetId)
            peers.reconcileRoster(context.groupId)
        }
        actionRegistry.registerUnit("sync_causal_ledger_event") {
            withContext(Dispatchers.IO) { peers.syncIfIdle() }
        }
        // Former members stay visible and read-only because membership is derived from the roster
        // (SharedGroupMemberEntity.isActive), which deactivate_member and reconcileRoster maintain.
        declarative(
            "enforce_read_only_history",
            "preserve_chat_history",
            "preserve_settled_transactions",
            "display_as_former_member",
        )
        // Invitation candidates and the invite button are gated by canExecuteRule in the UI.
        declarative("show_recipient_in_invitation_list", "enable_invite_action")
        // One MarmotManager session generates, publishes (kind 30443) and confirms the key package,
        // so the steps cannot be split across separate registry actions.
        actionRegistry.register("generate_fresh_mls_keypackage") {
            try {
                ActionResult.Success(peers.publishInviteKey())
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (failure: Exception) {
                ActionResult.Failure("Could not publish your invite key: ${failure.message}", failure)
            }
        }
        declarative("publish_keypackage_kind_30443", "publish_signed_invitation_status_enabled")
        // PeerSplitService.sync() fetches pending welcomes, joins invited groups and retries queued invitations.
        actionRegistry.registerUnit("sync_pending_welcomes") {
            withContext(Dispatchers.IO) { peers.sync() }
        }
        declarative("process_valid_previous_invitations", "sync_joined_groups")
        // The local on/off flag is persisted by the caller from the rule result; there is no
        // relay-side revocation of published key packages yet.
        declarative(
            "publish_signed_invitation_status_disabled",
            "stop_keypackage_publication",
            "request_deletion_of_published_keypackages",
            "preserve_pending_welcome_keys",
            "defer_incoming_welcomes",
        )
    }

    private fun declarative(vararg actionNames: String): Unit {
        actionNames.forEach { actionName: String -> actionRegistry.registerUnit(actionName) {} }
    }

    companion object {
        fun readRulesYaml(context: Context): String {
            return context.assets.open("group_membership.yaml").bufferedReader().use { it.readText() }
        }
    }
}
