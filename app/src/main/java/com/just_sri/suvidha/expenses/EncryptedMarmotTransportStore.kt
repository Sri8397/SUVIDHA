package com.just_sri.suvidha.expenses

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.vitorpamplona.marmot.MarmotIngestDedupStore
import com.vitorpamplona.marmot.protocolCore.MarmotPublishObligationStore
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Publish obligations contain private MLS states and must be encrypted before reaching disk. */
internal class EncryptedMarmotTransportStore(context: Context) : MarmotPublishObligationStore {
    private val file = File(context.noBackupFilesDir, "peersplit/transport.enc")
    private val mutex = Mutex()

    override suspend fun save(obligationId: String, bytes: ByteArray) = mutex.withLock {
        require(obligationId.matches(ID) && bytes.size in 1..MAX_BYTES)
        val root = read()
        root.getJSONObject("obligations").put(obligationId, Base64.getEncoder().encodeToString(bytes))
        write(root)
    }

    override suspend fun delete(obligationId: String) = mutex.withLock {
        val root = read(); root.getJSONObject("obligations").remove(obligationId); write(root)
    }

    override suspend fun loadAll(): List<ByteArray> = mutex.withLock {
        val obligations = read().getJSONObject("obligations")
        obligations.keys().asSequence().map { Base64.getDecoder().decode(obligations.getString(it)) }.toList()
    }

    override suspend fun saveGate(groupId: String, gate: String) = mutex.withLock {
        require(groupId.matches(ID))
        val root = read(); root.getJSONObject("gates").put(groupId, gate); write(root)
    }

    override suspend fun deleteGate(groupId: String) = mutex.withLock {
        val root = read(); root.getJSONObject("gates").remove(groupId); write(root)
    }

    override suspend fun loadGates(): Map<String, String> = mutex.withLock {
        val gates = read().getJSONObject("gates")
        gates.keys().asSequence().associateWith { gates.getString(it) }
    }

    val ingestDedup = object : MarmotIngestDedupStore {
        override suspend fun mark(eventId: String) = mutex.withLock {
            require(eventId.matches(ID))
            val root = read(); val marks = root.getJSONArray("marks")
            if ((0 until marks.length()).none { marks.getString(it) == eventId }) { marks.put(eventId); write(root) }
        }
        override suspend fun loadAll(): Set<String> = mutex.withLock {
            val marks = read().getJSONArray("marks")
            (0 until marks.length()).mapTo(mutableSetOf()) { marks.getString(it) }
        }
    }

    suspend fun exportForRecovery(): ByteArray = mutex.withLock { read().toString().toByteArray() }

    suspend fun replaceFromRecovery(encoded: ByteArray?) = mutex.withLock {
        write(if (encoded == null) empty() else parse(encoded))
    }

    private fun empty() = JSONObject().put("version", 1).put("obligations", JSONObject()).put("gates", JSONObject()).put("marks", JSONArray())

    private fun parse(bytes: ByteArray): JSONObject {
        require(bytes.size <= MAX_BYTES)
        return JSONObject(bytes.toString(Charsets.UTF_8)).also { root ->
            require(root.getInt("version") == 1)
            val obligations = root.getJSONObject("obligations")
            require(obligations.keys().asSequence().all { it.matches(ID) })
            val gates = root.getJSONObject("gates")
            require(gates.keys().asSequence().all { it.matches(ID) })
            val marks = root.getJSONArray("marks")
            require(marks.length() <= 100_000 && (0 until marks.length()).all { marks.getString(it).matches(ID) })
        }
    }

    private fun read(): JSONObject {
        if (!file.exists()) return empty()
        val bytes = file.readBytes()
        require(bytes.size in 30..(MAX_BYTES + 29) && bytes[0] == 1.toByte())
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(1, 13)))
        cipher.updateAAD(AAD)
        val plain = cipher.doFinal(bytes, 13, bytes.size - 13)
        return try { parse(plain) } finally { plain.fill(0); bytes.fill(0) }
    }

    private fun write(root: JSONObject) {
        val plain = root.toString().toByteArray()
        require(plain.size <= MAX_BYTES)
        val temp = File(file.parentFile, "${file.name}.tmp")
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key()); cipher.updateAAD(AAD)
            require(cipher.iv.size == 12)
            val encoded = byteArrayOf(1) + cipher.iv + cipher.doFinal(plain)
            file.parentFile?.mkdirs()
            FileOutputStream(temp).use { it.write(encoded); it.flush(); it.fd.sync() }
            Files.move(temp.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            encoded.fill(0)
        } finally { plain.fill(0); temp.delete() }
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance("AES", "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        return generator.generateKey()
    }

    private companion object {
        val ID = Regex("[0-9a-f]{64}")
        const val MAX_BYTES = 32 * 1024 * 1024
        const val ALIAS = "suvidha_peersplit_transport_wrap_v1"
        val AAD = "suvidha-peersplit-transport-v1".toByteArray()
    }
}
