package com.just_sri.suvidha.expenses.ui

import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.just_sri.suvidha.expenses.Muted
import com.just_sri.suvidha.expenses.ui.theme.ThemePreference

@Composable
internal fun ThemeSelectionScreen(
    currentTheme: ThemePreference,
    onSelectTheme: (ThemePreference) -> Unit,
    back: () -> Unit
) {
    PageColumn {
        PageHeader("Theme", back)
        PageList {
            item {
                Text(
                    text = "Choose your preferred appearance.",
                    color = Muted,
                    fontSize = 13.sp
                )
            }
            item {
                Panel {
                    ThemePreference.entries.forEachIndexed { index, pref ->
                        LedgerRow(
                            title = pref.title,
                            subtitle = pref.subtitle,
                            onClick = {
                                onSelectTheme(pref)
                                back()
                            },
                            trailing = {
                                RadioButton(
                                    selected = currentTheme == pref,
                                    onClick = {
                                        onSelectTheme(pref)
                                        back()
                                    }
                                )
                            },
                            divider = index < ThemePreference.entries.lastIndex
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ThemeSelectionScreen(model: AppViewModel, back: () -> Unit) {
    ThemeSelectionScreen(
        currentTheme = model.themePreference,
        onSelectTheme = { model.updateThemePreference(it) },
        back = back
    )
}

@Preview(showBackground = true, widthDp = 411)
@Composable
private fun ThemeSelectionScreenPreview() {
    SuvidhaTheme {
        ThemeSelectionScreen(
            currentTheme = ThemePreference.SYSTEM,
            onSelectTheme = {},
            back = {}
        )
    }
}

