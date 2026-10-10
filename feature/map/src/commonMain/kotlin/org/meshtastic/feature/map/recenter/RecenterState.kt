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

import org.meshtastic.core.model.Node
import org.meshtastic.core.model.freshness.NodeFreshness
import org.meshtastic.core.model.freshness.PositionState
import org.meshtastic.core.model.geo.BroadcastCheck
import org.meshtastic.core.model.geo.LatLon
import org.meshtastic.core.model.geo.PositionComparison
import org.meshtastic.core.model.geo.RadioFixQuality
import org.meshtastic.core.repository.PhoneFix
import org.meshtastic.core.repository.RecenterTarget

/**
 * One of this user's two positions as the "Recaler" card shows it: where, how old in the command post view's words
 * ([NodeFreshness]), and the fix time when known.
 */
data class RecenterSide(val point: LatLon, val state: PositionState, val fixEpochSeconds: Long?)

/** A broadcast made from the card: when it was handed to the radio, and when the position it carried was taken. */
data class SentBroadcast(val sentEpochSeconds: Long, val fixEpochSeconds: Long)

/**
 * What the "Recaler" card shows: the radio's position (from the radio's GPS, the one the network receives) beside the
 * phone's (from the phone's GPS, the blue dot), each with its fix time and accuracy when known, the gap between them,
 * and whether the radio's position can be broadcast now.
 */
data class RecenterState(
    val radio: RecenterSide?,
    val radioQuality: RadioFixQuality?,
    val phone: RecenterSide?,
    val phoneAccuracyMeters: Float?,
    val target: RecenterTarget,
    val broadcast: BroadcastCheck,
    val lastSent: SentBroadcast?,
    val sendFailed: Boolean,
) {
    /** Distance between the two positions in metres, when both are known. */
    val gapMeters: Int?
        get() = if (radio != null && phone != null) PositionComparison.gapMeters(radio.point, phone.point) else null

    /** Where the map centres: the chosen position, or the other one when the chosen one is missing. */
    val centre: LatLon?
        get() =
            when (target) {
                RecenterTarget.RADIO -> radio?.point ?: phone?.point
                RecenterTarget.PHONE -> phone?.point ?: radio?.point
            }

    /** Whether [centre] is the other position, the chosen one being missing: the card says so. */
    val centredOnOther: Boolean
        get() =
            when (target) {
                RecenterTarget.RADIO -> radio == null && phone != null
                RecenterTarget.PHONE -> phone == null && radio != null
            }

    companion object {
        /** The radio's side, from this phone's own node; null when the radio holds no position. */
        fun radioSide(node: Node?, nowEpochSeconds: Long): RecenterSide? {
            val position = node?.validPosition ?: return null
            val fix = position.timestamp.toUInt().toLong().takeIf { it > 0L }
            return RecenterSide(
                point = LatLon(node.latitude, node.longitude),
                state = NodeFreshness.position(node, nowEpochSeconds),
                fixEpochSeconds = fix,
            )
        }

        /** The radio's own account of its fix: satellites, HDOP and PDOP, coarsening by the channel. */
        fun radioQuality(node: Node?): RadioFixQuality? =
            node?.validPosition?.let { RadioFixQuality.of(it.sats_in_view, it.HDOP, it.PDOP, it.precision_bits) }

        /** The phone's side, with the same age rule as a node whose sender gave its fix time. */
        fun phoneSide(fix: PhoneFix?, nowEpochSeconds: Long): RecenterSide? =
            fix?.takeIf { it.latitude in -LatLon.MAX_LATITUDE..LatLon.MAX_LATITUDE }
                ?.takeIf { it.longitude in -LatLon.MAX_LONGITUDE..LatLon.MAX_LONGITUDE }
                ?.let {
                    RecenterSide(
                        point = LatLon(it.latitude, it.longitude),
                        state =
                        NodeFreshness.position(
                            hasPosition = true,
                            fixTimestampSeconds = it.fixEpochSeconds.toInt(),
                            positionTimeSeconds = 0,
                            nowSeconds = nowEpochSeconds,
                        ),
                        fixEpochSeconds = it.fixEpochSeconds.takeIf { seconds -> seconds > 0L },
                    )
                }
    }
}
