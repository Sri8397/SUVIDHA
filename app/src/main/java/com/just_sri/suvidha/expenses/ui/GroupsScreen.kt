package com.just_sri.suvidha.expenses.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.just_sri.suvidha.expenses.*
import com.just_sri.suvidha.expenses.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GroupsScreen(data: AppData, model: AppViewModel, navigate: (String) -> Unit, back: () -> Unit) {
    var search: String by rememberSaveable { mutableStateOf("") }
    var byName: Boolean by rememberSaveable { mutableStateOf(false) }
    val visible: List<SharedGroupEntity> = data.groups.filter { it.name.contains(search.trim(), true) }.let { if (byName) it.sortedBy { it.name.lowercase() } else it.sortedByDescending { it.updatedAtMillis } }
    val ptrState = rememberPullToRefreshState()
    PageColumn {
        PageHeader("Groups", back) { TextButton({ navigate("new-group") }, enabled = !model.busy) { Icon(Icons.Outlined.Add, null, Modifier.size(18.dp)); Text("New group", fontSize = 12.sp) } }
        PullToRefreshBox(
            isRefreshing = model.busy,
            onRefresh = model::sync,
            modifier = Modifier.fillMaxSize(),
            state = ptrState,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = ptrState,
                    isRefreshing = model.busy,
                    modifier = Modifier.align(Alignment.TopCenter),
                    containerColor = CardSurface,
                    color = Green
                )
            }
        ) {
            PageList {
                item { SearchField(search, { search = it }, "Search groups") }
                item { SectionHeading("Your groups", "${visible.size}") { TextButton({ byName = !byName }) { Text(if (byName) "Name A–Z ▾" else "Recently added ▾", fontSize = 11.sp) } } }
                if (visible.isEmpty()) item { EmptyState(if (data.groups.isEmpty()) "Share expenses together" else "No groups found", if (data.groups.isEmpty()) "Create a group or sync an invitation from a friend." else "Try a different name.") }
                item {
                    if (visible.isNotEmpty()) Panel { visible.forEachIndexed { i, group -> LedgerRow(group.name,
                        "${data.members.count { it.groupId == group.id && it.isActive }} members · ${group.currencyCode}", icon = Icons.Outlined.DateRange,
                        onClick = { navigate("group/${group.id}") }, divider = i < visible.lastIndex) } }
                }
            }
        }
    }
}

@Composable
internal fun NewGroupScreen(data: AppData, model: AppViewModel, navigate: (String) -> Unit, back: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var symbol by rememberSaveable { mutableStateOf(model.currency) }
    var currencyMenu by remember { mutableStateOf(false) }
    val chosen = remember { mutableStateMapOf<String, Boolean>() }
    var search by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val friends = data.friends.filter { it.name.contains(search.trim(), true) }
    PageColumn {
        PageHeader("New group", back)
        PageList {
            item { InputField(name, { name = it.take(80); error = null }, "e.g. Trip to Goa", "Group name *") }
            item {
                Box {
                    SecondaryButton("Currency · ${currencyCodeFor(symbol)} ($symbol)", { currencyMenu = true })
                    DropdownMenu(currencyMenu, { currencyMenu = false }) { supportedCurrencies.forEach { (value, label) -> DropdownMenuItem({ Text(label) }, { symbol = value; currencyMenu = false }) } }
                }
            }
            item { SectionHeading("Add friends", "${chosen.values.count { it }} selected") }
            item { Text("You’ll be added automatically. Selected friends receive an invitation.", color = Muted, fontSize = 13.sp) }
            item { SearchField(search, { search = it }, "Search friends") }
            item { TextButton({ navigate("new-friend") }) { Text("+ Add a new friend") } }
            if (friends.isEmpty()) item { EmptyState("No friends to show", "Add a friend by public ID, or create the group and invite someone later.") }
            itemsIndexed(friends, key = { _, friend -> friend.id }) { _, friend ->
                Panel { LedgerRow(friend.name, if (data.friendKey(friend.id) == null) "Add a public ID to invite this friend" else "Ready to invite",
                    trailing = { Checkbox(chosen[friend.id] == true, { chosen[friend.id] = it }, enabled = data.friendKey(friend.id) != null && !model.busy) }) }
            }
            item { ErrorText(error) }
            item { PrimaryButton(if (model.busy) "Creating…" else "Create group", {
                if (name.isBlank()) { error = "Enter a group name."; return@PrimaryButton }
                var created: String? = null
                model.perform(onDone = { created?.let { navigate("group/$it") } }) {
                    val id = model.peers.createGroup(name, currencyCodeFor(symbol)); created = id
                    val failures = mutableListOf<String>()
                    chosen.filterValues { it }.keys.forEach { friendId ->
                        val key = data.friendKey(friendId) ?: return@forEach
                        try { model.peers.invite(id, key) } catch (e: kotlinx.coroutines.CancellationException) { throw e }
                        catch (_: Exception) { failures += data.friends.find { it.id == friendId }?.name.orEmpty() }
                    }
                    model.message = if (failures.isEmpty()) "Group created" else "Group created. Invitations need a retry for ${failures.joinToString()}. Open Members to invite again."
                }
            }, enabled = !model.busy) }
        }
    }
}

