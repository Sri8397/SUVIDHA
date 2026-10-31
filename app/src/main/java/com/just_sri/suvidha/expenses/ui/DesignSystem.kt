package com.just_sri.suvidha.expenses.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import com.just_sri.suvidha.expenses.R
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.just_sri.suvidha.expenses.*

internal val Inter = FontFamily(Font(R.font.inter_400, FontWeight.Normal), Font(R.font.inter_500, FontWeight.Medium), Font(R.font.inter_600, FontWeight.SemiBold), Font(R.font.inter_700, FontWeight.Bold))
internal val Wordmark = FontFamily(Font(R.font.manrope_bold, FontWeight.Bold))

internal object Ui {
    val Mint = Color(0xFFE5F2EA)
    val Neutral = Color(0xFFEFF3EC)
    val Radius = RoundedCornerShape(20.dp)
    val FieldRadius = RoundedCornerShape(16.dp)
}

@Composable
internal fun SuvidhaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(primary = Green, onPrimary = Color.White,
        primaryContainer = Ui.Mint, onPrimaryContainer = Green, background = Canvas,
        onBackground = Ink, surface = CardSurface, onSurface = Ink,
        surfaceVariant = Ui.Neutral, onSurfaceVariant = Muted,
        surfaceContainer = CardSurface, surfaceContainerLow = CardSurface,
        surfaceContainerHigh = CardSurface, surfaceContainerHighest = CardSurface,
        outlineVariant = BorderLight,
        outline = Muted, error = Danger), typography = Typography(
            bodyLarge = TextStyle(fontFamily = Inter, fontSize = 15.sp, lineHeight = 22.sp),
            bodyMedium = TextStyle(fontFamily = Inter, fontSize = 14.sp, lineHeight = 20.sp),
            labelLarge = TextStyle(fontFamily = Inter, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)),
        content = content)
}

/** Centered on tablets, edge-to-edge on phones. Insets belong to the app shell. */
@Composable
internal fun PageColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize(), content = content)
}

@Composable
internal fun PageList(modifier: Modifier = Modifier, content: LazyListScope.() -> Unit) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 16.dp, 20.dp, 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
}

@Composable
internal fun PageHeader(title: String, onBack: () -> Unit, action: (@Composable RowScope.() -> Unit)? = null) {
    Column {
        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IconButton(onBack, Modifier.size(40.dp).background(Ui.Mint, CircleShape)) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", Modifier.size(20.dp), tint = Green)
            }
            Text(title, Modifier.weight(1f), color = Ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            action?.invoke(this)
        }
        HorizontalDivider(color = BorderLight)
    }
}

@Composable
internal fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(), color = CardSurface, shape = Ui.Radius, border = BorderStroke(1.dp, BorderLight)) {
        Column(content = content)
    }
}

@Composable
internal fun SectionHeading(title: String, detail: String? = null, action: (@Composable () -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            if (detail != null) {
                val detailText: String = if (detail.all { it.isDigit() }) "· $detail" else detail
                Text(detailText, color = Muted, fontSize = 13.sp)
            }
        }
        if (action != null) {
            Spacer(Modifier.weight(1f))
            action()
        }
    }
}

