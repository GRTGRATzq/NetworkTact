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

/**
 * Finds a position written in a message: `[POS]`, `[CR] FAIT OBSERVÉ`, or any text with a coordinate pasted from the
 * converter. Only complete coordinates count, each checked by [CoordinateParser]:
 * - MGRS with a grid zone that exists, its 100 km square and 5 + 5 digits;
 * - UTM with a grid zone that exists, an easting of six digits and a northing;
 * - DMS with degrees, minutes, seconds and a hemisphere on both axes.
 *
 * So a phone number, a time, a date or a reference number is never taken for a position. MGRS is looked for first, then
 * UTM, then DMS: the messages of this fork give MGRS, to 1 m, alongside the other formats.
 */
object MessageCoordinate {

    /** A position found in a message: [point], and the [format] and [text] it was written in. */
    data class Found(val point: LatLon, val format: CoordinateFormat, val text: String)

    private const val NOT_AFTER = """(?<![A-Z0-9])"""
    private const val NOT_BEFORE_DIGIT = """(?![0-9])"""
    private const val DMS_AXIS = """\d{1,3}(?: ?° ?| )\d{1,2}(?: ?['′] ?| )\d{1,2}(?:[.,]\d+)? ?(?:"|″|'')? ?"""

    private val candidates =
        listOf(
            CoordinateFormat.MGRS to Regex("""$NOT_AFTER\d{1,2} ?[A-Z] ?[A-Z]{2} ?\d{5} ?\d{5}$NOT_BEFORE_DIGIT"""),
            CoordinateFormat.UTM to
                Regex("""$NOT_AFTER\d{1,2} ?[A-Z] \d{6}(?:[.,]\d+)? \d{1,8}(?:[.,]\d+)?$NOT_BEFORE_DIGIT"""),
            CoordinateFormat.DMS to Regex("""$NOT_AFTER$DMS_AXIS[NS][ ,;]+$DMS_AXIS[EWO](?![A-Z])"""),
        )

    private val WHITESPACE = Regex("""\s+""")

    /** The first complete position in [text], or null when it holds none. */
    fun find(text: String): Found? {
        val normalized = text.uppercase().replace(WHITESPACE, " ")
        for ((format, pattern) in candidates) {
            for (match in pattern.findAll(normalized)) {
                val input = CoordinateParser.parse(match.value, format)
                if (input is CoordinateInput.Valid) return Found(input.point, format, match.value.trim())
            }
        }
        return null
    }
}
