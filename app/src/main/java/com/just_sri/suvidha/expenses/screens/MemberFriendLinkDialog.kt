package com.just_sri.suvidha.expenses.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.just_sri.suvidha.expenses.*
import com.just_sri.suvidha.expenses.components.AppEmptyState
import com.just_sri.suvidha.expenses.components.AppOutlinedTextField
import com.just_sri.suvidha.expenses.components.AppSearchBar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MemberFriendLinkDialog(
    repository: SharedGroupRepository,
    groupId: String,
    memberId: String,
    friends: List<Friend>,
    currentFriendId: String?,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var query by remember { mutableStateOf("") }
    var newFriendName by remember { mutableStateOf("") }
    var showContacts by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun dismissSheet() {
        if (!busy) {
            scope.launch {
                sheetState.hide()
            }.invokeOnCompletion {
                onDismiss()
            }
        }
    }

    fun save(action: suspend () -> Unit) {
        if (busy) return
        busy = true
        error = null
        scope.launch {
            try {
                action()
                dismissSheet()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                showContacts = false
                error = failure.message ?: "Could not update member link."
            } finally {
                busy = false
            }
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
                    save {
                        repository.linkMemberToContact(
                            groupId,
                            memberId,
                            id.removePrefix("phone_contact_"),
                            contactName,
                            number
                        )
                    }
                },
            )
        }
    } else {
        ModalBottomSheet(
            onDismissRequest = { if (!busy) onDismiss() },
            sheetState = sheetState,
            containerColor = Canvas,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = { BottomSheetDefaults.DragHandle() },
        ) {
            val currentFriend = remember(friends, currentFriendId) {
                friends.find { it.id == currentFriendId }
            }
            val filteredFriends = remember(friends, query) {
                friends.filter {
                    query.isBlank() || it.name.contains(query, ignoreCase = true)
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Header with Eyebrow, Title and Close Button
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "MEMBER IDENTITY",
                                color = Green,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.3.sp
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Link group member",
                                color = Ink,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(
                            onClick = { dismissSheet() },
                            enabled = !busy,
                            modifier = Modifier
                                .size(36.dp)
                                .background(CardSurface, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Muted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // 2. Member Public Key Card with Copy action
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = CardSurface,
                        border = BorderStroke(1.dp, BorderLight)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Green.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Green,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Nostr Public Key",
                                    fontSize = 11.sp,
                                    color = Muted,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${memberId.take(10)}…${memberId.takeLast(8)}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Ink
                                )
                            }
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Nostr Public Key", memberId))
                                    Toast.makeText(context, "Public key copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Copy public key",
                                    tint = Green,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // 3. Currently Linked Status or Helper text
                item {
                    if (currentFriend != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = Green.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, Green.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(Green, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = currentFriend.name.take(1).uppercase(),
                                        color = OffWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "CURRENTLY LINKED",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Green,
                                            letterSpacing = 0.8.sp
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .background(Green, CircleShape)
                                        )
                                        Text(
                                            text = "Local identity",
                                            fontSize = 11.sp,
                                            color = Muted
                                        )
                                    }
                                    Text(
                                        text = currentFriend.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Ink
                                    )
                                    if (!currentFriend.phoneNumber.isNullOrBlank()) {
                                        Text(
                                            text = currentFriend.phoneNumber,
                                            fontSize = 11.sp,
                                            color = Muted
                                        )
                                    }
                                }
                                TextButton(
                                    enabled = !busy,
                                    onClick = { save { repository.unlinkMemberFromFriend(memberId) } },
                                    colors = ButtonDefaults.textButtonColors(contentColor = Danger)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp),
                                        tint = Danger
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "Unlink",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Danger
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "Link this member to display their name on shared expenses instead of their cryptographic key.",
                            color = Muted,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }

                // 4. Quick Action: Pick from Phone Contacts
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(enabled = !busy) { showContacts = true },
                        shape = RoundedCornerShape(16.dp),
                        color = CardSurface,
                        border = BorderStroke(1.dp, BorderLight)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(Green.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = Green,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Choose from Phone Contacts",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Ink
                                )
                                Text(
                                    text = "Import contact from device address book",
                                    fontSize = 11.sp,
                                    color = Muted
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                                contentDescription = null,
                                tint = Muted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // 5. Section: Existing Friends
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Existing Friends",
                            color = Ink,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (friends.isNotEmpty()) {
                            Text(
                                text = "  ·  ${friends.size}",
                                color = Muted,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // Search Bar (if 3 or more friends, or if active search)
                if (friends.size >= 3 || query.isNotBlank()) {
                    item {
                        AppSearchBar(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = "Search friends…"
                        )
                    }
                }

                if (filteredFriends.isEmpty()) {
                    item {
                        AppEmptyState(
                            title = if (query.isBlank()) "No friends yet" else "No matching friends",
                            message = if (query.isBlank()) {
                                "Add a new friend below or choose from phone contacts."
                            } else {
                                "No friend found matching \"$query\"."
                            }
                        )
                    }
                } else {
                    items(filteredFriends, key = { it.id }) { friend ->
                        val isLinked = friend.id == currentFriendId
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable(enabled = !busy) {
                                    save { repository.linkMemberToFriend(groupId, memberId, friend.id) }
                                },
                            shape = RoundedCornerShape(14.dp),
                            color = CardSurface,
                            border = BorderStroke(1.dp, if (isLinked) Green else BorderLight)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            if (isLinked) Green else Green.copy(alpha = 0.09f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = friend.name.take(1).uppercase(),
                                        color = if (isLinked) OffWhite else Green,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = friend.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Ink,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (!friend.phoneNumber.isNullOrBlank()) {
                                        Text(
                                            text = friend.phoneNumber,
                                            fontSize = 11.sp,
                                            color = Muted
                                        )
                                    }
                                }
                                if (isLinked) {
                                    Surface(
                                        color = Green.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Green,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = "Linked",
                                                color = Green,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                } else {
                                    Surface(
                                        color = Canvas,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Link",
                                                color = Green,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Spacer(Modifier.width(3.dp))
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                                                contentDescription = null,
                                                tint = Green,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 6. Section: Create New Friend
                item {
                    Text(
                        text = "Or create a new friend",
                        color = Ink,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = CardSurface,
                        border = BorderStroke(1.dp, BorderLight)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Enter a name to create a friend on this device and link them to this member.",
                                fontSize = 12.sp,
                                color = Muted,
                                lineHeight = 16.sp
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AppOutlinedTextField(
                                    value = newFriendName,
                                    onValueChange = { newFriendName = it.take(100) },
                                    placeholder = "Friend's name (e.g. Alex)",
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Words,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            if (newFriendName.isNotBlank() && !busy) {
                                                save { repository.createFriendForMember(groupId, memberId, newFriendName.trim()) }
                                            }
                                        }
                                    )
                                )
                                Button(
                                    onClick = {
                                        save { repository.createFriendForMember(groupId, memberId, newFriendName.trim()) }
                                    },
                                    enabled = !busy && newFriendName.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Green,
                                        contentColor = OffWhite,
                                        disabledContainerColor = Green.copy(alpha = 0.35f),
                                        disabledContentColor = OffWhite.copy(alpha = 0.7f)
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.height(52.dp)
                                ) {
                                    if (busy) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = OffWhite,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text(
                                            text = "Add & Link",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 7. Error message
                error?.let { errText ->
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Danger.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, Danger.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = errText,
                                color = Danger,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

