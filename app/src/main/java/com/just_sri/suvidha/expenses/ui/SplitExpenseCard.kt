package com.just_sri.suvidha.expenses.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.just_sri.suvidha.expenses.*
import java.time.ZoneId

internal enum class SplitStatus {
    ACTIVE,
    CANCELLED
}

internal fun resolveSplitStatus(
    record: SharedLedgerEventRecord,
    events: List<SharedLedgerEventRecord>
): SplitStatus {
    if (record.description.contains("(Cancelled)", ignoreCase = true)) {
        return SplitStatus.CANCELLED
    }
    val domainEvent = runCatching { SharedLedgerCodec.toDomainEvent(record) }.getOrNull() ?: return SplitStatus.ACTIVE
    val expense = domainEvent.action as? SharedLedgerAction.ExpenseAdded ?: return SplitStatus.ACTIVE
    if (expense.debts.isEmpty()) {
        return SplitStatus.ACTIVE
    }
    val removedDebtIds: Set<String> = events.mapNotNull { eventRecord ->
        val parsedEvent = runCatching { SharedLedgerCodec.toDomainEvent(eventRecord) }.getOrNull()
        (parsedEvent?.action as? SharedLedgerAction.DebtRemoved)?.debtUnitId
    }.toSet()
    val isCancelled: Boolean = expense.debts.all { debt -> debt.id in removedDebtIds }
    return if (isCancelled) SplitStatus.CANCELLED else SplitStatus.ACTIVE
}

@Composable
internal fun SplitExpenseCard(
    record: SharedLedgerEventRecord,
    status: SplitStatus,
    data: AppData,
    group: SharedGroupEntity,
    zone: ZoneId,
    hasLaterPeerEvent: Boolean,
    splitIndex: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCancelled: Boolean = status == SplitStatus.CANCELLED
    val event = runCatching { SharedLedgerCodec.toDomainEvent(record) }.getOrNull()
    val expenseAction = event?.action as? SharedLedgerAction.ExpenseAdded
    val sharedTotal: Long = expenseAction?.debts?.sumOf { it.amountMinor } ?: 0L
    val isOutgoing: Boolean = record.actorMemberId == data.ownId
    val payerName: String = data.label(record.actorMemberId)
    val timeStr: String = formatChatTime(record.createdAtMillis, zone)
    val cardBackground: Color = if (isCancelled) {
        if (isOutgoing) Color(0xFFEDF2EE) else Color(0xFFF5F6F5)
    } else {
        if (isOutgoing) Color(0xFFE3F1E8) else CardSurface
    }
    val cardBorderColor: Color = if (isCancelled) {
        if (isOutgoing) Color(0xFFCAD4CD) else Color(0xFFD5DAD6)
    } else {
        if (isOutgoing) Color(0xFFD4E7D8) else BorderLight
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
    ) {
        if (!isOutgoing) {
            Text(
                text = payerName,
                color = Green,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
        }
        Surface(
            modifier = Modifier
                .widthIn(max = 285.dp)
                .clickable(onClick = onClick),
            shape = if (isOutgoing) RoundedCornerShape(18.dp, 4.dp, 18.dp, 18.dp) else RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp),
            color = cardBackground,
            border = BorderStroke(1.dp, cardBorderColor)
        ) {
            Column(Modifier.padding(horizontal = 13.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                SplitCardHeader(splitIndex, sharedTotal, group.currencyCode, isOutgoing, isCancelled)
                SplitCardBody(record.description, payerName, isCancelled)
                SplitCardFooter(timeStr, record.syncState, isOutgoing, hasLaterPeerEvent, isCancelled)
            }
        }
    }
}

@Composable
private fun SplitCardHeader(
    splitIndex: Int,
    sharedTotal: Long,
    currencyCode: String,
    isOutgoing: Boolean,
    isCancelled: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                Modifier.size(20.dp).background(if (isOutgoing) Color.White.copy(alpha = 0.85f) else Ui.Mint, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.DateRange,
                    contentDescription = null,
                    tint = if (isCancelled) Color(0xFF7E8B82) else Green,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = "Entry #${"%03d".format(splitIndex)}",
                color = Ink,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
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
        Text(
            text = minorMoney(sharedTotal, currencyCode),
            color = if (isCancelled) Color(0xFF7E8B82) else Green,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            textDecoration = if (isCancelled) TextDecoration.LineThrough else null
        )
    }
}

@Composable
private fun SplitCardBody(
    description: String,
    payerName: String,
    isCancelled: Boolean
) {
    Column {
        Text(
            text = description.ifBlank { "Shared expense" },
            color = if (isCancelled) Color(0xFF7E8B82) else Ink,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 18.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textDecoration = if (isCancelled) TextDecoration.LineThrough else null
        )
        Text(
            text = "Paid by $payerName",
            color = Muted,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun SplitCardFooter(
    timeStr: String,
    syncState: String,
    isOutgoing: Boolean,
    hasLaterPeerEvent: Boolean,
    isCancelled: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(timeStr, color = Muted, fontSize = 10.sp)
            DeliveryStatusIcon(syncState, isOutgoing, hasLaterPeerEvent)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = if (isCancelled) "View" else "Details",
                color = Green,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = Green,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
