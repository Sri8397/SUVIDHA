package com.just_sri.suvidha.expenses.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.just_sri.suvidha.expenses.*
import java.time.Instant
import java.time.ZoneId

@Composable
internal fun ProfileScreen(model: AppViewModel, navigate: (String) -> Unit, back: () -> Unit) {
    var editing by remember { mutableStateOf(false) }
    var showQr by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf(model.profileName) }
    val context = LocalContext.current
    PageColumn {
        PageHeader("Profile", back)
        PageList {
            item {
                Panel {
                    Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Avatar(model.profileName, primary = true, modifier = Modifier.size(56.dp))
                            Column(Modifier.weight(1f)) {
                                Text(model.profileName.ifBlank { "Your profile" }, color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                TextButton({ editing = true }, contentPadding = PaddingValues(0.dp)) { Text(if (model.profileName.isBlank()) "Add your name" else "Edit name", fontSize = 12.sp) }
                            }
                        }
                        Text("PUBLIC ID", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                        SelectionContainer { Text(model.publicId ?: "Preparing your identity…", color = Muted, fontSize = 12.sp, fontFamily = FontFamily.Monospace, lineHeight = 19.sp) }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SecondaryButton("Copy ID", {
                                model.publicId?.let { (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Suvidha public ID", it)); model.message = "Public ID copied" }
                            }, Modifier.weight(1f), enabled = model.publicId != null)
                            SecondaryButton("Share", { showQr = true }, Modifier.weight(1f), enabled = model.publicId != null)
                        }
                    }
                }
            }
            item { SectionHeading("Your preferences") }
            item { Panel {
                LedgerRow("Currency", supportedCurrencies.find { it.first == model.currency }?.second ?: model.currency, amount = currencyCodeFor(model.currency), onClick = { navigate("currency") }, divider = true)
                LedgerRow("Time zone", "${model.zone.id.replace('_', ' ')} · ${model.zone.rules.getOffset(Instant.now())}", amount = if (model.timezone == "AUTO") "Auto" else null, onClick = { navigate("timezone") }, divider = true)
                LedgerRow("Categories", "Organise your spending", amount = "${model.categories.size} tags", onClick = { navigate("categories") })
            } }
            item { SectionHeading("Data & privacy", "Always local") }
            item { Panel {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Encrypted backup", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text("Save a private copy of your expenses and groups, or restore a backup.", color = Muted, fontSize = 13.sp, lineHeight = 20.sp)
                    SecondaryButton("Export or restore backup", { navigate("backup") })
                }
            } }
            item { SectionHeading("Group invitations") }
            item { Panel {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Make your profile available for group invitations. Share your public ID, then sync to receive invitations.", color = Muted, fontSize = 13.sp, lineHeight = 20.sp)
                    SecondaryButton(if (model.busy) "Working…" else "Enable group invitations", { model.perform { model.message = model.peers.publishInviteKey() } }, enabled = !model.busy && model.publicId != null)
                }
            } }
        }
    }
    if (editing) ConfirmDialog("Your name", "This name is saved on your device.", "Save", { editing = false }, { model.updateProfileName(name); editing = false },
        content = { InputField(name, { name = it.take(60) }, "How should we greet you?") })
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
