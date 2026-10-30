package com.just_sri.suvidha.expenses.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.ui.res.painterResource
import com.just_sri.suvidha.expenses.R
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.just_sri.suvidha.expenses.*

/** Shared amount/details flow for personal expenses, direct splits and group splits. */
@Composable
internal fun MoneyFlow(data: AppData, model: AppViewModel, back: () -> Unit, group: SharedGroupEntity? = null, friend: Friend? = null) {
    var details by rememberSaveable { mutableStateOf(false) }
    var amount by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("Other") }
    var splitMode by rememberSaveable { mutableStateOf("Equally") }
    var payer by rememberSaveable { mutableStateOf("You") }
    var includeSelf by rememberSaveable { mutableStateOf(true) }
    val inputs = remember { mutableStateMapOf<String, String>() }
    val deselected = remember { mutableStateMapOf<String, Boolean>() }
    val code = group?.currencyCode ?: currencyCodeFor(model.currency)
    val symbol = currencySymbol(code)
    val isSplit = group != null || friend != null
    val participants = if (group != null) data.members.filter { it.groupId == group.id && it.isActive && it.memberId != data.ownId }.map { it.memberId to data.label(it.memberId) }
        else friend?.let { listOf(it.id to it.name) }.orEmpty()
    val selected = participants.filter { deselected[it.first] != true }
    val ownId = data.ownId ?: "self"
    val total = SharedSplitMoney.parseMinor(amount, code) ?: 0L
    var error by remember { mutableStateOf<String?>(null) }
    val title = if (isSplit) "Split expense" else "Add expense"
    val goBack = { if (details && !model.busy) details = false else if (!model.busy) back() }
    BackHandler(enabled = !model.busy, onBack = goBack)
    PageColumn {
        PageHeader(if (details) title else "", goBack)
        if (!details) {
            Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(28.dp))
                Text(if (isSplit) "Enter total amount" else "Enter expense amount", color = Muted, fontSize = 16.sp)
                Spacer(Modifier.height(24.dp))
                Text("$symbol ${amount.ifBlank { "0" }}", color = Ink, fontSize = if (amount.length > 9) 34.sp else 48.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp)
                Spacer(Modifier.weight(1f))
                PrimaryButton("Next", { details = true }, enabled = total > 0)
                Spacer(Modifier.height(20.dp))
                NumericKeypad(amount, { amount = it }, SharedSplitMoney.digits(code))
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                PageList(Modifier.weight(1f)) {
                    item {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Amount", color = Muted, fontSize = 13.sp)
                            TextButton({ details = false }, enabled = !model.busy) { Text("$symbol ${amount.ifBlank { "0" }}  ✎", color = Ink, fontSize = 36.sp, fontWeight = FontWeight.Bold) }
                        }
                    }
                    item { InputField(note, { note = it.take(200) }, "What’s this for?", label = "Description") }
                    if (!isSplit) {
                        item { SectionHeading("Category", if (isIncomeCategory(category)) "Income" else "Expense") }
                        item {
                            @OptIn(ExperimentalLayoutApi::class)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                model.categories.forEach { value -> FilterChip(selected = category == value, onClick = { category = value }, label = { Text(value, fontSize = 12.sp) }, shape = RoundedCornerShape(50)) }
                            }
                        }
                        item { Text(if (isIncomeCategory(category)) "This will be recorded under Income." else "This will be recorded under Expenses.", color = Muted, fontSize = 12.sp) }
                    } else {
                        item { Text(group?.let { "${it.name} · Paid by You" } ?: "Split with ${friend?.name}", color = Muted, fontSize = 13.sp) }
                        if (friend != null) item { Segments(listOf("You", friend.name), payer, { payer = it }) }
                        item { Segments(listOf("Equally", "Exact", "By shares"), splitMode, { splitMode = it; error = null }) }
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(includeSelf, { includeSelf = it })
                                Text("Include your share", color = Ink, fontSize = 14.sp)
                            }
                        }
                        items(participants, key = { it.first }) { (id, name) ->
                            Panel {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Checkbox(deselected[id] != true, { deselected[id] = !it; error = null })
                                    Text(name, Modifier.weight(1f), color = Ink, fontSize = 14.sp)
                                    if (splitMode != "Equally" && deselected[id] != true) InputField(inputs[id].orEmpty(), { inputs[id] = it.take(12); error = null },
                                        if (splitMode == "Exact") symbol else "1 share", modifier = Modifier.width(110.dp), keyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                                }
                            }
                        }
                        if (includeSelf && splitMode == "Exact") item { InputField(inputs[ownId].orEmpty(), { inputs[ownId] = it.take(12); error = null }, symbol, "Your exact share", keyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal)) }
                        if (includeSelf && splitMode == "By shares") item { InputField(inputs[ownId].orEmpty(), { inputs[ownId] = it.take(6); error = null }, "1", "Your shares", keyboard = KeyboardOptions(keyboardType = KeyboardType.Number)) }
                        if (participants.isEmpty()) item { EmptyState("Add a member first", "Invite a friend to this group before adding a split.") }
                    }
                    item { ErrorText(error) }
                }
                Surface(color = CardSurface, shadowElevation = 4.dp) {
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        fun save(another: Boolean) {
                            error = null
                            val allocation = if (!isSplit) emptyMap() else runCatching {
                                require(selected.isNotEmpty()) { "Select at least one friend." }
                                val ids = selected.map { it.first } + if (includeSelf) listOf(ownId) else emptyList()
                                if (splitMode == "Exact") {
                                    val exact = ids.associateWith { SharedSplitMoney.parseMinor(inputs[it].orEmpty(), code) ?: error("Enter every exact share.") }
                                    require(exact.values.fold(java.math.BigInteger.ZERO) { a, v -> a + v.toBigInteger() } == total.toBigInteger()) { "Exact shares must add up to $symbol $amount." }
                                    exact
                                } else SharedSplitMoney.allocate(total, ids.associateWith { if (splitMode == "By shares") (inputs[it]?.takeIf(String::isNotBlank)?.toIntOrNull() ?: if (inputs[it].isNullOrBlank()) 1 else 0) else 1 })
                            }.getOrElse { error = it.message ?: "Check the split amounts."; return }
                            val owed = allocation.filterKeys { it != ownId }.filterValues { it > 0 }
                            if (isSplit && owed.isEmpty()) { error = "The split must include a positive share for a friend."; return }
                            model.perform("Saved", onDone = {
                                if (another) { amount = ""; note = ""; details = false; inputs.clear() } else back()
                            }) {
                                when {
                                    group != null -> {
                                        model.peers.addExpense(group.id, SharedExpenseDraft(note, owed))
                                        model.backgroundSync()
                                    }
                                    friend != null -> {
                                        val value = SharedSplitMoney.format(owed.getValue(friend.id), code)
                                        model.repository.saveSplitTransactions(listOf(SplitDraft(friend.id, if (payer == "You") value else "-$value", note)), mapOf(friend.id to friend), data.friends)
                                    }
                                    else -> model.repository.addExpense(ExpenseDraft(category, amount, note.trim()))
                                }
                            }
                        }
                        PrimaryButton(if (model.busy) "Saving…" else "Save & exit · $symbol $amount", { save(false) }, enabled = !model.busy)
                        TextButton({ save(true) }, Modifier.fillMaxWidth(), enabled = !model.busy) { Text("Save & add another", fontSize = 13.sp) }
                    }
                }
            }
        }
    }
}

@Composable
internal fun NumericKeypad(value: String, onChange: (String) -> Unit, fractionDigits: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf(".", "0", "delete")).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { key ->
                    Surface(onClick = { onChange(keypadValue(value, key, fractionDigits)) }, modifier = Modifier.weight(1f), shape = Ui.FieldRadius,
                        color = CardSurface, enabled = key != "." || fractionDigits > 0) {
                        Box(Modifier.height(56.dp), contentAlignment = Alignment.Center) {
                            if (key == "delete") Icon(painterResource(R.drawable.ic_backspace), "Delete digit", tint = Ink)
                            else Text(key, color = Ink, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

internal fun keypadValue(current: String, key: String, fractionDigits: Int): String = when {
    key == "delete" -> current.dropLast(1)
    key == "." -> if (fractionDigits > 0 && '.' !in current) current.ifBlank { "0" } + "." else current
    key.length != 1 || key[0] !in '0'..'9' -> current
    current.count { it.isDigit() } >= 12 -> current
    '.' in current && current.substringAfter('.').length >= fractionDigits -> current
    current == "0" -> key
    else -> current + key
}
