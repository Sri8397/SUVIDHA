package com.just_sri.suvidha.expenses

import com.just_sri.suvidha.expenses.components.AppBackButton
import com.just_sri.suvidha.expenses.components.AppConfirmDialog
import com.just_sri.suvidha.expenses.components.AppInputDialog
import com.just_sri.suvidha.expenses.components.AppSearchBar
import com.just_sri.suvidha.expenses.components.AppSwipeItem
import com.just_sri.suvidha.expenses.components.AppTextInput
import com.just_sri.suvidha.expenses.components.ScreenHeader
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.*
import java.time.format.*
import java.util.Locale
import java.util.UUID

@Composable
internal fun SettingsMenu(
    categoryCount: Int,
    friendCount: Int,
    currency: String,
    timezoneId: String,
    modifier: Modifier,
    onOpen: (String) -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .then(modifier)
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScreenHeader("SETTINGS", "Manage preferences")
        SettingsRow("Currency", currency, icon = "¤", onClick = { onOpen("currency") })
        val tzSubtitle = if (timezoneId == "AUTO") {
            val devZone = ZoneId.systemDefault()
            val offset = try {
                val off = devZone.rules.getOffset(Instant.now())
                if (off.totalSeconds == 0) "UTC" else "UTC${off.id}"
            } catch (_: Exception) { "" }
            "Auto (${devZone.id.split('/').last().replace('_', ' ')} · $offset)"
        } else {
            val offset = try {
                val off = ZoneId.of(timezoneId).rules.getOffset(Instant.now())
                if (off.totalSeconds == 0) "UTC" else "UTC${off.id}"
            } catch (_: Exception) { "" }
            "${timezoneId.replace('_', ' ')} · $offset"
        }
        SettingsRow("Timezone", tzSubtitle, icon = "🌐", onClick = { onOpen("timezone") })
        SettingsRow("Categories", "$categoryCount expense categories", icon = "C", onClick = { onOpen("categories") })
        SettingsRow("Friends", "$friendCount friends", icon = "F", onClick = { onOpen("friends") })
        SettingsRow("Encrypted backup", "Manual file recovery", icon = "B", onClick = { onOpen("backup") })
    }
}

