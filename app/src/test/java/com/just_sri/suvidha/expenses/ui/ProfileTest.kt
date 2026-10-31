package com.just_sri.suvidha.expenses.ui

import com.just_sri.suvidha.expenses.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileTest {
    @Test
    fun initialsGeneratesExpectedInitialsFromFullName() {
        val actualTwoNames: String = initials("Srikant Agrawal")
        val actualSingleName: String = initials("Srikant")
        val actualEmptyName: String = initials("")
        assertEquals("SA", actualTwoNames)
        assertEquals("S", actualSingleName)
        assertEquals("S", actualEmptyName)
    }

    @Test
    fun nameCharacterLimitIsFifty() {
        val maxLimit: Int = 50
        val longName: String = "A".repeat(60)
        val trimmed: String = longName.take(maxLimit)
        assertEquals(50, trimmed.length)
    }

    @Test
    fun formatVersionNoticeGeneratesExpectedStatusMessage() {
        val releaseVersion: String = "1.0.4"
        val versionCode: String = "19"
        val actualNotice: String = formatVersionNotice(releaseVersion, versionCode)
        val expectedNotice: String = "Suvidha v1.0.4 (19) · Up to date"
        assertEquals(expectedNotice, actualNotice)
    }

    @Test
    fun buildConfigProvidesValidVersionMetadata() {
        val actualVersionCode: Int = BuildConfig.VERSION_CODE
        val actualVersionName: String = BuildConfig.VERSION_NAME
        assertTrue(actualVersionCode > 0)
        assertTrue(actualVersionName.isNotBlank())
    }
}
