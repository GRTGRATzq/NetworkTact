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
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PmTilesTileIdTest {

    @Test
    fun matchesTheExamplesOfTheSpecification() {
        assertEquals(0L, PmTilesTileId.of(0, 0, 0))
        assertEquals(1L, PmTilesTileId.of(1, 0, 0))
        assertEquals(2L, PmTilesTileId.of(1, 0, 1))
        assertEquals(3L, PmTilesTileId.of(1, 1, 1))
        assertEquals(4L, PmTilesTileId.of(1, 1, 0))
        assertEquals(5L, PmTilesTileId.of(2, 0, 0))
        assertEquals(19_078_479L, PmTilesTileId.of(12, 3423, 1763))
    }

    @Test
    fun eachZoomFillsExactlyItsOwnRangeOfIdentifiers() {
        for (zoom in 0..6) {
            val size = 1 shl zoom
            val ids = buildList { for (x in 0 until size) for (y in 0 until size) add(PmTilesTileId.of(zoom, x, y)) }
            val expected = (PmTilesTileId.firstAtZoom(zoom) until PmTilesTileId.firstAtZoom(zoom + 1)).toList()
            assertEquals(expected, ids.sorted(), "zoom $zoom")
        }
    }

    @Test
    fun deepZoomsStayPositive() {
        val last = (1 shl PmTilesTileId.MAX_ZOOM) - 1
        val id = PmTilesTileId.of(PmTilesTileId.MAX_ZOOM, last, 0)
        assertEquals(true, id >= PmTilesTileId.firstAtZoom(PmTilesTileId.MAX_ZOOM), "id $id")
        assertEquals(true, id < PmTilesTileId.firstAtZoom(PmTilesTileId.MAX_ZOOM + 1), "id $id")
    }

    @Test
    fun refusesTilesOutsideTheirZoom() {
        assertFailsWith<IllegalArgumentException> { PmTilesTileId.of(2, 4, 0) }
        assertFailsWith<IllegalArgumentException> { PmTilesTileId.of(2, 0, -1) }
        assertFailsWith<IllegalArgumentException> { PmTilesTileId.of(PmTilesTileId.MAX_ZOOM + 1, 0, 0) }
    }
}
