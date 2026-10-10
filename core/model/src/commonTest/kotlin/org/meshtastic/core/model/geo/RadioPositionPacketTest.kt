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
package org.meshtastic.core.model.geo

import org.meshtastic.core.model.NodeAddress
import org.meshtastic.proto.PortNum
import org.meshtastic.proto.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class RadioPositionPacketTest {
    private val radioPosition =
        Position.Builder()
            .also { wb ->
                wb.latitude_i = 488_584_000
                wb.longitude_i = 23_470_000
                wb.altitude = 35
                wb.timestamp = 1_760_000_000
                wb.time = 1_760_000_300
                wb.sats_in_view = 9
                wb.HDOP = 120
            }
            .build()

    @Test
    fun packetIsAStandardPositionBroadcastOnThePrimaryChannel() {
        val packet = RadioPositionPacket.of(radioPosition)
        assertEquals(NodeAddress.ID_BROADCAST, packet.to)
        assertEquals(PortNum.POSITION_APP.value, packet.dataType)
        assertEquals(0, packet.channel)
        assertFalse(packet.wantAck)
    }

    @Test
    fun payloadIsTheRadioPositionWithItsFixTime() {
        val bytes = assertNotNull(RadioPositionPacket.of(radioPosition).bytes)
        val sent = Position.ADAPTER.decode(bytes)
        assertEquals(488_584_000, sent.latitude_i)
        assertEquals(23_470_000, sent.longitude_i)
        assertEquals(35, sent.altitude)
        assertEquals(9, sent.sats_in_view)
        assertEquals(120, sent.HDOP)
        assertEquals(1_760_000_000, sent.timestamp)
        // The time of the fix, never the time of the broadcast.
        assertEquals(1_760_000_000, sent.time)
    }
}
