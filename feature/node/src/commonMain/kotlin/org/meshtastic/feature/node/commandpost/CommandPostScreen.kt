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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.util.NumberFormatter
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.command_post_contact_inconsistent
import org.meshtastic.core.resources.command_post_contact_never
import org.meshtastic.core.resources.command_post_contact_recent
import org.meshtastic.core.resources.command_post_contact_stale
import org.meshtastic.core.resources.command_post_duration_days
import org.meshtastic.core.resources.command_post_duration_hours
import org.meshtastic.core.resources.command_post_duration_minutes
import org.meshtastic.core.resources.command_post_duration_seconds
import org.meshtastic.core.resources.command_post_empty
import org.meshtastic.core.resources.command_post_position_fix_unknown
import org.meshtastic.core.resources.command_post_position_fresh
import org.meshtastic.core.resources.command_post_position_inconsistent
import org.meshtastic.core.resources.command_post_position_no_time
import org.meshtastic.core.resources.command_post_position_none
import org.meshtastic.core.resources.command_post_position_stale
import org.meshtastic.core.resources.command_post_position_stale_at_least
import org.meshtastic.core.resources.command_post_read_only
import org.meshtastic.core.resources.command_post_title
import org.meshtastic.core.resources.message
import org.meshtastic.core.ui.component.MainAppBar
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.Message
import org.meshtastic.core.ui.icon.Warning
import kotlin.time.Duration

private const val COORDINATE_DECIMALS = 5
private const val MINUTES_PER_HOUR = 60
private const val HOURS_PER_DAY = 24

@Composable
fun CommandPostScreen(
    viewModel: CommandPostViewModel,
    onNavigateUp: () -> Unit,
    onOpenNode: (Int) -> Unit,
    onOpenMessages: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    Scaffold(
        modifier = modifier,
        topBar = {
            MainAppBar(
                title = stringResource(Res.string.command_post_title),
                ourNode = null,
                showNodeChip = false,
                canNavigateUp = true,
                onNavigateUp = onNavigateUp,
                onClickChip = {},
                actions = {},
            )
        },
    ) { padding ->
        CommandPostContent(
            rows = rows,
            onOpenNode = onOpenNode,
            onOpenMessages = onOpenMessages,
            modifier = Modifier.padding(padding),
        )
    }
}

/** Stateless list of [CommandPostRow]s; tapping a row opens the node, the Message button its direct conversation. */
@Composable
fun CommandPostContent(
    rows: List<CommandPostRow>,
    onOpenNode: (Int) -> Unit,
    onOpenMessages: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(
                text = stringResource(Res.string.command_post_read_only),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (rows.isEmpty()) {
            item { Text(text = stringResource(Res.string.command_post_empty)) }
        }
        items(rows, key = { it.num }) { row ->
            CommandPostCard(row = row, onOpenNode = onOpenNode, onOpenMessages = onOpenMessages)
        }
    }
}

@Composable
private fun CommandPostCard(row: CommandPostRow, onOpenNode: (Int) -> Unit, onOpenMessages: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().testTag("command_post_row_${row.num}").clickable { onOpenNode(row.num) }) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = row.shortName.ifBlank { row.longName },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (row.longName.isNotBlank() && row.longName != row.shortName) {
                        Text(text = row.longName, style = MaterialTheme.typography.bodySmall)
                    }
                }
                row.directMessageKey?.let { key ->
                    OutlinedButton(onClick = { onOpenMessages(key) }) {
                        Icon(MeshtasticIcons.Message, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(text = stringResource(Res.string.message), modifier = Modifier.padding(start = 6.dp))
                    }
                }
            }
            ContactLine(row.contact)
            PositionLine(row.position)
            if (row.latitude != null && row.longitude != null) {
                Text(
                    text =
                    "${NumberFormatter.format(row.latitude, COORDINATE_DECIMALS)}, " +
                        NumberFormatter.format(row.longitude, COORDINATE_DECIMALS),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ContactLine(state: ContactState) {
    val neutral = MaterialTheme.colorScheme.onSurfaceVariant
    val (text, color) =
        when (state) {
            ContactState.NeverHeard -> stringResource(Res.string.command_post_contact_never) to neutral

            is ContactState.SeenRecently ->
                stringResource(Res.string.command_post_contact_recent, formatAge(state.age)) to
                    MaterialTheme.colorScheme.onSurface

            is ContactState.NotHeardSince ->
                stringResource(Res.string.command_post_contact_stale, formatAge(state.age)) to neutral

            is ContactState.InconsistentTimestamp ->
                stringResource(Res.string.command_post_contact_inconsistent, formatAge(state.ahead)) to
                    MaterialTheme.colorScheme.error
        }
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = color)
}

@Composable
private fun PositionLine(state: PositionState) {
    val neutral = MaterialTheme.colorScheme.onSurfaceVariant
    val error = MaterialTheme.colorScheme.error
    // Only a fix time from the sender earns the "fresh" colour; every other state stays neutral or alarming.
    val (text, color) =
        when (state) {
            PositionState.NoPosition -> stringResource(Res.string.command_post_position_none) to neutral

            is PositionState.Fresh ->
                stringResource(Res.string.command_post_position_fresh, formatAge(state.age)) to
                    MaterialTheme.colorScheme.primary

            is PositionState.Stale ->
                stringResource(Res.string.command_post_position_stale, formatAge(state.age)) to error

            is PositionState.StaleAtLeast ->
                stringResource(Res.string.command_post_position_stale_at_least, formatAge(state.minAge)) to error

            is PositionState.ReceivedFixTimeUnknown ->
                stringResource(Res.string.command_post_position_fix_unknown, formatAge(state.minAge)) to neutral

            PositionState.NoFixTime -> stringResource(Res.string.command_post_position_no_time) to neutral

            is PositionState.InconsistentTimestamp ->
                stringResource(Res.string.command_post_position_inconsistent, formatAge(state.ahead)) to error
        }
    val alarming = color == error
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (alarming) {
            Icon(
                MeshtasticIcons.Warning,
                contentDescription = null,
                tint = color,
                modifier = Modifier.padding(end = 4.dp).size(16.dp),
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (alarming) FontWeight.Bold else FontWeight.Normal,
            color = color,
        )
    }
}

@Composable
private fun formatAge(duration: Duration): String {
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
