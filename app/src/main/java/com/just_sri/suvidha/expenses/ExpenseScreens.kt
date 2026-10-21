package com.just_sri.suvidha.expenses

import androidx.compose.foundation.Canvas as DrawCanvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
private fun ScreenHeader(
    eyebrow: String,
    title: String,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Text(eyebrow, color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
        Spacer(Modifier.height(4.dp))
        Text(title, color = Ink, fontSize = 30.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HeroBalanceCard(
    eyebrow: String,
    amount: Double,
    subtitle: String,
    isNegative: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = if (isNegative) Color(0xFF773E3A) else Ink,
                shape = RoundedCornerShape(24.dp)
            )
            .padding(22.dp)
    ) {
        Text(
            text = eyebrow.uppercase(),
            color = Color(0xFFB8C8BF),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.3.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "${LocalCurrency.current} ${money(amount)}",
            color = Color.White,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = subtitle,
            color = Color(0xFFD0DAD4),
            fontSize = 13.sp
        )
    }
}

@Composable
internal fun ExpensesPage(expenses: List<Expense>, categories: List<String>, modifier: Modifier, onDelete: (String) -> Unit) {
    var selected by remember { mutableStateOf("All") }
    var trendPeriod by remember { mutableStateOf("Week") }
    var pendingDelete by remember { mutableStateOf<Expense?>(null) }
    val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
    val todayTotal = expenses.filter { SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(it.date)) == today }.sumOf { it.amount }
    val visible = expenses.filter { selected == "All" || it.category == selected }
    LazyColumn(
        modifier = Modifier.fillMaxSize().then(modifier),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ScreenHeader("SUVIDHA  /  MONEY", "Expenses")
        }
        item {
            HeroBalanceCard(
                eyebrow = "SPENT TODAY",
                amount = todayTotal,
                subtitle = "${expenses.size} expenses recorded"
            )
        }
        item { ExpenseTrend(expenses, trendPeriod) { trendPeriod = it } }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Recent expenses  ·  ${expenses.size}", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("Swipe to delete", color = Muted, fontSize = 11.sp)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                (listOf("All") + categories).forEach { category ->
                    val active = selected == category
                    Text(
                        category,
                        modifier = Modifier
                            .background(if (active) Green else Color.White, RoundedCornerShape(50))
                            .clickable { selected = category }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        color = if (active) Color.White else Muted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        if (visible.isEmpty()) item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(18.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No expenses yet", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text("Tap + to record one.", color = Muted, fontSize = 13.sp)
            }
        }
        items(visible, key = { it.id }) { expense ->
            SwipeToDeleteItem(onDeleteRequested = { pendingDelete = expense }) {
                ExpenseRow(expense)
            }
        }
    }
    pendingDelete?.let { expense ->
        ConfirmDeleteDialog(
            title = "Delete expense?",
            message = "\"${expense.description}\" (${LocalCurrency.current} ${money(expense.amount)}) will be removed. This cannot be undone.",
            confirmLabel = "Delete",
            onConfirm = { onDelete(expense.id); pendingDelete = null },
            onDismiss = { pendingDelete = null }
        )
    }
}

