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
package org.meshtastic.core.model.geo

import org.meshtastic.proto.Waypoint
import kotlin.math.roundToInt

/** Waypoint coordinates travel as degrees scaled by 1e7. */
private const val DEGREES_TO_E7 = 1e7

/** The existing Meshtastic waypoint (WAYPOINT_APP) for this point: nothing else is added to the radio protocol. */
fun SharedPoint.toWaypoint(): Waypoint = Waypoint.Builder()
    .also { wb ->
        wb.id = id
        wb.latitude_i = (point.latitude * DEGREES_TO_E7).roundToInt()
        wb.longitude_i = (point.longitude * DEGREES_TO_E7).roundToInt()
        wb.expire = expireEpochSeconds.toInt()
        wb.locked_to = lockedTo
        wb.name = name
        wb.description = description
        wb.icon = icon
    }
    .build()
