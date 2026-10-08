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
package org.meshtastic.feature.messaging.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.util.DateFormatter
import org.meshtastic.core.model.Message
import org.meshtastic.core.model.geo.MessagePoint
import org.meshtastic.core.model.team.TeamSuffix
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.map_point_observed_fact
import org.meshtastic.core.resources.map_point_observed_fact_no_time
import org.meshtastic.core.resources.map_point_other
import org.meshtastic.core.resources.map_point_position
import org.meshtastic.core.resources.map_point_position_inconsistent
import org.meshtastic.core.resources.map_point_position_no_time
import org.meshtastic.core.resources.map_point_position_old
import org.meshtastic.core.resources.map_point_position_old_no_time
import org.meshtastic.core.resources.map_point_sender_phone

/** What the map says about the point of [message]: its kind, its time or that the time is unknown, and its sender. */
@Composable
internal fun messagePointLabel(message: Message): String {
    val point = remember(message.text) { MessagePoint.of(message.text) }
    val name =
        TeamSuffix.displayName(message.node.user.long_name)
            .ifBlank { message.node.user.short_name }
            .let { if (point.fromPhone) stringResource(Res.string.map_point_sender_phone, it) else it }
    val clock = point.clock
    return when (point.kind) {
        MessagePoint.Kind.OBSERVED_FACT ->
            if (clock != null) {
                stringResource(Res.string.map_point_observed_fact, clock, name)
            } else {
                stringResource(Res.string.map_point_observed_fact_no_time, name)
            }

        MessagePoint.Kind.POSITION -> positionLabel(point, name)

        MessagePoint.Kind.POINT ->
            stringResource(Res.string.map_point_other, name, DateFormatter.formatDateTimeShort(message.displayTime))
    }
}

@Composable
private fun positionLabel(point: MessagePoint, name: String): String {
    val clock = point.clock
    return when {
        point.inconsistent -> stringResource(Res.string.map_point_position_inconsistent, name)
        clock == null && point.old -> stringResource(Res.string.map_point_position_old_no_time, name)
        clock == null -> stringResource(Res.string.map_point_position_no_time, name)
        point.old -> stringResource(Res.string.map_point_position_old, clock, name)
        else -> stringResource(Res.string.map_point_position, clock, name)
    }
}
