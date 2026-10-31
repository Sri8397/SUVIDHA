package com.just_sri.suvidha.expenses.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.just_sri.suvidha.expenses.*
import com.just_sri.suvidha.expenses.R
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FriendsScreen(data: AppData, model: AppViewModel, navigate: (String) -> Unit, back: () -> Unit) {
    var search: String by rememberSaveable { mutableStateOf("") }
    var sortName: Boolean by rememberSaveable { mutableStateOf(false) }
    var code: String by rememberSaveable(model.currency) { mutableStateOf(currencyCodeFor(model.currency)) }
    val codes: List<String> = (listOf(currencyCodeFor(model.currency)) + data.groups.map { it.currencyCode }).distinct()
    val balances: Map<String, Double> = data.friends.associate { it.id to data.balance(it.id, code, currencyCodeFor(model.currency)) }
    val visible: List<Friend> = data.friends.filter { it.name.contains(search.trim(), true) }.let { list -> if (sortName) list.sortedBy { it.name.lowercase() } else list.sortedByDescending { abs(balances[it.id] ?: 0.0) } }
    val ptrState = rememberPullToRefreshState()
    PageColumn {
        PageHeader("Friends") { AddHeaderButton("Add friend") { navigate("new-friend") } }
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
                if (codes.size > 1) item { FilterChips(codes, code, { code = it }) }
                item {
                    Panel {
                        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            BalanceFigure("You're owed", "${currencySymbol(code)} ${money(balances.values.filter { it > 0 }.sum())}", Modifier.weight(1f), Green)
                            VerticalDivider(Modifier.height(48.dp), color = BorderLight)
                            BalanceFigure("You owe", "${currencySymbol(code)} ${money(abs(balances.values.filter { it < 0 }.sum()))}", Modifier.weight(1f), Danger)
                        }
                    }
                }
                item { SearchField(search, { search = it }, "Search friends") }
                item { SectionHeading("Your friends", "${visible.size}") { TextButton({ sortName = !sortName }) { Text(if (sortName) "Name A–Z ▾" else "Highest balance ▾", fontSize = 11.sp) } } }
                if (visible.isEmpty()) item {
                    if (data.friends.isEmpty()) PrimaryButton("Add friend", { navigate("new-friend") })
                    else EmptyState("No friends found", "Try a different name.")
                }
                item {
                    if (visible.isNotEmpty()) Panel {
                        visible.forEachIndexed { i, friend ->
                            val balance = balances[friend.id] ?: 0.0
                            LedgerRow(friend.name, if (balance == 0.0) "All settled" else "View transactions", "${currencySymbol(code)} ${money(abs(balance))}",
                                amountLabel = if (balance > 0) "Owes you" else if (balance < 0) "You owe" else null,
                                amountColor = if (balance < 0) Danger else if (balance > 0) Green else Muted,
                                onClick = { navigate("friend/${friend.id}") }, divider = i < visible.lastIndex)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun BalanceFigure(label: String, amount: String, modifier: Modifier = Modifier, color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified) {
    val finalColor = if (color != androidx.compose.ui.graphics.Color.Unspecified) color else Ink
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = Muted, fontSize = 12.sp)
        Text(
            amount,
            color = finalColor,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
internal fun AddFriendScreen(model: AppViewModel, back: () -> Unit) {
    val context: Context = LocalContext.current
    var name: String by rememberSaveable { mutableStateOf("") }
    var publicId: String by rememberSaveable { mutableStateOf("") }
    var error: String? by remember { mutableStateOf<String?>(null) }
    fun startScan(): Unit {
        launchQrScanner(
            context = context,
            onSuccess = { detectedId: String ->
                publicId = detectedId
                error = null
                if (detectedId.length == 64) {
                    model.message = "Public ID detected"
                }
            },
            onError = { message: String ->
                model.message = message
            }
        )
    }
    PageColumn {
        PageHeader("Add friend", back)
        PageList {
            item {
                Panel {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Text("Public ID is optional. It's used when you split expenses with this friend.", color = Muted, fontSize = 14.sp, lineHeight = 22.sp)
                        Text("64 characters · letters a–f and numbers 0–9", color = Muted, fontSize = 12.sp)
                        InputField(
                            value = publicId,
                            onChange = { publicId = it; error = null },
                            placeholder = "Paste or scan full public ID",
                            label = "Public ID (optional)",
                            labelTrailing = {
                                Surface(
                                    onClick = { startScan() },
                                    shape = CircleShape,
                                    color = Ui.Mint,
                                    border = BorderStroke(1.dp, BorderLight)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_home_qr),
                                            contentDescription = "Scan QR code",
                                            tint = Green,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text("Scan code", color = Green, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        )
                        InputField(name, { name = it.take(60); error = null }, "How you know them", "Name *")
                        ErrorText(error)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SecondaryButton("Cancel", back, Modifier.weight(1f), enabled = !model.busy)
                            PrimaryButton(if (model.busy) "Saving…" else "Add friend", {
                                val key = publicId.trim().lowercase()
                                when {
                                    name.isBlank() -> error = "Enter your friend’s name."
                                    key.isNotEmpty() && !key.matches(Regex("[0-9a-f]{64}")) -> error = "Enter a full 64-character public ID using a–f and 0–9."
                                    else -> model.perform("Friend added", back) { model.repository.addFriend(name, key, model.publicId) }
                                }
                            }, Modifier.weight(1.15f), enabled = !model.busy)
                        }
                    }
                }
            }
        }
    }
}

private fun launchQrScanner(
    context: Context,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
): Unit {
    try {
        val options: GmsBarcodeScannerOptions = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .enableAutoZoom()
            .build()
        GmsBarcodeScanning.getClient(context, options).startScan()
            .addOnSuccessListener { barcode: Barcode ->
                val raw: String = barcode.rawValue ?: barcode.displayValue ?: ""
                val matched: String? = Regex("[0-9a-fA-F]{64}").find(raw)?.value?.lowercase()
                when {
                    matched != null -> onSuccess(matched)
                    raw.isNotBlank() -> onSuccess(raw.trim())
                }
            }
            .addOnFailureListener { e: Exception ->
                onError("Scan failed: ${e.localizedMessage ?: "Could not open scanner"}")
            }
    } catch (e: Exception) {
        onError("Scanner unavailable: ${e.localizedMessage ?: "Could not open scanner"}")
    }
}

private data class FriendEntry(val key: String, val title: String, val groupName: String, val dateText: String, val amount: String,
    val positive: Boolean, val settled: Boolean, val date: Long, val group: FriendGroupTransaction)

@Composable
internal fun FriendScreen(friend: Friend, data: AppData, model: AppViewModel, navigate: (String) -> Unit, back: () -> Unit) {
    val groupEntries = remember(data.groupTransactions, friend.id) { data.groupTransactions[friend.id].orEmpty() }
    val groups = remember(groupEntries) { groupEntries.map { it.groupId to it.groupName }.distinct() }
    var selectedGroupIds by rememberSaveable(groups) { mutableStateOf(groups.map { it.first }.toSet()) }
    var filterMenuOpen by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var remove by remember { mutableStateOf(false) }
    var edit by remember { mutableStateOf(false) }
    var name by rememberSaveable(friend.id) { mutableStateOf(friend.name) }
    val context = LocalContext.current

    val entries = remember(groupEntries, selectedGroupIds) {
        groupEntries.filter { selectedGroupIds.contains(it.groupId) }.map { txn ->
            val isPositive = txn.amount > 0
            val sign = if (isPositive) "+" else "−"
            val formatted = "$sign${currencySymbol(txn.currencyCode)} ${money(abs(txn.amount))}"
            FriendEntry(
                key = "g:${txn.debtUnitId}",
                title = txn.description,
                groupName = txn.groupName,
                dateText = displayDate(txn.occurredAtMillis, model),
                amount = formatted,
                positive = isPositive,
                settled = txn.isSettled,
                date = txn.occurredAtMillis,
                group = txn
            )
        }.sortedByDescending { it.date }
    }

    val isAllSelected = groups.isNotEmpty() && selectedGroupIds.size == groups.size
    val filterLabel = when {
        isAllSelected -> "All groups"
        selectedGroupIds.isEmpty() -> "No groups"
        selectedGroupIds.size == 1 -> groups.find { selectedGroupIds.contains(it.first) }?.second ?: "1 group"
        else -> "${selectedGroupIds.size} groups"
    }

    PageColumn {
        PageHeader(friend.name, back) {
            Box {
                IconButton({ menu = true }) { Icon(Icons.Outlined.MoreVert, "Friend options") }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem({ Text("Edit name") }, { menu = false; edit = true })
                    data.friendKey(friend.id)?.let { key -> DropdownMenuItem({ Text("Copy public ID") }, {
                        menu = false; (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Public ID", key)); model.message = "Public ID copied"
                    }) }
                    DropdownMenuItem({ Text("Remove friend", color = Danger) }, { menu = false; remove = true })
                }
            }
        }
        PageList {
            item {
                Panel {
                    val filteredTxns = groupEntries.filter { selectedGroupIds.contains(it.groupId) }
                    val codes = filteredTxns.map { it.currencyCode }.distinct().ifEmpty { listOf(currencyCodeFor(model.currency)) }
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 15.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        codes.forEach { code ->
                            val net = filteredTxns.filter { !it.isSettled && it.currencyCode == code }.sumOf { it.remainingAmount }
                            val summary = when {
                                selectedGroupIds.isEmpty() -> "Select groups to see a balance"
                                net > 0 -> "${friend.name} owes you"
                                net < 0 -> "You owe ${friend.name}"
                                else -> "All settled"
                            }
                            val scopeText = when {
                                isAllSelected -> ""
                                selectedGroupIds.isNotEmpty() -> "Selected groups"
                                else -> "No groups selected"
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f, fill = false)) {
                                    Text(summary, color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
                                    Text(
                                        "${currencySymbol(code)} ${money(abs(net))}",
                                        color = if (net < 0) Danger else if (net == 0.0) Muted else Green,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = (-0.5).sp
                                    )
                                }
                                if (scopeText.isNotEmpty()) {
                                    Text(scopeText, color = Muted, fontSize = 11.sp, lineHeight = 17.sp)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Transactions", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("· ${entries.size}", color = Muted, fontSize = 12.sp)
                    }
                    if (groups.isNotEmpty()) {
                        Box {
                            Surface(
                                onClick = { filterMenuOpen = true },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, BorderLight),
                                color = CardSurface
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Outlined.FilterList, contentDescription = "Filter", tint = Muted, modifier = Modifier.size(15.dp))
                                    Text(filterLabel, color = Ink, fontSize = 12.sp, maxLines = 1)
                                    Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null, tint = Muted, modifier = Modifier.size(16.dp))
                                }
                            }

                            DropdownMenu(
                                expanded = filterMenuOpen,
                                onDismissRequest = { filterMenuOpen = false },
                                modifier = Modifier.widthIn(min = 220.dp, max = 290.dp).background(CardSurface)
                            ) {
                                Text(
                                    "Show transactions from",
                                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Ink
                                )
                                HorizontalDivider(color = BorderLight)
                                DropdownMenuItem(
                                    text = { Text("All groups", fontWeight = FontWeight.SemiBold) },
                                    leadingIcon = {
                                        Checkbox(
                                            checked = isAllSelected,
                                            onCheckedChange = { checked ->
                                                selectedGroupIds = if (checked) groups.map { it.first }.toSet() else emptySet()
                                            }
                                        )
                                    },
                                    onClick = {
                                        selectedGroupIds = if (isAllSelected) emptySet() else groups.map { it.first }.toSet()
                                    }
                                )
                                HorizontalDivider(color = BorderLight)
                                groups.forEach { (groupId, groupTitle) ->
                                    val isChecked = selectedGroupIds.contains(groupId)
                                    DropdownMenuItem(
                                        text = { Text(groupTitle, fontSize = 13.sp) },
                                        leadingIcon = {
                                            Checkbox(
                                                checked = isChecked,
                                                onCheckedChange = { checked ->
                                                    selectedGroupIds = if (checked) selectedGroupIds + groupId else selectedGroupIds - groupId
                                                }
                                            )
                                        },
                                        onClick = {
                                            selectedGroupIds = if (isChecked) selectedGroupIds - groupId else selectedGroupIds + groupId
                                        }
                                    )
                                }
                                HorizontalDivider(color = BorderLight)
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${selectedGroupIds.size} of ${groups.size} selected", fontSize = 12.sp, color = Muted)
                                    TextButton(onClick = { filterMenuOpen = false }) {
                                        Text("Done", color = Green, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (entries.isEmpty()) {
                item {
                    val emptyTitle = if (selectedGroupIds.isEmpty()) "No groups selected" else "No transactions yet"
                    val emptyDesc = if (selectedGroupIds.isEmpty()) "Choose one or more groups using the filter above." else "There are no transactions in the selected groups."
                    EmptyState(emptyTitle, emptyDesc)
                }
            } else {
                item {
                    Panel {
                        Column {
                            entries.forEachIndexed { index, entry ->
                                if (index > 0) HorizontalDivider(color = BorderLight)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { navigate("group/${entry.group.groupId}") }
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(entry.title, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, lineHeight = 21.sp)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(entry.groupName, color = Muted, fontSize = 11.sp, lineHeight = 18.sp)
                                            Text("·", color = Muted, fontSize = 11.sp)
                                            Text(entry.dateText, color = Muted, fontSize = 11.sp, lineHeight = 18.sp)
                                        }
                                    }
                                    Text(
                                        entry.amount,
                                        color = if (entry.settled) Muted else if (entry.positive) Green else Danger,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        lineHeight = 21.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (remove) ConfirmDialog("Remove friend?", "${friend.name} will be removed. Group history stays available.", "Remove", { remove = false },
        { model.perform("Friend removed", back) { model.repository.deleteFriend(friend.id) } }, model.busy)
    if (edit) ConfirmDialog("Edit friend", "Choose the name you want to see in your ledger.", "Save", { edit = false }, {
        if (name.isNotBlank()) model.perform("Name updated", { edit = false }) { model.repository.saveFriend(friend.copy(name = name.trim(), updatedAt = System.currentTimeMillis())) }
    }, model.busy, content = { InputField(name, { name = it.take(60) }, "Friend's name") })
}
