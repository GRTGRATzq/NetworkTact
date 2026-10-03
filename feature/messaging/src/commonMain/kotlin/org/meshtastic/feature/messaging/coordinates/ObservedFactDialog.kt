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
package org.meshtastic.feature.messaging.coordinates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.geo.CoordinateFormat
import org.meshtastic.core.model.geo.CoordinateParser
import org.meshtastic.core.model.geo.PositionMessage
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.cancel
import org.meshtastic.core.resources.coords_fact_description
import org.meshtastic.core.resources.coords_fact_description_required
import org.meshtastic.core.resources.coords_fact_insert
import org.meshtastic.core.resources.coords_fact_place
import org.meshtastic.core.resources.coords_fact_review
import org.meshtastic.core.resources.coords_fact_title
import org.meshtastic.core.resources.coords_fact_too_long
import org.meshtastic.feature.messaging.component.AlertPill

/**
 * Short observed fact form: the place, typed in UTM, MGRS or DMS and checked as it is typed, and a free description.
 * The resulting `[CR] FAIT OBSERVÉ` report is shown before it goes into the input, where the user reviews and sends.
 */
@Composable
internal fun ObservedFactDialog(onInsert: (String) -> Unit, onDismiss: () -> Unit) {
    // Saved by name: an enum is not saveable on every platform.
    var formatName by rememberSaveable { mutableStateOf(CoordinateFormat.MGRS.name) }
    val format = CoordinateFormat.valueOf(formatName)
    var placeText by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    val place = remember(placeText, format) { CoordinateParser.parse(placeText, format) }
    val draft = remember(place, format, description) { observedFactDraft(place, format, description) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.coords_fact_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CoordinateField(
                    format = format,
                    text = placeText,
                    input = place,
                    onFormatChange = { formatName = it.name },
                    onTextChange = { placeText = it },
                    label = stringResource(Res.string.coords_fact_place),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(Res.string.coords_fact_description)) },
                    placeholder = { Text(stringResource(Res.string.coords_fact_description_required)) },
                    minLines = 2,
                    maxLines = MAX_DESCRIPTION_LINES,
                )
                draft.message?.let { message ->
                    Text(message, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                }
                if (draft.bytes > PositionMessage.MAX_MESSAGE_BYTES) {
                    AlertPill(
                        text =
                        stringResource(
                            Res.string.coords_fact_too_long,
                            draft.bytes,
                            PositionMessage.MAX_MESSAGE_BYTES,
                        ),
                    )
                }
                Text(
                    stringResource(Res.string.coords_fact_review),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { draft.message?.let(onInsert) }, enabled = draft.canInsert) {
                Text(stringResource(Res.string.coords_fact_insert))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
    )
}

private const val MAX_DESCRIPTION_LINES = 4
