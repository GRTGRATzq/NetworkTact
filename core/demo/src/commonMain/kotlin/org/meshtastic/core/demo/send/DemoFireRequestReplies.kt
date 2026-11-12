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
import org.meshtastic.core.common.util.systemTimeZone
import org.meshtastic.core.demo.data.DemoDataSet
import org.meshtastic.core.demo.repository.DemoPacketRepository
import org.meshtastic.core.demo.store.DemoStore
import org.meshtastic.core.model.DataPacket
import org.meshtastic.core.model.MessageStatus
import org.meshtastic.core.model.NodeAddress
import org.meshtastic.core.model.firerequest.FireRequestPart
import org.meshtastic.core.model.firerequest.FireRequestReply
import org.meshtastic.core.model.firerequest.Gdh
import org.meshtastic.core.model.firerequest.ReplyKind
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

/**
 * The recipient's answer to a fire request sent during a demo: once the last part has been acknowledged, the recipient
 * sends back `[DT <n°>] GDH attribution …` [ALLOCATION_AFTER_ACK] later, then `GDH fin` [END_AFTER_ALLOCATION] after
 * that, each dated at the moment it arrives, in the zone the form uses. CHARLIE-3, out of reach, never acknowledges and
 * never answers. The answers are stored in the demo history as received; nothing leaves the phone, and a demo that ends
 * stops them.
 */
class DemoFireRequestReplies(
    private val store: DemoStore,
    private val packets: DemoPacketRepository,
    private val scope: CoroutineScope,
) {

    /** [text] was just sent in a direct message to [destination], in conversation [contactKey], on [channel]. */
    fun onSent(text: String, contactKey: String, destination: String, channel: Int) {
        val outOfReach =
            destination == NodeAddress.ID_BROADCAST || destination == DemoDataSet.id(DemoDataSet.CHARLIE3_NUM)
        val part = FireRequestPart.parse(text)?.takeIf { it.k == it.count && !outOfReach } ?: return
        val session = store.session
        scope.launch {
            delay((DemoSendMessageUseCase.ACKNOWLEDGED_AT + ALLOCATION_AFTER_ACK).milliseconds)
            if (store.session != session) return@launch
            receive(FireRequestReply(part.number, ReplyKind.ALLOCATION, gdhNow()), contactKey, destination, channel)
            delay(END_AFTER_ALLOCATION.milliseconds)
            if (store.session != session) return@launch
            receive(FireRequestReply(part.number, ReplyKind.END, gdhNow()), contactKey, destination, channel)
        }
    }

    private suspend fun receive(reply: FireRequestReply, contactKey: String, from: String, channel: Int) {
        val packet =
            DataPacket(to = store.myId.value ?: NodeAddress.ID_LOCAL, channel = channel, text = reply.text()).apply {
                this.from = from
                id = Random.nextInt(1, Int.MAX_VALUE)
                time = nowMillis
                status = MessageStatus.RECEIVED
            }
        packets.savePacket(
            myNodeNum = store.myNodeInfo.value?.myNodeNum ?: 0,
            contactKey = contactKey,
            packet = packet,
            receivedTime = packet.time,
            read = false,
        )
    }

    private fun gdhNow(): String =
        Gdh.of(nowMillis / MILLIS_PER_SECOND, store.fireRequestZone.value, systemTimeZone).format()

    companion object {
        const val ALLOCATION_AFTER_ACK = 8_000L
        const val END_AFTER_ALLOCATION = 20_000L
        private const val MILLIS_PER_SECOND = 1_000L
    }
}
