package com.just_sri.suvidha.expenses

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.security.SecureRandom

@RunWith(AndroidJUnit4::class)
class EncryptedMlsGroupStateStoreTest {
    @Test
    fun pendingCommitsGatesAndIngestMarkersSurviveRecreationAndRecovery() = runBlocking {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = java.io.File(base.noBackupFilesDir, "test-transport-${java.util.UUID.randomUUID()}").apply { mkdirs() }
        val context = object : android.content.ContextWrapper(base) {
            override fun getNoBackupFilesDir(): java.io.File = directory
        }
        val id = "a".repeat(64)
        val groupId = "b".repeat(64)
        val encoded = "opaque pending MLS commit with private state".toByteArray()
        try {
            val store = EncryptedMarmotTransportStore(context)
            store.save(id, encoded)
            store.saveGate(groupId, "pending")
            store.ingestDedup.mark(id)
            val reopened = EncryptedMarmotTransportStore(context)
            assertArrayEquals(encoded, reopened.loadAll().single())
            assertEquals(mapOf(groupId to "pending"), reopened.loadGates())
            assertEquals(setOf(id), reopened.ingestDedup.loadAll())
            val recovery = reopened.exportForRecovery()
            reopened.replaceFromRecovery(null)
            assertEquals(emptyList<ByteArray>(), reopened.loadAll())
            reopened.replaceFromRecovery(recovery)
            assertArrayEquals(encoded, reopened.loadAll().single())
            reopened.delete(id)
            reopened.deleteGate(groupId)
            assertEquals(emptyList<ByteArray>(), reopened.loadAll())
            assertEquals(emptyMap<String, String>(), reopened.loadGates())
            recovery.fill(0)
        } finally { encoded.fill(0); directory.deleteRecursively() }
    }

    @Test
    fun keyPackageBundleSurvivesStoreRecreationEncryptedAtRest() = runBlocking {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = java.io.File(base.noBackupFilesDir, "test-keypackage-${java.util.UUID.randomUUID()}").apply { mkdirs() }
        val context = object : android.content.ContextWrapper(base) {
            override fun getNoBackupFilesDir(): java.io.File = directory
        }
        val first = EncryptedKeyPackageBundleStore(context)
        val second = EncryptedKeyPackageBundleStore(context)
        val bundle = ByteArray(128).also(SecureRandom()::nextBytes)
        try {
            first.save(bundle)
            assertArrayEquals(bundle, second.load())
            first.delete()
            assertEquals(null, second.load())
        } finally {
            first.delete()
            bundle.fill(0)
            directory.deleteRecursively()
        }
    }

    @Test
    fun stateRetainedEpochAndSenderRatchetSurviveReloadAndRecoveryExport() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = EncryptedMlsGroupStateStore(context)
        val groupId = ByteArray(32).also(SecureRandom()::nextBytes).joinToString("") { "%02x".format(it) }
        val current = byteArrayOf(1, 3, 5, 7)
        val retained = listOf(byteArrayOf(2, 4, 6), byteArrayOf(8, 9))
        val ratchet = byteArrayOf(11, 12, 13)
        try {
            store.save(groupId, current)
            store.saveRetainedEpochs(groupId, retained)
            assertEquals(true, store.saveSenderRatchet(groupId, ratchet))
            assertArrayEquals(current, store.load(groupId))
            assertEquals(2, store.loadRetainedEpochs(groupId).size)
            assertArrayEquals(ratchet, store.loadSenderRatchet(groupId))

            val exported = store.exportGroupForRecovery(groupId) ?: error("Recovery export was missing.")
            store.delete(groupId)
            store.restoreGroupFromRecovery(groupId, exported)
            assertArrayEquals(current, store.load(groupId))
            assertEquals(2, store.loadRetainedEpochs(groupId).size)
            assertArrayEquals(ratchet, store.loadSenderRatchet(groupId))
            exported.fill(0)
        } finally {
            store.delete(groupId)
            current.fill(0)
            retained.forEach { it.fill(0) }
            ratchet.fill(0)
        }
    }
}
