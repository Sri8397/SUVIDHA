package com.just_sri.suvidha.expenses

import com.just_sri.suvidha.expenses.components.AppPrimaryButton
import com.just_sri.suvidha.expenses.components.AppSearchBar
import com.just_sri.suvidha.expenses.components.AppTopBar

import android.Manifest
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class PhoneContact(
    val id: String,
    val name: String,
    val rawNumber: String,
    val normalizedNumber: String,
    val displayNumber: String
)

@Composable
internal fun ContactsScreen(
    existingFriendIds: Set<String>,
    assignedContactIds: Set<String> = emptySet(),
    friendToLink: FriendBalance? = null,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    // Adding is unavailable while assigning a contact to an existing friend.
    onAddFriend: (id: String, name: String, number: String) -> Unit = { _, _, _ -> },
    onLinkContact: (friend: FriendBalance, contactId: String, number: String) -> Unit = { _, _, _ -> }
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED)
    }
    var contacts by remember { mutableStateOf<List<PhoneContact>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val addedContactIds = remember { mutableStateListOf<String>() }
    val scope = rememberCoroutineScope()

    fun loadContacts() {
        loading = true
        scope.launch {
            val found = withContext(Dispatchers.IO) {
                val rows = mutableListOf<PhoneContact>()
                val seen = mutableSetOf<String>()
                val projection = arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                )
                context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    projection,
                    null,
                    null,
                    "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} COLLATE NOCASE ASC"
                )?.use { cursor ->
                    val idColumn = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                    val nameColumn = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numberColumn = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    while (cursor.moveToNext()) {
                        val id = cursor.getString(idColumn) ?: continue
                        val name = cursor.getString(nameColumn)?.trim().orEmpty()
                        val rawNumber = cursor.getString(numberColumn)?.trim().orEmpty()
                        val cleaned = PhoneUtils.clean(rawNumber) ?: continue
                        val (e164, formatted) = cleaned
                        if (name.isNotBlank() && seen.add(e164)) {
                            rows += PhoneContact(id, name, rawNumber, e164, formatted)
                        }
                    }
                }
                rows
            }
            contacts = found
            loading = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        if (granted) loadContacts()
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) loadContacts()
    }

    val availableContacts = remember(contacts, existingFriendIds, assignedContactIds, addedContactIds.toList(), friendToLink) {
        contacts.filterNot { contact ->
            if (friendToLink != null) {
                contact.id in assignedContactIds
            } else {
                "phone_contact_${contact.id}" in existingFriendIds || contact.id in addedContactIds
            }
        }
    }

    val visibleContacts = remember(availableContacts, query) {
        val needle = query.trim().lowercase()
        val digits = needle.filter(Char::isDigit)
        if (needle.isBlank()) availableContacts else availableContacts.filter { contact ->
            contact.name.lowercase().contains(needle) ||
                (digits.isNotBlank() && (contact.normalizedNumber.contains(digits) || contact.rawNumber.contains(digits)))
        }
    }

    Column(
        modifier = modifier.fillMaxSize().background(Canvas).statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AppTopBar(
            title = if (friendToLink != null) "Assign contact" else "Start splitting",
            subtitle = if (hasPermission) "${availableContacts.size} contacts" else "Select people from your contacts",
            onBack = onBack,
            modifier = Modifier.padding(top = 12.dp),
            horizontalPadding = 0.dp,
            verticalPadding = 0.dp
        )

        if (hasPermission) {
            AppSearchBar(value = query, onValueChange = { query = it }, placeholder = "Search contacts")
            when {
                loading -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Green)
                }
                contacts.isEmpty() -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("No contacts with phone numbers found.", color = Muted, fontSize = 14.sp)
                }
                availableContacts.isEmpty() -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text(if (friendToLink != null) "All contacts with phone numbers are already assigned." else "All contacts have already been added.", color = Muted, fontSize = 14.sp)
                }
                visibleContacts.isEmpty() -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("No matching contacts.", color = Muted, fontSize = 14.sp)
                }
                else -> LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 110.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item {
                        Text("${visibleContacts.size} contacts", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp))
                    }
                    items(visibleContacts, key = { it.id + it.normalizedNumber }) { contact ->
                        val friendId = "phone_contact_${contact.id}"
                        Row(
                            Modifier
                                .animateItem()
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(44.dp).background(Color(0xFFEAF4EE), CircleShape), contentAlignment = Alignment.Center) {
                                Text(contact.name.firstOrNull()?.uppercase() ?: "?", color = Green, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Column(Modifier.weight(1f).padding(start = 12.dp, end = 10.dp)) {
                                Text(contact.name, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                Text(contact.displayNumber, color = Muted, fontSize = 12.sp, maxLines = 1)
                            }
                            Button(
                                onClick = {
                                    if (friendToLink != null) {
                                        onLinkContact(friendToLink, contact.id, contact.normalizedNumber)
                                        onBack()
                                    } else {
                                        onAddFriend(friendId, contact.name, contact.normalizedNumber)
                                        addedContactIds.add(contact.id)
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 18.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Green,
                                    contentColor = OffWhite
                                )
                            ) { Text(if (friendToLink != null) "Assign" else "Add", fontWeight = FontWeight.SemiBold) }
                        }
                    }
                }
            }
        } else {
            Column(
                Modifier.fillMaxWidth().weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Find people to split with", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Allow contact access to browse and search your phone contacts.", color = Muted, fontSize = 14.sp)
                Spacer(Modifier.height(18.dp))
                AppPrimaryButton(
                    text = "Show contacts",
                    onClick = { permissionLauncher.launch(Manifest.permission.READ_CONTACTS) }
                )
            }
        }
    }
}
