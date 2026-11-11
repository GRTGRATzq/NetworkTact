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
package org.meshtastic.feature.map.offline.extract

import kotlin.test.Test
import kotlin.test.assertTrue

class SizeEstimateTest {

    @Test
    fun theMacFileOfCincuFallsInTheRange() {
        // The 30 km rectangle around Cincu, zoom 15: 15 MB made on a Mac on 8 November 2026 (14.9 MB by the probe).
        val range = SizeEstimate.rough(GeoBounds(24.4126, 45.6475, 25.1874, 46.1865), 0, 15)
        assertTrue(range.low <= 15_000_000 && 15_000_000 <= range.high, "$range")
    }

    @Test
    fun theTownOfDraguignanFallsInTheRange() {
        // 8 km around Draguignan: 4.7 MB of tiles for the circle (probe), a little more for its rectangle.
        val range = SizeEstimate.rough(GeoBounds(6.3659, 43.4651, 6.5641, 43.6089), 0, 15)
        assertTrue(range.low <= 4_700_000 && 4_700_000 <= range.high, "$range")
    }

    @Test
    fun aLowerZoomIsSmaller() {
        val cincu = GeoBounds(24.4126, 45.6475, 25.1874, 46.1865)
        assertTrue(SizeEstimate.rough(cincu, 0, 14).high < SizeEstimate.rough(cincu, 0, 15).high)
    }
}
