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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.firerequest.ImpactLine
import org.meshtastic.core.model.firerequest.Location
import org.meshtastic.core.model.firerequest.LruCoordinate
import org.meshtastic.core.model.firerequest.LruLine
import org.meshtastic.core.model.firerequest.PolarLine
import org.meshtastic.core.model.firerequest.RectangularLine
import org.meshtastic.core.model.firerequest.ReferenceLine
import org.meshtastic.core.model.firerequest.Shift
import org.meshtastic.core.model.geo.CoordinateError
import org.meshtastic.core.model.geo.CoordinateFormat
import org.meshtastic.core.model.geo.CoordinateInput
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.firereq_lru_help
import org.meshtastic.core.resources.firereq_paste_help
import org.meshtastic.core.resources.firereq_paste_into
import org.meshtastic.core.resources.firereq_paste_none
import org.meshtastic.core.resources.firereq_rep_direction
import org.meshtastic.feature.messaging.coordinates.coordinateErrorText
import org.meshtastic.core.model.firerequest.FireRequestSheet as Sheet

// Box 7 of the form: one block per line of the sheet.

@Composable
internal fun LocationBox(box: Location, edit: (Location) -> Unit) {
    BoxCard(BOX_LOCATION) {
        Help(stringResource(Res.string.firereq_paste_help))
        PasteField(ImpactLine.G) { edit(box.copy(g = it.copy(z = box.g.z))) }
        PasteField(ImpactLine.H) { edit(box.copy(h = it.copy(z = box.h.z))) }
        Text(Sheet.POLAR, style = MaterialTheme.typography.labelLarge)
        Polar(ImpactLine.A, box.a) { edit(box.copy(a = it)) }
        Polar(ImpactLine.B, box.b) { edit(box.copy(b = it)) }
        Polar(ImpactLine.C, box.c) { edit(box.copy(c = it)) }
        Text(Sheet.RECTANGULAR, style = MaterialTheme.typography.labelLarge)
        Rectangular(ImpactLine.D, box.d) { edit(box.copy(d = it)) }
        Rectangular(ImpactLine.E, box.e) { edit(box.copy(e = it)) }
        Rectangular(ImpactLine.F, box.f) { edit(box.copy(f = it)) }
        Reference(box.reference) { edit(box.copy(reference = it)) }
        Text(Sheet.LRU, style = MaterialTheme.typography.labelLarge)
        Lru(ImpactLine.G, box.g) { edit(box.copy(g = it)) }
        Lru(ImpactLine.H, box.h) { edit(box.copy(h = it)) }
        Help(stringResource(Res.string.firereq_lru_help))
        // The sheet gives no legend for the TLE codes: they are shown alone.
        Text(Sheet.TLE, style = MaterialTheme.typography.labelLarge)
        Choices {
            Sheet.TLE_CODES.forEach { code ->
                Choice(code, box.tle == code) { edit(box.copy(tle = if (box.tle == code) "" else code)) }
            }
        }
    }
}

/** A field to paste a copied coordinate into: once a complete one reads, it fills line [line] and empties itself. */
@Composable
private fun PasteField(line: ImpactLine, onCoordinate: (LruLine) -> Unit) {
    var text by remember { mutableStateOf("") }
    Column {
        OutlinedTextField(
            value = text,
            onValueChange = { pasted ->
                val coordinate = LruCoordinate.fromText(pasted)
                if (coordinate == null) {
                    text = pasted
                } else {
                    text = ""
                    onCoordinate(coordinate)
                }
            },
            label = { Text(stringResource(Res.string.firereq_paste_into, line.name)) },
            singleLine = true,
            isError = text.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        )
        if (text.isNotBlank()) Help(stringResource(Res.string.firereq_paste_none), isError = true)
    }
}

@Composable
private fun Polar(letter: ImpactLine, line: PolarLine, edit: (PolarLine) -> Unit) {
    Choices {
        Text(letter.name)
        NumberField(Sheet.BEARING_LETTER, line.bearing, { edit(line.copy(bearing = it)) }, unit = MILS, width = 88.dp)
        NumberField(
            Sheet.DISTANCE_LETTER,
            line.distance,
            { edit(line.copy(distance = it)) },
            unit = Sheet.METRES,
            width = 88.dp,
        )
        NumberField(Sheet.SITE_LETTER, line.site, { edit(line.copy(site = it)) }, unit = Sheet.METRES, width = 88.dp)
    }
}

