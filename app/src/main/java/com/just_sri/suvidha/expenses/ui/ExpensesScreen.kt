package com.just_sri.suvidha.expenses.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.Canvas as ChartCanvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.just_sri.suvidha.expenses.*
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun ExpensesScreen(data: AppData, model: AppViewModel, navigate: (String) -> Unit, back: () -> Unit) {
    var section by rememberSaveable { mutableStateOf("Spend") }
    var category by rememberSaveable { mutableStateOf("All") }
    var modeName by rememberSaveable { mutableStateOf("Week") }
    var anchorText by rememberSaveable { mutableStateOf<String?>(null) }
    var detail by remember { mutableStateOf<Expense?>(null) }
    var deleting by remember { mutableStateOf<Expense?>(null) }
    val mode = TrendMode.valueOf(modeName)
    val today = LocalDate.now(model.zone)
    val anchor = anchorText?.let(LocalDate::parse)
    val source = data.expenses.filter { isIncomeCategory(it.category) == (section == "Earn") }
    val span = anchor?.let { trendPeriodSpan(mode, it) } ?: PeriodSpan(today, today)
    val total = source.filter { expenseDay(it.date, model.zone) in span }.sumOf { it.amount }
    val visible = source.filter { (anchor == null || expenseDay(it.date, model.zone) in span) && (category == "All" || it.category == category) }
    val context = LocalContext.current
    val selectDate = {
        val initial = anchor ?: today
        DatePickerDialog(context, { _, y, m, d -> anchorText = LocalDate.of(y, m + 1, d).toString() }, initial.year, initial.monthValue - 1, initial.dayOfMonth).apply {
            datePicker.maxDate = today.atTime(23, 59).atZone(model.zone).toInstant().toEpochMilli()
        }.show()
    }
    PageColumn {
        PageHeader("Expenses", back) { TextButton({ navigate("add-expense") }) { Icon(Icons.Outlined.Add, null, Modifier.size(18.dp)); Text("Add", fontSize = 13.sp) } }
        PageList {
            item {
                Column(Modifier.fillMaxWidth().background(Ink, RoundedCornerShape(24.dp)).padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (anchor == null) if (section == "Spend") "SPENT TODAY" else "EARNED TODAY" else periodLabel(mode, span).uppercase(),
                            Modifier.weight(1f), color = Color(0xFFB8C8BF), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
                        IconButton(selectDate, Modifier.size(34.dp)) { Icon(Icons.Outlined.DateRange, "Choose period", tint = Color.White, modifier = Modifier.size(20.dp)) }
                    }
                    Text("${model.currency} ${money(total)}", color = Color.White, fontSize = if (money(total).length > 14) 25.sp else 36.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (anchor == null) "${source.count { expenseDay(it.date, model.zone) == today }} records today" else "${source.count { expenseDay(it.date, model.zone) in span }} records in this period",
                            Modifier.weight(1f), color = Color(0xFFC6D5CA), fontSize = 12.sp)
                        TextButton({ section = if (section == "Spend") "Earn" else "Spend"; category = "All" }) {
                            Text(if (section == "Spend") "Spend ▾" else "Earn ▾", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
            item {
                Panel {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(if (section == "Spend") "Spending trend" else "Earning trend", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Segments(TrendMode.entries.map { it.name }, modeName, { modeName = it; anchorText = null })
                        ExpenseChart(source, mode, anchor, model, { anchorText = it.toString() })
                    }
                }
            }
            item {
                SectionHeading(if (section == "Spend") "Recent expenses" else "Income records", "${visible.size} records") {
                    if (anchor != null) TextButton({ anchorText = null }) { Text("Show all", fontSize = 12.sp) }
                }
            }
            item { FilterChips(listOf("All") + model.categories.filter { isIncomeCategory(it) == (section == "Earn") }, category, { category = it }) }
            if (visible.isEmpty()) item { EmptyState("No records here yet", "Add an expense or choose a different period or category.") }
            itemsIndexed(visible, key = { _, expense -> expense.id }) { _, expense ->
                Panel { LedgerRow(expense.description.ifBlank { expense.category }, "${expense.category} · ${displayDate(expense.date, model)}",
                    amount = "${if (section == "Earn") "+" else "−"}${model.currency} ${money(expense.amount)}",
                    amountColor = if (section == "Earn") Green else Ink, onClick = { detail = expense }) }
            }
        }
    }
    detail?.let { expense ->
        AlertDialog(onDismissRequest = { detail = null }, title = { Text(expense.description.ifBlank { expense.category }) },
            text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { DetailLine("Amount", "${model.currency} ${money(expense.amount)}"); DetailLine("Category", expense.category); DetailLine("Date", displayDate(expense.date, model)) } },
            confirmButton = { TextButton({ detail = null }) { Text("Done") } }, dismissButton = { TextButton({ detail = null; deleting = expense }) { Text("Delete", color = Danger) } })
    }
    deleting?.let { expense -> ConfirmDialog("Delete record?", "This will remove ${expense.description.ifBlank { expense.category }} from your expenses.", "Delete", { deleting = null },
        { model.perform("Record deleted", { deleting = null }) { model.repository.deleteExpense(expense.id) } }, busy = model.busy) }
}

@Composable
private fun ExpenseChart(expenses: List<Expense>, mode: TrendMode, anchor: LocalDate?, model: AppViewModel, onSelect: (LocalDate) -> Unit) {
    val today = LocalDate.now(model.zone)
    val periods = buildTrendPeriods(mode, today, anchor?.let { trendPeriodSpan(mode, it).start })
    val values = periods.map { period -> expenses.filter { expenseDay(it.date, model.zone) in period }.sumOf { it.amount } }
    val maximum = (values.maxOrNull() ?: 0.0).coerceAtLeast(1.0)
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.height(96.dp).width(44.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Text(money(maximum), color = Muted, fontSize = 10.sp, maxLines = 1)
                Text(money(maximum / 2), color = Muted, fontSize = 10.sp, maxLines = 1)
                Text("0", color = Muted, fontSize = 10.sp)
            }
            ChartCanvas(Modifier.weight(1f).height(96.dp).pointerInput(periods) { detectTapGestures { tap ->
                val i = ((tap.x / size.width) * (periods.size - 1)).toInt().coerceIn(periods.indices)
                onSelect(periods[i].start)
            } }) {
                val points = values.mapIndexed { i, value -> Offset(i * size.width / (values.size - 1).coerceAtLeast(1), size.height - 8.dp.toPx() - (value / maximum * (size.height - 16.dp.toPx())).toFloat()) }
                repeat(3) { i -> drawLine(BorderLight, Offset(0f, i * size.height / 2), Offset(size.width, i * size.height / 2), 1.dp.toPx()) }
                val area = Path().apply { moveTo(0f, size.height); points.forEach { lineTo(it.x, it.y) }; lineTo(size.width, size.height); close() }
                drawPath(area, Green.copy(alpha = .07f))
                val line = Path().apply { points.forEachIndexed { i, point -> if (i == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y) } }
                drawPath(line, Green, style = Stroke(2.dp.toPx()))
                points.forEachIndexed { i, point -> drawCircle(if (anchor != null && anchor in periods[i]) Ink else Green, 3.dp.toPx(), point) }
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 54.dp, top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            periods.forEachIndexed { i, span -> Text(trendPointLabel(mode, span, i), color = Muted, fontSize = 9.sp) }
        }
    }
}

internal fun displayDate(timestamp: Long, model: AppViewModel): String = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()).withZone(model.zone).format(Instant.ofEpochMilli(timestamp))
