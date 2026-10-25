package com.just_sri.suvidha.expenses

import android.content.Context
import android.telephony.PhoneNumberUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale

/** Keeps the existing Room UI backed by a private Firestore collection for each Firebase UID. */
class FirebaseSync(private val context: Context, private val database: ExpenseDatabase) {
    private val firestore = FirebaseFirestore.getInstance()
    private val preferences = context.getSharedPreferences("firebase_sync", Context.MODE_PRIVATE)
    private var activeJob: Job? = null
    @Volatile private var activeUid: String? = null
    private val registrations = mutableListOf<ListenerRegistration>()
    private val phoneToUidCache = java.util.concurrent.ConcurrentHashMap<String, String>()

    fun start(uid: String, scope: CoroutineScope, onReady: () -> Unit = {}, onError: (String) -> Unit = {}) {
        stop()
        activeUid = uid
        // Notify immediately so local UI is never blocked
        onReady()

        activeJob = scope.launch(Dispatchers.IO) {
          try {
            val expenses = database.expenses()
            val friends = database.friendBalances()
            val splits = database.splitTransactions()

            registerUser(uid)

            preferences.edit().putString("owner_uid", uid).apply()

            // Real-time listeners: when online or from cache, merge remote records into local Room
            registrations += collection(uid, "expenses").addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null && activeUid == uid) scope.launch(Dispatchers.IO) {
                    try {
                        val incoming = snapshot.toExpenses()
                        if (incoming.isNotEmpty()) expenses.insertAll(incoming)
                    } catch (_: Exception) {}
                }
            }
            registrations += collection(uid, "friends").addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null && activeUid == uid) scope.launch(Dispatchers.IO) {
                    try {
                        val incoming = snapshot.toFriends()
                        if (incoming.isNotEmpty()) friends.insertAll(incoming)
                    } catch (_: Exception) {}
                }
            }
            // Backward-compatible listener for existing friend_balances
            registrations += collection(uid, "friend_balances").addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null && activeUid == uid) scope.launch(Dispatchers.IO) {
                    try {
                        val incoming = snapshot.toFriends()
                        if (incoming.isNotEmpty()) friends.insertAll(incoming)
                    } catch (_: Exception) {}
                }
            }
            registrations += collection(uid, "split_transactions").addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null && activeUid == uid) scope.launch(Dispatchers.IO) {
                    try {
                        val incoming = snapshot.toSplits()
                        if (incoming.isNotEmpty()) splits.insertAll(incoming)
                    } catch (_: Exception) {}
                }
            }
            registrations += firestore.collection("shared_splits")
                .whereEqualTo("recipientUid", uid)
                .addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null && activeUid == uid) scope.launch(Dispatchers.IO) {
                        try {
                            processSharedSplitsSnapshot(uid, snapshot)
                        } catch (_: Exception) {}
                    }
                }

            // Sync Room database changes to Firestore (works offline via Firestore offline queue)
            launch { expenses.observeAll().collectLatest { rows -> syncCollection(uid, "expenses", rows) { it.id to it.toMap() } } }
            launch { friends.observeAll().collectLatest { rows -> syncCollection(uid, "friends", rows.filterNot { it.id.startsWith("shared_user_") }) { it.id to it.toMap() } } }
            launch { splits.observeAll().collectLatest { rows -> syncCollection(uid, "split_transactions", rows.filterNot { it.isShared }) { it.id to it.toMap() } } }
          } catch (_: Exception) {
            // Keep app completely functional offline
          }
        }
    }

    fun deleteItem(collectionName: String, id: String) {
        val uid = activeUid ?: return
        try {
            collection(uid, collectionName).document(id).delete()
        } catch (_: Exception) {}
    }

    suspend fun shareSplitToContact(transaction: SplitTransaction, friend: FriendBalance): Boolean {
        val ownerUid = activeUid ?: return false
        val phone = friend.phoneNumber?.let(::normalizePhoneNumber) ?: return false
        return try {
            val recipient = firestore.collection("user_directory").document(phone).get().await()
            val recipientUid = recipient.getString("uid") ?: return false
            if (recipientUid == ownerUid) return false
            firestore.collection("shared_splits").document(transaction.id).set(
                mapOf(
                    "ownerUid" to ownerUid,
                    "recipientUid" to recipientUid,
                    "amount" to transaction.amount,
                    "note" to transaction.note,
                    "date" to transaction.date,
                    "contentHash" to sharedSplitHash(ownerUid, recipientUid, transaction.amount, transaction.note, transaction.date),
                    "isSettled" to transaction.isSettled,
                    "settledAt" to transaction.settledAt
                )
            ).await()
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun refreshSharedSplits() {
        val uid = activeUid ?: return
        try {
            val snapshot = firestore.collection("shared_splits")
                .whereEqualTo("recipientUid", uid)
                .get()
                .await()
            processSharedSplitsSnapshot(uid, snapshot)
        } catch (_: Exception) {}
    }

    private suspend fun processSharedSplitsSnapshot(uid: String, snapshot: QuerySnapshot) {
        val splits = database.splitTransactions()
        val friends = database.friendBalances()

        val received = snapshot.documents.filter { document ->
            val ownerUid = document.getString("ownerUid") ?: return@filter false
            val recipientUid = document.getString("recipientUid") ?: return@filter false
            val amount = document.getDouble("amount") ?: return@filter false
            val note = document.getString("note") ?: return@filter false
            val date = document.getLong("date") ?: return@filter false
            document.getString("contentHash") == sharedSplitHash(ownerUid, recipientUid, amount, note, date)
        }
        splits.deleteShared()
        friends.deleteSharedUsers()

        val localFriends = friends.observeAll().firstValue()
        val linkedFriendsByUid = mutableMapOf<String, FriendBalance>()
        localFriends.filter { !it.phoneNumber.isNullOrBlank() }.forEach { localFriend ->
            findRegisteredUserId(localFriend.phoneNumber!!)?.let { linkedFriendsByUid[it] = localFriend }
        }

        val transactions = received.mapNotNull { document ->
            val amount = document.getDouble("amount") ?: return@mapNotNull null
            val ownerUid = document.getString("ownerUid") ?: return@mapNotNull null
            val targetFriendId = linkedFriendsByUid[ownerUid]?.id ?: "shared_user_$ownerUid"
            SplitTransaction(
                id = "shared_${document.id}",
                friendId = targetFriendId,
                amount = -amount,
                note = document.getString("note") ?: "Split bill",
                date = document.getLong("date") ?: 0L,
                isSettled = document.getBoolean("isSettled") ?: false,
                settledAt = document.getLong("settledAt"),
                isShared = true
            )
        }
        if (transactions.isNotEmpty()) splits.insertAll(transactions)

        val friendsByOwner = received.mapNotNull { document ->
            val ownerUid = document.getString("ownerUid") ?: return@mapNotNull null
            ownerUid to document
        }.groupBy({ it.first }, { it.second })

        val ownerUids = (friendsByOwner.keys + linkedFriendsByUid.keys).distinct()
        val sharedFriends = ownerUids.mapNotNull { ownerUid ->
            val documents = friendsByOwner[ownerUid].orEmpty()
            val linkedFriend = linkedFriendsByUid[ownerUid]
            if (linkedFriend == null && documents.isEmpty()) return@mapNotNull null
            val friendId = linkedFriend?.id ?: "shared_user_$ownerUid"
            val profileName = if (linkedFriend == null) {
                try {
                    firestore.collection("user_profiles").document(ownerUid).get().await().getString("displayName")
                } catch (_: Exception) { null }
            } else null
            FriendBalance(
                id = friendId,
                name = linkedFriend?.name ?: profileName?.takeIf { it.isNotBlank() } ?: "Shared friend",
                updatedAt = maxOf(linkedFriend?.updatedAt ?: 0L, documents.maxOfOrNull { it.getLong("date") ?: 0L } ?: 0L),
                phoneNumber = linkedFriend?.phoneNumber,
                contactId = linkedFriend?.contactId
            )
        }
        if (sharedFriends.isNotEmpty()) friends.insertAll(sharedFriends)
    }

    suspend fun findRegisteredUserId(phoneNumber: String): String? {
        val phone = normalizePhoneNumber(phoneNumber) ?: return null
        phoneToUidCache[phone]?.let { return it }
        return try {
            val uid = firestore.collection("user_directory").document(phone).get().await().getString("uid")
            if (uid != null) phoneToUidCache[phone] = uid
            uid
        } catch (_: Exception) {
            null
        }
    }

    suspend fun settleSharedSplit(transactionId: String, settledAt: Long) {
        if (activeUid == null) return
        try {
            firestore.collection("shared_splits").document(transactionId).update(
                mapOf("isSettled" to true, "settledAt" to settledAt)
            ).await()
        } catch (_: Exception) {}
    }

    suspend fun deleteSharedSplit(transactionId: String) {
        if (activeUid == null) return
        try {
            firestore.collection("shared_splits").document(transactionId).delete().await()
        } catch (_: Exception) {}
    }

    fun stop() {
        activeJob?.cancel()
        activeJob = null
        activeUid = null
        phoneToUidCache.clear()
        registrations.forEach { it.remove() }
        registrations.clear()
    }

    private fun collection(uid: String, name: String) =
        firestore.collection("users").document(uid).collection(name)

    private suspend fun registerUser(uid: String) {
        val authUser = FirebaseAuth.getInstance().currentUser ?: return
        val phone = authUser.phoneNumber?.let(::normalizePhoneNumber) ?: return
        try {
            firestore.collection("users").document(uid).set(
                mapOf(
                    "phoneNumber" to phone,
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()
            firestore.collection("user_directory").document(phone).set(
                mapOf(
                    "uid" to uid,
                    "phoneNumber" to phone,
                    "displayName" to (authUser.displayName?.takeIf { it.isNotBlank() } ?: "Suvidha user")
                )
            ).await()
            firestore.collection("user_profiles").document(uid).set(
                mapOf("phoneNumber" to phone, "displayName" to (authUser.displayName?.takeIf { it.isNotBlank() } ?: "Suvidha user"))
            ).await()
        } catch (_: Exception) {}
    }

    private fun normalizePhoneNumber(number: String): String? {
        return PhoneUtils.normalizeToE164(number)
    }

    private fun sharedSplitHash(ownerUid: String, recipientUid: String, amount: Double, note: String, date: Long): String {
        val fields = listOf(ownerUid, recipientUid, java.lang.Double.toHexString(amount), date.toString(), note)
        val canonical = fields.joinToString(separator = "") { "${it.length}:$it" }
        val digest = MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }

    private suspend fun <T> syncCollection(uid: String, name: String, rows: List<T>, encode: (T) -> Pair<String, Map<String, Any?>>) {
        try {
            val col = collection(uid, name)
            rows.forEach { row ->
                val (id, data) = encode(row)
                col.document(id).set(data)
            }
        } catch (_: Exception) {}
    }

    private fun Expense.toMap() = mapOf("category" to category, "amount" to amount, "description" to description, "date" to date)
    private fun FriendBalance.toMap() = mapOf(
        "name" to name,
        "updatedAt" to updatedAt,
        "phoneNumber" to phoneNumber,
        "contactId" to contactId
    )
    private fun SplitTransaction.toMap() = mapOf("friendId" to friendId, "amount" to amount, "note" to note, "date" to date, "isSettled" to isSettled, "settledAt" to settledAt, "isShared" to isShared)

    private fun QuerySnapshot.toExpenses() = documents.mapNotNull { d ->
        val amount = d.getDouble("amount") ?: return@mapNotNull null
        Expense(d.id, d.getString("category") ?: "Other", amount, d.getString("description") ?: "", d.getLong("date") ?: 0L)
    }
    private fun QuerySnapshot.toFriends() = documents.mapNotNull { d ->
        FriendBalance(
            id = d.id,
            name = d.getString("name") ?: "",
            updatedAt = d.getLong("updatedAt") ?: 0L,
            phoneNumber = d.getString("phoneNumber"),
            contactId = d.getString("contactId")
        )
    }
    private fun QuerySnapshot.toSplits() = documents.mapNotNull { d ->
        val amount = d.getDouble("amount") ?: return@mapNotNull null
        SplitTransaction(d.id, d.getString("friendId") ?: "", amount, d.getString("note") ?: "", d.getLong("date") ?: 0L, d.getBoolean("isSettled") ?: false, d.getLong("settledAt"), d.getBoolean("isShared") ?: false)
    }

    private suspend fun <T> kotlinx.coroutines.flow.Flow<List<T>>.firstValue(): List<T> = first()
}
