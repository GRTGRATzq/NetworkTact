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

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import org.meshtastic.core.demo.data.DemoDataSet
import org.meshtastic.core.demo.repository.DemoPacketRepository
import org.meshtastic.core.demo.store.DemoStore
import org.meshtastic.core.model.MessageStatus
import org.meshtastic.proto.Routing
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class DemoSendMessageUseCaseTest {

    private val store = DemoStore().apply { load(DemoDataSet.create(NOW, TimeZone.UTC), commandPostMode = false) }
    private val packets = DemoPacketRepository(store)

    private fun TestScope.send() = DemoSendMessageUseCase(store, packets, backgroundScope)

    private fun statusOf(packetId: Int): MessageStatus? =
        store.packets.value.single { it.packet.id == packetId }.packet.status

    private fun TestScope.at(millis: Long) {
        advanceTimeBy(millis - currentTime)
        runCurrent()
    }

    @Test
    fun aChannelMessageIsRelayedAndNeverAcknowledged() = runTest {
        val id = send()("Point de situation", DemoDataSet.GENERAL_KEY)
        assertEquals(MessageStatus.QUEUED, statusOf(id))

        at(DemoSendMessageUseCase.HANDED_TO_RADIO_AT)
        assertEquals(MessageStatus.ENROUTE, statusOf(id))

        at(DemoSendMessageUseCase.RELAYED_AT)
        assertEquals(MessageStatus.DELIVERED, statusOf(id))

        at(DemoSendMessageUseCase.FAILED_AT * 2)
        assertEquals(MessageStatus.DELIVERED, statusOf(id))
    }

    @Test
    fun aDirectMessageToBravo2IsAcknowledged() = runTest {
        val id = send()("Confirmez", DemoDataSet.BRAVO2_DIRECT_KEY)

        at(DemoSendMessageUseCase.RELAYED_AT)
        assertEquals(MessageStatus.DELIVERED, statusOf(id))

        at(DemoSendMessageUseCase.ACKNOWLEDGED_AT)
        assertEquals(MessageStatus.RECEIVED, statusOf(id))
    }

    @Test
    fun aDirectMessageToCharlie3Fails() = runTest {
        val id = send()("Rejoignez", DemoDataSet.CHARLIE3_DIRECT_KEY)

        at(DemoSendMessageUseCase.FAILED_AT)
        assertEquals(MessageStatus.ERROR, statusOf(id))
        assertEquals(Routing.Error.MAX_RETRANSMIT.value, store.packets.value.single { it.packet.id == id }.routingError)
    }

    @Test
    fun theMessageIsFromPc0OnItsConversation() = runTest {
        val id = send()("Départ", DemoDataSet.TEAM_ALPHA_KEY)
        val stored = store.packets.value.single { it.packet.id == id }
        assertEquals(DemoDataSet.TEAM_ALPHA_KEY, stored.contactKey)
        assertEquals(DemoDataSet.id(DemoDataSet.PC0_NUM), stored.packet.from)
        assertEquals(1, stored.packet.channel)
    }

    @Test
    fun aSimulationNeverReachesTheNextDemoSession() = runTest {
        val id = send()("Avant la sortie", DemoDataSet.GENERAL_KEY)
        store.clear()
        store.load(DemoDataSet.create(NOW, TimeZone.UTC), commandPostMode = false)
        val before = store.packets.value

        at(DemoSendMessageUseCase.FAILED_AT)
        assertEquals(before, store.packets.value)
        assertNull(store.packets.value.firstOrNull { it.packet.id == id })
    }

    private companion object {
        const val NOW = 1_790_000_000_000L
    }
}
