package com.just_sri.suvidha.expenses.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.outlined.Tag
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
    val visible: List<SharedGroupEntity> = data.groups.filter { (model.showLeftGroups || !data.hasLeft(it.id)) && it.name.contains(search.trim(), true) }.let { if (byName) it.sortedBy { it.name.lowercase() } else it.sortedByDescending { it.updatedAtMillis } }
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
                    val id = model.createGroup(name, currencyCodeFor(symbol)); created = id
                    val failures = mutableListOf<String>()
                    chosen.filterValues { it }.keys.forEach { friendId ->
                        val key = data.friendKey(friendId) ?: return@forEach
                        try { model.inviteMember(id, key) } catch (e: kotlinx.coroutines.CancellationException) { throw e }
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
    var amountTab by rememberSaveable(group.id) { mutableStateOf("Net amounts") }
    var menu by remember { mutableStateOf(false) }
    var payment by remember { mutableStateOf<SharedDebtBalance?>(null) }
    var removeDebt by remember { mutableStateOf<SharedDebtBalance?>(null) }
    var selectedMember by remember { mutableStateOf<SharedGroupMemberEntity?>(null) }
    var selectedDebt by remember { mutableStateOf<SharedDebtBalance?>(null) }
    var amount by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val projection = remember(group, data.events, data.members) { data.projection(group) }
    val members = data.members.filter { it.groupId == group.id && (it.isActive || projection.balanceByMemberId.getOrDefault(it.memberId, 0) != 0L) }
    val debts = projection.debtById.values.filter { it.remainingAmountMinor > 0 }
    val ownNetMinor: Long = data.ownId?.let { projection.balanceByMemberId[it] } ?: 0L
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
    if (selectedMember != null) {
        val member: SharedGroupMemberEntity = selectedMember!!
        BackHandler { selectedMember = null }
        MemberDetailScreen(
            member = member,
            group = group,
            data = data,
            balance = projection.balanceByMemberId.getOrDefault(member.memberId, 0L),
            debts = debts,
            back = { selectedMember = null },
            onSettleDebt = { d: SharedDebtBalance ->
                selectedMember = null
                payment = d
                amount = SharedSplitMoney.format(d.remainingAmountMinor, group.currencyCode)
                error = null
            }
        )
    } else if (selectedDebt != null) {
        val debt: SharedDebtBalance = selectedDebt!!
        BackHandler { selectedDebt = null }
        DebtDetailScreen(
            debt = debt,
            group = group,
            data = data,
            back = { selectedDebt = null },
            onRecordPayment = {
                selectedDebt = null
                payment = debt
                amount = SharedSplitMoney.format(debt.remainingAmountMinor, group.currencyCode)
                error = null
            },
            onDeleteDebt = {
                selectedDebt = null
                removeDebt = debt
            }
        )
    } else {
        if (tab == "Amount") BackHandler { tab = "Group chat" }
        PageColumn {
            if (tab == "Amount") {
                PageHeader("Amount", { tab = "Group chat" })
            } else PageHeader(group.name, back, leading = {
                IconButton({ navigate("members/${group.id}") }, Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.Groups, "Group settings", tint = Green, modifier = Modifier.size(22.dp))
                }
            }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        IconButton({ menu = true }, Modifier.size(36.dp)) { Icon(Icons.Outlined.MoreVert, "Group options") }
                        DropdownMenu(menu, { menu = false }) {
                            DropdownMenuItem({ Text("Group settings") }, { menu = false; navigate("members/${group.id}") })
                            DropdownMenuItem({ Text("Sync now") }, { menu = false; model.sync() }, enabled = !model.busy)
                        }
                    }
                }
            }
            if (tab != "Amount") {
                GroupBalanceStrip(netAmountMinor = ownNetMinor, currencyCode = group.currencyCode, onClick = { tab = "Amount" })
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
                Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)) {
                    Segments(listOf("Net amounts", "Outstanding payments"), amountTab, { amountTab = it })
                }
                if (amountTab == "Net amounts") {
                    PageList {
                        item { SectionHeading("Net amounts", "${members.size} people") }
                        item { Text("+ To receive    ·    − To pay", color = Muted, fontSize = 12.sp) }
                        if (members.isEmpty()) {
                            item { EmptyState("No members", "Add members to this group to track net balances.") }
                        } else {
                            item {
                                Panel {
                                    members.forEachIndexed { index: Int, member: SharedGroupMemberEntity ->
                                        val balance: Long = projection.balanceByMemberId.getOrDefault(member.memberId, 0L)
                                        LedgerRow(
                                            title = data.label(member.memberId),
                                            subtitle = if (!member.isActive) "Former member" else null,
                                            amount = "${if (balance > 0) "+" else if (balance < 0) "−" else ""}${minorMoney(abs(balance), group.currencyCode)}",
                                            amountColor = if (balance < 0) Danger else if (balance > 0) Green else Muted,
                                            divider = index < members.lastIndex,
                                            onClick = { selectedMember = member }
                                        )
                                    }
                                }
                            }
                        }
                        if (members.count { it.isActive } <= 1) {
                            item { SecondaryButton("Add group members", { navigate("members/${group.id}") }) }
                        }
                    }
                } else {
                    PageList {
                        item { SectionHeading("Outstanding payments", "${debts.size}") }
                        if (debts.isEmpty()) {
                            item { EmptyState("All caught up", "There are no outstanding payments in this group.") }
                        } else {
                            item {
                                Panel {
                                    debts.forEachIndexed { index: Int, debt: SharedDebtBalance ->
                                        val isDebtor: Boolean = debt.debtorMemberId == data.ownId
                                        val isCreditor: Boolean = debt.creditorMemberId == data.ownId
                                        LedgerRow(
                                            title = "${data.label(debt.debtorMemberId)} → ${data.label(debt.creditorMemberId)}",
                                            subtitle = if (isCreditor) "Tap to record or view details" else if (isDebtor) "You owe this payment" else "Awaiting payment confirmation",
                                            amount = minorMoney(debt.remainingAmountMinor, group.currencyCode),
                                            amountColor = if (isDebtor) Danger else if (isCreditor) Green else Ink,
                                            divider = index < debts.lastIndex,
                                            onClick = { selectedDebt = debt }
                                        )
                                    }
                                }
                            }
                        }
                        if (members.count { it.isActive } <= 1) {
                            item { SecondaryButton("Add group members", { navigate("members/${group.id}") }) }
                        }
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
                    model.recordPayment(group.id, debt.debtUnitId, value)
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
        { model.perform("Debt removed", { removeDebt = null }) { model.deleteDebt(group.id, debt.debtUnitId) } }, model.busy) }
}

