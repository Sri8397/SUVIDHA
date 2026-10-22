package com.just_sri.suvidha.expenses

import android.app.DatePickerDialog
import androidx.compose.foundation.Canvas as DrawCanvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import java.util.UUID

private val detailTimeFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy · hh:mm:ss a", Locale.getDefault())
private val rowDateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
private val rowDateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.getDefault())

private fun rowAmountFontSize(length: Int) = when {
    length > 20 -> 12.sp
    length > 15 -> 13.sp
    length > 11 -> 14.sp
    else -> 15.sp
}

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
    modifier: Modifier = Modifier,
    trailingAction: (@Composable () -> Unit)? = null
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
            letterSpacing = 1.3.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(8.dp))
        val formattedAmount = "${LocalCurrency.current} ${money(amount)}"
        val amountFontSize = when {
            formattedAmount.length > 22 -> 20.sp
            formattedAmount.length > 17 -> 24.sp
            formattedAmount.length > 13 -> 28.sp
            else -> 34.sp
        }
        Text(
            text = formattedAmount,
            color = OffWhite,
            fontSize = amountFontSize,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 34.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = subtitle,
                color = Color(0xFFD0DAD4),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (trailingAction != null) {
                trailingAction()
            } else {
                Spacer(Modifier.size(34.dp))
            }
        }
    }
}

