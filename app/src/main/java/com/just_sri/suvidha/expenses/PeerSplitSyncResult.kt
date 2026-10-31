package com.just_sri.suvidha.expenses

/** Transport failures need another attempt; invalid remote events should not cause a retry loop. */
internal data class PeerSplitSyncResult(val report: RelaySyncReport, val pendingUploads: Boolean) {
    val needsRetry: Boolean get() = pendingUploads || report.relaysResponded == 0 || report.relayFailures > 0

    /** Unreachable relays are already reported by [relayFailures]; only events we failed to apply count here. */
    val processingErrors: Int get() = (report.errors - report.relayFailures).coerceAtLeast(0)

    val message: String get() = summary("Up to date")

    fun summary(upToDate: String): String = when {
        report.relaysResponded == 0 -> "No relay responded. Your changes are saved and will retry automatically."
        pendingUploads -> "Updates received. Some changes are waiting to send and will retry automatically."
        processingErrors > 0 -> "Synced with some errors. Retry to check for more updates."
        report.relayFailures > 0 -> "$upToDate, but some relays did not respond. Retrying automatically."
        else -> "$upToDate · ${report.groupEventsProcessed} updates · ${report.welcomesProcessed} invitations"
    }
}