@Composable
internal fun GroupScreen(group: SharedGroupEntity, data: AppData, model: AppViewModel, navigate: (String) -> Unit, back: () -> Unit) {
    var tab by rememberSaveable(group.id) { mutableStateOf("Group chat") }
    var menu by remember { mutableStateOf(false) }
    var payment by remember { mutableStateOf<SharedDebtBalance?>(null) }
    var removeDebt by remember { mutableStateOf<SharedDebtBalance?>(null) }
    var amount by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val projection = remember(group, data.events, data.members) { data.projection(group) }
    val members = data.members.filter { it.groupId == group.id && (it.isActive || projection.balanceByMemberId.getOrDefault(it.memberId, 0) != 0L) }
    val debts = projection.debtById.values.filter { it.remainingAmountMinor > 0 }

    // Await each pass and stop polling when the screen leaves the foreground.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(group.id, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                delay(4000)
                if (!model.busy) model.backgroundSync().join()
            }
        }
    }

    PageColumn {
        PageHeader(group.name, back) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton({ navigate("members/${group.id}") }, Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.Person, "Members", tint = Green, modifier = Modifier.size(20.dp))
                }
                Box {
                    IconButton({ menu = true }, Modifier.size(36.dp)) { Icon(Icons.Outlined.MoreVert, "Group options") }
                    DropdownMenu(menu, { menu = false }) {
                        DropdownMenuItem({ Text("Members") }, { menu = false; navigate("members/${group.id}") })
                        DropdownMenuItem({ Text("Sync now") }, { menu = false; model.sync() }, enabled = !model.busy)
                    }
                }
            }
        }

        Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Segments(listOf("Group chat", "Amount"), tab, { tab = it })
        }

        if (projection.pendingEventIds.isNotEmpty()) {
            Text(
                "Some activity is waiting for earlier updates. Sync to complete the history.",
                color = Muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
            )
        }
        if (projection.rejectedEvents.isNotEmpty()) {
            Box(Modifier.padding(horizontal = 20.dp, vertical = 2.dp)) {
                ErrorText("${projection.rejectedEvents.size} conflicting updates could not be applied.")
            }
        }

        if (tab == "Amount") {
            PageList {
                item { SectionHeading("Net amounts", "${members.size} people") }
                item { Text("+ To receive    ·    − To pay", color = Muted, fontSize = 12.sp) }
                itemsIndexed(members, key = { _, member -> member.memberId }) { _, member ->
                    val balance = projection.balanceByMemberId.getOrDefault(member.memberId, 0L)
                    Panel {
                        LedgerRow(
                            data.label(member.memberId),
                            if (!member.isActive) "Former member" else null,
                            "${if (balance > 0) "+" else if (balance < 0) "−" else ""}${minorMoney(abs(balance), group.currencyCode)}",
                            amountColor = if (balance < 0) Danger else if (balance > 0) Green else Muted
                        )
                    }
                }
                item { SectionHeading("Outstanding payments", "${debts.size}") }
                if (debts.isEmpty()) item { EmptyState("All caught up", "There are no outstanding payments in this group.") }
                itemsIndexed(debts, key = { _, debt -> debt.debtUnitId }) { _, debt ->
                    Panel {
                        LedgerRow(
                            "${data.label(debt.debtorMemberId)} → ${data.label(debt.creditorMemberId)}",
                            if (debt.creditorMemberId == data.ownId) "Tap to record a payment" else "Awaiting payment confirmation",
                            minorMoney(debt.remainingAmountMinor, group.currencyCode),
                            amountColor = if (debt.debtorMemberId == data.ownId) Danger else Green,
                            onClick = if (debt.creditorMemberId == data.ownId) {{
                                payment = debt
                                amount = SharedSplitMoney.format(debt.remainingAmountMinor, group.currencyCode)
                                error = null
                            }} else null
                        )
                    }
                }
                item {
                    PrimaryButton(
                        "+ Add split expense",
                        { navigate("group-split/${group.id}") },
                        enabled = members.count { it.isActive } > 1 && data.ownId != null && !model.busy
                    )
                }
                if (members.count { it.isActive } <= 1) {
                    item { SecondaryButton("Add group members", { navigate("members/${group.id}") }) }
                }
            }
        } else {
            ChatScreen(
                group = group,
                data = data,
                model = model,
                navigate = navigate,
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        }
    }

    payment?.let { debt ->
        AlertDialog(onDismissRequest = { if (!model.busy) payment = null }, title = { Text("Record a payment") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Record money already paid. This updates the group balance.", color = Muted)
                InputField(amount, { amount = it.take(20); error = null }, "Amount", "Paid (${currencySymbol(group.currencyCode)})", keyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                ErrorText(error)
            } }, confirmButton = { TextButton({
                val value = SharedSplitMoney.parseMinor(amount, group.currencyCode)
                if (value == null || value !in 1..debt.remainingAmountMinor) error = "Enter an amount up to ${minorMoney(debt.remainingAmountMinor, group.currencyCode)}."
                else model.perform("Payment recorded", { payment = null }) {
                    model.peers.settle(group.id, debt.debtUnitId, value)
                    model.notifications.notifySettlement(
                        counterpartName = group.name,
                        amountFormatted = minorMoney(value, group.currencyCode),
                        isGroup = true
                    )
                }
            }, enabled = !model.busy) { Text("Record payment") } }, dismissButton = {
                Column { TextButton({ payment = null }, enabled = !model.busy) { Text("Cancel") }; TextButton({ payment = null; removeDebt = debt }, enabled = !model.busy) { Text("Delete debt", color = Danger) } }
            })
    }
    removeDebt?.let { debt -> ConfirmDialog("Delete outstanding debt?", "The remaining debt will be removed for all members. Earlier history stays available.", "Delete", { removeDebt = null },
        { model.perform("Debt removed", { removeDebt = null }) { model.peers.removeDebt(group.id, debt.debtUnitId) } }, model.busy) }
}

