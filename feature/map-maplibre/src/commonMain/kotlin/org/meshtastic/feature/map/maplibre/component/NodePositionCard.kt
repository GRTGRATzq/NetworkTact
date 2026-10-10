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
package org.meshtastic.feature.map.maplibre.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.util.DateFormatter
import org.meshtastic.core.common.util.nowSeconds
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.freshness.FreshnessThresholds
import org.meshtastic.core.model.freshness.PositionState
import org.meshtastic.core.model.team.TeamSuffix
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.close
import org.meshtastic.core.resources.command_post_duration_days
import org.meshtastic.core.resources.command_post_duration_hours
import org.meshtastic.core.resources.command_post_duration_minutes
import org.meshtastic.core.resources.command_post_duration_seconds
import org.meshtastic.core.resources.command_post_position_fix_unknown
import org.meshtastic.core.resources.command_post_position_fresh
import org.meshtastic.core.resources.command_post_position_inconsistent
import org.meshtastic.core.resources.command_post_position_no_time
import org.meshtastic.core.resources.command_post_position_none
import org.meshtastic.core.resources.command_post_position_stale
import org.meshtastic.core.resources.command_post_position_stale_at_least
import org.meshtastic.core.resources.details
import org.meshtastic.core.resources.map_node_fix_at
import org.meshtastic.core.resources.map_node_fix_on
import org.meshtastic.core.resources.map_node_fix_unknown
import org.meshtastic.core.ui.icon.Close
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.Warning
import org.meshtastic.core.ui.theme.TactColors
import org.meshtastic.core.ui.theme.TactHue
import org.meshtastic.feature.map.maplibre.geojson.chipAlert
import org.meshtastic.feature.map.maplibre.geojson.positionTime
import kotlin.time.Duration

/**
 * What a tap on a node chip shows: the node, how old its position is in the command post view's own words and style
 * (inverted pill when old or inconsistent, green only for a fix time from the sender within ten minutes), and when the
 * position was taken, with its date when not today, or that the time is unknown. "Details" opens the node's page.
 */
@Composable
internal fun NodePositionCard(node: Node, onDetails: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val now by
        produceState(nowSeconds) {
            while (true) {
                delay(FreshnessThresholds.Default.refreshInterval)
                value = nowSeconds
            }
        }
    val time = node.positionTime(now, TimeZone.currentSystemDefault())
    val name = TeamSuffix.displayName(node.user.long_name).ifBlank { node.user.short_name }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 3.dp,
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f, fill = false),
                )
                IconButton(onClick = onDismiss) {
                    Icon(MeshtasticIcons.Close, contentDescription = stringResource(Res.string.close))
                }
            }
            PositionAge(time.state)
            Text(
                text = fixTimeText(time.fixEpochSeconds, time.sameDay),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, end = 16.dp),
            )
            TextButton(onClick = onDetails) { Text(stringResource(Res.string.details)) }
        }
    }
}

/** The command post view's position line, in its style; also used by the "Recaler" card. */
@Composable
internal fun PositionAge(state: PositionState) {
    val text = positionText(state)
    when {
        state.chipAlert != null ->
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                modifier = Modifier.padding(end = 16.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(MeshtasticIcons.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(text = text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }

        state is PositionState.Fresh ->
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = TactColors.legible(TactHue.FRESH, MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.padding(end = 16.dp),
            )

        else ->
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 16.dp),
            )
    }
}

@Composable
private fun positionText(state: PositionState): String = when (state) {
    PositionState.NoPosition -> stringResource(Res.string.command_post_position_none)

    is PositionState.Fresh -> stringResource(Res.string.command_post_position_fresh, formatAge(state.age))

    is PositionState.Stale -> stringResource(Res.string.command_post_position_stale, formatAge(state.age))

    is PositionState.StaleAtLeast ->
        stringResource(Res.string.command_post_position_stale_at_least, formatAge(state.minAge))

    is PositionState.ReceivedFixTimeUnknown ->
        stringResource(Res.string.command_post_position_fix_unknown, formatAge(state.minAge))

    PositionState.NoFixTime -> stringResource(Res.string.command_post_position_no_time)

    is PositionState.InconsistentTimestamp ->
        stringResource(Res.string.command_post_position_inconsistent, formatAge(state.ahead))
}

/** "Relevée à 14:32", with the date when not today, or that the fix time is unknown. */
@Composable
internal fun fixTimeText(fixEpochSeconds: Long?, sameDay: Boolean): String {
    val fixMillis = fixEpochSeconds?.times(MILLIS_PER_SECOND) ?: return stringResource(Res.string.map_node_fix_unknown)
    return if (sameDay) {
        stringResource(Res.string.map_node_fix_at, DateFormatter.formatTime(fixMillis))
    } else {
        stringResource(Res.string.map_node_fix_on, DateFormatter.formatDateTimeShort(fixMillis))
    }
}

/** The command post view's durations: `45 s`, `25 min`, `2 h 05 min`, `3 d 4 h`. */
@Composable
internal fun formatAge(duration: Duration): String {
    val totalMinutes = duration.inWholeMinutes
    return when {
        totalMinutes < 1 -> stringResource(Res.string.command_post_duration_seconds, duration.inWholeSeconds.toString())

        totalMinutes < MINUTES_PER_HOUR ->
            stringResource(Res.string.command_post_duration_minutes, totalMinutes.toString())

        duration.inWholeHours < HOURS_PER_DAY ->
            stringResource(
                Res.string.command_post_duration_hours,
                duration.inWholeHours.toString(),
                (totalMinutes % MINUTES_PER_HOUR).toString().padStart(2, '0'),
            )

        else ->
            stringResource(
                Res.string.command_post_duration_days,
                duration.inWholeDays.toString(),
                (duration.inWholeHours % HOURS_PER_DAY).toString(),
            )
    }
}

private const val MILLIS_PER_SECOND = 1000L
private const val MINUTES_PER_HOUR = 60
private const val HOURS_PER_DAY = 24
