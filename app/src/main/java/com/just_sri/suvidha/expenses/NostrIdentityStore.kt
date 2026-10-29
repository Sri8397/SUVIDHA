package com.just_sri.suvidha.expenses

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.vitorpamplona.quartz.nip01Core.crypto.KeyPair
import com.vitorpamplona.quartz.nip01Core.signers.NostrSignerInternal
import com.vitorpamplona.quartz.nip01Core.core.toHexKey
import java.io.File
import java.security.KeyStore
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Local NIP-01 secp256k1 identity. The long-term signing key is never stored as plaintext. */
internal class NostrIdentityStore(context: Context) {
    private val keyFile = File(context.noBackupFilesDir, "peersplit/nostr-identity.v1")

    @Synchronized
    fun getOrCreate(): NostrIdentity {
        val privateKey = if (keyFile.exists()) decrypt(keyFile.readBytes()) else {
            val generated = KeyPair().privKey ?: error("Quartz did not produce a signing key.")
            try {
                keyFile.parentFile?.mkdirs()
                persistEncrypted(generated)
                generated.copyOf()
            } finally {
                generated.fill(0)
            }
        }
        return try {
            val keyPair = KeyPair(privKey = privateKey.copyOf())
            NostrIdentity(keyPair.pubKey.toHexKey(), NostrSignerInternal(keyPair))
        } finally {
            privateKey.fill(0)
        }
    }

    /** Export only into an already passphrase-encrypted app backup. */
    @Synchronized
    fun exportPrivateKeyForEncryptedBackup(): ByteArray? = if (keyFile.exists()) decrypt(keyFile.readBytes()) else null

    /** Re-wraps a key from an authenticated, passphrase-encrypted backup. */
    @Synchronized
    fun restorePrivateKeyFromEncryptedBackup(privateKey: ByteArray) {
        require(privateKey.size == 32) { "Invalid Nostr private key size." }
        val pair = KeyPair(privKey = privateKey.copyOf())
        pair.privKey?.fill(0)
        persistEncrypted(privateKey)
    }

    @Synchronized
    fun clear() {
        keyFile.delete()
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (store.containsAlias(KEY_ALIAS)) store.deleteEntry(KEY_ALIAS)
    }

    private fun encrypt(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, keystoreKey())
        val iv = cipher.iv
        require(iv.size == 12) { "Unexpected Android Keystore nonce size." }
        val encrypted = cipher.doFinal(plain)
        return byteArrayOf(1) + iv + encrypted
    }

    private fun decrypt(encoded: ByteArray): ByteArray {
        require(encoded.size in 30..128 && encoded[0].toInt() == 1) { "Invalid identity key file." }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, keystoreKey(), GCMParameterSpec(128, encoded.copyOfRange(1, 13)))
        return cipher.doFinal(encoded, 13, encoded.size - 13)
    }

    private fun persistEncrypted(privateKey: ByteArray) {
        keyFile.parentFile?.mkdirs()
        val encrypted = encrypt(privateKey)
        val temp = File(keyFile.parentFile, "${keyFile.name}.tmp")
        try {
            java.io.FileOutputStream(temp).use { output ->
                output.write(encrypted)
                output.flush()
                output.fd.sync()
            }
            Files.move(temp.toPath(), keyFile.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } finally {
            encrypted.fill(0)
            temp.delete()
        }
    }

    private fun keystoreKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val KEY_ALIAS = "suvidha_peersplit_identity_wrap_v1"
    }
}

internal data class NostrIdentity(val publicKeyHex: String, val signer: NostrSignerInternal)
