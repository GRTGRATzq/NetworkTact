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

import kotlinx.datetime.TimeZone
import org.meshtastic.core.model.utf8Size
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
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

    @Test
    fun observationTimeFollowsTheHeader() {
        assertEquals(
            "[CR] FAIT OBSERVÉ · 14:05 · Lieu MGRS 31U DQ 48251 11932 · fumée",
            ObservedFactMessage.build(
                place("31U DQ 48251 11932", CoordinateFormat.MGRS),
                CoordinateFormat.MGRS,
                "fumée",
                ObservedTime(hour = 14, minute = 5, previousDay = false),
            ),
        )
    }

    @Test
    fun observationTimeInTheFutureIsTheDayBefore() {
        // 00:10 UTC on 6 October 2026: 23:50 cannot have been observed yet today.
        val now = 1_791_245_400L
        val observed = ObservedTime.of(hour = 23, minute = 50, nowEpochSeconds = now, timeZone = TimeZone.UTC)
        assertEquals(ObservedTime(23, 50, previousDay = true), observed)
        assertEquals(
            "[CR] FAIT OBSERVÉ · 23:50 (veille) · Lieu MGRS 31U DQ 48251 11932 · fumée",
            ObservedFactMessage.build(
                place("31U DQ 48251 11932", CoordinateFormat.MGRS),
                CoordinateFormat.MGRS,
                "fumée",
                observed,
            ),
        )
    }

    @Test
    fun observationTimeAtTheCurrentMinuteIsToday() {
        val now = 1_791_245_400L
        assertEquals(ObservedTime(0, 10, previousDay = false), ObservedTime.of(0, 10, now, TimeZone.UTC))
        assertEquals(ObservedTime(0, 10, previousDay = false), ObservedTime.now(now, TimeZone.UTC))
        assertEquals(ObservedTime(0, 9, previousDay = false), ObservedTime.of(0, 9, now, TimeZone.UTC))
    }

    @Test
    fun observationTimeOutOfRangeIsRejected() {
        assertNull(ObservedTime.of(24, 0, 0L, TimeZone.UTC))
        assertNull(ObservedTime.of(12, 60, 0L, TimeZone.UTC))
        assertNull(ObservedTime.of(-1, 0, 0L, TimeZone.UTC))
    }

    @Test
    fun observationTimeIsReadBack() {
        assertEquals("14:05", ObservedFactMessage.observedAt("[CR] FAIT OBSERVÉ · 14:05 · Lieu MGRS 31U DQ 1 1 · a"))
        assertEquals(
            "23:50 (veille)",
            ObservedFactMessage.observedAt("[CR] FAIT OBSERVÉ · 23:50 (veille) · Lieu MGRS 31U DQ 1 1 · a"),
        )
    }

    @Test
    fun earlierFormatIsStillAnObservedFactWithoutTime() {
        val old = "[CR] FAIT OBSERVÉ · Lieu MGRS 31U DQ 48251 11932 · fumée"
        assertTrue(ObservedFactMessage.isObservedFact(old))
        assertTrue(old.startsWith("[CR] "))
        assertNull(ObservedFactMessage.observedAt(old))
        assertFalse(ObservedFactMessage.isObservedFact("[CR] RAS"))
    }

    @Test
    fun observationTimeAddsAtMostSixteenBytes() {
        val place = place("31U DQ 48251 11932", CoordinateFormat.MGRS)
        val without = ObservedFactMessage.build(place, CoordinateFormat.MGRS, "fumée").utf8Size()
        val with = ObservedFactMessage.build(place, CoordinateFormat.MGRS, "fumée", ObservedTime(23, 50, true))
        assertEquals(without + " · 23:50 (veille)".utf8Size(), with.utf8Size())
    }
}
