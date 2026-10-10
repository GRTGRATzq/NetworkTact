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
package org.meshtastic.feature.map.recenter

import kotlinx.coroutines.test.runTest
import org.meshtastic.core.model.ConnectionState
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.NodeAddress
import org.meshtastic.core.model.geo.BroadcastCheck
import org.meshtastic.core.model.geo.BroadcastRefusal
import org.meshtastic.core.model.geo.LatLon
import org.meshtastic.core.repository.PhoneFix
import org.meshtastic.core.repository.PhonePositionSource
import org.meshtastic.core.repository.RecenterTarget
import org.meshtastic.core.testing.FakeDemoMode
import org.meshtastic.core.testing.FakeMapPrefs
import org.meshtastic.core.testing.FakeNodeRepository
import org.meshtastic.core.testing.FakeRadioController
import org.meshtastic.proto.PortNum
import org.meshtastic.proto.Position
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RecenterViewModelTest {
    private var now = 1_760_000_000L
    private val nodes = FakeNodeRepository()
    private val radio = FakeRadioController().apply { setConnectionState(ConnectionState.Connected) }
    private val prefs = FakeMapPrefs()
    private val demo = FakeDemoMode()
    private var phoneFix: PhoneFix? = PhoneFix(48.8586, 2.3472, fixEpochSeconds = now - 20, accuracyMeters = 8f)

    private val viewModel =
        RecenterViewModel(
            nodeRepository = nodes,
            phonePositionSource = PhonePositionSource { phoneFix },
            messagingController = radio,
            connectionStateProvider = radio,
            mapPrefs = prefs,
            demoMode = demo,
        )
            .also { it.clock = { now } }

    private fun ownRadio(fixAgeSeconds: Long?) {
        val position =
            Position.Builder()
                .also { wb ->
                    wb.latitude_i = 488_584_000
                    wb.longitude_i = 23_470_000
                    wb.timestamp = fixAgeSeconds?.let { (now - it).toInt() } ?: 0
                    wb.sats_in_view = 9
                    wb.HDOP = 120
                }
                .build()
        nodes.setOurNode(Node(num = 1, position = position))
    }

    private fun assertNear(expected: LatLon, actual: LatLon?) {
        val point = assertNotNull(actual)
        assertTrue(abs(expected.latitude - point.latitude) < 1e-9, "$point")
        assertTrue(abs(expected.longitude - point.longitude) < 1e-9, "$point")
    }

    private fun positionPackets() = radio.sentPackets.filter { it.dataType == PortNum.POSITION_APP.value }

    @Test
    fun cardShowsBothPositionsTheirTimesAndTheGap() = runTest {
        ownRadio(fixAgeSeconds = 60)
        val state = viewModel.refresh()

        val radioSide = assertNotNull(state.radio)
        assertEquals(now - 60, radioSide.fixEpochSeconds)
        assertEquals(9, state.radioQuality?.satellites)
        assertEquals(1.2, state.radioQuality?.hdop)
        val phoneSide = assertNotNull(state.phone)
        assertEquals(now - 20, phoneSide.fixEpochSeconds)
        assertEquals(8f, state.phoneAccuracyMeters)
        assertEquals(27, state.gapMeters)
    }

    @Test
    fun recenterGoesToTheChosenPositionAndRemembersTheChoice() = runTest {
        ownRadio(fixAgeSeconds = 60)
        assertNear(LatLon(48.8584, 2.347), viewModel.refresh().centre)

        viewModel.setTarget(RecenterTarget.PHONE)

        assertNear(LatLon(48.8586, 2.3472), viewModel.state.value?.centre)
        assertEquals(RecenterTarget.PHONE, prefs.recenterTarget.value)
    }

    @Test
    fun withoutPhoneFixTheRadioIsUsedAndTheCardSaysSo() = runTest {
        ownRadio(fixAgeSeconds = 60)
        phoneFix = null
        viewModel.setTarget(RecenterTarget.PHONE)
        val state = viewModel.refresh()
        assertNear(LatLon(48.8584, 2.347), state.centre)
        assertTrue(state.centredOnOther)
    }

    @Test
    fun broadcastSendsOnePositionPacketWithTheFixTime() = runTest {
        ownRadio(fixAgeSeconds = 60)
        viewModel.refresh()

        val sent = assertNotNull(viewModel.broadcast())

        assertEquals(SentBroadcast(sentEpochSeconds = now, fixEpochSeconds = now - 60), sent)
        val packet = positionPackets().single()
        assertEquals(NodeAddress.ID_BROADCAST, packet.to)
        assertEquals(0, packet.channel)
        val payload = Position.ADAPTER.decode(assertNotNull(packet.bytes))
        assertEquals((now - 60).toInt(), payload.time)
        assertEquals(sent, viewModel.state.value?.lastSent)
    }

    @Test
    fun unknownFixTimeSendsNothing() = runTest {
        ownRadio(fixAgeSeconds = null)
        assertEquals(BroadcastCheck.Refused(BroadcastRefusal.FIX_TIME_UNKNOWN), viewModel.refresh().broadcast)
        assertNull(viewModel.broadcast())
        assertTrue(positionPackets().isEmpty())
    }

    @Test
    fun oldPositionNeedsTheConfirmationThenIsSentWithItsOldTime() = runTest {
        ownRadio(fixAgeSeconds = 25 * 60)
        val check = assertIs<BroadcastCheck.Ready>(viewModel.refresh().broadcast)
        assertTrue(check.old)

        // The card has shown the age and the user confirmed.
        assertNotNull(viewModel.broadcast())
        val payload = Position.ADAPTER.decode(assertNotNull(positionPackets().single().bytes))
        assertEquals((now - 25 * 60).toInt(), payload.time)
    }

    @Test
    fun disconnectedRadioSendsNothing() = runTest {
        ownRadio(fixAgeSeconds = 60)
        radio.setConnectionState(ConnectionState.Disconnected)
        assertEquals(BroadcastCheck.Refused(BroadcastRefusal.NOT_CONNECTED), viewModel.refresh().broadcast)
        assertNull(viewModel.broadcast())
        assertTrue(positionPackets().isEmpty())
    }

    @Test
    fun withoutRadioPositionThePhoneIsNeverSent() = runTest {
        nodes.setOurNode(Node(num = 1))
        assertEquals(BroadcastCheck.Refused(BroadcastRefusal.NO_RADIO_POSITION), viewModel.refresh().broadcast)
        assertNull(viewModel.broadcast())
        assertTrue(radio.sentPackets.isEmpty())
    }

    @Test
    fun demoModeSendsNothing() = runTest {
        ownRadio(fixAgeSeconds = 60)
        demo.activate()
        assertEquals(BroadcastCheck.Refused(BroadcastRefusal.DEMO), viewModel.refresh().broadcast)
        assertNull(viewModel.broadcast())
        assertTrue(radio.sentPackets.isEmpty())
    }

    @Test
    fun aSecondBroadcastWaitsThirtySeconds() = runTest {
        ownRadio(fixAgeSeconds = 60)
        assertNotNull(viewModel.broadcast())

        now += 10
        assertEquals(BroadcastCheck.CoolingDown(20), viewModel.refresh().broadcast)
        assertNull(viewModel.broadcast())
        assertEquals(1, positionPackets().size)

        now += 20
        assertIs<BroadcastCheck.Ready>(viewModel.refresh().broadcast)
        assertNotNull(viewModel.broadcast())
        assertEquals(2, positionPackets().size)
        assertFalse(viewModel.state.value?.sendFailed ?: true)
    }

    @Test
    fun aFailedSendIsReportedAndDoesNotStartTheWait() = runTest {
        ownRadio(fixAgeSeconds = 60)
        radio.throwOnSend = true

        assertNull(viewModel.broadcast())

        val state = assertNotNull(viewModel.state.value)
        assertTrue(state.sendFailed)
        assertNull(state.lastSent)
        assertIs<BroadcastCheck.Ready>(state.broadcast)
    }
}
