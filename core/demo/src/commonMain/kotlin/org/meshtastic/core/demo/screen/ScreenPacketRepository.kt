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
package org.meshtastic.core.demo.screen

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import org.meshtastic.core.model.ContactSettings
import org.meshtastic.core.model.DataPacket
import org.meshtastic.core.model.Message
import org.meshtastic.core.model.MessageStatus
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.Reaction
import org.meshtastic.core.repository.DemoMode
import org.meshtastic.core.repository.PacketRepository
import org.meshtastic.core.repository.PersistedPacket
import org.meshtastic.core.repository.PersistedPacketId
import org.meshtastic.core.repository.PersistedReaction
import org.meshtastic.core.repository.PersistedReactionId
import org.meshtastic.proto.MeshPacket

/**
 * The screens' [PacketRepository]: [real] while demo mode is off, [demo] while it is on. Marking read, deleting,
 * drafting, muting or pinning during a demo therefore only ever touches the demo history.
 */
@Suppress("TooManyFunctions") // Tous les membres sont imposés par l'interface.
class ScreenPacketRepository(
    private val real: PacketRepository,
    private val demo: PacketRepository,
    demoMode: DemoMode,
) : PacketRepository {
    private val switch = ScreenSwitch(demoMode)

    private fun current(): PacketRepository = switch.pick({ real }, { demo })

    override fun getWaypoints(): Flow<List<DataPacket>> = switch.flow({ real.getWaypoints() }, { demo.getWaypoints() })

    override fun getContacts(): Flow<Map<String, DataPacket>> =
        switch.flow({ real.getContacts() }, { demo.getContacts() })

    override fun getContactsPaged(): Flow<PagingData<Pair<String, DataPacket>>> =
        switch.flow({ real.getContactsPaged() }, { demo.getContactsPaged() })

    override suspend fun getMessageCount(contact: String): Int = current().getMessageCount(contact)

    override suspend fun getUnreadCount(contact: String): Int = current().getUnreadCount(contact)

    override fun getUnreadCountFlow(contact: String): Flow<Int> =
        switch.flow({ real.getUnreadCountFlow(contact) }, { demo.getUnreadCountFlow(contact) })

    override fun getFirstUnreadMessageUuid(contact: String): Flow<Long?> =
        switch.flow({ real.getFirstUnreadMessageUuid(contact) }, { demo.getFirstUnreadMessageUuid(contact) })

    override fun hasUnreadMessages(contact: String): Flow<Boolean> =
        switch.flow({ real.hasUnreadMessages(contact) }, { demo.hasUnreadMessages(contact) })

    override fun getUnreadCountTotal(): Flow<Int> =
        switch.flow({ real.getUnreadCountTotal() }, { demo.getUnreadCountTotal() })

    override suspend fun clearUnreadCount(contact: String, timestamp: Long) =
        current().clearUnreadCount(contact, timestamp)

    override suspend fun clearAllUnreadCounts() = current().clearAllUnreadCounts()

    override suspend fun updateLastReadMessage(contact: String, messageUuid: Long, lastReadTimestamp: Long) =
        current().updateLastReadMessage(contact, messageUuid, lastReadTimestamp)

    override suspend fun getQueuedPackets(): List<PersistedPacket> = current().getQueuedPackets()

    override suspend fun getEnroutePackets(): List<PersistedPacket> = current().getEnroutePackets()

    override suspend fun getEnrouteReactions(): List<PersistedReaction> = current().getEnrouteReactions()

    override suspend fun timeOutEnroutePacket(id: PersistedPacketId, routingError: Int): Boolean =
        current().timeOutEnroutePacket(id, routingError)

    override suspend fun timeOutEnrouteReaction(id: PersistedReactionId, routingError: Int): Boolean =
        current().timeOutEnrouteReaction(id, routingError)

    override suspend fun savePacket(
        myNodeNum: Int,
        contactKey: String,
        packet: DataPacket,
        receivedTime: Long,
        read: Boolean,
        filtered: Boolean,
    ): PersistedPacketId = current().savePacket(myNodeNum, contactKey, packet, receivedTime, read, filtered)

    override suspend fun getMessagesFrom(
        contact: String,
        limit: Int?,
        includeFiltered: Boolean,
        getNode: suspend (String?) -> Node,
    ): Flow<List<Message>> = switch.flow(
        { real.getMessagesFrom(contact, limit, includeFiltered, getNode) },
        { demo.getMessagesFrom(contact, limit, includeFiltered, getNode) },
    )

    override fun getMessagesFromPaged(contact: String, getNode: suspend (String?) -> Node): Flow<PagingData<Message>> =
        switch.flow({ real.getMessagesFromPaged(contact, getNode) }, { demo.getMessagesFromPaged(contact, getNode) })

    override fun getMessagesFromPaged(
        contactKey: String,
        includeFiltered: Boolean,
        getNode: suspend (String?) -> Node,
    ): Flow<PagingData<Message>> = switch.flow(
        { real.getMessagesFromPaged(contactKey, includeFiltered, getNode) },
        { demo.getMessagesFromPaged(contactKey, includeFiltered, getNode) },
    )

    override suspend fun updateMessageStatus(d: DataPacket, m: MessageStatus) = current().updateMessageStatus(d, m)

    override suspend fun updateMessageStatus(id: PersistedPacketId, status: MessageStatus) =
        current().updateMessageStatus(id, status)

    override suspend fun claimQueuedPacket(id: PersistedPacketId): PersistedPacket? = current().claimQueuedPacket(id)

    override suspend fun claimQueuedPacketByPacketIdIfUnique(packetId: Int): PersistedPacket? =
        current().claimQueuedPacketByPacketIdIfUnique(packetId)

    override suspend fun rollbackEnroutePacket(id: PersistedPacketId): Boolean = current().rollbackEnroutePacket(id)

    override suspend fun updateOutgoingMessageStatus(packet: MeshPacket, status: MessageStatus): PersistedPacketId? =
        current().updateOutgoingMessageStatus(packet, status)

    override suspend fun resolveOutgoingPacket(packet: MeshPacket): PersistedPacket? =
        current().resolveOutgoingPacket(packet)

    override suspend fun applyOutgoingQueueStatus(packet: MeshPacket, status: MessageStatus): PersistedPacket? =
        current().applyOutgoingQueueStatus(packet, status)

    override suspend fun applyOutgoingReactionQueueStatus(packetId: Int, status: MessageStatus): PersistedReaction? =
        current().applyOutgoingReactionQueueStatus(packetId, status)

    override suspend fun updateMessageId(d: DataPacket, id: Int) = current().updateMessageId(d, id)

    override suspend fun setMessageTranslation(uuid: Long, translatedText: String) =
        current().setMessageTranslation(uuid, translatedText)

    override suspend fun setShowTranslated(uuid: Long, showTranslated: Boolean) =
        current().setShowTranslated(uuid, showTranslated)

    override suspend fun deleteMessages(uuidList: List<Long>) = current().deleteMessages(uuidList)

    override suspend fun deleteContacts(contactList: List<String>) = current().deleteContacts(contactList)

    override suspend fun deleteWaypoint(id: Int) = current().deleteWaypoint(id)

    override fun getContactSettings(): Flow<Map<String, ContactSettings>> =
        switch.flow({ real.getContactSettings() }, { demo.getContactSettings() })

    override suspend fun getContactSettings(contact: String): ContactSettings = current().getContactSettings(contact)

    override suspend fun setMuteUntil(contacts: List<String>, until: Long) = current().setMuteUntil(contacts, until)

    override fun getFilteredCountFlow(contactKey: String): Flow<Int> =
        switch.flow({ real.getFilteredCountFlow(contactKey) }, { demo.getFilteredCountFlow(contactKey) })

    override suspend fun getFilteredCount(contactKey: String): Int = current().getFilteredCount(contactKey)

    override suspend fun setContactFilteringDisabled(contactKey: String, disabled: Boolean) =
        current().setContactFilteringDisabled(contactKey, disabled)

    override suspend fun setDraft(contactKey: String, draft: String) = current().setDraft(contactKey, draft)

    override suspend fun getDraft(contactKey: String): String = current().getDraft(contactKey)

    override suspend fun setPinned(contactKeys: List<String>, pinned: Boolean) =
        current().setPinned(contactKeys, pinned)

    override suspend fun markContactUnread(contactKey: String) = current().markContactUnread(contactKey)

    override suspend fun clearPacketDB() = current().clearPacketDB()

    override suspend fun updateFilteredBySender(senderId: String, filtered: Boolean) =
        current().updateFilteredBySender(senderId, filtered)

    override suspend fun getPacketByPacketId(packetId: Int): DataPacket? = current().getPacketByPacketId(packetId)

    override suspend fun getPacketByPacketIdIfUnique(packetId: Int): DataPacket? =
        current().getPacketByPacketIdIfUnique(packetId)

    override suspend fun getPacketByPersistedId(id: PersistedPacketId): DataPacket? =
        current().getPacketByPersistedId(id)

    override suspend fun getPacketById(id: Int): DataPacket? = current().getPacketById(id)

    override suspend fun insert(
        packet: DataPacket,
        myNodeNum: Int,
        contactKey: String,
        receivedTime: Long,
        read: Boolean,
        filtered: Boolean,
    ) = current().insert(packet, myNodeNum, contactKey, receivedTime, read, filtered)

    override suspend fun update(packet: DataPacket, routingError: Int) = current().update(packet, routingError)

    override suspend fun insertReaction(reaction: Reaction, myNodeNum: Int) =
        current().insertReaction(reaction, myNodeNum)

    override suspend fun updateReaction(reaction: Reaction) = current().updateReaction(reaction)

    override suspend fun getReactionByPacketId(packetId: Int): Reaction? = current().getReactionByPacketId(packetId)

    override suspend fun findPacketsWithId(packetId: Int): List<DataPacket> = current().findPacketsWithId(packetId)

    override suspend fun findReactionsWithId(packetId: Int): List<Reaction> = current().findReactionsWithId(packetId)

    override suspend fun updateSFPPStatus(
        packetId: Int,
        from: Int,
        to: Int,
        hash: ByteArray,
        status: MessageStatus,
        rxTime: Long,
        myNodeNum: Int?,
    ) = current().updateSFPPStatus(packetId, from, to, hash, status, rxTime, myNodeNum)

    override suspend fun updateSFPPStatusByHash(hash: ByteArray, status: MessageStatus, rxTime: Long) =
        current().updateSFPPStatusByHash(hash, status, rxTime)

    override fun searchMessages(query: String, contactKey: String?, getNode: (String?) -> Node): Flow<List<Message>> =
        switch.flow(
            { real.searchMessages(query, contactKey, getNode) },
            { demo.searchMessages(query, contactKey, getNode) },
        )
}
