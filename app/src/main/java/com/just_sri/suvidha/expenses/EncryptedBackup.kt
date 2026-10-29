package com.just_sri.suvidha.expenses

import org.json.JSONArray
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.SecureRandom
import java.util.Arrays
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

internal data class AppBackupSnapshot(
    val createdAtMillis: Long,
    val expenses: List<Expense>,
    val friends: List<Friend>,
    val splits: List<SplitTransaction>,
    val groups: List<SharedGroupEntity>,
    val members: List<SharedGroupMemberEntity>,
    val events: List<SharedLedgerEventRecord>,
    val identityPrivateKeyHex: String? = null,
    val mlsGroupStates: List<BackupMlsGroupState> = emptyList(),
    val transportOutbox: List<SharedTransportOutboxEntity> = emptyList(),
    val transportStateBase64: String? = null,
    val keyPackageBundleBase64: String? = null,
    val memberFriendLinks: List<SharedMemberFriendLinkEntity> = emptyList(),
)

internal data class BackupMlsGroupState(val groupId: String, val encodedStateBase64: String)

internal data class BackupPreview(
    val createdAtMillis: Long,
    val expenseCount: Int,
    val friendCount: Int,
    val splitCount: Int,
    val groupCount: Int,
    val eventCount: Int,
    val identityIncluded: Boolean,
    val mlsGroupCount: Int,
    val linkedIdentityCount: Int = 0,
)

internal class BackupFormatException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Local manual recovery container: PBKDF2-HMAC-SHA256 + AES-256-GCM with the header authenticated. */
internal object EncryptedBackup {
    private val magic = "SUVIDHA1".toByteArray(Charsets.US_ASCII)
    private const val formatVersion: Byte = 1
    private const val kdfId: Byte = 1
    private const val iterations = 600_000
    private const val minIterations = 600_000
    private const val maxIterations = 1_200_000
    private const val saltLength = 16
    private const val nonceLength = 12
    private const val keyBits = 256
    private const val tagBits = 128
    private const val headerLength = 8 + 1 + 1 + 4 + 1 + 1 + saltLength + nonceLength
    const val MAX_FILE_BYTES = 64 * 1024 * 1024

    fun encrypt(plaintext: ByteArray, passphrase: CharArray): ByteArray = try {
        require(plaintext.isNotEmpty() && plaintext.size <= MAX_FILE_BYTES - headerLength - 16) { "Backup is too large." }
        require(passphrase.isNotEmpty()) { "Recovery passphrase is required." }
        val salt = ByteArray(saltLength).also(SecureRandom()::nextBytes)
        val nonce = ByteArray(nonceLength).also(SecureRandom()::nextBytes)
        val header = ByteBuffer.allocate(headerLength).order(ByteOrder.BIG_ENDIAN)
            .put(magic).put(formatVersion).put(kdfId).putInt(iterations)
            .put(saltLength.toByte()).put(nonceLength.toByte()).put(salt).put(nonce).array()
        val key = derive(passphrase, salt, iterations)
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(tagBits, nonce))
            cipher.updateAAD(header)
            header + cipher.doFinal(plaintext)
        } finally { Arrays.fill(key, 0) }
    } finally { Arrays.fill(passphrase, '\u0000') }

    fun decrypt(file: ByteArray, passphrase: CharArray): ByteArray {
        return try {
            if (file.size !in (headerLength + 16)..MAX_FILE_BYTES) throw BackupFormatException("Backup file is incomplete or too large.")
            val buffer = ByteBuffer.wrap(file).order(ByteOrder.BIG_ENDIAN)
            val foundMagic = ByteArray(magic.size).also(buffer::get)
            if (!foundMagic.contentEquals(magic)) throw BackupFormatException("This is not a SUVIDHA backup.")
            if (buffer.get() != formatVersion || buffer.get() != kdfId) throw BackupFormatException("Backup format is not supported.")
            val rounds = buffer.int
            if (rounds !in minIterations..maxIterations) throw BackupFormatException("Backup recovery settings are not supported.")
            val saltSize = buffer.get().toInt() and 0xff
            val nonceSize = buffer.get().toInt() and 0xff
            if (saltSize != saltLength || nonceSize != nonceLength) throw BackupFormatException("Backup header is invalid.")
            val salt = ByteArray(saltSize).also(buffer::get)
            val nonce = ByteArray(nonceSize).also(buffer::get)
            val head = file.copyOfRange(0, buffer.position())
            val ciphertext = file.copyOfRange(buffer.position(), file.size)
            val key = derive(passphrase, salt, rounds)
            try {
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(tagBits, nonce))
                cipher.updateAAD(head)
                cipher.doFinal(ciphertext)
            } catch (_: AEADBadTagException) {
                throw BackupFormatException("Passphrase is incorrect or the backup was changed.")
            } finally { Arrays.fill(key, 0) }
        } catch (e: BackupFormatException) {
            throw e
        } catch (e: Exception) {
            throw BackupFormatException("Could not open this backup.", e)
        } finally { Arrays.fill(passphrase, '\u0000') }
    }

    private fun derive(passphrase: CharArray, salt: ByteArray, rounds: Int): ByteArray {
        val spec = PBEKeySpec(passphrase, salt, rounds, keyBits)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded }
        finally { spec.clearPassword() }
    }
}

