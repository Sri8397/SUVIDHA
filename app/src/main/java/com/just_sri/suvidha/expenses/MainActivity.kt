package com.just_sri.suvidha.expenses

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.outlined.Person
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
import com.just_sri.suvidha.expenses.screens.ExpenseFlowScreen
import com.just_sri.suvidha.expenses.screens.BackupManagementPage
import com.just_sri.suvidha.expenses.screens.SharedGroupsPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZoneId

class MainActivity : ComponentActivity() {
    private val prefs by lazy { getSharedPreferences("expense_settings", Context.MODE_PRIVATE) }
    private var currentIntent by mutableStateOf<Intent?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        currentIntent = intent
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentIntent = intent
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

        SyncNotificationHelper.ensureChannel(this)

        setContent {
            var databaseState by remember {
                mutableStateOf<DatabaseStartupState>(DatabaseStartupState.Loading)
            }
            val scope = rememberCoroutineScope()
            val openDatabase: (Boolean) -> Unit = { deleteExisting ->
                databaseState = DatabaseStartupState.Loading
                scope.launch {
                    val result = withContext(Dispatchers.IO) {
                        runCatching {
                            if (deleteExisting) {
                                ExpenseDatabase.deleteAndCreateFresh(this@MainActivity)
                            } else {
                                ExpenseDatabase.get(this@MainActivity)
                            }
                        }
                    }
                    databaseState = result.fold(
                        onSuccess = { DatabaseStartupState.Ready(it) },
                        onFailure = { DatabaseStartupState.Failed(it) }
                    )
                }
            }

            LaunchedEffect(Unit) { openDatabase(false) }

            when (val state = databaseState) {
                DatabaseStartupState.Loading -> DatabaseRecoveryScreen(loading = true)
                is DatabaseStartupState.Failed -> DatabaseRecoveryScreen(
                    loading = false,
                    onRetry = { openDatabase(false) },
                    onDeleteAndContinue = { openDatabase(true) }
                )
                is DatabaseStartupState.Ready -> {
                val database = state.database
                val repository = remember(database) { ExpenseRepository(database) }
                val splitDao = remember(database) { database.splitTransactions() }

                val expenses by repository.observeExpenses().collectAsStateWithLifecycle(emptyList())
                val friends by repository.observeFriendsWithBalances().collectAsStateWithLifecycle(emptyList())

                var categories by remember {
                    mutableStateOf(
                        prefs.getString("categories", null)?.split("|")?.filter { it.isNotBlank() }
                            ?: defaultExpenseCategories
                    )
                }
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
                        val activeIntent = currentIntent ?: intent
                        val initialTab = remember(activeIntent) {
                            if (activeIntent?.getStringExtra("navigate_to") == "shared_groups") 2 else 0
                        }
                        ExpenseHome(
                            initialTab = initialTab,
                            database = database,
                            expenses = expenses,
                            friends = friends,
                            categories = categories,
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
                            onFriendsChange = { updated ->
                                scope.launch {
                                    repository.replaceFriends(friends, updated)
                                }
                            },
                            onAssignContact = { friend, contactId, number ->
                                scope.launch {
                                    val linkedFriend = friend.copy(contactId = contactId, phoneNumber = number)
                                    repository.saveFriend(linkedFriend)
                                    Toast.makeText(this@MainActivity, "Contact linked to ${friend.name}.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onDeleteFriend = { id ->
                                scope.launch {
                                    repository.deleteFriend(id)
                                }
                            },
                            onAdd = { draft ->
                                scope.launch {
                                    repository.addExpense(draft)
                                }
                            },
                            onDelete = { id ->
                                scope.launch {
                                    repository.deleteExpense(id)
                                }
                            },
                            onSaveSplitTransactions = { entries, friendsMap ->
                                scope.launch {
                                    repository.saveSplitTransactions(entries, friendsMap, friends)
                                }
                            },
                            onDeleteSplitTransaction = { txn ->
                                scope.launch {
                                    repository.deleteSplitTransaction(txn, friends)
                                }
                            },
                            onSettleSplitTransaction = { txn ->
                                scope.launch {
                                    val friendName = friends.firstOrNull { it.id == txn.friendId }?.name ?: "Friend"
                                    repository.settleSplitTransaction(txn, friendName, friends)
                                }
                            }
                        )
                    }
                }
                }
            }
        }
    }
}

