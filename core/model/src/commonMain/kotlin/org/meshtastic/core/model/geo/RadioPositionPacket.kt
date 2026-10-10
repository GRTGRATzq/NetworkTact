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

import okio.ByteString.Companion.toByteString
import org.meshtastic.core.model.DataPacket
import org.meshtastic.core.model.NodeAddress
import org.meshtastic.proto.PortNum
import org.meshtastic.proto.Position

/**
 * The one packet "Diffuser ma position maintenant" sends: a standard Meshtastic position (`POSITION_APP`, no new packet
 * type) broadcast on the primary channel, through the app's existing send function, as waypoints are.
 *
 * It carries the radio's last position as the radio gave it, with the GPS fix time (`timestamp`) unchanged and copied
 * into `time`, so that a receiver reads the age of the fix and never the time of the broadcast. No acknowledgement is
 * asked: a broadcast has no recipient to confirm it, and asking would only make the radio repeat it. The firmware may
 * coarsen the position to the channel's precision setting.
 */
object RadioPositionPacket {
    const val PRIMARY_CHANNEL = 0

    fun of(radioPosition: Position): DataPacket = DataPacket(
        to = NodeAddress.ID_BROADCAST,
        bytes = Position.ADAPTER.encode(payload(radioPosition)).toByteString(),
        dataType = PortNum.POSITION_APP.value,
        channel = PRIMARY_CHANNEL,
        wantAck = false,
    )

    /** [radioPosition] with `time` set to its fix time. */
    fun payload(radioPosition: Position): Position =
        radioPosition.newBuilder().also { wb -> wb.time = radioPosition.timestamp }.build()
}
