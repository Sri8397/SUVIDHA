package com.just_sri.suvidha.expenses

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.Alignment
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
import java.time.ZoneId
import java.util.UUID

class MainActivity : ComponentActivity() {
    private val dao by lazy { ExpenseDatabase.get(this).expenses() }
    private val friendDao by lazy { ExpenseDatabase.get(this).friendBalances() }
    private val splitDao by lazy { ExpenseDatabase.get(this).splitTransactions() }
    private val prefs by lazy { getSharedPreferences("expense_settings", Context.MODE_PRIVATE) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        setContent {
            val expenses by dao.observeAll().collectAsStateWithLifecycle(emptyList())
            val friends by friendDao.observeAll().collectAsStateWithLifecycle(emptyList())
            var categories by remember { mutableStateOf(prefs.getString("categories", null)?.split("|")?.filter { it.isNotBlank() } ?: defaultExpenseCategories) }
            var currency by remember { mutableStateOf(prefs.getString("currency", "₹") ?: "₹") }
            var timezoneId by remember { mutableStateOf(prefs.getString("timezone_id", "AUTO") ?: "AUTO") }
            val effectiveZoneId = remember(timezoneId) {
                if (timezoneId == "AUTO") {
                    ZoneId.systemDefault()
                } else {
                    try {
                        ZoneId.of(timezoneId)
                    } catch (_: Exception) {
                        ZoneId.systemDefault()
                    }
                }
            }
            CompositionLocalProvider(
                LocalCurrency provides currency,
                LocalZoneId provides effectiveZoneId
            ) {
                MaterialTheme(colorScheme = lightColorScheme(primary = Green, background = Canvas, surface = CardSurface)) {
                    ExpenseHome(
                        expenses = expenses, friends = friends, categories = categories,
                        currency = currency,
                        timezoneId = timezoneId,
                        splitDao = splitDao,
                        onCategoriesChange = {
                            categories = (it.filter { name -> name.isNotBlank() && !name.equals("Other", true) } + "Other").distinct()
                            prefs.edit().putString("categories", categories.joinToString("|")).apply()
                        },
                        onCurrencyChange = { sym ->
                            currency = sym
                            prefs.edit().putString("currency", sym).apply()
                        },
                        onTimezoneChange = { tz ->
                            timezoneId = tz
                            prefs.edit().putString("timezone_id", tz).apply()
                        },
                        onFriendsChange = { updated -> lifecycleScope.launch {
                            val ids = updated.map { it.id }.toSet()
                            friends.filter { it.id !in ids }.forEach { friendDao.delete(it.id) }
                            updated.forEach { friendDao.save(it) }
                        } },
                        onDeleteFriend = { id -> lifecycleScope.launch { friendDao.delete(id) } },
                        onAdd = { draft ->
                            lifecycleScope.launch {
                                dao.insert(
                                    Expense(
                                        UUID.randomUUID().toString(),
                                        draft.category,
                                        draft.amount.toDouble(),
                                        draft.note.ifBlank { draft.category },
                                        System.currentTimeMillis()
                                    )
                                )
                            }
                        },
                        onDelete = { id -> lifecycleScope.launch { dao.delete(id) } },
                        onSaveSplitTransactions = { entries, friendsMap ->
                            lifecycleScope.launch {
                                val txns = entries.map { entry ->
                                    SplitTransaction(UUID.randomUUID().toString(), entry.friendId, entry.amount.toDouble(), entry.note.trim(), System.currentTimeMillis())
                                }
                                splitDao.insertAll(txns)
                                val updated = friendsMap.toMutableMap()
                                entries.forEach { entry ->
                                    val friend = updated[entry.friendId] ?: return@forEach
                                    updated[friend.id] = friend.copy(amount = friend.amount + entry.amount.toDouble(), updatedAt = System.currentTimeMillis())
                                }
                                updated.values.forEach { friendDao.save(it) }
                            }
                        },
                        onDeleteSplitTransaction = { txn ->
                            lifecycleScope.launch {
                                splitDao.delete(txn.id)
                                friends.firstOrNull { it.id == txn.friendId }?.let { friend ->
                                    friendDao.save(friend.copy(amount = friend.amount - txn.amount, updatedAt = System.currentTimeMillis()))
                                }
                            }
                        },
                        onSettleSplitTransaction = { txn ->
                            lifecycleScope.launch {
                                val now = System.currentTimeMillis()
                                val friend = friends.firstOrNull { it.id == txn.friendId }
                                val friendName = friend?.name ?: "Friend"

                                splitDao.save(txn.copy(isSettled = true, settledAt = now))
                                if (friend != null) {
                                    friendDao.save(friend.copy(amount = friend.amount - txn.amount, updatedAt = now))
                                }

                                if (txn.amount != 0.0) {
                                    val isOwed = txn.amount > 0
                                    dao.insert(
                                        Expense(
                                            id = UUID.randomUUID().toString(),
                                            category = if (isOwed) "Earn" else "Other",
                                            amount = kotlin.math.abs(txn.amount),
                                            description = "Settled ${if (isOwed) "by" else "to"} $friendName: ${txn.note.ifBlank { "Split" }}",
                                            date = now
                                        )
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpenseHome(
    expenses: List<Expense>, friends: List<FriendBalance>, categories: List<String>,
    currency: String,
    timezoneId: String,
    splitDao: SplitTransactionDao,
    onCategoriesChange: (List<String>) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onTimezoneChange: (String) -> Unit,
    onFriendsChange: (List<FriendBalance>) -> Unit, onDeleteFriend: (String) -> Unit,
    onAdd: (ExpenseDraft) -> Unit,
    onDelete: (String) -> Unit,
    onSaveSplitTransactions: (List<SplitDraft>, Map<String, FriendBalance>) -> Unit,
    onDeleteSplitTransaction: (SplitTransaction) -> Unit,
    onSettleSplitTransaction: (SplitTransaction) -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }
    var settingsPage by remember { mutableStateOf<String?>(null) }
    var showAddExpense by remember { mutableStateOf(false) }
    var showSplitForm by remember { mutableStateOf(false) }
    var selectedSplitFriend by remember { mutableStateOf<String?>(null) }
    var viewingFriend by remember { mutableStateOf<FriendBalance?>(null) }
    if (showSplitForm) {
        SplitFlowScreen(
            friends = friends,
            initialFriendId = selectedSplitFriend,
            onBack = { showSplitForm = false },
            onSave = { entries ->
                onSaveSplitTransactions(entries, friends.associateBy { it.id })
                showSplitForm = false
            },
            onSaveAndNew = { entries ->
                onSaveSplitTransactions(entries, friends.associateBy { it.id })
            }
        )
    } else if (showAddExpense) {
        ExpenseFlowScreen(
            categories = categories,
            onBack = { showAddExpense = false },
            onSave = { draft ->
                onAdd(draft)
                showAddExpense = false
            },
            onSaveAndNew = { draft -> onAdd(draft) }
        )
    } else {
        Scaffold(
            containerColor = Canvas,
            contentWindowInsets = WindowInsets.statusBars,
            bottomBar = {
                if (viewingFriend == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(32.dp),
                            color = CardSurface.copy(alpha = 0.92f),
                            border = BorderStroke(1.dp, BorderLight),
                            shadowElevation = 8.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            NavigationBar(
                                containerColor = Color.Transparent,
                                tonalElevation = 0.dp,
                                windowInsets = WindowInsets(0, 0, 0, 0),
                                modifier = Modifier.height(64.dp)
                            ) {
                                val itemColors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Green,
                                    selectedTextColor = Green,
                                    indicatorColor = Color(0xFFE5F2EA),
                                    unselectedIconColor = Ink.copy(alpha = 0.6f)
                                )
                                NavigationBarItem(
                                    selected = tab == 0,
                                    onClick = { tab = 0 },
                                    icon = { Icon(painterResource(R.drawable.ic_analytics), contentDescription = "Expenses", modifier = Modifier.size(32.dp)) },
                                    colors = itemColors
                                )
                                NavigationBarItem(
                                    selected = tab == 1,
                                    onClick = { tab = 1 },
                                    icon = { Icon(painterResource(R.drawable.ic_community), contentDescription = "Split", modifier = Modifier.size(33.dp)) },
                                    colors = itemColors
                                )
                                NavigationBarItem(
                                    selected = tab == 2,
                                    onClick = { tab = 2 },
                                    icon = { Icon(painterResource(R.drawable.ic_settings), contentDescription = "Settings", modifier = Modifier.size(26.dp)) },
                                    colors = itemColors
                                )
                            }
                        }
                    }
                }
            },
            floatingActionButton = {
                if (tab == 0 || tab == 1 || viewingFriend != null) SmallFloatingActionButton(
                    onClick = {
                        val vf = viewingFriend
                        if (vf != null) { selectedSplitFriend = vf.id; showSplitForm = true }
                        else if (tab == 0) showAddExpense = true
                        else if (friends.isEmpty()) { tab = 2; settingsPage = "friends" }
                        else { selectedSplitFriend = null; showSplitForm = true }
                    },
                    containerColor = Green,
                    contentColor = OffWhite,
                    shape = CircleShape
                ) { Text("+", fontSize = 26.sp, fontWeight = FontWeight.Normal) }
            }
        ) { padding ->
            val currentViewingFriend = viewingFriend
            val scrollModifier = Modifier.padding(top = padding.calculateTopPadding())
            when {
                currentViewingFriend != null -> {
                    // Keep the friend object in sync with the latest from friends list
                    val liveFriend = friends.firstOrNull { it.id == currentViewingFriend.id } ?: currentViewingFriend
                    val transactions by splitDao.observeByFriend(liveFriend.id).collectAsStateWithLifecycle(emptyList())
                    FriendTransactionsPage(
                        friend = liveFriend,
                        transactions = transactions,
                        modifier = Modifier.padding(padding),
                        onBack = { viewingFriend = null },
                        onAddSplit = { friendId -> selectedSplitFriend = friendId; showSplitForm = true },
                        onDeleteTransaction = onDeleteSplitTransaction,
                        onSettleTransaction = onSettleSplitTransaction
                    )
                }
                tab == 0 -> ExpensesPage(expenses, categories, scrollModifier, onDelete)
                tab == 1 -> SplitPage(friends, scrollModifier, onDeleteFriend = onDeleteFriend, onAddSplit = { friendId -> selectedSplitFriend = friendId; showSplitForm = true }, onViewFriend = { friend -> viewingFriend = friend })
                else -> when (settingsPage) {
                    "currency" -> CurrencyManagementPage(currency, Modifier.padding(padding), onBack = { settingsPage = null }, onSave = { onCurrencyChange(it); settingsPage = null })
                    "timezone" -> TimezoneManagementPage(timezoneId, Modifier.padding(padding), onBack = { settingsPage = null }, onSave = { onTimezoneChange(it); settingsPage = null })
                    "categories" -> CategoryManagementPage(categories, Modifier.padding(padding), onBack = { settingsPage = null }, onSave = onCategoriesChange)
                    "friends" -> FriendsManagementPage(friends, Modifier.padding(padding), onBack = { settingsPage = null }, onSave = onFriendsChange)
                    else -> SettingsMenu(categories.size, friends.size, currency, timezoneId, Modifier.padding(padding), onOpen = { settingsPage = it })
                }
            }
        }
        BackHandler(enabled = viewingFriend != null) { viewingFriend = null }
        BackHandler(enabled = tab == 2 && settingsPage != null) { settingsPage = null }
    }
}

