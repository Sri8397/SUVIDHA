package com.just_sri.suvidha.expenses.ui

import android.app.Application
import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.just_sri.suvidha.expenses.*
import com.just_sri.suvidha.expenses.engine.ActionResult
import com.just_sri.suvidha.expenses.engine.ExecutionResult
import com.just_sri.suvidha.expenses.engine.RuleContext
import com.just_sri.suvidha.expenses.engine.ValidationResult
import com.just_sri.suvidha.expenses.service.GroupMembershipService
import com.just_sri.suvidha.expenses.ui.theme.ThemePreference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.ZoneId

internal class AppViewModel(app: Application, val database: ExpenseDatabase) : AndroidViewModel(app) {
    val repository = ExpenseRepository(database)
    val groups = SharedGroupRepository(database)
    val peers = PeerSplitService(app, database)
    val backups = BackupService(app, database, peers)
    val notifications = NotificationService(app)
    private val membership by lazy { GroupMembershipService(app, database, groups, peers, peers.relay) }
    private val prefs = app.getSharedPreferences("expense_settings", Context.MODE_PRIVATE)
    private val relaySettings = RelaySettings(app)
    var relays: List<String> by mutableStateOf(relaySettings.urls()); private set
    var relaysCustomized: Boolean by mutableStateOf(relaySettings.isCustom()); private set
    var currency by mutableStateOf(prefs.getString("currency", "₹") ?: "₹"); private set
    var timezone by mutableStateOf(prefs.getString("timezone_id", "AUTO") ?: "AUTO"); private set
    var profileName by mutableStateOf(prefs.getString("profile_name", "") ?: ""); private set
    var categories by mutableStateOf(prefs.getString("categories", null)?.split("|")?.filter { it.isNotBlank() } ?: defaultExpenseCategories); private set
    var groupInvitationsEnabled: Boolean by mutableStateOf(prefs.getBoolean("group_invitations_enabled", false)); private set
    var showLeftGroups: Boolean by mutableStateOf(prefs.getBoolean("show_left_groups", true)); private set
    var themePreference: ThemePreference by mutableStateOf(ThemePreference.from(prefs.getString("theme_preference", "SYSTEM"))); private set
    var pendingBackup: ByteArray? = null
    var restorePreview by mutableStateOf<AppBackupSnapshot?>(null)
    override fun onCleared() { pendingBackup?.fill(0); pendingBackup = null; restorePreview = null; super.onCleared() }
    var publicId by mutableStateOf<String?>(null); private set
    var busy by mutableStateOf(false); private set
    var message by mutableStateOf<String?>(null)
    val zone: ZoneId get() = if (timezone == "AUTO") ZoneId.systemDefault() else runCatching { ZoneId.of(timezone) }.getOrDefault(ZoneId.systemDefault())
    private val refreshService = com.just_sri.suvidha.expenses.service.RefreshService(app, database, peers)

