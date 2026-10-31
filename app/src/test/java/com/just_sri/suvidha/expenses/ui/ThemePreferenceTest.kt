package com.just_sri.suvidha.expenses.ui

import com.just_sri.suvidha.expenses.ui.theme.ThemeConfig
import com.just_sri.suvidha.expenses.ui.theme.ThemePreference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemePreferenceTest {

    @Test
    fun themePreferenceHasExpectedTitlesAndSubtitles() {
        assertEquals("System default", ThemePreference.SYSTEM.title)
        assertEquals("Follow device appearance", ThemePreference.SYSTEM.subtitle)

        assertEquals("Light", ThemePreference.LIGHT.title)
        assertEquals("Always use light theme", ThemePreference.LIGHT.subtitle)

        assertEquals("Dark", ThemePreference.DARK.title)
        assertEquals("Always use dark theme", ThemePreference.DARK.subtitle)
    }

    @Test
    fun fromParsesValuesCorrectly() {
        assertEquals(ThemePreference.SYSTEM, ThemePreference.from("SYSTEM"))
        assertEquals(ThemePreference.SYSTEM, ThemePreference.from("system"))
        assertEquals(ThemePreference.LIGHT, ThemePreference.from("LIGHT"))
        assertEquals(ThemePreference.LIGHT, ThemePreference.from("light"))
        assertEquals(ThemePreference.DARK, ThemePreference.from("DARK"))
        assertEquals(ThemePreference.DARK, ThemePreference.from("dark"))
        assertEquals(ThemePreference.SYSTEM, ThemePreference.from(null))
        assertEquals(ThemePreference.SYSTEM, ThemePreference.from("UNKNOWN"))
    }

    @Test
    fun themePreferenceResolvesDarkThemeCorrectly() {
        fun resolveDarkTheme(pref: ThemePreference, isSystemDark: Boolean): Boolean = when (pref) {
            ThemePreference.SYSTEM -> isSystemDark
            ThemePreference.LIGHT -> false
            ThemePreference.DARK -> true
        }

        assertFalse(resolveDarkTheme(ThemePreference.SYSTEM, isSystemDark = false))
        assertTrue(resolveDarkTheme(ThemePreference.SYSTEM, isSystemDark = true))

        assertFalse(resolveDarkTheme(ThemePreference.LIGHT, isSystemDark = false))
        assertFalse(resolveDarkTheme(ThemePreference.LIGHT, isSystemDark = true))

        assertTrue(resolveDarkTheme(ThemePreference.DARK, isSystemDark = false))
        assertTrue(resolveDarkTheme(ThemePreference.DARK, isSystemDark = true))
    }

    @Test
    fun themeConfigIsTypeAliasedToThemePreference() {
        val config: ThemeConfig = ThemePreference.DARK
        assertEquals(ThemePreference.DARK, config)
    }
}
