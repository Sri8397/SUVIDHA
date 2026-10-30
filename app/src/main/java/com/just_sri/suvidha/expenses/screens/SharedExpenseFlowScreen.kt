package com.just_sri.suvidha.expenses.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.just_sri.suvidha.expenses.*
import java.util.Currency

private enum class SharedSplitMode { EQUAL, EXACT, SHARES }

/** The existing amount keypad and split layout, backed by integer group allocations. */
@Composable
internal fun SharedExpenseFlowScreen(
    groupName: String,
    currencyCode: String,
    ownMemberId: String,
    members: List<SharedExpenseParticipant>,
    saving: Boolean,
    error: String?,
    onBack: () -> Unit,
    onSave: (SharedExpenseDraft, Boolean) -> Unit,
) {
    var distribution by remember { mutableStateOf(false) }
    var amountInput by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(SharedSplitMode.EQUAL) }
    var includeMyself by remember { mutableStateOf(true) }
    var ownShares by remember { mutableIntStateOf(1) }
    var note by remember { mutableStateOf("") }
    var showNote by remember { mutableStateOf(false) }
    val memberIds = members.map { it.memberId }
    var selected by remember(memberIds) { mutableStateOf(memberIds.toSet()) }
    val weights = remember(memberIds) { mutableStateMapOf<String, Int>().apply { memberIds.forEach { put(it, 1) } } }
    val exact = remember(memberIds) { mutableStateMapOf<String, String>().apply { memberIds.forEach { put(it, "") } } }
    val symbol = remember(currencyCode) { com.just_sri.suvidha.expenses.constants.currencySymbol(currencyCode) }
    val enteredTotal = SharedSplitMoney.parseMinor(amountInput, currencyCode) ?: 0L
    val chosen = members.filter { it.memberId in selected }
    val calculated = runCatching {
        if (mode == SharedSplitMode.EXACT) chosen.associate { member ->
            member.memberId to (SharedSplitMoney.parseMinor(exact[member.memberId].orEmpty(), currencyCode)
                ?.takeIf { it > 0 } ?: throw IllegalArgumentException("Enter a positive exact amount for each selected member."))
        } else {
            val shares = chosen.associate { it.memberId to if (mode == SharedSplitMode.SHARES) weights[it.memberId] ?: 1 else 1 }.toMutableMap()
            if (includeMyself) shares[ownMemberId] = if (mode == SharedSplitMode.SHARES) ownShares else 1
            SharedSplitMoney.allocate(enteredTotal, shares)
        }
    }.getOrNull()
    val total = if (mode == SharedSplitMode.EXACT) runCatching {
        calculated?.values?.fold(0L) { sum, amount -> Math.addExact(sum, amount) } ?: 0L
    }.getOrNull() else enteredTotal
    val debts = calculated.orEmpty().filter { (id, amount) -> id != ownMemberId && amount > 0 }
    val valid = chosen.isNotEmpty() && calculated != null && total != null && total > 0 && debts.isNotEmpty()

    BackHandler { if (!saving) { if (distribution) distribution = false else onBack() } }
    if (showNote) NoteEntryDialog(note, { showNote = false }, { note = it.take(200); showNote = false })

    Surface(Modifier.fillMaxSize(), color = Canvas) {
        if (!distribution) {
            AmountEntryStep(
                title = "Enter amount to split",
                currency = symbol,
                amountText = amountInput,
                onAmountChange = { amountInput = it },
                onBack = onBack,
                onPrimary = { if (enteredTotal > 0) { mode = SharedSplitMode.EQUAL; distribution = true } },
                primaryEnabled = enteredTotal > 0,
                fractionDigits = SharedSplitMoney.digits(currencyCode),
                trailingAction = { TextButton(onClick = { mode = SharedSplitMode.EXACT; distribution = true }) { Text("Exact amounts →", color = Green) } },
            )
        } else {
            Scaffold(
                containerColor = Canvas,
                contentWindowInsets = WindowInsets.statusBars,
                bottomBar = {
                    FlowSaveBar(
                        label = if (saving) "Saving…" else "Save split · $symbol ${SharedSplitMoney.format(total ?: 0L, currencyCode)}",
                        enabled = valid && !saving,
                        onClick = { onSave(SharedExpenseDraft(note.ifBlank { "Shared expense" }, debts), false) },
                        secondaryLabel = "Save & add another",
                        onSecondary = { onSave(SharedExpenseDraft(note.ifBlank { "Shared expense" }, debts), true) },
                    )
                },
            ) { padding ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()).imePadding(),
                    contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item { FlowTopBar("Split in $groupName", { if (!saving) distribution = false }) }
                    item {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (mode == SharedSplitMode.EXACT) "Total owed by members" else "Amount to split", color = Muted)
                            Text("$symbol ${SharedSplitMoney.format(total ?: 0L, currencyCode)}", color = Ink, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                            Text("Paid by you", color = Green, fontWeight = FontWeight.SemiBold)
                            TextButton(onClick = { showNote = true }) { Text(note.ifBlank { "What's this for?" }) }
                            if (mode != SharedSplitMode.EXACT) TextButton(onClick = { distribution = false }) { Text("Edit amount") }
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            SharedSplitMode.entries.forEach { option ->
                                FilterChip(selected = option == mode, onClick = { mode = option }, colors = FilterChipDefaults.filterChipColors(containerColor = CardSurface, labelColor = Muted, selectedContainerColor = Green, selectedLabelColor = OffWhite), label = {
                                    Text(when (option) { SharedSplitMode.EQUAL -> "Equally"; SharedSplitMode.EXACT -> "Exact (123)"; SharedSplitMode.SHARES -> "By shares" })
                                })
                            }
                        }
                    }
                    if (mode != SharedSplitMode.EXACT) item {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp).background(CardSurface, RoundedCornerShape(16.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(includeMyself, { includeMyself = it })
                            Column(Modifier.weight(1f)) {
                                Text("You", color = Ink, fontWeight = FontWeight.SemiBold)
                                Text(if (includeMyself) "Your share: $symbol ${SharedSplitMoney.format(calculated?.get(ownMemberId) ?: 0L, currencyCode)}" else "Excluded from split", color = Muted, fontSize = 12.sp)
                            }
                            if (mode == SharedSplitMode.SHARES && includeMyself) SplitShareStepper(ownShares) { ownShares = it.coerceIn(1, 10_000) }
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Members (${chosen.size}/${members.size})", color = Ink, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            TextButton(onClick = { selected = if (chosen.size == members.size) emptySet() else memberIds.toSet() }) { Text(if (chosen.size == members.size) "Deselect all" else "Select all") }
                        }
                    }
                    items(members, key = { it.memberId }) { member ->
                        val included = member.memberId in selected
                        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp).background(CardSurface, RoundedCornerShape(16.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(included, { selected = if (it) selected + member.memberId else selected - member.memberId })
                            Box(Modifier.size(36.dp).background(Green.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) { Text(member.name.take(1).uppercase(), color = Green, fontWeight = FontWeight.Bold) }
                            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                Text(member.name, color = Ink, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${member.memberId.take(8)}…", color = Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (mode != SharedSplitMode.EXACT && included) Text("$symbol ${SharedSplitMoney.format(calculated?.get(member.memberId) ?: 0L, currencyCode)}", color = Green, fontWeight = FontWeight.Bold)
                            }
                            if (included && mode == SharedSplitMode.SHARES) SplitShareStepper(weights[member.memberId] ?: 1) { weights[member.memberId] = it.coerceIn(1, 10_000) }
                            if (included && mode == SharedSplitMode.EXACT) OutlinedTextField(
                                value = exact[member.memberId].orEmpty(),
                                onValueChange = { exact[member.memberId] = it.filter { c -> c.isDigit() || c == '.' }.take(24) },
                                label = { Text(symbol) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.width(112.dp),
                            )
                        }
                    }
                    if (mode == SharedSplitMode.EXACT) item { Text("Enter the amount each selected member owes you. Your own share is excluded.", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 24.dp)) }
                    error?.let { message -> item { Text(message, color = Danger, modifier = Modifier.padding(horizontal = 24.dp)) } }
                }
            }
        }
    }
}

