package com.just_sri.suvidha.expenses

import android.app.DatePickerDialog
import androidx.compose.foundation.Canvas as DrawCanvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
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
    val zoneId = LocalZoneId.current
    val today = remember(zoneId) { LocalDate.now(zoneId) }
    var selectedAnchor by remember { mutableStateOf<LocalDate?>(null) }
    var showMonthPicker by remember { mutableStateOf(false) }

    fun expenseLocalDate(timestamp: Long): LocalDate =
        Instant.ofEpochMilli(timestamp).atZone(zoneId).toLocalDate()

    val isEarnExpense: (Expense) -> Boolean = { it.category.equals("Earn", true) || it.category.equals("Salary", true) }
    var activeSection by remember { mutableStateOf("Spend") } // "Spend" or "Earn"
    var selected by remember { mutableStateOf("All") }
    var trendMode by remember { mutableStateOf(TrendMode.Week) }
    var pendingDelete by remember { mutableStateOf<Expense?>(null) }
    var selectedExpenseForDetail by remember { mutableStateOf<Expense?>(null) }

    val spendExpenses = remember(expenses) { expenses.filter { !isEarnExpense(it) } }
    val earnExpenses = remember(expenses) { expenses.filter { isEarnExpense(it) } }

    val currentList = if (activeSection == "Spend") spendExpenses else earnExpenses

    // The trend chart owns the day/week/month choice; Earn has no chart, so it stays day-based.
    val periodMode = if (activeSection == "Spend") trendMode else TrendMode.Day
    val hasSelection = selectedAnchor != null
    val displaySpan = selectedAnchor
        ?.let { trendPeriodSpan(periodMode, it) }
        ?.let { span ->
            when {
                span.start.isAfter(today) -> PeriodSpan(today, today)
                span.endInclusive.isAfter(today) -> span.copy(endInclusive = today)
                else -> span
            }
        } ?: PeriodSpan(today, today)

    val periodExpenses = currentList.filter { expenseLocalDate(it.date) in displaySpan }
    val displayAmount = periodExpenses.sumOf { it.amount }

    val displaySubtitle = heroSubtitle(
        count = periodExpenses.size,
        isSpend = activeSection == "Spend",
        mode = periodMode,
        span = displaySpan,
        today = today,
        hasSelection = hasSelection
    )

    val displayEyebrow = heroEyebrow(
        isSpend = activeSection == "Spend",
        mode = periodMode,
        span = displaySpan,
        today = today,
        hasSelection = hasSelection
    )

    val resetLabel = when (periodMode) {
        TrendMode.Day -> "Today"
        TrendMode.Week -> "This week"
        TrendMode.Month -> "This month"
    }

    val oldestTimestamp = currentList.minOfOrNull { it.date }
    val earliestDate = oldestTimestamp?.let { expenseLocalDate(it) } ?: today
    val minDate = if (earliestDate.isAfter(today)) today else earliestDate

    val openPeriodPicker: () -> Unit = {
        if (periodMode == TrendMode.Month) {
            showMonthPicker = true
        } else {
            val initial = selectedAnchor ?: today
            val dialog = DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    selectedAnchor = LocalDate.of(year, month + 1, dayOfMonth)
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
    }

    val trailingCalendarAction: @Composable () -> Unit = {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (hasSelection) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(OffWhite.copy(alpha = 0.22f))
                        .clickable { selectedAnchor = currentPeriodAnchor(periodMode, today) }
                        .padding(horizontal = 9.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        resetLabel,
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
                    .background(if (hasSelection) Green else OffWhite.copy(alpha = 0.16f))
                    .clickable { openPeriodPicker() },
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
        (!hasSelection || expenseLocalDate(it.date) in displaySpan) &&
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

                Row(
                    modifier = Modifier
                        .background(CardSurface, RoundedCornerShape(20.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("Spend", "Earn").forEach { section ->
                        val active = activeSection == section
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (active) Green else Color.Transparent)
                                .clickable { activeSection = section; selected = "All"; selectedAnchor = null }
                                .padding(horizontal = 16.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(section, color = if (active) OffWhite else Muted, fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
            item {
                ExpenseTrend(
                    expenses = spendExpenses,
                    mode = trendMode,
                    anchor = selectedAnchor,
                    zoneId = zoneId,
                    onModeChange = { trendMode = it },
                    onSelectPeriod = { selectedAnchor = it }
                )
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val headerTitle = if (hasSelection) {
                    val periodText = periodLabel(periodMode, displaySpan, compact = true)
                    if (activeSection == "Spend") "Expenses · $periodText  ·  ${visible.size}" else "Earned · $periodText  ·  ${visible.size}"
                } else {
                    if (activeSection == "Spend") "Recent expenses  ·  ${spendExpenses.size}" else "Earned records  ·  ${earnExpenses.size}"
                }
                Text(
                    headerTitle,
                    color = Ink,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
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
                    if (hasSelection) {
                        val scope = periodScope(periodMode, displaySpan, today, hasSelection = true)
                        if (activeSection == "Spend") "No expenses $scope" else "No earnings $scope"
                    } else {
                        if (activeSection == "Spend") "No expenses yet" else "No earnings yet"
                    },
                    color = Ink,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    if (hasSelection) {
                        "Tap a point on the chart, use the calendar or 'Show all' to view other periods."
                    } else {
                        if (activeSection == "Spend") "Tap + to record one." else "When you settle split money with friends, it will show up here."
                    },
                    color = Muted,
                    fontSize = 13.sp
                )
                if (hasSelection) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Show all",
                        color = Green,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedAnchor = null }
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
    if (showMonthPicker) {
        MonthPickerDialog(
            initial = selectedAnchor ?: today,
            earliest = minDate,
            latest = today,
            onPick = { monthStart ->
                selectedAnchor = monthStart
                showMonthPicker = false
            },
            onDismiss = { showMonthPicker = false }
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

    val filteredList = remember(searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isBlank()) {
            allTimezones
        } else {
            allTimezones.filter {
                it.city.lowercase().contains(q) ||
                it.continent.lowercase().contains(q) ||
                it.id.lowercase().contains(q) ||
                it.offsetString.lowercase().contains(q)
            }
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().then(modifier),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SettingsSubHeader("Timezone", onBack = onBack) }
        item {
            Text(
                "Select timezone for transaction timestamps and daily spending calculations",
                color = Muted,
                fontSize = 14.sp
            )
        }
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search city, continent, or UTC offset...", color = Muted, fontSize = 14.sp) },
                singleLine = true,
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Text("✕", color = Muted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Green,
                    cursorColor = Green,
                    focusedContainerColor = CardSurface,
                    unfocusedContainerColor = CardSurface
                ),
                modifier = Modifier.fillMaxWidth()
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

        if (filteredList.isEmpty() && !showAuto) {
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
            items(filteredList, key = { it.id }) { option ->
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

private enum class TrendMode(val label: String) {
    Day("Day"),
    Week("Week"),
    Month("Month")
}

private data class PeriodSpan(val start: LocalDate, val endInclusive: LocalDate) {
    operator fun contains(date: LocalDate): Boolean = !date.isBefore(start) && !date.isAfter(endInclusive)
}

private val dayMonthFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
private val dayMonthYearFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
private val monthYearFullFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
private val monthYearShortFormatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault())
private val monthOnlyFormatter = DateTimeFormatter.ofPattern("MMM", Locale.getDefault())

private fun expenseDay(timestamp: Long, zoneId: ZoneId): LocalDate =
    Instant.ofEpochMilli(timestamp).atZone(zoneId).toLocalDate()

/** Start/end (inclusive) of the day, week or month that [anchor] falls into. */
private fun trendPeriodSpan(mode: TrendMode, anchor: LocalDate): PeriodSpan = when (mode) {
    TrendMode.Day -> PeriodSpan(anchor, anchor)
    TrendMode.Week -> {
        val monday = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        PeriodSpan(monday, monday.plusDays(6))
    }
    TrendMode.Month -> {
        val first = anchor.withDayOfMonth(1)
        PeriodSpan(first, first.withDayOfMonth(first.lengthOfMonth()))
    }
}

private fun shiftPeriod(mode: TrendMode, start: LocalDate, offset: Int): PeriodSpan = when (mode) {
    TrendMode.Day -> {
        val shifted = start.minusDays(offset.toLong())
        PeriodSpan(shifted, shifted)
    }
    TrendMode.Week -> {
        val shifted = start.minusWeeks(offset.toLong())
        PeriodSpan(shifted, shifted.plusDays(6))
    }
    TrendMode.Month -> {
        val shifted = start.minusMonths(offset.toLong())
        PeriodSpan(shifted, shifted.withDayOfMonth(shifted.lengthOfMonth()))
    }
}

private fun trendPointCount(mode: TrendMode): Int = when (mode) {
    TrendMode.Day -> 7
    TrendMode.Week -> 4
    TrendMode.Month -> 6
}

/**
 * Builds the chart window (oldest → newest). It normally ends on the current period; when a
 * selection older than the window exists the window slides so the selection stays visible.
 */
private fun buildTrendPeriods(mode: TrendMode, today: LocalDate, selectedStart: LocalDate?): List<PeriodSpan> {
    val count = trendPointCount(mode)
    val currentStart = trendPeriodSpan(mode, today).start
    fun window(anchorStart: LocalDate): List<PeriodSpan> =
        (count - 1 downTo 0).map { shiftPeriod(mode, anchorStart, it) }

    val anchoredAtToday = window(currentStart)
    if (selectedStart == null || anchoredAtToday.any { it.start == selectedStart }) return anchoredAtToday
    return window(trendPeriodSpan(mode, selectedStart).start)
}

private fun trendPointLabel(mode: TrendMode, span: PeriodSpan, index: Int): String = when (mode) {
    TrendMode.Day -> span.start.dayOfWeek.name.take(3).lowercase(Locale.getDefault()).replaceFirstChar { it.uppercase(Locale.getDefault()) }
    TrendMode.Week -> "W${index + 1}"
    TrendMode.Month -> span.start.month.name.take(3).lowercase(Locale.getDefault()).replaceFirstChar { it.uppercase(Locale.getDefault()) }
}

private fun periodLabel(mode: TrendMode, span: PeriodSpan, compact: Boolean = false): String = when (mode) {
    TrendMode.Day -> (if (compact) dayMonthFormatter else dayMonthYearFormatter).format(span.start)
    TrendMode.Month -> (if (compact) monthYearShortFormatter else monthYearFullFormatter).format(span.start)
    TrendMode.Week -> {
        val start = span.start
        val end = span.endInclusive
        when {
            start == end -> (if (compact) dayMonthFormatter else dayMonthYearFormatter).format(start)
            start.month == end.month -> {
                val endPart = if (compact) monthOnlyFormatter.format(end) else monthYearShortFormatter.format(end)
                "${start.dayOfMonth} – ${end.dayOfMonth} $endPart"
            }
            else -> "${dayMonthFormatter.format(start)} – " + (if (compact) dayMonthFormatter else dayMonthYearFormatter).format(end)
        }
    }
}

private fun periodScope(mode: TrendMode, span: PeriodSpan, today: LocalDate, hasSelection: Boolean): String {
    if (!hasSelection) return "today"
    return when (mode) {
        TrendMode.Day -> if (span.start == today) "today" else "on this day"
        TrendMode.Week -> if (span.start == trendPeriodSpan(TrendMode.Week, today).start) "this week" else "that week"
        TrendMode.Month -> if (span.start == trendPeriodSpan(TrendMode.Month, today).start) "this month" else "in that month"
    }
}

/** Anchor that lands on the current period for [mode]; a day jumps back to the plain "today" default. */
private fun currentPeriodAnchor(mode: TrendMode, today: LocalDate): LocalDate? = when (mode) {
    TrendMode.Day -> null
    TrendMode.Week, TrendMode.Month -> today
}

private fun heroEyebrow(isSpend: Boolean, mode: TrendMode, span: PeriodSpan, today: LocalDate, hasSelection: Boolean): String {
    val verb = if (isSpend) "SPENT" else "EARNED"
    if (!hasSelection) return "$verb TODAY"
    return when (mode) {
        TrendMode.Day ->
            if (span.start == today) "$verb TODAY" else "$verb ON ${periodLabel(mode, span).uppercase(Locale.getDefault())}"
        TrendMode.Week ->
            if (span.start == trendPeriodSpan(TrendMode.Week, today).start) "$verb THIS WEEK"
            else "$verb · ${periodLabel(mode, span).uppercase(Locale.getDefault())}"
        TrendMode.Month ->
            if (span.start == trendPeriodSpan(TrendMode.Month, today).start) "$verb THIS MONTH"
            else "$verb IN ${periodLabel(mode, span).uppercase(Locale.getDefault())}"
    }
}

private fun heroSubtitle(count: Int, isSpend: Boolean, mode: TrendMode, span: PeriodSpan, today: LocalDate, hasSelection: Boolean): String {
    val noun = if (isSpend) "expense" else "earning"
    val scope = periodScope(mode, span, today, hasSelection)
    return when (count) {
        0 -> "No ${noun}s $scope"
        1 -> "1 $noun $scope"
        else -> "$count ${noun}s $scope"
    }
}

/** Pixel positions used by both the drawing code and the tap handler so hits match the dots. */
private fun trendOffsets(values: List<Double>, width: Float, height: Float, horizontalPadding: Float, verticalPadding: Float): List<Offset> {
    val left = horizontalPadding
    val right = (width - horizontalPadding).coerceAtLeast(left + 1f)
    val top = verticalPadding
    val bottom = (height - verticalPadding).coerceAtLeast(top + 1f)
    val max = (values.maxOrNull() ?: 0.0).let { if (it <= 0.0) 1.0 else it }
    return values.mapIndexed { index, value ->
        val x = if (values.size == 1) (left + right) / 2f else left + (right - left) * index / (values.size - 1)
        val y = bottom - ((value / max).toFloat() * (bottom - top))
        Offset(x, y)
    }
}

@Composable
private fun ExpenseTrend(
    expenses: List<Expense>,
    mode: TrendMode,
    anchor: LocalDate?,
    zoneId: ZoneId,
    onModeChange: (TrendMode) -> Unit,
    onSelectPeriod: (LocalDate) -> Unit
) {
    val today = remember(zoneId) { LocalDate.now(zoneId) }
    val selectedStart = anchor?.let { trendPeriodSpan(mode, it).start }
    val periods = remember(mode, today, selectedStart) { buildTrendPeriods(mode, today, selectedStart) }
    val values = remember(periods, expenses, zoneId) {
        periods.map { span ->
            val lastDay = if (span.endInclusive.isAfter(today)) today else span.endInclusive
            expenses.filter { expense ->
                expenseDay(expense.date, zoneId) in span.start..lastDay
            }.sumOf { it.amount }
        }
    }
    val selectedIndex = periods.indexOfFirst { it.start == selectedStart }

    Column(Modifier.fillMaxWidth().background(CardSurface, RoundedCornerShape(24.dp)).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Spending trend", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TrendMode.values().forEach { choice ->
                val active = mode == choice
                Text(
                    choice.label,
                    color = if (active) OffWhite else Muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .background(if (active) Green else Canvas, RoundedCornerShape(50))
                        .clickable { onModeChange(choice) }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }
        }
        if (expenses.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                Text("Your spending trend will appear here.", color = Muted, fontSize = 13.sp)
            }
        } else {
            DrawCanvas(
                Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .padding(top = 14.dp, bottom = 4.dp)
                    .pointerInput(periods, values) {
                        detectTapGestures { tap ->
                            if (values.isEmpty()) return@detectTapGestures
                            val coords = trendOffsets(values, size.width.toFloat(), size.height.toFloat(), 10.dp.toPx(), 10.dp.toPx())
                            val nearest = coords.indices.minByOrNull { index -> kotlin.math.abs(coords[index].x - tap.x) }
                            if (nearest != null) onSelectPeriod(periods[nearest].start)
                        }
                    }
            ) {
                val hPad = 10.dp.toPx()
                val vPad = 10.dp.toPx()
                val left = hPad
                val right = size.width - hPad
                val top = vPad
                val bottom = size.height - vPad
                for (line in 0..2) {
                    val y = top + (bottom - top) * line / 2f
                    drawLine(Color(0xFFE9EEEA), Offset(left, y), Offset(right, y), 1.dp.toPx())
                }
                val coords = trendOffsets(values, size.width, size.height, hPad, vPad)
                if (coords.size > 1) {
                    val path = Path()
                    coords.forEachIndexed { index, point ->
                        if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
                    }
                    drawPath(path, Green, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
                coords.forEachIndexed { index, point ->
                    if (index == selectedIndex) {
                        drawCircle(Green.copy(alpha = 0.16f), radius = 9.dp.toPx(), center = point)
                        drawCircle(Green, radius = 5.5.dp.toPx(), center = point)
                        drawCircle(CardSurface, radius = 2.dp.toPx(), center = point)
                    } else {
                        drawCircle(Green, radius = 4.dp.toPx(), center = point)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                periods.forEachIndexed { index, span ->
                    Text(
                        trendPointLabel(mode, span, index),
                        color = if (index == selectedIndex) Green else Muted,
                        fontSize = 10.sp,
                        fontWeight = if (index == selectedIndex) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

private val monthShortNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

@Composable
private fun MonthPickerDialog(
    initial: LocalDate,
    earliest: LocalDate,
    latest: LocalDate,
    onPick: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    var year by remember(initial) { mutableStateOf(initial.year) }
    val minYear = minOf(earliest.year, latest.year)
    val maxYear = maxOf(earliest.year, latest.year)
    val earliestMonth = earliest.withDayOfMonth(1)
    val latestMonth = latest.withDayOfMonth(1)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select month", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (year > minYear) Canvas else Canvas.copy(alpha = 0.5f))
                            .clickable(enabled = year > minYear) { year -= 1 },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Previous year",
                            tint = if (year > minYear) Green else Muted.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        year.toString(),
                        color = Ink,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (year < maxYear) Canvas else Canvas.copy(alpha = 0.5f))
                            .clickable(enabled = year < maxYear) { year += 1 },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = "Next year",
                            tint = if (year < maxYear) Green else Muted.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    (0 until 12).chunked(3).forEach { rowMonths ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            rowMonths.forEach { monthIndex ->
                                val monthStart = LocalDate.of(year, monthIndex + 1, 1)
                                val enabled = !monthStart.isBefore(earliestMonth) && !monthStart.isAfter(latestMonth)
                                val active = year == initial.year && monthIndex + 1 == initial.monthValue
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (active) Green else Canvas)
                                        .clickable(enabled = enabled) { onPick(monthStart) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        monthShortNames[monthIndex],
                                        color = when {
                                            active -> OffWhite
                                            enabled -> Ink
                                            else -> Muted.copy(alpha = 0.35f)
                                        },
                                        fontSize = 13.sp,
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Green, fontWeight = FontWeight.SemiBold)
            }
        }
    )
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
    val zoneId = LocalZoneId.current
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
        SettingsMenu(categoryCount = 8, friendCount = 3, currency = "₹", timezoneId = "AUTO", modifier = Modifier, onOpen = {})
    }
}
