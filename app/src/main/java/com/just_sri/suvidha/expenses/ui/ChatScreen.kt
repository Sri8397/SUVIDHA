package com.just_sri.suvidha.expenses.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.just_sri.suvidha.expenses.*
import com.just_sri.suvidha.expenses.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun ChatScreen(
    group: SharedGroupEntity,
    data: AppData,
    model: AppViewModel,
    navigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var messageText: String by rememberSaveable(group.id) { mutableStateOf("") }
    val chatEvents: List<SharedLedgerEventRecord> = remember(data.events, group.id) {
        data.events.filter { it.groupId == group.id && (it.actionType == "chat_message" || it.actionType == "expense_added") }.sortedBy { it.createdAtMillis }
    }
    val listState: LazyListState = rememberLazyListState()
    val members = remember(data.members, group.id) { data.members.filter { it.groupId == group.id } }
    var isActiveMember: Boolean by remember(group.id) { mutableStateOf(true) }
    LaunchedEffect(group.id, data.members) { isActiveMember = model.canPostMessage(group.id) }
    val canSplit: Boolean = isActiveMember && members.count { it.isActive } > 1 && data.ownId != null && !model.busy
    LaunchedEffect(chatEvents.size) {
        if (chatEvents.isNotEmpty()) {
            listState.animateScrollToItem(chatEvents.lastIndex)
        }
    }
    Column(modifier = modifier) {
        if (chatEvents.isEmpty()) {
            EmptyChatPlaceholder(Modifier.weight(1f).fillMaxWidth())
        } else {
            ChatMessageList(
                chatEvents = chatEvents,
                data = data,
                model = model,
                group = group,
                listState = listState,
                navigate = navigate,
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        }
        ChatComposer(
            messageText = messageText,
            onMessageChange = { text: String -> if (text.length <= 2000) messageText = text },
            onSendMessage = {
                val trimmedText: String = messageText.trim()
                if (trimmedText.isNotBlank()) {
                    messageText = ""
                    model.sendChatMessage(group.id, trimmedText)
                }
            },
            onAddSplit = { navigate("group-split/${group.id}") },
            canSplit = canSplit,
            canSend = isActiveMember && messageText.isNotBlank() && !model.busy
        )
    }
}

@Composable
private fun EmptyChatPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Your group starts here",
                color = Green,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Send a message or tap + to split an expense with your group.",
                color = Muted,
                fontSize = 13.sp,
                lineHeight = 21.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ChatMessageList(
    chatEvents: List<SharedLedgerEventRecord>,
    data: AppData,
    model: AppViewModel,
    group: SharedGroupEntity,
    listState: LazyListState,
    navigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        itemsIndexed(chatEvents, key = { _, record -> record.id }) { index: Int, record: SharedLedgerEventRecord ->
            val currentDay: LocalDate = Instant.ofEpochMilli(record.createdAtMillis).atZone(model.zone).toLocalDate()
            val prevDay: LocalDate? = if (index > 0) Instant.ofEpochMilli(chatEvents[index - 1].createdAtMillis).atZone(model.zone).toLocalDate() else null
            if (prevDay == null || prevDay != currentDay) {
                ChatDayDivider(record.createdAtMillis, model.zone)
            }
            val hasLaterPeerEvent: Boolean = chatEvents.drop(index + 1).any { it.actorMemberId != data.ownId }
            val startsRun: Boolean = prevDay != currentDay || chatEvents[index - 1].actorMemberId != record.actorMemberId
            when (record.actionType) {
                "chat_message" -> ChatMessageBubble(record, data, model.zone, hasLaterPeerEvent, startsRun)
                "expense_added" -> {
                    val splitIndex: Int = chatEvents.filter { it.actionType == "expense_added" }.indexOf(record) + 1
                    val status: SplitStatus = resolveSplitStatus(record, data.events)
                    SplitExpenseCard(
                        record = record,
                        status = status,
                        data = data,
                        group = group,
                        zone = model.zone,
                        hasLaterPeerEvent = hasLaterPeerEvent,
                        splitIndex = splitIndex,
                        startsRun = startsRun,
                        onClick = { navigate("split-detail/${group.id}/${record.id}") }
                    )
                }
            }
        }
    }
}

/** Only the first bubble of a sender's run gets the pointed corner; follow-ups are fully rounded. */
internal fun chatBubbleShape(isOutgoing: Boolean, startsRun: Boolean): RoundedCornerShape = when {
    !startsRun -> RoundedCornerShape(18.dp)
    isOutgoing -> RoundedCornerShape(18.dp, 4.dp, 18.dp, 18.dp)
    else -> RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp)
}

