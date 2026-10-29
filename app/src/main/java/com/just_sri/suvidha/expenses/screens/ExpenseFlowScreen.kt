package com.just_sri.suvidha.expenses.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.just_sri.suvidha.expenses.AmountEntryStep
import com.just_sri.suvidha.expenses.Canvas
import com.just_sri.suvidha.expenses.CardSurface
import com.just_sri.suvidha.expenses.ExpenseDraft
import com.just_sri.suvidha.expenses.FlowAmountHero
import com.just_sri.suvidha.expenses.FlowSaveBar
import com.just_sri.suvidha.expenses.FlowTopBar
import com.just_sri.suvidha.expenses.Green
import com.just_sri.suvidha.expenses.Ink
import com.just_sri.suvidha.expenses.LocalCurrency
import com.just_sri.suvidha.expenses.Muted
import com.just_sri.suvidha.expenses.NoteEntryDialog
import com.just_sri.suvidha.expenses.OffWhite
import com.just_sri.suvidha.expenses.isIncomeCategory
import com.just_sri.suvidha.expenses.money

private enum class ExpenseStep {
    AMOUNT_INPUT,
    DETAILS
}

/**
 * Full-screen Add Expense flow, mirroring [SplitFlowScreen]:
 * step 1 enters the amount with the shared keypad, step 2 picks category + note.
 */
@Composable
internal fun ExpenseFlowScreen(
    categories: List<String>,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onSave: (ExpenseDraft) -> Unit,
    onSaveAndNew: (ExpenseDraft) -> Unit = onSave
) {
    var step by remember { mutableStateOf(ExpenseStep.AMOUNT_INPUT) }
    var amountInput by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var showNoteDialog by remember { mutableStateOf(false) }
    var category by remember(categories) {
        mutableStateOf(
            categories.firstOrNull { it.equals("Other", true) }
                ?: categories.firstOrNull()
                ?: "Other"
        )
    }

    val currency = LocalCurrency.current

    BackHandler {
        if (step == ExpenseStep.DETAILS) {
            step = ExpenseStep.AMOUNT_INPUT
        } else {
            onBack()
        }
    }

    if (showNoteDialog) {
        NoteEntryDialog(
            initialNote = note,
            onDismiss = { showNoteDialog = false },
            onConfirm = { updatedNote ->
                note = updatedNote
                showNoteDialog = false
            }
        )
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Canvas
    ) {
        if (step == ExpenseStep.AMOUNT_INPUT) {
            AmountEntryStep(
                title = "Enter expense amount",
                currency = currency,
                amountText = amountInput,
                onAmountChange = { amountInput = it },
                onBack = onBack,
                onPrimary = {
                    if ((amountInput.toDoubleOrNull() ?: 0.0) > 0.0) {
                        step = ExpenseStep.DETAILS
                    }
                }
            )
        } else {
            ExpenseDetailsStep(
                currency = currency,
                amountText = amountInput,
                note = note,
                category = category,
                categories = categories,
                onBack = { step = ExpenseStep.AMOUNT_INPUT },
                onEditAmount = { step = ExpenseStep.AMOUNT_INPUT },
                onOpenNoteDialog = { showNoteDialog = true },
                onCategoryChange = { category = it },
                onSave = {
                    onSave(
                        ExpenseDraft(
                            category = category,
                            amount = amountInput,
                            note = note.trim()
                        )
                    )
                },
                onSaveAndNew = {
                    onSaveAndNew(
                        ExpenseDraft(
                            category = category,
                            amount = amountInput,
                            note = note.trim()
                        )
                    )
                    amountInput = ""
                    note = ""
                    step = ExpenseStep.AMOUNT_INPUT
                }
            )
        }
    }
}

@Composable
private fun ExpenseDetailsStep(
    currency: String,
    amountText: String,
    note: String,
    category: String,
    categories: List<String>,
    onBack: () -> Unit,
    onEditAmount: () -> Unit,
    onOpenNoteDialog: () -> Unit,
    onCategoryChange: (String) -> Unit,
    onSave: () -> Unit,
    onSaveAndNew: () -> Unit
) {
    val amount = amountText.toDoubleOrNull() ?: 0.0
    val isEarn = isIncomeCategory(category)

    Scaffold(
        containerColor = Canvas,
        contentWindowInsets = WindowInsets.statusBars,
        bottomBar = {
            FlowSaveBar(
                label = "Save & exit  ·  $currency ${money(amount)}",
                enabled = amount > 0.0,
                onClick = onSave,
                secondaryLabel = "Save & add another",
                onSecondary = onSaveAndNew
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
            contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + 24.dp)
        ) {
            item {
                FlowTopBar(title = "Add expense", onBack = onBack)
            }

            // Hero amount — tap to go back to the keypad.
            item {
                FlowAmountHero(
                    title = "Amount",
                    currency = currency,
                    amount = amount,
                    note = note,
                    onOpenNoteDialog = onOpenNoteDialog,
                    onEditAmount = onEditAmount
                )
            }

            // Category picker
            item {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Category",
                            color = Ink,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = if (isEarn) "Income" else "Expense",
                            color = if (isEarn) Green else Muted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    CategoryChips(
                        categories = categories,
                        selected = category,
                        onSelect = onCategoryChange
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = if (isEarn) {
                            "This will be recorded under Earn."
                        } else {
                            "This will be recorded under Expenses."
                        },
                        color = Muted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryChips(
    categories: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { category ->
            val isSelected = category == selected
            Text(
                text = category,
                color = if (isSelected) OffWhite else Muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .widthIn(max = 200.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) Green else CardSurface)
                    .clickable { onSelect(category) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    }
}
