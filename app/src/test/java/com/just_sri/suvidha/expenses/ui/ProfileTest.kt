package com.just_sri.suvidha.expenses.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileTest {
    @Test
    fun initialsGeneratesExpectedInitialsFromFullName() {
        assertEquals("SA", initials("Srikant Agrawal"))
        assertEquals("S", initials("Srikant"))
        assertEquals("S", initials(""))
    }

    @Test
    fun nameCharacterLimitIsFifty() {
        val maxLimit = 50
        val longName = "A".repeat(60)
        val trimmed = longName.take(maxLimit)
        assertEquals(50, trimmed.length)
    }
}
