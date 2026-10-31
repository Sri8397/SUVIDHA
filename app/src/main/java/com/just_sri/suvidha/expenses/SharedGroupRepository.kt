package com.just_sri.suvidha.expenses

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

/** Room persistence for local group metadata, membership, and deterministic ledger replay. */
internal class SharedGroupRepository(private val database: ExpenseDatabase) {
    private val groups = database.sharedGroups()
    private val members = database.sharedGroupMembers()
    private val events = database.sharedLedgerEvents()

    fun observeGroups(): Flow<List<SharedGroupEntity>> = groups.observeAll()

    suspend fun findGroup(groupId: String): SharedGroupEntity? = groups.findById(groupId)

    fun observeGroupEvents(groupId: String): Flow<List<SharedLedgerEventRecord>> = events.observeByGroup(groupId)

    fun observeGroupMembers(groupId: String): Flow<List<SharedGroupMemberEntity>> = members.observeByGroup(groupId)

    fun observeMemberFriendLinks(): Flow<List<SharedMemberFriendLinkEntity>> = database.sharedMemberFriendLinks().observeAll()

    suspend fun linkMemberToFriend(groupId: String, memberId: String, friendId: String) = database.withTransaction {
        require(memberId.matches(Regex("[0-9a-f]{64}")) && members.find(groupId, memberId) != null) { "Choose a member of this group." }
        require(database.friends().getAll().any { it.id == friendId }) { "This friend is no longer available." }
        database.sharedMemberFriendLinks().save(SharedMemberFriendLinkEntity(memberId, friendId, System.currentTimeMillis()))
    }

    suspend fun unlinkMemberFromFriend(memberId: String) = database.sharedMemberFriendLinks().delete(memberId)

    suspend fun createFriendForMember(groupId: String, memberId: String, name: String) = database.withTransaction {
        require(name.isNotBlank()) { "Enter a friend's name." }
        val friend = Friend(java.util.UUID.randomUUID().toString(), name.trim().take(100), System.currentTimeMillis())
        database.friends().save(friend)
        linkMemberToFriend(groupId, memberId, friend.id)
    }

    suspend fun linkMemberToContact(groupId: String, memberId: String, contactId: String, name: String, phone: String) = database.withTransaction {
        require(name.isNotBlank() && contactId.isNotBlank()) { "Choose a valid contact." }
        val friendId = "phone_contact_$contactId"
        val existing = database.friends().getAll().firstOrNull { it.contactId == contactId || it.id == friendId }
        val friend = existing ?: Friend(friendId, name.trim(), System.currentTimeMillis(), phone, contactId)
        if (existing == null) database.friends().save(friend)
        linkMemberToFriend(groupId, memberId, friend.id)
    }

    suspend fun createGroup(
        id: String,
        name: String,
        currencyCode: String,
        ownerMemberId: String,
        ownerDisplayName: String,
        nowMillis: Long = System.currentTimeMillis(),
    ): SharedGroupEntity = database.withTransaction {
        require(id.isNotBlank()) { "Group ID is required." }
        require(name.isNotBlank()) { "Group name is required." }
        require(currencyCode.matches(Regex("[A-Z]{3}")) && runCatching { SharedSplitMoney.digits(currencyCode) }.isSuccess) { "Use a supported three-letter uppercase currency code." }
        require(ownerMemberId.isNotBlank()) { "Owner member ID is required." }
        require(ownerDisplayName.isNotBlank()) { "Owner display name is required." }

        val group = SharedGroupEntity(
            id = id,
            name = name.trim(),
            currencyCode = currencyCode,
            createdAtMillis = nowMillis,
            updatedAtMillis = nowMillis,
        )
        groups.insert(group)
        members.insert(
            SharedGroupMemberEntity(
                groupId = id,
                memberId = ownerMemberId,
                displayName = ownerDisplayName.trim(),
                role = "owner",
                joinedAtMillis = nowMillis,
                isActive = true,
            )
        )
        group
    }

