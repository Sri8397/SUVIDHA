package com.just_sri.suvidha.expenses

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.just_sri.suvidha.expenses.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.withLock

class MainActivity : ComponentActivity() {
    private var destination by mutableStateOf("home")
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); destination = routeFor(intent) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        destination = routeFor(intent)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT))
        SyncNotificationHelper.ensureChannel(this)
        setContent {
            var database by remember { mutableStateOf<ExpenseDatabase?>(null) }
            var failed by remember { mutableStateOf(false) }
            var reset by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()
            fun open(fresh: Boolean = false) {
                failed = false
                scope.launch {
                    try { database = withContext(Dispatchers.IO) {
                        if (fresh) PeerSplitService.protocolMutex.withLock { ExpenseDatabase.deleteAndCreateFresh(this@MainActivity) }
                        else ExpenseDatabase.get(this@MainActivity)
                    } }
                    catch (_: Exception) { failed = true }
                }
            }
            LaunchedEffect(Unit) { open() }
            SuvidhaTheme {
                val db = database
                if (db != null) {
                    val model: AppViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T = AppViewModel(application, db) as T
                    })
                    SuvidhaApp(model, destination)
                } else Surface(Modifier.fillMaxSize(), color = Canvas) {
                    Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        if (!failed) { CircularProgressIndicator(); Spacer(Modifier.height(18.dp)); Text("Opening Suvidha…") }
                        else {
                            Text("Couldn’t open your data")
                            Spacer(Modifier.height(12.dp))
                            Text("Retry to keep your existing data. You can reset only if you want to start over.")
                            Spacer(Modifier.height(20.dp))
                            PrimaryButton("Retry", { open() })
                            TextButton({ reset = true }) { Text("Reset local data", color = Danger) }
                        }
                    }
                }
                if (reset) ConfirmDialog("Delete local data?", "This permanently removes this device’s data. A backup is required to recover it.", "Delete and start over", { reset = false }, { reset = false; open(true) })
            }
        }
    }
    private fun routeFor(intent: Intent?): String = when (intent?.getStringExtra("navigate_to")) { "shared_groups" -> "groups"; "backup" -> "backup"; else -> "home" }
}
