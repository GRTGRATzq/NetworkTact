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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import org.meshtastic.core.model.firerequest.AdjustMethod
import org.meshtastic.core.model.firerequest.Adjustment
import org.meshtastic.core.model.firerequest.Command
import org.meshtastic.core.model.firerequest.Friends
import org.meshtastic.core.model.firerequest.Openness
import org.meshtastic.core.model.firerequest.Shape
import org.meshtastic.core.model.firerequest.ShapeKind
import org.meshtastic.core.model.firerequest.Side
import org.meshtastic.core.model.firerequest.FireRequestSheet as Sheet

// Boxes 8 to 11 of the form, in the order and with the words of the sheet.

@Composable
internal fun ShapeBox(box: Shape, edit: (Shape) -> Unit) {
    BoxCard(BOX_SHAPE) {
        Choices {
            ShapeKind.entries.forEach { kind ->
                Choice(kind.label, box.kind == kind) { edit(box.copy(kind = if (box.kind == kind) null else kind)) }
            }
        }
        SheetField(Sheet.CENTRAL_POINT, box.centralPoint, { edit(box.copy(centralPoint = it)) }, width = 180.dp)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Side.entries.forEach { side ->
                Choice(side.label, box.side == side) { edit(box.copy(side = if (box.side == side) null else side)) }
            }
            // LG and LD have no unit, as on the sheet.
            NumberField("", box.sideValue, { edit(box.copy(sideValue = it)) })
        }
        NumberField(Sheet.BEARING, box.bearing, { edit(box.copy(bearing = it)) }, unit = MILS)
        NumberField(Sheet.FRONT, box.front, { edit(box.copy(front = it)) }, unit = Sheet.METRES)
        NumberField(Sheet.DEPTH, box.depth, { edit(box.copy(depth = it)) }, unit = Sheet.METRES)
        Choices {
            Openness.entries.forEach { openness ->
                Choice(openness.label, box.openness == openness) {
                    edit(box.copy(openness = if (box.openness == openness) null else openness))
                }
            }
        }
        SheetField(Sheet.CENTRE, box.centre, { edit(box.copy(centre = it)) }, width = 180.dp)
        NumberField(Sheet.RADIUS, box.radius, { edit(box.copy(radius = it)) }, unit = Sheet.METRES)
    }
}

@Composable
internal fun AdjustmentBox(box: Adjustment, edit: (Adjustment) -> Unit) {
    BoxCard(BOX_ADJUSTMENT) {
        Choices {
            AdjustMethod.entries.forEach { method ->
                Choice(method.label, box.method == method) {
                    edit(box.copy(method = if (box.method == method) null else method))
                }
            }
        }
        NumberField(
            Sheet.OBSERVATION_BEARING,
            box.observationBearing,
            { edit(box.copy(observationBearing = it)) },
            unit = MILS,
        )
        Text(Sheet.OBSERVER)
        // Typed by the operator, never filled in.
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            NumberField(Sheet.X, box.observerX, { edit(box.copy(observerX = it)) }, width = 104.dp)
            NumberField(Sheet.Y, box.observerY, { edit(box.copy(observerY = it)) }, width = 104.dp)
        }
        NumberField(Sheet.Z, box.observerZ, { edit(box.copy(observerZ = it)) })
    }
}

@Composable
internal fun FriendsBox(box: Friends, edit: (Friends) -> Unit) {
    BoxCard(BOX_FRIENDS) {
        NumberField(Sheet.DISTANCE, box.distance, { edit(box.copy(distance = it)) }, unit = Sheet.METRES)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(
                Sheet.BEARING,
                box.bearing.takeIf { it !in Sheet.CARDINAL_POINTS }.orEmpty(),
                { edit(box.copy(bearing = it)) },
                unit = MILS,
            )
            Text(Sheet.OR)
        }
        Choices {
            Sheet.CARDINAL_POINTS.forEach { point ->
                Choice(point, box.bearing == point) {
                    edit(box.copy(bearing = if (box.bearing == point) "" else point))
                }
            }
        }
        CodeChoice(Sheet.PROTECTION, Sheet.PROTECTION_CODES, box.protection) { edit(box.copy(protection = it)) }
    }
}

@Composable
internal fun CommandBox(box: Command, edit: (Command) -> Unit) {
    BoxCard(BOX_COMMAND) {
        Choices {
            Choice(Sheet.AT_MY_COMMAND, box.atMyCommand) { edit(box.copy(atMyCommand = !box.atMyCommand)) }
            Choice(Sheet.WHEN_READY, box.whenReady) { edit(box.copy(whenReady = !box.whenReady)) }
        }
        TimeChoice(Sheet.WHEN_READY_NLT, box.whenReadyNlt) { edit(box.copy(whenReadyNlt = it)) }
        TimeChoice(Sheet.SYNCHRO, box.synchro) { edit(box.copy(synchro = it)) }
        TimeChoice(Sheet.TIME_ON_TARGET, box.timeOnTarget) { edit(box.copy(timeOnTarget = it)) }
        Choice(Sheet.TIME_ON_TARGET_LIMITED, box.limitedStart != null) {
            edit(
                if (box.limitedStart == null) {
                    box.copy(limitedStart = "")
                } else {
                    box.copy(limitedStart = null, limitedEnd = "")
                },
            )
        }
        box.limitedStart?.let { start ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WordField(Sheet.START, start, { edit(box.copy(limitedStart = it.uppercase())) })
                WordField(Sheet.END, box.limitedEnd, { edit(box.copy(limitedEnd = it.uppercase())) })
            }
        }
        TimeChoice(Sheet.FIRE_TIME, box.fireTime) { edit(box.copy(fireTime = it)) }
        Choice(Sheet.AT_COMMAND_OF, box.atCommandOf != null) {
            edit(box.copy(atCommandOf = if (box.atCommandOf == null) "" else null))
        }
        box.atCommandOf?.let { value ->
            SheetField(Sheet.AT_COMMAND_OF, value, { edit(box.copy(atCommandOf = it)) }, width = 200.dp)
        }
    }
}

/** A choice with a time `..H..` (or the Synchro box): ticked when it holds a value, typed as one word. */
@Composable
private fun TimeChoice(label: String, value: String?, onChange: (String?) -> Unit) {
    Column {
        Choice(label, value != null) { onChange(if (value == null) "" else null) }
        value?.let { WordField(label, it, { typed -> onChange(typed.uppercase()) }) }
    }
}
