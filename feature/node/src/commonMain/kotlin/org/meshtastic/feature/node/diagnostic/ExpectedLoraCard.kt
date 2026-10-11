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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.diagnostic.LoraSetting
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.cancel
import org.meshtastic.core.resources.diag_expected_clear
import org.meshtastic.core.resources.diag_expected_edit
import org.meshtastic.core.resources.diag_expected_none
import org.meshtastic.core.resources.diag_expected_pc_only
import org.meshtastic.core.resources.diag_expected_preset
import org.meshtastic.core.resources.diag_expected_region
import org.meshtastic.core.resources.diag_expected_summary
import org.meshtastic.core.resources.diag_expected_title
import org.meshtastic.core.resources.diag_expected_use_mine
import org.meshtastic.core.resources.save

/**
 * The expected LoRa setting: shown to everyone, changed in command post mode only. It is kept on this phone and never
 * sent; the diagnostic compares the radio's setting with it.
 */
@Composable
internal fun ExpectedLoraCard(
    expected: LoraSetting?,
    localLora: LoraSetting?,
    commandPostMode: Boolean,
    onExpectedChange: (LoraSetting?) -> Unit,
) {
    var editing by remember { mutableStateOf(false) }
    DiagnosticCard(title = stringResource(Res.string.diag_expected_title)) {
        Text(
            text = expected?.let { loraSettingText(it) } ?: stringResource(Res.string.diag_expected_none),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = stringResource(Res.string.diag_expected_summary),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (commandPostMode) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { editing = true }) { Text(stringResource(Res.string.diag_expected_edit)) }
                OutlinedButton(
                    onClick = { onExpectedChange(localLora) },
                    enabled = localLora != null && localLora.region != LoraSetting.UNSET_REGION,
                ) {
                    Text(stringResource(Res.string.diag_expected_use_mine))
                }
                OutlinedButton(onClick = { onExpectedChange(null) }, enabled = expected != null) {
                    Text(stringResource(Res.string.diag_expected_clear))
                }
            }
        } else {
            Text(
                text = stringResource(Res.string.diag_expected_pc_only),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (editing) {
        ExpectedLoraDialog(
            initial = expected ?: localLora?.takeIf { it.region != LoraSetting.UNSET_REGION },
            onSave = {
                onExpectedChange(it)
                editing = false
            },
            onDismiss = { editing = false },
        )
    }
}

@Composable
private fun ExpectedLoraDialog(initial: LoraSetting?, onSave: (LoraSetting) -> Unit, onDismiss: () -> Unit) {
    var region by remember { mutableStateOf(initial?.region ?: DiagnosticViewModel.REGION_CHOICES.first()) }
    var preset by remember { mutableStateOf(initial?.preset ?: DiagnosticViewModel.PRESET_CHOICES.first()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.diag_expected_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ChoiceField(
                    label = stringResource(Res.string.diag_expected_region),
                    value = region,
                    choices = DiagnosticViewModel.REGION_CHOICES,
                    choiceText = { it },
                    onChoose = { region = it },
                )
                ChoiceField(
                    label = stringResource(Res.string.diag_expected_preset),
                    value = preset,
                    choices = DiagnosticViewModel.PRESET_CHOICES,
                    choiceText = { it },
                    onChoose = { preset = it },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(LoraSetting(region, preset)) }) { Text(stringResource(Res.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
    )
}

/** A labelled button that opens a list of [choices]. */
@Composable
internal fun <T> ChoiceField(
    label: String,
    value: T,
    choices: List<T>,
    choiceText: (T) -> String,
    onChoose: (T) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        Box {
            OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) { Text(choiceText(value)) }
            DropdownMenu(
                expanded = open,
                onDismissRequest = { open = false },
                modifier = Modifier.heightIn(max = 320.dp),
            ) {
                choices.forEach { choice ->
                    DropdownMenuItem(
                        text = { Text(choiceText(choice)) },
                        onClick = {
                            onChoose(choice)
                            open = false
                        },
                    )
                }
            }
        }
    }
}
