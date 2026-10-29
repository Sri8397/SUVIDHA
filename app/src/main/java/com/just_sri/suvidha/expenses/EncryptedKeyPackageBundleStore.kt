package com.just_sri.suvidha.expenses

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.vitorpamplona.marmot.mip00KeyPackages.KeyPackageBundleStore
import java.io.File
import java.io.FileOutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Persists the private MLS KeyPackage bundle encrypted by a non-exportable Android Keystore key. */
internal class EncryptedKeyPackageBundleStore(context: Context) : KeyPackageBundleStore {
    private val file = File(context.noBackupFilesDir, "peersplit/keypackage/bundle.enc").apply { parentFile?.mkdirs() }
    private val mutex = Mutex()

    override suspend fun save(snapshot: ByteArray) = mutex.withLock {
        require(snapshot.isNotEmpty() && snapshot.size <= MAX_BUNDLE_BYTES) { "Invalid MLS KeyPackage bundle size." }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        cipher.updateAAD(AAD)
        val iv = cipher.iv
        require(iv.size == NONCE_BYTES)
        val encrypted = byteArrayOf(FORMAT_VERSION) + iv + cipher.doFinal(snapshot)
        val temp = File(file.parentFile, "${file.name}.tmp")
        try {
            FileOutputStream(temp).use { output ->
                output.write(encrypted)
                output.flush()
                output.fd.sync()
            }
            if (!temp.renameTo(file)) {
                file.delete()
                check(temp.renameTo(file)) { "Could not atomically save the encrypted KeyPackage bundle." }
            }
        } finally {
            encrypted.fill(0)
            temp.delete()
        }
    }

    override suspend fun load(): ByteArray? = mutex.withLock {
        if (!file.exists()) return@withLock null
        val bytes = file.readBytes()
        try {
            require(bytes.size in (1 + NONCE_BYTES + TAG_BYTES)..(MAX_BUNDLE_BYTES + 1 + NONCE_BYTES + TAG_BYTES)) {
                "Invalid encrypted KeyPackage bundle file."
            }
            require(bytes[0] == FORMAT_VERSION) { "Unsupported encrypted KeyPackage bundle version." }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BYTES * 8, bytes.copyOfRange(1, 1 + NONCE_BYTES)))
            cipher.updateAAD(AAD)
            cipher.doFinal(bytes, 1 + NONCE_BYTES, bytes.size - 1 - NONCE_BYTES)
        } finally {
            bytes.fill(0)
        }
    }

    override suspend fun delete() = mutex.withLock { file.delete(); Unit }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val KEY_ALIAS = "suvidha_peersplit_keypackage_wrap_v1"
        const val FORMAT_VERSION: Byte = 1
        const val NONCE_BYTES = 12
        const val TAG_BYTES = 16
        const val MAX_BUNDLE_BYTES = 8 * 1024 * 1024
        val AAD = "suvidha-peersplit-keypackage-v1".toByteArray(Charsets.US_ASCII)
    }
}