    suspend fun createJoinedGroup(
        id: String,
        name: String,
        currencyCode: String,
        activeMemberIds: Set<String>,
        adminMemberIds: Set<String>,
        nowMillis: Long = System.currentTimeMillis(),
    ) = database.withTransaction {
        require(id.matches(Regex("[0-9a-f]{64}"))) { "Invalid joined group ID." }
        require(name.isNotBlank()) { "Joined group is missing a name." }
        require(currencyCode.matches(Regex("[A-Z]{3}")) && runCatching { SharedSplitMoney.digits(currencyCode) }.isSuccess) { "Joined group has an invalid currency." }
        require(activeMemberIds.isNotEmpty() && activeMemberIds.all { it.matches(Regex("[0-9a-f]{64}")) }) {
            "Joined group has an invalid membership list."
        }
        if (groups.findById(id) == null) {
            groups.insert(SharedGroupEntity(id, name.trim(), currencyCode, nowMillis, nowMillis))
        }
        activeMemberIds.forEach { memberId ->
            val previous = members.find(id, memberId)
            members.upsert(
                SharedGroupMemberEntity(
                    groupId = id,
                    memberId = memberId,
                    displayName = previous?.displayName ?: "Nostr · ${memberId.take(8)}…",
                    role = if (memberId in adminMemberIds) "owner" else "member",
                    joinedAtMillis = previous?.joinedAtMillis ?: nowMillis,
                    isActive = true,
                ),
            )
        }
    }

    suspend fun appendLocalEvent(event: SharedLedgerEvent, transportEventJson: String? = null): SharedLedgerProjection = database.withTransaction {
        appendEvent(event, if (transportEventJson == null) "local" else "pending", requireActiveActor = true, allowPendingParents = false, transportEventJson = transportEventJson)
    }

    suspend fun appendRemoteEvent(event: SharedLedgerEvent, transportEventJson: String? = null): SharedLedgerProjection = database.withTransaction {
        // The MLS layer authenticates the sender. The Room roster can lag behind a valid
        // membership commit, and relays may deliver causally later events first.
        appendEvent(event, "received", requireActiveActor = false, allowPendingParents = true, transportEventJson = transportEventJson)
    }

    private suspend fun appendEvent(
        event: SharedLedgerEvent,
        initialSyncState: String,
        requireActiveActor: Boolean,
        allowPendingParents: Boolean,
        transportEventJson: String? = null,
    ): SharedLedgerProjection {
        val group = groups.findById(event.groupId) ?: error("Group does not exist.")
        require(!requireActiveActor || members.findActive(event.groupId, event.actorMemberId) != null) {
            "Only an active group member can add a local event."
        }

        val (actionType, payloadJson) = SharedLedgerCodec.encodeAction(event.action)
        val record = SharedLedgerEventRecord(
            id = event.eventId,
            groupId = event.groupId,
            schemaVersion = 1,
            actorMemberId = event.actorMemberId,
            actionType = actionType,
            payloadJson = payloadJson,
            parentEventIdsJson = SharedLedgerCodec.encodeParents(event.parentEventIds),
            createdAtMillis = event.occurredAtMillis,
            description = event.description,
            syncState = initialSyncState,
            transportEventJson = transportEventJson,
        )
        val previousRecord = events.findById(event.eventId)
        if (previousRecord != null) {
            require(SharedLedgerCodec.toDomainEvent(previousRecord) == event) { "A different event already uses this event ID." }
            return replay(group, events.getByGroup(event.groupId), members.getByGroup(event.groupId).find { it.role == "owner" }?.memberId)
        }

        val existing = events.getByGroup(event.groupId)
        val existingIds = existing.mapTo(mutableSetOf(), SharedLedgerEventRecord::id)
        require(allowPendingParents || event.parentEventIds.all { it in existingIds }) {
            "A local event must reference parent events already stored in this group."
        }
        val candidate = (existing + record).map(SharedLedgerCodec::toDomainEvent)
        val ownerMemberId = members.getByGroup(event.groupId).find { it.role == "owner" }?.memberId
        val projection = projectSharedLedger(event.groupId, candidate, group.currencyCode, ownerMemberId)
        require(event.eventId in projection.acceptedEventIds || (allowPendingParents && event.eventId in projection.pendingEventIds)) {
            projection.rejectedEvents[event.eventId] ?: "Event cannot be applied until its dependencies arrive."
        }

        events.insert(record)
        groups.updateTimestamp(group.id, maxOf(group.updatedAtMillis, event.occurredAtMillis))
        if (event.action is SharedLedgerAction.GroupRenamed) applyLatestRename(event.groupId, projection.acceptedEventIds)
        return projection
    }

