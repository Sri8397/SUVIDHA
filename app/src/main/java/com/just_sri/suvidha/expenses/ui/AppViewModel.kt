package com.just_sri.suvidha.expenses.ui

import android.app.Application
import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.just_sri.suvidha.expenses.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.ZoneId

internal class AppViewModel(app: Application, val database: ExpenseDatabase) : AndroidViewModel(app) {
    val repository = ExpenseRepository(database)
    val groups = SharedGroupRepository(database)
    val peers = PeerSplitService(app, database)
    val backups = BackupService(app, database, peers)
    private val prefs = app.getSharedPreferences("expense_settings", Context.MODE_PRIVATE)
    var currency by mutableStateOf(prefs.getString("currency", "₹") ?: "₹"); private set
    var timezone by mutableStateOf(prefs.getString("timezone_id", "AUTO") ?: "AUTO"); private set
    var profileName by mutableStateOf(prefs.getString("profile_name", "") ?: ""); private set
    var categories by mutableStateOf(prefs.getString("categories", null)?.split("|")?.filter { it.isNotBlank() } ?: defaultExpenseCategories); private set
    var pendingBackup: ByteArray? = null
    var restorePreview by mutableStateOf<AppBackupSnapshot?>(null)
    override fun onCleared() { pendingBackup?.fill(0); pendingBackup = null; restorePreview = null; super.onCleared() }
    var publicId by mutableStateOf<String?>(null); private set
    var busy by mutableStateOf(false); private set
    var message by mutableStateOf<String?>(null)
    val zone: ZoneId get() = if (timezone == "AUTO") ZoneId.systemDefault() else runCatching { ZoneId.of(timezone) }.getOrDefault(ZoneId.systemDefault())

    init {
        refreshIdentity()
        viewModelScope.launch { PeerSplitSyncScheduler.ensurePeriodic(app) }
    }
    fun refreshIdentity() { viewModelScope.launch { try { publicId = peers.publicId() } catch (e: Exception) { message = e.message ?: "Could not open your identity." } } }
    fun updateCurrency(value: String) { currency = value; prefs.edit().putString("currency", value).apply() }
    fun updateTimezone(value: String) { timezone = value; prefs.edit().putString("timezone_id", value).apply() }
    fun updateProfileName(value: String) { profileName = value.trim().take(60); prefs.edit().putString("profile_name", profileName).apply() }
    fun updateCategories(value: List<String>) {
        categories = (value.map { it.trim() }.filter { it.isNotBlank() && it != "Other" } + "Other").distinct()
        prefs.edit().putString("categories", categories.joinToString("|")).apply()
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
    fun sync() = perform {
        val result = peers.sync()
        message = result.message
        if (result.needsRetry) PeerSplitSyncScheduler.request(getApplication())
    }
    fun sendChatMessage(groupId: String, text: String) = viewModelScope.launch {
        try {
            peers.sendChatMessage(groupId, text)
            backgroundSync()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            message = failure.message ?: "Could not send message"
        }
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
    fun projection(group: SharedGroupEntity): SharedLedgerProjection = projectSharedLedger(group.id,
        events.filter { it.groupId == group.id }.mapNotNull { runCatching { SharedLedgerCodec.toDomainEvent(it) }.getOrNull() },
        group.currencyCode, members.find { it.groupId == group.id && it.role == "owner" }?.memberId)
    fun balance(friendId: String, currencyCode: String, personalCurrencyCode: String): Double =
        (if (currencyCode == personalCurrencyCode) splits.filter { it.friendId == friendId && !it.isSettled }.sumOf { it.amount } else 0.0) +
            groupTransactions[friendId].orEmpty().filter { !it.isSettled && it.currencyCode == currencyCode }.sumOf { it.remainingAmount }
}

internal fun minorMoney(value: Long, code: String): String = "${currencySymbol(code)} ${SharedSplitMoney.format(value, code)}"
