package com.just_sri.suvidha.expenses

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.vitorpamplona.marmot.groups.MlsGroupStateStore
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Durable encrypted Marmot/MLS state, including the sender ratchet needed to prevent nonce reuse. */
internal class EncryptedMlsGroupStateStore(context: Context) : MlsGroupStateStore {
    val transportStore = EncryptedMarmotTransportStore(context)
    private val directory = File(context.noBackupFilesDir, "peersplit/mls").apply { mkdirs() }
    private val mutex = Mutex()

    override suspend fun save(nostrGroupId: String, state: ByteArray) {
        mutex.withLock {
            require(state.isNotEmpty() && state.size <= MAX_BLOB_BYTES)
            val previous = loadContainer(nostrGroupId)
            val replacement = previous.deepCopy().copy(state = state.copyOf(), senderRatchet = null)
            previous.clear()
            saveContainer(nostrGroupId, replacement)
        }
    }

    override suspend fun load(nostrGroupId: String): ByteArray? = mutex.withLock {
        loadContainer(nostrGroupId).let { state -> state.state?.copyOf().also { state.clear() } }
    }

    override suspend fun delete(nostrGroupId: String) {
        mutex.withLock { file(nostrGroupId).delete() }
    }

    override suspend fun listGroups(): List<String> = mutex.withLock { listGroupIds() }

    private fun listGroupIds(): List<String> = directory.listFiles()
        .orEmpty()
        .filter { it.isFile && it.extension == "enc" }
        .map { it.nameWithoutExtension }
        .filter { it.matches(GROUP_ID) }

    override suspend fun saveRetainedEpochs(nostrGroupId: String, retainedSecrets: List<ByteArray>) {
        mutex.withLock {
            require(retainedSecrets.size <= MAX_RETAINED_EPOCHS && retainedSecrets.all { it.isNotEmpty() && it.size <= MAX_BLOB_BYTES })
            val previous = loadContainer(nostrGroupId)
            val replacement = previous.deepCopy().copy(retained = retainedSecrets.map { it.copyOf() })
            previous.clear()
            saveContainer(nostrGroupId, replacement)
        }
    }

    override suspend fun loadRetainedEpochs(nostrGroupId: String): List<ByteArray> = mutex.withLock {
        loadContainer(nostrGroupId).let { state -> state.retained.map { it.copyOf() }.also { state.clear() } }
    }

    override suspend fun saveSenderRatchet(nostrGroupId: String, state: ByteArray): Boolean {
        return mutex.withLock {
            require(state.isNotEmpty() && state.size <= MAX_BLOB_BYTES)
            val previous = loadContainer(nostrGroupId)
            val replacement = previous.deepCopy().copy(senderRatchet = state.copyOf())
            previous.clear()
            saveContainer(nostrGroupId, replacement)
            true
        }
    }

    override suspend fun loadSenderRatchet(nostrGroupId: String): ByteArray? = mutex.withLock {
        loadContainer(nostrGroupId).let { state -> state.senderRatchet?.copyOf().also { state.clear() } }
    }

    /** Export plaintext state only for inclusion inside the passphrase-encrypted recovery file. */
    suspend fun exportGroupForRecovery(nostrGroupId: String): ByteArray? = mutex.withLock {
        if (!file(nostrGroupId).exists()) return@withLock null
        val state = loadContainer(nostrGroupId)
        try { encode(state) } finally { state.clear() }
    }

    suspend fun restoreGroupFromRecovery(nostrGroupId: String, encoded: ByteArray) = mutex.withLock {
        require(encoded.size <= MAX_BLOB_BYTES)
        val state = decode(encoded)
        saveContainer(nostrGroupId, state)
    }

    suspend fun exportAllForRecovery(): Map<String, ByteArray> = mutex.withLock { directory.listFiles().orEmpty()
        .filter { it.isFile && it.extension == "enc" && it.nameWithoutExtension.matches(GROUP_ID) }
        .mapNotNull { file ->
            val state = loadContainer(file.nameWithoutExtension)
            try { file.nameWithoutExtension to encode(state) } finally { state.clear() }
        }
        .toMap()
    }

    suspend fun replaceAllFromRecovery(states: Map<String, ByteArray>) = mutex.withLock {
        require(states.keys.all { it.matches(GROUP_ID) } && states.values.all { it.size <= MAX_BLOB_BYTES })
        directory.listFiles().orEmpty().filter { it.isFile && it.extension == "enc" && it.nameWithoutExtension !in states.keys }.forEach { it.delete() }
        states.forEach { (id, encoded) -> saveContainer(id, decode(encoded)) }
    }

