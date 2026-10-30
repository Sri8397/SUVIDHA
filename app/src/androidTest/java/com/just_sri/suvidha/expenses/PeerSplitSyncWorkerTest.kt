package com.just_sri.suvidha.expenses

import android.content.Context
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class PeerSplitSyncWorkerTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val healthy = PeerSplitSyncResult(RelaySyncReport(2, 0, 0, 0, 0), false)

    private fun worker(attempts: Int = 0, sync: suspend () -> PeerSplitSyncResult): PeerSplitSyncWorker =
        TestListenableWorkerBuilder<PeerSplitSyncWorker>(context)
            .setRunAttemptCount(attempts)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker =
                    PeerSplitSyncWorker(appContext, workerParameters, sync)
            }).build()

    @Test fun workerCanBeConstructedByDefaultFactoryWithoutAnActivity() {
        assertNotNull(TestListenableWorkerBuilder<PeerSplitSyncWorker>(context).build())
    }

    @Test fun healthySyncCompletes() = runBlocking {
        assertEquals(ListenableWorker.Result.success(), worker { healthy }.doWork())
    }

    @Test fun undeliveredOutboxRetriesAfterSuccessfulFetch() = runBlocking {
        assertEquals(ListenableWorker.Result.retry(), worker { healthy.copy(pendingUploads = true) }.doWork())
    }

    @Test fun networkExceptionRetries() = runBlocking {
        assertEquals(ListenableWorker.Result.retry(), worker { throw IOException("offline") }.doWork())
    }

    @Test fun failedRelayRetries() = runBlocking {
        assertEquals(ListenableWorker.Result.retry(), worker {
            healthy.copy(report = healthy.report.copy(relaysResponded = 0, errors = 2, relayFailures = 2))
        }.doWork())
    }

    @Test fun badRemoteEventsDoNotKeepRetrying() = runBlocking {
        assertEquals(ListenableWorker.Result.success(), worker { healthy.copy(report = healthy.report.copy(errors = 2)) }.doWork())
    }

    @Test fun retriesAreBoundedAndNextPeriodicPassCanTryAgain() = runBlocking {
        assertEquals(ListenableWorker.Result.failure(), worker(attempts = 4) { healthy.copy(pendingUploads = true) }.doWork())
    }

    @Test fun invalidLocalStateDoesNotCreateAnImmediateRetryLoop() = runBlocking {
        assertEquals(ListenableWorker.Result.failure(), worker { error("invalid state") }.doWork())
    }

    @Test fun cancellationIsPropagatedToWorkManager() = runBlocking {
        try {
            worker { throw CancellationException("stopped") }.doWork()
            fail("Cancellation must reach WorkManager")
        } catch (_: CancellationException) { }
    }

    @Test fun separateServicesShareLockAndPollingDoesNotWaitBehindEditsOrRecovery() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, ExpenseDatabase::class.java).build()
        try {
            val first = PeerSplitService(context, db)
            val second = PeerSplitService(context, db)
            assertSame(first.operationMutex, second.operationMutex)
            first.operationMutex.lock()
            try {
                assertNull(second.syncIfIdle())
                val identity = async(start = CoroutineStart.UNDISPATCHED) { second.publicId() }
                assertFalse(identity.isCompleted)
                identity.cancelAndJoin()
            } finally { first.operationMutex.unlock() }
        } finally { db.close() }
    }
}
