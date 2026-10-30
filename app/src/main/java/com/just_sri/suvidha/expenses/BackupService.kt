package com.just_sri.suvidha.expenses

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Base64

/** Coordinates Room snapshots and keystore recovery outside the presentation layer. */
internal class BackupService(context: Context, database: ExpenseDatabase, private val peers: PeerSplitService) {
    private val appContext = context.applicationContext
    private val repo = ExpenseBackupRepository(database)
    private val identityStore = NostrIdentityStore(context)
    private val mlsStore = EncryptedMlsGroupStateStore(context)
    private val keyPackageStore = EncryptedKeyPackageBundleStore(context)

    suspend fun export(secret: CharArray): ByteArray = peers.operationMutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                val material = capture()
                val snapshot = try {
                    repo.createSnapshot(
                        identityPrivateKeyHex = material.identity?.joinToString("") { "%02x".format(it) },
                        mlsGroupStates = material.groups.map { (id, state) -> BackupMlsGroupState(id, encode(state)) },
                        transportStateBase64 = material.transport?.let(::encode),
                        keyPackageBundleBase64 = material.keyPackage?.let(::encode))
                } finally { material.wipe() }
                val plain = BackupJsonCodec.encode(snapshot)
                try { EncryptedBackup.encrypt(plain, secret) } finally { plain.fill(0) }
            } finally { secret.fill('\u0000') }
        }
    }

    suspend fun restore(snapshot: AppBackupSnapshot) = peers.operationMutex.withLock {
        withContext(Dispatchers.IO) {
            val previous = capture()
            var imported: RecoveryMaterial? = null
            try {
                imported = RecoveryMaterial(snapshot.identityPrivateKeyHex?.let { hex -> ByteArray(hex.length / 2) { i -> hex.substring(i * 2, i * 2 + 2).toInt(16).toByte() } },
                    snapshot.mlsGroupStates.associate { it.groupId to Base64.getDecoder().decode(it.encodedStateBase64) },
                    snapshot.transportStateBase64?.let { Base64.getDecoder().decode(it) },
                    snapshot.keyPackageBundleBase64?.let { Base64.getDecoder().decode(it) })
                try {
                    apply(imported)
                    repo.replaceWith(snapshot)
                } catch (failure: Exception) {
                    try { apply(previous) } catch (rollback: Exception) { failure.addSuppressed(rollback) }
                    throw failure
                }
            } finally { previous.wipe(); imported?.wipe() }
            PeerSplitSyncScheduler.request(appContext, afterChange = true)
        }
    }

    private suspend fun capture() = RecoveryMaterial(identityStore.exportPrivateKeyForEncryptedBackup(),
        mlsStore.exportAllForRecovery(), mlsStore.transportStore.exportForRecovery(), keyPackageStore.load())

    private suspend fun apply(material: RecoveryMaterial) {
        if (material.identity == null) identityStore.clear() else identityStore.restorePrivateKeyFromEncryptedBackup(material.identity)
        mlsStore.replaceAllFromRecovery(material.groups)
        mlsStore.transportStore.replaceFromRecovery(material.transport)
        if (material.keyPackage == null) keyPackageStore.delete() else keyPackageStore.save(material.keyPackage)
    }

    private fun encode(bytes: ByteArray) = Base64.getEncoder().encodeToString(bytes)
    private data class RecoveryMaterial(val identity: ByteArray?, val groups: Map<String, ByteArray>, val transport: ByteArray?, val keyPackage: ByteArray?) {
        fun wipe() { identity?.fill(0); groups.values.forEach { it.fill(0) }; transport?.fill(0); keyPackage?.fill(0) }
    }
}