    private fun loadContainer(id: String): MlsState {
        val path = file(id)
        if (!path.exists()) return MlsState()
        val bytes = path.readBytes()
        require(bytes.size in (1 + NONCE_SIZE + TAG_SIZE)..MAX_FILE_BYTES) { "Invalid encrypted MLS state file." }
        require(bytes[0].toInt() == FORMAT_VERSION.toInt()) { "Unsupported encrypted MLS state version." }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_SIZE * 8, bytes.copyOfRange(1, 1 + NONCE_SIZE)))
        cipher.updateAAD(id.toByteArray(Charsets.US_ASCII))
        val plaintext = cipher.doFinal(bytes, 1 + NONCE_SIZE, bytes.size - 1 - NONCE_SIZE)
        return try { decode(plaintext) } finally { plaintext.fill(0); bytes.fill(0) }
    }

    private fun saveContainer(id: String, state: MlsState) {
        val plain = encode(state)
        require(plain.size <= MAX_FILE_BYTES)
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key())
            cipher.updateAAD(id.toByteArray(Charsets.US_ASCII))
            val nonce = cipher.iv.also { require(it.size == NONCE_SIZE) }
            val encrypted = byteArrayOf(FORMAT_VERSION) + nonce + cipher.doFinal(plain)
            val target = file(id)
            val temp = File(directory, "${target.name}.tmp")
            java.io.FileOutputStream(temp).use { output ->
                output.write(encrypted)
                output.flush()
                output.fd.sync()
            }
            if (!temp.renameTo(target)) {
                target.delete()
                check(temp.renameTo(target)) { "Could not atomically store MLS group state." }
            }
            encrypted.fill(0)
        } finally { plain.fill(0); state.clear() }
    }

    private fun encode(state: MlsState): ByteArray = ByteArrayOutputStream().use { bytes ->
        DataOutputStream(bytes).use { out ->
            out.writeInt(STATE_MAGIC)
            out.writeByte(STATE_VERSION)
            out.writeOptional(state.state)
            out.writeInt(state.retained.size)
            state.retained.forEach { out.writeBounded(it) }
            out.writeOptional(state.senderRatchet)
        }
        bytes.toByteArray()
    }

    private fun decode(bytes: ByteArray): MlsState = DataInputStream(ByteArrayInputStream(bytes)).use { input ->
        require(input.readInt() == STATE_MAGIC && input.readUnsignedByte() == STATE_VERSION) { "Invalid MLS recovery state." }
        val current = input.readOptional()
        val count = input.readInt()
        require(count in 0..MAX_RETAINED_EPOCHS)
        val retained = List(count) { input.readBounded() }
        val ratchet = input.readOptional()
        require(input.available() == 0) { "Trailing bytes in MLS recovery state." }
        MlsState(current, retained, ratchet)
    }

    private fun file(id: String): File {
        require(id.matches(GROUP_ID)) { "MLS group ID must be a 32-byte lowercase hex identifier." }
        return File(directory, "$id.enc")
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build())
        return generator.generateKey()
    }

    private data class MlsState(
        val state: ByteArray? = null,
        val retained: List<ByteArray> = emptyList(),
        val senderRatchet: ByteArray? = null,
    ) {
        fun deepCopy() = MlsState(state?.copyOf(), retained.map { it.copyOf() }, senderRatchet?.copyOf())

        fun clear() {
            state?.fill(0)
            retained.forEach { it.fill(0) }
            senderRatchet?.fill(0)
        }
    }

    private fun DataOutputStream.writeOptional(value: ByteArray?) {
        writeBoolean(value != null)
        if (value != null) writeBounded(value)
    }

    private fun DataOutputStream.writeBounded(value: ByteArray) {
        require(value.size in 1..MAX_BLOB_BYTES)
        writeInt(value.size); write(value)
    }

    private fun DataInputStream.readOptional(): ByteArray? = if (readBoolean()) readBounded() else null

    private fun DataInputStream.readBounded(): ByteArray {
        val size = readInt()
        require(size in 1..MAX_BLOB_BYTES && size <= available()) { "Invalid MLS state length." }
        return ByteArray(size).also(::readFully)
    }

    private companion object {
        const val KEY_ALIAS = "suvidha_peersplit_mls_wrap_v1"
        const val FORMAT_VERSION: Byte = 1
        const val STATE_VERSION = 1
        const val STATE_MAGIC = 0x53504d31
        const val NONCE_SIZE = 12
        const val TAG_SIZE = 16
        const val MAX_BLOB_BYTES = 16 * 1024 * 1024
        const val MAX_FILE_BYTES = MAX_BLOB_BYTES * 34
        const val MAX_RETAINED_EPOCHS = 32
        val GROUP_ID = Regex("[0-9a-f]{64}")
    }
}
