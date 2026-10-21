package com.just_sri.suvidha.expenses

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.UUID

class MainActivity : ComponentActivity() {
    private val dao by lazy { ExpenseDatabase.get(this).expenses() }
    private val friendDao by lazy { ExpenseDatabase.get(this).friendBalances() }
    private val prefs by lazy { getSharedPreferences("expense_settings", Context.MODE_PRIVATE) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Green, background = Canvas, surface = Color.White)) {
                val expenses by dao.observeAll().collectAsStateWithLifecycle(emptyList())
                val friends by friendDao.observeAll().collectAsStateWithLifecycle(emptyList())
                var categories by remember { mutableStateOf(prefs.getString("categories", null)?.split("|")?.filter { it.isNotBlank() } ?: defaultExpenseCategories) }
                ExpenseHome(expenses = expenses, friends = friends, categories = categories, onCategoriesChange = {
                    categories = (it.filter { name -> name.isNotBlank() && !name.equals("Other", true) } + "Other").distinct()
                    prefs.edit().putString("categories", categories.joinToString("|")).apply()
                }, onFriendsChange = { updated -> lifecycleScope.launch {
                    val ids = updated.map { it.id }.toSet()
                    friends.filter { it.id !in ids }.forEach { friendDao.delete(it.id) }
                    updated.forEach { friendDao.save(it) }
                } }, onDeleteFriend = { id -> lifecycleScope.launch { friendDao.delete(id) } },
                onAdd = { drafts ->
                    lifecycleScope.launch { dao.insertAll(drafts.map { draft -> Expense(UUID.randomUUID().toString(), draft.category, draft.amount.toDouble(), draft.note.ifBlank { draft.category }, System.currentTimeMillis()) }) }
                }, onDelete = { id -> lifecycleScope.launch { dao.delete(id) } })
            }
        }
    }
}

@Composable
private fun ExpenseHome(
    expenses: List<Expense>, friends: List<FriendBalance>, categories: List<String>,
    onCategoriesChange: (List<String>) -> Unit,
    onFriendsChange: (List<FriendBalance>) -> Unit, onDeleteFriend: (String) -> Unit,
    onAdd: (List<ExpenseDraft>) -> Unit,
    onDelete: (String) -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }
    var settingsPage by remember { mutableStateOf<String?>(null) }
    var showAddExpense by remember { mutableStateOf(false) }
    var showSplitForm by remember { mutableStateOf(false) }
    var selectedSplitFriend by remember { mutableStateOf<String?>(null) }
    Scaffold(
        containerColor = Canvas,
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                val itemColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Green,
                    selectedTextColor = Green,
                    indicatorColor = Color(0xFFE5F2EA)
                )
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.AutoMirrored.Outlined.List, contentDescription = "Expenses", modifier = Modifier.size(36.dp)) },
                    colors = itemColors
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(painterResource(R.drawable.ic_payment_split), contentDescription = "Split", modifier = Modifier.size(36.dp)) },
                    colors = itemColors
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    icon = { Icon(Icons.Outlined.Settings, contentDescription = "Settings", modifier = Modifier.size(36.dp)) },
                    colors = itemColors
                )
            }
        },
        floatingActionButton = {
            if (tab == 0 || tab == 1) SmallFloatingActionButton(
                onClick = {
                    if (tab == 0) showAddExpense = true
                    else if (friends.isEmpty()) { tab = 2; settingsPage = "friends" }
                    else { selectedSplitFriend = null; showSplitForm = true }
                },
                containerColor = Green,
                contentColor = Color.White,
                shape = CircleShape
            ) { Text("+", fontSize = 26.sp, fontWeight = FontWeight.Normal) }
        }
    ) { padding ->
        when (tab) {
            0 -> ExpensesPage(expenses, categories, Modifier.padding(padding), onDelete)
            1 -> SplitPage(friends, Modifier.padding(padding), onDeleteFriend = onDeleteFriend, onAddSplit = { friendId -> selectedSplitFriend = friendId; showSplitForm = true })
            else -> when (settingsPage) {
                "categories" -> CategoryManagementPage(categories, Modifier.padding(padding), onBack = { settingsPage = null }, onSave = onCategoriesChange)
                "friends" -> FriendsManagementPage(friends, Modifier.padding(padding), onBack = { settingsPage = null }, onSave = onFriendsChange)
                else -> SettingsMenu(categories.size, friends.size, Modifier.padding(padding), onOpen = { settingsPage = it })
            }
        }
    }
    BackHandler(enabled = tab == 2 && settingsPage != null) { settingsPage = null }
    if (showAddExpense) AddExpenseDialog(categories, { showAddExpense = false }) { drafts ->
        onAdd(drafts); showAddExpense = false
    }
    if (showSplitForm) SplitEntryDialog(friends, selectedSplitFriend, onDismiss = { showSplitForm = false }) { entries ->
        val updated = friends.associateBy { it.id }.toMutableMap()
        entries.forEach { entry ->
            val friend = updated[entry.friendId] ?: return@forEach
            val entered = entry.amount.toDouble()
            updated[friend.id] = friend.copy(amount = friend.amount + entered, updatedAt = System.currentTimeMillis())
        }
        onFriendsChange(updated.values.toList())
        showSplitForm = false
    }
}

