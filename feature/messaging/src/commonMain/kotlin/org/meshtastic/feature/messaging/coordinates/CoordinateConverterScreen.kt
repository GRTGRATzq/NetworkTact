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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.geo.CoordinateFormat
import org.meshtastic.core.model.geo.CoordinateInput
import org.meshtastic.core.model.geo.CoordinateParser
import org.meshtastic.core.model.geo.FormattedCoordinates
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.coords_converter_intro
import org.meshtastic.core.resources.coords_converter_limits
import org.meshtastic.core.resources.coords_converter_title
import org.meshtastic.core.resources.coords_not_covered
import org.meshtastic.core.ui.component.MainAppBar
import org.meshtastic.core.ui.theme.AppTheme

/** Offline converter: a position typed in MGRS, UTM or DMS, shown in the other two formats. */
@Composable
fun CoordinateConverterScreen(onNavigateUp: () -> Unit, modifier: Modifier = Modifier, input: String = "") {
    Scaffold(
        modifier = modifier,
        topBar = {
            MainAppBar(
                title = stringResource(Res.string.coords_converter_title),
                ourNode = null,
                showNodeChip = false,
                canNavigateUp = true,
                onNavigateUp = onNavigateUp,
                actions = {},
                onClickChip = {},
            )
        },
    ) { innerPadding ->
        CoordinateConverterContent(modifier = Modifier.fillMaxSize().padding(innerPadding), initialInput = input)
    }
}

@Composable
internal fun CoordinateConverterContent(modifier: Modifier = Modifier, initialInput: String = "") {
    // Saved by name: an enum is not saveable on every platform.
    var formatName by rememberSaveable { mutableStateOf(initialFormat(initialInput).name) }
    val format = CoordinateFormat.valueOf(formatName)
    var text by rememberSaveable { mutableStateOf(initialInput) }
    val input = remember(text, format) { CoordinateParser.parse(text, format) }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(Res.string.coords_converter_intro), style = MaterialTheme.typography.bodyMedium)
        CoordinateField(
            format = format,
            text = text,
            input = input,
            onFormatChange = { formatName = it.name },
            onTextChange = { text = it },
        )
        if (input is CoordinateInput.Valid) {
            val formatted = remember(input) { FormattedCoordinates.of(input.point) }
            ConvertedCoordinates(formatted = formatted, typedAs = format)
        }
        Text(
            stringResource(Res.string.coords_converter_limits),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** The format [input] is written in, MGRS when it is empty or matches none, so the field opens on the right tab. */
internal fun initialFormat(input: String): CoordinateFormat =
    CoordinateFormat.entries.firstOrNull { CoordinateParser.parse(input, it) is CoordinateInput.Valid }
        ?: CoordinateFormat.MGRS

/** The two formats the user did not type, selectable so they can be copied. */
@Composable
private fun ConvertedCoordinates(formatted: FormattedCoordinates, typedAs: CoordinateFormat) {
    val notCovered = stringResource(Res.string.coords_not_covered)
    Card(modifier = Modifier.fillMaxWidth()) {
        SelectionContainer {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CoordinateFormat.entries
                    .filter { it != typedAs }
                    .forEach { other ->
                        Column {
                            Text(
                                other.name,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                formatted.format(other) ?: notCovered,
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun CoordinateConverterContentPreview() {
    AppTheme { CoordinateConverterContent() }
}
