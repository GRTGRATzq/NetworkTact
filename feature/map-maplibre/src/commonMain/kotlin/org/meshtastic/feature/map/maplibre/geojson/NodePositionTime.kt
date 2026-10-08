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
package org.meshtastic.feature.map.maplibre.geojson

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.freshness.NodeFreshness
import org.meshtastic.core.model.freshness.PositionState
import kotlin.time.Instant

/**
 * What a node chip says about its position without a tap. Only the alarming states of the command post view
 * ([NodeFreshness], the one rule): an old position, or a fix time ahead of this phone's clock. Never a colour: an
 * inverted tag, as the command post view's pill, red and amber being reserved for message priorities.
 */
internal enum class ChipAlert {
    OLD,
    INCONSISTENT,
}

/** The chip tag for this state, exactly where the command post view draws its inverted pill. */
internal val PositionState.chipAlert: ChipAlert?
    get() =
        when (this) {
            is PositionState.Stale,
            is PositionState.StaleAtLeast,
            -> ChipAlert.OLD

            is PositionState.InconsistentTimestamp -> ChipAlert.INCONSISTENT

            is PositionState.Fresh,
            is PositionState.ReceivedFixTimeUnknown,
            PositionState.NoFixTime,
            PositionState.NoPosition,
            -> null
        }

/**
 * When a node's position was taken, as the map tells it on a tap.
 *
 * @property state the command post view's reading of the position ([NodeFreshness.position]).
 * @property fixEpochSeconds the sender's GPS fix time; null when it did not give one, so the time is unknown.
 * @property sameDay whether the fix is from today in the phone's time zone; otherwise the date is shown with the time.
 */
internal data class NodePositionTime(val state: PositionState, val fixEpochSeconds: Long?, val sameDay: Boolean)

internal fun Node.positionTime(nowSeconds: Long, timeZone: TimeZone): NodePositionTime {
    val fix = position.timestamp.toUInt().toLong().takeIf { it > 0L }
    val sameDay = fix != null && fix.localDate(timeZone) == nowSeconds.localDate(timeZone)
    return NodePositionTime(NodeFreshness.position(this, nowSeconds), fix, sameDay)
}

/** The chip tag of each node that has one, by node number. */
internal fun positionAlerts(nodes: List<Node>, nowSeconds: Long): Map<Int, ChipAlert> =
    nodes.mapNotNull { node -> NodeFreshness.position(node, nowSeconds).chipAlert?.let { node.num to it } }.toMap()

private fun Long.localDate(timeZone: TimeZone) = Instant.fromEpochSeconds(this).toLocalDateTime(timeZone).date
