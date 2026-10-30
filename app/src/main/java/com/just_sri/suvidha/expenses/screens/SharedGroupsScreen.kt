package com.just_sri.suvidha.expenses.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.KeyboardType
import com.just_sri.suvidha.expenses.CardSurface
import com.just_sri.suvidha.expenses.ExpenseDatabase
import com.just_sri.suvidha.expenses.EncryptedKeyPackageBundleStore
import com.just_sri.suvidha.expenses.EncryptedMlsGroupStateStore
import com.just_sri.suvidha.expenses.Danger
import com.just_sri.suvidha.expenses.Green
import com.just_sri.suvidha.expenses.Ink
import com.just_sri.suvidha.expenses.Muted
import com.just_sri.suvidha.expenses.OffWhite
import com.just_sri.suvidha.expenses.NostrIdentityStore
import com.just_sri.suvidha.expenses.PeerSplitRelaySync
import com.just_sri.suvidha.expenses.RelaySyncReport
import com.just_sri.suvidha.expenses.SharedGroupEntity
import com.just_sri.suvidha.expenses.SharedGroupRepository
import com.just_sri.suvidha.expenses.SyncNotificationHelper
import com.just_sri.suvidha.expenses.SharedLedgerAction
import com.just_sri.suvidha.expenses.SharedLedgerEvent
import com.just_sri.suvidha.expenses.SharedDebtUnit
import com.just_sri.suvidha.expenses.SharedDebtBalance
import com.just_sri.suvidha.expenses.SharedSettlementAllocation
import com.just_sri.suvidha.expenses.SharedExpenseParticipant
import com.just_sri.suvidha.expenses.SharedLedgerCodec
import com.just_sri.suvidha.expenses.projectSharedLedger
import com.just_sri.suvidha.expenses.components.AppPrimaryButton
import com.just_sri.suvidha.expenses.components.AppSecondaryButton
import com.just_sri.suvidha.expenses.components.AppSwipeItem
import com.just_sri.suvidha.expenses.components.ScreenHeader
import com.just_sri.suvidha.expenses.components.AppListRow
import com.just_sri.suvidha.expenses.components.AppListStyle
import com.just_sri.suvidha.expenses.components.AppSectionHeader
import com.just_sri.suvidha.expenses.components.AppEmptyState
import com.just_sri.suvidha.expenses.components.AppTopBar
import com.just_sri.suvidha.expenses.LocalZoneId
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.vitorpamplona.marmot.client.MarmotManager
import com.vitorpamplona.marmot.client.MarmotPublisher
import com.vitorpamplona.marmot.mip01Groups.MarmotGroupData
import com.vitorpamplona.quartz.marmot.mip00KeyPackages.KeyPackageEvent
import com.vitorpamplona.quartz.nip01Core.core.Event
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Currency
import java.security.SecureRandom
import java.util.UUID

