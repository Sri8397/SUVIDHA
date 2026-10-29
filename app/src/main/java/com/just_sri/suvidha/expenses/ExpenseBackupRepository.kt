package com.just_sri.suvidha.expenses

import androidx.room.withTransaction

/** Consistent local snapshot and all-or-nothing replacement for manual recovery. */
internal class ExpenseBackupRepository(private val database: ExpenseDatabase) {
    suspend fun createSnapshot(
        identityPrivateKeyHex: String? = null,
        mlsGroupStates: List<BackupMlsGroupState> = emptyList(),
        transportStateBase64: String? = null,
        keyPackageBundleBase64: String? = null,
    ): AppBackupSnapshot = database.withTransaction {
        AppBackupSnapshot(
            createdAtMillis = System.currentTimeMillis(),
            expenses = database.expenses().getAll(),
            friends = database.friends().getAll(),
            splits = database.splitTransactions().getAll(),
            groups = database.sharedGroups().getAll(),
            members = database.sharedGroupMembers().getAll(),
            events = database.sharedLedgerEvents().getAll(),
            identityPrivateKeyHex = identityPrivateKeyHex,
            mlsGroupStates = mlsGroupStates,
            transportOutbox = database.sharedTransportOutbox().getAll(),
            transportStateBase64 = transportStateBase64,
            keyPackageBundleBase64 = keyPackageBundleBase64,
            memberFriendLinks = database.sharedMemberFriendLinks().getAll(),
        ).also(BackupJsonCodec::validate)
    }

    suspend fun replaceWith(snapshot: AppBackupSnapshot) = database.withTransaction {
        BackupJsonCodec.validate(snapshot)
        database.sharedLedgerEvents().deleteAll()
        database.sharedMemberFriendLinks().deleteAll()
        database.sharedTransportOutbox().deleteAll()
        database.sharedGroupMembers().deleteAll()
        database.sharedGroups().deleteAll()
        database.splitTransactions().deleteAll()
        database.friends().deleteAll()
        database.expenses().deleteAll()
        database.expenses().restoreAll(snapshot.expenses)
        database.friends().restoreAll(snapshot.friends)
        database.splitTransactions().restoreAll(snapshot.splits)
        database.sharedGroups().restoreAll(snapshot.groups)
        database.sharedGroupMembers().restoreAll(snapshot.members)
        database.sharedLedgerEvents().restoreAll(snapshot.events)
        database.sharedTransportOutbox().restoreAll(snapshot.transportOutbox)
        database.sharedMemberFriendLinks().restoreAll(snapshot.memberFriendLinks)
    }
}
