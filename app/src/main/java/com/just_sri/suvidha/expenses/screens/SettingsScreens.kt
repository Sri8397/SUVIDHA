package com.just_sri.suvidha.expenses

import com.just_sri.suvidha.expenses.components.AppListRow
import com.just_sri.suvidha.expenses.components.AppListStyle
import com.just_sri.suvidha.expenses.components.AppSectionHeader
import com.just_sri.suvidha.expenses.components.AppTopBar
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
    LazyColumn(
        Modifier.fillMaxSize().then(modifier),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(AppListStyle.Spacing)
    ) {
        item { ScreenHeader("SETTINGS", "Preferences") }
        item { SettingsRow("Currency", currency, icon = "¤", onClick = { onOpen("currency") }) }
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
        item { SettingsRow("Timezone", tzSubtitle, icon = "🌐", onClick = { onOpen("timezone") }) }
        item { SettingsRow("Categories", "$categoryCount expense categories", icon = "C", onClick = { onOpen("categories") }) }
        item { SettingsRow("Friends", "$friendCount friends", icon = "F", onClick = { onOpen("friends") }) }
        item { SettingsRow("Encrypted backup", "Manual file recovery", icon = "B", onClick = { onOpen("backup") }) }
    }
}

@Composable
private fun SettingsRow(title: String, subtitle: String, icon: String = title.take(1), onClick: () -> Unit) {
    AppListRow(title = title, subtitle = subtitle, leadingText = icon, onClick = onClick)
}

@Composable
private fun SettingsSubHeader(title: String, modifier: Modifier = Modifier, onBack: () -> Unit, onDone: (() -> Unit)? = null) {
    AppTopBar(title, onBack, modifier, horizontalPadding = 0.dp, verticalPadding = 0.dp, actions = {
        if (onDone != null) IconButton(onClick = onDone) {
            Icon(Icons.Outlined.Check, contentDescription = "Done", tint = Green)
        }
    })
}

@Composable
internal fun CurrencyManagementPage(current: String, modifier: Modifier, onBack: () -> Unit, onSave: (String) -> Unit) {
    Column(Modifier.fillMaxSize().then(modifier)) {
        SettingsSubHeader("Currency", Modifier.padding(horizontal = 20.dp, vertical = 12.dp), onBack = onBack)
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(AppListStyle.Spacing),
        ) {
            item { Text("Select your currency", color = Muted, fontSize = 14.sp) }
            items(supportedCurrencies, key = { it.first }) { (symbol, label) ->
                AppListRow(
                    title = label.substringAfter("  ", label),
                    leadingText = symbol,
                    onClick = { onSave(symbol) },
                    trailing = { if (symbol == current) Icon(Icons.Outlined.Check, contentDescription = "Selected", tint = Green) },
                )
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
        verticalArrangement = Arrangement.spacedBy(AppListStyle.Spacing)
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
                AppListRow(
                    title = "Automatic",
                    subtitle = "${defaultDeviceZone.id.replace('_', ' ')} · $defaultDeviceOffset",
                    leadingText = "A",
                    onClick = { onSave("AUTO") },
                    trailing = { if (isSelected) Icon(Icons.Outlined.Check, contentDescription = "Selected", tint = Green) },
                )
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
                AppListRow(
                    title = option.city,
                    subtitle = "${option.continent} · ${option.offsetString}",
                    leadingText = option.continent.take(2).uppercase(),
                    onClick = { onSave(option.id) },
                    trailing = { if (isSelected) Icon(Icons.Outlined.Check, contentDescription = "Selected", tint = Green) },
                )
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
            verticalArrangement = Arrangement.spacedBy(AppListStyle.Spacing)
        ) {
        item { AppSectionHeader("Expense categories", editable.size) }
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
                AppListRow(
                    title = category,
                    subtitle = if (category.equals("Other", true)) "Default category" else null,
                    onClick = { editingCategory = category; editCategoryText = category },
                )
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
            verticalArrangement = Arrangement.spacedBy(AppListStyle.Spacing)
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
                AppListRow(
                    title = friend.name,
                    subtitle = friend.phoneNumber?.takeIf { it.isNotBlank() }?.let { PhoneUtils.formatDisplay(it) }
                        ?: if (friend.contactId != null) "Contact linked" else "No contact linked",
                    trailing = {
                        if (friend.contactId == null) TextButton(onClick = { onAssignContact(friend) }) { Text("Link", color = Green) }
                        else Icon(Icons.Outlined.Check, contentDescription = "Contact linked", tint = Green, modifier = Modifier.size(18.dp))
                    },
                    onClick = { editingFriend = friend; editFriendText = friend.name },
                )
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

