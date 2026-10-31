package com.just_sri.suvidha.expenses.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.just_sri.suvidha.expenses.R
import com.just_sri.suvidha.expenses.*
import java.time.Instant
import java.time.ZoneId

@Composable
internal fun ProfileScreen(model: AppViewModel, navigate: (String) -> Unit, back: () -> Unit) {
    var editingName by rememberSaveable { mutableStateOf(false) }
    var tempName by rememberSaveable { mutableStateOf(model.profileName) }
    var showQr by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    PageColumn {
        PageHeader("Profile", back)
        PageList {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = CardSurface,
                    border = BorderStroke(1.dp, BorderLight)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 22.dp, start = 14.dp, end = 14.dp, bottom = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(78.dp)
                                .background(Color(0xFFD0EBD6), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials(model.profileName),
                                color = Color(0xFF1E5E3A),
                                fontSize = 27.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = (-0.9).sp
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(x = 2.dp, y = 1.dp)
                                    .size(28.dp)
                                    .background(Color(0xFF004625), CircleShape)
                                    .border(2.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Verified,
                                    contentDescription = "Profile badge",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = model.profileName.ifBlank { "Your profile" },
                            color = Color(0xFF1A211B),
                            fontSize = 19.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 26.sp,
                            letterSpacing = (-0.5).sp
                        )
                        Spacer(Modifier.height(17.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFEFF3EC), RoundedCornerShape(13.dp))
                                .border(1.dp, BorderLight, RoundedCornerShape(13.dp))
                                .padding(start = 10.dp, top = 6.dp, end = 8.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "ID",
                                    color = Color(0xFF647368),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.3.sp
                                )
                                SelectionContainer {
                                    Text(
                                        text = model.publicId ?: "Preparing your identity…",
                                        color = Color(0xFF1A211B),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    onClick = {
                                        model.publicId?.let {
                                            (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Suvidha public ID", it))
                                            model.message = "Public ID copied"
                                        }
                                    },
                                    shape = CircleShape,
                                    color = CardSurface,
                                    border = BorderStroke(1.dp, BorderLight),
                                    enabled = model.publicId != null
                                ) {
                                    Row(
                                        modifier = Modifier.height(34.dp).padding(horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.ContentCopy,
                                            contentDescription = null,
                                            modifier = Modifier.size(13.dp),
                                            tint = Color(0xFF00512D)
                                        )
                                        Text(
                                            text = "Copy",
                                            color = Color(0xFF1A211B),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                Surface(
                                    onClick = { showQr = true },
                                    shape = CircleShape,
                                    color = CardSurface,
                                    border = BorderStroke(1.dp, BorderLight),
                                    enabled = model.publicId != null
                                ) {
                                    Row(
                                        modifier = Modifier.height(34.dp).padding(horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Share,
                                            contentDescription = null,
                                            modifier = Modifier.size(13.dp),
                                            tint = Color(0xFF00512D)
                                        )
                                        Text(
                                            text = "Share",
                                            color = Color(0xFF1A211B),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item { SectionHeading("Profile") }
            item {
                Panel {
                    LedgerRow(
                        title = "Name",
                        subtitle = "Tap to edit your display name",
                        leading = {
                            Box(
                                modifier = Modifier
                                    .size(width = 34.dp, height = 36.dp)
                                    .background(Color(0xFFE5F2EA), RoundedCornerShape(11.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(19.dp),
                                    tint = Color(0xFF1E5E3A)
                                )
                            }
                        },
                        trailing = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(model.profileName.ifBlank { "Add name" }, color = Muted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = Muted, modifier = Modifier.size(18.dp))
                            }
                        },
                        onClick = {
                            tempName = model.profileName
                            editingName = true
                        }
                    )
                }
            }
            item { SectionHeading("Your preferences") }
            item { Panel {
                LedgerRow(
                    title = "Currency",
                    subtitle = supportedCurrencies.find { it.first == model.currency }?.second ?: model.currency,
                    onClick = { navigate("currency") },
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(currencyCodeFor(model.currency), color = Muted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = Muted, modifier = Modifier.size(18.dp))
                        }
                    },
                    divider = true
                )
                LedgerRow(
                    title = "Time zone",
                    subtitle = "${model.zone.id.replace('_', ' ')} · ${model.zone.rules.getOffset(Instant.now())}",
                    onClick = { navigate("timezone") },
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(if (model.timezone == "AUTO") "Auto" else timezoneOption(model.timezone).city, color = Muted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = Muted, modifier = Modifier.size(18.dp))
                        }
                    },
                    divider = true
                )
                LedgerRow(
                    title = "Categories",
                    subtitle = "Organise your spending",
                    onClick = { navigate("categories") },
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("${model.categories.size} tags", color = Muted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = Muted, modifier = Modifier.size(18.dp))
                        }
                    },
                    divider = true
                )
                LedgerRow(
                    title = "Enable group invitation",
                    subtitle = if (model.groupInvitationsEnabled) "Active · Friends can invite you" else "Allow friends to invite you via your public ID",
                    leading = {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFE5F2EA), RoundedCornerShape(13.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_friends),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = Color(0xFF1E5E3A)
                            )
                        }
                    },
                    trailing = {
                        Switch(
                            checked = model.groupInvitationsEnabled,
                            onCheckedChange = { model.toggleGroupInvitations(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF004625),
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color(0xFFD1DBD3),
                                uncheckedBorderColor = Color.Transparent,
                                checkedBorderColor = Color.Transparent
                            )
                        )
                    }
                )
            } }
            item { SectionHeading("About") }
            item {
                Panel {
                    LedgerRow(
                        title = "Version number",
                        onClick = { model.message = "Suvidha v1.0.4 (18) · Up to date" },
                        leading = {
                            Box(
                                modifier = Modifier
                                    .size(34.dp, 36.dp)
                                    .background(Color(0xFFEAF0E8), RoundedCornerShape(11.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.Info, null, Modifier.size(19.dp), tint = Color(0xFF628467))
                            }
                        },
                        trailing = {
                            Text("18", color = Muted, fontSize = 13.sp, fontWeight = FontWeight.Normal)
                        },
                        divider = true
                    )
                    LedgerRow(
                        title = "Release version",
                        onClick = { model.message = "Suvidha v1.0.4 (18) · Up to date" },
                        leading = {
                            Box(
                                modifier = Modifier
                                    .size(34.dp, 36.dp)
                                    .background(Color(0xFFE5F2EA), RoundedCornerShape(11.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.CheckCircle, null, Modifier.size(19.dp), tint = Color(0xFF1E5E3A))
                            }
                        },
                        trailing = {
                            Text("1.0.4", color = Muted, fontSize = 13.sp, fontWeight = FontWeight.Normal)
                        }
                    )
                }
            }
            item { SectionHeading("Data & privacy", "Always local") }
            item { Panel {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Encrypted backup", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text("Save a private copy of your expenses and groups, or restore a backup.", color = Muted, fontSize = 13.sp, lineHeight = 20.sp)
                    SecondaryButton("Export or restore backup", { navigate("backup") })
                }
            } }
        }
    }
    if (editingName) {
        AlertDialog(
            onDismissRequest = { editingName = false },
            title = { Text("Your name", fontSize = 20.sp, fontWeight = FontWeight.SemiBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("This name will be visible to your friends and groups.", color = Muted, fontSize = 13.sp, lineHeight = 20.sp)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = tempName,
                            onValueChange = { if (it.length <= 50) tempName = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("e.g. Srikant Agrawal", color = Muted, fontSize = 14.sp) },
                            singleLine = true,
                            maxLines = 1,
                            shape = Ui.FieldRadius,
                            textStyle = TextStyle(color = Ink, fontSize = 15.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = BorderLight,
                                focusedBorderColor = Green,
                                unfocusedContainerColor = CardSurface,
                                focusedContainerColor = CardSurface
                            )
                        )
                        Text(
                            text = "${tempName.length}/50",
                            color = Muted,
                            fontSize = 11.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { model.updateProfileName(tempName); editingName = false }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { editingName = false }) { Text("Cancel") } },
            containerColor = CardSurface,
            shape = Ui.Radius
        )
    }
    if (showQr && model.publicId != null) {
        QrCodeDialog(
            name = model.profileName,
            publicId = model.publicId!!,
            onDismiss = { showQr = false },
            onShare = {
                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, model.publicId)
                }, "Share public ID"))
            },
            onCopy = {
                (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Suvidha public ID", model.publicId))
                model.message = "Public ID copied"
            }
        )
    }
}

