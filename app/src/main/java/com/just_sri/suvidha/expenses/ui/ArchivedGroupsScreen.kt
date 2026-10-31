package com.just_sri.suvidha.expenses.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.just_sri.suvidha.expenses.*

@Composable
internal fun ArchivedGroupsScreen(data: AppData, model: AppViewModel, back: () -> Unit) {
    val archived: List<SharedGroupEntity> = data.groups.filter { model.archivedGroupIds.contains(it.id) }.sortedByDescending { it.updatedAtMillis }
    PageColumn {
        PageHeader("Archived groups", back)
        PageList {
            item { SectionHeading("Archived", "${archived.size}") }
            if (archived.isEmpty()) item { EmptyState("No archived groups.", "Groups you archive from the Groups list appear here.") }
            item {
                if (archived.isNotEmpty()) Panel { archived.forEachIndexed { i, group ->
                    LedgerRow(group.name, "${data.members.count { it.groupId == group.id && it.isActive }} members · ${group.currencyCode}",
                        icon = Icons.Outlined.DateRange, divider = i < archived.lastIndex,
                        trailing = { UnarchiveButton { model.unarchiveGroups(setOf(group.id)) } })
                } }
            }
        }
    }
}

@Composable
private fun UnarchiveButton(onClick: () -> Unit) {
    OutlinedButton(onClick, Modifier.heightIn(min = 40.dp), shape = CircleShape, border = BorderStroke(1.dp, BorderLight),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
        Text("Unarchive", color = Green, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
