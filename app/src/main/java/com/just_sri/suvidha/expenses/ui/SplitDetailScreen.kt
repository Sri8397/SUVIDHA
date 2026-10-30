package com.just_sri.suvidha.expenses.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.just_sri.suvidha.expenses.*
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun SplitDetailScreen(
    group: SharedGroupEntity,
    record: SharedLedgerEventRecord,
    data: AppData,
    model: AppViewModel,
    back: () -> Unit,
) {
    val event = remember(record) { runCatching { SharedLedgerCodec.toDomainEvent(record) }.getOrNull() }
    val expenseAction = event?.action as? SharedLedgerAction.ExpenseAdded
    val debts = expenseAction?.debts.orEmpty()
    val payerId = record.actorMemberId
    val payerName = data.label(payerId)

    // Calculate participant shares
    // Debts contain what other members owe the payer.
    // In standard split, the payer also paid their own share.
    val avgShare = if (debts.isNotEmpty()) debts.sumOf { it.amountMinor } / debts.size else 0L
    val totalAmount = debts.sumOf { it.amountMinor } + avgShare

    data class ParticipantShare(
        val memberId: String,
        val name: String,
        val isPayer: Boolean,
        val shareAmount: Long,
    )

    val participants = remember(debts, payerId) {
        val list = mutableListOf<ParticipantShare>()
        // Payer
        list.add(ParticipantShare(payerId, payerName, isPayer = true, shareAmount = avgShare))
        // Debtors
        debts.forEach { debt ->
            list.add(ParticipantShare(debt.debtorMemberId, data.label(debt.debtorMemberId), isPayer = false, shareAmount = debt.amountMinor))
        }
        list
    }

    val yourShare = if (data.ownId == payerId) {
        avgShare
    } else {
        debts.find { it.debtorMemberId == data.ownId }?.amountMinor ?: 0L
    }

    val formattedDate = remember(record.createdAtMillis, model.zone) {
        DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.getDefault())
            .withZone(model.zone)
            .format(Instant.ofEpochMilli(record.createdAtMillis))
    }

    PageColumn {
        PageHeader("Split details", back)
        PageList {
            // Hero section matching .split-hero
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = CardSurface,
                    border = BorderStroke(1.dp, BorderLight)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = "${group.name} · Split expense",
                            color = Muted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = record.description.ifBlank { "Split expense" },
                                modifier = Modifier.weight(1f).padding(end = 12.dp),
                                color = Ink,
                                fontSize = 17.sp,
                                lineHeight = 23.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = minorMoney(totalAmount, group.currencyCode),
                                color = Green,
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Paid by $payerName",
                                color = Muted,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "·",
                                color = Muted,
                                fontSize = 13.sp
                            )
                            Text(
                                text = formattedDate,
                                color = Muted,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        // Your share box
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Ui.Mint,
                            shape = RoundedCornerShape(9.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Your share",
                                    color = Green,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = minorMoney(yourShare, group.currencyCode),
                                    color = Green,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Members section matching .split-members
            item {
                SectionHeading("${participants.size} people")
            }

            item {
                Panel {
                    participants.forEachIndexed { index, participant ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
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
                                        text = "Paid ${minorMoney(totalAmount, group.currencyCode)}",
                                        color = Muted,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            Text(
                                text = minorMoney(participant.shareAmount, group.currencyCode),
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
        }
    }
}
