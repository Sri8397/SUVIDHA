package com.just_sri.suvidha.expenses.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.ui.draw.clip
import com.just_sri.suvidha.expenses.BorderLight
import com.just_sri.suvidha.expenses.Canvas
import com.just_sri.suvidha.expenses.CardSurface
import com.just_sri.suvidha.expenses.Danger
import com.just_sri.suvidha.expenses.Green
import com.just_sri.suvidha.expenses.Ink
import com.just_sri.suvidha.expenses.Muted
import com.just_sri.suvidha.expenses.OffWhite

/**
 * Shared, reusable UI components for the SUVIDHA application.
 * Grouped together to avoid fragmenting each small component into its own file.
 */

// ---------------------------------------------------------------------------
// 1. Navigation & Headers
// ---------------------------------------------------------------------------

/**
 * Standardized circular back button used throughout screens and modal flows.
 */
@Composable
fun AppBackButton(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "Back",
    tint: Color = Ink,
    backgroundColor: Color = CardSurface
) {
    IconButton(
        onClick = onBack,
        modifier = modifier
            .size(40.dp)
            .background(backgroundColor, CircleShape)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = contentDescription,
            tint = tint
        )
    }
}

/**
 * Clean top bar featuring a back button, title, and optional actions on the right.
 */
@Composable
fun AppTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: (@Composable () -> Unit)? = null,
    horizontalPadding: Dp = 20.dp,
    verticalPadding: Dp = 14.dp
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppBackButton(onBack = onBack)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Ink,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = Muted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (actions != null) {
            Spacer(Modifier.width(8.dp))
            actions()
        }
    }
}

/**
 * Screen header with an uppercase category/eyebrow and bold title.
 */
@Composable
fun ScreenHeader(
    eyebrow: String,
    title: String,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
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
            color = Ink,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ---------------------------------------------------------------------------
// 2. Buttons
// ---------------------------------------------------------------------------

/**
 * Primary action button styled with brand green and rounded corners.
 */
@Composable
fun AppPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 52.dp,
    shape: Shape = RoundedCornerShape(26.dp),
    leadingIcon: (@Composable () -> Unit)? = null
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
        shape = shape,
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Outlined / Secondary action button.
 */
@Composable
fun AppSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 52.dp,
    shape: Shape = RoundedCornerShape(26.dp)
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        border = BorderStroke(1.dp, BorderLight),
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        Text(
            text = text,
            color = if (enabled) Ink else Muted,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ---------------------------------------------------------------------------
// 3. Inputs & Cards
// ---------------------------------------------------------------------------

/** Rounded, brand-styled search field shared by searchable screens. */
@Composable
fun AppSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search",
    contentDescription: String = "Search",
    trailingContent: (@Composable () -> Unit)? = null
) {
    AppTextInput(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = placeholder,
        leadingContent = {
            Icon(Icons.Outlined.Search, contentDescription = contentDescription, tint = Muted, modifier = Modifier.size(20.dp))
        },
        trailingContent = trailingContent
    )
}

/** Filled, rounded single-line input used by forms and search fields. */
@Composable
fun AppTextInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = CardSurface,
        border = BorderStroke(1.dp, BorderLight)
    ) {
        Row(
            Modifier.fillMaxWidth().height(50.dp).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingContent != null) {
                leadingContent()
                Spacer(Modifier.width(10.dp))
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = Ink,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    lineHeightStyle = LineHeightStyle(
                        alignment = LineHeightStyle.Alignment.Center,
                        trim = LineHeightStyle.Trim.Both
                    )
                ),
                cursorBrush = SolidColor(Green),
                modifier = Modifier.weight(1f).fillMaxHeight(),
                decorationBox = { inner ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                        if (value.isBlank()) Text(placeholder, color = Muted, fontSize = 15.sp)
                        inner()
                    }
                }
            )
            trailingContent?.invoke()
        }
    }
}

/**
 * Standard text field with brand-consistent border colors and rounded corners.
 */
@Composable
fun AppOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    maxLines: Int = 1,
    shape: Shape = RoundedCornerShape(16.dp),
    prefix: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    textStyle: TextStyle = TextStyle(color = Ink, fontSize = 15.sp)
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = Muted, fontSize = 14.sp) },
        singleLine = singleLine,
        maxLines = maxLines,
        shape = shape,
        prefix = prefix,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        isError = isError,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        textStyle = textStyle,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Green,
            unfocusedBorderColor = BorderLight,
            cursorColor = Green,
            focusedContainerColor = CardSurface,
            unfocusedContainerColor = CardSurface
        ),
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * Surface card container with consistent border and rounded shape.
 * Acts like a React wrapper component (`children` -> `content`).
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    backgroundColor: Color = CardSurface,
    borderColor: Color = BorderLight,
    borderWidth: Dp = 1.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val baseModifier = if (onClick != null) {
        modifier.clickable(onClick = onClick)
    } else {
        modifier
    }

    Surface(
        shape = shape,
        color = backgroundColor,
        border = BorderStroke(borderWidth, borderColor),
        modifier = baseModifier
    ) {
        content()
    }
}

/**
 * Reusable swipe container for list items (swipe right for primary action, swipe left for danger action).
 */
@Composable
fun AppSwipeItem(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    onLeftSwipe: (() -> Unit)? = null,
    leftLabel: String = "Delete",
    leftIcon: ImageVector = Icons.AutoMirrored.Outlined.ArrowBack, // placeholder default, caller usually overrides
    leftColor: Color = Danger,
    onRightSwipe: (() -> Unit)? = null,
    rightLabel: String = "Action",
    rightIcon: ImageVector = Icons.AutoMirrored.Outlined.ArrowBack,
    rightColor: Color = Green,
    threshold: Float = 0.28f,
    content: @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> {
                    if (onLeftSwipe != null) onLeftSwipe()
                    false
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    if (onRightSwipe != null) onRightSwipe()
                    false
                }
                else -> false
            }
        },
        positionalThreshold = { distance -> distance * threshold }
    )

    val isLeft = dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart
    val isRight = dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier.fillMaxWidth(),
        enableDismissFromStartToEnd = onRightSwipe != null,
        enableDismissFromEndToStart = onLeftSwipe != null,
        backgroundContent = {
            when {
                isLeft && onLeftSwipe != null -> {
                    Row(
                        Modifier
                            .fillMaxSize()
                            .clip(shape)
                            .background(leftColor)
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(leftIcon, contentDescription = leftLabel, tint = OffWhite, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(leftLabel, color = OffWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                isRight && onRightSwipe != null -> {
                    Row(
                        Modifier
                            .fillMaxSize()
                            .clip(shape)
                            .background(rightColor)
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(rightIcon, contentDescription = rightLabel, tint = OffWhite, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(rightLabel, color = OffWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                else -> Box(Modifier.fillMaxSize())
            }
        }
    ) { content() }
}

/**
 * Standardized info field card used in detail popups and item views.
 */
@Composable
fun DetailField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    bgColor: Color = Canvas,
    labelColor: Color = Muted,
    valueColor: Color = Ink
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Text(label, color = labelColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.Medium, lineHeight = 18.sp)
    }
}

