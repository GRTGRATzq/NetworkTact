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

import org.meshtastic.core.model.geo.FormattedCoordinates
import org.meshtastic.core.model.geo.MapPointCoordinate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * « Coller une coordonnée » (box 7, lines G and H) on what the clipboard holds after a copy on the map or in the
 * converter: the real [MapPointCoordinate.messageText] of points of the Cincu training area, the area the maps of this
 * fork are downloaded for (`GeoBounds(24.4, 45.6, 25.2, 46.2)`: west, south, east, north).
 */
class LruCoordinateClipboardTest {

    private companion object {
        const val WEST = 24.4
        const val SOUTH = 45.6
        const val EAST = 25.2
        const val NORTH = 46.2
        const val STEPS = 8

        /** Off round values, like a real long press, and still inside the area at its edges. */
        const val MARGIN = 0.0013

        // By code: these characters cannot be told apart from a space in a source file.
        val NBSP = 0x00A0.toChar().toString()
        val NARROW_NBSP = 0x202F.toChar().toString()
    }

    /** A grid of 9 × 9 points across the area, from edge to edge. */
    private val cincu =
        (0..STEPS).flatMap { i ->
            (0..STEPS).map { j ->
                val latitude = SOUTH + MARGIN + i * (NORTH - SOUTH - 2 * MARGIN) / STEPS
                val longitude = WEST + MARGIN + j * (EAST - WEST - 2 * MARGIN) / STEPS
                assertNotNull(MapPointCoordinate.of(latitude, longitude))
            }
        }

    /** The line the point's MGRS stands for, Z left to the caller. */
    private fun expected(point: MapPointCoordinate, z: String = ""): LruLine {
        val (zoneBand, square, x, y) = assertNotNull(point.formatted.mgrs).split(" ")
        return LruLine(zoneBand.dropLast(1), zoneBand.last().toString(), square, x, y, z)
    }

    @Test
    fun `the text copied by a long press anywhere in Cincu fills the line and keeps its altitude`() {
        val line = LruLine(z = "512")
        for (point in cincu) {
            assertEquals(
                expected(point, z = "512"),
                LruCoordinate.fromClipboard(line, point.messageText),
                point.messageText,
            )
        }
    }

    @Test
    fun `the Cincu area is in zone 35T`() {
        for (point in cincu) assertEquals("35T", assertNotNull(point.formatted.mgrs).substringBefore(" "))
    }

    @Test
    fun `the same text with no-break spaces in place of its spaces still fills the line`() {
        val line = LruLine(zone = "34", band = "U", square = "AA", x = "1", y = "2", z = "480")
        for (point in cincu) {
            for (space in listOf(NBSP, NARROW_NBSP)) {
                val text = point.messageText.replace(" ", space)
                assertEquals(expected(point, z = "480"), LruCoordinate.fromClipboard(line, text), text)
            }
        }
    }

    @Test
    fun `the converter's MGRS alone, as Copy and a selection put it, fills the line`() {
        for (point in cincu) {
            val mgrs = assertNotNull(FormattedCoordinates.of(point.point).mgrs)
            assertEquals(expected(point), LruCoordinate.fromClipboard(LruLine(), mgrs), mgrs)
        }
    }

    @Test
    fun `an empty clipboard or one without a complete coordinate changes nothing`() {
        val line = LruLine(zone = "35", z = "300")
        assertNull(LruCoordinate.fromClipboard(line, null))
        assertNull(LruCoordinate.fromClipboard(line, ""))
        assertNull(LruCoordinate.fromClipboard(line, "Point carte"))
        assertNull(LruCoordinate.fromClipboard(line, "Point carte · MGRS 35T LL 3300 9030"))
    }
}