@Composable
private fun CalendarIcon(
    modifier: Modifier = Modifier,
    tint: Color = OffWhite
) {
    androidx.compose.foundation.Canvas(modifier = modifier.size(18.dp)) {
        val strokeWidth = 1.7.dp.toPx()
        val cornerRadius = 2.5.dp.toPx()
        val w = size.width
        val h = size.height

        drawRoundRect(
            color = tint,
            topLeft = androidx.compose.ui.geometry.Offset(0f, h * 0.18f),
            size = androidx.compose.ui.geometry.Size(w, h * 0.82f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
        )
        drawLine(
            color = tint,
            start = androidx.compose.ui.geometry.Offset(w * 0.28f, 0f),
            end = androidx.compose.ui.geometry.Offset(w * 0.28f, h * 0.26f),
            strokeWidth = strokeWidth,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = androidx.compose.ui.geometry.Offset(w * 0.72f, 0f),
            end = androidx.compose.ui.geometry.Offset(w * 0.72f, h * 0.26f),
            strokeWidth = strokeWidth,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = androidx.compose.ui.geometry.Offset(0f, h * 0.44f),
            end = androidx.compose.ui.geometry.Offset(w, h * 0.44f),
            strokeWidth = strokeWidth * 0.8f
        )
        val dotRadius = 1.0.dp.toPx()
        for (r in 0 until 2) {
            for (c in 0 until 3) {
                val cx = w * (0.28f + c * 0.22f)
                val cy = h * (0.62f + r * 0.18f)
                drawCircle(color = tint, radius = dotRadius, center = androidx.compose.ui.geometry.Offset(cx, cy))
            }
        }
    }
}

@Composable
internal fun ExpensesPage(expenses: List<Expense>, categories: List<String>, modifier: Modifier, onDelete: (String) -> Unit) {
    val context = LocalContext.current
    // Dates are always shown in the device's timezone; currency is only a display symbol.
    val zoneId = remember { ZoneId.systemDefault() }
    val today = remember(zoneId) { LocalDate.now(zoneId) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    fun expenseLocalDate(timestamp: Long): LocalDate =
        Instant.ofEpochMilli(timestamp).atZone(zoneId).toLocalDate()

    val isEarnExpense: (Expense) -> Boolean = { it.category.equals("Earn", true) || it.category.equals("Salary", true) }
    var activeSection by remember { mutableStateOf("Spend") } // "Spend" or "Earn"
    var selected by remember { mutableStateOf("All") }
    var trendPeriod by remember { mutableStateOf("Week") }
    var pendingDelete by remember { mutableStateOf<Expense?>(null) }
    var selectedExpenseForDetail by remember { mutableStateOf<Expense?>(null) }

    val spendExpenses = remember(expenses) { expenses.filter { !isEarnExpense(it) } }
    val earnExpenses = remember(expenses) { expenses.filter { isEarnExpense(it) } }

    val currentList = if (activeSection == "Spend") spendExpenses else earnExpenses

    val todaySpent = spendExpenses.filter { expenseLocalDate(it.date) == today }.sumOf { it.amount }
    val todayEarned = earnExpenses.filter { expenseLocalDate(it.date) == today }.sumOf { it.amount }

    val isViewingPastDate = selectedDate != null && selectedDate != today

    val displayAmount = if (selectedDate != null) {
        currentList.filter { expenseLocalDate(it.date) == selectedDate }.sumOf { it.amount }
    } else {
        if (activeSection == "Spend") todaySpent else todayEarned
    }

    val displaySubtitle = if (selectedDate != null) {
        val count = currentList.count { expenseLocalDate(it.date) == selectedDate }
        "$count ${if (activeSection == "Spend") "expenses" else "earnings"} on this day"
    } else {
        if (activeSection == "Spend") "${spendExpenses.size} expenses recorded" else "${earnExpenses.size} earnings recorded"
    }

    val displayEyebrow = if (isViewingPastDate) {
        val dateLabel = selectedDate!!.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()))
        if (activeSection == "Spend") "SPENT ON ${dateLabel.uppercase()}" else "EARNED ON ${dateLabel.uppercase()}"
    } else {
        if (activeSection == "Spend") "SPENT TODAY" else "EARNED TODAY"
    }

    val oldestTimestamp = currentList.minOfOrNull { it.date }
    val earliestDate = oldestTimestamp?.let { expenseLocalDate(it) } ?: today
    val minDate = if (earliestDate.isAfter(today)) today else earliestDate

    val openDatePicker = {
        val initial = selectedDate ?: today
        val dialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
            },
            initial.year,
            initial.monthValue - 1,
            initial.dayOfMonth
        )
        val minEpoch = minDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val maxEpoch = today.atTime(LocalTime.MAX).atZone(zoneId).toInstant().toEpochMilli()
        dialog.datePicker.minDate = minOf(minEpoch, maxEpoch)
        dialog.datePicker.maxDate = maxEpoch
        dialog.show()
    }

    val trailingCalendarAction: @Composable () -> Unit = {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isViewingPastDate) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(OffWhite.copy(alpha = 0.22f))
                        .clickable { selectedDate = null }
                        .padding(horizontal = 9.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Today",
                        color = OffWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (selectedDate != null) Green else OffWhite.copy(alpha = 0.16f))
                    .clickable { openDatePicker() },
                contentAlignment = Alignment.Center
            ) {
                CalendarIcon(
                    tint = OffWhite,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }

    val visible = currentList.filter {
        (selectedDate == null || expenseLocalDate(it.date) == selectedDate) &&
        (selected == "All" || it.category == selected)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().then(modifier),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ScreenHeader("SUVIDHA  /  MONEY", if (activeSection == "Earn") "Income" else "Expenses", modifier = Modifier.weight(1f))
                val formatBadge: (Int) -> String? = { count ->
                    when {
                        count < 1 -> null
                        count > 99 -> "99+"
                        else -> count.toString()
                    }
                }
                val spendBadge = formatBadge(spendExpenses.size)
                val earnBadge = formatBadge(earnExpenses.size)

                Row(
                    modifier = Modifier
                        .background(CardSurface, RoundedCornerShape(20.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("Spend" to spendBadge, "Earn" to earnBadge).forEach { (section, badge) ->
                        val active = activeSection == section
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (active) Green else Color.Transparent)
                                .clickable { activeSection = section; selected = "All"; selectedDate = null }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(section, color = if (active) OffWhite else Muted, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                if (badge != null) {
                                    Spacer(Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(if (active) OffWhite.copy(alpha = 0.25f) else Green.copy(alpha = 0.15f), CircleShape)
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    ) {
                                        Text(badge, color = if (active) OffWhite else Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            HeroBalanceCard(
                eyebrow = displayEyebrow,
                amount = displayAmount,
                subtitle = displaySubtitle,
                trailingAction = trailingCalendarAction
            )
        }
        if (activeSection == "Spend") {
            item { ExpenseTrend(spendExpenses, trendPeriod) { trendPeriod = it } }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val headerTitle = if (selectedDate != null) {
                    val dateLabel = selectedDate!!.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))
                    if (activeSection == "Spend") "Expenses on $dateLabel  ·  ${visible.size}" else "Earned on $dateLabel  ·  ${visible.size}"
                } else {
                    if (activeSection == "Spend") "Recent expenses  ·  ${spendExpenses.size}" else "Earned records  ·  ${earnExpenses.size}"
                }
                Text(
                    headerTitle,
                    color = Ink,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (selectedDate != null) {
                    Text(
                        "Show all",
                        color = Green,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { selectedDate = null }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                } else {
                    Text("Swipe to delete", color = Muted, fontSize = 11.sp)
                }
            }
            if (activeSection == "Spend") {
                val availableCategories = listOf("All") + categories.filter { !it.equals("Earn", true) }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    availableCategories.forEach { category ->
                        val active = selected == category
                        Text(
                            category,
                            modifier = Modifier
                                .background(if (active) Green else CardSurface, RoundedCornerShape(50))
                                .clickable { selected = category }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            color = if (active) OffWhite else Muted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
        if (visible.isEmpty()) item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(CardSurface, RoundedCornerShape(18.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    if (selectedDate != null) {
                        if (activeSection == "Spend") "No expenses on this date" else "No earnings on this date"
                    } else {
                        if (activeSection == "Spend") "No expenses yet" else "No earnings yet"
                    },
                    color = Ink,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    if (selectedDate != null) {
                        "Tap the calendar or 'Show all' to view other dates."
                    } else {
                        if (activeSection == "Spend") "Tap + to record one." else "When you settle split money with friends, it will show up here."
                    },
                    color = Muted,
                    fontSize = 13.sp
                )
                if (selectedDate != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Show all",
                        color = Green,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedDate = null }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
        items(visible, key = { it.id }) { expense ->
            SwipeToDeleteItem(onDeleteRequested = { pendingDelete = expense }) {
                ExpenseRow(expense, zoneId = zoneId, onClick = { selectedExpenseForDetail = expense })
            }
        }
    }
    pendingDelete?.let { expense ->
        val safeDesc = expense.description.replace('\n', ' ').trim().let {
            if (it.length > 50) it.take(50) + "…" else it
        }
        ConfirmDeleteDialog(
            title = "Delete record?",
            message = "\"${safeDesc}\" (${LocalCurrency.current} ${money(expense.amount)}) will be removed. This cannot be undone.",
            confirmLabel = "Delete",
            onConfirm = { onDelete(expense.id); pendingDelete = null },
            onDismiss = { pendingDelete = null }
        )
    }
    selectedExpenseForDetail?.let { expense ->
        ExpenseDetailDialog(
            expense = expense,
            zoneId = zoneId,
            currency = LocalCurrency.current,
            onDismiss = { selectedExpenseForDetail = null }
        )
    }
}

@Composable
private fun ExpenseRow(expense: Expense, zoneId: ZoneId, onClick: (() -> Unit)? = null) {
    val isEarn = expense.category.equals("Earn", true) || expense.category.equals("Salary", true)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardSurface)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(44.dp).background(if (isEarn) Color(0xFFEAF4EE) else Color(0xFFF5F7F4), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (isEarn) "↑" else expense.category.take(1).uppercase(),
                color = if (isEarn) Green else Ink,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
        val cleanDesc = expense.description.replace('\n', ' ').trim().ifEmpty { expense.category }
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 14.dp)
        ) {
            Text(
                text = cleanDesc,
                color = Ink,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${expense.category}  ·  ${rowDateFormatter.format(Instant.ofEpochMilli(expense.date).atZone(zoneId))}",
                color = Muted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        val formattedAmount = "${if (isEarn) "+" else "−"} ${LocalCurrency.current} ${money(expense.amount)}"
        Text(
            text = formattedAmount,
            color = if (isEarn) Green else Ink,
            fontWeight = FontWeight.Bold,
            fontSize = rowAmountFontSize(formattedAmount.length),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(max = 160.dp)
        )
    }
}

@Composable
internal fun SplitPage(friends: List<FriendBalance>, modifier: Modifier, onDeleteFriend: (String) -> Unit, onAddSplit: (String?) -> Unit, onViewFriend: (FriendBalance) -> Unit) {
    val net = friends.sumOf { it.amount }
    var pendingDelete by remember { mutableStateOf<FriendBalance?>(null) }
    LazyColumn(
        Modifier.fillMaxSize().then(modifier),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 120.dp),
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
                    .background(CardSurface, RoundedCornerShape(18.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No friends yet", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text("Add friends in Settings to start tracking.", color = Muted, fontSize = 13.sp)
            }
        } else {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Friends  ·  ${friends.size}",
                        color = Ink,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text("Swipe to delete", color = Muted, fontSize = 11.sp)
                }
            }
        }
        items(friends, key = { it.id }) { friend ->
            SwipeToDeleteItem(onDeleteRequested = { pendingDelete = friend }) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(CardSurface, RoundedCornerShape(18.dp))
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
                        Text(
                            friend.name.replace('\n', ' ').trim(),
                            color = Ink,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            if (friend.amount > 0) "Owes you" else if (friend.amount < 0) "You owe" else "Settled",
                            color = Muted,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    val formattedAmount = "${LocalCurrency.current} ${money(kotlin.math.abs(friend.amount))}"
                    Text(
                        formattedAmount,
                        color = if (friend.amount > 0) Green else if (friend.amount < 0) Color(0xFFB64C45) else Muted,
                        fontWeight = FontWeight.Bold,
                        fontSize = rowAmountFontSize(formattedAmount.length),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End,
                        modifier = Modifier.widthIn(max = 160.dp)
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
                Icon(Icons.Outlined.Delete, contentDescription = null, tint = OffWhite, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Delete", color = OffWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
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
internal fun SwipeTransactionItem(
    onDeleteRequested: () -> Unit,
    onSettleRequested: () -> Unit,
    enabledSettle: Boolean = true,
    shape: Shape = RoundedCornerShape(18.dp),
    content: @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> {
                    // Left swipe: Settle
                    if (enabledSettle) onSettleRequested()
                    false
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    // Right swipe: Delete
                    onDeleteRequested()
                    false
                }
                else -> false
            }
        },
        positionalThreshold = { distance -> distance * 0.28f }
    )

    val isLeftSwipe = dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart
    val isRightSwipe = dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd
    val settleYellow = Color(0xFFE5A000)

    SwipeToDismissBox(
        state = dismissState,
        modifier = Modifier.fillMaxWidth(),
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = enabledSettle,
        backgroundContent = {
            when {
                isLeftSwipe && enabledSettle -> {
                    Row(
                        Modifier
                            .fillMaxSize()
                            .clip(shape)
                            .background(settleYellow)
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Check, contentDescription = "Settle", tint = OffWhite, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Settle", color = OffWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                isRightSwipe -> {
                    Row(
                        Modifier
                            .fillMaxSize()
                            .clip(shape)
                            .background(Danger)
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = OffWhite, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Delete", color = OffWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                else -> Box(Modifier.fillMaxSize())
            }
        }
    ) { content() }
}

@Composable
internal fun ConfirmSettleDialog(
    title: String,
    message: String,
    confirmLabel: String = "Settle",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold, color = Ink) },
        text = { Text(message, color = Muted, fontSize = 14.sp) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = Color(0xFFD97706), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Muted) }
        }
    )
}

@Composable
internal fun ExpenseDetailDialog(
    expense: Expense,
    zoneId: ZoneId,
    currency: String,
    onDismiss: () -> Unit
) {
    val isEarn = expense.category.equals("Earn", true) || expense.category.equals("Salary", true)
    val formattedTime = remember(expense.date, zoneId) {
        Instant.ofEpochMilli(expense.date).atZone(zoneId).format(detailTimeFormatter)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Green),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Done", color = OffWhite, fontWeight = FontWeight.SemiBold)
            }
        },
        title = {
            Column {
                Text(
                    text = if (isEarn) "INCOME DETAILS" else "EXPENSE DETAILS",
                    color = Green,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.3.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${if (isEarn) "+" else "−"} $currency ${money(expense.amount)}",
                    color = if (isEarn) Green else Ink,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Category", color = Muted, fontSize = 12.sp, modifier = Modifier.width(90.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isEarn) Color(0xFFEAF4EE) else Color(0xFFF0F4F1))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = expense.category,
                            color = if (isEarn) Green else Ink,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Canvas, RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Text("Exact Date & Time", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = formattedTime,
                        color = Ink,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 18.sp
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Canvas, RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Text("Description", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = expense.description.ifBlank { "No description provided" },
                        color = Ink,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 20.sp
                    )
                }
            }
        },
        shape = RoundedCornerShape(22.dp),
        containerColor = CardSurface
    )
}

@Composable
internal fun SplitTransactionDetailDialog(
    txn: SplitTransaction,
    friendName: String,
    zoneId: ZoneId,
    currency: String,
    onDismiss: () -> Unit
) {
    val isSettled = txn.isSettled
    val settleBg = Color(0xFFFEF3C7)
    val settleColor = Color(0xFFD97706)
    val formattedCreatedTime = remember(txn.date, zoneId) {
        Instant.ofEpochMilli(txn.date).atZone(zoneId).format(detailTimeFormatter)
    }
    val formattedSettleTime = remember(txn.settledAt, txn.date, zoneId) {
        val timestamp = txn.settledAt ?: txn.date
        Instant.ofEpochMilli(timestamp).atZone(zoneId).format(detailTimeFormatter)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Green),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Done", color = OffWhite, fontWeight = FontWeight.SemiBold)
            }
        },
        title = {
            Column {
                Text(
                    text = "SPLIT TRANSACTION",
                    color = Green,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.3.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${if (isSettled) "✓ " else if (txn.amount > 0) "+" else "−"} $currency ${money(kotlin.math.abs(txn.amount))}",
                    color = if (isSettled) settleColor else if (txn.amount > 0) Green else Danger,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Friend", color = Muted, fontSize = 11.sp)
                        Text(friendName, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSettled) settleBg else if (txn.amount > 0) Color(0xFFEAF4EE) else Color(0xFFFBECEB))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isSettled) "✓ Settled" else if (txn.amount > 0) "Owes you" else "You owe",
                            color = if (isSettled) settleColor else if (txn.amount > 0) Green else Danger,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Canvas, RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Text("Exact Date & Time", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = formattedCreatedTime,
                        color = Ink,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 18.sp
                    )
                }

                if (isSettled) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(settleBg.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Text("Settle Time", color = settleColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = formattedSettleTime,
                            color = Ink,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 18.sp
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Canvas, RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Text("Description", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = txn.note.ifBlank { "Split bill" },
                        color = Ink,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 20.sp
                    )
                }
            }
        },
        shape = RoundedCornerShape(22.dp),
        containerColor = CardSurface
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
                    .background(CardSurface, RoundedCornerShape(16.dp))
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
                    .background(CardSurface, RoundedCornerShape(16.dp))
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
        val date = Instant.ofEpochMilli(expense.date).atZone(ZoneId.systemDefault()).toLocalDate()
        !date.isBefore(point.start) && !date.isAfter(point.end)
    }.sumOf { it.amount } }
    val periodTotal = values.sum()
    Column(Modifier.fillMaxWidth().background(CardSurface, RoundedCornerShape(24.dp)).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Spending trend", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    "${LocalCurrency.current} ${money(periodTotal)} in this ${period.lowercase()}",
                    color = Muted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            listOf("Day", "Week", "Month").forEach { choice ->
                val active = period == choice
                Text(
                    choice,
                    color = if (active) OffWhite else Muted,
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
    MaterialTheme(colorScheme = lightColorScheme(primary = Green, background = Canvas, surface = CardSurface)) {
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
    MaterialTheme(colorScheme = lightColorScheme(primary = Green, background = Canvas, surface = CardSurface)) {
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
    onDeleteTransaction: (SplitTransaction) -> Unit,
    onSettleTransaction: (SplitTransaction) -> Unit = {}
) {
    var pendingDelete by remember { mutableStateOf<SplitTransaction?>(null) }
    var pendingSettle by remember { mutableStateOf<SplitTransaction?>(null) }
    var selectedTxnForDetail by remember { mutableStateOf<SplitTransaction?>(null) }
    val zoneId = remember { ZoneId.systemDefault() }
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
            }
        }
        if (transactions.isEmpty()) item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(CardSurface, RoundedCornerShape(18.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No transactions yet", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text("Tap + to add a split with ${friend.name}.", color = Muted, fontSize = 13.sp)
            }
        }
        items(transactions, key = { it.id }) { txn ->
            val isSettled = txn.isSettled
            SwipeTransactionItem(
                onDeleteRequested = { pendingDelete = txn },
                onSettleRequested = { if (!isSettled) pendingSettle = txn },
                enabledSettle = !isSettled
            ) {
                val settleColor = Color(0xFFD97706)
                val settleBg = Color(0xFFFEF3C7)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(CardSurface)
                        .clickable { selectedTxnForDetail = txn }
                        .then(
                            if (isSettled) Modifier.border(1.5.dp, Color(0xFFFDE68A), RoundedCornerShape(18.dp))
                            else Modifier
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(44.dp).background(
                            if (isSettled) settleBg else if (txn.amount > 0) Color(0xFFEAF4EE) else Color(0xFFFBECEB),
                            CircleShape
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (isSettled) "✓" else if (txn.amount > 0) "↑" else "↓",
                            color = if (isSettled) settleColor else if (txn.amount > 0) Green else Danger,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                    Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                        Text(
                            txn.note.replace('\n', ' ').trim().ifBlank { if (txn.amount > 0) "They owe you" else "You owe" },
                            color = Ink,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            rowDateTimeFormatter.format(Instant.ofEpochMilli(txn.date).atZone(zoneId)),
                            color = Muted,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    val formattedAmount = "${if (isSettled) "✓ " else if (txn.amount > 0) "+" else "−"} ${LocalCurrency.current} ${money(kotlin.math.abs(txn.amount))}"
                    Text(
                        formattedAmount,
                        color = if (isSettled) settleColor else if (txn.amount > 0) Green else Danger,
                        fontWeight = FontWeight.Bold,
                        fontSize = rowAmountFontSize(formattedAmount.length),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End,
                        modifier = Modifier.widthIn(max = 160.dp)
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
    pendingSettle?.let { txn ->
        ConfirmSettleDialog(
            title = "Settle transaction?",
            message = "${LocalCurrency.current} ${money(kotlin.math.abs(txn.amount))} with ${friend.name} will be marked as settled. ${if (txn.amount > 0) "This money will be added to your Earn section." else ""}",
            confirmLabel = "Settle",
            onConfirm = {
                onSettleTransaction(txn)
                pendingSettle = null
            },
            onDismiss = { pendingSettle = null }
        )
    }
    selectedTxnForDetail?.let { txn ->
        val currency = LocalCurrency.current
        SplitTransactionDetailDialog(
            txn = txn,
            friendName = friend.name,
            zoneId = zoneId,
            currency = currency,
            onDismiss = { selectedTxnForDetail = null }
        )
    }
}

@Preview(showBackground = true, name = "Settings")
@Composable
private fun SettingsPreview() {
    MaterialTheme(colorScheme = lightColorScheme(primary = Green, background = Canvas, surface = CardSurface)) {
        SettingsMenu(categoryCount = 8, friendCount = 3, currency = "₹", modifier = Modifier, onOpen = {})
    }
}
