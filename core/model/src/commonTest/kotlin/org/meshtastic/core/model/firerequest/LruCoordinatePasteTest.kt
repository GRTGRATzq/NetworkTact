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

import org.meshtastic.core.model.geo.CoordinateFormat
import org.meshtastic.core.model.geo.FormattedCoordinates
import org.meshtastic.core.model.geo.LatLon
import org.meshtastic.core.model.geo.MapPointCoordinate
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * D13: « Coller une coordonnée » (box 7, lines G and H) on the texts the app itself produces, not on hand-written
 * examples: the long press on the map ([MapPointCoordinate.messageText]) and the converter's selectable results
 * ([FormattedCoordinates.format]), over points all around the world, then with what a phone's clipboard or keyboard may
 * do to such a text.
 */
class LruCoordinatePasteTest {

    private companion object {
        /** About 100 m of latitude. */
        const val NEAR_DEGREES = 0.001

        // By code: these characters cannot be told apart from a space, or seen at all, in a source file.
        val NBSP = 0x00A0.toChar().toString()
        val NARROW = 0x202F.toChar().toString()
        val THIN = 0x2009.toChar().toString()
        val ZERO_WIDTH = 0x200B.toChar().toString()
        val BOM = 0xFEFF.toChar().toString()

        val EXPECTED = LruLine("35", "T", "LL", "33000", "90300")
    }

    private val points =
        listOf(
            LatLon(45.065, 26.147), // 35T LL, the sheet's example
            LatLon(48.856667, 2.350833),
            LatLon(-45.6456, 23.3545),
            LatLon(0.0001, -179.99),
            LatLon(-33.8688, 151.2093),
            LatLon(64.1466, -21.9426),
            LatLon(1.2966, 103.7764),
            LatLon(-79.5, 0.5),
            LatLon(83.9, 100.0),
        ) + (-80..80 step 10).flatMap { lat -> (-180..170 step 15).map { lon -> LatLon(lat + 0.37, lon + 0.61) } }

    private fun mapText(point: LatLon) = MapPointCoordinate.of(point.latitude, point.longitude)!!.messageText

    private fun lineOf(point: LatLon): LruLine {
        val mgrs = assertNotNull(FormattedCoordinates.of(point).mgrs)
        val (zoneBand, square, x, y) = mgrs.split(" ")
        return LruLine(zone = zoneBand.dropLast(1), band = zoneBand.last().toString(), square = square, x = x, y = y)
    }

    @Test
    fun `the text copied by a long press on the map fills the line, all around the world`() {
        for (point in points) {
            assertEquals(lineOf(point), LruCoordinate.fromText(mapText(point)), mapText(point))
        }
    }

    @Test
    fun `every format the converter shows fills the line, selected alone or with its label`() {
        for (point in points) {
            val formatted = FormattedCoordinates.of(point)
            for (format in CoordinateFormat.entries) {
                val value = formatted.format(format) ?: continue
                for (text in listOf(value, "${format.name}\n$value")) {
                    val line = assertNotNull(LruCoordinate.fromText(text), text)
                    if (format == CoordinateFormat.MGRS) {
                        assertEquals(lineOf(point), line, text)
                    } else {
                        // UTM is rounded to the metre and DMS to the second, where MGRS is cut: the line read back is
                        // the same place to within a few tens of metres, not the same digits.
                        val read = assertNotNull(LruCoordinate.point(line), text)
                        assertTrue(abs(read.latitude - point.latitude) < NEAR_DEGREES, text)
                        assertTrue(abs(read.longitude - point.longitude) < NEAR_DEGREES * 2, text)
                    }
                }
            }
        }
    }

    @Test
    fun `the whole converter selection, three formats one under the other, fills the line`() {
        val point = LatLon(45.065, 26.147)
        val formatted = FormattedCoordinates.of(point)
        val selection = CoordinateFormat.entries.joinToString("\n") { "${it.name}\n${formatted.format(it)}" }

        assertEquals(lineOf(point), LruCoordinate.fromText(selection))
    }

    @Test
    fun `an MGRS alone fills the line, with or without spaces between its groups`() {
        assertEquals(EXPECTED, LruCoordinate.fromText("35T LL 33000 90300"))
        assertEquals(EXPECTED, LruCoordinate.fromText("35TLL3300090300"))
        assertEquals(EXPECTED, LruCoordinate.fromText("35TLL 33000 90300"))
        assertEquals(EXPECTED, LruCoordinate.fromText("MGRS 35TLL3300090300"))
        assertEquals(EXPECTED, LruCoordinate.fromText("Point carte · MGRS 35TLL3300090300 · UTM 35T 433000 4990300"))
        assertEquals(EXPECTED, LruCoordinate.fromText("35t ll 33000 90300"))
    }

    @Test
    fun `invisible characters and line breaks of a copied text do not hide the coordinate`() {
        val variants =
            listOf(
                "Point carte$NBSP·${NBSP}MGRS${NBSP}35T${NBSP}LL${NBSP}33000${NBSP}90300$NBSP·${NBSP}UTM",
                "Point carte $NARROW· MGRS 35T${NARROW}LL${NARROW}33000${NARROW}90300 $NARROW· UTM",
                "Point carte · MGRS 35T LL 33000 90300\r\n· UTM 35T 433000 4990300",
                "Point carte\n·\nMGRS\n35T\nLL\n33000\n90300",
                "${ZERO_WIDTH}Point carte · MGRS 35T$ZERO_WIDTH LL 33000$ZERO_WIDTH 90300$BOM",
                "Point carte $THIN·$THIN MGRS${THIN}35T${THIN}LL${THIN}33000${THIN}90300",
                "  \t Point carte · MGRS 35T LL 33000 90300 · UTM 35T 433000 4990300 · DMS 45°03'53\"N 026°08'49\"E  ",
                "\"Point carte · MGRS 35T LL 33000 90300\"",
                "MGRS : 35T LL 33000 90300.",
            )

        for (text in variants) assertEquals(EXPECTED, LruCoordinate.fromText(text), text)
    }

    @Test
    fun `a whole text pasted in a box fills the line and keeps its altitude, never the box`() {
        val line = LruLine(zone = "12", z = "640")
        val pasted = mapText(LatLon(45.065, 26.147))

        assertTrue(LruCoordinate.isLongInput(pasted))
        assertEquals(lineOf(LatLon(45.065, 26.147)).copy(z = "640"), LruCoordinate.fromLongInput(line, pasted))
        assertEquals(EXPECTED.copy(z = "640"), LruCoordinate.fromLongInput(line, "35TLL3300090300"))
    }

    @Test
    fun `a long text without a coordinate changes nothing, and a value of a box is not long`() {
        val line = LruLine(zone = "35")

        assertNull(LruCoordinate.fromLongInput(line, "Point carte · sans coordonnée complète"))
        // What fits a box is typed as usual, never read as a coordinate.
        assertNull(LruCoordinate.fromLongInput(line, "35T LL"))
        for (value in listOf("35", "T", "LL", "33000", "90300", " 35 ")) {
            assertEquals(false, LruCoordinate.isLongInput(value), value)
        }
    }

    @Test
    fun `a text without an MGRS or another coordinate fills nothing`() {
        assertNull(LruCoordinate.fromText("Point carte"))
        assertNull(LruCoordinate.fromText("Point carte · MGRS 35T LL 3300 9030"))
    }
}
