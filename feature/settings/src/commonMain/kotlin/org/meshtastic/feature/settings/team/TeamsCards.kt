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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.util.DateFormatter
import org.meshtastic.core.model.NodeAddress
import org.meshtastic.core.model.team.RosterInput
import org.meshtastic.core.model.team.TeamRoster
import org.meshtastic.core.model.team.TeamRosterRecord
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.teams_blocked_licensed
import org.meshtastic.core.resources.teams_blocked_no_node
import org.meshtastic.core.resources.teams_blocked_not_connected
import org.meshtastic.core.resources.teams_current_list
import org.meshtastic.core.resources.teams_error_empty
import org.meshtastic.core.resources.teams_error_invalid_name
import org.meshtastic.core.resources.teams_error_message_too_long
import org.meshtastic.core.resources.teams_error_name_too_long
import org.meshtastic.core.resources.teams_manual_hint
import org.meshtastic.core.resources.teams_manual_title
import org.meshtastic.core.resources.teams_manual_use
import org.meshtastic.core.resources.teams_my_radio_name
import org.meshtastic.core.resources.teams_my_team
import org.meshtastic.core.resources.teams_my_team_not_in_list
import org.meshtastic.core.resources.teams_no_list
import org.meshtastic.core.resources.teams_none
import org.meshtastic.core.resources.teams_origin_broadcast
import org.meshtastic.core.resources.teams_origin_manual
import org.meshtastic.core.resources.teams_origin_radio
import org.meshtastic.core.resources.teams_pending_accept
import org.meshtastic.core.resources.teams_pending_dismiss
import org.meshtastic.core.resources.teams_pending_from
import org.meshtastic.core.resources.teams_pending_question
import org.meshtastic.core.resources.teams_pending_title
import org.meshtastic.core.resources.teams_send_failed
import org.meshtastic.core.resources.teams_sent
import org.meshtastic.core.resources.teams_unknown_sender

/** A received list waiting to be adopted, with who sent it and when. */
@Composable
internal fun PendingRosterCard(
    record: TeamRosterRecord,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(Res.string.teams_pending_title), style = MaterialTheme.typography.titleMedium)
            PendingRosterDetails(record)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(stringResource(Res.string.teams_pending_dismiss)) }
                Button(onClick = onAccept) { Text(stringResource(Res.string.teams_pending_accept)) }
            }
        }
    }
}

/** Sender name and id, time, the teams, and the authentication warning: everything needed to decide. */
@Composable
internal fun PendingRosterDetails(record: TeamRosterRecord, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text =
            stringResource(
                Res.string.teams_pending_from,
                record.senderName ?: stringResource(Res.string.teams_unknown_sender),
                record.senderNum?.let(NodeAddress::numToDefaultId).orEmpty(),
                formatRecordTime(record),
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(text = record.teams.joinToString(", "), style = MaterialTheme.typography.bodyLarge)
        Text(
            text = stringResource(Res.string.teams_pending_question),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun CurrentListCard(record: TeamRosterRecord?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(Res.string.teams_current_list), style = MaterialTheme.typography.titleMedium)
            if (record == null) {
                Text(stringResource(Res.string.teams_no_list), style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(record.teams.joinToString(", "), style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = originText(record),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun originText(record: TeamRosterRecord): String {
    val time = formatRecordTime(record)
    return when (record.source) {
        TeamRosterRecord.Source.RADIO ->
            stringResource(
                Res.string.teams_origin_radio,
                record.senderName ?: stringResource(Res.string.teams_unknown_sender),
                record.senderNum?.let(NodeAddress::numToDefaultId).orEmpty(),
                time,
            )

        TeamRosterRecord.Source.MANUAL -> stringResource(Res.string.teams_origin_manual, time)

        TeamRosterRecord.Source.BROADCAST -> stringResource(Res.string.teams_origin_broadcast, time)
    }
}

private fun formatRecordTime(record: TeamRosterRecord): String =
    DateFormatter.formatDateTimeShort(record.timeSeconds * MILLIS_PER_SECOND)

private const val MILLIS_PER_SECOND = 1000L

@Composable
internal fun MyTeamCard(state: TeamsUiState, sendResult: TeamSendResult?, onChooseTeam: (String?) -> Unit) {
    val enabled = state.block == null
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(Res.string.teams_my_team), style = MaterialTheme.typography.titleMedium)
            state.myLongName?.let {
                Text(stringResource(Res.string.teams_my_radio_name, it), style = MaterialTheme.typography.bodyMedium)
            }
            state.block?.let { block ->
                Text(
                    text = stringResource(block.messageRes()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (state.myTeamMissingFromList) {
                Text(
                    text = stringResource(Res.string.teams_my_team_not_in_list, state.myTeam.orEmpty()),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Column(modifier = Modifier.selectableGroup()) {
                state.roster?.teams?.forEach { team ->
                    TeamOption(
                        label = team,
                        selected = state.myTeam.equals(team, ignoreCase = true),
                        enabled = enabled,
                        onClick = { onChooseTeam(team) },
                    )
                }
                TeamOption(
                    label = stringResource(Res.string.teams_none),
                    selected = state.myLongName != null && state.myTeam == null,
                    enabled = enabled,
                    onClick = { onChooseTeam(null) },
                )
            }
            sendResult?.let { result ->
                Text(
                    text =
                    stringResource(
                        if (result == TeamSendResult.SENT) Res.string.teams_sent else Res.string.teams_send_failed,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

private fun OwnerWriteBlock.messageRes() = when (this) {
    OwnerWriteBlock.NOT_CONNECTED -> Res.string.teams_blocked_not_connected
    OwnerWriteBlock.NO_LOCAL_NODE -> Res.string.teams_blocked_no_node
    OwnerWriteBlock.LICENSED -> Res.string.teams_blocked_licensed
}

@Composable
private fun TeamOption(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Row(
        modifier =
        Modifier.fillMaxWidth()
            .heightIn(min = 48.dp)
            // Choosing the current team again changes nothing, so it sends nothing either.
            .selectable(
                selected = selected,
                enabled = enabled && !selected,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Text(text = label, modifier = Modifier.padding(start = 12.dp), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
internal fun ManualEntryCard(onAdoptManual: (TeamRoster) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val input = remember(text) { text.takeIf { it.isNotBlank() }?.let(TeamRoster::parseInput) }
    val error = input?.errorText()
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(Res.string.teams_manual_title), style = MaterialTheme.typography.titleMedium)
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
            Button(
                onClick = {
                    (input as? RosterInput.Valid)?.let {
                        onAdoptManual(it.roster)
                        text = ""
                    }
                },
                enabled = input is RosterInput.Valid,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(stringResource(Res.string.teams_manual_use))
            }
        }
    }
}

/** The message for an invalid list, or null when [this] is valid. */
@Composable
internal fun RosterInput.errorText(): String? = when (this) {
    is RosterInput.Valid -> null

    RosterInput.Empty -> stringResource(Res.string.teams_error_empty)

    is RosterInput.InvalidName -> stringResource(Res.string.teams_error_invalid_name, name)

    is RosterInput.NameTooLong ->
        stringResource(Res.string.teams_error_name_too_long, name, TeamRoster.MAX_TEAM_NAME_BYTES)

    is RosterInput.MessageTooLong ->
        stringResource(Res.string.teams_error_message_too_long, bytes, TeamRoster.MAX_MESSAGE_BYTES)
}
