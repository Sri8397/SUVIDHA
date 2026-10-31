package com.just_sri.suvidha.expenses.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.just_sri.suvidha.expenses.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** Advanced setting: which Nostr relays carry encrypted group traffic and invitations. */
@Composable
internal fun RelaysScreen(model: AppViewModel, back: () -> Unit) {
    var newRelay by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var testing by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<Map<String, Long?>>(emptyMap()) }
    var confirmReset by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    fun runTest() {
        if (testing) return
        testing = true
        results = emptyMap()
        scope.launch {
            try {
                results = coroutineScope { model.relays.map { url -> async { url to model.probeRelay(url) } }.awaitAll().toMap() }
            } finally { testing = false }
        }
    }
    fun addRelay() {
        if (adding) return
        val url: String? = RelaySettings.normalize(newRelay)
        if (url == null || url in model.relays || model.relays.size >= RelaySettings.MAX_RELAYS) {
            error = model.addRelay(newRelay)
            return
        }
        adding = true
        error = null
        scope.launch {
            try {
                val latency: Long? = model.probeRelay(url)
                if (latency == null) {
                    error = "Couldn't reach $url. Check the address and try again."
                    return@launch
                }
                error = model.addRelay(url)
                if (error == null) { newRelay = ""; results = mapOf(url to latency) }
            } finally { adding = false }
        }
    }
    PageColumn {
        PageHeader("Relay servers", back)
        PageList {
            item { Text("Relays carry your encrypted group messages and invitations. They cannot read your data, but they can see when you sync and your IP address.", color = Muted, fontSize = 13.sp, lineHeight = 20.sp) }
            item { Text("A friend can invite you only if you both use at least one relay in common. Changes apply to your next sync; existing groups keep working on the relays they were created with.", color = Muted, fontSize = 13.sp, lineHeight = 20.sp) }
            item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                InputField(newRelay, { newRelay = it.take(200); error = null }, "wss://relay.example.com", modifier = Modifier.weight(1f))
                TextButton({ addRelay() }, enabled = !adding && newRelay.isNotBlank()) { Text(if (adding) "Checking…" else "Add") }
            } }
            item { ErrorText(error) }
            item { Panel { model.relays.forEachIndexed { i, url ->
                val probed = url in results
                val latency: Long? = results[url]
                val status: String = when {
                    testing -> "Testing…"
                    !probed -> if (url in RelaySettings.DEFAULT_URLS) "Default" else "Custom"
                    latency == null -> "Not reachable"
                    else -> "Reachable · $latency ms"
                }
                LedgerRow(url, status, divider = i < model.relays.lastIndex,
                    trailing = { TextButton({ error = model.removeRelay(url); results = results - url }) { Text("Remove", color = Danger, fontSize = 12.sp) } })
            } } }
            item { SecondaryButton(if (testing) "Testing…" else "Test connections", { runTest() }, enabled = !testing) }
            if (model.relaysCustomized) item { TextButton({ confirmReset = true }) { Text("Restore default relays") } }
        }
    }
    if (confirmReset) ConfirmDialog("Restore default relays?", "Your custom relay list will be replaced with the app defaults.", "Restore", { confirmReset = false }, {
        model.resetRelays(); results = emptyMap(); error = null; confirmReset = false
    })
}
