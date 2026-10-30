package com.just_sri.suvidha.expenses.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.just_sri.suvidha.expenses.*

internal object AppListStyle {
    val Shape = RoundedCornerShape(14.dp)
    val Spacing = 8.dp
    val PagePadding = 20.dp
}

/** Shared list anatomy: avatar, readable title/details, and an optional value or action. */
@Composable
internal fun AppListRow(
    title: String,
    subtitle: String? = null,
    leadingText: String = title.take(1).uppercase(),
    modifier: Modifier = Modifier,
    accent: Color = Green,
    value: String? = null,
    valueLabel: String? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    Surface(
        color = CardSurface,
        shape = AppListStyle.Shape,
        modifier = modifier.fillMaxWidth().clip(AppListStyle.Shape)
            .then(if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier),
    ) {
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(30.dp).background(accent.copy(alpha = 0.09f), CircleShape), contentAlignment = Alignment.Center) {
                    Text(leadingText, color = accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(title.replace('\n', ' ').trim(), color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    subtitle?.takeIf { it.isNotBlank() }?.let {
                        Text(it, color = Muted, fontSize = 11.sp, lineHeight = 14.sp)
                    }
                }
                if (trailing != null) trailing()
                else if (onClick != null && value == null) Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = Muted, modifier = Modifier.size(16.dp))
            }
            // Values get their own line so long names, currencies and large font sizes do not collide.
            value?.let {
                Row(Modifier.fillMaxWidth().padding(start = 40.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    valueLabel?.let { label -> Text(label, modifier = Modifier.weight(1f), color = Muted, fontSize = 11.sp) }
                    Text(it, modifier = if (valueLabel == null) Modifier.fillMaxWidth() else Modifier.weight(1f), color = accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
                }
            }
        }
    }
}

@Composable
internal fun AppSectionHeader(title: String, count: Int? = null, action: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        count?.let { Text("  ·  $it", color = Muted, fontSize = 14.sp) }
        Spacer(Modifier.weight(1f))
        action?.invoke()
    }
}

@Composable
internal fun AppEmptyState(title: String, message: String, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth(), shape = AppListStyle.Shape, color = CardSurface) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(message, color = Muted, fontSize = 13.sp, lineHeight = 19.sp)
        }
    }
}
