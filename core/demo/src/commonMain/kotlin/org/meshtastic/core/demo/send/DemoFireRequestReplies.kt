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
import org.meshtastic.core.model.firerequest.FireRequestAck
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
            receive(
                FireRequestReply(part.number, ReplyKind.ALLOCATION, gdhNow()).text(),
                contactKey,
                destination,
                channel,
            )
            delay(END_AFTER_ALLOCATION.milliseconds)
            if (store.session != session) return@launch
            receive(FireRequestReply(part.number, ReplyKind.END, gdhNow()).text(), contactKey, destination, channel)
        }
    }

    /**
     * [text] was just broadcast on a channel: once its last part is out, ALPHA-1 then BRAVO-2 send their « Accusé DT »
     * in direct messages, [ACK_AFTER_BROADCAST] then twice that later; CHARLIE-3, out of reach, never does.
     */
    fun onBroadcast(text: String) {
        val part = FireRequestPart.parse(text)?.takeIf { it.k == it.count } ?: return
        val session = store.session
        scope.launch {
            listOf(
                DemoDataSet.ALPHA1_NUM to DemoDataSet.ALPHA1_DIRECT_KEY,
                DemoDataSet.BRAVO2_NUM to DemoDataSet.BRAVO2_DIRECT_KEY,
            )
                .forEach { (num, key) ->
                    delay(ACK_AFTER_BROADCAST.milliseconds)
                    if (store.session != session) return@launch
                    val ack = FireRequestAck(part.number, gdhNow()).text()
                    receive(ack, key, DemoDataSet.id(num), NodeAddress.PKC_CHANNEL_INDEX)
                }
        }
    }

    private suspend fun receive(text: String, contactKey: String, from: String, channel: Int) {
        val packet =
            DataPacket(to = store.myId.value ?: NodeAddress.ID_LOCAL, channel = channel, text = text).apply {
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
        const val ACK_AFTER_BROADCAST = 10_000L
        private const val MILLIS_PER_SECOND = 1_000L
    }
}
