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

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CoordinateParserTest {

    private fun valid(text: String, format: CoordinateFormat): LatLon =
        assertIs<CoordinateInput.Valid>(CoordinateParser.parse(text, format), text).point

    private fun error(text: String, format: CoordinateFormat): CoordinateError =
        assertIs<CoordinateInput.Invalid>(CoordinateParser.parse(text, format), text).error

    private fun assertNear(expected: LatLon, actual: LatLon, degrees: Double) = assertTrue(
        abs(expected.latitude - actual.latitude) < degrees && abs(expected.longitude - actual.longitude) < degrees,
        "expected $expected, got $actual",
    )

    @Test
    fun dms_format_to_the_second() {
        assertEquals("48°51'24\"N 002°21'03\"E", Dms.format(LatLon(48.856667, 2.350833)))
        assertEquals("33°51'25\"S 151°12'54\"E", Dms.format(LatLon(-33.857, 151.215)))
        assertEquals("38°53'52\"N 077°02'11\"W", Dms.format(LatLon(38.8977, -77.0365)))
        assertEquals("00°00'00\"N 000°00'00\"E", Dms.format(LatLon(-0.0000001, -0.0000001)))
    }

    @Test
    fun dms_seconds_carry_into_minutes_and_degrees() {
        assertEquals("01°00'00\"N 010°00'00\"E", Dms.format(LatLon(0.99999, 9.99999)))
    }

    @Test
    fun dms_parsing() {
        assertNear(LatLon(48.856667, 2.350833), valid("48°51'24\"N 002°21'03\"E", CoordinateFormat.DMS), 1e-5)
        assertNear(LatLon(48.856667, 2.350833), valid("48° 51′ 24″ n, 2°21'3\"e", CoordinateFormat.DMS), 1e-5)
        assertNear(LatLon(-33.857, -77.0365), valid("33°51'25.2\"S 77°02'11.4\"O", CoordinateFormat.DMS), 1e-6)
        assertNear(LatLon(-33.857, -77.0365), valid("33°51'25,2\"S 77°02'11,4\"W", CoordinateFormat.DMS), 1e-6)
    }

    @Test
    fun dms_symbols_may_be_replaced_by_spaces() {
        val expected = valid("48°51'24\"N 002°21'03\"E", CoordinateFormat.DMS)
        assertEquals(expected, valid("48 51 24 N 2 21 3 E", CoordinateFormat.DMS))
        assertEquals(expected, valid("48 51 24N, 002 21 03E", CoordinateFormat.DMS))
        assertEquals(CoordinateError.SYNTAX, error("48 51 N 2 21 E", CoordinateFormat.DMS))
        assertEquals(CoordinateError.SYNTAX, error("48 51 24 2 21 3", CoordinateFormat.DMS))
    }

    @Test
    fun dms_rejections() {
        assertEquals(CoordinateError.EMPTY, error("   ", CoordinateFormat.DMS))
        assertEquals(CoordinateError.SYNTAX, error("48.8566 2.3522", CoordinateFormat.DMS))
        assertEquals(CoordinateError.SYNTAX, error("48°51'N 002°21'E", CoordinateFormat.DMS))
        assertEquals(CoordinateError.SYNTAX, error("002°21'03\"E 48°51'24\"N", CoordinateFormat.DMS))
        assertEquals(CoordinateError.MINUTES_SECONDS, error("48°60'00\"N 002°21'03\"E", CoordinateFormat.DMS))
        assertEquals(CoordinateError.MINUTES_SECONDS, error("48°51'60\"N 002°21'03\"E", CoordinateFormat.DMS))
        assertEquals(CoordinateError.DEGREES, error("91°00'00\"N 002°21'03\"E", CoordinateFormat.DMS))
        assertEquals(CoordinateError.DEGREES, error("48°51'24\"N 180°00'01\"E", CoordinateFormat.DMS))
    }

    @Test
    fun dms_beyond_utm_limits_is_valid_but_has_no_grid_reference() {
        val formatted = FormattedCoordinates.of(valid("85°00'00\"N 010°00'00\"E", CoordinateFormat.DMS))
        assertNull(formatted.mgrs)
        assertNull(formatted.utm)
        assertEquals("85°00'00\"N 010°00'00\"E", formatted.dms)
    }

    @Test
    fun mgrs_parsing_with_or_without_spaces() {
        val spaced = valid("31U DQ 48251 11932", CoordinateFormat.MGRS)
        assertEquals(spaced, valid("31udq4825111932", CoordinateFormat.MGRS))
        assertEquals("31U 448251 5411932", Utm.fromLatLon(spaced)?.format())
    }

    @Test
    fun mgrs_rejections() {
        assertEquals(CoordinateError.SYNTAX, error("Paris", CoordinateFormat.MGRS))
        assertEquals(CoordinateError.ZONE, error("61U DQ 48251 11932", CoordinateFormat.MGRS))
        assertEquals(CoordinateError.ZONE, error("32X MH 11111 11111", CoordinateFormat.MGRS))
        assertEquals(CoordinateError.BAND, error("31I DQ 48251 11932", CoordinateFormat.MGRS))
        assertEquals(CoordinateError.BAND, error("31Z DQ 48251 11932", CoordinateFormat.MGRS))
        assertEquals(CoordinateError.SQUARE, error("31U JQ 48251 11932", CoordinateFormat.MGRS))
        assertEquals(CoordinateError.PRECISION, error("31U DQ 4825 1193", CoordinateFormat.MGRS))
        // Row Q of zone 31 lies near 12.6° N (+ 2 000 km steps), never in band N (0°–8° N).
        assertEquals(CoordinateError.OUTSIDE_CELL, error("31N DQ 48251 11932", CoordinateFormat.MGRS))
    }

    @Test
    fun a_typed_reference_reads_back_unchanged() {
        val references = listOf("31U DQ 48251 11932", "30M YD 22561 89402", "32V KN 97508 00645", "57X VF 50793 86116")
        for (reference in references) {
            assertEquals(reference, FormattedCoordinates.of(valid(reference, CoordinateFormat.MGRS)).mgrs)
        }
        for (reference in listOf("31U 448251 5411932", "56H 334873 6252266", "18S 323394 4307395")) {
            assertEquals(reference, FormattedCoordinates.of(valid(reference, CoordinateFormat.UTM)).utm)
        }
        val dms = "48°51'24\"N 002°21'03\"E"
        assertEquals(dms, FormattedCoordinates.of(valid(dms, CoordinateFormat.DMS)).dms)
    }

    @Test
    fun utm_parsing() {
        assertNear(LatLon(48.8583, 2.2945), valid("31U 448251.898 5411943.794", CoordinateFormat.UTM), 1e-7)
        assertNear(LatLon(-33.857, 151.215), valid("56h 334873,199 6252266,092", CoordinateFormat.UTM), 1e-7)
        assertNear(LatLon(48.8583, 2.2945), valid("31 U 448252 5411944", CoordinateFormat.UTM), 1e-5)
    }

    @Test
    fun utm_rejections() {
        assertEquals(CoordinateError.SYNTAX, error("31U 448251", CoordinateFormat.UTM))
        assertEquals(CoordinateError.ZONE, error("0U 448251 5411932", CoordinateFormat.UTM))
        assertEquals(CoordinateError.BAND, error("31O 448251 5411932", CoordinateFormat.UTM))
        assertEquals(CoordinateError.EASTING, error("31U 48251 5411932", CoordinateFormat.UTM))
        assertEquals(CoordinateError.NORTHING, error("31U 448251 10000001", CoordinateFormat.UTM))
        // "N" is band N (0°–8° N), not the northern hemisphere: Paris's northing is far outside it.
        assertEquals(CoordinateError.OUTSIDE_CELL, error("31N 448251 5411932", CoordinateFormat.UTM))
    }

    @Test
    fun all_three_formats_of_one_point() {
        val formatted = FormattedCoordinates.of(LatLon(48.8583, 2.2945))
        assertEquals("31U DQ 48251 11943", formatted.mgrs)
        assertEquals("31U 448252 5411944", formatted.utm)
        assertEquals("48°51'30\"N 002°17'40\"E", formatted.dms)
    }
}