@Composable
internal fun SharedGroupsPage(database: ExpenseDatabase, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repository = remember(database) { SharedGroupRepository(database) }
    val identityStore = remember(context) { NostrIdentityStore(context.applicationContext) }
    val mlsStore = remember(context) { EncryptedMlsGroupStateStore(context.applicationContext) }
    val keyPackageStore = remember(context) { EncryptedKeyPackageBundleStore(context.applicationContext) }
    val relaySync = remember { PeerSplitRelaySync() }
    val groups by repository.observeGroups().collectAsState(emptyList())
    val pendingWelcomes by repository.observePendingWelcomes().collectAsState(emptyList())
    val scope = rememberCoroutineScope()
    var showCreate by remember { mutableStateOf(false) }
    var groupName by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("INR") }
    var selected by remember { mutableStateOf<SharedGroupEntity?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var syncing by remember { mutableStateOf(false) }
    var publicKeyHex by remember { mutableStateOf<String?>(null) }
    var showIdentity by remember { mutableStateOf(false) }
    var keyPackageStatus by remember { mutableStateOf<String?>(null) }
    var publishingKeyPackage by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(identityStore, mlsStore) {
        runCatching {
            val identity = withContext(Dispatchers.IO) { identityStore.getOrCreate() }
            publicKeyHex = identity.publicKeyHex
            withContext(Dispatchers.IO) {
                managerFor(identity, mlsStore, keyPackageStore, relaySync).restoreAll()
            }
        }.onFailure { error = "Could not open encrypted group keys: ${it.message ?: "unknown error"}" }
    }

    if (selected != null) {
        GroupDetailPage(
            database = database,
            group = selected!!,
            modifier = modifier,
            actorMemberId = publicKeyHex,
            identityStore = identityStore,
            mlsStore = mlsStore,
            keyPackageStore = keyPackageStore,
            relaySync = relaySync,
            onBack = { selected = null },
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize().then(modifier).padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp)) {
        ScreenHeader("BAKAYA  /  GROUPS", "Shared groups")
        Text("Split expenses. Keep everyone in sync.", color = Muted, fontSize = 13.sp)

        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AppPrimaryButton(
                text = "New group",
                onClick = { error = null; showCreate = true },
                enabled = !syncing && !publishingKeyPackage,
                height = 52.dp,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AppSecondaryButton(
                    text = "My Nostr key",
                    onClick = { keyPackageStatus = null; showIdentity = true },
                    modifier = Modifier.weight(1f),
                    height = 48.dp,
                )
                AppSecondaryButton(
                    text = if (syncing) "Syncing…" else "Sync now",
                    onClick = {
                    if (syncing) return@AppSecondaryButton
                    syncing = true
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        !SyncNotificationHelper.hasNotificationPermission(context)
                    ) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    scope.launch {
                        try {
                            val identity = withContext(Dispatchers.IO) { identityStore.getOrCreate() }
                            val manager = managerFor(identity, mlsStore, keyPackageStore, relaySync)
                            withContext(Dispatchers.IO) { manager.restoreAll() }
                            withContext(Dispatchers.IO) {
                                repository.pendingWelcomes().forEach { pending ->
                                    if (relaySync.publish(Event.fromJson(pending.eventJson), relaySync.relayUrls().toSet())) repository.markWelcomePublished(pending.id)
                                }
                                manager.activeGroupIds().forEach { groupId ->
                                    if (repository.findGroup(groupId) == null) persistJoinedGroup(manager, groupId, repository, identity.publicKeyHex)
                                }
                                groups.forEach { group ->
                                    repository.pendingEncryptedEvents(group.id).forEach { pending ->
                                        pending.transportEventJson?.let { json ->
                                            val event = Event.fromJson(json)
                                            if (relaySync.publish(event, relaySync.relayUrls().toSet())) repository.markEventPublished(pending.id)
                                        }
                                    }
                                }
                            }
                            val report = withContext(Dispatchers.IO) {
                                relaySync.sync(
                                    manager,
                                    identity.signer,
                                    knownEventIds = repository.storedTransportEvents().mapTo(mutableSetOf()) { Event.fromJson(it).id },
                                    onLedgerEvent = { event, outerJson -> repository.appendRemoteEvent(event, outerJson) },
                                    onWelcomeJoined = { joinedId -> persistJoinedGroup(manager, joinedId, repository, identity.publicKeyHex) },
                                    onMembersChanged = { groupId, memberIds, admins ->
                                        repository.syncMemberRoster(groupId, memberIds, admins)
                                    },
                                )
                            }
                            val (title, message) = syncNotificationDetails(report)
                            val posted = SyncNotificationHelper.showSyncNotification(context, title, message)
                            if (!posted) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                }
                            }
                        } catch (failure: Exception) {
                            val title = "Sync Error"
                            val message = "Sync could not finish. Check your connection and retry."
                            val posted = SyncNotificationHelper.showSyncNotification(context, title, message)
                            if (!posted) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                }
                            }
                        } finally { syncing = false }
                    }
                    },
                    enabled = !syncing && !publishingKeyPackage,
                    modifier = Modifier.weight(1f),
                    height = 48.dp,
                )
            }
        }
        if (pendingWelcomes.isNotEmpty()) Text("${pendingWelcomes.size} invitation(s) waiting to send. Tap Sync now to retry.", color = Muted, modifier = Modifier.padding(top = 8.dp))
        error?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
        if (groups.isEmpty()) {
            AppEmptyState(
                "Share expenses together",
                "Create a group, invite your friends and keep track of who owes what.",
                modifier = Modifier.padding(top = 20.dp),
            )
        } else {
            AppSectionHeader("Your groups", groups.size)
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(AppListStyle.Spacing),
                contentPadding = PaddingValues(vertical = 10.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(groups, key = { it.id }) { group ->
                    AppListRow(
                        title = group.name,
                        subtitle = "${group.currencyCode} · Shared expenses",
                        onClick = { selected = group },
                        enabled = !syncing && !publishingKeyPackage,
                    )
                }
            }
        }
    }

    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("Create a group") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = groupName, onValueChange = { groupName = it; error = null }, label = { Text("Group name") }, singleLine = true)
                    OutlinedTextField(value = currency, onValueChange = { currency = it.uppercase().take(3); error = null }, label = { Text("Currency code") }, singleLine = true)
                    error?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = groupName.trim()
                    val code = currency.trim().uppercase()
                    if (name.isBlank()) { error = "Enter a group name."; return@TextButton }
                    if (!code.matches(Regex("[A-Z]{3}")) || runCatching { Currency.getInstance(code).defaultFractionDigits < 0 }.getOrDefault(true)) { error = "Enter a valid currency code, such as INR."; return@TextButton }
                    scope.launch {
                        runCatching {
                            val identity = withContext(Dispatchers.IO) { identityStore.getOrCreate() }
                            val groupId = ByteArray(32).also(SecureRandom()::nextBytes).joinToString("") { "%02x".format(it) }
                            withContext(Dispatchers.IO) {
                                managerFor(identity, mlsStore, keyPackageStore, relaySync).createGroup(
                                    groupId,
                                    MarmotGroupData(
                                        nostrGroupId = groupId,
                                        name = name,
                                        description = "suvidha.currency=$code",
                                        adminPubkeys = listOf(identity.publicKeyHex),
                                        relays = relaySync.relayUrls().map { it.url },
                                    ),
                                )
                            }
                            runCatching {
                                repository.createGroup(
                                    id = groupId,
                                    name = name,
                                    currencyCode = code,
                                    ownerMemberId = identity.publicKeyHex,
                                    ownerDisplayName = "You · ${identity.publicKeyHex.take(8)}…",
                                )
                            }.getOrElse { failure ->
                                withContext(Dispatchers.IO) { mlsStore.delete(groupId) }
                                throw failure
                            }
                        }.onSuccess {
                            showCreate = false
                            groupName = ""
                        }.onFailure { error = it.message ?: "Could not create group." }
                    }
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("Cancel") } },
        )
    }

    if (showIdentity) {
        val key = publicKeyHex
        AlertDialog(
            onDismissRequest = { if (!publishingKeyPackage) showIdentity = false },
            title = { Text("My Nostr identity") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This public key identifies you in PeerSplit groups. Share it with group admins.", color = Muted)
                    if (key == null) Text("Preparing your identity…", color = Muted)
                    else SelectionContainer { Text(key, color = Ink) }
                    keyPackageStatus?.let { Text(it, color = Muted) }
                }
            },
            confirmButton = {
                Column(horizontalAlignment = Alignment.End) {
                    TextButton(enabled = key != null, onClick = {
                        key?.let {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("PeerSplit Nostr public key", it))
                            Toast.makeText(context, "Public key copied", Toast.LENGTH_SHORT).show()
                        }
                    }) { Text("Copy public key") }
                    TextButton(enabled = !publishingKeyPackage && !syncing && key != null, onClick = {
                        publishingKeyPackage = true
                        keyPackageStatus = "Creating and publishing your invite key…"
                        scope.launch {
                            try {
                                val identity = withContext(Dispatchers.IO) { identityStore.getOrCreate() }
                                val manager = managerFor(identity, mlsStore, keyPackageStore, relaySync)
                                withContext(Dispatchers.IO) { manager.restoreAll() }
                                val event = withContext(Dispatchers.IO) {
                                    manager.generateKeyPackageEvent(relaySync.relayUrls(), "primary", false)
                                }
                                val accepted = withContext(Dispatchers.IO) {
                                    relaySync.publish(event, relaySync.relayUrls().toSet())
                                }
                                if (!accepted) error("No relay confirmed the invite key. Check your connection and retry.")
                                withContext(Dispatchers.IO) { manager.confirmKeyPackagePublished(event) }
                                keyPackageStatus = "Invite key published. Share your public key with group members."
                            } catch (failure: Exception) {
                                keyPackageStatus = failure.message ?: "Could not publish the invite key."
                            } finally {
                                publishingKeyPackage = false
                            }
                        }
                    }) { Text(if (publishingKeyPackage) "Publishing…" else "Publish invite key") }
                }
            },
            dismissButton = { TextButton(enabled = !publishingKeyPackage, onClick = { showIdentity = false }) { Text("Close") } },
        )
    }
}

