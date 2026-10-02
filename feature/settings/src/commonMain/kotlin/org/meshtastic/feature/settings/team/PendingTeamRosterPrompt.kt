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

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.meshtastic.core.model.team.TeamRosterRecord
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.teams_pending_accept
import org.meshtastic.core.resources.teams_pending_dismiss
import org.meshtastic.core.resources.teams_pending_later
import org.meshtastic.core.resources.teams_pending_title

/**
 * Asks, wherever the user is in the app, whether to adopt a team list just received over the mesh. Nothing is adopted
 * without an answer. "Later" only hides this prompt for that list; it stays on the Teams screen until decided, and a
 * newer list asks again.
 */
@Composable
fun PendingTeamRosterPrompt(modifier: Modifier = Modifier, viewModel: TeamsViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var postponed by remember { mutableStateOf<TeamRosterRecord?>(null) }
    val pending = state.pending
    if (pending != null && pending != postponed) {
        AlertDialog(
            onDismissRequest = { postponed = pending },
            modifier = modifier,
            title = { Text(stringResource(Res.string.teams_pending_title)) },
            text = { PendingRosterDetails(pending) },
            confirmButton = {
                Button(onClick = { viewModel.acceptPending(pending) }) {
                    Text(stringResource(Res.string.teams_pending_accept))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissPending(pending) }) {
                    Text(stringResource(Res.string.teams_pending_dismiss))
                }
                TextButton(onClick = { postponed = pending }) { Text(stringResource(Res.string.teams_pending_later)) }
            },
        )
    }
}
