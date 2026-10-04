/*
 * Copyright (c) 2026 Meshtastic LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.meshtastic.core.demo.repository

import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import org.meshtastic.core.demo.data.DemoPacket
import org.meshtastic.core.demo.store.DemoStore
import org.meshtastic.core.model.ContactSettings
import org.meshtastic.core.model.DataPacket
import org.meshtastic.core.model.Message
import org.meshtastic.core.model.MessageStatus
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.NodeAddress
import org.meshtastic.core.model.Reaction
import org.meshtastic.core.model.util.getShortDateTime
import org.meshtastic.core.repository.PacketRepository
import org.meshtastic.core.repository.PersistedPacket
import org.meshtastic.core.repository.PersistedPacketId
import org.meshtastic.core.repository.PersistedReaction
import org.meshtastic.core.repository.PersistedReactionId
import org.meshtastic.proto.MeshPacket

/**
 * [PacketRepository] over the demo's conversations. Reading, marking read, drafts, muting, pinning and deleting all act
 * on the in-memory history only. The radio-queue members belong to the radio service, which never uses this class: they
 * find nothing.
 */
@Suppress("TooManyFunctions") // Tous les membres sont imposés par l'interface.
class DemoPacketRepository(private val store: DemoStore) : PacketRepository {

    private val complete = LoadState.NotLoading(endOfPaginationReached = true)
    private val allLoaded = LoadStates(refresh = complete, prepend = complete, append = complete)

    private val myNodeNum: Int
        get() = store.myNodeInfo.value?.myNodeNum ?: 0

    private fun DemoPacket.isFromMe(): Boolean = packet.from == store.myId.value || packet.from == NodeAddress.ID_LOCAL

    private fun DemoPacket.isUnread(): Boolean = !read && !isFromMe()

    private fun conversation(packets: List<DemoPacket>, contact: String): List<DemoPacket> =
        packets.filter { it.contactKey == contact }.sortedByDescending { it.receivedTime }

    private suspend fun DemoPacket.toMessage(getNode: suspend (String?) -> Node): Message {
        val node = getNode(packet.from)
        return Message(
            uuid = uuid,
            receivedTime = receivedTime,
            node = node,
            text = packet.text.orEmpty(),
            fromLocal = node.user.id == NodeAddress.ID_LOCAL || node.num == myNodeNum,
            time = getShortDateTime(packet.time.takeIf { it > 0 } ?: receivedTime),
            meshTime = packet.time,
            read = read,
            status = packet.status,
            routingError = routingError,
            packetId = packet.id,
            emojis = emptyList(),
            snr = packet.snr,
            rssi = packet.rssi,
            hopsAway = hopsAway,
            replyId = packet.replyId,
        )
    }

    override fun getWaypoints(): Flow<List<DataPacket>> = flowOf(emptyList())

    override fun getContacts(): Flow<Map<String, DataPacket>> = store.packets.map { packets ->
        packets
            .groupBy { it.contactKey }
            .mapValues { (_, conversation) -> conversation.maxBy { it.receivedTime }.packet }
    }

    override fun getContactsPaged(): Flow<PagingData<Pair<String, DataPacket>>> = getContacts().map { contacts ->
        val latestFirst =
            contacts.entries
                .sortedByDescending { it.value.time }
                .map { (contactKey, packet) -> contactKey to packet }
        PagingData.from(latestFirst, sourceLoadStates = allLoaded)
    }

    override suspend fun getMessageCount(contact: String): Int = store.packets.value.count { it.contactKey == contact }

    override suspend fun getUnreadCount(contact: String): Int =
        store.packets.value.count { it.contactKey == contact && it.isUnread() }

    override fun getUnreadCountFlow(contact: String): Flow<Int> =
        store.packets.map { packets -> packets.count { it.contactKey == contact && it.isUnread() } }

    override fun getFirstUnreadMessageUuid(contact: String): Flow<Long?> = store.packets.map { packets ->
        packets.filter { it.contactKey == contact && it.isUnread() }.minByOrNull { it.receivedTime }?.uuid
    }

    override fun hasUnreadMessages(contact: String): Flow<Boolean> = getUnreadCountFlow(contact).map { it > 0 }

    override fun getUnreadCountTotal(): Flow<Int> = store.packets.map { packets -> packets.count { it.isUnread() } }

    override suspend fun clearUnreadCount(contact: String, timestamp: Long) {
        markRead { it.contactKey == contact && it.receivedTime <= timestamp }
    }

    override suspend fun clearAllUnreadCounts() {
        markRead { true }
    }

