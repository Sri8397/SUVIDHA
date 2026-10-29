package com.just_sri.suvidha.expenses

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class EncryptedBackupTest {
    @Test
    fun encryptionRoundTripsAndClearsPassphraseBuffer() {
        val original = "versioned local recovery file".toByteArray()
        val passphrase = "a sufficiently long phrase".toCharArray()
        val encrypted = EncryptedBackup.encrypt(original, passphrase)

        assertTrue(passphrase.all { it == '\u0000' })
        assertArrayEquals(original, EncryptedBackup.decrypt(encrypted, "a sufficiently long phrase".toCharArray()))
    }

    @Test
    fun tamperedCiphertextIsRejected() {
        val encrypted = EncryptedBackup.encrypt(byteArrayOf(1, 2, 3), "a sufficiently long phrase".toCharArray())
        encrypted[encrypted.lastIndex] = (encrypted.last().toInt() xor 1).toByte()
        assertThrows(BackupFormatException::class.java) {
            EncryptedBackup.decrypt(encrypted, "a sufficiently long phrase".toCharArray())
        }
    }
}
