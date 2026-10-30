package com.just_sri.suvidha.expenses.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
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

@Composable
internal fun FriendsScreen(data: AppData, model: AppViewModel, navigate: (String) -> Unit, back: () -> Unit) {
    var search by rememberSaveable { mutableStateOf("") }
    var sortName by rememberSaveable { mutableStateOf(false) }
    var code by rememberSaveable(model.currency) { mutableStateOf(currencyCodeFor(model.currency)) }
    val codes = (listOf(currencyCodeFor(model.currency)) + data.groups.map { it.currencyCode }).distinct()
    val balances = data.friends.associate { it.id to data.balance(it.id, code, currencyCodeFor(model.currency)) }
    val visible = data.friends.filter { it.name.contains(search.trim(), true) }.let { list -> if (sortName) list.sortedBy { it.name.lowercase() } else list.sortedByDescending { abs(balances[it.id] ?: 0.0) } }
    PageColumn {
        PageHeader("Friends", back) { TextButton({ navigate("new-friend") }) { Icon(Icons.Outlined.Add, null, Modifier.size(18.dp)); Text("Add friend", fontSize = 12.sp) } }
        PageList {
            if (codes.size > 1) item { FilterChips(codes, code, { code = it }) }
            item {
                Panel {
                    Row(Modifier.padding(18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        BalanceFigure("You're owed", "${currencySymbol(code)} ${money(balances.values.filter { it > 0 }.sum())}", Modifier.weight(1f), Green)
                        VerticalDivider(Modifier.height(48.dp), color = BorderLight)
                        BalanceFigure("You owe", "${currencySymbol(code)} ${money(abs(balances.values.filter { it < 0 }.sum()))}", Modifier.weight(1f), Danger)
                    }
                }
            }
            item { SearchField(search, { search = it }, "Search friends") }
            item { SectionHeading("Your friends", "${visible.size}") { TextButton({ sortName = !sortName }) { Text(if (sortName) "Name A–Z ▾" else "Highest balance ▾", fontSize = 11.sp) } } }
            if (visible.isEmpty()) item { EmptyState(if (data.friends.isEmpty()) "No friends yet" else "No friends found", if (data.friends.isEmpty()) "Save a friend’s name and public ID to start sharing expenses." else "Try a different name.") }
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

@Composable
internal fun BalanceFigure(label: String, amount: String, modifier: Modifier = Modifier, color: androidx.compose.ui.graphics.Color = Ink) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = Muted, fontSize = 12.sp)
        Text(amount, color = color, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun AddFriendScreen(model: AppViewModel, back: () -> Unit) {
    val context = LocalContext.current
    var name by rememberSaveable { mutableStateOf("") }
    var publicId by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val scanner = remember(context) {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .enableAutoZoom()
            .build()
        GmsBarcodeScanning.getClient(context, options)
    }

    fun startScan() {
        scanner.startScan()
            .addOnSuccessListener { barcode ->
                val raw = barcode.rawValue ?: barcode.displayValue ?: ""
                val matched = Regex("[0-9a-fA-F]{64}").find(raw)?.value?.lowercase()
                if (matched != null) {
                    publicId = matched
                    error = null
                    model.message = "Public ID detected"
                } else if (raw.isNotBlank()) {
                    publicId = raw.trim()
                    error = null
                }
            }
            .addOnFailureListener { e ->
                model.message = "Scan failed: ${e.localizedMessage ?: "Could not open scanner"}"
            }
    }

    PageColumn {
        PageHeader("Add friend", back)
        PageList {
            item {
                Panel {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Text("* Required", color = Muted, fontSize = 12.sp)
                        Text("Ask your friend for the public ID on their profile. Once saved, you can add them to groups.", color = Muted, fontSize = 14.sp, lineHeight = 22.sp)
                        InputField(
                            value = publicId,
                            onChange = { publicId = it; error = null },
                            placeholder = "Paste or scan full public ID",
                            label = "Public ID *",
                            multiline = true,
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
                        Text("64 characters · letters a–f and numbers 0–9", color = Muted, fontSize = 12.sp)
                        InputField(name, { name = it.take(60); error = null }, "How you know them", "Name *")
                        ErrorText(error)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SecondaryButton("Cancel", back, Modifier.weight(1f), enabled = !model.busy)
                            PrimaryButton(if (model.busy) "Saving…" else "Add friend", {
                                val key = publicId.trim().lowercase()
                                when {
                                    !key.matches(Regex("[0-9a-f]{64}")) -> error = "Enter a full 64-character public ID using a–f and 0–9."
                                    name.isBlank() -> error = "Enter your friend’s name."
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

private data class FriendEntry(val key: String, val title: String, val subtitle: String, val amount: String,
    val positive: Boolean, val settled: Boolean, val date: Long, val personal: SplitTransaction? = null, val group: FriendGroupTransaction? = null)

@Composable
internal fun FriendScreen(friend: Friend, data: AppData, model: AppViewModel, navigate: (String) -> Unit, back: () -> Unit) {
    var groupFilter by rememberSaveable { mutableStateOf("All") }
    var selected by remember { mutableStateOf<FriendEntry?>(null) }
    var menu by remember { mutableStateOf(false) }
    var remove by remember { mutableStateOf(false) }
    var edit by remember { mutableStateOf(false) }
    var name by rememberSaveable(friend.id) { mutableStateOf(friend.name) }
    val context = LocalContext.current
    val groupEntries = data.groupTransactions[friend.id].orEmpty()
    val groups = groupEntries.map { it.groupId to it.groupName }.distinct()
    val entries = (data.splits.filter { it.friendId == friend.id }.map { split -> FriendEntry("p:${split.id}", split.note.ifBlank { "Split expense" }, "Personal · ${displayDate(split.date, model)}",
        "${model.currency} ${money(abs(split.amount))}", split.amount > 0, split.isSettled, split.date, personal = split) } +
        groupEntries.map { txn -> FriendEntry("g:${txn.debtUnitId}", txn.description, "${txn.groupName} · ${displayDate(txn.occurredAtMillis, model)}", "${currencySymbol(txn.currencyCode)} ${money(abs(txn.amount))}", txn.amount > 0, txn.isSettled, txn.occurredAtMillis, group = txn) })
        .filter { groupFilter == "All" || groupFilter == "Personal" && it.personal != null || it.group?.groupId == groupFilter }.sortedByDescending { it.date }
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
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        val codes = (listOf(currencyCodeFor(model.currency)) + groupEntries.map { it.currencyCode }).distinct()
                        codes.forEach { code ->
                            val balance = data.balance(friend.id, code, currencyCodeFor(model.currency))
                            BalanceFigure(if (balance > 0) "${friend.name} owes you" else if (balance < 0) "You owe ${friend.name}" else "All settled · $code",
                                "${currencySymbol(code)} ${money(abs(balance))}", color = if (balance < 0) Danger else Green)
                        }
                        PrimaryButton("Add split expense", { navigate("direct-split/${friend.id}") })
                    }
                }
            }
            item {
                val labels = listOf("All", "Personal") + groups.map { it.second }
                val selectedLabel = groups.find { it.first == groupFilter }?.second ?: groupFilter
                FilterChips(labels, selectedLabel, { value -> groupFilter = groups.find { it.second == value }?.first ?: value })
            }
            item { SectionHeading("Transactions", "${entries.size}") }
            if (entries.isEmpty()) item { EmptyState("No transactions here yet", "Add a split to start keeping track.") }
            itemsIndexed(entries, key = { _, entry -> entry.key }) { _, entry ->
                Panel { LedgerRow(entry.title, entry.subtitle, entry.amount,
                    if (entry.settled) "Settled" else if (entry.positive) "Owes you" else "You owe",
                    if (entry.settled) Muted else if (entry.positive) Green else Danger, onClick = { selected = entry }) }
            }
        }
    }
    selected?.let { entry ->
        val txn = entry.personal
        AlertDialog(onDismissRequest = { if (!model.busy) selected = null }, title = { Text(entry.title) },
            text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { DetailLine("Amount", entry.amount); DetailLine("Details", entry.subtitle); Text(if (entry.settled) "Settled" else if (entry.positive) "Owes you" else "You owe") } },
            confirmButton = {
                if (txn != null && !txn.isSettled) TextButton({ model.perform("Payment recorded", { selected = null }) { model.repository.settleSplitTransaction(txn, friend.name, data.friends) } }, enabled = !model.busy) { Text("Mark settled") }
                else if (entry.group != null) TextButton({ selected = null; navigate("group/${entry.group.groupId}") }) { Text("Open group") }
                else TextButton({ selected = null }) { Text("Done") }
            }, dismissButton = {
                if (txn != null) TextButton({ model.perform("Split removed", { selected = null }) { model.repository.deleteSplitTransaction(txn, data.friends) } }, enabled = !model.busy) { Text("Delete", color = Danger) }
                else TextButton({ selected = null }) { Text("Close") }
            })
    }
    if (remove) ConfirmDialog("Remove friend?", "${friend.name} and their local splits will be removed. Group history stays available.", "Remove", { remove = false },
        { model.perform("Friend removed", back) { model.repository.deleteFriend(friend.id) } }, model.busy)
    if (edit) ConfirmDialog("Edit friend", "Choose the name you want to see in your ledger.", "Save", { edit = false }, {
        if (name.isNotBlank()) model.perform("Name updated", { edit = false }) { model.repository.saveFriend(friend.copy(name = name.trim(), updatedAt = System.currentTimeMillis())) }
    }, model.busy, content = { InputField(name, { name = it.take(60) }, "Friend's name") })
}
