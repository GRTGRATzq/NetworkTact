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
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Reference values:
 * - IBM developerWorks, "Convert between geographic coordinate systems" (j-coordconvert), listing 7: UTM rounded to the
 *   metre;
 * - Chris Veness, geodesy library test suite (test/utm-mgrs-tests.js, MIT), whose values agree with GeographicLib and
 *   the NGA converter, given to the millimetre or better.
 */
class UtmTest {

    private fun utm(latitude: Double, longitude: Double): UtmCoordinate =
        checkNotNull(Utm.fromLatLon(LatLon(latitude, longitude)))

    private fun assertMetres(expected: Double, actual: Double, tolerance: Double = 0.001) =
        assertTrue(abs(expected - actual) <= tolerance, "expected $expected, got $actual")

    private fun assertDegrees(expected: Double, actual: Double) =
        assertTrue(abs(expected - actual) <= 1e-9, "expected $expected, got $actual")

    @Test
    fun equator_and_greenwich() {
        val u = utm(0.0, 0.0)
        assertEquals(31, u.zone)
        assertEquals('N', u.band)
        assertMetres(166021.443081, u.easting, 1e-6)
        assertMetres(0.0, u.northing, 1e-6)
        assertEquals("31N 166021 0", u.format())
    }

    @Test
    fun north_east_and_south_west() {
        val ne = utm(1.0, 1.0)
        assertMetres(277438.26352, ne.easting, 1e-5)
        assertMetres(110597.97252, ne.northing, 1e-5)
        val sw = utm(-1.0, -1.0)
        assertEquals(30, sw.zone)
        assertEquals('M', sw.band)
        assertMetres(722561.73648, sw.easting, 1e-5)
        assertMetres(9889402.02748, sw.northing, 1e-5)
    }

    @Test
    fun zone_boundary_projects_into_the_neighbouring_zone() {
        // 1° E belongs to zone 31; projected in zone 30 it lies far east of the false easting.
        val (easting, northing) = Utm.project(1.0, 1.0, 30)
        assertMetres(945396.68398, easting, 1e-5)
        assertMetres(110801.83255, northing, 1e-5)
        assertEquals(30, Utm.zoneOf(1.0, -0.000001))
        assertEquals(31, Utm.zoneOf(1.0, 0.0))
        assertEquals(60, Utm.zoneOf(1.0, 180.0))
        assertEquals(1, Utm.zoneOf(1.0, -180.0))
    }

    @Test
    fun published_points_north_south_east_west() {
        assertEquals("31U 448252 5411944", utm(48.8583, 2.2945).format()) // Eiffel tower
        assertMetres(448251.898, utm(48.8583, 2.2945).easting)
        assertMetres(5411943.794, utm(48.8583, 2.2945).northing)
        assertMetres(334873.199, utm(-33.857, 151.215).easting) // Sydney opera house
        assertMetres(6252266.092, utm(-33.857, 151.215).northing)
        assertMetres(323394.296, utm(38.8977, -77.0365).easting) // White House
        assertMetres(4307395.634, utm(38.8977, -77.0365).northing)
        assertMetres(683466.254, utm(-22.9519, -43.2106).easting) // Rio, Christ the Redeemer
        assertMetres(7460687.433, utm(-22.9519, -43.2106).northing)
    }

    @Test
    fun ibm_listing_7() {
        assertEquals("30N 808084 14386", utm(0.13, -0.2324).format())
        assertEquals("34G 683474 4942631", utm(-45.6456, 23.3545).format())
        assertEquals("25L 404859 8588691", utm(-12.765, -33.8765).format())
        assertEquals("8Q 453580 2594273", utm(23.4578, -135.4545).format())
        assertEquals("57X 450794 8586116", utm(77.345, 156.9876).format())
    }

    @Test
    fun inverse_returns_the_published_latitude_and_longitude() {
        val eiffel = Utm.toLatLon(31, true, 448251.898, 5411943.794)
        assertTrue(abs(eiffel.latitude - 48.8583) < 1e-7 && abs(eiffel.longitude - 2.2945) < 1e-7)
        val rio = Utm.toLatLon(23, false, 683466.254, 7460687.433)
        assertTrue(abs(rio.latitude + 22.9519) < 1e-7 && abs(rio.longitude + 43.2106) < 1e-7)
        val origin = Utm.toLatLon(31, true, 166021.443081, 0.0)
        assertDegrees(0.0, origin.latitude)
        assertTrue(abs(origin.longitude) < 1e-9)
    }

    @Test
    fun norway_exception() {
        // Bergen, 5.32° E, would be zone 31 without the 32V extension.
        val bergen = utm(60.39135, 5.3249)
        assertEquals(32, bergen.zone)
        assertEquals('V', bergen.band)
        assertMetres(297508.41, bergen.easting, 0.005)
        assertMetres(6700645.30, bergen.northing, 0.005)
        assertEquals(32, Utm.zoneOf(60.0, 4.0))
        assertEquals(31, Utm.zoneOf(60.0, 2.9))
        assertEquals(31, Utm.zoneOf(55.9, 4.0))
    }

    @Test
    fun svalbard_exceptions() {
        assertEquals(31, Utm.zoneOf(75.0, 8.0))
        assertEquals(33, Utm.zoneOf(75.0, 10.0))
        assertEquals(33, Utm.zoneOf(75.0, 20.0))
        assertEquals(35, Utm.zoneOf(75.0, 22.0))
        assertEquals(35, Utm.zoneOf(75.0, 32.0))
        assertEquals(37, Utm.zoneOf(75.0, 34.0))
        assertEquals(38, Utm.zoneOf(75.0, 43.0))
        assertNull(Utm.zoneLongitudes(32, 'X'))
        assertNull(Utm.zoneLongitudes(34, 'X'))
        assertNull(Utm.zoneLongitudes(36, 'X'))
    }

    @Test
    fun bands() {
        assertEquals('C', Utm.bandOf(-80.0))
        assertEquals('M', Utm.bandOf(-0.0001))
        assertEquals('N', Utm.bandOf(0.0))
        assertEquals('U', Utm.bandOf(48.8583))
        assertEquals('W', Utm.bandOf(71.9))
        assertEquals('X', Utm.bandOf(72.0))
        assertEquals('X', Utm.bandOf(84.0))
    }

    @Test
    fun poles_are_not_covered() {
        assertNull(Utm.fromLatLon(LatLon(84.0001, 0.0)))
        assertNull(Utm.fromLatLon(LatLon(-80.0001, 0.0)))
        assertNull(Utm.fromLatLon(LatLon(90.0, 0.0)))
        assertEquals(false, LatLon(85.0, 0.0).isInUtmLimits)
    }
}