private fun managerFor(
    identity: com.just_sri.suvidha.expenses.NostrIdentity,
    mlsStore: EncryptedMlsGroupStateStore,
    keyPackageStore: EncryptedKeyPackageBundleStore,
    relaySync: PeerSplitRelaySync,
) = MarmotManager(
    signer = identity.signer,
    store = mlsStore,
    keyPackageStore = keyPackageStore,
    publisher = MarmotPublisher { event, relays -> relaySync.publish(event, relays) },
    publishObligationStore = mlsStore.transportStore,
    ingestDedupStore = mlsStore.transportStore.ingestDedup,
)

private fun syncNotificationDetails(report: RelaySyncReport): Pair<String, String> = when {
    report.relaysResponded == 0 -> "Sync Failed" to "No relay responded. Your local group data is still available."
    report.errors > 0 -> "Sync Finished with Warnings" to "Checked ${report.relaysResponded} relay(s); processed ${report.groupEventsProcessed} group update(s) and ${report.welcomesProcessed} invite(s). Some requests or events failed; retry to check for more."
    else -> "Sync Complete" to "Checked ${report.relaysResponded} relay(s); processed ${report.groupEventsProcessed} encrypted group event(s) and ${report.welcomesProcessed} invite(s)."
}

private suspend fun persistJoinedGroup(
    manager: MarmotManager,
    groupId: String,
    repository: SharedGroupRepository,
    ownMemberId: String,
) {
    val view = manager.groupView(groupId) ?: error("Joined group metadata is unavailable.")
    val currency = Regex("(?:^|;)\\s*suvidha\\.currency=([A-Z]{3})(?:;|$)")
        .find(view.description.orEmpty())?.groupValues?.get(1)
        ?: repository.findGroup(groupId)?.currencyCode
        ?: error("This invitation is missing Suvidha currency metadata.")
    val memberIds = manager.memberPubkeys(groupId).mapTo(linkedSetOf()) { it.pubkey }
    memberIds += ownMemberId
    repository.createJoinedGroup(groupId, view.name.ifBlank { "Shared group" }, currency, memberIds, view.adminPubkeys.toSet())
}

