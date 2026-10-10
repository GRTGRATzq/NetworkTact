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
package org.meshtastic.feature.messaging.coordinates

import kotlinx.datetime.TimeZone
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.freshness.NodeFreshness
import org.meshtastic.core.model.freshness.PositionState
import org.meshtastic.core.model.geo.LatLon
import org.meshtastic.core.model.geo.PositionMessage
import org.meshtastic.core.model.geo.PositionSource
import org.meshtastic.core.model.team.TeamSuffix
import org.meshtastic.core.repository.PhoneFix

/** What the "Ma position" shortcut can put in the input. */
sealed interface MyPositionResult {
    /**
     * [text] to review and send; [fromPhone] when the radio had no position; [shortened] when DMS/UTM were left out.
     */
    data class Filled(val text: String, val fromPhone: Boolean, val shortened: Boolean) : MyPositionResult

    /** Neither the radio nor the phone has a position (or the radio is unknown): nothing is filled in. */
    data object NoPosition : MyPositionResult
}

/**
 * The `[POS]` message for the user's own radio [ourNode], or for the [phone] fix when the radio has no position. The
 * radio's age follows the command post rule ([NodeFreshness]); the phone's comes from its provider's fix time.
 */
internal fun myPositionMessage(
    ourNode: Node?,
    phone: PhoneFix?,
    nowEpochSeconds: Long,
    timeZone: TimeZone,
): MyPositionResult {
    val radio = ourNode?.takeIf { it.validPosition != null }
    return when {
        ourNode == null -> MyPositionResult.NoPosition

        radio != null ->
            filled(
                name = displayName(radio),
                point = LatLon(radio.latitude, radio.longitude),
                source = PositionSource.RADIO,
                state = NodeFreshness.position(radio, nowEpochSeconds),
                nowEpochSeconds = nowEpochSeconds,
                timeZone = timeZone,
            )

        phone != null ->
            filled(
                name = displayName(ourNode),
                point = LatLon(phone.latitude, phone.longitude),
                source = PositionSource.PHONE,
                // The provider's fix time is passed as the fix timestamp, never as a reception time.
                state =
                NodeFreshness.position(
                    hasPosition = true,
                    fixTimestampSeconds = phone.fixEpochSeconds.toInt(),
                    positionTimeSeconds = 0,
                    nowSeconds = nowEpochSeconds,
                ),
                nowEpochSeconds = nowEpochSeconds,
                timeZone = timeZone,
            )

        else -> MyPositionResult.NoPosition
    }
}

private fun displayName(node: Node): String =
    TeamSuffix.displayName(node.user.long_name).ifBlank { node.user.short_name }

private fun filled(
    name: String,
    point: LatLon,
    source: PositionSource,
    state: PositionState,
    nowEpochSeconds: Long,
    timeZone: TimeZone,
): MyPositionResult.Filled {
    val text = PositionMessage.build(name, point, source, state, nowEpochSeconds, timeZone)
    return MyPositionResult.Filled(
        text = text,
        fromPhone = source == PositionSource.PHONE,
        shortened = point.isInUtmLimits && !text.contains(" · DMS "),
    )
}
