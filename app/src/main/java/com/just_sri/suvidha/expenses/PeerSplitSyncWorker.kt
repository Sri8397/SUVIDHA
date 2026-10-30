package com.just_sri.suvidha.expenses

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import java.io.IOException

class PeerSplitSyncWorker internal constructor(
    context: Context,
    params: WorkerParameters,
    private val synchronize: suspend () -> PeerSplitSyncResult,
) : CoroutineWorker(context, params) {
    constructor(context: Context, params: WorkerParameters) : this(
        context, params, { PeerSplitService.syncInBackground(context.applicationContext) },
    )

    override suspend fun doWork(): Result = try {
        // Keep a large/offline outbox from holding the protocol lock for an entire job deadline.
        val result = withTimeout(4 * 60_000L) { synchronize() }
        if (result.needsRetry) retryOrFinish() else Result.success()
    } catch (_: TimeoutCancellationException) {
        retryOrFinish()
    } catch (cancelled: CancellationException) {
        // WorkManager reschedules stopped work. Never convert its cancellation into success.
        throw cancelled
    } catch (_: IOException) {
        retryOrFinish()
    } catch (_: Exception) {
        Log.w("PeerSplitSync", "Sync could not open or apply group state; local data is retained.")
        Result.failure()
    }

    private fun retryOrFinish(): Result = if (runAttemptCount < 4) Result.retry() else Result.failure()
}
