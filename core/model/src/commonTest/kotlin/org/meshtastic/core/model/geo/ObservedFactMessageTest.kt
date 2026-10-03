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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ObservedFactMessageTest {

    private fun place(text: String, format: CoordinateFormat) =
        FormattedCoordinates.of(assertIs<CoordinateInput.Valid>(CoordinateParser.parse(text, format)).point)

    @Test
    fun place_typed_in_dms_is_followed_by_mgrs() {
        assertEquals(
            "[CR] FAIT OBSERVÉ · Lieu DMS 48°51'24\"N 002°21'03\"E = MGRS 31U DQ 52382 11725 · deux véhicules arrêtés",
            ObservedFactMessage.build(
                place("48°51'24\"N 002°21'03\"E", CoordinateFormat.DMS),
                CoordinateFormat.DMS,
                "deux véhicules arrêtés",
            ),
        )
    }

    @Test
    fun place_typed_in_utm_is_followed_by_mgrs() {
        assertEquals(
            "[CR] FAIT OBSERVÉ · Lieu UTM 31U 448251 5411932 = MGRS 31U DQ 48251 11932 · pont coupé",
            ObservedFactMessage.build(
                place("31U 448251 5411932", CoordinateFormat.UTM),
                CoordinateFormat.UTM,
                "pont coupé",
            ),
        )
    }

    @Test
    fun place_typed_in_mgrs_is_given_once() {
        assertEquals(
            "[CR] FAIT OBSERVÉ · Lieu MGRS 31U DQ 48251 11932 · fumée",
            ObservedFactMessage.build(place("31udq4825111932", CoordinateFormat.MGRS), CoordinateFormat.MGRS, "fumée"),
        )
    }

    @Test
    fun description_is_put_on_one_line() {
        assertTrue(
            ObservedFactMessage.build(
                place("31U DQ 48251 11932", CoordinateFormat.MGRS),
                CoordinateFormat.MGRS,
                "  a\n\n b  ",
            )
                .endsWith(" · a b"),
        )
    }

    @Test
    fun polar_place_has_only_dms() {
        assertEquals(
            "[CR] FAIT OBSERVÉ · Lieu DMS 85°00'00\"N 010°00'00\"E · glace",
            ObservedFactMessage.build(
                place("85°00'00\"N 010°00'00\"E", CoordinateFormat.DMS),
                CoordinateFormat.DMS,
                "glace",
            ),
        )
    }

    @Test
    fun report_is_filed_under_the_cr_prefix() {
        assertTrue(ObservedFactMessage.HEADER.startsWith("[CR] "))
    }
}
