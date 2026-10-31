package com.just_sri.suvidha.expenses.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.just_sri.suvidha.expenses.*
import com.just_sri.suvidha.expenses.ui.theme.LocalSuvidhaColors

@Composable
internal fun HomeScreen(data: AppData, name: String, navigate: (String) -> Unit) {
    PageColumn {
        Column {
            Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                val isDark = LocalSuvidhaColors.current.isDark
                Text("suvidha.", fontFamily = Wordmark, color = if (isDark) Ink else Color(0xFF49634F), fontSize = 25.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp)
                IconButton({ navigate("profile") }, Modifier.size(40.dp)) { Avatar(name, primary = true) }
            }
            HorizontalDivider(color = BorderLight)
        }
        PageList {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("WELCOME BACK", color = Muted, fontSize = 11.sp, letterSpacing = 1.1.sp, fontWeight = FontWeight.SemiBold)
                    Text(name.ifBlank { "Your money, organised." }, color = Ink, fontSize = 27.sp, lineHeight = 35.sp, letterSpacing = (-0.8).sp, fontWeight = FontWeight.Bold)
                    Text("Your money, a little more organised.", color = Muted, fontSize = 13.sp)
                }
            }
            item { SectionHeading("Quick actions") }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Add expense" to "add-expense", "Expenses" to "expenses", "Friends" to "friends", "Groups" to "groups").forEachIndexed { i, (title, route) ->
                        Column(Modifier.weight(1f).clickable { navigate(route) }.padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.size(52.dp).background(if (i == 0) Green else Ui.Mint, RoundedCornerShape(17.dp)), contentAlignment = Alignment.Center) {
                                if (i == 0) Icon(Icons.Outlined.Add, null, tint = OnGreen)
                                else AppIcon(route, tint = Green)
                            }
                            Text(title, color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                        }
                    }
                }
            }
            item { SectionHeading("Recent friends & groups") }
            item {
                val recent = (data.groups.map { Recent(it.name, "Group · ${data.members.count { m -> m.groupId == it.id && m.isActive }} members", "group/${it.id}", it.updatedAtMillis, true) } +
                    data.friends.map { friend -> Recent(friend.name, "Friend · View transactions", "friend/${friend.id}",
                        maxOf(friend.updatedAt, data.splits.filter { it.friendId == friend.id }.maxOfOrNull { it.date } ?: 0,
                            data.groupTransactions[friend.id].orEmpty().maxOfOrNull { it.occurredAtMillis } ?: 0), false) }).sortedByDescending { it.date }.take(8)
                if (recent.isEmpty()) EmptyState("A place for your everyday money", "Add a friend or create a group to keep your shared expenses together.") {
                    SecondaryButton("Add your first friend", { navigate("new-friend") })
                } else Panel {
                    recent.forEachIndexed { i, entry ->
                        LedgerRow(entry.title, entry.subtitle, icon = if (entry.group) Icons.Outlined.DateRange else null,
                            onClick = { navigate(entry.route) }, divider = i < recent.lastIndex)
                    }
                }
            }
        }
    }
}

private data class Recent(val title: String, val subtitle: String, val route: String, val date: Long, val group: Boolean)