/** Payload version is independent of the encrypted container version and Room version. */
internal object BackupJsonCodec {
    private const val payloadVersion = 4
    private const val roomVersion = 4
    private const val maxJsonBytes = 48 * 1024 * 1024
    private const val maxRows = 100_000

    fun encode(s: AppBackupSnapshot): ByteArray {
        validate(s)
        val root = JSONObject()
            .put("formatVersion", payloadVersion).put("roomSchemaVersion", roomVersion)
            .put("createdAtMillis", s.createdAtMillis)
            .put("expenses", JSONArray().also { a -> s.expenses.forEach { a.put(it.toJson()) } })
            .put("friends", JSONArray().also { a -> s.friends.forEach { a.put(it.toJson()) } })
            .put("splits", JSONArray().also { a -> s.splits.forEach { a.put(it.toJson()) } })
            .put("groups", JSONArray().also { a -> s.groups.forEach { a.put(it.toJson()) } })
            .put("members", JSONArray().also { a -> s.members.forEach { a.put(it.toJson()) } })
            .put("events", JSONArray().also { a -> s.events.forEach { a.put(it.toJson()) } })
            .putNullable("identityPrivateKeyHex", s.identityPrivateKeyHex)
            .put("mlsGroupStates", JSONArray().also { a -> s.mlsGroupStates.forEach { a.put(JSONObject().put("groupId", it.groupId).put("state", it.encodedStateBase64)) } })
            .put("transportOutbox", JSONArray().also { a -> s.transportOutbox.forEach { a.put(JSONObject().put("id", it.id).put("groupId", it.groupId).put("purpose", it.purpose).put("eventJson", it.eventJson).put("createdAtMillis", it.createdAtMillis)) } })
            .putNullable("transportStateBase64", s.transportStateBase64)
            .putNullable("keyPackageBundleBase64", s.keyPackageBundleBase64)
            .put("memberFriendLinks", JSONArray().also { a -> s.memberFriendLinks.forEach { a.put(JSONObject().put("memberId", it.memberId).put("friendId", it.friendId).put("linkedAtMillis", it.linkedAtMillis)) } })
        return root.toString().toByteArray(Charsets.UTF_8).also {
            if (it.size > maxJsonBytes) throw BackupFormatException("Backup contents are too large.")
        }
    }

