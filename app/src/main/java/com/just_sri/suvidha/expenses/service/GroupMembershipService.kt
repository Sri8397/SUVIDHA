package com.just_sri.suvidha.expenses.service

import android.content.Context
import android.util.Log
import com.just_sri.suvidha.expenses.ExpenseDatabase
import com.just_sri.suvidha.expenses.PeerSplitRelaySync
import com.just_sri.suvidha.expenses.PeerSplitService
import com.just_sri.suvidha.expenses.SharedExpenseDraft
import com.just_sri.suvidha.expenses.SharedGroupMemberEntity
import com.just_sri.suvidha.expenses.SharedGroupRepository
import com.just_sri.suvidha.expenses.SharedLedgerAction
import com.just_sri.suvidha.expenses.SharedLedgerCodec
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

/**
 * Runs every group workflow declared in group_membership.yaml. Actors and conditions here are
 * the single place permissions are checked; the PeerSplitService calls only do protocol work.
 */
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
    val engine: RuleEngine = RuleEngine(conditionRegistry, actionRegistry, requireActors = true)

    init {
        registerActors()
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

    private suspend fun activeMembers(groupId: String): List<SharedGroupMemberEntity> =
        database.sharedGroupMembers().getByGroup(groupId).filter { it.isActive }

    private fun registerActors(): Unit {
        conditionRegistry.registerActor("active_member") { context: RuleContext ->
            val isActive: Boolean = activeMembers(context.groupId).any { it.memberId == context.actorMemberId }
            if (isActive) ValidationResult.Allowed else ValidationResult.Denied("active_member", "Only an active group member can do this.")
        }
        conditionRegistry.registerActor("owner_only") { context: RuleContext ->
            val isOwner: Boolean = activeMembers(context.groupId).any { it.memberId == context.actorMemberId && it.role == "owner" }
            if (isOwner) ValidationResult.Allowed else ValidationResult.Denied("owner_only", "Only the group owner can do this.")
        }
        conditionRegistry.registerActor("user") { ValidationResult.Allowed }
    }

    private fun registerConditions(): Unit {
        conditionRegistry.register("owner_must_transfer_or_close") { context: RuleContext ->
            val members: List<SharedGroupMemberEntity> = activeMembers(context.groupId)
            val isOwner: Boolean = members.any { it.memberId == context.actorMemberId && it.role == "owner" }
            if (isOwner && members.any { it.memberId != context.actorMemberId }) {
                ValidationResult.Denied("owner_must_transfer_or_close", "Transfer group ownership or close the group before leaving.")
            } else {
                ValidationResult.Allowed
            }
        }
        conditionRegistry.register("own_balance_settled") { context: RuleContext ->
            val hasOpenBalance: Boolean = repository.replayGroup(context.groupId).debtById.values.any { debt ->
                (debt.debtorMemberId == context.actorMemberId || debt.creditorMemberId == context.actorMemberId) && debt.remainingAmountMinor > 0L
            }
            if (!hasOpenBalance) ValidationResult.Allowed
            else ValidationResult.Denied("own_balance_settled", "Settle your balance in this group before leaving.")
        }
        conditionRegistry.register("target_is_other_active_member") { context: RuleContext ->
            val targetId: String = context.targetMemberId.orEmpty()
            val isOtherActive: Boolean = targetId != context.actorMemberId && activeMembers(context.groupId).any { it.memberId == targetId }
            if (isOtherActive) ValidationResult.Allowed else ValidationResult.Denied("target_is_other_active_member", "Choose another active member of this group.")
        }
        conditionRegistry.register("no_outstanding_pairwise_debts") { context: RuleContext ->
            val targetId: String = context.targetMemberId ?: context.actorMemberId
            val hasDebts: Boolean = repository.replayGroup(context.groupId).debtById.values.any { debt ->
                (debt.debtorMemberId == targetId || debt.creditorMemberId == targetId) && debt.remainingAmountMinor > 0L
            }
            if (!hasDebts) ValidationResult.Allowed else ValidationResult.Denied("no_outstanding_pairwise_debts", "Settle this member's balance before removing them.")
        }
        conditionRegistry.register("no_open_transactions") { context: RuleContext ->
            val hasOpen: Boolean = repository.replayGroup(context.groupId).debtById.values.any { it.remainingAmountMinor > 0L }
            if (hasOpen) ValidationResult.Denied("no_open_transactions", "Settle or delete all open transactions before closing the group.") else ValidationResult.Allowed
        }
        conditionRegistry.register("group_name_valid") { context: RuleContext ->
            val length: Int = context.note.orEmpty().trim().length
            if (length in 1..SharedLedgerCodec.MAX_GROUP_NAME_LENGTH) ValidationResult.Allowed
            else ValidationResult.Denied("group_name_valid", "Enter a group name with up to ${SharedLedgerCodec.MAX_GROUP_NAME_LENGTH} characters.")
        }
        conditionRegistry.register("recipient_is_friend") { context: RuleContext ->
            val targetId: String = context.targetMemberId.orEmpty()
            val isFriend: Boolean = database.sharedMemberFriendLinks().getAll().any { it.memberId == targetId }
            if (isFriend) ValidationResult.Allowed else ValidationResult.Denied("recipient_is_friend", "Only friends can be invited.")
        }
        conditionRegistry.register("recipient_has_nostr_id") { context: RuleContext ->
            val hasNostrKey: Boolean = context.targetMemberId.orEmpty().matches(NOSTR_KEY)
            if (hasNostrKey) ValidationResult.Allowed else ValidationResult.Denied("recipient_has_nostr_id", "Choose a friend with a valid public ID.")
        }
        conditionRegistry.register("recipient_not_member") { context: RuleContext ->
            val isMember: Boolean = activeMembers(context.groupId).any { it.memberId == context.targetMemberId }
            if (!isMember) ValidationResult.Allowed else ValidationResult.Denied("recipient_not_member", "This friend is already a member.")
        }
        conditionRegistry.register("recipient_has_valid_keypackage") { context: RuleContext ->
            val isAvailable: Boolean = peers.checkInvitationAvailable(context.targetMemberId.orEmpty())
            if (isAvailable) ValidationResult.Allowed
            else ValidationResult.Denied("recipient_has_valid_keypackage", "No invite key found. Ask this friend to enable group invitations in Profile, then retry.")
        }
        conditionRegistry.register("actor_is_split_author") { context: RuleContext ->
            val record = database.sharedLedgerEvents().findById(context.attribute(EVENT_ID))
            val isAuthor: Boolean = record != null && record.groupId == context.groupId &&
                record.actionType == "expense_added" && record.actorMemberId == context.actorMemberId
            if (isAuthor) ValidationResult.Allowed else ValidationResult.Denied("actor_is_split_author", "Only the member who created this split can cancel it.")
        }
        conditionRegistry.register("split_has_outstanding_debts") { context: RuleContext ->
            val hasOutstanding: Boolean = outstandingSplitDebtIds(context).isNotEmpty()
            if (hasOutstanding) ValidationResult.Allowed else ValidationResult.Denied("split_has_outstanding_debts", "This split has nothing left to cancel.")
        }
        conditionRegistry.register("actor_is_outstanding_debt_creditor") { context: RuleContext ->
            val debt = repository.replayGroup(context.groupId).debtById[context.attribute(DEBT_UNIT_ID)]
            val isCreditor: Boolean = debt != null && debt.creditorMemberId == context.actorMemberId && debt.remainingAmountMinor > 0L
            if (isCreditor) ValidationResult.Allowed else ValidationResult.Denied("actor_is_outstanding_debt_creditor", "Only the person who added the split can delete an outstanding debt.")
        }
        conditionRegistry.register("message_text_valid") { context: RuleContext ->
            val text: String = context.note.orEmpty()
            when {
                text.isBlank() -> ValidationResult.Denied("message_text_valid", "Message cannot be blank.")
                text.length > SharedLedgerCodec.MAX_CHAT_LENGTH -> ValidationResult.Denied("message_text_valid", "Message is too long (max ${SharedLedgerCodec.MAX_CHAT_LENGTH} characters).")
                else -> ValidationResult.Allowed
            }
        }
        conditionRegistry.register("split_participants_are_active_members") { context: RuleContext ->
            val participantIds: Set<String> = (context.attributes[SPLIT_DRAFT] as? SharedExpenseDraft)?.amountsMinorByMemberId?.keys.orEmpty()
            val active: Set<String> = activeMembers(context.groupId).map { it.memberId }.toSet()
            if (participantIds.isNotEmpty() && active.containsAll(participantIds)) ValidationResult.Allowed
            else ValidationResult.Denied("split_participants_are_active_members", "Select active group members.")
        }
        conditionRegistry.register("payment_amount_within_balance") { context: RuleContext ->
            val remaining: Long = repository.replayGroup(context.groupId).debtById[context.attribute(DEBT_UNIT_ID)]?.remainingAmountMinor ?: 0L
            val amount: Long = context.attributes[AMOUNT_MINOR] as? Long ?: 0L
            if (amount in 1..remaining) ValidationResult.Allowed
            else ValidationResult.Denied("payment_amount_within_balance", "Enter an amount within the outstanding balance.")
        }
        conditionRegistry.register("has_nostr_identity") {
            if (peers.publicId().matches(NOSTR_KEY)) ValidationResult.Allowed
            else ValidationResult.Denied("has_nostr_identity", "A Nostr identity is required.")
        }
    }

    private fun registerActions(): Unit {
        actionRegistry.register("request_mls_self_removal") { context: RuleContext ->
            reconcilingOnFailure(context.groupId, "MLS self-removal failed") { peers.leaveGroup(context.groupId) }
        }
        // The self-remove is only a request until the group admin commits it; report the leave as failed until the roster agrees.
        actionRegistry.register("confirm_self_removal") { context: RuleContext ->
            peers.reconcileRoster(context.groupId)
            val isStillMember: Boolean = activeMembers(context.groupId).any { it.memberId == context.actorMemberId }
            if (isStillMember) ActionResult.Failure("Your removal is waiting for the group admin to confirm. You are still a member until then.")
            else ActionResult.Success()
        }
        actionRegistry.register("mls_remove_member") { context: RuleContext ->
            reconcilingOnFailure(context.groupId, "MLS removal failed") { peers.removeMember(context.groupId, context.targetMemberId.orEmpty()) }
        }
        actionRegistry.register("mls_set_group_admins") { context: RuleContext ->
            reconcilingOnFailure(context.groupId, "Ownership transfer failed") { peers.transferOwnership(context.groupId, context.targetMemberId.orEmpty()) }
        }
        actionRegistry.register("mls_remove_all_members") { context: RuleContext ->
            reconcilingOnFailure(context.groupId, "Closing the group failed") { peers.removeAllOtherMembers(context.groupId) }
        }
        actionRegistry.register("mls_add_member") { context: RuleContext ->
            try {
                ActionResult.Success(peers.invite(context.groupId, context.targetMemberId.orEmpty()))
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (failure: Exception) {
                ActionResult.Failure(failure.message ?: "Could not send the invitation.", failure)
            }
        }
        actionRegistry.register("create_mls_group") { context: RuleContext ->
            try {
                ActionResult.Success(peers.createGroup(context.note.orEmpty(), context.attribute(CURRENCY_CODE)))
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (failure: Exception) {
                ActionResult.Failure(failure.message ?: "Could not create the group.", failure)
            }
        }
        actionRegistry.registerUnit("publish_chat_message") { context: RuleContext ->
            peers.sendChatMessage(context.groupId, context.note.orEmpty())
        }
        actionRegistry.registerUnit("publish_split_expense") { context: RuleContext ->
            peers.addExpense(context.groupId, context.attributes[SPLIT_DRAFT] as SharedExpenseDraft)
        }
        actionRegistry.registerUnit("publish_settlement") { context: RuleContext ->
            peers.settle(context.groupId, context.attribute(DEBT_UNIT_ID), context.attributes[AMOUNT_MINOR] as Long)
        }
        actionRegistry.registerUnit("publish_group_rename") { context: RuleContext ->
            peers.renameGroup(context.groupId, context.note.orEmpty())
        }
        actionRegistry.registerUnit("publish_debt_removal") { context: RuleContext ->
            peers.removeDebt(context.groupId, context.attribute(DEBT_UNIT_ID))
        }
        // Each outstanding debt gets a relayed DebtRemoved event; the split shows CANCELLED once none remain.
        actionRegistry.registerUnit("publish_split_debt_removals") { context: RuleContext ->
            outstandingSplitDebtIds(context).forEach { debtUnitId: String -> peers.removeDebt(context.groupId, debtUnitId) }
        }
        actionRegistry.registerUnit("sync_causal_ledger_event") {
            withContext(Dispatchers.IO) { peers.syncIfIdle() }
        }
        // One MarmotManager session generates, publishes (kind 30443) and confirms the key package.
        actionRegistry.register("generate_fresh_mls_keypackage") {
            try {
                ActionResult.Success(peers.publishInviteKey())
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (failure: Exception) {
                ActionResult.Failure("Could not publish your invite key: ${failure.message}", failure)
            }
        }
        // PeerSplitService.sync() fetches pending welcomes, joins invited groups and retries queued invitations.
        actionRegistry.registerUnit("sync_pending_welcomes") {
            withContext(Dispatchers.IO) { peers.sync() }
        }
    }

    /** Runs an MLS roster change and re-reads the roster either way, so Room never lags the MLS state. */
    private suspend fun reconcilingOnFailure(groupId: String, failurePrefix: String, change: suspend () -> Unit): ActionResult {
        return try {
            change()
            peers.reconcileRoster(groupId)
            ActionResult.Success()
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (failure: Exception) {
            try {
                peers.reconcileRoster(groupId)
            } catch (recoveryFailure: Exception) {
                Log.w("GroupMembershipService", "Roster reconciliation after a failed change failed", recoveryFailure)
            }
            ActionResult.Failure("$failurePrefix: ${failure.message}", failure)
        }
    }

    /** Debt units of the split in attributes[eventId] that still have an amount outstanding. */
    private suspend fun outstandingSplitDebtIds(context: RuleContext): List<String> {
        val record = database.sharedLedgerEvents().findById(context.attribute(EVENT_ID)) ?: return emptyList()
        val expense = runCatching { SharedLedgerCodec.toDomainEvent(record).action }.getOrNull() as? SharedLedgerAction.ExpenseAdded ?: return emptyList()
        val debts = repository.replayGroup(context.groupId).debtById
        return expense.debts.map { it.id }.filter { (debts[it]?.remainingAmountMinor ?: 0L) > 0L }
    }

    private fun RuleContext.attribute(name: String): String = attributes[name] as? String ?: ""

    companion object {
        const val EVENT_ID: String = "eventId"
        const val DEBT_UNIT_ID: String = "debtUnitId"
        const val SPLIT_DRAFT: String = "splitDraft"
        const val AMOUNT_MINOR: String = "amountMinor"
        const val CURRENCY_CODE: String = "currencyCode"
        private val NOSTR_KEY: Regex = Regex("[0-9a-f]{64}")

        fun readRulesYaml(context: Context): String {
            return context.assets.open("group_membership.yaml").bufferedReader().use { it.readText() }
        }
    }
}