private sealed class DatabaseStartupState {
    data object Loading : DatabaseStartupState()
    data class Ready(val database: ExpenseDatabase) : DatabaseStartupState()
    data class Failed(val error: Throwable) : DatabaseStartupState()
}

@Composable
private fun DatabaseRecoveryScreen(
    loading: Boolean,
    onRetry: () -> Unit = {},
    onDeleteAndContinue: () -> Unit = {}
) {
    var confirmDeletion by remember { mutableStateOf(false) }

    MaterialTheme(colorScheme = lightColorScheme(primary = Green, background = Canvas, surface = CardSurface)) {
        Surface(modifier = Modifier.fillMaxSize(), color = Canvas) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (loading) {
                    CircularProgressIndicator()
                    Text("Opening Suvidha…", modifier = Modifier.padding(top = 20.dp), color = Ink)
                } else {
                    Text("Couldn't open your database", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text(
                        "You can retry creating a backup. If that still fails, delete the old database and continue with a fresh one.",
                        modifier = Modifier.padding(top = 12.dp),
                        color = Ink.copy(alpha = 0.72f)
                    )
                    Button(onClick = onRetry, modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
                        Text("Retry")
                    }
                    OutlinedButton(
                        onClick = { confirmDeletion = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Delete old data and continue")
                    }
                }
            }
        }

        if (confirmDeletion) {
            AlertDialog(
                onDismissRequest = { confirmDeletion = false },
                title = { Text("Delete existing data?") },
                text = { Text("The old database will be permanently deleted. If the backup failed, this data cannot be recovered.") },
                confirmButton = {
                    Button(onClick = {
                        confirmDeletion = false
                        onDeleteAndContinue()
                    }) {
                        Text("Delete and continue")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { confirmDeletion = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun ExpenseHome(
    initialTab: Int = 0,
    database: ExpenseDatabase,
    expenses: List<Expense>,
    friends: List<FriendBalance>,
    categories: List<String>,
    currency: String,
    timezoneId: String,
    splitDao: SplitTransactionDao,
    onCategoriesChange: (List<String>) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onTimezoneChange: (String) -> Unit,
    onFriendsChange: (List<FriendBalance>) -> Unit,
    onDeleteFriend: (String) -> Unit,
    onAssignContact: (FriendBalance, String, String) -> Unit,
    onAdd: (ExpenseDraft) -> Unit,
    onDelete: (String) -> Unit,
    onSaveSplitTransactions: (List<SplitDraft>, Map<String, FriendBalance>) -> Unit,
    onDeleteSplitTransaction: (SplitTransaction) -> Unit,
    onSettleSplitTransaction: (SplitTransaction) -> Unit
) {
    var tab by remember(initialTab) { mutableIntStateOf(initialTab) }
    var settingsPage by remember { mutableStateOf<String?>(null) }
    var showAddExpense by remember { mutableStateOf(false) }
    var showSplitForm by remember { mutableStateOf(false) }
    var showContactPicker by remember { mutableStateOf(false) }
    var friendToLinkContactId by remember { mutableStateOf<String?>(null) }
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
    } else if (friendToLinkContactId != null) {
        val friendToLink = friends.firstOrNull { it.id == friendToLinkContactId }
        if (friendToLink != null) {
            ContactsScreen(
                existingFriendIds = friends.map { it.id }.toSet(),
                assignedContactIds = friends.mapNotNull { it.contactId }.toSet(),
                friendToLink = friendToLink,
                onBack = { friendToLinkContactId = null },
                onLinkContact = { targetFriend, contactId, number ->
                    val alreadyAssigned = friends.any { it.contactId == contactId }
                    if (targetFriend.contactId == null && !alreadyAssigned) {
                        onAssignContact(targetFriend, contactId, number)
                    }
                    friendToLinkContactId = null
                }
            )
        }
    } else if (showContactPicker) {
        ContactsScreen(
            existingFriendIds = friends.map { it.id }.toSet(),
            onBack = { showContactPicker = false },
            onAddFriend = { id, name, number ->
                if (friends.none { it.id == id }) {
                    val contactId = id.removePrefix("phone_contact_")
                    onFriendsChange(friends + Friend(id, name, System.currentTimeMillis(), number, contactId))
                }
            }
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
                                    icon = { Icon(painterResource(R.drawable.ic_community), contentDescription = "Friends", modifier = Modifier.size(33.dp)) },
                                    colors = itemColors
                                )
                                NavigationBarItem(
                                    selected = tab == 2,
                                    onClick = { tab = 2 },
                                    icon = { Icon(Icons.Outlined.Person, contentDescription = "Groups", modifier = Modifier.size(26.dp)) },
                                    colors = itemColors
                                )
                                NavigationBarItem(
                                    selected = tab == 3,
                                    onClick = { tab = 3 },
                                    icon = { Icon(painterResource(R.drawable.ic_settings), contentDescription = "Settings", modifier = Modifier.size(26.dp)) },
                                    colors = itemColors
                                )
                            }
                        }
                    }
                }
            },
            floatingActionButton = {
                if ((tab == 0 || tab == 1) && viewingFriend == null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (tab == 1 && viewingFriend == null) {
                            SmallFloatingActionButton(
                                onClick = { showContactPicker = true },
                                containerColor = CardSurface,
                                contentColor = Green,
                                shape = CircleShape
                            ) {
                                Icon(Icons.Outlined.Person, contentDescription = "Add from contacts", modifier = Modifier.size(22.dp))
                            }
                        }
                        if (tab == 0) SmallFloatingActionButton(
                            onClick = { showAddExpense = true },
                            containerColor = Green,
                            contentColor = OffWhite,
                            shape = CircleShape
                        ) { Text("+", fontSize = 26.sp, fontWeight = FontWeight.Normal) }
                    }
                }
            }
        ) { padding ->
            val currentViewingFriend = viewingFriend
            val scrollModifier = Modifier.padding(top = padding.calculateTopPadding())
            when {
                currentViewingFriend != null -> {
                    val liveFriend = friends.firstOrNull { it.id == currentViewingFriend.id } ?: currentViewingFriend
                    val transactions by splitDao.observeByFriend(liveFriend.id).collectAsStateWithLifecycle(emptyList())
                    FriendTransactionsPage(
                        friend = liveFriend,
                        transactions = transactions,
                        modifier = Modifier.padding(padding),
                        onBack = { viewingFriend = null },
                        onAddSplit = { friendId -> selectedSplitFriend = friendId; showSplitForm = true }
                    )
                }
                tab == 0 -> ExpensesPage(expenses, categories, scrollModifier, onDelete)
                tab == 1 -> SplitPage(
                    friends = friends,
                    modifier = scrollModifier,
                    onDeleteFriend = onDeleteFriend,
                    onAddSplit = { friendId -> selectedSplitFriend = friendId; showSplitForm = true },
                    onViewFriend = { friend -> viewingFriend = friend }
                )
                tab == 2 -> SharedGroupsPage(database, Modifier.padding(padding))
                else -> when (settingsPage) {
                    "backup" -> BackupManagementPage(database, Modifier.padding(padding), onBack = { settingsPage = null })
                    "currency" -> CurrencyManagementPage(currency, Modifier.padding(padding), onBack = { settingsPage = null }, onSave = { onCurrencyChange(it); settingsPage = null })
                    "timezone" -> TimezoneManagementPage(timezoneId, Modifier.padding(padding), onBack = { settingsPage = null }, onSave = { onTimezoneChange(it); settingsPage = null })
                    "categories" -> CategoryManagementPage(categories, Modifier.padding(padding), onBack = { settingsPage = null }, onSave = onCategoriesChange)
                    "friends" -> FriendsManagementPage(
                        friends,
                        Modifier.padding(padding),
                        onBack = { settingsPage = null },
                        onSave = onFriendsChange,
                        onAssignContact = { friend ->
                            if (friend.contactId == null) friendToLinkContactId = friend.id
                        }
                    )
                    else -> SettingsMenu(categories.size, friends.size, currency, timezoneId, Modifier.padding(padding), onOpen = { settingsPage = it })
                }
            }
        }
        BackHandler(enabled = viewingFriend != null) { viewingFriend = null }
        BackHandler(enabled = showContactPicker) { showContactPicker = false }
        BackHandler(enabled = tab == 3 && settingsPage != null) { settingsPage = null }
    }
}
