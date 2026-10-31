package com.just_sri.suvidha.expenses.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.just_sri.suvidha.expenses.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun BackupScreen(model: AppViewModel, back: () -> Unit) {
    val context = LocalContext.current
    var mode by remember { mutableStateOf<String?>(null) }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var isPassphraseVisible by remember { mutableStateOf(false) }
    val passphraseTransformation: VisualTransformation = if (isPassphraseVisible) VisualTransformation.None else PasswordVisualTransformation()
    var source by remember { mutableStateOf<Uri?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val bytes = model.pendingBackup; model.pendingBackup = null
        if (uri != null && bytes != null) model.perform("Encrypted backup saved") {
            try { withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(bytes) } ?: error("Could not open the destination.") } }
            finally { bytes.fill(0) }
        } else bytes?.fill(0)
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { source = uri; password = ""; confirm = ""; error = null; mode = "restore" }
    }
    PageColumn {
        PageHeader("Encrypted backup", back)
        PageList {
            item { SectionHeading("Your data, kept private") }
            item { Panel {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Save a private copy of your expenses, friends, groups and identity.", color = Ink, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold)
                    Text("Your backup is encrypted with a recovery passphrase. Keep it somewhere safe; you’ll need it to restore the file.", color = Muted, fontSize = 14.sp, lineHeight = 22.sp)
                    PrimaryButton("Export backup", { password = ""; confirm = ""; error = null; mode = "export" }, enabled = !model.busy)
                    SecondaryButton("Restore backup", { open.launch(arrayOf("application/octet-stream", "application/*")) }, enabled = !model.busy)
                }
            } }
            if (model.busy) item { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { CircularProgressIndicator(Modifier.size(22.dp)); Text("Processing on this device…", color = Muted, fontSize = 13.sp) } }
        }
    }
    mode?.let { operation ->
        ConfirmDialog(if (operation == "export") "Set a recovery passphrase" else "Enter your passphrase",
            if (operation == "export") "Use at least 12 characters. Keep this phrase separately from your backup." else "Use the passphrase you chose when exporting this backup.",
            if (operation == "export") "Choose destination" else "Preview backup", { mode = null; password = ""; confirm = "" }, {
                when {
                    operation == "export" && password.length < 12 -> error = "Use at least 12 characters."
                    operation == "export" && password != confirm -> error = "Passphrases do not match."
                    password.isEmpty() -> error = "Enter your recovery passphrase."
                    else -> {
                        val secret = password.toCharArray(); val uri = source
                        password = ""; confirm = ""; mode = null
                        if (operation == "export") model.perform(onDone = { create.launch("suvidha-backup-${System.currentTimeMillis()}.suvb") }) {
                            model.pendingBackup?.fill(0)
                            model.pendingBackup = model.backups.export(secret)
                        } else model.perform {
                            try {
                                val snapshot = withContext(Dispatchers.IO) {
                                    val encrypted = context.contentResolver.openInputStream(uri ?: error("Choose a backup file."))?.use { input ->
                                        val output = java.io.ByteArrayOutputStream()
                                        val buffer = ByteArray(8192)
                                        while (true) {
                                            val count = input.read(buffer); if (count < 0) break
                                            require(output.size().toLong() + count <= EncryptedBackup.MAX_FILE_BYTES) { "Backup file is too large." }
                                            output.write(buffer, 0, count)
                                        }
                                        output.toByteArray()
                                    } ?: error("Could not open this file.")
                                    val plain = try { EncryptedBackup.decrypt(encrypted, secret) } finally { encrypted.fill(0) }
                                    try { BackupJsonCodec.decode(plain) } finally { plain.fill(0) }
                                }
                                model.restorePreview = snapshot
                            } finally { secret.fill('\u0000') }
                        }
                    }
                }
            }, model.busy, error, content = {
                InputField(password, { password = it; error = null }, "Passphrase", keyboard = KeyboardOptions(keyboardType = KeyboardType.Password), transformation = passphraseTransformation)
                if (operation == "export") InputField(confirm, { confirm = it; error = null }, "Confirm passphrase", keyboard = KeyboardOptions(keyboardType = KeyboardType.Password), transformation = passphraseTransformation)
                Row(Modifier.fillMaxWidth().clickable { isPassphraseVisible = !isPassphraseVisible }, verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(isPassphraseVisible, { isPassphraseVisible = it })
                    Text("Show passphrase", color = Ink, fontSize = 14.sp)
                }
            })
    }
    model.restorePreview?.let { snapshot ->
        val preview = BackupJsonCodec.preview(snapshot)
        ConfirmDialog("Review backup", "Created ${displayDate(preview.createdAtMillis, model)}. Restoring replaces the data currently on this device.", "Replace local data",
            { model.restorePreview = null }, { model.perform("Backup restored", { model.restorePreview = null; model.refreshIdentity() }) { model.backups.restore(snapshot) } }, model.busy,
            content = {
                Text("${preview.expenseCount} expenses · ${preview.friendCount} friends · ${preview.splitCount} splits")
                Text("${preview.groupCount} groups · ${preview.eventCount} shared updates")
                Text(if (preview.identityIncluded) "Includes your identity and group keys." else "This backup has no identity key.", color = Muted)
            })
    }
}