@Composable
internal fun Avatar(
    name: String,
    icon: ImageVector? = null,
    primary: Boolean = false,
    modifier: Modifier = Modifier,
    showBadge: Boolean = false
): Unit {
    Box(modifier.size(40.dp).background(if (primary) Green else Ui.Mint, if (icon == null) CircleShape else RoundedCornerShape(13.dp)),
        contentAlignment = Alignment.Center) {
        if (icon != null) Icon(icon, null, Modifier.size(21.dp), tint = if (primary) Color.White else Green)
        else Text(initials(name), color = if (primary) Color.White else Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        if (showBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
                    .size(20.dp)
                    .background(Color(0xFF004625), CircleShape)
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Verified,
                    contentDescription = "Profile badge",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

internal fun initials(name: String): String = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.take(2).joinToString("") { it.take(1).uppercase() }.ifBlank { "S" }

@Composable
internal fun PreferenceRow(
    title: String,
    subtitle: String? = null,
    icon: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    divider: Boolean = false
): Unit {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (icon != null) icon()
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = title,
                    color = Ink,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 21.sp
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        color = Muted,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
            if (trailingContent != null) trailingContent()
        }
        if (divider) HorizontalDivider(color = BorderLight)
    }
}

/** One row layout for friends, groups, preferences and transaction history. */
@Composable
internal fun LedgerRow(
    title: String,
    subtitle: String? = null,
    amount: String? = null,
    amountLabel: String? = null,
    amountColor: Color = Ink,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    divider: Boolean = false,
    leading: (@Composable () -> Unit)? = null
): Unit {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (leading != null) {
                leading()
            } else {
                Avatar(title, icon)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                    Text(title, Modifier.weight(1f), color = Ink, fontSize = 14.sp, lineHeight = 21.sp,
                        fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (amount != null && amount.length <= 17) Text(amount, color = amountColor, fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
                if (amount != null && amount.length > 17) Text(amount, color = amountColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                if (!subtitle.isNullOrBlank()) Text(subtitle, color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
                if (amountLabel != null) Text(amountLabel, color = amountColor, fontSize = 12.sp)
            }
            if (trailing != null) {
                trailing()
            } else if (onClick != null && amount == null) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = Muted, modifier = Modifier.size(18.dp))
            }
        }
        if (divider) HorizontalDivider(color = BorderLight)
    }
}

@Composable
internal fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(onClick, modifier.fillMaxWidth().heightIn(min = 50.dp), enabled = enabled, shape = CircleShape,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)) {
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(onClick, modifier.fillMaxWidth().heightIn(min = 50.dp), enabled = enabled, shape = CircleShape,
        border = BorderStroke(1.dp, BorderLight), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
        Text(text, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun InputField(value: String, onChange: (String) -> Unit, placeholder: String = "", label: String? = null,
    modifier: Modifier = Modifier, multiline: Boolean = false, keyboard: KeyboardOptions = KeyboardOptions.Default,
    error: Boolean = false, transformation: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null,
    labelTrailing: (@Composable () -> Unit)? = null) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (label != null || labelTrailing != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (label != null) Text(label, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) else Spacer(Modifier.width(1.dp))
                labelTrailing?.invoke()
            }
        }
        OutlinedTextField(value, onChange, Modifier.fillMaxWidth(), placeholder = { Text(placeholder, color = Muted, fontSize = 14.sp) },
            singleLine = !multiline, minLines = if (multiline) 3 else 1, maxLines = if (multiline) 5 else 1,
            shape = Ui.FieldRadius, keyboardOptions = keyboard, isError = error, visualTransformation = transformation,
            trailingIcon = trailing, textStyle = TextStyle(color = Ink, fontSize = 16.sp, lineHeight = 22.sp),
            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = BorderLight, focusedBorderColor = Green,
                unfocusedContainerColor = CardSurface, focusedContainerColor = CardSurface))
    }
}

@Composable
internal fun SearchField(query: String, onChange: (String) -> Unit, placeholder: String) {
    OutlinedTextField(query, onChange, Modifier.fillMaxWidth(), singleLine = true, shape = Ui.FieldRadius,
        placeholder = { Text(placeholder, color = Muted, fontSize = 14.sp) },
        leadingIcon = { Icon(Icons.Outlined.Search, null, tint = Muted, modifier = Modifier.size(20.dp)) },
        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = BorderLight,
            focusedBorderColor = Green, unfocusedContainerColor = CardSurface, focusedContainerColor = CardSurface))
}

@Composable
internal fun Segments(options: List<String>, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.clip(CircleShape).background(Ui.Neutral).padding(4.dp)) {
        options.forEach { option ->
            Surface(onClick = { onSelect(option) }, color = if (selected == option) Green else Color.Transparent,
                shape = CircleShape, modifier = Modifier.weight(1f)) {
                Box(Modifier.heightIn(min = 38.dp).padding(horizontal = 10.dp, vertical = 9.dp), contentAlignment = Alignment.Center) {
                    Text(option, color = if (selected == option) Color.White else Muted, fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
internal fun FilterChips(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { value ->
            Surface(onClick = { onSelect(value) }, shape = CircleShape, color = if (value == selected) Green else CardSurface,
                border = BorderStroke(1.dp, if (value == selected) Green else BorderLight)) {
                Text(value, Modifier.padding(14.dp, 10.dp), color = if (value == selected) Color.White else Muted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
internal fun EmptyState(title: String, description: String, action: (@Composable () -> Unit)? = null) {
    Panel {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(description, color = Muted, fontSize = 13.sp, lineHeight = 20.sp)
            action?.invoke()
        }
    }
}

@Composable
internal fun ErrorText(message: String?) {
    if (message != null) Text(message, color = Danger, fontSize = 13.sp, lineHeight = 20.sp)
}

@Composable
internal fun ConfirmDialog(title: String, message: String, confirm: String, onDismiss: () -> Unit, onConfirm: () -> Unit,
    busy: Boolean = false, error: String? = null, content: (@Composable () -> Unit)? = null) {
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() }, title = { Text(title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { Text(message); content?.invoke(); ErrorText(error) } },
        confirmButton = { TextButton(onConfirm, enabled = !busy) { Text(if (busy) "Saving…" else confirm) } },
        dismissButton = { TextButton(onDismiss, enabled = !busy) { Text("Cancel") } }, containerColor = CardSurface, shape = Ui.Radius)
}

@Composable
internal fun DetailLine(label: String, value: String) {
    Column(Modifier.fillMaxWidth().background(Ui.Neutral, Ui.FieldRadius).padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = Muted, fontSize = 12.sp)
        Text(value, color = Ink, fontSize = 14.sp, lineHeight = 21.sp)
    }
}
