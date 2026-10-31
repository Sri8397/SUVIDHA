package com.just_sri.suvidha.expenses.ui

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas as ChartCanvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.just_sri.suvidha.expenses.BorderLight
import com.just_sri.suvidha.expenses.Expense
import com.just_sri.suvidha.expenses.Green
import com.just_sri.suvidha.expenses.Ink
import com.just_sri.suvidha.expenses.Muted
import com.just_sri.suvidha.expenses.PeriodSpan
import com.just_sri.suvidha.expenses.SharedGroupEntity
import com.just_sri.suvidha.expenses.TrendMode
import com.just_sri.suvidha.expenses.buildTrendPeriods
import com.just_sri.suvidha.expenses.expenseDay
import com.just_sri.suvidha.expenses.heroEyebrow
import com.just_sri.suvidha.expenses.heroSubtitle
import com.just_sri.suvidha.expenses.isIncomeCategory
import com.just_sri.suvidha.expenses.money
import com.just_sri.suvidha.expenses.trendPeriodSpan
import com.just_sri.suvidha.expenses.trendPointLabel
import com.just_sri.suvidha.expenses.trendRangeText
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun ExpensesScreen(
    data: AppData,
    model: AppViewModel,
    navigate: (String) -> Unit,
    back: () -> Unit
): Unit {
    var section: String by rememberSaveable { mutableStateOf("Spend") }
    var category: String by rememberSaveable { mutableStateOf("All") }
    var modeName: String by rememberSaveable { mutableStateOf("Week") }
    var anchorText: String? by rememberSaveable { mutableStateOf<String?>(null) }
    var detail: Expense? by remember { mutableStateOf<Expense?>(null) }
    var deleting: Expense? by remember { mutableStateOf<Expense?>(null) }
    val mode: TrendMode = TrendMode.valueOf(modeName)
    val today: LocalDate = LocalDate.now(model.zone)
    val anchor: LocalDate? = anchorText?.let(LocalDate::parse)
    val source: List<Expense> = data.expenses.filter { isIncomeCategory(it.category) == (section == "Earn") }
    val span: PeriodSpan = anchor?.let { trendPeriodSpan(mode, it) } ?: PeriodSpan(today, today)
    val total: Double = source.filter { expenseDay(it.date, model.zone) in span }.sumOf { it.amount }
    val visible: List<Expense> = source.filter { (anchor == null || expenseDay(it.date, model.zone) in span) && (category == "All" || it.category == category) }
    val context: android.content.Context = LocalContext.current
    val selectDate: () -> Unit = {
        val initial: LocalDate = anchor ?: today
        DatePickerDialog(context, { _, y, m, d -> anchorText = LocalDate.of(y, m + 1, d).toString() }, initial.year, initial.monthValue - 1, initial.dayOfMonth).apply {
            datePicker.maxDate = today.atTime(23, 59).atZone(model.zone).toInstant().toEpochMilli()
        }.show()
    }
    if (detail != null) {
        val selectedExpense: Expense = detail!!
        BackHandler { detail = null }
        ExpenseDetailScreen(
            expense = selectedExpense,
            data = data,
            model = model,
            back = { detail = null },
            onDelete = { deleting = selectedExpense }
        )
    } else {
        PageColumn {
            PageHeader("Expenses", back) {
                TextButton({ navigate("add-expense") }) {
                    Icon(Icons.Outlined.Add, null, Modifier.size(18.dp))
                    Text("Add", fontSize = 13.sp)
                }
            }
            PageList {
                item {
                    TotalHeroCard(
                        section = section,
                        onSectionChange = { selectedSection: String ->
                            section = selectedSection
                            category = "All"
                        },
                        total = total,
                        count = source.count { expenseDay(it.date, model.zone) in span },
                        currency = model.currency,
                        isToday = anchor == null,
                        span = span,
                        mode = mode,
                        today = today,
                        onSelectDate = selectDate
                    )
                }
                item {
                    SpendingTrendPanel(
                        section = section,
                        mode = mode,
                        modeName = modeName,
                        onModeChange = { selectedMode: String ->
                            modeName = selectedMode
                            anchorText = null
                        },
                        anchor = anchor,
                        source = source,
                        model = model,
                        onSelectPeriod = { selectedDate: LocalDate ->
                            anchorText = selectedDate.toString()
                        }
                    )
                }
                item {
                    SectionHeading(
                        title = if (section == "Spend") "Recent expenses" else "Income records",
                        detail = "${visible.size} records"
                    ) {
                        if (anchor != null) TextButton({ anchorText = null }) { Text("Show all", fontSize = 12.sp) }
                    }
                }
                item {
                    FilterChips(
                        options = listOf("All") + model.categories.filter { isIncomeCategory(it) == (section == "Earn") },
                        selected = category,
                        onSelect = { selectedCategory: String -> category = selectedCategory }
                    )
                }
                if (visible.isEmpty()) {
                    item {
                        EmptyState("No records here yet", "Add an expense or choose a different period or category.")
                    }
                } else {
                    item {
                        Panel {
                            visible.forEachIndexed { index: Int, expense: Expense ->
                                LedgerRow(
                                    title = expense.description.ifBlank { expense.category },
                                    subtitle = "${expense.category} · ${displayDate(expense.date, model)}",
                                    amount = "${if (section == "Earn") "+" else "−"}${model.currency} ${money(expense.amount)}",
                                    amountColor = if (section == "Earn") Green else Ink,
                                    icon = categoryIcon(expense.category),
                                    divider = index < visible.lastIndex,
                                    onClick = { detail = expense }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    deleting?.let { expense: Expense ->
        ConfirmDialog(
            title = "Delete record?",
            message = "This will remove ${expense.description.ifBlank { expense.category }} from your expenses.",
            confirm = "Delete",
            onDismiss = { deleting = null },
            onConfirm = {
                model.perform("Record deleted", {
                    deleting = null
                    detail = null
                }) {
                    model.repository.deleteExpense(expense.id)
                }
            },
            busy = model.busy
        )
    }
}

private val DeleteSurface: Color = Color(0xFFFFF8F7)
private val DeleteBorder: Color = Color(0xFFFFDAD6)
private val DeleteText: Color = Color(0xFFBA1A1A)

@Composable
internal fun ExpenseDetailScreen(
    expense: Expense,
    data: AppData,
    model: AppViewModel,
    back: () -> Unit,
    onDelete: () -> Unit
): Unit {
    val isIncome: Boolean = isIncomeCategory(expense.category)
    val linkedGroup: SharedGroupEntity? = data.groups.find { expense.description.contains(it.name, ignoreCase = true) }
    PageColumn {
        PageHeader("Expense details", back)
        PageList {
            item {
                ExpenseHeroHeader(
                    expense = expense,
                    currency = model.currency,
                    isShared = linkedGroup != null
                )
            }
            item {
                Text(
                    text = "LEDGER INFORMATION",
                    color = Muted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                )
            }
            item {
                Panel {
                    LedgerDetailRow(
                        label = "Date",
                        value = displayDate(expense.date, model),
                        icon = Icons.Outlined.CalendarToday,
                        showDivider = true
                    )
                    LedgerDetailRow(
                        label = "Category",
                        value = expense.category,
                        icon = categoryIcon(expense.category),
                        showDivider = true
                    )
                    LedgerDetailRow(
                        label = "Type & Flow",
                        value = if (linkedGroup != null) "Shared Split" else if (isIncome) "Income (Credit)" else "Expense (Debit)",
                        icon = Icons.Outlined.AccountBalanceWallet,
                        isBadge = true,
                        showDivider = true
                    )
                    LedgerDetailRow(
                        label = "Paid by",
                        value = "You",
                        icon = Icons.Outlined.Person,
                        showDivider = linkedGroup != null
                    )
                    if (linkedGroup != null) {
                        LedgerDetailRow(
                            label = "Group / Context",
                            value = linkedGroup.name,
                            icon = Icons.Outlined.Groups,
                            showDivider = true
                        )
                    }
                    LedgerDetailRow(
                        label = "Status",
                        value = if (linkedGroup != null) "Synced with group" else "Saved locally",
                        icon = Icons.Outlined.CheckCircle,
                        isBadge = true,
                        showDivider = true
                    )
                    LedgerDetailRow(
                        label = "Ledger Entry ID",
                        value = expense.id,
                        icon = Icons.Outlined.Tag,
                        isMono = true,
                        showDivider = false
                    )
                }
            }
            item {
                Spacer(Modifier.height(16.dp))
                DeleteExpenseButton(onClick = onDelete)
            }
        }
    }
}

@Composable
internal fun ExpenseHeroHeader(
    expense: Expense,
    currency: String,
    modifier: Modifier = Modifier,
    isShared: Boolean = false
): Unit {
    val isIncome: Boolean = isIncomeCategory(expense.category)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .background(Ui.Mint, RoundedCornerShape(999.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = if (isShared) "SHARED SPLIT" else if (isIncome) "INCOME" else "PERSONAL EXPENSE",
                color = Green,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = expense.description.ifBlank { expense.category },
            color = Ink,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 32.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${if (isIncome) "+" else "−"} $currency${money(expense.amount)}",
            color = if (isIncome) Green else Ink,
            fontSize = 38.sp,
            fontWeight = FontWeight.Bold,
            style = TextStyle(fontFeatureSettings = "tnum")
        )
    }
}

@Composable
internal fun LedgerDetailRow(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    isBadge: Boolean = false,
    isMono: Boolean = false,
    isValueGreen: Boolean = false,
    showDivider: Boolean = true
): Unit {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color(0xFF7E8B82)
                )
                Text(
                    text = label,
                    color = Muted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            if (isBadge) {
                Box(
                    modifier = Modifier
                        .background(Ui.Mint, RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = value,
                        color = Green,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Text(
                    text = value,
                    color = if (isValueGreen) Green else Ink,
                    fontSize = if (isMono) 12.sp else 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = if (isMono) FontFamily.Monospace else null
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(color = BorderLight)
        }
    }
}

@Composable
internal fun DeleteExpenseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
): Unit {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp),
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        color = DeleteSurface,
        border = BorderStroke(1.dp, DeleteBorder)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = null,
                tint = DeleteText,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Delete expense",
                color = DeleteText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

internal fun categoryIcon(category: String): ImageVector = when (category.trim().lowercase(Locale.ROOT)) {
    "shopping" -> Icons.Outlined.ShoppingBag
    "household", "home" -> Icons.Outlined.Home
    "cart", "groceries" -> Icons.Outlined.ShoppingCart
    "food", "food & drinks", "coffee", "cafe" -> Icons.Outlined.LocalCafe
    "travel", "commute", "transport" -> Icons.Outlined.DirectionsCar
    "bills", "utilities" -> Icons.Outlined.Receipt
    "salary", "earn" -> Icons.Outlined.AccountBalanceWallet
    else -> Icons.AutoMirrored.Outlined.ReceiptLong
}

@Composable
private fun TotalHeroCard(
    section: String,
    onSectionChange: (String) -> Unit,
    total: Double,
    count: Int,
    currency: String,
    isToday: Boolean,
    span: PeriodSpan,
    mode: TrendMode,
    today: LocalDate,
    onSelectDate: () -> Unit
): Unit {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Ink, RoundedCornerShape(24.dp))
            .padding(horizontal = 22.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val eyebrowText: String = if (isToday) {
                if (section == "Spend") "SPENT TODAY" else "EARNED TODAY"
            } else {
                heroEyebrow(section == "Spend", mode, span, today, true)
            }
            Text(
                text = eyebrowText,
                modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp),
                color = Color(0xFFC6D5CA),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            ModeSegmentedToggle(
                selected = section,
                onSelect = onSectionChange
            )
        }
        val formattedAmount: String = "$currency${money(total)}"
        Text(
            text = formattedAmount,
            color = Color.White,
            fontSize = if (formattedAmount.length > 14) 26.sp else 36.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-1.2).sp,
            lineHeight = 44.sp
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val countText: String = if (isToday) {
                if (section == "Spend") {
                    if (count == 1) "1 expense today" else "$count expenses today"
                } else {
                    if (count == 0) "No income today" else if (count == 1) "1 income record today" else "$count income records today"
                }
            } else {
                heroSubtitle(count, section == "Spend", mode, span, today, true)
            }
            Text(
                text = countText,
                color = Color(0xFFC6D5CA),
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal
            )
            IconButton(
                onClick = onSelectDate,
                modifier = Modifier
                    .size(36.dp)
                    .background(Color.White.copy(alpha = 0.08f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = "Select date",
                    tint = Color(0xFFDCE8DF),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ModeSegmentedToggle(
    selected: String,
    onSelect: (String) -> Unit
): Unit {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.12f))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf("Spend", "Earn").forEach { option: String ->
            val isSelected: Boolean = selected == option
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isSelected) Ui.Neutral else Color.Transparent)
                    .clickable { onSelect(option) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option,
                    color = if (isSelected) Green else Color(0xFFD9E5DC),
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun SpendingTrendPanel(
    section: String,
    mode: TrendMode,
    modeName: String,
    onModeChange: (String) -> Unit,
    anchor: LocalDate?,
    source: List<Expense>,
    model: AppViewModel,
    onSelectPeriod: (LocalDate) -> Unit
): Unit {
    val today: LocalDate = LocalDate.now(model.zone)
    val periods: List<PeriodSpan> = buildTrendPeriods(mode, today, anchor?.let { trendPeriodSpan(mode, it).start })
    val rangeText: String = trendRangeText(mode, periods)
    Panel {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = if (section == "Spend") "Spending trend" else "Earning trend",
                        color = Ink,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = rangeText,
                        color = Muted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                PeriodSegmentedToggle(
                    selected = modeName,
                    onSelect = onModeChange
                )
            }
            ExpenseChart(
                expenses = source,
                mode = mode,
                periods = periods,
                model = model,
                onSelect = onSelectPeriod
            )
        }
    }
}

@Composable
private fun PeriodSegmentedToggle(
    selected: String,
    onSelect: (String) -> Unit
): Unit {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(Ui.Neutral)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TrendMode.entries.forEach { mode: TrendMode ->
            val option: String = mode.name
            val isSelected: Boolean = selected == option
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isSelected) Green else Color.Transparent)
                    .clickable { onSelect(option) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option,
                    color = if (isSelected) Color.White else Muted,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun ExpenseChart(
    expenses: List<Expense>,
    mode: TrendMode,
    periods: List<PeriodSpan>,
    model: AppViewModel,
    onSelect: (LocalDate) -> Unit
): Unit {
    val values: List<Double> = periods.map { period: PeriodSpan ->
        expenses.filter { expenseDay(it.date, model.zone) in period }.sumOf { it.amount }
    }
    val maximum: Double = (values.maxOrNull() ?: 0.0).coerceAtLeast(100.0)
    Column {
        ChartCanvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .pointerInput(periods) {
                    detectTapGestures { tap: Offset ->
                        val colWidth: Float = size.width / periods.size.coerceAtLeast(1).toFloat()
                        val index: Int = (tap.x / colWidth).toInt().coerceIn(periods.indices)
                        onSelect(periods[index].start)
                    }
                }
        ) {
            val top: Float = 6.dp.toPx()
            val mid: Float = size.height / 2f
            val base: Float = size.height - 8.dp.toPx()
            val colWidth: Float = size.width / values.size.coerceAtLeast(1).toFloat()
            listOf(top, mid, base).forEach { y: Float ->
                drawLine(
                    color = BorderLight,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }
            val points: List<Offset> = values.mapIndexed { i: Int, value: Double ->
                val x: Float = (i + 0.5f) * colWidth
                val y: Float = base - ((value / maximum).toFloat() * (base - top))
                Offset(x, y)
            }
            if (points.isNotEmpty()) {
                val areaPath: Path = Path().apply {
                    moveTo(points.first().x, base)
                    points.forEach { pt: Offset -> lineTo(pt.x, pt.y) }
                    lineTo(points.last().x, base)
                    close()
                }
                drawPath(
                    path = areaPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Green.copy(alpha = 0.15f), Green.copy(alpha = 0.0f)),
                        startY = top,
                        endY = base
                    )
                )
                val linePath: Path = Path().apply {
                    points.forEachIndexed { i: Int, pt: Offset ->
                        if (i == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                    }
                }
                drawPath(
                    path = linePath,
                    color = Green,
                    style = Stroke(
                        width = 2.5.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
                points.forEach { pt: Offset ->
                    drawCircle(color = Green, radius = 3.5.dp.toPx(), center = pt)
                    drawCircle(
                        color = Color.White,
                        radius = 3.5.dp.toPx(),
                        center = pt,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            periods.forEachIndexed { i: Int, span: PeriodSpan ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = trendPointLabel(mode, span, i),
                        color = Muted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

internal fun displayDate(timestamp: Long, model: AppViewModel): String =
    DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()).withZone(model.zone).format(Instant.ofEpochMilli(timestamp))

@Preview(showBackground = true, widthDp = 411)
@Composable
private fun TotalHeroCardPreview(): Unit {
    SuvidhaTheme {
        TotalHeroCard(
            section = "Spend",
            onSectionChange = {},
            total = 300.0,
            count = 1,
            currency = "₹",
            isToday = true,
            span = PeriodSpan(LocalDate.now(), LocalDate.now()),
            mode = TrendMode.Week,
            today = LocalDate.now(),
            onSelectDate = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 411)
@Composable
private fun SpendingTrendPanelPreview(): Unit {
    SuvidhaTheme {
        Panel {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = "Spending trend",
                            color = Ink,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "1–30 Oct",
                            color = Muted,
                            fontSize = 11.sp
                        )
                    }
                    PeriodSegmentedToggle(
                        selected = "Week",
                        onSelect = {}
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 411)
@Composable
private fun ExpenseDetailPreview(): Unit {
    SuvidhaTheme {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            val sampleExpense: Expense = Expense(
                id = "exp_20261030_m01",
                category = "Shopping",
                amount = 300.0,
                description = "Mobile Cover and Glass",
                date = 1793343600000L
            )
            ExpenseHeroHeader(
                expense = sampleExpense,
                currency = "₹"
            )
            Text(
                text = "LEDGER INFORMATION",
                color = Muted,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
            )
            Panel {
                LedgerDetailRow(
                    label = "Date",
                    value = "30 Oct 2026",
                    icon = Icons.Outlined.CalendarToday,
                    showDivider = true
                )
                LedgerDetailRow(
                    label = "Category",
                    value = "Shopping",
                    icon = Icons.Outlined.ShoppingBag,
                    showDivider = true
                )
                LedgerDetailRow(
                    label = "Type & Flow",
                    value = "Expense (Debit)",
                    icon = Icons.Outlined.AccountBalanceWallet,
                    isBadge = true,
                    showDivider = true
                )
                LedgerDetailRow(
                    label = "Paid by",
                    value = "You",
                    icon = Icons.Outlined.Person,
                    showDivider = false
                )
                LedgerDetailRow(
                    label = "Status",
                    value = "Saved locally",
                    icon = Icons.Outlined.CheckCircle,
                    isBadge = true,
                    showDivider = true
                )
                LedgerDetailRow(
                    label = "Ledger Entry ID",
                    value = "exp_20261030_m01",
                    icon = Icons.Outlined.Tag,
                    isMono = true,
                    showDivider = false
                )
            }
            Spacer(Modifier.height(16.dp))
            DeleteExpenseButton(onClick = {})
        }
    }
}