@Composable
private fun ExpenseRow(expense: Expense) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(44.dp).background(Color(0xFFEAF4EE), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(expense.category.take(1).uppercase(), color = Green, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(expense.description, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(2.dp))
            Text(
                "${expense.category}  ·  ${SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(expense.date))}",
                color = Muted,
                fontSize = 12.sp
            )
        }
        Text("− ${LocalCurrency.current} ${money(expense.amount)}", color = Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

@Composable
internal fun SplitPage(friends: List<FriendBalance>, modifier: Modifier, onDeleteFriend: (String) -> Unit, onAddSplit: (String?) -> Unit, onViewFriend: (FriendBalance) -> Unit) {
    val net = friends.sumOf { it.amount }
    var pendingDelete by remember { mutableStateOf<FriendBalance?>(null) }
    LazyColumn(
        Modifier.fillMaxSize().then(modifier),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ScreenHeader("BAKAYA  /  FRIENDS", "Split expenses")
        }
        item {
            HeroBalanceCard(
                eyebrow = "YOUR NET BALANCE",
                amount = kotlin.math.abs(net),
                subtitle = if (net > 0) "Friends owe you" else if (net < 0) "You owe friends" else "All settled",
                isNegative = net < 0
            )
        }
        if (friends.isEmpty()) item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(18.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No friends yet", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text("Add friends in Settings to start tracking.", color = Muted, fontSize = 13.sp)
            }
        }
        items(friends, key = { it.id }) { friend ->
            SwipeToDeleteItem(onDeleteRequested = { pendingDelete = friend }) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(18.dp))
                        .clickable { onViewFriend(friend) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(44.dp).background(Color(0xFFEAF4EE), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(friend.name.take(1).uppercase(), color = Green, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                        Text(friend.name, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            if (friend.amount > 0) "Owes you" else if (friend.amount < 0) "You owe" else "Settled",
                            color = Muted,
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        "${LocalCurrency.current} ${money(kotlin.math.abs(friend.amount))}",
                        color = if (friend.amount > 0) Green else if (friend.amount < 0) Color(0xFFB64C45) else Muted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
    pendingDelete?.let { friend ->
        ConfirmDeleteDialog(
            title = "Remove friend?",
            message = "${friend.name} and their balance of ${LocalCurrency.current} ${money(kotlin.math.abs(friend.amount))} will be removed. This cannot be undone.",
            confirmLabel = "Remove",
            onConfirm = { onDeleteFriend(friend.id); pendingDelete = null },
            onDismiss = { pendingDelete = null }
        )
    }
}

@Composable
internal fun SwipeToDeleteItem(
    onDeleteRequested: () -> Unit,
    shape: Shape = RoundedCornerShape(18.dp),
    content: @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) onDeleteRequested()
            false
        },
        positionalThreshold = { distance -> distance * 0.35f }
    )
    SwipeToDismissBox(
        state = dismissState,
        modifier = Modifier.fillMaxWidth(),
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Row(
                Modifier.fillMaxSize().clip(shape).background(Danger).padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Delete", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    ) { content() }
}

@Composable
internal fun ConfirmDeleteDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(message, color = Muted, fontSize = 14.sp) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmLabel, color = Danger, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Muted) }
        }
    )
}

@Composable
internal fun SettingsMenu(categoryCount: Int, friendCount: Int, currency: String, modifier: Modifier, onOpen: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .then(modifier)
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScreenHeader("SETTINGS", "Manage lists")
        SettingsRow("Currency", currency, icon = "¤", onClick = { onOpen("currency") })
        SettingsRow("Categories", "$categoryCount expense categories", icon = "C", onClick = { onOpen("categories") })
        SettingsRow("Friends", "$friendCount friends", icon = "F", onClick = { onOpen("friends") })
    }
}

