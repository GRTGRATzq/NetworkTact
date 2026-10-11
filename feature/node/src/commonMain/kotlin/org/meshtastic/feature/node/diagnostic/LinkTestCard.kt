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
package org.meshtastic.feature.node.diagnostic

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.util.DateFormatter
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.cancel
import org.meshtastic.core.resources.diag_test_confirm_title
import org.meshtastic.core.resources.diag_test_confirm_traceroute
import org.meshtastic.core.resources.diag_test_demo
import org.meshtastic.core.resources.diag_test_demo_sent
import org.meshtastic.core.resources.diag_test_explain
import org.meshtastic.core.resources.diag_test_no_node
import org.meshtastic.core.resources.diag_test_node
import org.meshtastic.core.resources.diag_test_not_connected
import org.meshtastic.core.resources.diag_test_sent
import org.meshtastic.core.resources.diag_test_title
import org.meshtastic.core.resources.diag_test_traceroute
import org.meshtastic.core.resources.diag_test_transmit
import org.meshtastic.core.resources.diag_test_wait

/**
 * "Test the link": the user picks a heard node, then confirms after reading the airtime warning. One traceroute per
 * confirmation; nothing is ever sent from this screen otherwise.
 */
@Composable
internal fun LinkTestCard(targets: List<TestTarget>, testState: LinkTestUiState, onTraceroute: (TestTarget) -> Unit) {
    var chosenNum by remember { mutableStateOf<Int?>(null) }
    var confirming by remember { mutableStateOf(false) }
    val chosen = targets.firstOrNull { it.num == chosenNum } ?: targets.firstOrNull()
    DiagnosticCard(title = stringResource(Res.string.diag_test_title)) {
        Text(text = stringResource(Res.string.diag_test_explain), style = MaterialTheme.typography.bodyMedium)
        if (chosen == null) {
            Text(text = stringResource(Res.string.diag_test_no_node), style = MaterialTheme.typography.bodyMedium)
        } else {
            ChoiceField(
                label = stringResource(Res.string.diag_test_node),
                value = chosen,
                choices = targets,
                choiceText = { it.name },
                onChoose = { chosenNum = it.num },
            )
            Button(onClick = { confirming = true }, enabled = testState.canTest) {
                Text(stringResource(Res.string.diag_test_traceroute))
            }
        }
        LinkTestStatus(testState)
    }
    if (confirming && chosen != null) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text(stringResource(Res.string.diag_test_confirm_title)) },
            text = { Text(stringResource(Res.string.diag_test_confirm_traceroute, chosen.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onTraceroute(chosen)
                        confirming = false
                    },
                ) {
                    Text(stringResource(Res.string.diag_test_transmit))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirming = false }) { Text(stringResource(Res.string.cancel)) }
            },
        )
    }
}

/** Why the test cannot run now, and what happened to the last one. */
@Composable
private fun LinkTestStatus(state: LinkTestUiState) {
    when {
        state.demo -> FactLine(stringResource(Res.string.diag_test_demo), alert = false)
        !state.connected -> FactLine(stringResource(Res.string.diag_test_not_connected), alert = true)
    }
    if (state.waitSeconds > 0) FactLine(stringResource(Res.string.diag_test_wait, state.waitSeconds), alert = false)
    state.lastSent?.let { sent ->
        val time = DateFormatter.formatTime(sent.atMillis)
        val text =
            if (sent.simulated) {
                stringResource(Res.string.diag_test_demo_sent, time)
            } else {
                stringResource(Res.string.diag_test_sent, time)
            }
        FactLine(text, alert = false)
    }
}
