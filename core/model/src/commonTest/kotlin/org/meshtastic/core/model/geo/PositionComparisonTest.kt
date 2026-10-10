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

class PositionComparisonTest {

    @Test
    fun samePositionHasNoGap() {
        val point = LatLon(48.8584, 2.347)
        assertEquals(0, PositionComparison.gapMeters(point, point))
    }

    @Test
    fun gapAlongAMeridianIsAboutOneHundredAndElevenMetresPerThousandthOfADegree() {
        // 0.001° of latitude is 111.2 m on a sphere of radius 6,371 km.
        assertEquals(111, PositionComparison.gapMeters(LatLon(48.858, 2.347), LatLon(48.859, 2.347)))
    }

    @Test
    fun gapAlongAParallelShrinksWithLatitude() {
        // 0.001° of longitude at 48.858° N: 111.2 m × cos(48.858°) = 73.2 m.
        assertEquals(73, PositionComparison.gapMeters(LatLon(48.858, 2.347), LatLon(48.858, 2.348)))
    }

    @Test
    fun gapIsTheSameBothWays() {
        val radio = LatLon(48.8584, 2.347)
        val phone = LatLon(48.8586, 2.3473)
        assertEquals(PositionComparison.gapMeters(radio, phone), PositionComparison.gapMeters(phone, radio))
        assertEquals(31, PositionComparison.gapMeters(radio, phone))
    }

    @Test
    fun fixQualityReadsHundredthsAndLeavesMissingPartsOut() {
        val quality = RadioFixQuality.of(satsInView = 9, hdopHundredths = 120, pdopHundredths = 0, precisionBits = 32)
        assertEquals(9, quality.satellites)
        assertEquals(1.2, quality.hdop)
        assertNull(quality.pdop)
        assertNull(quality.channelPrecisionMeters)
        assertFalse(quality.isEmpty)
    }

    @Test
    fun fixQualityWithNothingSentIsEmpty() {
        assertTrue(RadioFixQuality.of(0, 0, 0, 0).isEmpty)
    }

    @Test
    fun coarsenedChannelGivesItsRadius() {
        assertEquals(364.7622, RadioFixQuality.of(0, 0, 0, precisionBits = 16).channelPrecisionMeters)
    }
}
