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

import org.meshtastic.core.model.firerequest.FireRequestSheet as Sheet

/** The lines of box 7, without the box number; TLE ends the last one, or has a line of its own. */
internal fun location(box: Location): List<String> {
    val lines =
        listOfNotNull(
            polar(ImpactLine.A, box.a),
            polar(ImpactLine.B, box.b),
            polar(ImpactLine.C, box.c),
            rectangular(ImpactLine.D, box.d),
            rectangular(ImpactLine.E, box.e),
            rectangular(ImpactLine.F, box.f),
            reference(box.reference),
            lru(ImpactLine.G, box.g),
            lru(ImpactLine.H, box.h),
        )
    val tle = labelled(Sheet.TLE, box.tle) ?: return lines
    return if (lines.isEmpty()) listOf(tle) else lines.dropLast(1) + (lines.last() + Sheet.SEPARATOR + tle)
}

internal fun polar(letter: ImpactLine, line: PolarLine): String? = lineOf(
    letter.name,
    listOfNotNull(
        labelled(Sheet.BEARING_LETTER, line.bearing),
        labelled(Sheet.DISTANCE_LETTER, line.distance, Sheet.METRES),
        labelled(Sheet.SITE_LETTER, line.site, Sheet.METRES),
    ),
)

internal fun rectangular(letter: ImpactLine, line: RectangularLine): String? = lineOf(
    letter.name,
    listOfNotNull(labelled(Sheet.X, line.x), labelled(Sheet.Y, line.y), labelled(Sheet.Z, line.z, Sheet.METRES)),
)

internal fun reference(line: ReferenceLine): String? {
    val shifts =
        listOfNotNull(
            shift(line.eastWest, line.eastWestM),
            shift(line.northSouth, line.northSouthM),
            shift(line.rightLeft, line.rightLeftM),
            shift(line.nearFar, line.nearFarM),
            shift(line.upDown, line.upDownM),
        )
    val head = listOf(Sheet.REFERENCE, cleanText(line.first), cleanText(line.second)).filter { it.isNotEmpty() }
    if (head.size == 1 && shifts.isEmpty()) return null
    return (listOf(head.joinToString(" ")) + shifts).joinToString(Sheet.SEPARATOR)
}

/** `G 35T LL 33000 - 90300 · Z 610 m`: the MGRS coordinate, then Z. */
internal fun lru(letter: ImpactLine, line: LruLine): String? {
    val head = listOf(cleanText(line.zone) + cleanText(line.band), cleanText(line.square), cleanText(line.x))
    val northing = cleanText(line.y)
    val coordinate =
        (head.filter { it.isNotEmpty() }.joinToString(" ") + if (northing.isEmpty()) "" else " - $northing")
            .trim()
            .ifEmpty { null }
    return lineOf(letter.name, listOfNotNull(coordinate, labelled(Sheet.Z, line.z, Sheet.METRES)))
}