@Composable
internal fun MembersScreen(group: SharedGroupEntity, data: AppData, model: AppViewModel, navigate: (String) -> Unit, back: () -> Unit) {
    val members = data.members.filter { it.groupId == group.id && it.isActive }
    val owner = members.any { it.memberId == data.ownId && it.role == "owner" }
    var adding by remember { mutableStateOf(false) }
    var remove by remember { mutableStateOf<SharedGroupMemberEntity?>(null) }
    var linking by remember { mutableStateOf<SharedGroupMemberEntity?>(null) }
    var newName by rememberSaveable { mutableStateOf("") }
    var search by rememberSaveable { mutableStateOf("") }
    val candidates = data.friends.filter { friend -> data.friendKey(friend.id)?.let { key -> members.none { it.memberId == key } } == true && friend.name.contains(search.trim(), true) }
    PageColumn {
        PageHeader(if (adding) "Add members" else "Group members", { if (adding) adding = false else back() })
        PageList {
            item { Text(group.name, color = Muted, fontSize = 13.sp) }
            if (adding) {
                item { SearchField(search, { search = it }, "Search friends") }
                item { TextButton({ navigate("new-friend") }) { Text("+ Add a new friend") } }
                if (candidates.isEmpty()) item { EmptyState("No available friends", "Add a friend with a public ID to invite them to this group.") }
                itemsIndexed(candidates, key = { _, friend -> friend.id }) { _, friend ->
                    Panel { LedgerRow(friend.name, "Invite to ${group.name}", trailing = { TextButton({
                        data.friendKey(friend.id)?.let { key -> model.perform(onDone = { adding = false }) { model.message = model.peers.invite(group.id, key) } }
                    }, enabled = !model.busy) { Text("Add") } }) }
                }
            } else {
                item { SectionHeading("Members", "${members.size} people") }
                item { Panel { members.forEachIndexed { i, member ->
                    LedgerRow(data.label(member.memberId), if (member.role == "owner") "Group admin" else "Member", divider = i < members.lastIndex,
                        onClick = if (member.memberId != data.ownId) {{ linking = member; newName = data.label(member.memberId).takeUnless { it.contains('…') }.orEmpty() }} else null,
                        trailing = if (owner && member.memberId != data.ownId) {{
                            IconButton(
                                onClick = { remove = member },
                                enabled = !model.busy,
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(color = Color(0xFFFFF5F5), shape = CircleShape)
                                    .border(1.dp, Color(0xFFFED7D7), CircleShape)
                            ) {
                                Icon(
                                    Icons.Outlined.Close,
                                    contentDescription = "Remove ${data.label(member.memberId)}",
                                    tint = Color(0xFFC53030),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }} else null)
                } } }
                if (owner) item { PrimaryButton("+ Add members", { adding = true }, enabled = !model.busy) }
            }
        }
    }
    remove?.let { member -> ConfirmDialog("Remove member?", "${data.label(member.memberId)} will be removed from ${group.name}. Their balance must be settled first.", "Remove", { remove = null },
        { model.perform("Member removed", { remove = null }) { model.peers.removeMember(group.id, member.memberId) } }, model.busy) }
    linking?.let { member ->
        AlertDialog(onDismissRequest = { if (!model.busy) linking = null }, title = { Text("Name this member") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Save a name for this public ID on your device.", color = Muted)
                InputField(newName, { newName = it.take(60) }, "Friend's name")
                if (data.friends.isNotEmpty()) { Text("Or link an existing friend", fontSize = 13.sp, color = Muted)
                    Column(Modifier.heightIn(max = 180.dp).verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                        data.friends.forEach { friend -> TextButton({ model.perform("Friend linked", { linking = null }) { model.groups.linkMemberToFriend(group.id, member.memberId, friend.id) } }, enabled = !model.busy) { Text(friend.name) } }
                    }
                }
            }
        }, confirmButton = { TextButton({ model.perform("Friend saved", { linking = null }) {
            val current = data.links.find { it.memberId == member.memberId }?.let { link -> data.friends.find { it.id == link.friendId } }
            if (current != null) model.repository.saveFriend(current.copy(name = newName.trim())) else model.groups.createFriendForMember(group.id, member.memberId, newName)
        } }, enabled = newName.isNotBlank() && !model.busy) { Text("Save name") } }, dismissButton = { TextButton({ linking = null }, enabled = !model.busy) { Text("Cancel") } })
    }
}


