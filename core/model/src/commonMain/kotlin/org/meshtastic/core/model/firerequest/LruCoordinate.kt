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
package org.meshtastic.core.model.firerequest

import org.meshtastic.core.model.geo.CoordinateError
import org.meshtastic.core.model.geo.CoordinateFormat
import org.meshtastic.core.model.geo.CoordinateInput
import org.meshtastic.core.model.geo.CoordinateParser
import org.meshtastic.core.model.geo.FormattedCoordinates
import org.meshtastic.core.model.geo.LatLon
import org.meshtastic.core.model.geo.MessageCoordinate

/**
 * The coordinate of an L R U line of box 7 (`35T LL 33000 - 90300`), checked by the coordinate converter: UTM zone,
 * latitude band and 100 km square that exist, X and Y on five digits each (1 m). Only these lines are read as a
 * position: no other text of a request, and no other message, is ever taken for one here.
 */
object LruCoordinate {

    /** The line's coordinate checked as MGRS; [CoordinateError.EMPTY] when none of its pieces is filled in. */
    fun check(line: LruLine): CoordinateInput {
        val pieces = listOf(line.zone, line.band, line.square, line.x, line.y).map { token(it).uppercase() }
        val x = pieces[EASTING]
        val y = pieces[NORTHING]
        return when {
            pieces.all { it.isEmpty() } -> CoordinateInput.Invalid(CoordinateError.EMPTY)

            // The converter takes the ten digits together: X and Y must have five each.
            x.length != DIGITS || y.length != DIGITS ->
                CoordinateInput.Invalid(
                    if ((x + y).all { it.isDigit() }) CoordinateError.PRECISION else CoordinateError.SYNTAX,
                )

            else ->
                CoordinateParser.parse(
                    pieces.take(2).joinToString("") + " " + pieces.drop(2).joinToString(" "),
                    CoordinateFormat.MGRS,
                )
        }
    }

    /** The position of the line, or null when its coordinate is incomplete or does not exist. */
    fun point(line: LruLine): LatLon? = (check(line) as? CoordinateInput.Valid)?.point

    /**
     * The coordinate of a pasted text (the « Point carte · MGRS … » copied from the map, a converter result, or any
     * message holding a complete coordinate), as the pieces of an L R U line; null when it holds none. Z is not set: no
     * copied text carries an altitude.
     */
    fun fromText(text: String): LruLine? = MessageCoordinate.find(text)
        ?.let { FormattedCoordinates.of(it.point).mgrs }
        ?.let { MGRS.find(it) }
        ?.groupValues
        ?.drop(1)
        ?.let { LruLine(zone = it[0], band = it[1], square = it[2], x = it[EASTING], y = it[NORTHING]) }

    private const val DIGITS = 5
    private const val EASTING = 3
    private const val NORTHING = 4

    /** `35T LL 33000 90300`: zone, band, square, X, Y; X and Y at the same indices as in an L R U line's pieces. */
    private val MGRS = Regex("""^(\d{1,2})([A-Z]) ([A-Z]{2}) (\d{5}) (\d{5})$""")
}
