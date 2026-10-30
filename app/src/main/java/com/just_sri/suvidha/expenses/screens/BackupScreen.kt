package com.just_sri.suvidha.expenses.screens

import android.Manifest
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.just_sri.suvidha.expenses.AppBackupSnapshot
import com.just_sri.suvidha.expenses.BackupFormatException
import com.just_sri.suvidha.expenses.BackupJsonCodec
import com.just_sri.suvidha.expenses.BackupPreview
import com.just_sri.suvidha.expenses.BorderLight
import com.just_sri.suvidha.expenses.CardSurface
import com.just_sri.suvidha.expenses.Danger
import com.just_sri.suvidha.expenses.EncryptedBackup
import com.just_sri.suvidha.expenses.ExpenseBackupRepository
import com.just_sri.suvidha.expenses.ExpenseDatabase
import com.just_sri.suvidha.expenses.NostrIdentityStore
import com.just_sri.suvidha.expenses.EncryptedMlsGroupStateStore
import com.just_sri.suvidha.expenses.EncryptedKeyPackageBundleStore
import com.just_sri.suvidha.expenses.BackupMlsGroupState
import com.just_sri.suvidha.expenses.Green
import com.just_sri.suvidha.expenses.Ink
import com.just_sri.suvidha.expenses.Muted
import com.just_sri.suvidha.expenses.R
import com.just_sri.suvidha.expenses.SyncNotificationHelper
import com.just_sri.suvidha.expenses.components.AppPrimaryButton
import com.just_sri.suvidha.expenses.components.AppSecondaryButton
import com.just_sri.suvidha.expenses.components.AppTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Arrays
import java.util.Base64

