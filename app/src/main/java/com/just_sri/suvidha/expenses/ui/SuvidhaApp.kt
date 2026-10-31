package com.just_sri.suvidha.expenses.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.just_sri.suvidha.expenses.*
import com.just_sri.suvidha.expenses.R
import kotlinx.coroutines.awaitCancellation

@Composable
internal fun SuvidhaApp(model: AppViewModel, initialRoute: String = "home") {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(model, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            model.onResume()
            awaitCancellation()
        }
    }
    val db = model.database
    val expenses by remember(db) { db.expenses().observeAll() }.collectAsStateWithLifecycle(emptyList())
    val friends by remember(db) { db.friends().observeAll() }.collectAsStateWithLifecycle(emptyList())
    val splits by remember(db) { db.splitTransactions().observeAll() }.collectAsStateWithLifecycle(emptyList())
    val groups by remember(db) { db.sharedGroups().observeAll() }.collectAsStateWithLifecycle(emptyList())
    val members by remember(db) { db.sharedGroupMembers().observeAll() }.collectAsStateWithLifecycle(emptyList())
    val events by remember(db) { db.sharedLedgerEvents().observeAll() }.collectAsStateWithLifecycle(emptyList())
    val links by remember(db) { db.sharedMemberFriendLinks().observeAll() }.collectAsStateWithLifecycle(emptyList())
    val data = remember(expenses, friends, splits, groups, members, events, links, model.publicId) { AppData(expenses, friends, splits, groups, members, events, links, model.publicId) }
    var routes by rememberSaveable { mutableStateOf<List<String>>(listOf(initialRoute)) }
    LaunchedEffect(initialRoute) { if (initialRoute != "home") routes = listOf(initialRoute) }
    val route = routes.last()
    val destinations = listOf("home", "expenses", "friends", "groups", "profile")
    val main = route in destinations
    val navigate: (String) -> Unit = { target ->
        routes = when {
            target in destinations -> listOf(target)
            routes.last() == "new-group" && target.startsWith("group/") -> listOf("groups", target)
            else -> routes + target
        }
    }
    val back: () -> Unit = { if (routes.last() == route) routes = if (routes.size > 1) routes.dropLast(1) else listOf("home") }
    BackHandler(enabled = routes.size > 1 || route != "home", onBack = back)
    val snackbar = remember { SnackbarHostState() }
    val message = model.message
    LaunchedEffect(message) {
        if (message != null) { snackbar.showSnackbar(message); if (model.message == message) model.message = null }
    }
    CompositionLocalProvider(LocalCurrency provides model.currency, LocalZoneId provides model.zone) {
        SuvidhaTheme(themePref = model.themePreference) {
            Surface(Modifier.fillMaxSize(), color = Canvas) {
                Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(), contentAlignment = Alignment.TopCenter) {
                    Scaffold(Modifier.widthIn(max = 560.dp).fillMaxSize(), containerColor = Canvas,
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        bottomBar = { if (main) MainNavigation(route, navigate) },
                        snackbarHost = { SnackbarHost(snackbar) }) { padding ->
                        Column(Modifier.padding(padding).fillMaxSize()) {
                            if (model.busy) LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp))
                            Box(Modifier.weight(1f)) {
                                key(route) {
                                    val id = route.substringAfter('/', "")
                                    val group = groups.find { it.id == id }
                                    val friend = friends.find { it.id == id }
                                    when {
                                        route == "home" -> HomeScreen(data, model.profileName, navigate)
                                        route == "expenses" -> ExpensesScreen(data, model, navigate, back)
                                        route == "friends" -> FriendsScreen(data, model, navigate, back)
                                        route == "groups" -> GroupsScreen(data, model, navigate, back)
                                        route == "profile" -> ProfileScreen(model, navigate, back)
                                        route == "new-friend" -> AddFriendScreen(model, back)
                                        route == "new-group" -> NewGroupScreen(data, model, navigate, back)
                                        route == "add-expense" -> MoneyFlow(data, model, back)
                                        route == "theme" -> ThemeSelectionScreen(model, back)
                                        route == "currency" -> CurrencyScreen(model, back)
                                        route == "timezone" -> TimezoneScreen(model, back)
                                        route == "categories" -> CategoriesScreen(model, back)
                                        route == "relays" -> RelaysScreen(model, back)
                                        route == "backup" -> BackupScreen(model, back)
                                        route.startsWith("group/") && group != null -> GroupScreen(group, data, model, navigate, back)
                                        route.startsWith("members/") && group != null -> MembersScreen(group, data, model, navigate, back)
                                        route.startsWith("group-split/") && group != null -> MoneyFlow(data, model, back, group = group)
                                        route.startsWith("split-detail/") -> {
                                            val parts = route.removePrefix("split-detail/").split("/")
                                            val splitGroupId = parts.getOrNull(0)
                                            val splitEventId = parts.getOrNull(1)
                                            val splitGroup = groups.find { it.id == splitGroupId }
                                            val splitEvent = events.find { it.id == splitEventId }
                                            if (splitGroup != null && splitEvent != null) {
                                                SplitDetailScreen(splitGroup, splitEvent, data, model, back)
                                            } else {
                                                PageColumn {
                                                    PageHeader("Split details", back)
                                                    PageList { item { EmptyState("Split not found", "This split may have been removed or does not exist.") } }
                                                }
                                            }
                                        }
                                        route.startsWith("friend/") && friend != null -> FriendScreen(friend, data, model, navigate, back)
                                        route.startsWith("direct-split/") && friend != null -> MoneyFlow(data, model, back, friend = friend)
                                        route.startsWith("expense/") -> {
                                            val expenseId: String = route.removePrefix("expense/")
                                            val expense: Expense? = expenses.find { it.id == expenseId }
                                            if (expense != null) {
                                                ExpenseDetailScreen(
                                                    expense = expense,
                                                    data = data,
                                                    model = model,
                                                    back = back,
                                                    onDelete = {
                                                        model.perform("Record deleted", { back() }) {
                                                            model.repository.deleteExpense(expense.id)
                                                        }
                                                    }
                                                )
                                            } else {
                                                PageColumn {
                                                    PageHeader("Expense details", back)
                                                    PageList { item { EmptyState("Expense not found", "This expense may have been removed or does not exist.") } }
                                                }
                                            }
                                        }
                                        else -> PageColumn { PageHeader("Suvidha", back); PageList { item { EmptyState("No longer available", "This item may have been removed or restored from a different backup.") } } }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MainNavigation(selected: String, onSelect: (String) -> Unit) {
    Column {
        HorizontalDivider(color = BorderLight)
        Row(Modifier.fillMaxWidth().background(Canvas).padding(horizontal = 8.dp, vertical = 8.dp)) {
            listOf("home" to "Home", "expenses" to "Expenses", "friends" to "Friends", "groups" to "Groups", "profile" to "Profile").forEach { (route, title) ->
                Column(Modifier.weight(1f).clickable { onSelect(route) }.padding(vertical = 2.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Box(Modifier.size(52.dp, 30.dp).background(if (selected == route) Ui.Mint else Color.Transparent, CircleShape), contentAlignment = Alignment.Center) {
                        AppIcon(route, if (selected == route) Green else Muted)
                    }
                    Text(title, color = if (selected == route) Green else Muted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
internal fun AppIcon(name: String, tint: Color = Green) {
    when (name) {
        "home" -> Icon(Icons.Outlined.Home, null, Modifier.size(24.dp), tint)
        "friends" -> Icon(painterResource(R.drawable.ic_friends), null, Modifier.size(24.dp), tint)
        "profile" -> Icon(Icons.Outlined.Person, null, Modifier.size(24.dp), tint)
        else -> Icon(painterResource(if (name == "expenses") R.drawable.ic_analytics else R.drawable.ic_community), null, Modifier.size(24.dp), tint)
    }
}
