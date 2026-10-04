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
package org.meshtastic.core.demo.send

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.demo.data.DemoDataSet
import org.meshtastic.core.demo.repository.DemoPacketRepository
import org.meshtastic.core.demo.store.DemoStore
import org.meshtastic.core.model.ContactKey
import org.meshtastic.core.model.DataPacket
import org.meshtastic.core.model.MessageStatus
import org.meshtastic.core.model.NodeAddress
import org.meshtastic.core.repository.usecase.SendMessageUseCase
import org.meshtastic.proto.Routing
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

/**
 * Sending during a demo: the message is stored in the demo history and its state moves on as a radio's would, but no
 * packet is built for a radio and nothing leaves the phone.
 *
 * On a channel: waiting for my radio, then sent to my radio after 1.5 s, then relayed by the mesh after 3 s, and it
 * stays there, as a channel message is never acknowledged. Direct: the same steps, then acknowledged by the recipient
 * after 5 s; CHARLIE-3 plays a node out of reach, and its messages fail after 8 s with "no node confirmed receipt".
 */
class DemoSendMessageUseCase(
    private val store: DemoStore,
    private val packets: DemoPacketRepository,
    private val scope: CoroutineScope,
) : SendMessageUseCase {

    override suspend operator fun invoke(text: String, contactKey: String, replyId: Int?): Int {
        val key = ContactKey(contactKey)
        if (key.isRetired) return 0
        val channel = key.channelOrNull
        val destination = key.addressString
        val isDirect = channel == null || channel == NodeAddress.PKC_CHANNEL_INDEX
        val packetId = Random.nextInt(1, Int.MAX_VALUE)
        val packet =
            DataPacket(destination, channel ?: 0, text, replyId).apply {
                from = store.myId.value ?: NodeAddress.ID_LOCAL
                id = packetId
                time = nowMillis
                status = MessageStatus.QUEUED
            }
        val stored =
            packets.savePacket(
                myNodeNum = store.myNodeInfo.value?.myNodeNum ?: 0,
                contactKey = contactKey,
                packet = packet,
                receivedTime = packet.time,
            )
        val session = store.session
        scope.launch { simulate(stored.uuid, session, isDirect, destination) }
        return packetId
    }

    private suspend fun simulate(uuid: Long, session: Long, isDirect: Boolean, destination: String) {
        val steps = buildList {
            add(Step(HANDED_TO_RADIO_AT, MessageStatus.ENROUTE))
            add(Step(RELAYED_AT, MessageStatus.DELIVERED))
            if (isDirect && destination == DemoDataSet.id(DemoDataSet.CHARLIE3_NUM)) {
                add(Step(FAILED_AT, MessageStatus.ERROR, Routing.Error.MAX_RETRANSMIT.value))
            } else if (isDirect) {
                add(Step(ACKNOWLEDGED_AT, MessageStatus.RECEIVED))
            }
        }
        var elapsed = 0L
        for (step in steps) {
            delay((step.atMillis - elapsed).milliseconds)
            elapsed = step.atMillis
            if (store.session != session) break
            packets.updateStatus(uuid, step.status, step.routingError)
        }
    }

    private class Step(val atMillis: Long, val status: MessageStatus, val routingError: Int = 0)

    companion object {
        const val HANDED_TO_RADIO_AT = 1_500L
        const val RELAYED_AT = 3_000L
        const val ACKNOWLEDGED_AT = 5_000L
        const val FAILED_AT = 8_000L
    }
}