    /** Admin renames are replayed in causal-time order so every device settles on the same name. */
    private suspend fun applyLatestRename(groupId: String, acceptedEventIds: Set<String>) {
        val admins = members.getByGroup(groupId).filter { it.role == "owner" }.mapTo(mutableSetOf()) { it.memberId }
        val latest = events.getByGroup(groupId)
            .filter { it.actionType == "group_renamed" && it.id in acceptedEventIds && it.actorMemberId in admins }
            .maxWithOrNull(compareBy<SharedLedgerEventRecord>({ it.createdAtMillis }, { it.id })) ?: return
        val name = (SharedLedgerCodec.decodeAction(latest.actionType, latest.payloadJson) as SharedLedgerAction.GroupRenamed).name.trim()
        if (name.isNotBlank()) groups.updateName(groupId, name)
    }

    suspend fun persistEncryptedEvent(eventId: String, eventJson: String) = database.withTransaction {
        require(events.findById(eventId) != null) { "Shared ledger event does not exist." }
        events.setTransportEvent(eventId, eventJson)
    }

    suspend fun markEventPublished(eventId: String) = events.markPublished(eventId)

    suspend fun pendingEncryptedEvents(groupId: String): List<SharedLedgerEventRecord> = events.getPendingTransportEvents(groupId)

    suspend fun storedTransportEvents(): List<String> = events.getAll().mapNotNull { it.transportEventJson }

    suspend fun queueWelcome(groupId: String, memberId: String, eventId: String, eventJson: String) = database.withTransaction {
        addInvitedMember(groupId, memberId)
        database.sharedTransportOutbox().insert(SharedTransportOutboxEntity(eventId, groupId, "welcome", eventJson, System.currentTimeMillis()))
    }

    suspend fun pendingWelcomes(): List<SharedTransportOutboxEntity> = database.sharedTransportOutbox().getAll()

    fun observePendingWelcomes(): Flow<List<SharedTransportOutboxEntity>> = database.sharedTransportOutbox().observeAll()

    suspend fun markWelcomePublished(eventId: String) = database.sharedTransportOutbox().delete(eventId)

    suspend fun syncMemberRoster(groupId: String, activeMemberIds: Set<String>, ownerMemberIds: Set<String> = emptySet()) = database.withTransaction {
        require(groups.findById(groupId) != null) { "Group does not exist." }
        val current = members.getAll().filter { it.groupId == groupId }.associateBy(SharedGroupMemberEntity::memberId)
        current.values.filter { it.isActive && it.memberId !in activeMemberIds }.forEach {
            members.markInactive(groupId, it.memberId)
        }
        activeMemberIds.forEach { memberId ->
            if (!memberId.matches(Regex("[0-9a-f]{64}"))) return@forEach
            val existing = current[memberId]
            members.upsert(
                SharedGroupMemberEntity(
                    groupId = groupId,
                    memberId = memberId,
                    displayName = existing?.displayName ?: "Nostr · ${memberId.take(8)}…",
                    role = if (memberId in ownerMemberIds) "owner" else "member",
                    joinedAtMillis = existing?.joinedAtMillis ?: System.currentTimeMillis(),
                    isActive = true,
                ),
            )
        }
    }

    suspend fun addInvitedMember(groupId: String, memberId: String, nowMillis: Long = System.currentTimeMillis()) = database.withTransaction {
        require(groups.findById(groupId) != null) { "Group does not exist." }
        require(memberId.matches(Regex("[0-9a-f]{64}"))) { "The invitee public key is invalid." }
        val existing = members.find(groupId, memberId)
        if (existing == null) {
            members.insert(
                SharedGroupMemberEntity(
                    groupId = groupId,
                    memberId = memberId,
                    displayName = "Nostr · ${memberId.take(8)}…",
                    role = "member",
                    joinedAtMillis = nowMillis,
                    isActive = true,
                ),
            )
        } else if (!existing.isActive) {
            members.upsert(existing.copy(isActive = true, joinedAtMillis = nowMillis))
        }
    }

    suspend fun deleteEvent(id: String) = events.delete(id)

    suspend fun replayGroup(groupId: String): SharedLedgerProjection {
        val group = groups.findById(groupId) ?: error("Group does not exist.")
        val ownerMemberId = members.getByGroup(groupId).find { it.role == "owner" }?.memberId
        return replay(group, events.getByGroup(groupId), ownerMemberId)
    }

    private fun replay(
        group: SharedGroupEntity,
        records: List<SharedLedgerEventRecord>,
        ownerMemberId: String? = null,
    ): SharedLedgerProjection = projectSharedLedger(
        groupId = group.id,
        events = records.map(SharedLedgerCodec::toDomainEvent),
        currencyCode = group.currencyCode,
        ownerMemberId = ownerMemberId,
    )
}