@Composable
internal fun CurrencyScreen(model: AppViewModel, back: () -> Unit) {
    var search by rememberSaveable { mutableStateOf("") }
    val visible = supportedCurrencies.filter { (symbol, label) -> label.contains(search, true) || currencyCodeFor(symbol).contains(search, true) }
    PageColumn {
        PageHeader("Currency", back)
        PageList {
            item { SearchField(search, { search = it }, "Search currencies") }
            item { Text("Used for personal expenses and new groups. Existing groups keep their currency.", color = Muted, fontSize = 13.sp) }
            itemsIndexed(visible, key = { _, item -> item.first }) { _, (symbol, label) ->
                Panel { LedgerRow(label, currencyCodeFor(symbol), onClick = { model.updateCurrency(symbol); back() }, trailing = {
                    RadioButton(symbol == model.currency, { model.updateCurrency(symbol); back() })
                }) }
            }
        }
    }
}

@Composable
internal fun TimezoneScreen(model: AppViewModel, back: () -> Unit) {
    var search by rememberSaveable { mutableStateOf("") }
    val visible = remember(search) { timezoneIds.filter { it.replace('_', ' ').contains(search.trim(), true) } }
    PageColumn {
        PageHeader("Time zone", back)
        PageList {
            item { SearchField(search, { search = it }, "Search cities or time zones") }
            item { Panel { LedgerRow("Automatic", "Use your device time zone", onClick = { model.updateTimezone("AUTO"); back() }, trailing = { RadioButton(model.timezone == "AUTO", { model.updateTimezone("AUTO"); back() }) }) } }
            itemsIndexed(visible, key = { _, zone -> zone }) { _, zone ->
                val option = timezoneOption(zone)
                Panel { LedgerRow(option.city, "${option.continent} · ${option.offsetString}", onClick = { model.updateTimezone(zone); back() }, trailing = { RadioButton(model.timezone == zone, { model.updateTimezone(zone); back() }) }) }
            }
            if (visible.isEmpty()) item { EmptyState("No time zones found", "Try another city or region.") }
        }
    }
}

