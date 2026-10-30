package com.just_sri.suvidha.expenses

import android.content.Context
import android.content.ContextWrapper
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit
import java.io.File
import java.util.UUID

class PeerSplitSyncSchedulerTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var work: WorkManager
    private val entered = CompletableDeferred<Unit>()
    private val release = CompletableDeferred<Unit>()

    @Before fun setup() {
        val configuration = Configuration.Builder().setExecutor(SynchronousExecutor())
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker? {
                    if (workerClassName != PeerSplitSyncWorker::class.java.name) return null
                    return PeerSplitSyncWorker(appContext, workerParameters) {
                        entered.complete(Unit)
                        release.await()
                        PeerSplitSyncResult(RelaySyncReport(2, 0, 0, 0, 0), false)
                    }
                }
            }).build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, configuration)
        work = WorkManager.getInstance(context)
    }

    @After fun close() {
        release.complete(Unit)
        work.cancelAllWork().result.get(10, TimeUnit.SECONDS)
        WorkManagerTestInitHelper.closeWorkDatabase()
    }

    private fun infos(name: String) = work.getWorkInfosForUniqueWork(name).get(10, TimeUnit.SECONDS)

    @Test fun periodicScheduleSurvivesRepeatedRegistrationAndRequiresNetwork() = runBlocking {
        PeerSplitSyncScheduler.ensurePeriodic(context)
        val original = infos(PeerSplitSyncScheduler.PERIODIC_WORK).single()
        PeerSplitSyncScheduler.ensurePeriodic(context)
        val current = infos(PeerSplitSyncScheduler.PERIODIC_WORK).single()
        assertEquals(original.id, current.id)
        assertEquals(NetworkType.CONNECTED, current.constraints.requiredNetworkType)
        assertEquals(15 * 60_000L, current.periodicityInfo!!.repeatIntervalMillis)
    }

    @Test fun resumeRequestsCoalesceWhileOffline() = runBlocking {
        repeat(3) { PeerSplitSyncScheduler.request(context) }
        val pending = infos(PeerSplitSyncScheduler.IMMEDIATE_WORK).single()
        assertEquals(WorkInfo.State.ENQUEUED, pending.state)
        assertEquals(NetworkType.CONNECTED, pending.constraints.requiredNetworkType)
        assertFalse(entered.isCompleted)
    }

    @Test fun changeDuringRunningSyncGetsFollowUpAndResumeDoesNotAddAnother() = runBlocking<Unit> {
        PeerSplitSyncScheduler.request(context)
        val first = infos(PeerSplitSyncScheduler.IMMEDIATE_WORK).single()
        WorkManagerTestInitHelper.getTestDriver(context)!!.setAllConstraintsMet(first.id)
        withTimeout(10_000) { entered.await() }
        assertEquals(WorkInfo.State.RUNNING, infos(PeerSplitSyncScheduler.IMMEDIATE_WORK).single().state)
        repeat(5) { PeerSplitSyncScheduler.request(context, afterChange = true) }
        PeerSplitSyncScheduler.request(context)
        val chain = infos(PeerSplitSyncScheduler.IMMEDIATE_WORK)
        assertEquals(2, chain.size)
        val followUp = chain.single { it.id != first.id }
        assertEquals(WorkInfo.State.BLOCKED, followUp.state)
        release.complete(Unit)
        withTimeout(10_000) {
            work.getWorkInfosForUniqueWorkFlow(PeerSplitSyncScheduler.IMMEDIATE_WORK).first { entries ->
                entries.any { it.id == first.id && it.state == WorkInfo.State.SUCCEEDED }
            }
        }
        WorkManagerTestInitHelper.getTestDriver(context)!!.setAllConstraintsMet(followUp.id)
        withTimeout(10_000) {
            work.getWorkInfosForUniqueWorkFlow(PeerSplitSyncScheduler.IMMEDIATE_WORK).first { entries ->
                entries.all { it.state == WorkInfo.State.SUCCEEDED }
            }
        }
    }

    @Test fun offlineSavePersistsExactEncryptedOutboxAcrossDatabaseReopen() = runBlocking {
        val directory = File(context.noBackupFilesDir, "test-sync-${UUID.randomUUID()}").apply { mkdirs() }
        val isolated = object : ContextWrapper(context) {
            override fun getNoBackupFilesDir(): File = directory
        }
        val databaseName = "test-sync-${UUID.randomUUID()}.db"
        var db = Room.databaseBuilder(context, ExpenseDatabase::class.java, databaseName).build()
        try {
            val service = PeerSplitService(isolated, db)
            val groupId = withTimeout(15_000) { service.createGroup("Offline group", "INR") }
            withTimeout(15_000) { service.sendChatMessage(groupId, "Saved while offline") }
            val pending = SharedGroupRepository(db).pendingEncryptedEvents(groupId).single()
            assertEquals("pending", pending.syncState)
            assertNotNull(pending.transportEventJson)
            assertFalse(pending.transportEventJson!!.contains("Saved while offline"))
            assertFalse(entered.isCompleted)
            assertEquals(WorkInfo.State.ENQUEUED, infos(PeerSplitSyncScheduler.IMMEDIATE_WORK).single().state)
            db.close()
            db = Room.databaseBuilder(context, ExpenseDatabase::class.java, databaseName).build()
            val restored = SharedGroupRepository(db).pendingEncryptedEvents(groupId).single()
            assertEquals(pending.id, restored.id)
            assertEquals(pending.transportEventJson, restored.transportEventJson)
        } finally {
            db.close()
            context.deleteDatabase(databaseName)
            directory.deleteRecursively()
        }
    }
}