private fun formatMinor(value: Long, currencyCode: String): String {
    val digits = runCatching { Currency.getInstance(currencyCode).defaultFractionDigits.coerceAtLeast(0) }.getOrDefault(2)
    return "$currencyCode ${BigDecimal.valueOf(value, digits).setScale(digits).toPlainString()}"
}

private suspend fun saveAndPublishLedgerEvent(
    event: SharedLedgerEvent,
    repository: SharedGroupRepository,
    identityStore: NostrIdentityStore,
    mlsStore: EncryptedMlsGroupStateStore,
    keyPackageStore: EncryptedKeyPackageBundleStore,
    relaySync: PeerSplitRelaySync,
    onCommitted: () -> Unit,
) {
    val outer = withContext(Dispatchers.IO) {
    val identity = identityStore.getOrCreate()
    require(identity.publicKeyHex == event.actorMemberId)
    val manager = managerFor(identity, mlsStore, keyPackageStore, relaySync)
    manager.restoreAll()
    val outer = com.just_sri.suvidha.expenses.PeerSplitLedgerWire.encrypt(manager, event)
    // The ledger row and its encrypted outbox payload commit in one Room transaction.
    repository.appendLocalEvent(event, outer.toJson())
    outer
    }
    onCommitted()
    withContext(Dispatchers.IO) {
        if (relaySync.publish(outer, relaySync.relayUrls().toSet())) repository.markEventPublished(event.eventId)
    }
}

