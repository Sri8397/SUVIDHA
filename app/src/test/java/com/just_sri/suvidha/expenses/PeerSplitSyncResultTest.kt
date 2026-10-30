package com.just_sri.suvidha.expenses

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PeerSplitSyncResultTest {
    private val healthy = RelaySyncReport(2, 3, 1, 3, 0)

    @Test fun successfulSyncDoesNotRetry() {
        assertFalse(PeerSplitSyncResult(healthy, false).needsRetry)
    }

    @Test fun outgoingChangesRetryEvenWhenIncomingFetchSucceeded() {
        assertTrue(PeerSplitSyncResult(healthy, true).needsRetry)
    }

    @Test fun failedRelayRetriesEvenIfAnotherRelayResponded() {
        assertTrue(PeerSplitSyncResult(healthy.copy(relaysResponded = 1, errors = 1, relayFailures = 1), false).needsRetry)
    }

    @Test fun noRelayResponseRetries() {
        assertTrue(PeerSplitSyncResult(healthy.copy(relaysResponded = 0), false).needsRetry)
    }

    @Test fun invalidHistoricalEventsDoNotCreateEndlessRetries() {
        assertFalse(PeerSplitSyncResult(healthy.copy(errors = 5), false).needsRetry)
    }
}
