package com.just_sri.suvidha.expenses

import android.app.DatePickerDialog
import android.content.Intent
import com.just_sri.suvidha.expenses.components.AppConfirmDialog
import com.just_sri.suvidha.expenses.components.AppSwipeItem
import com.just_sri.suvidha.expenses.components.DetailField
import com.just_sri.suvidha.expenses.components.ScreenHeader
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val detailTimeFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy · hh:mm:ss a", Locale.getDefault())
private val rowDateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
private val rowDateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.getDefault())

private fun rowAmountFontSize(length: Int) = when {
    length > 20 -> 12.sp
    length > 15 -> 13.sp
    length > 11 -> 14.sp
    else -> 15.sp
}

private fun Modifier.verticalScrollbar(
    scrollState: androidx.compose.foundation.ScrollState,
    thumbColor: Color = Color(0xFF94A3B8),
    trackColor: Color = Color(0xFFE2E8F0).copy(alpha = 0.6f),
    width: androidx.compose.ui.unit.Dp = 4.dp
): Modifier = this.drawWithContent {
    drawContent()
    if (scrollState.maxValue > 0) {
        val viewHeight = size.height
        val totalHeight = viewHeight + scrollState.maxValue
        val thumbHeight = (viewHeight / totalHeight * viewHeight).coerceIn(24.dp.toPx(), viewHeight)
        val thumbOffset = (scrollState.value.toFloat() / scrollState.maxValue) * (viewHeight - thumbHeight)

        val trackX = size.width - width.toPx()
        drawRoundRect(
            color = trackColor,
            topLeft = Offset(trackX, 0f),
            size = Size(width.toPx(), viewHeight),
            cornerRadius = CornerRadius(width.toPx() / 2f, width.toPx() / 2f)
        )
        drawRoundRect(
            color = thumbColor,
            topLeft = Offset(trackX, thumbOffset),
            size = Size(width.toPx(), thumbHeight),
            cornerRadius = CornerRadius(width.toPx() / 2f, width.toPx() / 2f)
        )
    }
}


// ScreenHeader is now provided by com.just_sri.suvidha.expenses.components.ScreenHeader

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

    var activeSection by remember { mutableStateOf("Spend") } // "Spend" or "Earn"
    var selected by remember { mutableStateOf("All") }
    var trendMode by remember { mutableStateOf(TrendMode.Week) }
    var pendingDelete by remember { mutableStateOf<Expense?>(null) }
    var selectedExpenseForDetail by remember { mutableStateOf<Expense?>(null) }

    val spendExpenses = remember(expenses) { expenses.filterNot { isIncomeCategory(it.category) } }
    val earnExpenses = remember(expenses) { expenses.filter { isIncomeCategory(it.category) } }

    val currentList = if (activeSection == "Spend") spendExpenses else earnExpenses

    // Both Spend and Earn trend charts own the day/week/month choice.
    val periodMode = trendMode
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

    val periodExpenses = currentList.filter { expenseDay(it.date, zoneId) in displaySpan }
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
    val earliestDate = oldestTimestamp?.let { expenseDay(it, zoneId) } ?: today
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
        (!hasSelection || expenseDay(it.date, zoneId) in displaySpan) &&
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
        item {
            val isSpend = activeSection == "Spend"
            ExpenseTrend(
                title = if (isSpend) "Spending trend" else "Earning trend",
                emptyText = if (isSpend) "Your spending trend will appear here." else "Your earning trend will appear here.",
                expenses = if (isSpend) spendExpenses else earnExpenses,
                mode = trendMode,
                anchor = selectedAnchor,
                zoneId = zoneId,
                onModeChange = { trendMode = it },
                onSelectPeriod = { selectedAnchor = it }
            )
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
    val isEarn = isIncomeCategory(expense.category)
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
            ScreenHeader("BAKAYA  /  FRIENDS", "Friends")
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
            message = "${friend.name} and their local balance of ${LocalCurrency.current} ${money(kotlin.math.abs(friend.amount))} will be removed. Shared group membership and shared history remain. This cannot be undone.",
            confirmLabel = "Remove",
            onConfirm = { onDeleteFriend(friend.id); pendingDelete = null },
            onDismiss = { pendingDelete = null }
        )
    }
}

@Composable
private fun SwipeToDeleteItem(
    onDeleteRequested: () -> Unit,
    shape: Shape = RoundedCornerShape(18.dp),
    content: @Composable () -> Unit
) = AppSwipeItem(
    shape = shape,
    onLeftSwipe = onDeleteRequested,
    leftLabel = "Delete",
    leftIcon = Icons.Outlined.Delete,
    leftColor = Danger,
    threshold = 0.35f,
    content = content
)