@Composable
private fun SettingsRow(title: String, subtitle: String, icon: String = title.take(1), onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(CardSurface, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(44.dp).background(Color(0xFFEAF4EE), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, color = Green, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(title, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = Muted, fontSize = 12.sp)
        }
        Icon(
            Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = null,
            tint = Muted,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun SettingsSubHeader(title: String, modifier: Modifier = Modifier, onBack: () -> Unit, onDone: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().then(modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = Green
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            color = Ink,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        if (onDone != null) {
            IconButton(
                onClick = onDone,
                modifier = Modifier.size(40.dp).background(Green, CircleShape)
            ) {
                Icon(Icons.Outlined.Check, contentDescription = "Done", tint = OffWhite)
            }
        }
    }
}

@Composable
internal fun CurrencyManagementPage(current: String, modifier: Modifier, onBack: () -> Unit, onSave: (String) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize().then(modifier),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SettingsSubHeader("Currency", onBack = onBack) }
        item { Text("Select your currency", color = Muted, fontSize = 14.sp) }
        items(supportedCurrencies, key = { it.first }) { (symbol, label) ->
            val selected = symbol == current
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(
                        if (selected) Color(0xFFEAF4EE) else CardSurface,
                        RoundedCornerShape(16.dp)
                    )
                    .clickable { onSave(symbol) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(44.dp).background(
                        if (selected) Green else Color(0xFFEAF4EE), CircleShape
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(symbol, color = if (selected) OffWhite else Green, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Text(
                    label,
                    color = if (selected) Ink else Muted,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(start = 14.dp).weight(1f)
                )
                if (selected) {
                    Text("✓", color = Green, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
internal fun TimezoneManagementPage(
    current: String,
    modifier: Modifier,
    onBack: () -> Unit,
    onSave: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val defaultDeviceZone = remember { ZoneId.systemDefault() }
    val defaultDeviceOffset = remember {
        val off = defaultDeviceZone.rules.getOffset(Instant.now())
        if (off.totalSeconds == 0) "UTC" else "UTC${off.id}"
    }

    val filteredIds = remember(searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isBlank()) {
            timezoneIds
        } else {
            val now = Instant.now()
            timezoneIds.filter { id ->
                val city = id.substringAfter('/').replace('_', ' ')
                val offset = ZoneId.of(id).rules.getOffset(now)
                val offsetString = if (offset.totalSeconds == 0) "UTC" else "UTC${offset.id}"
                city.lowercase().contains(q) ||
                id.substringBefore('/').lowercase().contains(q) ||
                id.lowercase().contains(q) ||
                offsetString.lowercase().contains(q)
            }
        }
    }

    Column(Modifier.fillMaxSize().then(modifier)) {
      SettingsSubHeader("Timezone", Modifier.padding(horizontal = 20.dp, vertical = 12.dp), onBack = onBack)
      LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        item {
            AppSearchBar(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = "Search…",
                trailingContent = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Text("✕", color = Muted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }

        val showAuto = searchQuery.isBlank() ||
            "automatic".contains(searchQuery.trim().lowercase()) ||
            "device".contains(searchQuery.trim().lowercase()) ||
            "auto".contains(searchQuery.trim().lowercase())

        if (showAuto) {
            item {
                val isSelected = current == "AUTO"
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            if (isSelected) Color(0xFFEAF4EE) else CardSurface,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { onSave("AUTO") }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(44.dp).background(
                            if (isSelected) Green else Color(0xFFEAF4EE), CircleShape
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🌐", fontSize = 16.sp)
                    }
                    Column(
                        Modifier.padding(horizontal = 14.dp).weight(1f)
                    ) {
                        Text(
                            "Automatic (Device Time)",
                            color = if (isSelected) Ink else Muted,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            fontSize = 15.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Current device timezone: ${defaultDeviceZone.id.replace('_', ' ')} · $defaultDeviceOffset",
                            color = Muted,
                            fontSize = 12.sp
                        )
                    }
                    if (isSelected) {
                        Text("✓", color = Green, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }

        if (filteredIds.isEmpty() && !showAuto) {
            item {
                Box(
                    Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No timezones matching \"$searchQuery\"",
                        color = Muted,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            items(filteredIds, key = { it }) { timezoneId ->
                val option = remember(timezoneId) { timezoneOption(timezoneId) }
                val isSelected = current == option.id
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            if (isSelected) Color(0xFFEAF4EE) else CardSurface,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { onSave(option.id) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(44.dp).background(
                            if (isSelected) Green else Color(0xFFEAF4EE), CircleShape
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            option.continent.take(2).uppercase(),
                            color = if (isSelected) OffWhite else Green,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Column(
                        Modifier.padding(horizontal = 14.dp).weight(1f)
                    ) {
                        Text(
                            "${option.city} (${option.continent})",
                            color = if (isSelected) Ink else Muted,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            option.id,
                            color = Muted.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        option.offsetString,
                        color = if (isSelected) Green else Muted,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .background(if (isSelected) Color(0xFFD4EAD9) else Color(0xFFEFF2EE), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    if (isSelected) {
                        Spacer(Modifier.width(8.dp))
                        Text("✓", color = Green, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
      }
    }
}

@Composable
internal fun CategoryManagementPage(categories: List<String>, modifier: Modifier, onBack: () -> Unit, onSave: (List<String>) -> Unit) {
    var editable by remember(categories) { mutableStateOf(categories) }
    var newName by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<String?>(null) }
    var editingCategory by remember { mutableStateOf<String?>(null) }
    var editCategoryText by remember { mutableStateOf("") }
    fun persist(values: List<String>) {
        editable = (values.map { it.trim() }.filter { it.isNotEmpty() && !it.equals("Other", true) }.distinct() + "Other")
        onSave(editable)
    }
    Column(Modifier.fillMaxSize().then(modifier)) {
        SettingsSubHeader("Categories", Modifier.padding(horizontal = 20.dp, vertical = 12.dp), onBack = onBack, onDone = onBack)
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
        item { Text("Expense categories", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                AppTextInput(
                    value = newName,
                    onValueChange = { newName = it.take(25) },
                    placeholder = "New category",
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(10.dp))
                Button(
                    onClick = {
                        val name = newName.trim()
                        if (name.isNotEmpty() && editable.none { it.equals(name, true) }) {
                            persist(editable.filterNot { it.equals("Other", true) } + name)
                            newName = ""
                        }
                    },
                    modifier = Modifier.height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Green)
                ) {
                    Text("Add", fontWeight = FontWeight.SemiBold)
                }
            }
        }
        itemsIndexed(editable, key = { _, category -> category.lowercase() }) { _, category ->
            SwipeManagementItem(
                onEditRequested = { editingCategory = category; editCategoryText = category },
                onDeleteRequested = { if (!category.equals("Other", true)) pendingDelete = category },
                canDelete = !category.equals("Other", true)
            ) {
                Row(
                    Modifier.fillMaxWidth().background(CardSurface, RoundedCornerShape(14.dp)).padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(category, modifier = Modifier.weight(1f), color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    if (category.equals("Other", true)) Text("Default", color = Green, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        item {
            Text("Other is always available as the default category.", color = Muted, fontSize = 12.sp)
        }
        }
    }
    pendingDelete?.let { category ->
        ConfirmDeleteDialog(
            title = "Remove category?",
            message = "$category will be removed from your expense categories.",
            confirmLabel = "Remove",
            onConfirm = { persist(editable.filterNot { it == category }); pendingDelete = null },
            onDismiss = { pendingDelete = null }
        )
    }
    editingCategory?.let { category ->
        EditNameDialog(
            title = "Edit category",
            value = editCategoryText,
            onValueChange = { editCategoryText = it.take(25) },
            onDismiss = { editingCategory = null },
            onSave = {
                val name = editCategoryText.trim()
                if (name.isNotEmpty() && editable.none { it != category && it.equals(name, true) }) {
                    persist(editable.map { if (it == category) name else it })
                }
                editingCategory = null
            }
        )
    }
}

@Composable
internal fun FriendsManagementPage(
    friends: List<FriendBalance>,
    modifier: Modifier,
    onBack: () -> Unit,
    onSave: (List<FriendBalance>) -> Unit,
    onAssignContact: (FriendBalance) -> Unit
) {
    var editable by remember(friends) { mutableStateOf(friends) }
    var newName by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<FriendBalance?>(null) }
    var editingFriend by remember { mutableStateOf<FriendBalance?>(null) }
    var editFriendText by remember { mutableStateOf("") }
    fun persist(values: List<FriendBalance>) {
        editable = values.map { it.copy(name = it.name.trim()) }.filter { it.name.isNotEmpty() }
        onSave(editable)
    }
    Column(Modifier.fillMaxSize().then(modifier)) {
        SettingsSubHeader("Friends", Modifier.padding(horizontal = 20.dp, vertical = 12.dp), onBack = onBack, onDone = onBack)
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
        item { Text("People you split expenses with", color = Muted, fontSize = 14.sp) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                AppTextInput(
                    value = newName,
                    onValueChange = { newName = it.take(25) },
                    placeholder = "Friend's name",
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(10.dp))
                Button(
                    onClick = {
                        val name = newName.trim()
                        if (name.isNotEmpty() && editable.none { it.name.equals(name, true) }) {
                            persist(editable + Friend(UUID.randomUUID().toString(), name, System.currentTimeMillis()))
                            newName = ""
                        }
                    },
                    modifier = Modifier.height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Green)
                ) {
                    Text("Add", fontWeight = FontWeight.SemiBold)
                }
            }
        }
        itemsIndexed(editable, key = { _, friend -> friend.id }) { index, friend ->
            SwipeManagementItem(
                onEditRequested = { editingFriend = friend; editFriendText = friend.name },
                onDeleteRequested = { pendingDelete = friend }
            ) {
                Row(
                    Modifier.fillMaxWidth().background(CardSurface, RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(friend.name, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        friend.phoneNumber?.takeIf { it.isNotBlank() }?.let {
                            Text(PhoneUtils.formatDisplay(it), color = Muted, fontSize = 12.sp)
                        }
                    }
                    if (friend.contactId == null) {
                        Box(
                            modifier = Modifier
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onAssignContact(friend) }
                                .padding(horizontal = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Assign contact",
                                color = Green,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .background(Color(0xFFEAF4EE), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    } else {
                        Text("Assigned", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp))
                    }
                }
            }
        }
        }
    }
    pendingDelete?.let { friend ->
        ConfirmDeleteDialog(
            title = "Remove friend?",
            message = "${friend.name} and their local balance will be removed. Shared group membership and shared history remain. This cannot be undone.",
            confirmLabel = "Remove",
            onConfirm = { persist(editable.filterNot { it.id == friend.id }); pendingDelete = null },
            onDismiss = { pendingDelete = null }
        )
    }
    editingFriend?.let { friend ->
        EditNameDialog(
            title = "Edit friend",
            value = editFriendText,
            onValueChange = { editFriendText = it.take(25) },
            onDismiss = { editingFriend = null },
            onSave = {
                val name = editFriendText.trim()
                if (name.isNotEmpty() && editable.none { it.id != friend.id && it.name.equals(name, true) }) {
                    persist(editable.map { if (it.id == friend.id) it.copy(name = name) else it })
                }
                editingFriend = null
            }
        )
    }
}

@Composable
private fun EditNameDialog(title: String, value: String, onValueChange: (String) -> Unit, onDismiss: () -> Unit, onSave: () -> Unit) {
    AppInputDialog(
        title = title,
        initialValue = value,
        placeholder = "Friend's name",
        confirmLabel = "Save",
        onConfirm = {
            onValueChange(it)
            onSave()
        },
        onDismiss = onDismiss
    )
}

@Composable
private fun SwipeManagementItem(
    onEditRequested: () -> Unit,
    onDeleteRequested: () -> Unit,
    canDelete: Boolean = true,
    content: @Composable () -> Unit
) = AppSwipeItem(
    shape = RoundedCornerShape(16.dp),
    onLeftSwipe = if (canDelete) onDeleteRequested else null,
    leftLabel = "Delete",
    leftIcon = Icons.Outlined.Delete,
    leftColor = Danger,
    onRightSwipe = onEditRequested,
    rightLabel = "Edit",
    rightIcon = Icons.Outlined.Edit,
    rightColor = Green,
    threshold = 0.28f,
    content = content
)