@Composable
private fun ChatDayDivider(timestamp: Long, zone: ZoneId) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = Ui.Neutral
        ) {
            Text(
                formatDayPill(timestamp, zone),
                color = Muted,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun ChatMessageBubble(
    record: SharedLedgerEventRecord,
    data: AppData,
    zone: ZoneId,
    hasLaterPeerEvent: Boolean,
    startsRun: Boolean
) {
    val event = runCatching { SharedLedgerCodec.toDomainEvent(record) }.getOrNull()
    val chatText: String = (event?.action as? SharedLedgerAction.ChatMessage)?.text ?: record.description
    val isOutgoing: Boolean = record.actorMemberId == data.ownId
    val senderName: String = data.label(record.actorMemberId)
    val timeStr: String = formatChatTime(record.createdAtMillis, zone)
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
    ) {
        if (!isOutgoing && startsRun) {
            Text(
                text = senderName,
                color = Green,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
        }
        val isDark = com.just_sri.suvidha.expenses.ui.theme.LocalSuvidhaColors.current.isDark
        val outgoingBg = if (isDark) Color(0xFF1C2E24) else Color(0xFFE3F1E8)
        val outgoingBorder = if (isDark) Color(0xFF244131) else Color(0xFFD4E7D8)
        Surface(
            modifier = Modifier.widthIn(max = 285.dp),
            shape = chatBubbleShape(isOutgoing, startsRun),
            color = if (isOutgoing) outgoingBg else CardSurface,
            border = BorderStroke(1.dp, if (isOutgoing) outgoingBorder else BorderLight)
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(
                    text = chatText,
                    color = Ink,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(3.dp))
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = timeStr,
                        color = Muted,
                        fontSize = 10.sp
                    )
                    DeliveryStatusIcon(record.syncState, isOutgoing, hasLaterPeerEvent)
                }
            }
        }
    }
}

@Composable
private fun ChatComposer(
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onAddSplit: () -> Unit,
    canSplit: Boolean,
    canSend: Boolean
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Canvas,
        border = BorderStroke(1.dp, BorderLight)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onAddSplit,
                modifier = Modifier.size(42.dp).background(Ui.Mint, CircleShape),
                enabled = canSplit
            ) {
                Icon(Icons.Outlined.Add, "Add split", tint = Green, modifier = Modifier.size(22.dp))
            }
            OutlinedTextField(
                value = messageText,
                onValueChange = onMessageChange,
                modifier = Modifier.weight(1f).heightIn(min = 44.dp, max = 120.dp),
                placeholder = { Text("Message", color = Muted, fontSize = 14.sp) },
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = BorderLight,
                    focusedBorderColor = Green,
                    unfocusedContainerColor = CardSurface,
                    focusedContainerColor = CardSurface
                ),
                singleLine = false,
                minLines = 1,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                textStyle = TextStyle(color = Ink, fontSize = 14.sp, fontFamily = Inter, lineHeight = 20.sp)
            )
            IconButton(
                onClick = {
                    onSendMessage()
                    keyboard?.hide()
                    focusManager.clearFocus()
                },
                modifier = Modifier.size(42.dp).background(if (canSend) Green else Ui.Neutral, CircleShape),
                enabled = canSend
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_send),
                    contentDescription = "Send message",
                    tint = if (canSend) OnGreen else Muted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun formatDayPill(timestamp: Long, zone: ZoneId): String {
    val date: LocalDate = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
    val today: LocalDate = LocalDate.now(zone)
    return when {
        date == today -> "Today"
        date == today.minusDays(1) -> "Yesterday"
        date.year == today.year -> DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()).withZone(zone).format(Instant.ofEpochMilli(timestamp))
        else -> DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()).withZone(zone).format(Instant.ofEpochMilli(timestamp))
    }
}

internal fun formatChatTime(timestamp: Long, zone: ZoneId): String {
    return DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()).withZone(zone).format(Instant.ofEpochMilli(timestamp))
}

@Composable
internal fun DeliveryStatusIcon(syncState: String, isOutgoing: Boolean, hasLaterPeerEvent: Boolean) {
    if (!isOutgoing) return
    when {
        syncState == "failed" -> {
            Icon(
                painter = painterResource(R.drawable.ic_status_failed),
                contentDescription = "Failed",
                tint = Danger,
                modifier = Modifier.size(13.dp)
            )
        }
        syncState in listOf("local", "pending") -> {
            Icon(
                painter = painterResource(R.drawable.ic_clock),
                contentDescription = "Pending / Sending",
                tint = Muted,
                modifier = Modifier.size(13.dp)
            )
        }
        syncState == "received" || hasLaterPeerEvent -> {
            Icon(
                painter = painterResource(R.drawable.ic_tick_double),
                contentDescription = "Delivered",
                tint = Green,
                modifier = Modifier.size(16.dp)
            )
        }
        else -> {
            Icon(
                painter = painterResource(R.drawable.ic_tick_single),
                contentDescription = "Sent",
                tint = Muted,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
