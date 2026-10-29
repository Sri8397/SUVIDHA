package com.just_sri.suvidha.expenses

import com.just_sri.suvidha.expenses.components.AppOutlinedTextField
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private enum class SplitMode {
    EQUAL,
    EXACT,
    SHARES
}

private enum class SplitStep {
    AMOUNT_INPUT,
    DISTRIBUTION
}

@Composable
internal fun SplitFlowScreen(
    friends: List<FriendBalance>,
    initialFriendId: String?,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onSave: (List<SplitDraft>) -> Unit,
    onSaveAndNew: (List<SplitDraft>) -> Unit = onSave
) {
    var step by remember {
        mutableStateOf(if (friends.isEmpty()) SplitStep.DISTRIBUTION else SplitStep.AMOUNT_INPUT)
    }
    var amountInput by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var showNoteDialog by remember { mutableStateOf(false) }
    var splitMode by remember { mutableStateOf(SplitMode.EQUAL) }
    var includeMyself by remember { mutableStateOf(true) }
    var myShares by remember { mutableIntStateOf(1) }

    // Map of friendId -> isSelected
    val selectedFriends = remember(friends, initialFriendId) {
        mutableStateMapOf<String, Boolean>().apply {
            friends.forEach { friend ->
                put(friend.id, initialFriendId == null || friend.id == initialFriendId)
            }
        }
    }

    // Map of friendId -> shares count (for SHARES mode)
    val friendShares = remember(friends) {
        mutableStateMapOf<String, Int>().apply {
            friends.forEach { put(it.id, 1) }
        }
    }

    // Map of friendId -> custom amount text (for EXACT mode)
    val exactAmounts = remember(friends) {
        mutableStateMapOf<String, String>().apply {
            friends.forEach { put(it.id, "") }
        }
    }

    // Direction: true = "They owe you" (positive), false = "You owe them" (negative)
    var theyOweYou by remember { mutableStateOf(true) }

    val currency = LocalCurrency.current

    BackHandler {
        if (step == SplitStep.DISTRIBUTION && friends.isNotEmpty()) {
            step = SplitStep.AMOUNT_INPUT
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
        if (step == SplitStep.AMOUNT_INPUT) {
            AmountEntryStep(
                title = "Enter amount to split",
                currency = currency,
                amountText = amountInput,
                onAmountChange = { amountInput = it },
                onBack = onBack,
                onPrimary = {
                    if ((amountInput.toDoubleOrNull() ?: 0.0) > 0.0) {
                        splitMode = SplitMode.EQUAL
                        step = SplitStep.DISTRIBUTION
                    }
                },
                trailingAction = {
                    TextButton(onClick = {
                        splitMode = SplitMode.EXACT
                        step = SplitStep.DISTRIBUTION
                    }) {
                        Text("Exact amounts →", color = Green, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }
            )
        } else {
            DistributionStep(
                currency = currency,
                totalAmount = amountInput.toDoubleOrNull() ?: 0.0,
                note = note,
                splitMode = splitMode,
                includeMyself = includeMyself,
                myShares = myShares,
                friends = friends,
                selectedFriends = selectedFriends,
                friendShares = friendShares,
                exactAmounts = exactAmounts,
                theyOweYou = theyOweYou,
                onBack = {
                    if (friends.isNotEmpty()) step = SplitStep.AMOUNT_INPUT else onBack()
                },
                onEditAmount = { step = SplitStep.AMOUNT_INPUT },
                onOpenNoteDialog = { showNoteDialog = true },
                onSplitModeChange = { splitMode = it },
                onIncludeMyselfChange = { includeMyself = it },
                onMySharesChange = { myShares = it },
                onFriendToggle = { id, selected -> selectedFriends[id] = selected },
                onFriendSharesChange = { id, count -> friendShares[id] = count },
                onExactAmountChange = { id, text -> exactAmounts[id] = text },
                onTheyOweYouChange = { theyOweYou = it },
                onSave = onSave,
                onSaveAndNew = { drafts ->
                    onSaveAndNew(drafts)
                    amountInput = ""
                    note = ""
                    splitMode = SplitMode.EQUAL
                    includeMyself = true
                    myShares = 1
                    theyOweYou = true
                    friends.forEach { friend ->
                        selectedFriends[friend.id] = initialFriendId == null || friend.id == initialFriendId
                        friendShares[friend.id] = 1
                        exactAmounts[friend.id] = ""
                    }
                    step = if (friends.isEmpty()) SplitStep.DISTRIBUTION else SplitStep.AMOUNT_INPUT
                }
            )
        }
    }
}

// -------------------------------------------------------------
// STEP 2: SPLIT DISTRIBUTION (Google Pay Style with Current Theme)
// -------------------------------------------------------------
@Composable
private fun DistributionStep(
    currency: String,
    totalAmount: Double,
    note: String,
    splitMode: SplitMode,
    includeMyself: Boolean,
    myShares: Int,
    friends: List<FriendBalance>,
    selectedFriends: Map<String, Boolean>,
    friendShares: Map<String, Int>,
    exactAmounts: Map<String, String>,
    theyOweYou: Boolean,
    onBack: () -> Unit,
    onEditAmount: () -> Unit,
    onOpenNoteDialog: () -> Unit,
    onSplitModeChange: (SplitMode) -> Unit,
    onIncludeMyselfChange: (Boolean) -> Unit,
    onMySharesChange: (Int) -> Unit,
    onFriendToggle: (String, Boolean) -> Unit,
    onFriendSharesChange: (String, Int) -> Unit,
    onExactAmountChange: (String, String) -> Unit,
    onTheyOweYouChange: (Boolean) -> Unit,
    onSave: (List<SplitDraft>) -> Unit,
    onSaveAndNew: (List<SplitDraft>) -> Unit
) {
    val selectedFriendList = friends.filter { selectedFriends[it.id] == true }

    // Computations
    val computedTotal = when (splitMode) {
        SplitMode.EXACT -> {
            selectedFriendList.sumOf { exactAmounts[it.id]?.toDoubleOrNull() ?: 0.0 }
        }
        else -> totalAmount
    }

    val totalUnits = when (splitMode) {
        SplitMode.EQUAL -> selectedFriendList.size + (if (includeMyself) 1 else 0)
        SplitMode.SHARES -> selectedFriendList.sumOf { friendShares[it.id] ?: 1 } + (if (includeMyself) myShares else 0)
        SplitMode.EXACT -> 1
    }

    fun calculateFriendShare(friendId: String): Double = when (splitMode) {
        SplitMode.EXACT -> exactAmounts[friendId]?.toDoubleOrNull() ?: 0.0
        else -> if (totalUnits > 0) (computedTotal * (if (splitMode == SplitMode.SHARES) friendShares[friendId] ?: 1 else 1)) / totalUnits else 0.0
    }

    val myComputedShare = if (includeMyself && totalUnits > 0 && splitMode != SplitMode.EXACT) {
        (computedTotal * (if (splitMode == SplitMode.SHARES) myShares else 1)) / totalUnits
    } else 0.0

    val isValidToSave = when (splitMode) {
        SplitMode.EXACT -> {
            selectedFriendList.isNotEmpty() && selectedFriendList.any { (exactAmounts[it.id]?.toDoubleOrNull() ?: 0.0) > 0.0 }
        }
        else -> {
            computedTotal > 0 && selectedFriendList.isNotEmpty()
        }
    }

    Scaffold(
        containerColor = Canvas,
        contentWindowInsets = WindowInsets.statusBars,
        bottomBar = {
            val buildDrafts: () -> List<SplitDraft> = {
                val sign = if (theyOweYou) 1.0 else -1.0
                selectedFriendList.mapNotNull { friend ->
                    val amount = calculateFriendShare(friend.id)
                    if (amount > 0.001) {
                        SplitDraft(
                            friendId = friend.id,
                            amount = String.format(java.util.Locale.US, "%.2f", amount * sign),
                            note = note.ifBlank { "Split bill" }
                        )
                    } else null
                }
            }
            FlowSaveBar(
                label = "Save & exit  ·  $currency ${money(computedTotal)}",
                enabled = isValidToSave,
                onClick = {
                    val drafts = buildDrafts()
                    if (drafts.isNotEmpty()) onSave(drafts)
                },
                secondaryLabel = "Save & add another",
                onSecondary = {
                    val drafts = buildDrafts()
                    if (drafts.isNotEmpty()) onSaveAndNew(drafts)
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
            contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + 24.dp)
        ) {
            // Top Bar
            item {
                FlowTopBar(title = "Split expense", onBack = onBack)
            }

            // Hero Amount Card
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    FlowAmountHero(
                        title = if (splitMode == SplitMode.EXACT) "Total from friends" else "Enter amount to split",
                        currency = currency,
                        amount = computedTotal,
                        note = note,
                        onOpenNoteDialog = onOpenNoteDialog,
                        onEditAmount = if (splitMode != SplitMode.EXACT) onEditAmount else null
                    )

                    Spacer(Modifier.height(14.dp))

                    // Payer Direction Pill (They owe you vs You owe them)
                    Row(
                        modifier = Modifier
                            .background(CardSurface, RoundedCornerShape(20.dp))
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val selectedBg = Green
                        val unselectedBg = Color.Transparent
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (theyOweYou) selectedBg else unselectedBg)
                                .clickable { onTheyOweYouChange(true) }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                "They owe you",
                                color = if (theyOweYou) OffWhite else Muted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (!theyOweYou) Danger else unselectedBg)
                                .clickable { onTheyOweYouChange(false) }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                "You owe them",
                                color = if (!theyOweYou) OffWhite else Muted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Split Mode Tabs (Equal, Exact, Shares)
            item {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CardSurface, RoundedCornerShape(16.dp))
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SplitTabItem(
                            title = "Equally",
                            isSelected = splitMode == SplitMode.EQUAL,
                            modifier = Modifier.weight(1f),
                            onClick = { onSplitModeChange(SplitMode.EQUAL) }
                        )
                        SplitTabItem(
                            title = "Exact (123)",
                            isSelected = splitMode == SplitMode.EXACT,
                            modifier = Modifier.weight(1f),
                            onClick = { onSplitModeChange(SplitMode.EXACT) }
                        )
                        SplitTabItem(
                            title = "By shares",
                            isSelected = splitMode == SplitMode.SHARES,
                            modifier = Modifier.weight(1f),
                            onClick = { onSplitModeChange(SplitMode.SHARES) }
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = when (splitMode) {
                            SplitMode.EQUAL -> "Split equally between selected people"
                            SplitMode.EXACT -> "Specify exact amounts for each friend (no initial amount needed)"
                            SplitMode.SHARES -> "Split based on share ratios (+ / -)"
                        },
                        color = Muted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            // "You" row (if EQUAL or SHARES mode)
            if (splitMode != SplitMode.EXACT) {
                item {
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CardSurface, RoundedCornerShape(16.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = includeMyself,
                                onCheckedChange = onIncludeMyselfChange,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Green,
                                    checkmarkColor = OffWhite
                                )
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Green.copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("You", color = Green, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("You", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                if (includeMyself) {
                                    Text(
                                        "Your share: $currency ${money(myComputedShare)}",
                                        color = Muted,
                                        fontSize = 12.sp
                                    )
                                } else {
                                    Text("Excluded from split", color = Muted, fontSize = 12.sp)
                                }
                            }

                            if (splitMode == SplitMode.SHARES && includeMyself) {
                                SplitShareStepper(
                                    value = myShares,
                                    onValueChange = onMySharesChange
                                )
                            }
                        }
                    }
                }
            }

            // Friends Header & Select All / Deselect All
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Friends (${selectedFriendList.size}/${friends.size})",
                        color = Ink,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = {
                            val allSelected = selectedFriendList.size == friends.size
                            friends.forEach { friend ->
                                onFriendToggle(friend.id, !allSelected)
                            }
                        },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = if (selectedFriendList.size == friends.size) "Deselect all" else "Select all",
                            color = Green,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (friends.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            .background(CardSurface, RoundedCornerShape(16.dp))
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No friends available", fontWeight = FontWeight.Bold, color = Ink)
                        Spacer(Modifier.height(6.dp))
                        Text("Add friends from Settings → Friends to split bills with them.", color = Muted, fontSize = 13.sp, textAlign = TextAlign.Center)
                    }
                }
            }

            // Friends list
            items(friends, key = { it.id }) { friend ->
                val isSelected = selectedFriends[friend.id] == true
                val friendShare = calculateFriendShare(friend.id)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .background(CardSurface, RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onFriendToggle(friend.id, it) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Green,
                            checkmarkColor = OffWhite
                        )
                    )
                    Spacer(Modifier.width(8.dp))

                    FriendAvatarBadge(name = friend.name)

                    Spacer(Modifier.width(12.dp))

                    Column(Modifier.weight(1f)) {
                        Text(
                            text = friend.name,
                            color = if (isSelected) Ink else Muted,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isSelected && splitMode != SplitMode.EXACT) {
                            Text(
                                text = "$currency ${money(friendShare)}",
                                color = Green,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        } else if (!isSelected) {
                            Text("Excluded", color = Muted, fontSize = 12.sp)
                        }
                    }

                    when {
                        isSelected && splitMode == SplitMode.SHARES -> {
                            val currentShares = friendShares[friend.id] ?: 1
                            SplitShareStepper(
                                value = currentShares,
                                onValueChange = { onFriendSharesChange(friend.id, it) }
                            )
                        }
                        isSelected && splitMode == SplitMode.EXACT -> {
                            val currentAmount = exactAmounts[friend.id].orEmpty()
                            AppOutlinedTextField(
                                value = currentAmount,
                                onValueChange = { onExactAmountChange(friend.id, it) },
                                placeholder = "0",
                                prefix = { Text(currency, color = Muted, fontSize = 13.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.width(110.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SplitTabItem(
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Green else Canvas)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (isSelected) OffWhite else Ink,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
internal fun SplitShareStepper(
    value: Int,
    onValueChange: (Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(Canvas, RoundedCornerShape(20.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        IconButton(
            onClick = { if (value > 1) onValueChange(value - 1) },
            enabled = value > 1,
            modifier = Modifier.size(32.dp)
        ) {
            Text("−", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (value > 1) Ink else Muted)
        }
        Text(
            text = value.toString(),
            color = Ink,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 6.dp)
        )
        IconButton(
            onClick = { onValueChange(value + 1) },
            modifier = Modifier.size(32.dp)
        ) {
            Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
        }
    }
}

private val avatarPalettes = listOf(
    Color(0xFFE8F5E9) to Color(0xFF2E7D32),
    Color(0xFFE3F2FD) to Color(0xFF1565C0),
    Color(0xFFFFF3E0) to Color(0xFFE65100),
    Color(0xFFF3E5F5) to Color(0xFF7B1FA2),
    Color(0xFFFFEBEE) to Color(0xFFC62828),
    Color(0xFFE0F7FA) to Color(0xFF00838F),
)

@Composable
private fun FriendAvatarBadge(name: String, modifier: Modifier = Modifier) {
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    // Mask the sign bit so Int.MIN_VALUE cannot produce an invalid palette index.
    val colorPair = avatarPalettes[(name.hashCode() and Int.MAX_VALUE) % avatarPalettes.size]
    Box(
        modifier = modifier
            .size(40.dp)
            .background(colorPair.first, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            color = colorPair.second,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}
