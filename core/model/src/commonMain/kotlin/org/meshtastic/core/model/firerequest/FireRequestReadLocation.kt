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

// Reading box 7 back, one line of the sheet at a time: the inverse of FireRequestLocation.kt.

private val COORDINATE = Regex("""^(\d{1,2})([A-Z]) ([A-Z]{2}) (\d+) - (\d+)$""")
private const val SQUARE = 3
private const val EASTING = 4
private const val NORTHING = 5

private val POLAR_LINES = setOf(ImpactLine.A, ImpactLine.B, ImpactLine.C)
private val RECTANGULAR_LINES = setOf(ImpactLine.D, ImpactLine.E, ImpactLine.F)

/** [location] with the line of box 7 written [content] (`G 35T LL 33000 - 90300 · Z 610 m · TLE 2`) read into it. */
internal fun readLocationLine(content: String, location: Location, unread: MutableList<String>): Location {
    // TLE ends the last line, or has one of its own.
    val elements = if (content == Sheet.NOT_FILLED) emptyList() else content.split(Sheet.SEPARATOR)
    val tle = elements.firstNotNullOfOrNull { valueOf(it, Sheet.TLE) }
    val withTle = tle?.let { location.copy(tle = it) } ?: location
    val rest = elements.filter { valueOf(it, Sheet.TLE) == null }
    val first = rest.firstOrNull() ?: return withTle
    val line = ImpactLine.entries.firstOrNull { it.name == first.substringBefore(" ") }
    val body = listOfNotNull(first.substringAfter(" ", "").ifEmpty { null }) + rest.drop(1)
    return when {
        valueOf(first, Sheet.REFERENCE) != null -> withTle.copy(reference = readReference(rest, unread))
        line != null -> withLine(withTle, line, body, unread)
        else -> withTle.also { unread += rest }
    }
}

private fun withLine(location: Location, line: ImpactLine, body: List<String>, unread: MutableList<String>) =
    when (line) {
        in POLAR_LINES -> {
            val polar = readPolar(body, unread)
            when (line) {
                ImpactLine.A -> location.copy(a = polar)
                ImpactLine.B -> location.copy(b = polar)
                else -> location.copy(c = polar)
            }
        }

        in RECTANGULAR_LINES -> {
            val rectangular = readRectangular(body, unread)
            when (line) {
                ImpactLine.D -> location.copy(d = rectangular)
                ImpactLine.E -> location.copy(e = rectangular)
                else -> location.copy(f = rectangular)
            }
        }

        ImpactLine.G -> location.copy(g = readLru(body, unread))

        else -> location.copy(h = readLru(body, unread))
    }

private fun readPolar(body: List<String>, unread: MutableList<String>): PolarLine =
    fold(body, unread, PolarLine()) { line, element ->
        valueOf(element, Sheet.BEARING_LETTER)?.let { line.copy(bearing = it) }
            ?: measureOf(element, Sheet.DISTANCE_LETTER, Sheet.METRES)?.let { line.copy(distance = it) }
            ?: measureOf(element, Sheet.SITE_LETTER, Sheet.METRES)?.let { line.copy(site = it) }
    }

private fun readRectangular(body: List<String>, unread: MutableList<String>): RectangularLine =
    fold(body, unread, RectangularLine()) { line, element ->
        valueOf(element, Sheet.X)?.let { line.copy(x = it) }
            ?: valueOf(element, Sheet.Y)?.let { line.copy(y = it) }
            ?: measureOf(element, Sheet.Z, Sheet.METRES)?.let { line.copy(z = it) }
    }

private fun readLru(body: List<String>, unread: MutableList<String>): LruLine =
    fold(body, unread, LruLine()) { line, element ->
        COORDINATE.find(element)?.groupValues?.let { group ->
            line.copy(zone = group[1], band = group[2], square = group[SQUARE], x = group[EASTING], y = group[NORTHING])
        } ?: measureOf(element, Sheet.Z, Sheet.METRES)?.let { line.copy(z = it) }
    }

/** `Rep R1 R2 · Plus à l'est 200 m · …`: the two free boxes are single words, then the shifts. */
private fun readReference(elements: List<String>, unread: MutableList<String>): ReferenceLine {
    val boxes = valueOf(elements.first(), Sheet.REFERENCE).orEmpty().split(" ").filter { it.isNotEmpty() }
    val start = ReferenceLine(first = boxes.getOrElse(0) { "" }, second = boxes.getOrElse(1) { "" })
    if (boxes.size > 2) unread += elements.first()
    return fold(elements.drop(1), unread, start) { line, element ->
        shiftOf(element)?.let { (shift, metres) -> withShift(line, shift, metres) }
    }
}

private fun withShift(line: ReferenceLine, shift: Shift, metres: String): ReferenceLine = when (shift) {
    Shift.EAST,
    Shift.WEST,
    -> line.copy(eastWest = shift, eastWestM = metres)

    Shift.NORTH,
    Shift.SOUTH,
    -> line.copy(northSouth = shift, northSouthM = metres)

    Shift.RIGHT,
    Shift.LEFT,
    -> line.copy(rightLeft = shift, rightLeftM = metres)

    Shift.NEARER,
    Shift.FURTHER,
    -> line.copy(nearFar = shift, nearFarM = metres)

    Shift.HIGHER,
    Shift.LOWER,
    -> line.copy(upDown = shift, upDownM = metres)
}
