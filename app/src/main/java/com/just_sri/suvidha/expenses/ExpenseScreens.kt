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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
internal fun ExpensesPage(expenses: List<Expense>, categories: List<String>, modifier: Modifier, onDelete: (String) -> Unit) {
    var selected by remember { mutableStateOf("All") }
    var trendPeriod by remember { mutableStateOf("Week") }
    val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
    val todayTotal = expenses.filter { SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(it.date)) == today }.sumOf { it.amount }
    val visible = expenses.filter { selected == "All" || it.category == selected }
    LazyColumn(modifier = Modifier.fillMaxSize().then(modifier), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("SUVIDHA  /  MONEY", color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                    Text("Expenses", color = Ink, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        item {
            Column(Modifier.fillMaxWidth().background(Ink, RoundedCornerShape(24.dp)).padding(22.dp)) {
                Text("SPENT TODAY", color = Color(0xFFB8C8BF), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
                Spacer(Modifier.height(8.dp)); Text("₹ ${money(todayTotal)}", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp)); Text("${expenses.size} expenses recorded", color = Color(0xFFD0DAD4), fontSize = 13.sp)
            }
        }
        item { ExpenseTrend(expenses, trendPeriod) { trendPeriod = it } }
        item {
            Text("Recent expenses  ·  ${expenses.size}", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                (listOf("All") + categories).forEach { category ->
                    val active = selected == category
                    Text(category, modifier = Modifier.background(if (active) Green else Color.White, RoundedCornerShape(50)).clickable { selected = category }.padding(horizontal = 13.dp, vertical = 9.dp), color = if (active) Color.White else Muted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
        if (visible.isEmpty()) item {
            Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(20.dp)).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No expenses yet", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(5.dp)); Text("Tap Add expense to record one.", color = Muted, fontSize = 13.sp)
            }
        }
        items(visible, key = { it.id }) { expense ->
            Row(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(18.dp)).padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).background(Color(0xFFEAF4EE), CircleShape), contentAlignment = Alignment.Center) { Text(expense.category.take(1), color = Green, fontWeight = FontWeight.Bold) }
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(expense.description, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("${expense.category}  ·  ${SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(expense.date))}", color = Muted, fontSize = 12.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("− ₹ ${money(expense.amount)}", color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Delete", color = Color(0xFFB64C45), fontSize = 11.sp, modifier = Modifier.clickable { onDelete(expense.id) }.padding(top = 4.dp))
                }
            }
        }
    }
}

@Composable
internal fun SplitPage(friends: List<FriendBalance>, modifier: Modifier, onAddSplit: (String?) -> Unit) {
    val net = friends.sumOf { it.amount }
    LazyColumn(Modifier.fillMaxSize().then(modifier), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("BAKAYA  /  FRIENDS", color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
            Text("Split expenses", color = Ink, fontSize = 29.sp, fontWeight = FontWeight.Bold)
            Text("Keep a simple running balance with friends.", color = Muted, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
        }
        item {
            Column(Modifier.fillMaxWidth().background(if (net >= 0) Ink else Color(0xFF773E3A), RoundedCornerShape(22.dp)).padding(20.dp)) {
                Text("YOUR NET BALANCE", color = Color(0xFFD0DAD4), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text("₹ ${money(kotlin.math.abs(net))}", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
                Text(if (net > 0) "Friends owe you" else if (net < 0) "You owe friends" else "All settled", color = Color(0xFFE1E9E4), fontSize = 13.sp)
            }
        }
        if (friends.isEmpty()) item {
            Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(18.dp)).padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No friends yet", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text("Add friends in Settings to start tracking.", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 5.dp))
            }
        }
        items(friends, key = { it.id }) { friend ->
            Row(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(18.dp)).clickable { onAddSplit(friend.id) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).background(Color(0xFFEAF4EE), CircleShape), contentAlignment = Alignment.Center) { Text(friend.name.take(1).uppercase(), color = Green, fontWeight = FontWeight.Bold) }
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(friend.name, color = Ink, fontWeight = FontWeight.SemiBold)
                    Text(if (friend.amount > 0) "Owes you" else if (friend.amount < 0) "You owe" else "Settled", color = Muted, fontSize = 12.sp)
                }
                Text("₹ ${money(kotlin.math.abs(friend.amount))}", color = if (friend.amount >= 0) Green else Color(0xFFB64C45), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
internal fun SettingsMenu(categoryCount: Int, friendCount: Int, modifier: Modifier, onOpen: (String) -> Unit) {
    Column(Modifier.fillMaxSize().then(modifier).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("SETTINGS", color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
        Text("Manage lists", color = Ink, fontSize = 29.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        SettingsRow("Categories", "$categoryCount expense categories", onClick = { onOpen("categories") })
        SettingsRow("Friends", "$friendCount friends", onClick = { onOpen("friends") })
    }
}

@Composable
private fun SettingsRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).background(Color(0xFFEAF4EE), CircleShape), contentAlignment = Alignment.Center) {
            Text(if (title == "Friends") "F" else "C", color = Green, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(title, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(subtitle, color = Muted, fontSize = 12.sp)
        }
        Text("›", color = Muted, fontSize = 26.sp)
    }
}

@Composable
private fun SettingsSubHeader(title: String, modifier: Modifier, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().then(modifier), verticalAlignment = Alignment.CenterVertically) {
        Text("‹", color = Green, fontSize = 30.sp, modifier = Modifier.clickable(onClick = onBack).padding(end = 12.dp))
        Column {
            Text("SETTINGS", color = Green, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Text(title, color = Ink, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun CategoryManagementPage(categories: List<String>, modifier: Modifier, onBack: () -> Unit, onSave: (List<String>) -> Unit) {
    var editable by remember(categories) { mutableStateOf(categories) }
    var newName by remember { mutableStateOf("") }
    LazyColumn(Modifier.fillMaxSize().then(modifier), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SettingsSubHeader("Categories", Modifier, onBack) }
        item { Text("Expense categories", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(newName, { newName = it }, label = { Text("New category") }, singleLine = true, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp))
                Spacer(Modifier.width(8.dp))
                Button(onClick = { val name = newName.trim(); if (name.isNotEmpty() && editable.none { it.equals(name, true) }) { editable = (editable.filterNot { it.equals("Other", true) } + name + "Other").distinct(); onSave(editable); newName = "" } }, shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text("Add") }
            }
        }
        itemsIndexed(editable, key = { index, _ -> index }) { index, category ->
            Row(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(category, { value -> editable = editable.toMutableList().also { it[index] = value } }, modifier = Modifier.weight(1f), singleLine = true, enabled = !category.equals("Other", true), shape = RoundedCornerShape(12.dp))
                if (!category.equals("Other", true)) TextButton(onClick = { editable = editable.filterNot { it == category }; onSave(editable) }) { Text("Remove", color = Color(0xFFB64C45)) }
                else Text("Default", color = Green, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp))
            }
        }
        item {
            Text("Other is always available as the default category.", color = Muted, fontSize = 12.sp)
        }
        item {
            Button(onClick = {
                editable = (editable.map { it.trim() }.filter { it.isNotEmpty() && !it.equals("Other", true) }.distinct() + "Other")
                onSave(editable)
            }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text("Save changes") }
        }
    }
}

@Composable
internal fun FriendsManagementPage(friends: List<FriendBalance>, modifier: Modifier, onBack: () -> Unit, onSave: (List<FriendBalance>) -> Unit) {
    var editable by remember(friends) { mutableStateOf(friends) }
    var newName by remember { mutableStateOf("") }
    LazyColumn(Modifier.fillMaxSize().then(modifier), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SettingsSubHeader("Friends", Modifier, onBack) }
        item { Text("People you split expenses with", color = Muted, fontSize = 14.sp) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(newName, { newName = it }, label = { Text("Friend's name") }, singleLine = true, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp))
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    val name = newName.trim()
                    if (name.isNotEmpty() && editable.none { it.name.equals(name, true) }) {
                        editable = editable + FriendBalance(UUID.randomUUID().toString(), name, 0.0, System.currentTimeMillis())
                        newName = ""
                    }
                }, shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text("Add") }
            }
        }
        itemsIndexed(editable, key = { _, friend -> friend.id }) { index, friend ->
            Row(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(friend.name, { name -> editable = editable.toMutableList().also { it[index] = friend.copy(name = name) } }, modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(12.dp))
                TextButton(onClick = { editable = editable.filterNot { it.id == friend.id } }) { Text("Remove", color = Color(0xFFB64C45)) }
            }
        }
        item { Button(onClick = { onSave(editable.map { it.copy(name = it.name.trim()) }.filter { it.name.isNotEmpty() }) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text("Save changes") } }
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
    Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(22.dp)).padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Spending trend", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("₹ ${money(periodTotal)} in this ${period.lowercase()}", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
            }
            listOf("Day", "Week", "Month").forEach { choice ->
                val active = period == choice
                Text(choice, color = if (active) Color.White else Muted, fontSize = 11.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 4.dp).background(if (active) Green else Canvas, RoundedCornerShape(50)).clickable { onPeriodChange(choice) }.padding(horizontal = 9.dp, vertical = 7.dp))
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
            onAddSplit = {}
        )
    }
}

@Preview(showBackground = true, name = "Settings")
@Composable
private fun SettingsPreview() {
    MaterialTheme(colorScheme = lightColorScheme(primary = Green, background = Canvas, surface = Color.White)) {
        SettingsMenu(categoryCount = 8, friendCount = 3, modifier = Modifier, onOpen = {})
    }
}

