package com.just_sri.suvidha.expenses

/** Transport failures need another attempt; invalid remote events should not cause a retry loop. */
internal data class PeerSplitSyncResult(val report: RelaySyncReport, val pendingUploads: Boolean) {
    val needsRetry: Boolean get() = pendingUploads || report.relaysResponded == 0 || report.relayFailures > 0

    val message: String get() = when {
        report.relaysResponded == 0 -> "No relay responded. Your changes are saved and will retry automatically."
        pendingUploads -> "Updates received. Some changes are waiting to send and will retry automatically."
        report.errors > 0 -> "Synced with some errors. Retry to check for more updates."
        else -> "Up to date · ${report.groupEventsProcessed} updates · ${report.welcomesProcessed} invitations"
    }
}
