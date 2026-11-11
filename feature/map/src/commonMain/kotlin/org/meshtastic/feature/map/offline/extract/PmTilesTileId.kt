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

/**
 * PMTiles v3 tile identifiers (spec §4.1): the cumulative position of a tile on the Hilbert curves of zoom 0, then zoom
 * 1, and so on. Zoom `z` starts right after the `(4^z - 1) / 3` tiles of the zooms above it, so every tile of an
 * archive has one identifier and the identifiers of one zoom are contiguous.
 *
 * The directories of an archive are sorted by these identifiers, which is what lets an extract find the entries of a
 * region by binary search instead of reading the whole directory.
 */
object PmTilesTileId {
    /** The deepest zoom whose identifiers still fit in a [Long] with room to spare. */
    const val MAX_ZOOM = 26

    /** The identifier of the tile at [zoom], column [x] and row [y] (XYZ: row 0 at the north). */
    fun of(zoom: Int, x: Int, y: Int): Long {
        require(zoom in 0..MAX_ZOOM) { "zoom $zoom out of 0..$MAX_ZOOM" }
        val size = 1L shl zoom
        require(x in 0 until size && y in 0 until size) { "tile $x/$y outside zoom $zoom" }
        return firstAtZoom(zoom) + hilbertIndex(size, x.toLong(), y.toLong())
    }

    /** The identifier of the first tile of [zoom]: the number of tiles of all the zooms above it. */
    fun firstAtZoom(zoom: Int): Long {
        require(zoom in 0..MAX_ZOOM + 1) { "zoom $zoom out of 0..${MAX_ZOOM + 1}" }
        return ((1L shl (2 * zoom)) - 1) / GEOMETRIC_SUM_DIVISOR
    }

    /** Position of `(x, y)` along the Hilbert curve filling a `size × size` grid. */
    private fun hilbertIndex(size: Long, column: Long, row: Long): Long {
        var x = column
        var y = row
        var index = 0L
        var half = size / 2
        while (half > 0) {
            val rx = if (x and half > 0) 1L else 0L
            val ry = if (y and half > 0) 1L else 0L
            index += half * half * ((QUADRANT_FACTOR * rx) xor ry)
            if (ry == 0L) {
                if (rx == 1L) {
                    x = size - 1 - x
                    y = size - 1 - y
                }
                val swap = x
                x = y
                y = swap
            }
            half /= 2
        }
        return index
    }

    private const val QUADRANT_FACTOR = 3L

    /** 1 + 4 + 16 + … + 4^(z-1) = (4^z - 1) / 3. */
    private const val GEOMETRIC_SUM_DIVISOR = 3L
}