    init {
        refreshIdentity()
        viewModelScope.launch { PeerSplitSyncScheduler.ensurePeriodic(app) }
    }
    fun refreshIdentity() { viewModelScope.launch { try { publicId = peers.publicId() } catch (e: Exception) { message = e.message ?: "Could not open your identity." } } }
    fun updateCurrency(value: String) { currency = value; prefs.edit().putString("currency", value).apply() }
    fun updateTimezone(value: String) { timezone = value; prefs.edit().putString("timezone_id", value).apply() }
    fun updateThemePreference(value: ThemePreference) {
        themePreference = value
        prefs.edit().putString("theme_preference", value.name).apply()
    }
    fun updateShowLeftGroups(value: Boolean) { showLeftGroups = value; prefs.edit().putBoolean("show_left_groups", value).apply() }
    var archivedGroupIds: Set<String> by mutableStateOf(prefs.getStringSet("archived_group_ids", emptySet()).orEmpty().toSet()); private set
    fun archiveGroups(ids: Set<String>) { archivedGroupIds = archivedGroupIds + ids; persistArchivedGroups() }
    fun unarchiveGroups(ids: Set<String>) { archivedGroupIds = archivedGroupIds - ids; persistArchivedGroups() }
    private fun persistArchivedGroups() { prefs.edit().putStringSet("archived_group_ids", archivedGroupIds).apply() }
    fun updateProfileName(value: String) { profileName = value.trim().take(60); prefs.edit().putString("profile_name", profileName).apply() }
    fun updateCategories(value: List<String>) {
        categories = (value.map { it.trim() }.filter { it.isNotBlank() && it != "Other" } + "Other").distinct()
        prefs.edit().putString("categories", categories.joinToString("|")).apply()
    }
    /** Returns an error message, or null on success. */
    fun addRelay(input: String): String? = relaySettings.add(input).also { refreshRelays() }
    fun removeRelay(url: String): String? = relaySettings.remove(url).also { refreshRelays() }
    fun resetRelays() { relaySettings.reset(); refreshRelays() }
    suspend fun probeRelay(url: String): Long? = peers.relay.probe(url)
    private fun refreshRelays() { relays = relaySettings.urls(); relaysCustomized = relaySettings.isCustom() }
    fun toggleGroupInvitations(enabled: Boolean): Unit {
        if (enabled) {
            groupInvitationsEnabled = true
            prefs.edit().putBoolean("group_invitations_enabled", true).apply()
            perform {
                try {
                    val result: ExecutionResult.Success = runRule("invitation_enabled", RuleContext(actorMemberId = peers.publicId()))
                    message = (result.actionOutcomes["generate_fresh_mls_keypackage"] as? ActionResult.Success)?.message
                } catch (failure: Exception) {
                    groupInvitationsEnabled = false
                    prefs.edit().putBoolean("group_invitations_enabled", false).apply()
                    throw failure
                }
            }
        } else {
            groupInvitationsEnabled = false
            prefs.edit().putBoolean("group_invitations_enabled", false).apply()
            perform { runRule("invitation_disabled", RuleContext(actorMemberId = peers.publicId())) }
            message = "Group invitations disabled"
        }
    }

    // Every group workflow goes through group_membership.yaml; the rules hold all permission checks.
    suspend fun leaveGroup(groupId: String): Unit {
        runRule("self_exit", groupContext(groupId))
    }

    suspend fun transferOwnership(groupId: String, newOwnerId: String): Unit {
        runRule("transfer_ownership", groupContext(groupId, targetMemberId = newOwnerId))
    }

    suspend fun closeGroup(groupId: String): Unit {
        runRule("close_group", groupContext(groupId))
    }

    /** Why the group cannot be closed right now, or null when closing is allowed. */
    suspend fun closeGroupBlocker(groupId: String): String? =
        (membership.canExecuteRule("close_group", groupContext(groupId)) as? ValidationResult.Denied)?.reason

    suspend fun removeMember(groupId: String, memberId: String): Unit {
        runRule("admin_removal", groupContext(groupId, targetMemberId = memberId))
    }

    /** Returns the delivery status message for the invitation. */
    suspend fun inviteMember(groupId: String, memberId: String): String? {
        val result: ExecutionResult.Success = runRule("invite_member", groupContext(groupId, targetMemberId = memberId))
        return (result.actionOutcomes["mls_add_member"] as? ActionResult.Success)?.message
    }

    /** True when the friend's public ID is known and they have a valid, published invite key. */
    suspend fun canInvite(groupId: String, memberId: String): Boolean =
        membership.canExecuteRule("invite_member", groupContext(groupId, targetMemberId = memberId)) is ValidationResult.Allowed

    /** Returns the new group's ID. */
    suspend fun createGroup(name: String, currencyCode: String): String {
        val context: RuleContext = RuleContext(actorMemberId = peers.publicId(), note = name, attributes = mutableMapOf(GroupMembershipService.CURRENCY_CODE to currencyCode))
        val result: ExecutionResult.Success = runRule("create_group", context)
        return (result.actionOutcomes["create_mls_group"] as ActionResult.Success).message.orEmpty()
    }

    suspend fun addSplit(groupId: String, draft: SharedExpenseDraft): Unit {
        runRule("add_split", groupContext(groupId, attributes = mutableMapOf(GroupMembershipService.SPLIT_DRAFT to draft)))
    }

    suspend fun recordPayment(groupId: String, debtUnitId: String, amountMinor: Long): Unit {
        runRule("record_payment", groupContext(groupId, attributes = mutableMapOf(GroupMembershipService.DEBT_UNIT_ID to debtUnitId, GroupMembershipService.AMOUNT_MINOR to amountMinor)))
    }

    /** False when the current user is no longer an active member, so the composer can be disabled. */
    suspend fun canPostMessage(groupId: String): Boolean =
        membership.canExecuteRule("post_message", groupContext(groupId, note = "x")) is ValidationResult.Allowed

