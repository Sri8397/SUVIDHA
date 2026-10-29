package com.just_sri.suvidha.expenses.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.just_sri.suvidha.expenses.Danger
import com.just_sri.suvidha.expenses.CardSurface
import com.just_sri.suvidha.expenses.Green
import com.just_sri.suvidha.expenses.Ink
import com.just_sri.suvidha.expenses.Muted
import com.just_sri.suvidha.expenses.OffWhite

/**
 * Shared, reusable dialogs for confirmations, inputs, and detail views.
 */

/**
 * Reusable confirmation dialog for deletions, settlements, and warnings.
 */
@Composable
fun AppConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmLabel: String = "Confirm",
    dismissLabel: String = "Cancel",
    confirmColor: Color = Danger
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardSurface,
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = Ink,
                fontSize = 18.sp
            )
        },
        text = {
            Text(
                text = message,
                color = Muted,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmLabel,
                    color = confirmColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = dismissLabel,
                    color = Muted
                )
            }
        }
    )
}

/**
 * Reusable single-field input dialog (e.g., editing friend names, category names, notes).
 */
@Composable
fun AppInputDialog(
    title: String,
    initialValue: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    placeholder: String = "",
    confirmLabel: String = "Save",
    dismissLabel: String = "Cancel",
    maxLength: Int = 50,
    singleLine: Boolean = true,
    maxLines: Int = 1
) {
    var textValue by remember(initialValue) { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardSurface,
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = Ink,
                fontSize = 18.sp
            )
        },
        text = {
            AppOutlinedTextField(
                value = textValue,
                onValueChange = { if (it.length <= maxLength) textValue = it },
                placeholder = placeholder,
                singleLine = singleLine,
                maxLines = maxLines,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(textValue.trim()) },
                enabled = textValue.trim().isNotEmpty()
            ) {
                Text(
                    text = confirmLabel,
                    color = if (textValue.trim().isNotEmpty()) Green else Muted,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = dismissLabel,
                    color = Muted
                )
            }
        }
    )
}

/**
 * Reusable detail modal with an action button ("Done").
 */
@Composable
fun AppDetailDialog(
    eyebrow: String,
    title: String,
    titleColor: Color = Ink,
    onDismiss: () -> Unit,
    dismissLabel: String = "Done",
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardSurface,
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Green),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = dismissLabel,
                    color = OffWhite,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        title = {
            Column {
                Text(
                    text = eyebrow.uppercase(),
                    color = Green,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.3.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = title,
                    color = titleColor,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            content()
        }
    )
}
