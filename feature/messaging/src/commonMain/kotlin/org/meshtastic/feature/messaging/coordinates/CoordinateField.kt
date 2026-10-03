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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.geo.CoordinateError
import org.meshtastic.core.model.geo.CoordinateFormat
import org.meshtastic.core.model.geo.CoordinateInput
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.coords_error_band
import org.meshtastic.core.resources.coords_error_degrees
import org.meshtastic.core.resources.coords_error_easting
import org.meshtastic.core.resources.coords_error_empty
import org.meshtastic.core.resources.coords_error_minutes_seconds
import org.meshtastic.core.resources.coords_error_northing
import org.meshtastic.core.resources.coords_error_outside_cell
import org.meshtastic.core.resources.coords_error_precision
import org.meshtastic.core.resources.coords_error_square
import org.meshtastic.core.resources.coords_error_syntax
import org.meshtastic.core.resources.coords_error_zone
import org.meshtastic.core.resources.coords_hint
import org.meshtastic.core.resources.coords_input_format
import org.meshtastic.core.resources.coords_input_label
import org.meshtastic.feature.messaging.component.AlertPill

/** A well-formed example of each format, shown as the field's hint and in syntax errors. */
internal fun CoordinateFormat.example(): String = when (this) {
    CoordinateFormat.MGRS -> "31U DQ 48251 11932"
    CoordinateFormat.UTM -> "31U 448251 5411932"
    CoordinateFormat.DMS -> "48°51'24\"N 002°21'03\"E"
}

/** One sentence telling the user what to fix. */
@Composable
internal fun coordinateErrorText(error: CoordinateError, format: CoordinateFormat): String = when (error) {
    CoordinateError.EMPTY -> stringResource(Res.string.coords_error_empty)
    CoordinateError.SYNTAX -> stringResource(Res.string.coords_error_syntax, format.name, format.example())
    CoordinateError.ZONE -> stringResource(Res.string.coords_error_zone)
    CoordinateError.BAND -> stringResource(Res.string.coords_error_band)
    CoordinateError.SQUARE -> stringResource(Res.string.coords_error_square)
    CoordinateError.PRECISION -> stringResource(Res.string.coords_error_precision)
    CoordinateError.EASTING -> stringResource(Res.string.coords_error_easting)
    CoordinateError.NORTHING -> stringResource(Res.string.coords_error_northing)
    CoordinateError.DEGREES -> stringResource(Res.string.coords_error_degrees)
    CoordinateError.MINUTES_SECONDS -> stringResource(Res.string.coords_error_minutes_seconds)
    CoordinateError.OUTSIDE_CELL -> stringResource(Res.string.coords_error_outside_cell)
}

/**
 * A position typed in one of the three formats: format selector, then the text field, then, once something is typed and
 * it does not parse, why. The error uses the high-contrast [AlertPill], since red is reserved for urgent messages.
 */
@Composable
internal fun CoordinateField(
    format: CoordinateFormat,
    text: String,
    input: CoordinateInput,
    onFormatChange: (CoordinateFormat) -> Unit,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(Res.string.coords_input_label),
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val selectorDescription = stringResource(Res.string.coords_input_format)
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = selectorDescription },
        ) {
            CoordinateFormat.entries.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == format,
                    onClick = { onFormatChange(option) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = CoordinateFormat.entries.size),
                    label = { Text(option.name) },
                )
            }
        }
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            placeholder = { Text(stringResource(Res.string.coords_hint, format.example())) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            textStyle = MaterialTheme.typography.bodyLarge,
        )
        if (text.isNotBlank() && input is CoordinateInput.Invalid) {
            AlertPill(text = coordinateErrorText(input.error, format))
        }
    }
}