    suspend fun deleteDebt(groupId: String, debtUnitId: String): Unit {
        runRule("delete_debt", groupContext(groupId, attributes = mutableMapOf(GroupMembershipService.DEBT_UNIT_ID to debtUnitId)))
    }

    private suspend fun groupContext(
        groupId: String,
        targetMemberId: String? = null,
        note: String? = null,
        attributes: MutableMap<String, Any?> = mutableMapOf(),
    ): RuleContext = RuleContext(groupId = groupId, actorMemberId = peers.publicId(), targetMemberId = targetMemberId, note = note, attributes = attributes)

    private suspend fun runRule(ruleId: String, context: RuleContext): ExecutionResult.Success = when (val result = membership.executeRule(ruleId, context)) {
        is ExecutionResult.Success -> result
        is ExecutionResult.ValidationFailed -> throw IllegalStateException(result.violation.reason)
        is ExecutionResult.ActionFailed -> throw IllegalStateException(result.reason)
    }

    /** App-scoped actions keep running when a user opens another screen. */
    fun perform(success: String? = null, onDone: () -> Unit = {}, action: suspend () -> Unit) {
        if (busy) return
        busy = true
        viewModelScope.launch {
            try { action(); if (success != null) message = success; onDone() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { message = failure.message ?: "Could not complete this action. Try again." }
            finally { busy = false }
        }
    }
    fun onResume() { viewModelScope.launch { PeerSplitSyncScheduler.request(getApplication()) } }
    fun sync() = viewModelScope.launch {
        refreshService.manualPullRefresh(
            setBusy = { busy = it },
            setMessage = { message = it },
            isBusy = { busy },
        )
    }
    fun renameGroup(groupId: String, name: String, onDone: () -> Unit = {}) = perform("Group renamed", onDone) {
        runRule("rename_group", groupContext(groupId, note = name))
    }
    fun sendChatMessage(groupId: String, text: String) = viewModelScope.launch {
        try {
            runRule("post_message", groupContext(groupId, note = text))
            backgroundSync()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            message = failure.message ?: "Could not send message"
        }
    }
    fun cancelSplit(groupId: String, eventId: String, onDone: () -> Unit = {}) = perform("Split cancelled", onDone) {
        runRule("cancel_split", groupContext(groupId, attributes = mutableMapOf(GroupMembershipService.EVENT_ID to eventId)))
    }
    fun backgroundSync() = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        try { peers.syncIfIdle() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { /* Automatic work stays quiet; manual sync reports failures. */ }
    }
}

internal data class AppData(
    val expenses: List<Expense> = emptyList(), val friends: List<Friend> = emptyList(),
    val splits: List<SplitTransaction> = emptyList(), val groups: List<SharedGroupEntity> = emptyList(),
    val members: List<SharedGroupMemberEntity> = emptyList(), val events: List<SharedLedgerEventRecord> = emptyList(),
    val links: List<SharedMemberFriendLinkEntity> = emptyList(), val ownId: String? = null,
) {
    val groupTransactions by lazy { computeFriendGroupTransactions(groups, members, events, links, ownId) }
    fun label(id: String): String = if (id == ownId) "You" else links.find { it.memberId == id }?.let { link -> friends.find { it.id == link.friendId }?.name }
        ?: members.find { it.memberId == id }?.displayName?.takeUnless { it.startsWith("Nostr") } ?: "${id.take(6)}…${id.takeLast(4)}"
    fun friendKey(id: String): String? = links.find { it.friendId == id }?.memberId
    fun hasLeft(groupId: String): Boolean = members.any { it.groupId == groupId && it.memberId == ownId && !it.isActive }
    fun projection(group: SharedGroupEntity): SharedLedgerProjection = projectSharedLedger(group.id,
        events.filter { it.groupId == group.id }.mapNotNull { runCatching { SharedLedgerCodec.toDomainEvent(it) }.getOrNull() },
        group.currencyCode, members.find { it.groupId == group.id && it.role == "owner" }?.memberId)
    fun balance(friendId: String, currencyCode: String, personalCurrencyCode: String): Double =
        groupTransactions[friendId].orEmpty().filter { !it.isSettled && it.currencyCode == currencyCode }.sumOf { it.remainingAmount }
}

internal fun minorMoney(value: Long, code: String): String = "${currencySymbol(code)} ${SharedSplitMoney.format(value, code)}"
