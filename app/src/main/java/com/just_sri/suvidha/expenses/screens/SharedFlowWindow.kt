package com.just_sri.suvidha.expenses.screens

import android.os.Build
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.just_sri.suvidha.expenses.Canvas

/** A group-owned full-screen flow with the same system-bar appearance as the main money flow. */
@Composable
internal fun SharedFlowWindow(onDismissRequest: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        val view = LocalView.current
        val window = (view.parent as? DialogWindowProvider)?.window
        SideEffect {
            window?.let {
                it.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                it.statusBarColor = Canvas.toArgb()
                it.navigationBarColor = Canvas.toArgb()
                if (Build.VERSION.SDK_INT >= 29) it.isNavigationBarContrastEnforced = false
                WindowCompat.getInsetsController(it, view).apply {
                    isAppearanceLightStatusBars = true
                    isAppearanceLightNavigationBars = true
                }
            }
        }
        content()
    }
}
