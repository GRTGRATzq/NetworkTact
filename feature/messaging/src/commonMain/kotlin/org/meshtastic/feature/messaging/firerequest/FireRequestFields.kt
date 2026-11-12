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
package org.meshtastic.feature.messaging.firerequest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.firerequest.FireRequestSheet
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.firereq_no_space

// The numbers of the boxes of the sheet.
internal const val BOX_EFFECT = 3
internal const val BOX_OBSERVATION = 4
internal const val BOX_CHARGE = 5
internal const val BOX_TARGETS = 6
internal const val BOX_LOCATION = 7
internal const val BOX_SHAPE = 8
internal const val BOX_ADJUSTMENT = 9
internal const val BOX_FRIENDS = 10
internal const val BOX_COMMAND = 11

/** One box of the sheet: its number in a disc, its title as on the sheet, then its fields. */
@Composable
internal fun BoxCard(number: Int, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                ) {
                    Text(
                        text = number.toString(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(top = 3.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
                Text(FireRequestSheet.BOX_TITLES[number - 1], style = MaterialTheme.typography.titleSmall)
            }
            content()
        }
    }
}

/** A choice of the sheet, ticked or not. */
@Composable
internal fun Choice(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

/** A row of choices that wraps on narrow screens. */
@Composable
internal fun Choices(content: @Composable () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        content()
    }
}

/**
 * A box of the sheet: [value] as typed, passed through [filter] before [onChange]. A [prefilled] box has the green
 * background of what the app filled in. [unit] follows the field as on the sheet.
 */
@Composable
internal fun SheetField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 96.dp,
    numeric: Boolean = false,
    unit: String? = null,
    filter: (String) -> String = ::freeInput,
    prefilled: Boolean = false,
    isError: Boolean = false,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { onChange(filter(it)) },
            label = { Text(label) },
            singleLine = true,
            isError = isError,
            keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text),
            colors =
            if (prefilled) {
                OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    focusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                )
            } else {
                OutlinedTextFieldDefaults.colors()
            },
            modifier = Modifier.width(width),
        )
        if (unit != null) Text(unit)
    }
}

/** A number box: digits only, on the number keyboard. */
@Composable
internal fun NumberField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    unit: String? = null,
    width: Dp = 96.dp,
) {
    SheetField(label, value, onChange, width = width, numeric = true, unit = unit, filter = ::digitsInput)
}

/** A box sent as one word: the operator is told, under it, what will go without its spaces. */
@Composable
internal fun WordField(label: String, value: String, onChange: (String) -> Unit, width: Dp = 96.dp) {
    Column {
        SheetField(label, value, onChange, width = width)
        withoutSpaces(value)?.let { Help(stringResource(Res.string.firereq_no_space, it), isError = true) }
    }
}

/** A line of help or warning under a field. */
@Composable
internal fun Help(text: String, isError: Boolean = false) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** A choice holding a value (`Mettre en place [ ]`): ticked when [value] is not null, with its box once ticked. */
@Composable
internal fun ChoiceWithValue(label: String, value: String?, numeric: Boolean = false, onChange: (String?) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Choice(label, value != null) { onChange(if (value == null) "" else null) }
        if (value != null) {
            SheetField(label = "", value = value, onChange = { onChange(it) }, numeric = numeric, width = 80.dp)
        }
    }
}

/**
 * One code among [codes] (`1`, `2`…), each shown with its legend from the sheet (`2 : Mo 120`), or none; a second tap
 * on the chosen code clears it.
 */
@Composable
internal fun CodeChoice(label: String, codes: Map<String, String>, value: String, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Choices {
            codes.forEach { (code, legend) ->
                Choice("$code : $legend", value == code) { onChange(if (value == code) "" else code) }
            }
        }
    }
}