@Composable
private fun SettingsRow(title: String, subtitle: String, icon: String = title.take(1), onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(18.dp))
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
private fun SettingsSubHeader(title: String, modifier: Modifier = Modifier, onBack: () -> Unit) {
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
        Spacer(Modifier.width(8.dp))
        ScreenHeader("SETTINGS", title)
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
                        if (selected) Color(0xFFEAF4EE) else Color.White,
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
                    Text(symbol, color = if (selected) Color.White else Green, fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
internal fun CategoryManagementPage(categories: List<String>, modifier: Modifier, onBack: () -> Unit, onSave: (List<String>) -> Unit) {
    var editable by remember(categories) { mutableStateOf(categories) }
    var newName by remember { mutableStateOf("") }
    LazyColumn(
        Modifier.fillMaxSize().then(modifier),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { SettingsSubHeader("Categories", onBack = onBack) }
        item { Text("Expense categories", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    newName,
                    { newName = it },
                    label = { Text("New category") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(Modifier.width(10.dp))
                Button(
                    onClick = {
                        val name = newName.trim()
                        if (name.isNotEmpty() && editable.none { it.equals(name, true) }) {
                            editable = (editable.filterNot { it.equals("Other", true) } + name + "Other").distinct()
                            onSave(editable)
                            newName = ""
                        }
                    },
                    modifier = Modifier.height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Green)
                ) {
                    Text("Add", fontWeight = FontWeight.SemiBold)
                }
            }
        }
        itemsIndexed(editable, key = { index, _ -> index }) { index, category ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    category,
                    { value -> editable = editable.toMutableList().also { it[index] = value } },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    enabled = !category.equals("Other", true),
                    shape = RoundedCornerShape(12.dp)
                )
                if (!category.equals("Other", true)) {
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { editable = editable.filterNot { it == category }; onSave(editable) }) {
                        Text("Remove", color = Color(0xFFB64C45), fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Text("Default", color = Green, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 12.dp))
                }
            }
        }
        item {
            Text("Other is always available as the default category.", color = Muted, fontSize = 12.sp)
        }
        item {
            Button(
                onClick = {
                    editable = (editable.map { it.trim() }.filter { it.isNotEmpty() && !it.equals("Other", true) }.distinct() + "Other")
                    onSave(editable)
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Green)
            ) {
                Text("Save changes", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
internal fun FriendsManagementPage(friends: List<FriendBalance>, modifier: Modifier, onBack: () -> Unit, onSave: (List<FriendBalance>) -> Unit) {
    var editable by remember(friends) { mutableStateOf(friends) }
    var newName by remember { mutableStateOf("") }
    LazyColumn(
        Modifier.fillMaxSize().then(modifier),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { SettingsSubHeader("Friends", onBack = onBack) }
        item { Text("People you split expenses with", color = Muted, fontSize = 14.sp) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    newName,
                    { newName = it },
                    label = { Text("Friend's name") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(Modifier.width(10.dp))
                Button(
                    onClick = {
                        val name = newName.trim()
                        if (name.isNotEmpty() && editable.none { it.name.equals(name, true) }) {
                            editable = editable + FriendBalance(UUID.randomUUID().toString(), name, 0.0, System.currentTimeMillis())
                            newName = ""
                        }
                    },
                    modifier = Modifier.height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Green)
                ) {
                    Text("Add", fontWeight = FontWeight.SemiBold)
                }
            }
        }
        itemsIndexed(editable, key = { _, friend -> friend.id }) { index, friend ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    friend.name,
                    { name -> editable = editable.toMutableList().also { it[index] = friend.copy(name = name) } },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = { editable = editable.filterNot { it.id == friend.id } }) {
                    Text("Remove", color = Color(0xFFB64C45), fontWeight = FontWeight.SemiBold)
                }
            }
        }
        item {
            Button(
                onClick = { onSave(editable.map { it.copy(name = it.name.trim()) }.filter { it.name.isNotEmpty() }) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Green)
            ) {
                Text("Save changes", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private data class TrendPoint(val label: String, val start: LocalDate, val end: LocalDate)

@Composable
private fun ExpenseTrend(expenses: List<Expense>, period: String, onPeriodChange: (String) -> Unit) {
    val today = LocalDate.now()
    val points = when (period) {
        "Day" -> (6 downTo 0).map { offset -> val date = today.minusDays(offset.toLong()); TrendPoint(date.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }, date, date) }
        "Month" -> (5 downTo 0).map { offset -> val month = today.withDayOfMonth(1).minusMonths(offset.toLong()); TrendPoint(month.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }, month, month.withDayOfMonth(month.lengthOfMonth())) }
        else -> {
            val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            (3 downTo 0).map { offset -> val start = monday.minusWeeks(offset.toLong()); TrendPoint("W${4 - offset}", start, start.plusDays(6)) }
        }
    }
    val values = points.map { point -> expenses.filter { expense ->
        val date = java.time.Instant.ofEpochMilli(expense.date).atZone(ZoneId.systemDefault()).toLocalDate()
        !date.isBefore(point.start) && !date.isAfter(point.end)
    }.sumOf { it.amount } }
    val periodTotal = values.sum()
    Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(24.dp)).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Spending trend", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("${LocalCurrency.current} ${money(periodTotal)} in this ${period.lowercase()}", color = Muted, fontSize = 12.sp)
            }
            listOf("Day", "Week", "Month").forEach { choice ->
                val active = period == choice
                Text(
                    choice,
                    color = if (active) Color.White else Muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .background(if (active) Green else Canvas, RoundedCornerShape(50))
                        .clickable { onPeriodChange(choice) }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }
        }
        if (expenses.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) { Text("Your spending trend will appear here.", color = Muted, fontSize = 13.sp) }
        } else {
            DrawCanvas(Modifier.fillMaxWidth().height(150.dp).padding(top = 14.dp, bottom = 4.dp)) {
                val left = 8f; val right = size.width - 8f; val top = 8f; val bottom = size.height - 8f
                val max = (values.maxOrNull() ?: 0.0).let { if (it <= 0) 1.0 else it } 
                for (line in 0..2) {
                    val y = top + (bottom - top) * line / 2
                    drawLine(Color(0xFFE9EEEA), androidx.compose.ui.geometry.Offset(left, y), androidx.compose.ui.geometry.Offset(right, y), 1.dp.toPx())
                }
                val path = Path()
                val coords = values.mapIndexed { index, value ->
                    val x = if (values.size == 1) (left + right) / 2 else left + (right - left) * index / (values.size - 1)
                    val y = bottom - ((value / max).toFloat() * (bottom - top))
                    androidx.compose.ui.geometry.Offset(x, y)
                }
                coords.forEachIndexed { index, point -> if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y) }
                drawPath(path, Green, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx()))
                coords.forEach { drawCircle(Green, radius = 4.dp.toPx(), center = it) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                points.forEach { Text(it.label, color = Muted, fontSize = 10.sp) }
            }
        }
    }
}

