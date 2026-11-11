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

import kotlin.math.PI
import kotlin.math.cos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TileCoverTest {

    /** The rectangle around a centre, built as the probe (scripts/carte-sonde/sonde.py) builds it. */
    private fun around(latitude: Double, longitude: Double, radiusKm: Double): GeoBounds {
        val dLat = radiusKm / 111.32
        val dLon = radiusKm / (111.32 * cos(latitude * PI / 180))
        return GeoBounds(longitude - dLon, latitude - dLat, longitude + dLon, latitude + dLat)
    }

    /** Cincu (Romania), 30 km: the counts the probe measured on the 11 November 2026 edition (rectangle). */
    @Test
    fun cincuRectangleHasTheTileCountsOfTheProbe() {
        val cincu = around(45.917, 24.800, 30.0)
        val counts = TileCover.countsByZoom(cincu, 0, 15)
        assertEquals(listOf(90L, 342L, 1296L, 5041L), counts.subList(12, 16))
        assertEquals(6822L, counts.sum())
    }

    @Test
    fun draguignanRectangleHasTheTileCountsOfTheProbe() {
        val draguignan = around(43.537, 6.465, 8.0)
        assertEquals(listOf(9L, 30L, 100L, 361L), TileCover.countsByZoom(draguignan, 12, 15))
        assertEquals(517L, TileCover.count(draguignan, 0, 15))
    }

    @Test
    fun tileIdsAreSortedDistinctAndAsManyAsTheCount() {
        val cincu = around(45.917, 24.800, 30.0)
        val ids = TileCover.tileIds(cincu, 0, 15)
        assertEquals(6822, ids.size)
        assertEquals(ids.size, ids.distinct().size)
        assertEquals(ids.sorted(), ids.toList())
        assertEquals(PmTilesTileId.of(0, 0, 0), ids.first())
    }

    @Test
    fun minimumZoomLeavesOutTheZoomsAboveIt() {
        val cincu = around(45.917, 24.800, 30.0)
        val ids = TileCover.tileIds(cincu, 14, 15)
        assertEquals(1296 + 5041, ids.size)
        assertEquals(true, ids.first() >= PmTilesTileId.firstAtZoom(14))
    }

    @Test
    fun theWholeWorldIsEveryTile() {
        val world = GeoBounds(-180.0, -85.0511, 180.0, 85.0511)
        assertEquals(listOf(1L, 4L, 16L, 64L), TileCover.countsByZoom(world, 0, 3))
    }

    @Test
    fun oneTileWhenTheRectangleIsInsideIt() {
        val range = TileCover.rangeAt(GeoBounds(24.80, 45.91, 24.801, 45.911), 10)
        assertEquals(1L, range.count)
    }

    @Test
    fun refusesReversedAndAntimeridianRectangles() {
        assertFailsWith<IllegalArgumentException> { GeoBounds(25.0, 45.0, 24.0, 46.0) }
        assertFailsWith<IllegalArgumentException> { GeoBounds(170.0, -10.0, -170.0, 10.0) }
        assertFailsWith<IllegalArgumentException> { GeoBounds(24.0, 46.0, 25.0, 45.0) }
        assertFailsWith<IllegalArgumentException> { GeoBounds(24.0, 45.0, 181.0, 46.0) }
    }
}
