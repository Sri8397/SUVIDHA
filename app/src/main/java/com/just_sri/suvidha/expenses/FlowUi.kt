package com.just_sri.suvidha.expenses

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.just_sri.suvidha.expenses.components.AppBackButton
import com.just_sri.suvidha.expenses.components.AppInputDialog
import com.just_sri.suvidha.expenses.components.AppPrimaryButton
import com.just_sri.suvidha.expenses.components.AppTopBar

/**
 * Shared building blocks for the full-screen money flows (Add expense / Split expense).
 * Keeping them in one place guarantees both flows stay visually identical.
 */

@Composable
internal fun FlowBackButton(onBack: () -> Unit) {
    AppBackButton(onBack = onBack)
}

@Composable
internal fun FlowTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppTopBar(title = title, onBack = onBack, modifier = modifier)
}

/**
 * Step 1 of a money flow: large amount display + custom keypad.
 * [trailingAction] mirrors split's "Exact amounts →" shortcut.
 */
@Composable
internal fun AmountEntryStep(
    title: String,
    currency: String,
    amountText: String,
    onAmountChange: (String) -> Unit,
    onBack: () -> Unit,
    onPrimary: () -> Unit,
    primaryLabel: String = "Next",
    trailingAction: (@Composable () -> Unit)? = null,
    primaryEnabled: Boolean? = null,
    fractionDigits: Int = 2,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FlowBackButton(onBack)
            Spacer(Modifier.weight(1f))
            trailingAction?.invoke()
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text = title,
            color = Muted,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(Modifier.height(14.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            Text(
                text = currency,
                color = Ink,
                fontSize = 38.sp,
                fontWeight = FontWeight.Light,
                modifier = Modifier.padding(end = 8.dp)
            )
            val displayValue = if (amountText.isEmpty()) "0" else amountText
            Text(
                text = displayValue,
                color = Ink,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.weight(1f))

        val isValid = primaryEnabled ?: ((amountText.toDoubleOrNull() ?: 0.0) > 0.0)
        AppPrimaryButton(
            text = primaryLabel,
            onClick = onPrimary,
            enabled = isValid,
            shape = RoundedCornerShape(28.dp)
        )

        Spacer(Modifier.height(20.dp))

        NumericKeypad(
            amountText = amountText,
            onAmountChange = onAmountChange,
            fractionDigits = fractionDigits,
        )

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
internal fun NumericKeypad(
    amountText: String,
    onAmountChange: (String) -> Unit,
    fractionDigits: Int = 2,
) {
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(".", "0", "⌫")
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .background(CardSurface, RoundedCornerShape(16.dp))
                            .clickable {
                                handleKeyPress(key, amountText, onAmountChange, fractionDigits)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = key,
                            color = Ink,
                            fontSize = if (key == "⌫") 20.sp else 22.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

private fun handleKeyPress(key: String, current: String, onChange: (String) -> Unit, fractionDigits: Int) {
    when (key) {
        "⌫" -> {
            if (current.isNotEmpty()) onChange(current.dropLast(1))
        }
        "." -> {
            if (fractionDigits > 0 && !current.contains(".")) {
                onChange(if (current.isEmpty()) "0." else "$current.")
            }
        }
        else -> {
            // Personal flows default to two places; shared groups pass currency precision.
            if (current.contains(".")) {
                val decimals = current.substringAfter(".")
                if (decimals.length >= fractionDigits) return
            }
            if (current == "0") {
                onChange(key)
            } else if (current.length < 9) {
                onChange(current + key)
            }
        }
    }
}

@Composable
internal fun NoteEntryDialog(
    initialNote: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    AppInputDialog(
        title = "What's this for?",
        initialValue = initialNote,
        placeholder = "e.g. Dinner, Groceries, Movie (max 50 chars)",
        confirmLabel = "Done",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        maxLength = 50,
        singleLine = false,
        maxLines = 3
    )
}

@Composable
internal fun FlowSaveBar(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Surface(
            color = CardSurface.copy(alpha = 0.92f),
            shadowElevation = 8.dp,
            shape = RoundedCornerShape(32.dp),
            border = BorderStroke(1.dp, BorderLight),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                AppPrimaryButton(
                    text = label,
                    onClick = onClick,
                    enabled = enabled
                )
                if (secondaryLabel != null && onSecondary != null) {
                    TextButton(
                        onClick = onSecondary,
                        enabled = enabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                    ) {
                        Text(
                            text = secondaryLabel,
                            color = if (enabled) Green else Muted.copy(alpha = 0.6f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Shared hero amount card with tap-to-edit and "What's this for?" note chip.
 * Used identically by both ExpenseFlowScreen and SplitFlowScreen.
 */
@Composable
internal fun FlowAmountHero(
    title: String,
    currency: String,
    amount: Double,
    note: String,
    onOpenNoteDialog: () -> Unit,
    onEditAmount: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title, color = Muted, fontSize = 14.sp)
        Spacer(Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(enabled = onEditAmount != null) { onEditAmount?.invoke() }
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = currency,
                color = Ink,
                fontSize = 32.sp,
                fontWeight = FontWeight.Light,
                modifier = Modifier.padding(end = 6.dp)
            )
            Text(
                text = money(amount),
                color = Ink,
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold
            )
            if (onEditAmount != null) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "Edit amount",
                    tint = Green,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // "What's this for?" chip
        Surface(
            onClick = onOpenNoteDialog,
            color = CardSurface,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, BorderLight),
            modifier = Modifier.height(38.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(
                    text = if (note.isBlank()) "What’s this for?" else note,
                    color = if (note.isBlank()) Muted else Ink,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "Edit note",
                    tint = Muted,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

