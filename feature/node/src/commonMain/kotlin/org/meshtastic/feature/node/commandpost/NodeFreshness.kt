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
package org.meshtastic.feature.node.commandpost

import org.meshtastic.core.model.Node
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** How long ago a node was last heard (`lastHeard`, the time its last packet reached us). */
sealed interface ContactState {
    data object NeverHeard : ContactState

    data class SeenRecently(val age: Duration) : ContactState

    data class NotHeardSince(val age: Duration) : ContactState

    /** `lastHeard` lies further in the future than the clock tolerance allows. */
    data class InconsistentTimestamp(val ahead: Duration) : ContactState
}

/**
 * What can honestly be said about a node's last position.
 *
 * Only [Fresh] is ever presented as current. It requires the sender's GPS fix time (`Position.timestamp`, set by the
 * radio and never rewritten by the app). Without it the app only holds `Position.time`, which it fills with the
 * reception time when the sender left it empty: an age from that field is a lower bound, never proof of freshness.
 */
sealed interface PositionState {
    data object NoPosition : PositionState

    /** Fix time known and within [FreshnessThresholds.stalePosition]. */
    data class Fresh(val age: Duration) : PositionState

    /** Fix time known and older than [FreshnessThresholds.stalePosition]. */
    data class Stale(val age: Duration) : PositionState

    /** Fix time unknown, but even the lower bound is past [FreshnessThresholds.stalePosition]: certainly old. */
    data class StaleAtLeast(val minAge: Duration) : PositionState

    /** Fix time unknown and the lower bound is under the threshold: the position may be much older. */
    data class ReceivedFixTimeUnknown(val minAge: Duration) : PositionState

    /** Neither a fix time nor any other time: the age is unknown and is not estimated. */
    data object NoFixTime : PositionState

    /** The time lies further in the future than the clock tolerance allows. */
    data class InconsistentTimestamp(val ahead: Duration) : PositionState
}

object NodeFreshness {

    fun contact(
        lastHeardSeconds: Int,
        nowSeconds: Long,
        thresholds: FreshnessThresholds = FreshnessThresholds.Default,
    ): ContactState {
        val heard = lastHeardSeconds.toEpochSeconds() ?: return ContactState.NeverHeard
        val age = (nowSeconds - heard).seconds
        return when {
            -age > thresholds.clockTolerance -> ContactState.InconsistentTimestamp(-age)
            age <= thresholds.recentContact -> ContactState.SeenRecently(age.coerceAtLeast(Duration.ZERO))
            else -> ContactState.NotHeardSince(age)
        }
    }

    /**
     * @param hasPosition whether the node has a valid position at all.
     * @param fixTimestampSeconds `Position.timestamp`: the sender's GPS fix time, 0 when the sender omits it.
     * @param positionTimeSeconds `Position.time` as stored by the app, 0 when absent.
     */
    fun position(
        hasPosition: Boolean,
        fixTimestampSeconds: Int,
        positionTimeSeconds: Int,
        nowSeconds: Long,
        thresholds: FreshnessThresholds = FreshnessThresholds.Default,
    ): PositionState {
        if (!hasPosition) return PositionState.NoPosition

        val fix = fixTimestampSeconds.toEpochSeconds()
        if (fix != null) {
            val age = (nowSeconds - fix).seconds
            return when {
                -age > thresholds.clockTolerance -> PositionState.InconsistentTimestamp(-age)
                age > thresholds.stalePosition -> PositionState.Stale(age)
                else -> PositionState.Fresh(age.coerceAtLeast(Duration.ZERO))
            }
        }

        val time = positionTimeSeconds.toEpochSeconds() ?: return PositionState.NoFixTime
        val minAge = (nowSeconds - time).seconds
        return when {
            -minAge > thresholds.clockTolerance -> PositionState.InconsistentTimestamp(-minAge)
            minAge > thresholds.stalePosition -> PositionState.StaleAtLeast(minAge)
            else -> PositionState.ReceivedFixTimeUnknown(minAge.coerceAtLeast(Duration.ZERO))
        }
    }

    fun contact(node: Node, nowSeconds: Long, thresholds: FreshnessThresholds = FreshnessThresholds.Default) =
        contact(node.lastHeard, nowSeconds, thresholds)

    fun position(node: Node, nowSeconds: Long, thresholds: FreshnessThresholds = FreshnessThresholds.Default) =
        position(
            hasPosition = node.validPosition != null,
            fixTimestampSeconds = node.position.timestamp,
            positionTimeSeconds = node.position.time,
            nowSeconds = nowSeconds,
            thresholds = thresholds,
        )

    // Protobuf fixed32 fields arrive as signed Ints; read them unsigned so dates past 2038 stay positive.
    private fun Int.toEpochSeconds(): Long? = toUInt().toLong().takeIf { it > 0L }
}
