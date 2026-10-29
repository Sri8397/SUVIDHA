package com.just_sri.suvidha.expenses.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.just_sri.suvidha.expenses.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun MemberFriendLinkDialog(
    repository: SharedGroupRepository,
    groupId: String,
    memberId: String,
    friends: List<Friend>,
    currentFriendId: String?,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var showContacts by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    fun save(action: suspend () -> Unit) {
        if (busy) return
        busy = true; error = null
        scope.launch {
            try { action(); onDismiss() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { showContacts = false; error = failure.message ?: "Could not link this member." }
            finally { busy = false }
        }
    }

    if (showContacts) {
        SharedFlowWindow(onDismissRequest = { if (!busy) showContacts = false }) {
            ContactsScreen(
                existingFriendIds = emptySet(),
                titleOverride = "Link member to contact",
                actionLabel = "Link",
                onBack = { showContacts = false },
                onAddFriend = { id, contactName, number ->
                    save { repository.linkMemberToContact(groupId, memberId, id.removePrefix("phone_contact_"), contactName, number) }
                },
            )
        }
    } else {
        AlertDialog(
            onDismissRequest = { if (!busy) onDismiss() },
            title = { Text("Link group member") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SelectionContainer { Text(memberId, color = Muted) }
                    Text("Choose who this public key belongs to. The name is used across groups on this phone. Contact details stay local; existing balances stay in their own ledgers.")
                    OutlinedTextField(query, { query = it }, label = { Text("Find an existing friend") }, singleLine = true)
                    LazyColumn(Modifier.heightIn(max = 200.dp)) {
                        items(friends.filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }, key = { it.id }) { friend ->
                            TextButton(enabled = !busy, onClick = { save { repository.linkMemberToFriend(groupId, memberId, friend.id) } }, modifier = Modifier.fillMaxWidth()) {
                                Text(friend.name + if (friend.id == currentFriendId) " · Linked" else "")
                            }
                        }
                    }
                    TextButton(enabled = !busy, onClick = { showContacts = true }) { Text("Choose phone contact") }
                    OutlinedTextField(name, { name = it.take(100) }, label = { Text("Or add a new friend's name") }, singleLine = true)
                    TextButton(enabled = !busy && name.isNotBlank(), onClick = { save { repository.createFriendForMember(groupId, memberId, name) } }) { Text("Create friend & link") }
                    if (currentFriendId != null) TextButton(enabled = !busy, onClick = { save { repository.unlinkMemberFromFriend(memberId) } }) { Text("Remove local link") }
                    error?.let { Text(it, color = Danger) }
                }
            },
            confirmButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text(if (busy) "Saving…" else "Done") } },
        )
    }
}
