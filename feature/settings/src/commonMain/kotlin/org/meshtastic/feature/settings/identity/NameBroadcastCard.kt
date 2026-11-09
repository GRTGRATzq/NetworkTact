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
package org.meshtastic.feature.settings.identity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
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
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.identity.NameBroadcastCheck
import org.meshtastic.core.model.identity.NameBroadcastRefusal
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.cancel
import org.meshtastic.core.resources.name_broadcast_button
import org.meshtastic.core.resources.name_broadcast_confirm
import org.meshtastic.core.resources.name_broadcast_confirm_text
import org.meshtastic.core.resources.name_broadcast_confirm_title
import org.meshtastic.core.resources.name_broadcast_failed
import org.meshtastic.core.resources.name_broadcast_hint
import org.meshtastic.core.resources.name_broadcast_not_applied
import org.meshtastic.core.resources.name_broadcast_refused_no_node
import org.meshtastic.core.resources.name_broadcast_refused_not_connected
import org.meshtastic.core.resources.name_broadcast_sent
import org.meshtastic.core.resources.name_broadcast_simulated
import org.meshtastic.core.resources.name_broadcast_title
import org.meshtastic.core.resources.name_broadcast_wait
import org.meshtastic.core.resources.name_broadcast_waiting
import org.meshtastic.core.ui.theme.AppTheme

/**
 * "Diffuser mon nom": a button that broadcasts my radio's name once, after a confirmation that warns about air time,
 * then waits 30 s; refused while the radio is not connected. Also says what the automatic broadcast after a name change
 * came to. [onBroadcast] is called only once the user has confirmed.
 */
@Composable
fun NameBroadcastCard(state: NameBroadcastUiState, onBroadcast: () -> Unit, modifier: Modifier = Modifier) {
    var confirming by remember { mutableStateOf<NameBroadcastCheck.Ready?>(null) }
    NameBroadcastContent(
        state = state,
        onBroadcast = { confirming = state.check as? NameBroadcastCheck.Ready },
        modifier = modifier,
    )
    confirming?.let { ready ->
        NameBroadcastConfirmation(
            ready = ready,
            onConfirm = {
                confirming = null
                onBroadcast()
            },
            onDismiss = { confirming = null },
        )
    }
}

@Composable
internal fun NameBroadcastContent(state: NameBroadcastUiState, onBroadcast: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(Res.string.name_broadcast_title), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(Res.string.name_broadcast_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            state.event?.let { Text(text = it.message(), style = MaterialTheme.typography.bodyMedium) }
            state.check.reason()?.let { reason ->
                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(onClick = onBroadcast, enabled = state.check is NameBroadcastCheck.Ready) {
                Text(stringResource(Res.string.name_broadcast_button))
            }
        }
    }
}

@Composable
private fun NameBroadcastConfirmation(ready: NameBroadcastCheck.Ready, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.name_broadcast_confirm_title)) },
        text = { Text(stringResource(Res.string.name_broadcast_confirm_text, ready.longName, ready.shortName)) },
        confirmButton = { Button(onClick = onConfirm) { Text(stringResource(Res.string.name_broadcast_confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
    )
}

/** Why the button is off, or null when it is on. */
@Composable
private fun NameBroadcastCheck.reason(): String? = when (this) {
    is NameBroadcastCheck.Ready -> null
    is NameBroadcastCheck.CoolingDown -> stringResource(Res.string.name_broadcast_wait, remainingSeconds)
    is NameBroadcastCheck.Refused -> reason.text()
}

@Composable
private fun NameBroadcastRefusal.text(): String = when (this) {
    NameBroadcastRefusal.NOT_CONNECTED -> stringResource(Res.string.name_broadcast_refused_not_connected)
    NameBroadcastRefusal.NO_LOCAL_NODE -> stringResource(Res.string.name_broadcast_refused_no_node)
}

@Composable
private fun NameBroadcastEvent.message(): String = when (this) {
    is NameBroadcastEvent.Waiting -> stringResource(Res.string.name_broadcast_waiting, longName)
    is NameBroadcastEvent.Sent -> stringResource(Res.string.name_broadcast_sent, longName)
    is NameBroadcastEvent.Simulated -> stringResource(Res.string.name_broadcast_simulated, longName)
    NameBroadcastEvent.Failed -> stringResource(Res.string.name_broadcast_failed)
    is NameBroadcastEvent.NotApplied -> stringResource(Res.string.name_broadcast_not_applied, longName)
}

@PreviewLightDark
@Composable
private fun NameBroadcastContentPreview() {
    AppTheme {
        Surface {
            NameBroadcastContent(
                state =
                NameBroadcastUiState(
                    check = NameBroadcastCheck.CoolingDown(remainingSeconds = 21),
                    event = NameBroadcastEvent.Sent(longName = "CDS TIR [Alpha]", automatic = true),
                ),
                onBroadcast = {},
            )
        }
    }
}