    override suspend fun updateLastReadMessage(contact: String, messageUuid: Long, lastReadTimestamp: Long) {
        store.updateContactSettings(contact) {
            it.copy(lastReadMessageUuid = messageUuid, lastReadMessageTimestamp = lastReadTimestamp)
        }
        markRead { it.contactKey == contact && it.receivedTime <= lastReadTimestamp }
    }

    private fun markRead(which: (DemoPacket) -> Boolean) {
        store.packets.update { packets -> packets.map { if (which(it)) it.copy(read = true) else it } }
    }

    override suspend fun getQueuedPackets(): List<PersistedPacket> = emptyList()

    override suspend fun getEnroutePackets(): List<PersistedPacket> = emptyList()

    override suspend fun getEnrouteReactions(): List<PersistedReaction> = emptyList()

    override suspend fun timeOutEnroutePacket(id: PersistedPacketId, routingError: Int): Boolean = false

    override suspend fun timeOutEnrouteReaction(id: PersistedReactionId, routingError: Int): Boolean = false

    override suspend fun savePacket(
        myNodeNum: Int,
        contactKey: String,
        packet: DataPacket,
        receivedTime: Long,
        read: Boolean,
        filtered: Boolean,
    ): PersistedPacketId {
        val uuid = store.nextUuid()
        val stored = DemoPacket(uuid, contactKey, packet.copy(), receivedTime, read, hopsAway = 0)
        store.packets.update { it + stored }
        return PersistedPacketId(myNodeNum, uuid)
    }

    override suspend fun getMessagesFrom(
        contact: String,
        limit: Int?,
        includeFiltered: Boolean,
        getNode: suspend (String?) -> Node,
    ): Flow<List<Message>> = store.packets.map { packets ->
        val conversation = conversation(packets, contact)
        (if (limit != null) conversation.take(limit) else conversation).map { it.toMessage(getNode) }
    }

    override fun getMessagesFromPaged(contact: String, getNode: suspend (String?) -> Node): Flow<PagingData<Message>> =
        getMessagesFromPaged(contact, includeFiltered = true, getNode = getNode)

    override fun getMessagesFromPaged(
        contactKey: String,
        includeFiltered: Boolean,
        getNode: suspend (String?) -> Node,
    ): Flow<PagingData<Message>> = store.packets.map { packets ->
        PagingData.from(
            conversation(packets, contactKey).map { it.toMessage(getNode) },
            sourceLoadStates = allLoaded,
        )
    }

    override suspend fun updateMessageStatus(d: DataPacket, m: MessageStatus) {
        updatePackets({ it.packet.id == d.id }) { it.copy(packet = it.packet.copy(status = m)) }
    }

    override suspend fun updateMessageStatus(id: PersistedPacketId, status: MessageStatus) {
        updatePackets({ it.uuid == id.uuid }) { it.copy(packet = it.packet.copy(status = status)) }
    }

    /** Changes the state of a message sent during the demo, as the simulated radio moves it along. */
    fun updateStatus(uuid: Long, status: MessageStatus, routingError: Int = 0) {
        updatePackets({ it.uuid == uuid }) {
            it.copy(packet = it.packet.copy(status = status), routingError = routingError)
        }
    }

    private fun updatePackets(which: (DemoPacket) -> Boolean, transform: (DemoPacket) -> DemoPacket) {
        store.packets.update { packets -> packets.map { if (which(it)) transform(it) else it } }
    }

    override suspend fun claimQueuedPacket(id: PersistedPacketId): PersistedPacket? = null

    override suspend fun claimQueuedPacketByPacketIdIfUnique(packetId: Int): PersistedPacket? = null

    override suspend fun rollbackEnroutePacket(id: PersistedPacketId): Boolean = false

    override suspend fun updateOutgoingMessageStatus(packet: MeshPacket, status: MessageStatus): PersistedPacketId? =
        null

    override suspend fun resolveOutgoingPacket(packet: MeshPacket): PersistedPacket? = null

    override suspend fun applyOutgoingQueueStatus(packet: MeshPacket, status: MessageStatus): PersistedPacket? = null

    override suspend fun applyOutgoingReactionQueueStatus(packetId: Int, status: MessageStatus): PersistedReaction? =
        null

    override suspend fun updateMessageId(d: DataPacket, id: Int) {
        updatePackets({ it.packet.id == d.id }) { it.copy(packet = it.packet.copy(id = id)) }
    }

    override suspend fun setMessageTranslation(uuid: Long, translatedText: String) = Unit

    override suspend fun setShowTranslated(uuid: Long, showTranslated: Boolean) = Unit

