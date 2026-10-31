package com.just_sri.suvidha.expenses

import com.just_sri.suvidha.expenses.engine.SyncConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

/** Coordinates foreground sync timeouts with safe background execution. */
internal class PeerSplitSyncCoordinator(
    private val peers: PeerSplitService,
    val config: SyncConfig = SyncConfig(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    private val mutex = Mutex()
    private var inFlight: Deferred<PeerSplitSyncResult>? = null

    suspend fun syncWithForegroundLimit(
        timeoutMs: Long = config.foregroundTimeoutMs,
    ): PeerSplitSyncResult? {
        val job: Deferred<PeerSplitSyncResult> = mutex.withLock {
            val existing = inFlight
            if (existing != null && existing.isActive) {
                existing
            } else {
                val created = scope.async {
                    try {
                        peers.sync()
                    } finally {
                        mutex.withLock {
                            if (inFlight === this) inFlight = null
                        }
                    }
                }
                inFlight = created
                created
            }
        }

        return try {
            withTimeoutOrNull(timeoutMs) {
                job.await()
            }
        } catch (cancelled: CancellationException) {
            // Keep background job alive even if foreground caller was cancelled
            throw cancelled
        }
    }

    suspend fun syncInBackground(): PeerSplitSyncResult {
        return mutex.withLock {
            val existing = inFlight
            if (existing != null && existing.isActive) {
                existing
            } else {
                val created = scope.async {
                    try {
                        peers.sync()
                    } finally {
                        mutex.withLock {
                            if (inFlight === this) inFlight = null
                        }
                    }
                }
                inFlight = created
                created
            }
        }.await()
    }
}
