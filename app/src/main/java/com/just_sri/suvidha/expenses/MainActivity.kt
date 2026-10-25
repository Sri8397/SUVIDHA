package com.just_sri.suvidha.expenses

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Person
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
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.time.ZoneId
import java.util.UUID

class MainActivity : ComponentActivity() {
    private val sessionPrefs by lazy { getSharedPreferences("app_session", Context.MODE_PRIVATE) }
    private val offlinePrefs by lazy { getSharedPreferences("expense_settings", Context.MODE_PRIVATE) }
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
        FirebaseApp.initializeApp(this)
        setContent {
            val firebaseConfigured = remember { FirebaseApp.getApps(this@MainActivity).isNotEmpty() }
            if (!firebaseConfigured) {
                FirebaseSetupScreen()
            } else {
                val auth = remember { FirebaseAuth.getInstance() }
                var user by remember { mutableStateOf(auth.currentUser) }
                var continueOffline by remember {
                    mutableStateOf(sessionPrefs.getBoolean("continue_offline", offlinePrefs.getBoolean("continue_offline", false)))
                }

                DisposableEffect(auth) {
                    val listener = FirebaseAuth.AuthStateListener { user = it.currentUser }
                    auth.addAuthStateListener(listener)
                    onDispose { auth.removeAuthStateListener(listener) }
                }
                if (user == null && !continueOffline) {
                    PhoneSignInScreen(
                        auth = auth,
                        onContinueOffline = {
                            continueOffline = true
                            sessionPrefs.edit().putBoolean("continue_offline", true).apply()
                        }
                    )
                } else {
                    val accountUid = user?.uid
                    val database = remember(accountUid) { ExpenseDatabase.getForAccount(this@MainActivity, accountUid) }
                    val dao = remember(database) { database.expenses() }
                    val friendDao = remember(database) { database.friendBalances() }
                    val splitDao = remember(database) { database.splitTransactions() }
                    val firebaseSync = remember(database) { FirebaseSync(this@MainActivity, database) }
                    val prefs = remember(accountUid) {
                        if (accountUid == null) offlinePrefs
                        else getSharedPreferences("expense_settings_$accountUid", Context.MODE_PRIVATE)
                    }
                    DisposableEffect(firebaseSync, accountUid) {
                        if (accountUid != null) firebaseSync.start(accountUid, lifecycleScope)
                        else firebaseSync.stop()
                        onDispose { firebaseSync.stop() }
                    }
                    val expenses by dao.observeAll().collectAsStateWithLifecycle(emptyList())
                    val allSplits by splitDao.observeAll().collectAsStateWithLifecycle(emptyList())
                    val rawFriends by friendDao.observeAll().collectAsStateWithLifecycle(emptyList())
                    val friends = remember(rawFriends, allSplits) {
                        val unsettledByFriend = allSplits.filterNot { it.isSettled }.groupBy { it.friendId }
                        rawFriends.map { friend ->
                            val trueBalance = unsettledByFriend[friend.id]?.sumOf { it.amount } ?: 0.0
                            friend.withAmount(trueBalance)
                        }
                    }
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
                    val accountStatus = user?.phoneNumber?.ifBlank { "Signed In" } ?: if (user != null) "Signed In" else null

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
                                accountStatus = accountStatus,
                                onAccountAction = {
                                    if (user != null) {
                                        continueOffline = false
                                        sessionPrefs.edit().putBoolean("continue_offline", false).apply()
                                        auth.signOut()
                                    } else {
                                        continueOffline = false
                                        sessionPrefs.edit().putBoolean("continue_offline", false).apply()
                                    }
                                },
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
                                    friends.filter { it.id !in ids }.forEach {
                                        friendDao.delete(it.id)
                                        firebaseSync.deleteItem("friends", it.id)
                                        firebaseSync.deleteItem("friend_balances", it.id)
                                    }
                                    updated.forEach { friendDao.save(it) }
                                    firebaseSync.refreshSharedSplits()
                                } },
                                onAssignContact = { friend, contactId, number -> lifecycleScope.launch {
                                    val linkedFriend = friend.copy(contactId = contactId, phoneNumber = number)
                                    friendDao.save(linkedFriend)
                                    firebaseSync.refreshSharedSplits()
                                    val recipientUid = firebaseSync.findRegisteredUserId(number)
                                    if (recipientUid != null) {
                                        val oldTransactions = splitDao.observeByFriend(friend.id).first().filterNot { it.isShared }
                                        val allShared = oldTransactions.all { firebaseSync.shareSplitToContact(it, linkedFriend) }
                                        val message = if (allShared) "Ledger shared with ${friend.name}" else "Contact linked, but some ledger entries could not be shared"
                                        Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(this@MainActivity, "Contact isn't registered in Suvidha yet", Toast.LENGTH_SHORT).show()
                                    }
                                } },
                                onDeleteFriend = { id -> lifecycleScope.launch {
                                    friendDao.delete(id)
                                    firebaseSync.deleteItem("friends", id)
                                    firebaseSync.deleteItem("friend_balances", id)
                                } },
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
                                onDelete = { id -> lifecycleScope.launch {
                                    dao.delete(id)
                                    firebaseSync.deleteItem("expenses", id)
                                } },
                                onSaveSplitTransactions = { entries, friendsMap ->
                                    lifecycleScope.launch {
                                        val txns = entries.map { entry ->
                                            SplitTransaction(UUID.randomUUID().toString(), entry.friendId, entry.amount.toDouble(), entry.note.trim(), System.currentTimeMillis())
                                        }
                                        splitDao.insertAll(txns)
                                        val affectedFriendIds = entries.map { it.friendId }.distinct()
                                        val allCurrentSplits = splitDao.observeAll().first()
                                        affectedFriendIds.forEach { friendId ->
                                            val friend = friendsMap[friendId] ?: friends.firstOrNull { it.id == friendId }
                                            if (friend != null) {
                                                friendDao.save(friend.copy(updatedAt = System.currentTimeMillis()))
                                            }
                                        }
                                        txns.forEach { transaction ->
                                            val friend = friendsMap[transaction.friendId] ?: return@forEach
                                            if (friend.contactId != null && !friend.phoneNumber.isNullOrBlank()) {
                                                val shared = firebaseSync.shareSplitToContact(transaction, friend)
                                                val message = if (shared) {
                                                    "Split shared with ${friend.name}"
                                                } else {
                                                    "Saved locally. ${friend.name} isn't registered in Suvidha yet."
                                                }
                                                Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                },
                                onDeleteSplitTransaction = { txn ->
                                    lifecycleScope.launch {
                                        splitDao.delete(txn.id)
                                        firebaseSync.deleteItem("split_transactions", txn.id)
                                        if (!txn.isShared) firebaseSync.deleteSharedSplit(txn.id)
                                        val friend = friends.firstOrNull { it.id == txn.friendId }
                                        if (friend != null) {
                                            friendDao.save(friend.copy(updatedAt = System.currentTimeMillis()))
                                        }
                                    }
                                },
                                onSettleSplitTransaction = { txn ->
                                    lifecycleScope.launch {
                                        val now = System.currentTimeMillis()
                                        if (txn.isShared) return@launch
                                        val friend = friends.firstOrNull { it.id == txn.friendId }
                                        val friendName = friend?.name ?: "Friend"

                                        splitDao.save(txn.copy(isSettled = true, settledAt = now))
                                        firebaseSync.settleSharedSplit(txn.id, now)
                                        if (friend != null) {
                                            friendDao.save(friend.copy(updatedAt = now))
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
    }
}

@Composable
private fun ExpenseHome(
    expenses: List<Expense>, friends: List<FriendBalance>, categories: List<String>,
    currency: String,
    timezoneId: String,
    splitDao: SplitTransactionDao,
    accountStatus: String?,
    onAccountAction: () -> Unit,
    onCategoriesChange: (List<String>) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onTimezoneChange: (String) -> Unit,
    onFriendsChange: (List<FriendBalance>) -> Unit, onDeleteFriend: (String) -> Unit,
    onAssignContact: (FriendBalance, String, String) -> Unit,
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
    var showContactPicker by remember { mutableStateOf(false) }
    var friendToLinkContactId by remember { mutableStateOf<String?>(null) }
    var selectedSplitFriend by remember { mutableStateOf<String?>(null) }
    var viewingFriend by remember { mutableStateOf<FriendBalance?>(null) }
    val ownPhone = remember { PhoneUtils.normalizeToE164(FirebaseAuth.getInstance().currentUser?.phoneNumber) }
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
                onAddFriend = { _, _, _ -> },
                onLinkContact = { targetFriend, contactId, number ->
                    if (ownPhone != null && PhoneUtils.normalizeToE164(number) == ownPhone) return@ContactsScreen
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
                if (ownPhone != null && PhoneUtils.normalizeToE164(number) == ownPhone) return@ContactsScreen
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
                if (tab == 0 || tab == 1 || viewingFriend != null) {
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
                        SmallFloatingActionButton(
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
                }
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
                    "friends" -> FriendsManagementPage(
                        friends,
                        Modifier.padding(padding),
                        onBack = { settingsPage = null },
                        onSave = onFriendsChange,
                    onAssignContact = { friend ->
                        if (friend.contactId == null) friendToLinkContactId = friend.id
                    }
                    )
                    else -> SettingsMenu(categories.size, friends.size, currency, timezoneId, Modifier.padding(padding), onOpen = { settingsPage = it }, accountStatus = accountStatus, onAccountAction = onAccountAction)
                }
            }
        }
        BackHandler(enabled = viewingFriend != null) { viewingFriend = null }
        BackHandler(enabled = showContactPicker) { showContactPicker = false }
        BackHandler(enabled = tab == 2 && settingsPage != null) { settingsPage = null }
    }
}

@Composable
private fun FirebaseSetupScreen() {
    MaterialTheme(colorScheme = lightColorScheme(primary = Green, background = Canvas, surface = CardSurface)) {
        androidx.compose.foundation.layout.Column(
            Modifier.fillMaxSize().padding(28.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            Text("Firebase setup needed", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Add your Firebase Android configuration file at app/google-services.json, then rebuild. " +
                    "Enable Phone sign-in and publish the Firestore rules from firestore.rules.",
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Composable
private fun FirebaseSyncStatus(error: String?, onRetry: () -> Unit, onSignOut: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            if (error == null) {
                CircularProgressIndicator(
                    color = Green,
                    modifier = Modifier.size(44.dp),
                    strokeWidth = 3.5.dp
                )
            }
            Text(
                text = error ?: "Loading your account data…",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = if (error != null) Danger else Ink,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(top = 20.dp)
            )
            if (error != null) {
                androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = Green),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Retry", fontWeight = FontWeight.SemiBold)
                }
                androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
                TextButton(onClick = onSignOut) {
                    Text("Sign out", color = Muted)
                }
            }
        }
    }
}