    override suspend fun deleteMessages(uuidList: List<Long>) {
        val doomed = uuidList.toSet()
        store.packets.update { packets -> packets.filterNot { it.uuid in doomed } }
    }

    override suspend fun deleteContacts(contactList: List<String>) {
        val doomed = contactList.toSet()
        store.packets.update { packets -> packets.filterNot { it.contactKey in doomed } }
        store.contactSettings.update { it - doomed }
    }

    override suspend fun deleteWaypoint(id: Int) = Unit

    override fun getContactSettings(): Flow<Map<String, ContactSettings>> = store.contactSettings

    override suspend fun getContactSettings(contact: String): ContactSettings = store.contactSettings(contact)

    override suspend fun setMuteUntil(contacts: List<String>, until: Long) {
        contacts.forEach { contact -> store.updateContactSettings(contact) { it.copy(muteUntil = until) } }
    }

    override fun getFilteredCountFlow(contactKey: String): Flow<Int> = flowOf(0)

    override suspend fun getFilteredCount(contactKey: String): Int = 0

    override suspend fun setContactFilteringDisabled(contactKey: String, disabled: Boolean) {
        store.updateContactSettings(contactKey) { it.copy(filteringDisabled = disabled) }
    }

    override suspend fun setDraft(contactKey: String, draft: String) {
        store.updateContactSettings(contactKey) { it.copy(draft = draft) }
    }

    override suspend fun getDraft(contactKey: String): String = store.contactSettings(contactKey).draft

    override suspend fun setPinned(contactKeys: List<String>, pinned: Boolean) {
        contactKeys.forEach { contact -> store.updateContactSettings(contact) { it.copy(pinned = pinned) } }
    }

    override suspend fun markContactUnread(contactKey: String) {
        val latest =
            store.packets.value.filter { it.contactKey == contactKey && !it.isFromMe() }.maxByOrNull { it.receivedTime }
        if (latest != null) updatePackets({ it.uuid == latest.uuid }) { it.copy(read = false) }
    }

    override suspend fun clearPacketDB() {
        store.packets.value = emptyList()
    }

    override suspend fun updateFilteredBySender(senderId: String, filtered: Boolean) = Unit

    override suspend fun getPacketByPacketId(packetId: Int): DataPacket? =
        store.packets.value.firstOrNull { it.packet.id == packetId }?.packet

    override suspend fun getPacketByPacketIdIfUnique(packetId: Int): DataPacket? =
        store.packets.value.filter { it.packet.id == packetId }.singleOrNull()?.packet

    override suspend fun getPacketByPersistedId(id: PersistedPacketId): DataPacket? =
        store.packets.value.firstOrNull { it.uuid == id.uuid }?.packet

    override suspend fun getPacketById(id: Int): DataPacket? = getPacketByPacketId(id)

    override suspend fun insert(
        packet: DataPacket,
        myNodeNum: Int,
        contactKey: String,
        receivedTime: Long,
        read: Boolean,
        filtered: Boolean,
    ) {
        savePacket(myNodeNum, contactKey, packet, receivedTime, read, filtered)
    }

    override suspend fun update(packet: DataPacket, routingError: Int) {
        updatePackets({ it.packet.id == packet.id }) {
            it.copy(
                packet = packet.copy(),
                routingError = routingError.takeIf { error -> error >= 0 } ?: it.routingError,
            )
        }
    }

    override suspend fun insertReaction(reaction: Reaction, myNodeNum: Int) = Unit

    override suspend fun updateReaction(reaction: Reaction) = Unit

    override suspend fun getReactionByPacketId(packetId: Int): Reaction? = null

    override suspend fun findPacketsWithId(packetId: Int): List<DataPacket> =
        store.packets.value.filter { it.packet.id == packetId }.map { it.packet }

    override suspend fun findReactionsWithId(packetId: Int): List<Reaction> = emptyList()

    override suspend fun updateSFPPStatus(
        packetId: Int,
        from: Int,
        to: Int,
        hash: ByteArray,
        status: MessageStatus,
        rxTime: Long,
        myNodeNum: Int?,
    ) = Unit

    override suspend fun updateSFPPStatusByHash(hash: ByteArray, status: MessageStatus, rxTime: Long) = Unit

    override fun searchMessages(query: String, contactKey: String?, getNode: (String?) -> Node): Flow<List<Message>> =
        store.packets.map { packets ->
            packets
                .filter { (contactKey == null || it.contactKey == contactKey) && query.isNotBlank() }
                .filter { it.packet.text.orEmpty().contains(query, ignoreCase = true) }
                .sortedByDescending { it.receivedTime }
                .map { it.toMessage { id -> getNode(id) } }
        }
}
