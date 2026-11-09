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
package org.meshtastic.feature.settings.identity

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okio.ByteString.Companion.encodeUtf8
import org.meshtastic.core.common.di.asServiceScope
import org.meshtastic.core.model.ConnectionState
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.NodeAddress
import org.meshtastic.core.model.identity.NameBroadcastCheck
import org.meshtastic.core.testing.FakeNodeRepository
import org.meshtastic.core.testing.FakeRadioController
import org.meshtastic.core.testing.FakeServiceRepository
import org.meshtastic.proto.HardwareModel
import org.meshtastic.proto.PortNum
import org.meshtastic.proto.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class NameBroadcasterTest {

    private val myNum = 0x0badcafe
    private val start = 1_790_000_000L

    private val owner: User =
        User.Builder()
            .also {
                it.id = "!0badcafe"
                it.long_name = "CDS TIR"
                it.short_name = "CDST"
                it.hw_model = HardwareModel.TBEAM
                it.public_key = "0123456789abcdef0123456789abcdef".encodeUtf8()
            }
            .build()

    private fun User.withLongName(name: String): User = newBuilder().also { it.long_name = name }.build()

    private fun User.withUnmessagable(): User = newBuilder().also { it.is_unmessagable = true }.build()

    private val nodes = FakeNodeRepository()
    private val service = FakeServiceRepository()
    private val radio = FakeRadioController()

    private fun TestScope.broadcaster(): NameBroadcaster {
        nodes.setOurNode(Node(num = myNum, user = owner))
        service.setConnectionState(ConnectionState.Connected)
        val broadcaster =
            NameBroadcaster(
                nodeRepository = nodes,
                connectionStateProvider = service,
                messagingController = radio,
                scope = backgroundScope.asServiceScope(),
            )
        broadcaster.clock = { start + testScheduler.currentTime / MILLIS_PER_SECOND }
        return broadcaster
    }

    private fun sentUsers(): List<User> = radio.sentPackets.map { User.ADAPTER.decode(assertNotNull(it.bytes)) }

    @Test
    fun broadcast_sends_my_radio_user_once_as_a_nodeinfo_broadcast() = runTest {
        val broadcaster = broadcaster()

        assertTrue(broadcaster.broadcastNow())

        val packet = radio.sentPackets.single()
        assertEquals(NodeAddress.ID_BROADCAST, packet.to)
        assertEquals(PortNum.NODEINFO_APP.value, packet.dataType)
        assertEquals(0, packet.channel)
        assertFalse(packet.wantAck)
        assertEquals(listOf(owner), sentUsers())
        assertEquals(NameBroadcastEvent.Sent("CDS TIR", automatic = false), broadcaster.event.value)
    }

    @Test
    fun broadcast_then_waits_thirty_seconds() = runTest {
        val broadcaster = broadcaster()
        broadcaster.broadcastNow()

        assertEquals(NameBroadcastCheck.CoolingDown(30), broadcaster.check())
        assertFalse(broadcaster.broadcastNow())
        assertEquals(1, radio.sentPackets.size)

        advanceTimeBy(30_000)
        assertIs<NameBroadcastCheck.Ready>(broadcaster.check())
        assertTrue(broadcaster.broadcastNow())
        assertEquals(2, radio.sentPackets.size)
    }

    @Test
    fun broadcast_is_refused_while_the_radio_is_not_connected() = runTest {
        val broadcaster = broadcaster()
        service.setConnectionState(ConnectionState.Disconnected)

        assertFalse(broadcaster.broadcastNow())
        assertTrue(radio.sentPackets.isEmpty())
    }

    @Test
    fun a_failed_send_reports_it_and_does_not_start_the_wait() = runTest {
        val broadcaster = broadcaster()
        radio.throwOnSend = true

        assertFalse(broadcaster.broadcastNow())

        assertEquals(NameBroadcastEvent.Failed, broadcaster.event.value)
        assertIs<NameBroadcastCheck.Ready>(broadcaster.check())
    }

    @Test
    fun after_a_name_change_the_new_name_is_broadcast_once_the_radio_reports_it() = runTest {
        val broadcaster = broadcaster()
        val renamed = owner.withLongName("CDS TIR [Alpha]")

        broadcaster.ownerWritten(before = owner, sent = renamed)
        runCurrent()

        // The radio still reports the old name: nothing goes out, the old name above all.
        assertEquals(NameBroadcastEvent.Waiting("CDS TIR [Alpha]"), broadcaster.event.value)
        assertTrue(radio.sentPackets.isEmpty())

        // The radio restarts, then reports the new name.
        service.setConnectionState(ConnectionState.Disconnected)
        nodes.setOurNode(Node(num = myNum, user = renamed))
        runCurrent()
        assertTrue(radio.sentPackets.isEmpty())

        service.setConnectionState(ConnectionState.Connected)
        runCurrent()

        assertEquals(listOf(renamed), sentUsers())
        assertEquals(NameBroadcastEvent.Sent("CDS TIR [Alpha]", automatic = true), broadcaster.event.value)

        advanceTimeBy(600_000)
        assertEquals(1, radio.sentPackets.size)
    }

    @Test
    fun a_change_that_leaves_the_names_alone_broadcasts_nothing() = runTest {
        val broadcaster = broadcaster()

        broadcaster.ownerWritten(before = owner, sent = owner.withUnmessagable())
        advanceTimeBy(600_000)

        assertTrue(radio.sentPackets.isEmpty())
        assertEquals(null, broadcaster.event.value)
    }

    @Test
    fun nothing_is_broadcast_when_the_radio_never_reports_the_new_name() = runTest {
        val broadcaster = broadcaster()

        broadcaster.ownerWritten(before = owner, sent = owner.withLongName("CDS TIR [Alpha]"))
        advanceTimeBy(181_000)

        assertTrue(radio.sentPackets.isEmpty())
        assertEquals(NameBroadcastEvent.NotApplied("CDS TIR [Alpha]"), broadcaster.event.value)
    }

    @Test
    fun the_automatic_broadcast_waits_out_a_manual_one_just_made() = runTest {
        val broadcaster = broadcaster()
        broadcaster.broadcastNow()
        val renamed = owner.withLongName("CDS TIR [Alpha]")

        broadcaster.ownerWritten(before = owner, sent = renamed)
        nodes.setOurNode(Node(num = myNum, user = renamed))
        advanceTimeBy(10_000)
        assertEquals(1, radio.sentPackets.size)

        advanceTimeBy(21_000)
        assertEquals(listOf(owner, renamed), sentUsers())
    }

    private companion object {
        const val MILLIS_PER_SECOND = 1_000L
    }
}
