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
package org.meshtastic.core.model.identity

import okio.ByteString.Companion.toByteString
import org.meshtastic.core.model.DataPacket
import org.meshtastic.core.model.NodeAddress
import org.meshtastic.proto.PortNum
import org.meshtastic.proto.User

/** Why "Diffuser mon nom" is refused. */
enum class NameBroadcastRefusal {
    /** No radio connected to send through. */
    NOT_CONNECTED,

    /** My radio's own node, or its name, has not been received yet. */
    NO_LOCAL_NODE,
}

/** Whether my radio's name can be broadcast now. */
sealed interface NameBroadcastCheck {
    data class Refused(val reason: NameBroadcastRefusal) : NameBroadcastCheck

    /** A broadcast was made less than [NameBroadcast.COOLDOWN_SECONDS] ago. */
    data class CoolingDown(val remainingSeconds: Long) : NameBroadcastCheck

    /** [longName] and [shortName], as my radio reports them, can be broadcast once the user confirms. */
    data class Ready(val longName: String, val shortName: String) : NameBroadcastCheck
}

/**
 * "Diffuser mon nom": one broadcast of my radio's `User` (long name, short name, public key...) to the whole network,
 * so that the other stations show a new name without waiting for the firmware's own NodeInfo broadcast.
 *
 * After a name change the firmware does broadcast the new `User`, but only if it has not sent one recently (5 minutes
 * in 2.7, at least 30 minutes in 2.8, a delay that 2.8 keeps across the reboot that follows the change): the other
 * stations then keep the old name. A `NODEINFO_APP` packet sent by the phone is not held back by that delay: the
 * firmware sends it as it does any packet from the phone, with the radio's node number as sender, and every node that
 * receives it records the new name, as for the radio's own broadcast.
 *
 * The packet is a standard Meshtastic NodeInfo (no new packet type), broadcast on the primary channel, with neither
 * `want_response` (every node would answer with its own NodeInfo) nor an acknowledgement (a broadcast has no recipient
 * to confirm it). Its payload is the `User` my radio reports, public key included: a node that already holds the
 * radio's key drops a NodeInfo whose key differs.
 *
 * After a broadcast, another one waits [COOLDOWN_SECONDS], as "Diffuser ma position maintenant" does.
 */
object NameBroadcast {
    const val COOLDOWN_SECONDS = 30L

    /** How long to wait, after a name change, for my radio to report the new name before giving up. */
    const val APPLY_TIMEOUT_SECONDS = 180L

    const val PRIMARY_CHANNEL = 0

    /**
     * @param ownUser my radio's `User` as it reports it, null when unknown.
     * @param lastBroadcastEpochSeconds when the last name broadcast was made, null if none.
     */
    fun check(
        ownUser: User?,
        connected: Boolean,
        nowEpochSeconds: Long,
        lastBroadcastEpochSeconds: Long?,
    ): NameBroadcastCheck {
        val remaining = lastBroadcastEpochSeconds?.let { cooldownRemaining(it, nowEpochSeconds) } ?: 0L
        return when {
            !connected -> NameBroadcastCheck.Refused(NameBroadcastRefusal.NOT_CONNECTED)
            ownUser == null || ownUser.long_name.isBlank() ->
                NameBroadcastCheck.Refused(NameBroadcastRefusal.NO_LOCAL_NODE)
            remaining > 0 -> NameBroadcastCheck.CoolingDown(remaining)
            else -> NameBroadcastCheck.Ready(ownUser.long_name, ownUser.short_name)
        }
    }

    /** Seconds left before another broadcast, 0 when allowed. A clock set back counts as no time elapsed. */
    fun cooldownRemaining(lastBroadcastEpochSeconds: Long, nowEpochSeconds: Long): Long {
        val elapsed = (nowEpochSeconds - lastBroadcastEpochSeconds).coerceAtLeast(0L)
        return (COOLDOWN_SECONDS - elapsed).coerceAtLeast(0L)
    }

    /** True when writing [sent] over [before] changes a name the other stations show. */
    fun namesChanged(before: User, sent: User): Boolean =
        before.long_name != sent.long_name || before.short_name != sent.short_name

    /**
     * True when my radio reports the names written in [expected]: it has applied the new `User`, so a broadcast now
     * carries the new names and not the old ones.
     */
    fun isApplied(reported: User?, expected: User): Boolean =
        reported != null && reported.long_name == expected.long_name && reported.short_name == expected.short_name

    /** The one packet a name broadcast sends: [user] as my radio reports it, to every node, on the primary channel. */
    fun packet(user: User): DataPacket = DataPacket(
        to = NodeAddress.ID_BROADCAST,
        bytes = User.ADAPTER.encode(user).toByteString(),
        dataType = PortNum.NODEINFO_APP.value,
        channel = PRIMARY_CHANNEL,
        wantAck = false,
    )
}
