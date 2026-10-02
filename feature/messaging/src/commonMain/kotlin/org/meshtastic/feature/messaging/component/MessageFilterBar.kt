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

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.tactmsg_filter_empty
import org.meshtastic.core.resources.tactmsg_filter_team
import org.meshtastic.core.resources.tactmsg_filter_unacked
import org.meshtastic.core.resources.tactmsg_filter_unacked_direct_only
import org.meshtastic.core.resources.tactmsg_filters
import org.meshtastic.core.resources.teams_filter_no_list
import org.meshtastic.core.resources.teams_member
import org.meshtastic.core.ui.icon.Check
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.theme.AppTheme
import org.meshtastic.feature.messaging.filter.ThreadFilter
import org.meshtastic.feature.messaging.priority.MessagePriority

/**
 * Filters at the top of a conversation: priority (Info / CR / Urgent), unacknowledged direct messages, and one chip per
 * team of the adopted list ([teams]), which keeps the messages of senders declaring that team. Without a list the team
 * chip stays disabled and says why. Selection is shown by a check mark, not by colour.
 */
@Composable
internal fun MessageFilterBar(
    filter: ThreadFilter,
    isDirectMessage: Boolean,
    onFilterChange: (ThreadFilter) -> Unit,
    modifier: Modifier = Modifier,
    teams: List<String> = emptyList(),
) {
    val description = stringResource(Res.string.tactmsg_filters)
    Column(modifier = modifier.fillMaxWidth().semantics { contentDescription = description }) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MessagePriority.entries.forEach { priority ->
                CheckFilterChip(
                    selected = priority in filter.priorities,
                    label = stringResource(priority.badgeRes()),
                    onClick = { onFilterChange(filter.togglePriority(priority)) },
                )
            }
            CheckFilterChip(
                selected = isDirectMessage && filter.unackedOnly,
                label =
                if (isDirectMessage) {
                    stringResource(Res.string.tactmsg_filter_unacked)
                } else {
                    stringResource(Res.string.tactmsg_filter_unacked) +
                        " · " +
                        stringResource(Res.string.tactmsg_filter_unacked_direct_only)
                },
                enabled = isDirectMessage,
                onClick = { onFilterChange(filter.copy(unackedOnly = !filter.unackedOnly)) },
            )
            // A team selected before the list changed stays offered, so the filter can still be cleared.
            val teamChoices =
                teams + listOfNotNull(filter.team?.takeIf { selected -> teams.none { it.equals(selected, true) } })
            if (teamChoices.isEmpty()) {
                CheckFilterChip(
                    selected = false,
                    label = stringResource(Res.string.tactmsg_filter_team),
                    enabled = false,
                    onClick = {},
                )
                Text(
                    text = stringResource(Res.string.teams_filter_no_list),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            teamChoices.forEach { team ->
                CheckFilterChip(
                    selected = filter.team.equals(team, ignoreCase = true),
                    label = stringResource(Res.string.teams_member, team),
                    onClick = { onFilterChange(filter.toggleTeam(team)) },
                )
            }
        }
        HorizontalDivider()
    }
}

@Composable
private fun CheckFilterChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = modifier,
        enabled = enabled,
        leadingIcon =
        if (selected) {
            { Icon(MeshtasticIcons.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
        } else {
            null
        },
    )
}

/** Shown in place of the list when the active filters hide every loaded message. */
@Composable
internal fun FilteredEmptyNotice(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(Res.string.tactmsg_filter_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@PreviewLightDark
@Composable
private fun MessageFilterBarPreview() {
    AppTheme {
        Surface {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MessageFilterBar(
                    filter = ThreadFilter(priorities = setOf(MessagePriority.URGENT), unackedOnly = true),
                    isDirectMessage = true,
                    onFilterChange = {},
                )
                MessageFilterBar(
                    filter = ThreadFilter(team = "Alpha"),
                    isDirectMessage = false,
                    onFilterChange = {},
                    teams = listOf("Alpha", "Bravo"),
                )
                MessageFilterBar(filter = ThreadFilter(), isDirectMessage = false, onFilterChange = {})
            }
        }
    }
}