@Preview(showBackground = true, name = "Expenses")
@Composable
private fun ExpensesPreview() {
    MaterialTheme(colorScheme = lightColorScheme(primary = Green, background = Canvas, surface = Color.White)) {
        ExpensesPage(
            expenses = listOf(
                Expense("1", "Food", 240.0, "Lunch", System.currentTimeMillis()),
                Expense("2", "Transport", 80.0, "Auto", System.currentTimeMillis() - 86_400_000L)
            ),
            categories = defaultExpenseCategories,
            modifier = Modifier,
            onDelete = {}
        )
    }
}

@Preview(showBackground = true, name = "Split")
@Composable
private fun SplitPreview() {
    MaterialTheme(colorScheme = lightColorScheme(primary = Green, background = Canvas, surface = Color.White)) {
        SplitPage(
            friends = listOf(
                FriendBalance("1", "Aarav", 450.0, System.currentTimeMillis()),
                FriendBalance("2", "Meera", -120.0, System.currentTimeMillis())
            ),
            modifier = Modifier,
            onDeleteFriend = {},
            onAddSplit = {},
            onViewFriend = {}
        )
    }
}

@Composable
internal fun FriendTransactionsPage(
    friend: FriendBalance,
    transactions: List<SplitTransaction>,
    modifier: Modifier,
    onBack: () -> Unit,
    onAddSplit: (String) -> Unit,
    onDeleteTransaction: (SplitTransaction) -> Unit
) {
    var pendingDelete by remember { mutableStateOf<SplitTransaction?>(null) }
    LazyColumn(
        Modifier.fillMaxSize().then(modifier),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                Spacer(Modifier.width(8.dp))
                ScreenHeader("BAKAYA  /  HISTORY", friend.name)
            }
        }
        item {
            HeroBalanceCard(
                eyebrow = "NET BALANCE",
                amount = kotlin.math.abs(friend.amount),
                subtitle = if (friend.amount > 0) "${friend.name} owes you" else if (friend.amount < 0) "You owe ${friend.name}" else "All settled",
                isNegative = friend.amount < 0
            )
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Transactions  ·  ${transactions.size}", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("Swipe to delete", color = Muted, fontSize = 11.sp)
            }
        }
        if (transactions.isEmpty()) item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(18.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No transactions yet", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text("Tap + to add a split with ${friend.name}.", color = Muted, fontSize = 13.sp)
            }
        }
        items(transactions, key = { it.id }) { txn ->
            SwipeToDeleteItem(onDeleteRequested = { pendingDelete = txn }) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(18.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(44.dp).background(
                            if (txn.amount > 0) Color(0xFFEAF4EE) else Color(0xFFFBECEB),
                            CircleShape
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (txn.amount > 0) "↑" else "↓",
                            color = if (txn.amount > 0) Green else Danger,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                    Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                        Text(
                            txn.note.ifBlank { if (txn.amount > 0) "They owe you" else "You owe" },
                            color = Ink,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()).format(Date(txn.date)),
                            color = Muted,
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        "${if (txn.amount > 0) "+" else "−"} ${LocalCurrency.current} ${money(kotlin.math.abs(txn.amount))}",
                        color = if (txn.amount > 0) Green else Danger,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
    pendingDelete?.let { txn ->
        ConfirmDeleteDialog(
            title = "Delete transaction?",
            message = "${LocalCurrency.current} ${money(kotlin.math.abs(txn.amount))} split with ${friend.name} will be removed and the balance will be adjusted.",
            confirmLabel = "Delete",
            onConfirm = { onDeleteTransaction(txn); pendingDelete = null },
            onDismiss = { pendingDelete = null }
        )
    }
}

@Preview(showBackground = true, name = "Settings")
@Composable
private fun SettingsPreview() {
    MaterialTheme(colorScheme = lightColorScheme(primary = Green, background = Canvas, surface = Color.White)) {
        SettingsMenu(categoryCount = 8, friendCount = 3, currency = "₹", modifier = Modifier, onOpen = {})
    }
}
