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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Shared building blocks for the full-screen money flows (Add expense / Split expense).
 * Keeping them in one place guarantees both flows stay visually identical.
 */

@Composable
internal fun FlowBackButton(onBack: () -> Unit) {
    IconButton(
        onClick = onBack,
        modifier = Modifier
            .size(40.dp)
            .background(CardSurface, CircleShape)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = "Back",
            tint = Ink
        )
    }
}

@Composable
internal fun FlowTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FlowBackButton(onBack)
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            color = Ink,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
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
    trailingAction: (@Composable () -> Unit)? = null
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

        val isValid = (amountText.toDoubleOrNull() ?: 0.0) > 0.0
        Button(
            onClick = onPrimary,
            enabled = isValid,
            colors = ButtonDefaults.buttonColors(
                containerColor = Green,
                contentColor = OffWhite,
                disabledContainerColor = Green.copy(alpha = 0.35f),
                disabledContentColor = OffWhite.copy(alpha = 0.7f)
            ),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(primaryLabel, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(20.dp))

        NumericKeypad(
            amountText = amountText,
            onAmountChange = onAmountChange
        )

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
internal fun NumericKeypad(
    amountText: String,
    onAmountChange: (String) -> Unit
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
                                handleKeyPress(key, amountText, onAmountChange)
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

private fun handleKeyPress(key: String, current: String, onChange: (String) -> Unit) {
    when (key) {
        "⌫" -> {
            if (current.isNotEmpty()) onChange(current.dropLast(1))
        }
        "." -> {
            if (!current.contains(".")) {
                onChange(if (current.isEmpty()) "0." else "$current.")
            }
        }
        else -> {
            // Max two decimal places.
            if (current.contains(".")) {
                val decimals = current.substringAfter(".")
                if (decimals.length >= 2) return
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
    var tempNote by remember { mutableStateOf(initialNote) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("What's this for?", fontWeight = FontWeight.Bold, color = Ink) },
        text = {
            OutlinedTextField(
                value = tempNote,
                onValueChange = { if (it.length <= 50) tempNote = it },
                placeholder = { Text("e.g. Dinner, Groceries, Movie (max 50 chars)") },
                singleLine = false,
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Green,
                    cursorColor = Green
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(tempNote.trim()) }) {
                Text("Done", color = Green, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Muted)
            }
        }
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
                Button(
                    onClick = onClick,
                    enabled = enabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Green,
                        contentColor = OffWhite,
                        disabledContainerColor = Green.copy(alpha = 0.35f),
                        disabledContentColor = OffWhite.copy(alpha = 0.7f)
                    ),
                    shape = RoundedCornerShape(26.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
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
