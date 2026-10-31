package com.just_sri.suvidha.expenses.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.just_sri.suvidha.expenses.*
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class ParticipantShare(
    val memberId: String,
    val name: String,
    val isPayer: Boolean,
    val shareAmount: Long,
)

@Composable
internal fun SplitDetailScreen(
    group: SharedGroupEntity,
    record: SharedLedgerEventRecord,
    data: AppData,
    model: AppViewModel,
    back: () -> Unit,
) {
    var showCancelDialog: Boolean by remember { mutableStateOf(false) }
    val isCancelled: Boolean = resolveSplitStatus(record, data.events) == SplitStatus.CANCELLED
    val canCancel: Boolean = !isCancelled && record.actorMemberId == data.ownId
    val event = remember(record) { runCatching { SharedLedgerCodec.toDomainEvent(record) }.getOrNull() }
    val expenseAction = event?.action as? SharedLedgerAction.ExpenseAdded
    val debts = expenseAction?.debts.orEmpty()
    val payerId: String = record.actorMemberId
    val payerName: String = data.label(payerId)
    val avgShare: Long = if (debts.isNotEmpty()) debts.sumOf { it.amountMinor } / debts.size else 0L
    val totalAmount: Long = debts.sumOf { it.amountMinor } + avgShare
    val participants = remember(debts, payerId) {
        val list = mutableListOf<ParticipantShare>()
        list.add(ParticipantShare(payerId, payerName, isPayer = true, shareAmount = avgShare))
        debts.forEach { debt ->
            list.add(ParticipantShare(debt.debtorMemberId, data.label(debt.debtorMemberId), isPayer = false, shareAmount = debt.amountMinor))
        }
        list
    }
    val yourShare: Long = if (data.ownId == payerId) {
        avgShare
    } else {
        debts.find { it.debtorMemberId == data.ownId }?.amountMinor ?: 0L
    }
    val formattedDate: String = remember(record.createdAtMillis, model.zone) {
        DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.getDefault())
            .withZone(model.zone)
            .format(Instant.ofEpochMilli(record.createdAtMillis))
    }
    PageColumn {
        PageHeader("Split details", back)
        PageList {
            item {
                SplitHeroCard(group, record, payerName, formattedDate, totalAmount, yourShare, isCancelled)
            }
            item {
                SectionHeading("${participants.size} people")
            }
            item {
                SplitParticipantsPanel(participants, group.currencyCode, totalAmount)
            }
            if (canCancel) {
                item {
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { showCancelDialog = true },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFFFDAD6)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFFFFF8F7),
                            contentColor = Color(0xFFBA1A1A)
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        enabled = !model.busy
                    ) {
                        Icon(
                            Icons.Outlined.Cancel,
                            contentDescription = null,
                            tint = Color(0xFFBA1A1A),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Cancel split",
                            color = Color(0xFFBA1A1A),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
    if (showCancelDialog) {
        CancelSplitConfirmDialog(
            onDismiss = { showCancelDialog = false },
            onConfirm = {
                showCancelDialog = false
                model.cancelSplit(group.id, record.id) { back() }
            },
            isBusy = model.busy
        )
    }
}

@Composable
private fun SplitHeroCard(
    group: SharedGroupEntity,
    record: SharedLedgerEventRecord,
    payerName: String,
    formattedDate: String,
    totalAmount: Long,
    yourShare: Long,
    isCancelled: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = CardSurface,
        border = BorderStroke(1.dp, BorderLight)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${group.name} · Split expense",
                    color = Muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                if (isCancelled) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFFCE8E6)
                    ) {
                        Text(
                            text = "CANCELLED",
                            color = Color(0xFFBA1A1A),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = record.description.ifBlank { "Split expense" },
                    modifier = Modifier.weight(1f).padding(end = 12.dp),
                    color = if (isCancelled) Color(0xFF7E8B82) else Ink,
                    fontSize = 17.sp,
                    lineHeight = 23.sp,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (isCancelled) TextDecoration.LineThrough else null
                )
                Text(
                    text = minorMoney(totalAmount, group.currencyCode),
                    color = if (isCancelled) Color(0xFF7E8B82) else Green,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    textDecoration = if (isCancelled) TextDecoration.LineThrough else null
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(text = "Paid by $payerName", color = Muted, fontSize = 13.sp)
                Text(text = "·", color = Muted, fontSize = 13.sp)
                Text(text = formattedDate, color = Muted, fontSize = 13.sp)
            }
            Spacer(Modifier.height(12.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (isCancelled) Color(0xFFF5F6F5) else Ui.Mint,
                shape = RoundedCornerShape(9.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Your share",
                        color = if (isCancelled) Color(0xFF7E8B82) else Green,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = minorMoney(yourShare, group.currencyCode),
                        color = if (isCancelled) Color(0xFF7E8B82) else Green,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (isCancelled) TextDecoration.LineThrough else null
                    )
                }
            }
        }
    }
}

@Composable
private fun SplitParticipantsPanel(
    participants: List<ParticipantShare>,
    currencyCode: String,
    totalAmount: Long
) {
    Panel {
        participants.forEachIndexed { index: Int, participant: ParticipantShare ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Avatar(participant.name)
                Column(Modifier.weight(1f)) {
                    Text(
                        text = participant.name,
                        color = Ink,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (participant.isPayer) {
                        Text(
                            text = "Paid ${minorMoney(totalAmount, currencyCode)}",
                            color = Muted,
                            fontSize = 11.sp
                        )
                    }
                }
                Text(
                    text = minorMoney(participant.shareAmount, currencyCode),
                    color = Ink,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (index < participants.lastIndex) {
                HorizontalDivider(color = BorderLight)
            }
        }
    }
}

@Composable
private fun CancelSplitConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    isBusy: Boolean
) {
    AlertDialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        title = {
            Text(
                text = "Cancel this split?",
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                color = Ink
            )
        },
        text = {
            Text(
                text = "This will cancel the active split calculation for this expense.",
                fontSize = 13.sp,
                color = Muted,
                lineHeight = 19.sp
            )
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isBusy
            ) {
                Text("Keep", fontWeight = FontWeight.SemiBold)
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isBusy,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFBA1A1A),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Cancel split", fontWeight = FontWeight.SemiBold)
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = CardSurface
    )
}