/** Slim full-width strip under the group header showing the current user's net balance; tapping opens Amount. */
@Composable
internal fun GroupBalanceStrip(netAmountMinor: Long, currencyCode: String, onClick: () -> Unit, modifier: Modifier = Modifier): Unit {
    Surface(onClick = onClick, modifier = modifier.fillMaxWidth().height(44.dp), color = CardSurface) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GroupBalanceLabel(netAmountMinor, currencyCode, Modifier.weight(1f, fill = false))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("View balances", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, Modifier.size(16.dp), tint = Muted)
                }
            }
            HorizontalDivider(color = BorderLight)
        }
    }
}

@Composable
private fun GroupBalanceLabel(netAmountMinor: Long, currencyCode: String, modifier: Modifier = Modifier): Unit {
    if (netAmountMinor == 0L) {
        Text("All settled up", modifier, color = Muted, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        return
    }
    val isOwed: Boolean = netAmountMinor > 0
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(if (isOwed) "You're owed " else "You owe ", color = Muted, fontSize = 13.sp, maxLines = 1)
        Text(minorMoney(abs(netAmountMinor), currencyCode), color = if (isOwed) Green else Danger, fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun MemberDetailScreen(
    member: SharedGroupMemberEntity,
    group: SharedGroupEntity,
    data: AppData,
    balance: Long,
    debts: List<SharedDebtBalance>,
    back: () -> Unit,
    onSettleDebt: (SharedDebtBalance) -> Unit
): Unit {
    val name: String = data.label(member.memberId)
    val memberDebts: List<SharedDebtBalance> = debts.filter { it.debtorMemberId == member.memberId || it.creditorMemberId == member.memberId }
    PageColumn {
        PageHeader(name, back)
        PageList {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Box(
                        modifier = Modifier
                            .background(Ui.Mint, RoundedCornerShape(999.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (!member.isActive) "FORMER MEMBER" else if (member.role == "owner") "GROUP ADMIN" else "MEMBER",
                            color = Green,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = name,
                        color = Ink,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 32.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${if (balance > 0) "+" else if (balance < 0) "−" else ""} ${minorMoney(abs(balance), group.currencyCode)}",
                        color = if (balance < 0) Danger else if (balance > 0) Green else Muted,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(fontFeatureSettings = "tnum")
                    )
                }
            }
            item {
                Text(
                    text = "LEDGER INFORMATION",
                    color = Muted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                )
            }
            item {
                Panel {
                    LedgerDetailRow(
                        label = "Group",
                        value = group.name,
                        icon = Icons.Outlined.DateRange,
                        showDivider = true
                    )
                    LedgerDetailRow(
                        label = "Membership",
                        value = if (!member.isActive) "Former member" else if (member.role == "owner") "Group owner" else "Active member",
                        icon = Icons.Outlined.Person,
                        showDivider = true
                    )
                    LedgerDetailRow(
                        label = "Net Balance",
                        value = if (balance > 0) "To receive ${minorMoney(balance, group.currencyCode)}" else if (balance < 0) "Owes ${minorMoney(abs(balance), group.currencyCode)}" else "Fully settled",
                        icon = Icons.Outlined.AccountBalanceWallet,
                        isValueGreen = balance > 0,
                        showDivider = true
                    )
                    LedgerDetailRow(
                        label = "Status",
                        value = if (balance == 0L) "Settled" else "Active balance",
                        icon = Icons.Outlined.CheckCircle,
                        isBadge = true,
                        showDivider = true
                    )
                    LedgerDetailRow(
                        label = "Member ID",
                        value = member.memberId,
                        icon = Icons.Outlined.Tag,
                        isMono = true,
                        showDivider = false
                    )
                }
            }
            if (memberDebts.isNotEmpty()) {
                item {
                    Text(
                        text = "OUTSTANDING PAYMENTS (${memberDebts.size})",
                        color = Muted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                    )
                }
                item {
                    Panel {
                        memberDebts.forEachIndexed { i: Int, d: SharedDebtBalance ->
                            val isCreditor: Boolean = d.creditorMemberId == data.ownId
                            val isDebtor: Boolean = d.debtorMemberId == data.ownId
                            LedgerRow(
                                title = "${data.label(d.debtorMemberId)} → ${data.label(d.creditorMemberId)}",
                                subtitle = if (isCreditor) "Tap to record payment" else if (isDebtor) "You owe this payment" else "Pending between members",
                                amount = minorMoney(d.remainingAmountMinor, group.currencyCode),
                                amountColor = if (isDebtor) Danger else if (isCreditor) Green else Ink,
                                divider = i < memberDebts.lastIndex,
                                onClick = if (isCreditor) { { onSettleDebt(d) } } else null
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun DebtDetailScreen(
    debt: SharedDebtBalance,
    group: SharedGroupEntity,
    data: AppData,
    back: () -> Unit,
    onRecordPayment: () -> Unit,
    onDeleteDebt: () -> Unit
): Unit {
    val isCreditor: Boolean = debt.creditorMemberId == data.ownId
    val isDebtor: Boolean = debt.debtorMemberId == data.ownId
    PageColumn {
        PageHeader("Payment details", back)
        PageList {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    val roleColor: Color = if (isDebtor) Danger else if (isCreditor) Green else Muted
                    Box(
                        modifier = Modifier
                            .background(roleColor.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isCreditor) "RECEIVABLE PAYMENT" else if (isDebtor) "PAYABLE DEBT" else "OUTSTANDING PAYMENT",
                            color = roleColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "${data.label(debt.debtorMemberId)} → ${data.label(debt.creditorMemberId)}",
                        color = Ink,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 30.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = minorMoney(debt.remainingAmountMinor, group.currencyCode),
                        color = roleColor,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(fontFeatureSettings = "tnum")
                    )
                }
            }
            item {
                Text(
                    text = "LEDGER INFORMATION",
                    color = Muted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                )
            }
            item {
                Panel {
                    LedgerDetailRow(
                        label = "Payer (Owes)",
                        value = data.label(debt.debtorMemberId),
                        icon = Icons.Outlined.Person,
                        showDivider = true
                    )
                    LedgerDetailRow(
                        label = "Payee (To receive)",
                        value = data.label(debt.creditorMemberId),
                        icon = Icons.Outlined.Person,
                        showDivider = true
                    )
                    if (debt.originalAmountMinor != debt.remainingAmountMinor) {
                        LedgerDetailRow(
                            label = "Original Amount",
                            value = minorMoney(debt.originalAmountMinor, group.currencyCode),
                            icon = Icons.Outlined.AccountBalanceWallet,
                            showDivider = true
                        )
                        LedgerDetailRow(
                            label = "Already Settled",
                            value = minorMoney(debt.originalAmountMinor - debt.remainingAmountMinor, group.currencyCode),
                            icon = Icons.Outlined.CheckCircle,
                            isValueGreen = true,
                            showDivider = true
                        )
                    }
                    LedgerDetailRow(
                        label = "Status",
                        value = if (isCreditor) "Awaiting your confirmation" else if (isDebtor) "You owe this payment" else "Pending between members",
                        icon = Icons.Outlined.CheckCircle,
                        isBadge = true,
                        showDivider = true
                    )
                    LedgerDetailRow(
                        label = "Debt ID",
                        value = debt.debtUnitId.take(8),
                        icon = Icons.Outlined.Tag,
                        isMono = true,
                        showDivider = false
                    )
                }
            }
            if (isCreditor) {
                item {
                    Spacer(Modifier.height(8.dp))
                    PrimaryButton(
                        text = "Record payment",
                        onClick = onRecordPayment
                    )
                }
                item {
                    DeleteDebtButton(onClick = onDeleteDebt)
                }
            } else if (isDebtor) {
                item {
                    Panel {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("How to settle", fontWeight = FontWeight.SemiBold, color = Ink, fontSize = 14.sp)
                            Text(
                                "Pay ${data.label(debt.creditorMemberId)} via UPI or cash. They record it in Suvidha.",
                                color = Muted,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun DeleteDebtButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
): Unit {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(46.dp),
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFFFF8F7),
        border = BorderStroke(1.dp, Color(0xFFFFDAD6))
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Delete, null, tint = Danger, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Delete debt", color = Danger, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

private enum class GroupAction { Rename, OwnerLeave, Transfer, Close, Leave }

@Composable
internal fun MembersScreen(group: SharedGroupEntity, data: AppData, model: AppViewModel, navigate: (String) -> Unit, back: () -> Unit) {
    val members = data.members.filter { it.groupId == group.id && it.isActive }
    val owner = members.any { it.memberId == data.ownId && it.role == "owner" }
    val otherMembers: List<SharedGroupMemberEntity> = members.filter { it.memberId != data.ownId }
    var groupAction by remember { mutableStateOf<GroupAction?>(null) }
    var newGroupName by remember(group.id) { mutableStateOf(group.name) }
    var newOwnerId by remember { mutableStateOf<String?>(null) }
    var adding by remember { mutableStateOf(false) }
    var remove by remember { mutableStateOf<SharedGroupMemberEntity?>(null) }
    var linking by remember { mutableStateOf<SharedGroupMemberEntity?>(null) }
    var newName by rememberSaveable { mutableStateOf("") }
    var search by rememberSaveable { mutableStateOf("") }
    var availability: Map<String, Boolean> by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var checking: Set<String> by remember { mutableStateOf<Set<String>>(emptySet()) }
    val candidates = data.friends.filter { friend -> data.friendKey(friend.id)?.let { key -> members.none { it.memberId == key } } == true && friend.name.contains(search.trim(), true) }
    LaunchedEffect(adding, candidates) {
        if (adding) {
            candidates.forEach { friend ->
                val key: String? = data.friendKey(friend.id)
                if (key != null && key !in availability) {
                    checking = checking + key
                    val isAvailable: Boolean = model.canInvite(group.id, key)
                    availability = availability + (key to isAvailable)
                    checking = checking - key
                }
            }
        }
    }
    PageColumn {
        PageHeader(if (adding) "Add members" else "Group settings", { if (adding) adding = false else back() })
        PageList {
            item { Text(group.name, color = Muted, fontSize = 13.sp) }
            if (adding) {
                item { SearchField(search, { search = it }, "Search friends") }
                item { TextButton({ navigate("new-friend") }) { Text("+ Add a new friend") } }
                if (candidates.isEmpty()) item { EmptyState("No available friends", "Add a friend with a public ID to invite them to this group.") }
                itemsIndexed(candidates, key = { _, friend -> friend.id }) { _, friend ->
                    val key: String? = data.friendKey(friend.id)
                    val isChecking: Boolean = key != null && key in checking
                    val isAvailable: Boolean = key != null && availability[key] == true
                    val subtitle: String = when {
                        isChecking -> "Checking invite key…"
                        availability[key] == false -> "Invitations unavailable"
                        else -> "Invite to ${group.name}"
                    }
                    val canInvite: Boolean = !model.busy && isAvailable && !isChecking
                    val buttonLabel: String = when {
                        isChecking -> "Checking"
                        availability[key] == false -> "Unavailable"
                        else -> "Add"
                    }
                    Panel { LedgerRow(friend.name, subtitle, trailing = { TextButton({
                        key?.let { validKey -> model.perform(onDone = { adding = false }) { model.message = model.inviteMember(group.id, validKey) } }
                    }, enabled = canInvite) { Text(buttonLabel) } }) }
                }
            } else {
                item { SectionHeading("Group", null) }
                item { Panel {
                    if (owner) LedgerRow("Rename group", "Change the name everyone sees", icon = Icons.Outlined.Edit,
                        onClick = { newGroupName = group.name; groupAction = GroupAction.Rename }, divider = true)
                    if (owner && otherMembers.isNotEmpty()) LedgerRow("Transfer ownership", "Hand the group to another member", icon = Icons.Outlined.SwapHoriz,
                        onClick = { newOwnerId = null; groupAction = GroupAction.Transfer }, divider = true)
                    if (owner) LedgerRow("Close group", "Remove everyone and close the group", icon = Icons.Outlined.Lock,
                        onClick = { groupAction = GroupAction.Close }, divider = true)
                    if (!data.hasLeft(group.id)) LedgerRow("Leave group", "You will be removed from this group", icon = Icons.AutoMirrored.Outlined.ExitToApp,
                        onClick = { groupAction = if (owner && otherMembers.isNotEmpty()) GroupAction.OwnerLeave else GroupAction.Leave })
                } }
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
    if (groupAction == GroupAction.Rename) ConfirmDialog("Rename group", "All members will see the new name.", "Save", { groupAction = null }, {
        if (newGroupName.isNotBlank()) model.renameGroup(group.id, newGroupName) { groupAction = null }
    }, model.busy, content = { InputField(newGroupName, { newGroupName = it.take(80) }, "Group name") })
    if (groupAction == GroupAction.OwnerLeave) ConfirmDialog("Transfer ownership or close the group",
        "As the owner you can't leave while other members remain. Hand the group to another member, or close it for everyone.", "Transfer ownership", { groupAction = null },
        { newOwnerId = null; groupAction = GroupAction.Transfer }, model.busy,
        content = { TextButton({ groupAction = GroupAction.Close }) { Text("Close group", color = Danger) } })
    if (groupAction == GroupAction.Transfer) ConfirmDialog("Transfer ownership", "The new owner can rename the group and remove members. You will become a regular member.", "Transfer", { groupAction = null },
        { newOwnerId?.let { id -> model.perform("Ownership transferred", { groupAction = null }) { model.transferOwnership(group.id, id) } } },
        model.busy, content = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                otherMembers.forEach { member ->
                    Row(Modifier.fillMaxWidth().clickable { newOwnerId = member.memberId }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(newOwnerId == member.memberId, { newOwnerId = member.memberId })
                        Text(data.label(member.memberId))
                    }
                }
            }
        })
    if (groupAction == GroupAction.Close) {
        var closeBlocker: String? by remember { mutableStateOf(null) }
        LaunchedEffect(group.id, data.events) { closeBlocker = model.closeGroupBlocker(group.id) }
        ConfirmDialog("Close group?",
            closeBlocker ?: "All members will be removed and the group will close. Earlier history stays readable for former members.",
            "Close group", { groupAction = null },
            { if (closeBlocker == null) model.perform("Group closed", { groupAction = null; back() }) { model.closeGroup(group.id) } },
            model.busy)
    }
    if (groupAction == GroupAction.Leave) ConfirmDialog("Leave group?",
        "You will be removed from this group. You must settle your balance first.", "Leave group", { groupAction = null },
        { groupAction = null; model.perform("Left group", back) { model.leaveGroup(group.id) } }, model.busy)
    remove?.let { member -> ConfirmDialog("Remove member?", "${data.label(member.memberId)} will be removed from ${group.name}. Their balance must be settled first.", "Remove", { remove = null },
        { model.perform("Member removed", { remove = null }) { model.removeMember(group.id, member.memberId) } }, model.busy) }
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


