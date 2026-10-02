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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.team.TeamNameChange
import org.meshtastic.core.model.team.TeamRoster
import org.meshtastic.core.model.team.TeamRosterRecord
import org.meshtastic.core.model.team.TeamSuffix
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.cancel
import org.meshtastic.core.resources.teams_confirm_names
import org.meshtastic.core.resources.teams_confirm_send
import org.meshtastic.core.resources.teams_confirm_title
import org.meshtastic.core.resources.teams_confirm_truncated
import org.meshtastic.core.resources.teams_confirm_unchanged
import org.meshtastic.core.resources.teams_no_auth
import org.meshtastic.core.resources.teams_title
import org.meshtastic.core.ui.component.MainAppBar
import org.meshtastic.core.ui.theme.AppTheme

@Composable
fun TeamsScreen(viewModel: TeamsViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sendResult by viewModel.sendResult.collectAsStateWithLifecycle()
    val broadcastResult by viewModel.broadcastResult.collectAsStateWithLifecycle()
    // The team the user picked (null for "no team") with its name change, until the change is confirmed or dropped.
    var proposal by remember { mutableStateOf<TeamProposal?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            MainAppBar(
                title = stringResource(Res.string.teams_title),
                canNavigateUp = true,
                onNavigateUp = onBack,
                ourNode = null,
                showNodeChip = false,
                actions = {},
                onClickChip = {},
            )
        },
    ) { paddingValues ->
        TeamsContent(
            state = state,
            sendResult = sendResult,
            onChooseTeam = { team ->
                viewModel.clearSendResult()
                viewModel.previewTeam(team)?.let { proposal = TeamProposal(team, it) }
            },
            onAcceptPending = viewModel::acceptPending,
            onDismissPending = viewModel::dismissPending,
            onAdoptManual = viewModel::adoptManual,
            broadcastResult = broadcastResult,
            onBroadcast = { roster, channel ->
                viewModel.clearBroadcastResult()
                viewModel.broadcast(roster, channel)
            },
            modifier = Modifier.padding(paddingValues),
        )
    }

    proposal?.let { current ->
        TeamChangeDialog(
            currentName = state.myLongName.orEmpty(),
            change = current.change,
            onConfirm = {
                viewModel.applyTeam(current.team)
                proposal = null
            },
            onDismiss = { proposal = null },
        )
    }
}

private data class TeamProposal(val team: String?, val change: TeamNameChange)

@Composable
internal fun TeamsContent(
    state: TeamsUiState,
    sendResult: TeamSendResult?,
    onChooseTeam: (String?) -> Unit,
    onAcceptPending: (TeamRosterRecord) -> Unit,
    onDismissPending: (TeamRosterRecord) -> Unit,
    onAdoptManual: (TeamRoster) -> Unit,
    broadcastResult: BroadcastResult?,
    onBroadcast: (TeamRoster, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        state.pending?.let { pending ->
            item {
                PendingRosterCard(
                    record = pending,
                    onAccept = { onAcceptPending(pending) },
                    onDismiss = { onDismissPending(pending) },
                )
            }
        }
        item { CurrentListCard(state.roster) }
        if (state.commandPostMode) {
            item { TeamBroadcastCard(state = state, result = broadcastResult, onBroadcast = onBroadcast) }
        }
        item { MyTeamCard(state = state, sendResult = sendResult, onChooseTeam = onChooseTeam) }
        item { ManualEntryCard(onAdoptManual = onAdoptManual) }
        item {
            Text(
                text = stringResource(Res.string.teams_no_auth),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TeamChangeDialog(
    currentName: String,
    change: TeamNameChange,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.teams_confirm_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(Res.string.teams_confirm_names, currentName, change.longName))
                if (change.truncated) {
                    Text(
                        text = stringResource(Res.string.teams_confirm_truncated, TeamSuffix.LONG_NAME_MAX_BYTES),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Text(
                    text = stringResource(Res.string.teams_confirm_unchanged),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { Button(onClick = onConfirm) { Text(stringResource(Res.string.teams_confirm_send)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
    )
}

@PreviewLightDark
@Composable
private fun TeamsContentPreview() {
    AppTheme {
        Surface {
            TeamsContent(
                state =
                TeamsUiState(
                    roster =
                    TeamRosterRecord(
                        teams = listOf("Alpha", "Bravo"),
                        source = TeamRosterRecord.Source.RADIO,
                        senderNum = 0x1234abcd,
                        senderName = "PC-0",
                        timeSeconds = 1_790_000_000,
                    ),
                    pending =
                    TeamRosterRecord(
                        teams = listOf("Alpha", "Bravo", "Charlie"),
                        source = TeamRosterRecord.Source.RADIO,
                        senderNum = 0x1234abcd,
                        senderName = "PC-0",
                        timeSeconds = 1_790_000_600,
                    ),
                    myLongName = "ALPHA-1 [Alpha]",
                    myTeam = "Alpha",
                    block = null,
                    commandPostMode = true,
                    channels = listOf("Général", "Équipe Alpha", "PC"),
                    connected = true,
                ),
                sendResult = null,
                onChooseTeam = {},
                onAcceptPending = {},
                onDismissPending = {},
                onAdoptManual = {},
                broadcastResult = null,
                onBroadcast = { _, _ -> },
            )
        }
    }
}
