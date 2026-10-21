package com.just_sri.suvidha.expenses

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Person
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
import kotlin.math.abs
import kotlin.math.roundToLong

internal enum class SplitMode {
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
    onSave: (List<SplitDraft>) -> Unit
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
        var tempNote by remember { mutableStateOf(note) }
        AlertDialog(
            onDismissRequest = { showNoteDialog = false },
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
                TextButton(onClick = {
                    note = tempNote.trim()
                    showNoteDialog = false
                }) {
                    Text("Done", color = Green, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNoteDialog = false }) {
                    Text("Cancel", color = Muted)
                }
            }
        )
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Canvas
    ) {
        if (step == SplitStep.AMOUNT_INPUT) {
            AmountInputStep(
                currency = currency,
                amountText = amountInput,
                onAmountChange = { amountInput = it },
                onBack = onBack,
                onNext = {
                    if (amountInput.toDoubleOrNull() != null && amountInput.toDouble() > 0) {
                        splitMode = SplitMode.EQUAL
                        step = SplitStep.DISTRIBUTION
                    }
                },
                onSkipToExact = {
                    splitMode = SplitMode.EXACT
                    step = SplitStep.DISTRIBUTION
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
                onSave = onSave
            )
        }
    }
}

// -------------------------------------------------------------
// STEP 1: AMOUNT INPUT WITH CUSTOM KEYPAD (Matching GPay Style)
// -------------------------------------------------------------
@Composable
private fun AmountInputStep(
    currency: String,
    amountText: String,
    onAmountChange: (String) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSkipToExact: () -> Unit
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
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = Ink
                )
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onSkipToExact) {
                Text("Exact amounts →", color = Green, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text = "Enter amount to split",
            color = Muted,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(Modifier.height(14.dp))

        // Large Display Amount
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

        // Next Button
        val isValid = (amountText.toDoubleOrNull() ?: 0.0) > 0.0
        Button(
            onClick = onNext,
            enabled = isValid,
            colors = ButtonDefaults.buttonColors(
                containerColor = Green,
                contentColor = Color.White,
                disabledContainerColor = Green.copy(alpha = 0.35f),
                disabledContentColor = Color.White.copy(alpha = 0.7f)
            ),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("Next", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(20.dp))

        // Custom Numeric Keypad
        Keypad(
            amountText = amountText,
            onAmountChange = onAmountChange
        )

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun Keypad(
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
                            .background(Color.White, RoundedCornerShape(16.dp))
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
            // Check decimal places constraint (max 2 decimal places)
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
    onSave: (List<SplitDraft>) -> Unit
) {
    val selectedFriendList = friends.filter { selectedFriends[it.id] == true }

    // Computations
    val computedTotal = when (splitMode) {
        SplitMode.EXACT -> {
            selectedFriendList.sumOf { exactAmounts[it.id]?.toDoubleOrNull() ?: 0.0 }
        }
        else -> totalAmount
    }

    // Share calculations
    val totalUnits = when (splitMode) {
        SplitMode.EQUAL -> {
            selectedFriendList.size + (if (includeMyself) 1 else 0)
        }
        SplitMode.SHARES -> {
            selectedFriendList.sumOf { friendShares[it.id] ?: 1 } + (if (includeMyself) myShares else 0)
        }
        SplitMode.EXACT -> 1
    }

    fun calculateFriendShare(friendId: String): Double {
        return when (splitMode) {
            SplitMode.EXACT -> exactAmounts[friendId]?.toDoubleOrNull() ?: 0.0
            SplitMode.EQUAL -> {
                if (totalUnits > 0) computedTotal / totalUnits else 0.0
            }
            SplitMode.SHARES -> {
                val shares = friendShares[friendId] ?: 1
                if (totalUnits > 0) (computedTotal * shares) / totalUnits else 0.0
            }
        }
    }

    val myComputedShare = when (splitMode) {
        SplitMode.EXACT -> 0.0
        SplitMode.EQUAL -> if (includeMyself && totalUnits > 0) computedTotal / totalUnits else 0.0
        SplitMode.SHARES -> if (includeMyself && totalUnits > 0) (computedTotal * myShares) / totalUnits else 0.0
    }

    Scaffold(
        containerColor = Canvas,
        contentWindowInsets = WindowInsets.statusBars,
        bottomBar = {
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    val isValidToSave = when (splitMode) {
                        SplitMode.EXACT -> {
                            selectedFriendList.isNotEmpty() && selectedFriendList.any { (exactAmounts[it.id]?.toDoubleOrNull() ?: 0.0) > 0.0 }
                        }
                        else -> {
                            computedTotal > 0 && selectedFriendList.isNotEmpty()
                        }
                    }

                    Button(
                        onClick = {
                            val sign = if (theyOweYou) 1.0 else -1.0
                            val drafts = selectedFriendList.mapNotNull { friend ->
                                val amount = calculateFriendShare(friend.id)
                                if (amount > 0.001) {
                                    val finalAmount = amount * sign
                                    SplitDraft(
                                        friendId = friend.id,
                                        amount = String.format(java.util.Locale.US, "%.2f", finalAmount),
                                        note = note.ifBlank { "Split bill" }
                                    )
                                } else null
                            }
                            if (drafts.isNotEmpty()) onSave(drafts)
                        },
                        enabled = isValidToSave,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Green,
                            contentColor = Color.White,
                            disabledContainerColor = Green.copy(alpha = 0.35f),
                            disabledContentColor = Color.White.copy(alpha = 0.7f)
                        ),
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            text = "Save split  ·  $currency ${money(computedTotal)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Top Bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.White, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = Ink
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Split expense",
                        color = Ink,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Hero Amount Card
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (splitMode == SplitMode.EXACT) "Total from friends" else "Enter amount to split",
                        color = Muted,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = splitMode != SplitMode.EXACT) { onEditAmount() }
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
                            text = money(computedTotal),
                            color = Ink,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (splitMode != SplitMode.EXACT) {
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

                    // "What's this for?" Chip
                    Surface(
                        onClick = onOpenNoteDialog,
                        color = Color.White,
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E5E2)),
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
                                fontWeight = if (note.isBlank()) FontWeight.Normal else FontWeight.SemiBold
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Edit note",
                                tint = Green,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Payer Direction Pill (They owe you vs You owe them)
                    Row(
                        modifier = Modifier
                            .background(Color.White, RoundedCornerShape(20.dp))
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
                                color = if (theyOweYou) Color.White else Muted,
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
                                color = if (!theyOweYou) Color.White else Muted,
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
                            .background(Color.White, RoundedCornerShape(16.dp))
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
                                .background(Color.White, RoundedCornerShape(16.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = includeMyself,
                                onCheckedChange = onIncludeMyselfChange,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Green,
                                    checkmarkColor = Color.White
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
                                StepperControl(
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
                            .background(Color.White, RoundedCornerShape(16.dp))
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
                        .background(Color.White, RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onFriendToggle(friend.id, it) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Green,
                            checkmarkColor = Color.White
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
                            StepperControl(
                                value = currentShares,
                                onValueChange = { onFriendSharesChange(friend.id, it) }
                            )
                        }
                        isSelected && splitMode == SplitMode.EXACT -> {
                            val currentAmount = exactAmounts[friend.id].orEmpty()
                            OutlinedTextField(
                                value = currentAmount,
                                onValueChange = { onExactAmountChange(friend.id, it) },
                                placeholder = { Text("0") },
                                prefix = { Text(currency, color = Muted, fontSize = 13.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Green,
                                    cursorColor = Green
                                ),
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
            color = if (isSelected) Color.White else Ink,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun StepperControl(
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

@Composable
private fun FriendAvatarBadge(name: String, modifier: Modifier = Modifier) {
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val palette = listOf(
        Color(0xFFE8F5E9) to Color(0xFF2E7D32),
        Color(0xFFE3F2FD) to Color(0xFF1565C0),
        Color(0xFFFFF3E0) to Color(0xFFE65100),
        Color(0xFFF3E5F5) to Color(0xFF7B1FA2),
        Color(0xFFFFEBEE) to Color(0xFFC62828),
        Color(0xFFE0F7FA) to Color(0xFF00838F),
    )
    val colorPair = palette[abs(name.hashCode()) % palette.size]
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
