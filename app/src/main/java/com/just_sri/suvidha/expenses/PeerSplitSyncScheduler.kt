package com.just_sri.suvidha.expenses

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkInfo
import androidx.work.await
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.TimeUnit

internal object PeerSplitSyncScheduler {
    const val PERIODIC_WORK = "peersplit-periodic-sync"
    const val IMMEDIATE_WORK = "peersplit-immediate-sync"
    private val network = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
    private val schedulingMutex = Mutex()

    suspend fun ensurePeriodic(context: Context) = persistSchedule {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<PeerSplitSyncWorker>(15, TimeUnit.MINUTES)
                .setInitialDelay(15, TimeUnit.MINUTES)
                .setConstraints(network)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build(),
        ).await()
    }

    suspend fun request(context: Context, afterChange: Boolean = false) = persistSchedule {
        schedulingMutex.withLock {
            val work = WorkManager.getInstance(context)
            work.enqueueUniqueWork(
                IMMEDIATE_WORK,
                if (afterChange) ExistingWorkPolicy.APPEND_OR_REPLACE else ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<PeerSplitSyncWorker>()
                    .setConstraints(network)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                    .build(),
            )
        }
    }

    private suspend fun persistSchedule(enqueue: suspend () -> Unit) = withContext(NonCancellable) {
        // Finish persisting the request even when navigation cancels the caller after a local save.
        // Scheduling failure must not turn an already committed save into a reported failure.
        try { enqueue() }
        catch (_: Exception) { Log.w("PeerSplitSync", "Could not schedule sync; the durable outbox is retained.") }
    }
}