    fun decode(bytes: ByteArray): AppBackupSnapshot {
        if (bytes.isEmpty() || bytes.size > maxJsonBytes) throw BackupFormatException("Backup contents are invalid or too large.")
        try {
            val root = JSONObject(bytes.toString(Charsets.UTF_8))
            val version = root.getInt("formatVersion")
            val backupRoomVersion = root.getInt("roomSchemaVersion")
            if (version !in 1..payloadVersion || backupRoomVersion !in 1..roomVersion) {
                throw BackupFormatException("Backup was created by an unsupported app version.")
            }
            val result = AppBackupSnapshot(
                root.getLong("createdAtMillis"),
                root.array("expenses") { Expense(it.getString("id"), it.getString("category"), it.getDouble("amount"), it.getString("description"), it.getLong("date")) },
                root.array("friends") { Friend(it.getString("id"), it.getString("name"), it.getLong("updatedAt"), it.nullableString("phoneNumber"), it.nullableString("contactId")) },
                root.array("splits") { SplitTransaction(it.getString("id"), it.getString("friendId"), it.getDouble("amount"), it.getString("note"), it.getLong("date"), it.getBoolean("isSettled"), it.nullableLong("settledAt")) },
                root.array("groups") { SharedGroupEntity(it.getString("id"), it.getString("name"), it.getString("currencyCode"), it.getLong("createdAtMillis"), it.getLong("updatedAtMillis")) },
                root.array("members") { SharedGroupMemberEntity(it.getString("groupId"), it.getString("memberId"), it.getString("displayName"), it.getString("role"), it.getLong("joinedAtMillis"), it.getBoolean("isActive")) },
                root.array("events") { SharedLedgerEventRecord(it.getString("id"), it.getString("groupId"), it.getInt("schemaVersion"), it.getString("actorMemberId"), it.getString("actionType"), it.getString("payloadJson"), it.getString("parentEventIdsJson"), it.getLong("createdAtMillis"), it.optString("description", ""), it.nullableString("transportEventJson"), it.optString("syncState", "local")) },
                if (version >= 2) root.nullableString("identityPrivateKeyHex") else null,
                if (version >= 2) root.array("mlsGroupStates") { BackupMlsGroupState(it.getString("groupId"), it.getString("state")) } else emptyList(),
                if (version >= 3) root.array("transportOutbox") { SharedTransportOutboxEntity(it.getString("id"), it.getString("groupId"), it.getString("purpose"), it.getString("eventJson"), it.getLong("createdAtMillis")) } else emptyList(),
                if (version >= 3) root.nullableString("transportStateBase64") else null,
                if (version >= 3) root.nullableString("keyPackageBundleBase64") else null,
                if (version >= 4) root.array("memberFriendLinks") { SharedMemberFriendLinkEntity(it.getString("memberId"), it.getString("friendId"), it.getLong("linkedAtMillis")) } else emptyList(),
            )
            validate(result)
            return result
        } catch (e: BackupFormatException) { throw e }
        catch (e: Exception) { throw BackupFormatException("Backup contents are invalid.", e) }
    }

    fun preview(s: AppBackupSnapshot) = BackupPreview(s.createdAtMillis, s.expenses.size, s.friends.size, s.splits.size, s.groups.size, s.events.size, s.identityPrivateKeyHex != null, s.mlsGroupStates.size, s.memberFriendLinks.size)

