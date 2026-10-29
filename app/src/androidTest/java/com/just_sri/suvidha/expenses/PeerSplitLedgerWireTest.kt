package com.just_sri.suvidha.expenses

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PeerSplitLedgerWireTest {
    @Test
    fun fractionalOrStringMinorUnitsAreRejectedInsteadOfBeingRoundedByJson() {
        val event = SharedLedgerEvent("e", "g", "alice", emptySet(), 10,
            SharedLedgerAction.ExpenseAdded("INR", listOf(SharedDebtUnit("d", "alice", "bob", 1250))))
        listOf<Any>(1.25, "1250").forEach { badAmount ->
            val json = org.json.JSONObject(PeerSplitLedgerWire.encode(event))
            json.getJSONObject("action").getJSONArray("debts").getJSONObject(0).put("amountMinor", badAmount)
            assertNull(PeerSplitLedgerWire.decode(json.toString(), "g", "alice"))
        }
        val wrongVersion = org.json.JSONObject(PeerSplitLedgerWire.encode(event)).put("version", 1.5)
        assertNull(PeerSplitLedgerWire.decode(wrongVersion.toString(), "g", "alice"))
    }

    @Test
    fun envelopeBindsLedgerAuthorAndGroupToTheAuthenticatedMlsMessage() {
        val event = SharedLedgerEvent("e", "g", "alice", setOf("parent"), 10,
            SharedLedgerAction.ExpenseAdded("INR", listOf(SharedDebtUnit("d", "alice", "bob", 1250))), "Lunch")
        val encoded = PeerSplitLedgerWire.encode(event)
        assertEquals(event, PeerSplitLedgerWire.decode(encoded, "g", "alice"))
        assertNull(PeerSplitLedgerWire.decode(encoded, "other-group", "alice"))
        assertNull(PeerSplitLedgerWire.decode(encoded, "g", "mallory"))
        assertNull(PeerSplitLedgerWire.decode("x".repeat(128 * 1024 + 1), "g", "alice"))
    }
}
