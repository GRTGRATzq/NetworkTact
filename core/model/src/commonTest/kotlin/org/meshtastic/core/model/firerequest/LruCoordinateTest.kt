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
import org.meshtastic.core.model.geo.MessageCoordinate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class LruCoordinateTest {

    private val example = LruLine("35", "T", "LL", "33000", "90300", "610")

    @Test
    fun specification_example_is_checked_by_the_converter() {
        val checked = assertIs<CoordinateInput.Valid>(LruCoordinate.check(example))
        val converter =
            assertIs<CoordinateInput.Valid>(CoordinateParser.parse("35T LL 33000 90300", CoordinateFormat.MGRS))
        assertEquals(converter.point, checked.point)
        assertEquals(checked.point, LruCoordinate.point(example))
    }

    @Test
    fun the_line_read_from_a_received_request_gives_the_same_point() {
        val read = FireRequestReader.read(listOf("7 G 35T LL 33000 - 90300 · Z 610 m · TLE 2"))
        assertEquals(LruCoordinate.point(example), LruCoordinate.point(read.request.location.g))
    }

    @Test
    fun lowercase_and_spaces_are_tolerated() {
        assertNotNull(LruCoordinate.point(LruLine(" 35", "t", "ll ", "33000", "90300")))
    }

    @Test
    fun an_empty_line_is_empty_not_wrong() {
        assertEquals(CoordinateInput.Invalid(CoordinateError.EMPTY), LruCoordinate.check(LruLine(z = "610")))
    }

    @Test
    fun x_and_y_need_five_digits_each() {
        assertEquals(
            CoordinateInput.Invalid(CoordinateError.PRECISION),
            LruCoordinate.check(LruLine("35", "T", "LL", "3300", "903000")),
        )
        assertEquals(
            CoordinateInput.Invalid(CoordinateError.PRECISION),
            LruCoordinate.check(LruLine("35", "T", "LL", "3300", "9030")),
        )
        assertEquals(
            CoordinateInput.Invalid(CoordinateError.SYNTAX),
            LruCoordinate.check(LruLine("35", "T", "LL", "33OOO", "90300")),
        )
    }

    @Test
    fun zone_band_and_square_must_exist() {
        assertEquals(CoordinateInput.Invalid(CoordinateError.ZONE), LruCoordinate.check(example.copy(zone = "61")))
        assertEquals(CoordinateInput.Invalid(CoordinateError.BAND), LruCoordinate.check(example.copy(band = "I")))
        assertEquals(CoordinateInput.Invalid(CoordinateError.SQUARE), LruCoordinate.check(example.copy(square = "AL")))
        assertIs<CoordinateInput.Invalid>(LruCoordinate.check(example.copy(band = "")))
        assertNull(LruCoordinate.point(example.copy(square = "")))
    }

    @Test
    fun a_pasted_map_point_fills_the_line() {
        val pasted = "Point carte · MGRS 35T LL 33000 90300 · UTM 35T 433000 4990300 · DMS 45°03'53\"N 026°08'49\"E"
        val line = assertNotNull(LruCoordinate.fromText(pasted))
        assertEquals(LruLine("35", "T", "LL", "33000", "90300"), line)
    }

    @Test
    fun a_pasted_converter_result_fills_the_line() {
        assertEquals(LruLine("31", "U", "DQ", "48251", "11932"), LruCoordinate.fromText("31U DQ 48251 11932"))
        assertEquals(LruLine("31", "U", "DQ", "48251", "11932"), LruCoordinate.fromText("31U 448251 5411932"))
    }

    @Test
    fun a_text_without_a_coordinate_fills_nothing() {
        assertNull(LruCoordinate.fromText("Observateur X 34550 Y 88800 Z 640"))
        assertNull(LruCoordinate.fromText("06 12 34 56 78 · 14:30 · 11/11/2026"))
        assertNull(LruCoordinate.fromText(""))
    }

    @Test
    fun no_part_of_the_example_is_read_as_a_position_by_ordinary_messages() {
        // The dash of the sheet's form is not a format of MessageCoordinate: « Voir sur la carte » on an ordinary
        // message never finds a position in a part, and only box 7 lines G and H are read, here.
        for (part in FireRequestExamples.specificationParts) assertNull(MessageCoordinate.find(part), part)
        assertNull(MessageCoordinate.find("9 Grille · Gisement d'observ. 4800 · Observateur X 34550 Y 88800 Z 640"))
        assertNull(MessageCoordinate.find("7 D X 34550 · Y 88800 · Z 640 m"))
    }
}