@Composable
internal fun CategoriesScreen(model: AppViewModel, back: () -> Unit) {
    var newName by rememberSaveable { mutableStateOf("") }
    var editing by remember { mutableStateOf<String?>(null) }
    var replacement by remember { mutableStateOf("") }
    var removing by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    fun valid(value: String, old: String? = null): Boolean {
        if (value.isBlank()) { error = "Enter a category name."; return false }
        if (value.contains('|') || model.categories.any { it != old && it.equals(value.trim(), true) }) { error = "Choose a unique category name without |."; return false }
        return true
    }
    PageColumn {
        PageHeader("Categories", back)
        PageList {
            item { Text("Choose the categories you use to organise your spending.", color = Muted, fontSize = 13.sp) }
            item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                InputField(newName, { newName = it.take(30); error = null }, "New category", modifier = Modifier.weight(1f))
                TextButton({ if (valid(newName)) { model.updateCategories(model.categories + newName.trim()); newName = "" } }) { Text("Add") }
            } }
            item { ErrorText(error) }
            item { Panel { model.categories.forEachIndexed { i, category -> LedgerRow(category, if (category == "Other") "Default category" else null,
                divider = i < model.categories.lastIndex, onClick = if (category != "Other") {{ editing = category; replacement = category; error = null }} else null,
                trailing = if (category != "Other") {{ TextButton({ removing = category }) { Text("Remove", color = Danger, fontSize = 12.sp) } }} else null) } } }
        }
    }
    editing?.let { old -> ConfirmDialog("Edit category", "Past records keep their original category.", "Save", { editing = null }, {
        if (valid(replacement, old)) { model.updateCategories(model.categories.map { if (it == old) replacement.trim() else it }); editing = null }
    }, error = error, content = { InputField(replacement, { replacement = it.take(30); error = null }) }) }
    removing?.let { old -> ConfirmDialog("Remove category?", "Remove $old from your choices? Past records keep their category.", "Remove", { removing = null }, {
        model.updateCategories(model.categories.filterNot { it == old }); removing = null
    }) }
}