@Composable
private fun Rectangular(letter: ImpactLine, line: RectangularLine, edit: (RectangularLine) -> Unit) {
    Choices {
        Text(letter.name)
        NumberField(Sheet.X, line.x, { edit(line.copy(x = it)) }, width = 104.dp)
        NumberField(Sheet.Y, line.y, { edit(line.copy(y = it)) }, width = 104.dp)
        NumberField(Sheet.Z, line.z, { edit(line.copy(z = it)) }, unit = Sheet.METRES, width = 88.dp)
    }
}

@Composable
private fun Reference(line: ReferenceLine, edit: (ReferenceLine) -> Unit) {
    Column {
        Text(Sheet.REFERENCE, style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WordField("", line.first, { edit(line.copy(first = it)) })
            WordField("", line.second, { edit(line.copy(second = it)) })
        }
        ShiftRow(Shift.EAST, Shift.WEST, line.eastWest, line.eastWestM) { d, m ->
            edit(line.copy(eastWest = d, eastWestM = m))
        }
        ShiftRow(Shift.NORTH, Shift.SOUTH, line.northSouth, line.northSouthM) { d, m ->
            edit(line.copy(northSouth = d, northSouthM = m))
        }
        Text(Sheet.FOR_ME)
        ShiftRow(Shift.RIGHT, Shift.LEFT, line.rightLeft, line.rightLeftM) { d, m ->
            edit(line.copy(rightLeft = d, rightLeftM = m))
        }
        ShiftRow(Shift.NEARER, Shift.FURTHER, line.nearFar, line.nearFarM) { d, m ->
            edit(line.copy(nearFar = d, nearFarM = m))
        }
        ShiftRow(Shift.HIGHER, Shift.LOWER, line.upDown, line.upDownM) { d, m ->
            edit(line.copy(upDown = d, upDownM = m))
        }
    }
}

/** One shift of the Rep line: its two directions, then the metres; metres without a direction are flagged. */
@Composable
private fun ShiftRow(
    first: Shift,
    second: Shift,
    direction: Shift?,
    metres: String,
    onChange: (Shift?, String) -> Unit,
) {
    Column {
        Choices {
            listOf(first, second).forEach { shift ->
                Choice(shift.label, direction == shift) { onChange(if (direction == shift) null else shift, metres) }
            }
            NumberField("", metres, { onChange(direction, it) }, unit = Sheet.METRES, width = 88.dp)
        }
        if (direction == null && metres.isNotBlank()) {
            Help(stringResource(Res.string.firereq_rep_direction), isError = true)
        }
    }
}

@Composable
private fun Lru(letter: ImpactLine, line: LruLine, edit: (LruLine) -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(letter.name)
            WordField(Sheet.UTM_ZONE, line.zone, { edit(line.copy(zone = it)) }, width = 72.dp)
            WordField(Sheet.LATITUDE_BAND, line.band, { edit(line.copy(band = it.uppercase())) }, width = 72.dp)
            WordField(Sheet.SQUARE, line.square, { edit(line.copy(square = it.uppercase())) }, width = 72.dp)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            NumberField(Sheet.X, line.x, { edit(line.copy(x = it)) }, width = 104.dp)
            Text("-")
            NumberField(Sheet.Y, line.y, { edit(line.copy(y = it)) }, width = 104.dp)
        }
        NumberField(Sheet.Z, line.z, { edit(line.copy(z = it)) }, unit = Sheet.METRES, width = 88.dp)
        val checked = LruCoordinate.check(line)
        if (checked is CoordinateInput.Invalid && checked.error != CoordinateError.EMPTY) {
            Help(coordinateErrorText(checked.error, CoordinateFormat.MGRS), isError = true)
        }
    }
}

/** The unit of bearings: mils, written « m » barred as on the sheet. */
internal const val MILS = "m̶"