    fun validate(s: AppBackupSnapshot) {
        fun <T> unique(items: List<T>, key: (T) -> String, label: String) {
            if (items.size > maxRows || items.any { key(it).isBlank() } || items.map(key).toSet().size != items.size) {
                throw BackupFormatException("Backup has invalid $label records.")
            }
        }
        unique(s.expenses, Expense::id, "expense"); unique(s.friends, Friend::id, "friend")
        unique(s.splits, SplitTransaction::id, "split"); unique(s.groups, SharedGroupEntity::id, "group")
        unique(s.events, SharedLedgerEventRecord::id, "event")
        unique(s.transportOutbox, SharedTransportOutboxEntity::id, "transport outbox")
        unique(s.memberFriendLinks, SharedMemberFriendLinkEntity::memberId, "member/friend link")
        val friendIds = s.friends.mapTo(mutableSetOf(), Friend::id)
        if (s.memberFriendLinks.any { !it.memberId.matches(Regex("[0-9a-f]{64}")) || it.friendId !in friendIds }) {
            throw BackupFormatException("Backup contains invalid local member/friend links.")
        }
        if (s.members.size > maxRows || s.expenses.any { !it.amount.isFinite() } || s.splits.any { !it.amount.isFinite() }) {
            throw BackupFormatException("Backup contains invalid or excessive records.")
        }
        val groups = s.groups.mapTo(mutableSetOf(), SharedGroupEntity::id)
        if (s.transportOutbox.any { it.groupId !in groups || it.purpose != "welcome" || it.eventJson.length > 256 * 1024 }) {
            throw BackupFormatException("Backup contains an invalid pending invitation.")
        }
        val memberKeys = s.members.map { "${it.groupId}\u0000${it.memberId}" }
        if (memberKeys.toSet().size != memberKeys.size || s.members.any { it.groupId !in groups || it.memberId.isBlank() }) {
            throw BackupFormatException("Backup contains invalid group members.")
        }
        if (s.groups.any { !it.currencyCode.matches(Regex("[A-Z]{3}")) || runCatching { SharedSplitMoney.digits(it.currencyCode) }.isFailure } || s.events.any { it.groupId !in groups || it.schemaVersion != 1 }) {
            throw BackupFormatException("Backup contains unsupported group data.")
        }
        if (s.identityPrivateKeyHex != null && !s.identityPrivateKeyHex.matches(Regex("[0-9a-fA-F]{64}"))) {
            throw BackupFormatException("Backup contains an invalid identity key.")
        }
        listOf(s.transportStateBase64, s.keyPackageBundleBase64).filterNotNull().forEach { encoded ->
            if (runCatching { java.util.Base64.getDecoder().decode(encoded).size !in 1..(32 * 1024 * 1024) }.getOrDefault(true)) {
                throw BackupFormatException("Backup contains invalid protocol recovery data.")
            }
        }
        val stateIds = s.mlsGroupStates.map { it.groupId }
        if (stateIds.size != stateIds.toSet().size || s.mlsGroupStates.any {
                it.groupId !in groups || !it.groupId.matches(Regex("[0-9a-f]{64}")) || runCatching {
                    val decoded = java.util.Base64.getDecoder().decode(it.encodedStateBase64)
                    decoded.isEmpty() || decoded.size > 48 * 1024 * 1024
                }.getOrDefault(true)
            }) throw BackupFormatException("Backup contains invalid encrypted-group recovery data.")
        val knownEventIds = s.events.mapTo(mutableSetOf(), SharedLedgerEventRecord::id)
        s.groups.forEach { group ->
            val records = s.events.filter { it.groupId == group.id }
            val decoded = records.map(SharedLedgerCodec::toDomainEvent)
            if (records.any { record -> SharedLedgerCodec.decodeParents(record.parentEventIdsJson).any { it !in knownEventIds } }) {
                throw BackupFormatException("Backup is missing group event dependencies.")
            }
            val p = projectSharedLedger(group.id, decoded, group.currencyCode)
            if (p.pendingEventIds.isNotEmpty() || p.rejectedEvents.isNotEmpty()) throw BackupFormatException("Group event history cannot be replayed safely.")
        }
    }

    private fun <T> JSONObject.array(key: String, parser: (JSONObject) -> T): List<T> {
        val a = getJSONArray(key)
        if (a.length() > maxRows) throw BackupFormatException("Backup table is too large.")
        return List(a.length()) { parser(a.getJSONObject(it)) }
    }

    private fun Expense.toJson() = JSONObject().put("id", id).put("category", category).put("amount", amount).put("description", description).put("date", date)
    private fun Friend.toJson() = JSONObject().put("id", id).put("name", name).put("updatedAt", updatedAt).putNullable("phoneNumber", phoneNumber).putNullable("contactId", contactId)
    private fun SplitTransaction.toJson() = JSONObject().put("id", id).put("friendId", friendId).put("amount", amount).put("note", note).put("date", date).put("isSettled", isSettled).putNullable("settledAt", settledAt)
    private fun SharedGroupEntity.toJson() = JSONObject().put("id", id).put("name", name).put("currencyCode", currencyCode).put("createdAtMillis", createdAtMillis).put("updatedAtMillis", updatedAtMillis)
    private fun SharedGroupMemberEntity.toJson() = JSONObject().put("groupId", groupId).put("memberId", memberId).put("displayName", displayName).put("role", role).put("joinedAtMillis", joinedAtMillis).put("isActive", isActive)
    private fun SharedLedgerEventRecord.toJson() = JSONObject().put("id", id).put("groupId", groupId).put("schemaVersion", schemaVersion).put("actorMemberId", actorMemberId).put("actionType", actionType).put("payloadJson", payloadJson).put("parentEventIdsJson", parentEventIdsJson).put("createdAtMillis", createdAtMillis).put("description", description).putNullable("transportEventJson", transportEventJson).put("syncState", syncState)
    private fun JSONObject.putNullable(key: String, value: Any?) = put(key, value ?: JSONObject.NULL)
    private fun JSONObject.nullableString(key: String): String? = if (isNull(key)) null else getString(key)
    private fun JSONObject.nullableLong(key: String): Long? = if (isNull(key)) null else getLong(key)
}