@Composable
internal fun ConfirmDeleteDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AppConfirmDialog(
        title = title,
        message = message,
        confirmLabel = confirmLabel,
        confirmColor = Danger,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
private fun SwipeTransactionItem(
    onDeleteRequested: () -> Unit,
    onSettleRequested: () -> Unit,
    enabledDelete: Boolean = true,
    enabledSettle: Boolean = true,
    shape: Shape = RoundedCornerShape(18.dp),
    content: @Composable () -> Unit
) = AppSwipeItem(
    shape = shape,
    onLeftSwipe = if (enabledSettle) onSettleRequested else null,
    leftLabel = "Settle",
    leftIcon = Icons.Outlined.Check,
    leftColor = Color(0xFFE5A000),
    onRightSwipe = if (enabledDelete) onDeleteRequested else null,
    rightLabel = "Delete",
    rightIcon = Icons.Outlined.Delete,
    rightColor = Danger,
    threshold = 0.28f,
    content = content
)

@Composable
private fun ConfirmSettleDialog(
    title: String,
    message: String,
    confirmLabel: String = "Settle",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AppConfirmDialog(
        title = title,
        message = message,
        confirmLabel = confirmLabel,
        confirmColor = Color(0xFFD97706),
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
private fun ExpenseDetailDialog(
    expense: Expense,
    zoneId: ZoneId,
    currency: String,
    onDismiss: () -> Unit
) {
    val isEarn = isIncomeCategory(expense.category)
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
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp)
                    .verticalScrollbar(scrollState)
                    .padding(end = if (scrollState.maxValue > 0) 8.dp else 0.dp)
                    .verticalScroll(scrollState),
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

                DetailField("Exact Date & Time", formattedTime)
                DetailField("Description", expense.description.ifBlank { "No description provided" })
            }
        },
        shape = RoundedCornerShape(22.dp),
        containerColor = CardSurface
    )
}

@Composable
private fun SplitTransactionDetailDialog(
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
                val sign = if (txn.amount < 0) "− " else "+ "
                Text(
                    text = "${if (isSettled) "✓ " else ""}$sign$currency ${money(kotlin.math.abs(txn.amount))}",
                    color = if (isSettled) settleColor else if (txn.amount > 0) Green else Danger,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
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
                    if (!isSettled) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (txn.amount > 0) Color(0xFFEAF4EE) else Color(0xFFFBECEB))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (txn.amount > 0) "Owes you" else "You owe",
                                color = if (txn.amount > 0) Green else Danger,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                        .verticalScrollbar(scrollState)
                        .padding(end = if (scrollState.maxValue > 0) 8.dp else 0.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DetailField("Exact Date & Time", formattedCreatedTime)

                    if (isSettled) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(settleBg.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                .padding(12.dp)
                        ) {
                            Text("Settled By", color = settleColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text("You", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, lineHeight = 18.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("Settle Time", color = settleColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(formattedSettleTime, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Medium, lineHeight = 18.sp)
                        }
                    }

                    DetailField("Description", txn.note.ifBlank { "Split bill" })
                }
            }
        },
        shape = RoundedCornerShape(22.dp),
        containerColor = CardSurface
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
                FriendBalance("1", "Aarav", System.currentTimeMillis()).withAmount(450.0),
                FriendBalance("2", "Meera", System.currentTimeMillis()).withAmount(-120.0)
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
    val context = LocalContext.current
    val currency = LocalCurrency.current
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
            val netBalance = remember(transactions) {
                transactions.filterNot { it.isSettled }.sumOf { it.amount }
            }
            HeroBalanceCard(
                eyebrow = "NET BALANCE",
                amount = kotlin.math.abs(netBalance),
                subtitle = if (netBalance > 0) "${friend.name} owes you" else if (netBalance < 0) "You owe ${friend.name}" else "All settled",
                isNegative = netBalance < 0
            )
        }
        item {
            val netBalance = remember(transactions) {
                transactions.filterNot { it.isSettled }.sumOf { it.amount }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Transactions  ·  ${transactions.size}", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(
                    onClick = {
                        val message = "Hi ${friend.name}, a quick reminder that your balance with me is $currency ${money(kotlin.math.abs(netBalance))}. When you get a chance, could you settle it? Thanks!"
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, message)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Send reminder"))
                    },
                    enabled = netBalance > 0.0
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Share balance reminder",
                        tint = if (netBalance > 0.0) Green else Muted
                    )
                }
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
                enabledDelete = true,
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
                    val sign = if (txn.amount < 0) "− " else "+ "
                    val formattedAmount = "${if (isSettled) "✓ " else ""}$sign${LocalCurrency.current} ${money(kotlin.math.abs(txn.amount))}"
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