@Composable
internal fun BackupManagementPage(database: ExpenseDatabase, modifier: Modifier = Modifier, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember(database) { ExpenseBackupRepository(database) }
    val identityStore = remember(context) { NostrIdentityStore(context.applicationContext) }
    val mlsStore = remember(context) { EncryptedMlsGroupStateStore(context.applicationContext) }
    val keyPackageStore = remember(context) { EncryptedKeyPackageBundleStore(context.applicationContext) }
    var exportDialog by remember { mutableStateOf(false) }
    var importDialog by remember { mutableStateOf(false) }
    var passphrase by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var importUri by remember { mutableStateOf<Uri?>(null) }
    var fileToSave by remember { mutableStateOf<ByteArray?>(null) }
    var pending by remember { mutableStateOf<AppBackupSnapshot?>(null) }
    var preview by remember { mutableStateOf<BackupPreview?>(null) }
    var busy by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    val notifyUser: (String, String) -> Unit = { title, msg ->
        SyncNotificationHelper.showBackupNotification(context, title, msg)
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
    }

    val createFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val encrypted = fileToSave
        fileToSave = null
        if (uri != null && encrypted != null) scope.launch {
            busy = true
            try {
                withContext(Dispatchers.IO) {
                    val output = context.contentResolver.openOutputStream(uri, "wt") ?: error("Could not open selected destination.")
                    output.use { it.write(encrypted) }
                }
                notifyUser("Encrypted Backup", "Encrypted backup saved. Store its passphrase separately.")
            } catch (_: Exception) {
                notifyUser("Backup Failed", "Could not save the backup file.")
            }
            finally { Arrays.fill(encrypted, 0); busy = false }
        } else encrypted?.let { Arrays.fill(it, 0) }
    }
    val openFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            importUri = uri; passphrase = ""; importDialog = true
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().then(modifier).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        AppTopBar(
            title = "Encrypted backup",
            onBack = onBack,
            horizontalPadding = 20.dp,
            verticalPadding = 12.dp,
        )
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("BACKUP SETTINGS", color = Green, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Text(
                    "Create a backup file or restore one on this device.",
                    color = Muted,
                    fontSize = 16.sp,
                    lineHeight = 23.sp,
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = CardSurface,
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, BorderLight),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("End-to-end encrypted", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Your recovery passphrase stays with you. SUVIDHA does not save it.",
                        color = Muted,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    )
                    Text(
                        "Includes expenses, friends, splits, local groups, members, and group event history. App preferences and phone contacts are not included.",
                        color = Ink,
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                    )
                    Text(
                        "Keep the passphrase separately. Without it, the backup cannot be restored.",
                        color = Muted,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AppPrimaryButton(
                    text = "Create encrypted backup",
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            !SyncNotificationHelper.hasNotificationPermission(context)
                        ) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        passphrase = ""; confirm = ""; exportDialog = true
                    },
                    enabled = !busy,
                    height = 56.dp,
                )
                AppSecondaryButton(
                    text = "Restore from backup",
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            !SyncNotificationHelper.hasNotificationPermission(context)
                        ) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        openFile.launch(arrayOf("application/octet-stream", "application/*"))
                    },
                    enabled = !busy,
                    height = 56.dp,
                )
            }

            if (busy) Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = Green, modifier = Modifier.height(22.dp))
                Text("Processing on this device…", color = Muted, fontSize = 14.sp)
            }
        }
    }

    if (exportDialog) {
        var passwordsVisible by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { exportDialog = false; passphrase = ""; confirm = "" },
            title = { Text("Set a recovery passphrase") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Use at least 12 characters; a longer phrase is easier to remember and harder to guess.")
                    PasswordField(passphrase, { passphrase = it }, "Passphrase", passwordsVisible, { passwordsVisible = it })
                    PasswordField(confirm, { confirm = it }, "Confirm passphrase", passwordsVisible, { passwordsVisible = it })
                    when {
                        confirm.isNotEmpty() && passphrase != confirm -> Text("Passphrases do not match.", color = Danger, fontSize = 13.sp)
                        passphrase.isNotEmpty() && passphrase.length < 12 -> Text("Use at least 12 characters.", color = Muted, fontSize = 13.sp)
                        passphrase.length >= 12 && passphrase == confirm -> Text("Passphrases match.", color = Green, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !busy && passphrase.length >= 12 && passphrase == confirm,
                    onClick = {
                        val secret = passphrase.toCharArray(); passphrase = ""; confirm = ""; exportDialog = false
                        scope.launch {
                            busy = true
                            try {
                                val snapshot = withContext(Dispatchers.IO) {
                                    val identity = identityStore.exportPrivateKeyForEncryptedBackup()
                                    val mls = mlsStore.exportAllForRecovery()
                                    val transport = mlsStore.transportStore.exportForRecovery()
                                    val keyPackage = keyPackageStore.load()
                                    try {
                                        repo.createSnapshot(
                                            identityPrivateKeyHex = identity?.toLowerHex(),
                                            mlsGroupStates = mls.map { (id, state) -> BackupMlsGroupState(id, Base64.getEncoder().encodeToString(state)) },
                                            transportStateBase64 = Base64.getEncoder().encodeToString(transport),
                                            keyPackageBundleBase64 = keyPackage?.let { Base64.getEncoder().encodeToString(it) },
                                        )
                                    } finally {
                                        identity?.fill(0)
                                        mls.values.forEach { it.fill(0) }
                                        transport.fill(0)
                                        keyPackage?.fill(0)
                                    }
                                }
                                val plaintext = BackupJsonCodec.encode(snapshot)
                                val encrypted = try { withContext(Dispatchers.Default) { EncryptedBackup.encrypt(plaintext, secret) } }
                                finally { Arrays.fill(plaintext, 0) }
                                fileToSave = encrypted
                                createFile.launch("suvidha-backup-${System.currentTimeMillis()}.suvb")
                            } catch (e: Exception) {
                                Arrays.fill(secret, '\u0000')
                                val msg = if (e is BackupFormatException) e.message ?: "Could not prepare this backup." else "Could not prepare this backup."
                                notifyUser("Backup Failed", msg)
                            } finally { busy = false }
                        }
                    }
                ) { Text("Continue") }
            },
            dismissButton = { TextButton(onClick = { exportDialog = false; passphrase = ""; confirm = "" }) { Text("Cancel") } },
        )
    }

    if (importDialog) {
        var importPasswordVisible by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { importDialog = false; passphrase = ""; importUri = null },
            title = { Text("Enter the recovery passphrase") },
            text = { PasswordField(passphrase, { passphrase = it }, "Passphrase", importPasswordVisible, { importPasswordVisible = it }) },
            confirmButton = {
                TextButton(enabled = passphrase.isNotEmpty() && !busy, onClick = {
                    val secret = passphrase.toCharArray(); val uri = importUri; passphrase = ""; importDialog = false
                    if (uri == null) return@TextButton
                    scope.launch {
                        busy = true
                        try {
                            val encrypted = withContext(Dispatchers.IO) {
                                context.contentResolver.openInputStream(uri)?.use(::readBounded) ?: error("Could not open selected backup.")
                            }
                            val plaintext = withContext(Dispatchers.Default) { EncryptedBackup.decrypt(encrypted, secret) }
                            try {
                                val snapshot = BackupJsonCodec.decode(plaintext)
                                pending = snapshot; preview = BackupJsonCodec.preview(snapshot)
                            } finally { Arrays.fill(plaintext, 0) }
                        } catch (e: Exception) {
                            Arrays.fill(secret, '\u0000')
                            val msg = if (e is BackupFormatException) e.message ?: "Could not open this backup file." else "Could not open this backup file."
                            notifyUser("Restore Failed", msg)
                        } finally { importUri = null; busy = false }
                    }
                }) { Text("Preview backup") }
            },
            dismissButton = { TextButton(onClick = { importDialog = false; passphrase = ""; importUri = null }) { Text("Cancel") } },
        )
    }

    val p = preview
    val snapshot = pending
    if (p != null && snapshot != null) AlertDialog(
        onDismissRequest = { preview = null; pending = null },
        title = { Text("Review backup before restore") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Created ${formatDate(p.createdAtMillis)}")
                Text("${p.expenseCount} expenses · ${p.friendCount} friends · ${p.splitCount} splits")
                Text("${p.groupCount} groups · ${p.eventCount} group events")
                Text("${p.linkedIdentityCount} local member/friend links")
                Text(if (p.identityIncluded) "Nostr identity and ${p.mlsGroupCount} encrypted group-state record(s) included." else "No identity key is included. This backup cannot restore a Nostr identity.")
                Text("Restore replaces all current ledger and group data. Create a separate backup first if you may need the current data.", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        },
        confirmButton = { TextButton(enabled = !busy, onClick = {
            val restoreSnapshot = snapshot
            preview = null
            scope.launch {
                busy = true
                try {
                    withContext(Dispatchers.IO) {
                        val previousIdentity = identityStore.exportPrivateKeyForEncryptedBackup()
                        val previousStates = mlsStore.exportAllForRecovery()
                        val previousTransport = mlsStore.transportStore.exportForRecovery()
                        val previousKeyPackage = keyPackageStore.load()
                        val importedIdentity = restoreSnapshot.identityPrivateKeyHex?.hexBytes()
                        val importedStates = restoreSnapshot.mlsGroupStates.associate { it.groupId to Base64.getDecoder().decode(it.encodedStateBase64) }
                        val importedTransport = restoreSnapshot.transportStateBase64?.let { Base64.getDecoder().decode(it) }
                        val importedKeyPackage = restoreSnapshot.keyPackageBundleBase64?.let { Base64.getDecoder().decode(it) }
                        try {
                            try {
                                if (importedIdentity == null) identityStore.clear()
                                else identityStore.restorePrivateKeyFromEncryptedBackup(importedIdentity)
                                mlsStore.replaceAllFromRecovery(importedStates)
                                mlsStore.transportStore.replaceFromRecovery(importedTransport)
                                if (importedKeyPackage == null) keyPackageStore.delete() else keyPackageStore.save(importedKeyPackage)
                                repo.replaceWith(restoreSnapshot)
                            } catch (error: Exception) {
                                runCatching {
                                    if (previousIdentity == null) identityStore.clear()
                                    else identityStore.restorePrivateKeyFromEncryptedBackup(previousIdentity)
                                    mlsStore.replaceAllFromRecovery(previousStates)
                                    mlsStore.transportStore.replaceFromRecovery(previousTransport)
                                    if (previousKeyPackage == null) keyPackageStore.delete() else keyPackageStore.save(previousKeyPackage)
                                }
                                throw error
                            }
                        } finally {
                            previousIdentity?.fill(0)
                            importedIdentity?.fill(0)
                            previousStates.values.forEach { it.fill(0) }
                            importedStates.values.forEach { it.fill(0) }
                            previousTransport.fill(0)
                            previousKeyPackage?.fill(0)
                            importedTransport?.fill(0)
                            importedKeyPackage?.fill(0)
                        }
                    }
                    pending = null
                    notifyUser("Backup Restored", "Backup restored successfully.")
                }
                catch (_: Exception) {
                    notifyUser("Restore Failed", "Restore failed; the current database was left unchanged.")
                }
                finally { busy = false }
            }
        }) { Text("Replace local data") } },
        dismissButton = { TextButton(onClick = { preview = null; pending = null }) { Text("Cancel") } },
    )
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    visible: Boolean,
    onVisibleChange: (Boolean) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = { onVisibleChange(!visible) }) {
                Icon(
                    painter = painterResource(if (visible) R.drawable.ic_visibility_off else R.drawable.ic_visibility),
                    contentDescription = if (visible) "Hide $label" else "Show $label",
                    tint = Muted,
                )
            }
        },
        singleLine = true,
    )
}

private fun readBounded(input: java.io.InputStream): ByteArray {
    val output = ByteArrayOutputStream(); val buffer = ByteArray(8192); var total = 0
    while (true) {
        val n = input.read(buffer); if (n < 0) break
        total += n; if (total > EncryptedBackup.MAX_FILE_BYTES) throw BackupFormatException("Backup file is too large.")
        output.write(buffer, 0, n)
    }
    return output.toByteArray()
}

private fun formatDate(ms: Long) = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", java.util.Locale.getDefault()).withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(ms))

private fun ByteArray.toLowerHex() = joinToString("") { "%02x".format(it) }
private fun String.hexBytes() = ByteArray(length / 2) { index -> substring(index * 2, index * 2 + 2).toInt(16).toByte() }
