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

import org.meshtastic.core.model.Node
import org.meshtastic.core.model.freshness.FreshnessThresholds
import org.meshtastic.core.model.freshness.NodeFreshness
import org.meshtastic.core.model.freshness.PositionState
import kotlin.time.Duration

/** Why "Diffuser ma position maintenant" is refused. */
enum class BroadcastRefusal {
    /** Demo mode: nothing is ever sent. */
    DEMO,

    /** No radio connected to send through. */
    NOT_CONNECTED,

    /** The radio holds no position. The phone's is never broadcast in its place. */
    NO_RADIO_POSITION,

    /** The radio did not give the time of its GPS fix: the position would pass for a current one. */
    FIX_TIME_UNKNOWN,

    /** The fix time lies in the future: one of the clocks is wrong. */
    FIX_TIME_INCONSISTENT,
}

/** Whether the radio's position can be broadcast now, and if so what the user is told before confirming. */
sealed interface BroadcastCheck {
    data class Refused(val reason: BroadcastRefusal) : BroadcastCheck

    /** A broadcast was made less than [RadioPositionBroadcast.COOLDOWN_SECONDS] ago. */
    data class CoolingDown(val remainingSeconds: Long) : BroadcastCheck

    /**
     * The radio's position, taken at [fixEpochSeconds], can be broadcast once the user confirms. [old] past the command
     * post threshold: the confirmation then says how old it is, [age].
     */
    data class Ready(val fixEpochSeconds: Long, val age: Duration, val old: Boolean) : BroadcastCheck
}

/**
 * "Diffuser ma position maintenant": one manual broadcast of the radio's last position, with the time of its GPS fix
 * unchanged, never automatic nor periodic. Only the radio's position is ever sent, never the phone's.
 *
 * The rule follows the command post view ([NodeFreshness]): a position whose fix time is unknown or inconsistent is
 * refused, since receivers would take it for a current one; past the threshold it is sent only after a confirmation
 * that says how old it is. After a broadcast the button waits [COOLDOWN_SECONDS], so a repeated press does not load the
 * channel.
 */
object RadioPositionBroadcast {
    const val COOLDOWN_SECONDS = 30L

    /**
     * @param ownNode this phone's radio, null when unknown.
     * @param lastBroadcastEpochSeconds when the last broadcast was made from this screen, null if none.
     */
    fun check(
        ownNode: Node?,
        connected: Boolean,
        demo: Boolean,
        nowEpochSeconds: Long,
        lastBroadcastEpochSeconds: Long?,
        thresholds: FreshnessThresholds = FreshnessThresholds.Default,
    ): BroadcastCheck = when {
        demo -> BroadcastCheck.Refused(BroadcastRefusal.DEMO)

        !connected -> BroadcastCheck.Refused(BroadcastRefusal.NOT_CONNECTED)

        ownNode == null -> BroadcastCheck.Refused(BroadcastRefusal.NO_RADIO_POSITION)

        else -> {
            val remaining = lastBroadcastEpochSeconds?.let { cooldownRemaining(it, nowEpochSeconds) } ?: 0L
            positionCheck(ownNode, nowEpochSeconds, thresholds, remaining)
        }
    }

    /** Seconds left before another broadcast, 0 when allowed. A clock set back counts as no time elapsed. */
    fun cooldownRemaining(lastBroadcastEpochSeconds: Long, nowEpochSeconds: Long): Long {
        val elapsed = (nowEpochSeconds - lastBroadcastEpochSeconds).coerceAtLeast(0L)
        return (COOLDOWN_SECONDS - elapsed).coerceAtLeast(0L)
    }

    private fun positionCheck(
        node: Node,
        nowEpochSeconds: Long,
        thresholds: FreshnessThresholds,
        cooldownRemaining: Long,
    ): BroadcastCheck {
        val fix = node.position.timestamp.toLong()
        return when (val state = NodeFreshness.position(node, nowEpochSeconds, thresholds)) {
            PositionState.NoPosition -> BroadcastCheck.Refused(BroadcastRefusal.NO_RADIO_POSITION)

            is PositionState.InconsistentTimestamp -> BroadcastCheck.Refused(BroadcastRefusal.FIX_TIME_INCONSISTENT)

            is PositionState.ReceivedFixTimeUnknown,
            is PositionState.StaleAtLeast,
            PositionState.NoFixTime,
            -> BroadcastCheck.Refused(BroadcastRefusal.FIX_TIME_UNKNOWN)

            is PositionState.Fresh ->
                cooldownOr(cooldownRemaining) { BroadcastCheck.Ready(fix, state.age, old = false) }

            is PositionState.Stale -> cooldownOr(cooldownRemaining) { BroadcastCheck.Ready(fix, state.age, old = true) }
        }
    }

    private inline fun cooldownOr(remaining: Long, ready: () -> BroadcastCheck): BroadcastCheck =
        if (remaining > 0) BroadcastCheck.CoolingDown(remaining) else ready()
}
