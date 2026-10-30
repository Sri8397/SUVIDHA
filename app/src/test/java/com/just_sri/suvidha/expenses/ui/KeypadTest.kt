package com.just_sri.suvidha.expenses.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class KeypadTest {
    @Test fun decimalEntryHonorsTheCurrencyPrecision() {
        assertEquals("0.", keypadValue("", ".", 2))
        assertEquals("12.34", keypadValue("12.34", "5", 2))
        assertEquals("12", keypadValue("12", ".", 0))
        assertEquals("0.1", keypadValue("0.1", ".", 2))
    }
    @Test fun editingKeepsLeadingZerosAndDeletePredictable() {
        assertEquals("5", keypadValue("0", "5", 2))
        assertEquals("12.", keypadValue("12.3", "delete", 2))
        assertEquals("", keypadValue("", "delete", 2))
        assertEquals("123456789012", keypadValue("123456789012", "3", 2))
    }
}
