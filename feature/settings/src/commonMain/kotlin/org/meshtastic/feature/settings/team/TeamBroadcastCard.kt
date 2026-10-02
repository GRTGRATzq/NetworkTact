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
package org.meshtastic.feature.settings.team

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.team.RosterInput
import org.meshtastic.core.model.team.TeamRoster
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.teams_blocked_not_connected
import org.meshtastic.core.resources.teams_broadcast_channel
import org.meshtastic.core.resources.teams_broadcast_failed
import org.meshtastic.core.resources.teams_broadcast_hint
import org.meshtastic.core.resources.teams_broadcast_send
import org.meshtastic.core.resources.teams_broadcast_sent
import org.meshtastic.core.resources.teams_broadcast_title
import org.meshtastic.core.resources.teams_manual_hint

/**
 * Command post only: type the list, pick a channel, broadcast. The field starts from the current list. The same
 * validation as the manual entry applies, so a list too long for one text message cannot be sent.
 */
@Composable
internal fun TeamBroadcastCard(state: TeamsUiState, result: BroadcastResult?, onBroadcast: (TeamRoster, Int) -> Unit) {
    var text by
        rememberSaveable(state.roster?.teams) { mutableStateOf(state.roster?.teams?.joinToString("\n").orEmpty()) }
    var channel by rememberSaveable { mutableIntStateOf(0) }
    val input = remember(text) { text.takeIf { it.isNotBlank() }?.let(TeamRoster::parseInput) }
    val error = input?.errorText()
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(Res.string.teams_broadcast_title), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(Res.string.teams_broadcast_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(Res.string.teams_manual_hint)) },
                minLines = 3,
                isError = error != null,
                supportingText =
                if (error != null) {
                    { Text(error) }
                } else {
                    null
                },
            )
            Text(stringResource(Res.string.teams_broadcast_channel), style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.channels.forEachIndexed { index, name ->
                    FilterChip(selected = channel == index, onClick = { channel = index }, label = { Text(name) })
                }
            }
            if (!state.connected) {
                Text(
                    text = stringResource(Res.string.teams_blocked_not_connected),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = { (input as? RosterInput.Valid)?.let { onBroadcast(it.roster, channel) } },
                enabled = state.connected && input is RosterInput.Valid && channel in state.channels.indices,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(stringResource(Res.string.teams_broadcast_send))
            }
            result?.let {
                Text(
                    text =
                    when (it) {
                        is BroadcastResult.Sent -> stringResource(Res.string.teams_broadcast_sent, it.channelName)
                        BroadcastResult.Failed -> stringResource(Res.string.teams_broadcast_failed)
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
