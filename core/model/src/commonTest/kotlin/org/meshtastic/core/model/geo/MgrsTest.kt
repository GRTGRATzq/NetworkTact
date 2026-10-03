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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Reference values: IBM developerWorks j-coordconvert listing 7 (MGRS truncated to the metre) and the Chris Veness
 * geodesy test suite (test/utm-mgrs-tests.js), which also covers the Norway exception.
 */
class MgrsTest {

    private fun mgrs(latitude: Double, longitude: Double): String =
        checkNotNull(Mgrs.fromLatLon(LatLon(latitude, longitude))).format()

    private fun mgrsOfUtm(zone: Int, band: Char, easting: Double, northing: Double): String =
        Mgrs.fromUtm(UtmCoordinate(zone, band, easting, northing)).format()

    @Test
    fun equator_band_n() {
        assertEquals("31N AA 66021 00000", mgrs(0.0, 0.0))
        assertEquals("31N BB 77438 10597", mgrsOfUtm(31, 'N', 277438.263521, 110597.972524))
    }

    @Test
    fun ibm_listing_7() {
        assertEquals("30N ZF 08084 14385", mgrs(0.13, -0.2324))
        assertEquals("34G FQ 83473 42631", mgrs(-45.6456, 23.3545))
        assertEquals("25L DF 04859 88691", mgrs(-12.765, -33.8765))
        assertEquals("8Q ML 53580 94272", mgrs(23.4578, -135.4545))
        assertEquals("57X VF 50793 86116", mgrs(77.345, 156.9876))
    }

    @Test
    fun published_points_north_south_east_west() {
        assertEquals("30M YD 22561 89402", mgrsOfUtm(30, 'M', 722561.736479, 9889402.027476))
        assertEquals("31U DQ 48251 11943", mgrsOfUtm(31, 'U', 448251.898, 5411943.794))
        assertEquals("56H LH 34873 52266", mgrsOfUtm(56, 'H', 334873.199, 6252266.092))
        assertEquals("18S UJ 23394 07395", mgrsOfUtm(18, 'S', 323394.296, 4307395.634))
        assertEquals("23K PQ 83466 60687", mgrsOfUtm(23, 'K', 683466.254, 7460687.433))
    }

    @Test
    fun norway_exception() {
        assertEquals("32V KN 97508 00645", mgrs(60.39135, 5.3249))
    }

    @Test
    fun svalbard_exception() {
        // 75° N 10° E is in 33X, not 32X which does not exist.
        assertTrue(mgrs(75.0, 10.0).startsWith("33X "))
        assertTrue(mgrs(75.0, 8.0).startsWith("31X "))
    }

    @Test
    fun truncates_rather_than_rounds() {
        assertEquals("31U DQ 48251 11932", mgrsOfUtm(31, 'U', 448251.795, 5411932.678))
    }

    @Test
    fun back_to_utm_picks_the_2000_km_repetition_of_the_band() {
        val cases =
            listOf(
                MgrsCoordinate(31, 'U', 'D', 'Q', 48251, 11943) to "31U 448251 5411943",
                MgrsCoordinate(56, 'H', 'L', 'H', 34873, 52266) to "56H 334873 6252266",
                MgrsCoordinate(30, 'M', 'Y', 'D', 22561, 89402) to "30M 722561 9889402",
                MgrsCoordinate(32, 'V', 'K', 'N', 97508, 645) to "32V 297508 6700645",
                MgrsCoordinate(31, 'N', 'A', 'A', 66021, 0) to "31N 166021 0",
                MgrsCoordinate(57, 'X', 'V', 'F', 50793, 86116) to "57X 450793 8586116",
            )
        for ((mgrs, utm) in cases) assertEquals(utm, Mgrs.toUtm(mgrs)?.format(), mgrs.format())
    }

    @Test
    fun round_trip_at_band_edges() {
        for (latitude in listOf(-80.0, -64.0, -0.5, 0.0, 64.0, 83.9)) {
            for (longitude in listOf(-179.5, -3.0, 0.0, 3.0, 179.5)) {
                val point = LatLon(latitude, longitude)
                val mgrs = checkNotNull(Mgrs.fromLatLon(point))
                val utm = checkNotNull(Mgrs.toUtm(mgrs)) { mgrs.format() }
                val back = Utm.toLatLon(utm.zone, utm.isNorth, utm.easting, utm.northing)
                assertTrue(kotlin.math.abs(back.latitude - latitude) < 2e-5, "$point → ${mgrs.format()} → $back")
            }
        }
    }

    @Test
    fun squares_must_belong_to_the_zone() {
        assertTrue(Mgrs.isValidSquare(1, 'A', 'A'))
        assertFalse(Mgrs.isValidSquare(2, 'A', 'A'))
        assertFalse(Mgrs.isValidSquare(1, 'I', 'A'))
        assertFalse(Mgrs.isValidSquare(1, 'A', 'W'))
        assertNull(Mgrs.toUtm(MgrsCoordinate(2, 'C', 'A', 'A', 0, 0)))
    }

    @Test
    fun poles_are_not_covered() {
        assertNull(Mgrs.fromLatLon(LatLon(85.0, 0.0)))
        assertNull(Mgrs.fromLatLon(LatLon(-81.0, 0.0)))
    }
}
