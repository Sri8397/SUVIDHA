package com.just_sri.suvidha.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID

internal data class ExpenseDraft(val category: String = "Other", val amount: String = "", val note: String = "")

@Composable
internal fun AddExpenseDialog(categories: List<String>, onDismiss: () -> Unit, onSave: (List<ExpenseDraft>) -> Unit) {
    val drafts = remember { androidx.compose.runtime.mutableStateListOf(ExpenseDraft()) }
    var invalid by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add expenses", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.heightIn(max = 450.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                drafts.forEachIndexed { index, draft ->
                    var categoryMenu by remember(index) { mutableStateOf(false) }
                    Column(Modifier.fillMaxWidth().background(Canvas, RoundedCornerShape(16.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Expense ${index + 1}", color = Ink, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            if (drafts.size > 1) Text("Remove", color = Color(0xFFB64C45), fontSize = 12.sp, modifier = Modifier.clickable { drafts.removeAt(index) }.padding(6.dp))
                        }
                        Box {
                            TextButton(onClick = { categoryMenu = true }) { Text("${draft.category}  ▾", color = Green) }
                            DropdownMenu(expanded = categoryMenu, onDismissRequest = { categoryMenu = false }) {
                                categories.forEach { category ->
                                    DropdownMenuItem(text = { Text(category) }, onClick = { drafts[index] = draft.copy(category = category); categoryMenu = false })
                                }
                            }
                        }
                        val currency = LocalCurrency.current
                        OutlinedTextField(draft.amount, { value -> drafts[index] = draft.copy(amount = value); invalid = false }, label = { Text("Amount ($currency)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, isError = invalid && (draft.amount.toDoubleOrNull() ?: 0.0) <= 0, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(draft.note, { value -> drafts[index] = draft.copy(note = value) }, label = { Text("Description (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                }
                TextButton(onClick = { drafts.add(ExpenseDraft()) }, modifier = Modifier.fillMaxWidth()) { Text("+  Add another expense", color = Green) }
                if (invalid) Text("Enter a valid amount for each expense.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
        },
        confirmButton = { TextButton(onClick = {
            if (drafts.isEmpty() || drafts.any { (it.amount.toDoubleOrNull() ?: 0.0) <= 0 }) invalid = true
            else onSave(drafts.toList())
        }) { Text("Save ${drafts.size}") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

internal data class SplitDraft(val friendId: String, val amount: String = "", val note: String = "")

@Composable
internal fun SplitEntryDialog(friends: List<FriendBalance>, initialFriendId: String?, onDismiss: () -> Unit, onSave: (List<SplitDraft>) -> Unit) {
    val drafts = remember(friends, initialFriendId) {
        androidx.compose.runtime.mutableStateListOf(SplitDraft(initialFriendId ?: friends.firstOrNull()?.id.orEmpty()))
    }
    var invalid by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add split expenses", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.heightIn(max = 450.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Positive: they owe you. Negative: you owe them.", color = Muted, fontSize = 12.sp)
                Text("Each amount adds to the current balance. Use a minus sign to subtract.", color = Muted, fontSize = 11.sp)
                drafts.forEachIndexed { index, draft ->
                    var expanded by remember(index) { mutableStateOf(false) }
                    Column(Modifier.fillMaxWidth().background(Canvas, RoundedCornerShape(16.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Split ${index + 1}", color = Ink, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            if (drafts.size > 1) Text("Remove", color = Color(0xFFB64C45), fontSize = 12.sp, modifier = Modifier.clickable { drafts.removeAt(index) }.padding(6.dp))
                        }
                        Box {
                            val friendName = friends.firstOrNull { it.id == draft.friendId }?.name ?: "Choose friend"
                            TextButton(onClick = { expanded = true }) { Text("$friendName  ▾", color = Green) }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                friends.forEach { friend ->
                                    DropdownMenuItem(text = { Text(friend.name) }, onClick = { drafts[index] = draft.copy(friendId = friend.id); expanded = false })
                                }
                            }
                        }
                        val currency = LocalCurrency.current
                        OutlinedTextField(draft.amount, { amount -> drafts[index] = draft.copy(amount = amount); invalid = false }, label = { Text("Net amount ($currency)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, isError = invalid && draft.amount.toDoubleOrNull() == null, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(draft.note, { note -> drafts[index] = draft.copy(note = note) }, label = { Text("Description (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                }
                TextButton(onClick = { drafts.add(SplitDraft(friends.firstOrNull()?.id.orEmpty())) }, modifier = Modifier.fillMaxWidth()) { Text("+  Add another split", color = Green) }
                if (invalid) Text("Choose a friend and enter an amount for each split.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
        },
        confirmButton = { TextButton(onClick = {
            if (drafts.isEmpty() || drafts.any { it.friendId.isBlank() || it.amount.toDoubleOrNull() == null }) invalid = true
            else onSave(drafts.toList())
        }) { Text("Save ${drafts.size}") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