@Composable
private fun GroupDetailPage(
    database: ExpenseDatabase,
    group: SharedGroupEntity,
    modifier: Modifier,
    actorMemberId: String?,
    identityStore: NostrIdentityStore,
    mlsStore: EncryptedMlsGroupStateStore,
    keyPackageStore: EncryptedKeyPackageBundleStore,
    relaySync: PeerSplitRelaySync,
    onBack: () -> Unit,
) {
    val repository = remember(database) { SharedGroupRepository(database) }
    val records by repository.observeGroupEvents(group.id).collectAsState(emptyList())
    val members by repository.observeGroupMembers(group.id).collectAsState(emptyList())
    val scope = rememberCoroutineScope()
    var showInvite by remember { mutableStateOf(false) }
    var inviteKey by remember { mutableStateOf("") }
    var inviteStatus by remember { mutableStateOf<String?>(null) }
    var inviting by remember { mutableStateOf(false) }
    var showExpense by remember { mutableStateOf(false) }
    var expenseFlowResetKey by remember { mutableStateOf(0) }
    var expenseStatus by remember { mutableStateOf<String?>(null) }
    var savingExpense by remember { mutableStateOf(false) }
    var selectedDebt by remember { mutableStateOf<SharedDebtBalance?>(null) }
    var settlementAmount by remember { mutableStateOf("") }
    var settlementStatus by remember { mutableStateOf<String?>(null) }
    var settling by remember { mutableStateOf(false) }
    var linkMemberId by remember { mutableStateOf<String?>(null) }
    var selectedTab by rememberSaveable(group.id) { mutableStateOf(0) }
    var pendingDeleteDebt by remember { mutableStateOf<SharedDebtBalance?>(null) }
    var deletingDebt by remember { mutableStateOf(false) }
    var deleteDebtError by remember { mutableStateOf<String?>(null) }
    val friends by remember(database) { database.friends().observeAll() }.collectAsState(emptyList())
    val identityLinks by remember(repository) { repository.observeMemberFriendLinks() }.collectAsState(emptyList())
    val linkedFriends = identityLinks.mapNotNull { link -> friends.find { it.id == link.friendId }?.let { link.memberId to it } }.toMap()
    val ledgerEvents = records.mapNotNull { runCatching { SharedLedgerCodec.toDomainEvent(it) }.getOrNull() }
    val ownerMemberId = members.find { it.role == "owner" && it.isActive }?.memberId
    val isOwner = actorMemberId != null && actorMemberId == ownerMemberId
    val projection = remember(group, ledgerEvents, ownerMemberId) { projectSharedLedger(group.id, ledgerEvents, group.currencyCode, ownerMemberId) }
    BackHandler(enabled = !showExpense && !showInvite && selectedDebt == null && pendingDeleteDebt == null && linkMemberId == null, onBack = onBack)
    fun memberLabel(id: String): String = if (id == actorMemberId) "You" else linkedFriends[id]?.name ?: members.find { it.memberId == id }?.displayName ?: "Nostr · ${id.take(8)}…"
    val membersScroll = rememberLazyListState()
    val activitiesScroll = rememberLazyListState()
    val zoneId = LocalZoneId.current
    val activityDate = remember { DateTimeFormatter.ofPattern("d MMM · h:mm a", Locale.getDefault()) }
    val outstandingDebts = projection.debtById.values.filter { it.remainingAmountMinor > 0 }
    val ownBalance = actorMemberId?.let { projection.balanceByMemberId[it] } ?: 0L
    fun openPayment(debt: SharedDebtBalance) {
        selectedDebt = debt
        val digits = Currency.getInstance(group.currencyCode).defaultFractionDigits.coerceAtLeast(0)
        settlementAmount = BigDecimal.valueOf(debt.remainingAmountMinor, digits).toPlainString()
        settlementStatus = null
    }
    Column(Modifier.fillMaxSize().then(modifier)) {
        AppTopBar(
            title = group.name,
            subtitle = "${members.count { it.isActive }} members · ${group.currencyCode}",
            onBack = onBack,
            actions = {
                if (isOwner) TextButton(onClick = { inviteKey = ""; inviteStatus = null; showInvite = true }) {
                    Text("Invite", color = Green)
                }
            },
        )
        TabRow(selectedTabIndex = selectedTab, containerColor = OffWhite, contentColor = Green) {
            listOf("Members", "Activities").forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            state = if (selectedTab == 0) membersScroll else activitiesScroll,
            contentPadding = PaddingValues(horizontal = AppListStyle.PagePadding, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(AppListStyle.Spacing),
        ) {
            if (projection.pendingEventIds.isNotEmpty()) item {
                Text("Some updates are waiting for earlier activity. Sync the group again to complete its history.", color = Muted, fontSize = 12.sp)
            }
            if (projection.rejectedEvents.isNotEmpty()) item {
                Text("${projection.rejectedEvents.size} conflicting updates could not be applied.", color = Danger, fontSize = 12.sp)
            }
            if (selectedTab == 0) {
                item {
                    AppListRow(
                        title = "Your group balance",
                        subtitle = if (ownBalance > 0) "You are owed" else if (ownBalance < 0) "You owe" else "All settled",
                        leadingText = "↔",
                        value = formatMinor(kotlin.math.abs(ownBalance), group.currencyCode),
                        accent = if (ownBalance < 0) Danger else Green,
                    )
                }
                item {
                    AppPrimaryButton(
                        text = "Add shared expense",
                        onClick = { expenseFlowResetKey++; expenseStatus = null; showExpense = true },
                        enabled = actorMemberId != null && members.count { it.isActive } > 1,
                        height = 48.dp,
                    )
                }
                if (members.count { it.isActive } <= 1) item {
                    Text("Invite someone to start sharing expenses.", color = Muted, fontSize = 12.sp)
                }
                inviteStatus?.let { status -> item { Text(status, color = Muted, fontSize = 12.sp) } }
                item { AppSectionHeader("Members", members.size) }
                items(members, key = { "member:${it.memberId}" }) { member ->
                    val own = member.memberId == actorMemberId
                    val balance = if (own) ownBalance else projection.debtById.values.sumOf { debt ->
                        when {
                            debt.creditorMemberId == actorMemberId && debt.debtorMemberId == member.memberId -> debt.remainingAmountMinor
                            debt.debtorMemberId == actorMemberId && debt.creditorMemberId == member.memberId -> -debt.remainingAmountMinor
                            else -> 0L
                        }
                    }
                    val balanceLabel = when {
                        own && balance > 0 -> "You are owed"
                        own && balance < 0 -> "You owe"
                        balance > 0 -> "Owes you"
                        balance < 0 -> "You owe"
                        else -> "Settled"
                    }
                    AppListRow(
                        title = memberLabel(member.memberId),
                        subtitle = if (!member.isActive) "Former member" else if (member.role == "owner") "Group owner" else "Member",
                        value = formatMinor(kotlin.math.abs(balance), group.currencyCode),
                        valueLabel = balanceLabel,
                        accent = if (balance < 0) Danger else if (balance > 0) Green else Muted,
                        trailing = if (!own) {{
                            TextButton(onClick = { linkMemberId = member.memberId }) {
                                Text(if (member.memberId in linkedFriends) "Contact" else "Link", color = Green, fontSize = 12.sp)
                            }
                        }} else null,
                    )
                }
                item { AppSectionHeader("Outstanding debts", outstandingDebts.size) }
                if (outstandingDebts.isEmpty()) item {
                    AppEmptyState("All caught up", "There are no outstanding debts in this group.")
                }
                if (isOwner && outstandingDebts.isNotEmpty()) item {
                    Text("Swipe left to delete · right to record payment", color = Muted, fontSize = 12.sp)
                }
                items(outstandingDebts, key = { "debt:${it.debtUnitId}" }) { debt ->
                    val debtor = memberLabel(debt.debtorMemberId)
                    val creditor = memberLabel(debt.creditorMemberId)
                    AppSwipeItem(
                        onLeftSwipe = if (isOwner) {{ deleteDebtError = null; pendingDeleteDebt = debt }} else null,
                        leftIcon = Icons.Outlined.Delete,
                        onRightSwipe = if (isOwner) {{ openPayment(debt) }} else null,
                        rightLabel = "Settle",
                        rightIcon = Icons.Outlined.Check,
                    ) {
                        AppListRow(
                            title = if (debt.debtorMemberId == actorMemberId) "You owe $creditor" else "$debtor owes $creditor",
                            subtitle = "Outstanding payment",
                            leadingText = "↗",
                            value = formatMinor(debt.remainingAmountMinor, group.currencyCode),
                            accent = if (debt.debtorMemberId == actorMemberId) Danger else Green,
                        )
                    }
                }
            } else {
                item { AppSectionHeader("Group activity", records.size) }
                if (records.isEmpty()) item {
                    AppEmptyState("No activity yet", "Shared expenses, payments and deleted debts will appear here.")
                }
                items(records, key = { "event:${it.id}" }) { record ->
                    val actionTitle = when (record.actionType) {
                        "expense_added" -> "Shared expense"
                        "settlement_recorded" -> "Payment recorded"
                        "debt_removed" -> "Debt deleted"
                        else -> "Group update"
                    }
                    val syncLabel = when (record.syncState) {
                        "published", "received" -> "Synced"
                        else -> "Waiting to sync"
                    }
                    val date = activityDate.format(Instant.ofEpochMilli(record.createdAtMillis).atZone(zoneId))
                    AppListRow(
                        title = record.description.ifBlank { actionTitle },
                        subtitle = "$actionTitle · ${memberLabel(record.actorMemberId)}\n$date · $syncLabel",
                        leadingText = when (record.actionType) { "settlement_recorded" -> "✓"; "debt_removed" -> "−"; else -> "+" },
                        accent = if (record.actionType == "debt_removed") Muted else Green,
                    )
                }
            }
        }
    }

    if (showExpense && actorMemberId != null) {
        SharedFlowWindow(
            onDismissRequest = { if (!savingExpense) showExpense = false },
        ) {
            androidx.compose.runtime.key(expenseFlowResetKey) {
                SharedExpenseFlowScreen(
                    groupName = group.name,
                    currencyCode = group.currencyCode,
                    ownMemberId = actorMemberId,
                    members = members.filter { it.isActive && it.memberId != actorMemberId }.map { SharedExpenseParticipant(it.memberId, memberLabel(it.memberId)) },
                    saving = savingExpense,
                    error = expenseStatus,
                    onBack = { if (!savingExpense) showExpense = false },
                    onSave = save@{ draft, addAnother ->
                        if (savingExpense) return@save
                        savingExpense = true
                        expenseStatus = null
                        scope.launch {
                            var committed = false
                            try {
                                val identity = withContext(Dispatchers.IO) { identityStore.getOrCreate() }
                                val activeIds = members.filter { it.isActive && it.memberId != identity.publicKeyHex }.map { it.memberId }.toSet()
                                require(draft.amountsMinorByMemberId.isNotEmpty() && activeIds.containsAll(draft.amountsMinorByMemberId.keys)) { "Select active group members." }
                                require(draft.amountsMinorByMemberId.values.all { it > 0 }) { "Each owed amount must be positive." }
                                val event = SharedLedgerEvent(
                                    eventId = UUID.randomUUID().toString(),
                                    groupId = group.id,
                                    actorMemberId = identity.publicKeyHex,
                                    parentEventIds = projection.acceptedEventIds,
                                    occurredAtMillis = System.currentTimeMillis(),
                                    action = SharedLedgerAction.ExpenseAdded(group.currencyCode, draft.amountsMinorByMemberId.toSortedMap().map { (memberId, amount) ->
                                        SharedDebtUnit(UUID.randomUUID().toString(), identity.publicKeyHex, memberId, amount)
                                    }),
                                    description = draft.note.trim().take(200).ifBlank { "Shared expense" },
                                )
                                saveAndPublishLedgerEvent(event, repository, identityStore, mlsStore, keyPackageStore, relaySync) {
                                    committed = true
                                    savingExpense = false
                                    if (addAnother) expenseFlowResetKey++ else showExpense = false
                                }
                            } catch (cancelled: CancellationException) { throw cancelled }
                            catch (failure: Exception) {
                                if (!committed) expenseStatus = failure.message ?: "Could not save this split."
                            } finally { if (!committed) savingExpense = false }
                        }
                    },
                )
            }
        }
    }

    selectedDebt?.let { debt ->
        AlertDialog(
            onDismissRequest = { if (!settling) selectedDebt = null },
            title = { Text("Record a payment") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Record money already paid. This action updates the shared ledger and does not transfer money.", color = Muted)
                    OutlinedTextField(settlementAmount, { settlementAmount = it.take(16); settlementStatus = null }, label = { Text("Paid (${group.currencyCode})") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    settlementStatus?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(enabled = !settling, onClick = {
                    val amount = runCatching {
                        val digits = Currency.getInstance(group.currencyCode).defaultFractionDigits.coerceAtLeast(0)
                        BigDecimal(settlementAmount.trim()).movePointRight(digits).setScale(0, RoundingMode.UNNECESSARY).longValueExact()
                    }.getOrNull()
                    if (amount == null || amount <= 0 || amount > debt.remainingAmountMinor) {
                        settlementStatus = "Enter an amount up to ${formatMinor(debt.remainingAmountMinor, group.currencyCode)}."
                        return@TextButton
                    }
                    settling = true
                    scope.launch {
                        var committed = false
                        try {
                            val identity = withContext(Dispatchers.IO) { identityStore.getOrCreate() }
                            require(isOwner && identity.publicKeyHex == ownerMemberId) {
                                "Only the group owner can record this payment."
                            }
                            val event = SharedLedgerEvent(
                                eventId = UUID.randomUUID().toString(),
                                groupId = group.id,
                                actorMemberId = identity.publicKeyHex,
                                parentEventIds = projection.acceptedEventIds,
                                occurredAtMillis = System.currentTimeMillis(),
                                action = SharedLedgerAction.SettlementRecorded(listOf(SharedSettlementAllocation(debt.debtUnitId, amount))),
                                description = "Payment recorded · ${formatMinor(amount, group.currencyCode)}",
                            )
                            saveAndPublishLedgerEvent(event, repository, identityStore, mlsStore, keyPackageStore, relaySync) {
                                committed = true
                                selectedDebt = null
                                settling = false
                            }
                        } catch (cancelled: CancellationException) { throw cancelled }
                        catch (failure: Exception) {
                            if (!committed) settlementStatus = failure.message ?: "Could not record this payment."
                        } finally { if (!committed) settling = false }
                    }
                }) { Text(if (settling) "Saving…" else "Record payment") }
            },
            dismissButton = { TextButton(enabled = !settling, onClick = { selectedDebt = null }) { Text("Cancel") } },
        )
    }

    pendingDeleteDebt?.let { debt ->
        AlertDialog(
            onDismissRequest = { if (!deletingDebt) pendingDeleteDebt = null },
            title = { Text("Delete outstanding debt?") },
            text = {
                Column {
                    Text("The remaining debt will be removed for all group members. Earlier expenses and payments stay in the activity history.")
                    deleteDebtError?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(enabled = !deletingDebt, onClick = {
                    deletingDebt = true
                    deleteDebtError = null
                    scope.launch {
                        var committed = false
                        try {
                            val identity = withContext(Dispatchers.IO) { identityStore.getOrCreate() }
                            require(isOwner && identity.publicKeyHex == ownerMemberId) { "Only the group owner can delete a debt." }
                            val event = SharedLedgerEvent(
                                eventId = UUID.randomUUID().toString(),
                                groupId = group.id,
                                actorMemberId = identity.publicKeyHex,
                                parentEventIds = projection.acceptedEventIds,
                                occurredAtMillis = System.currentTimeMillis(),
                                action = SharedLedgerAction.DebtRemoved(debt.debtUnitId),
                                description = "Outstanding debt removed · ${formatMinor(debt.remainingAmountMinor, group.currencyCode)}",
                            )
                            saveAndPublishLedgerEvent(event, repository, identityStore, mlsStore, keyPackageStore, relaySync) {
                                committed = true
                                pendingDeleteDebt = null
                                deletingDebt = false
                            }
                        } catch (cancelled: CancellationException) { throw cancelled }
                        catch (failure: Exception) {
                            if (!committed) deleteDebtError = failure.message ?: "Could not delete this debt."
                        } finally { if (!committed) deletingDebt = false }
                    }
                }) { Text(if (deletingDebt) "Deleting…" else "Delete") }
            },
            dismissButton = { TextButton(enabled = !deletingDebt, onClick = { pendingDeleteDebt = null }) { Text("Cancel") } },
        )
    }

    linkMemberId?.let { memberId ->
        MemberFriendLinkDialog(repository, group.id, memberId, friends, identityLinks.find { it.memberId == memberId }?.friendId, onDismiss = { linkMemberId = null })
    }

    if (showInvite) {
        AlertDialog(
            onDismissRequest = { if (!inviting) showInvite = false },
            title = { Text("Invite a participant") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter their 64-character Nostr public key in hex. They must have published an MLS KeyPackage to one of the configured relays.", color = Muted)
                    val savedFriends = identityLinks.filter { link -> link.memberId != actorMemberId && members.none { it.memberId == link.memberId && it.isActive } }.mapNotNull { link -> friends.find { it.id == link.friendId }?.let { link.memberId to it.name } }
                    if (savedFriends.isNotEmpty()) {
                        Text("Saved friends", color = Ink, fontWeight = FontWeight.SemiBold)
                        LazyColumn(Modifier.heightIn(max = 140.dp)) {
                            items(savedFriends, key = { it.first }) { (memberId, name) ->
                                TextButton(enabled = !inviting, onClick = { inviteKey = memberId; inviteStatus = null }) { Text("$name · ${memberId.take(8)}…") }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = inviteKey,
                        onValueChange = { inviteKey = it.trim().lowercase(); inviteStatus = null },
                        label = { Text("Nostr public key (hex)") },
                        singleLine = true,
                        enabled = !inviting,
                    )
                    inviteStatus?.let { Text(it, color = Muted) }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !inviting,
                    onClick = {
                        if (!inviteKey.matches(Regex("[0-9a-f]{64}"))) {
                            inviteStatus = "Enter a valid 64-character public key in hex."
                            return@TextButton
                        }
                        if (members.any { it.memberId == inviteKey && it.isActive }) {
                            inviteStatus = "This person is already in the group. Use Sync now to retry any pending invitation."
                            return@TextButton
                        }
                        inviting = true
                        inviteStatus = "Looking for their published KeyPackage…"
                        val targetPubKey = inviteKey
                        scope.launch {
                            try {
                                val invitation = withContext(Dispatchers.IO) {
                                    val identity = identityStore.getOrCreate()
                                    val manager = managerFor(identity, mlsStore, keyPackageStore, relaySync)
                                    manager.restoreAll()
                                    val published = relaySync.fetchKeyPackageEvents(targetPubKey)
                                        .asSequence()
                                        .filter { it.pubKey.equals(targetPubKey, ignoreCase = true) }
                                        .mapNotNull { it as? KeyPackageEvent }
                                        .firstOrNull { it.hasValidEncoding() }
                                        ?: error("No published KeyPackage was found. Ask them to publish one, then retry.")
                                    manager.addMember(group.id, published, relaySync.relayUrls())
                                }
                                val welcome = invitation.second ?: error("The membership commit was not confirmed. Check your connection and retry.")
                                val delivered = withContext(Dispatchers.IO) {
                                    repository.queueWelcome(group.id, targetPubKey, welcome.giftWrapEvent.id, welcome.giftWrapEvent.toJson())
                                    val accepted = relaySync.publish(welcome.giftWrapEvent, relaySync.relayUrls().toSet())
                                    if (accepted) repository.markWelcomePublished(welcome.giftWrapEvent.id)
                                    accepted
                                }
                                inviteStatus = if (delivered) "Welcome sent to ${targetPubKey.take(8)}…; they can join after syncing." else "Invitation saved. No relay confirmed it; use Sync now to retry."
                                showInvite = false
                            } catch (failure: Exception) {
                                inviteStatus = failure.message ?: "Could not send the invitation. Check the connection and retry."
                            } finally {
                                inviting = false
                            }
                        }
                    },
                ) { Text(if (inviting) "Inviting…" else "Send invite") }
            },
            dismissButton = { TextButton(enabled = !inviting, onClick = { showInvite = false }) { Text("Cancel") } },
        )
    }
}
