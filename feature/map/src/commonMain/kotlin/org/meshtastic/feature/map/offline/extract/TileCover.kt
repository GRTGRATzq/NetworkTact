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
import kotlin.math.asinh
import kotlin.math.floor
import kotlin.math.tan

/**
 * A rectangle in degrees, as a map shows it: longitudes [west] to [east], latitudes [south] to [north].
 *
 * It may not cross the antimeridian (west must be less than east): NetworkTact's areas are a few dozen kilometres wide,
 * and refusing the case keeps the tile ranges simple.
 */
data class GeoBounds(val west: Double, val south: Double, val east: Double, val north: Double) {
    init {
        require(west in -MAX_LONGITUDE..MAX_LONGITUDE && east in -MAX_LONGITUDE..MAX_LONGITUDE) {
            "longitudes out of range: $west, $east"
        }
        require(south in -MAX_LATITUDE..MAX_LATITUDE && north in -MAX_LATITUDE..MAX_LATITUDE) {
            "latitudes out of range: $south, $north"
        }
        require(west < east) { "west $west must be less than east $east (no antimeridian crossing)" }
        require(south < north) { "south $south must be less than north $north" }
    }

    private companion object {
        const val MAX_LONGITUDE = 180.0
        const val MAX_LATITUDE = 90.0
    }
}

/** The tiles of one zoom inside a rectangle: columns [minX] to [maxX] and rows [minY] to [maxY], both inclusive. */
data class TileRange(val zoom: Int, val minX: Int, val maxX: Int, val minY: Int, val maxY: Int) {
    val count: Long
        get() = (maxX - minX + 1).toLong() * (maxY - minY + 1).toLong()
}

/**
 * Which tiles a [GeoBounds] covers, zoom by zoom, in the Web Mercator XYZ scheme the Protomaps basemap uses (row 0 at
 * the north). A tile is in when any part of it is: the same rule as `pmtiles extract --bbox`.
 */
object TileCover {
    /** Latitude limit of Web Mercator: beyond it, rows are clamped to the first or last one. */
    private const val MERCATOR_LIMIT = 85.0511287798066
    private const val FULL_TURN = 360.0
    private const val HALF_TURN = 180.0

    fun rangeAt(bounds: GeoBounds, zoom: Int): TileRange {
        require(zoom in 0..PmTilesTileId.MAX_ZOOM) { "zoom $zoom out of 0..${PmTilesTileId.MAX_ZOOM}" }
        val last = (1 shl zoom) - 1
        return TileRange(
            zoom = zoom,
            minX = column(bounds.west, zoom).coerceIn(0, last),
            maxX = column(bounds.east, zoom).coerceIn(0, last),
            minY = row(bounds.north, zoom).coerceIn(0, last),
            maxY = row(bounds.south, zoom).coerceIn(0, last),
        )
    }

    /** Number of tiles at each zoom from [minZoom] to [maxZoom], in that order. */
    fun countsByZoom(bounds: GeoBounds, minZoom: Int, maxZoom: Int): List<Long> =
        (minZoom..maxZoom).map { rangeAt(bounds, it).count }

    fun count(bounds: GeoBounds, minZoom: Int, maxZoom: Int): Long = countsByZoom(bounds, minZoom, maxZoom).sum()

    /** The identifiers of every covered tile from [minZoom] to [maxZoom], sorted ascending, each once. */
    fun tileIds(bounds: GeoBounds, minZoom: Int, maxZoom: Int): LongArray {
        val total = count(bounds, minZoom, maxZoom)
        require(total <= Int.MAX_VALUE) { "$total tiles: far beyond any area this app downloads" }
        val ids = LongArray(total.toInt())
        var next = 0
        for (zoom in minZoom..maxZoom) {
            val range = rangeAt(bounds, zoom)
            for (x in range.minX..range.maxX) {
                for (y in range.minY..range.maxY) {
                    ids[next++] = PmTilesTileId.of(zoom, x, y)
                }
            }
        }
        ids.sort()
        return ids
    }

    private fun column(longitude: Double, zoom: Int): Int =
        floor((longitude + HALF_TURN) / FULL_TURN * (1 shl zoom)).toInt()

    private fun row(latitude: Double, zoom: Int): Int {
        val radians = latitude.coerceIn(-MERCATOR_LIMIT, MERCATOR_LIMIT) * PI / HALF_TURN
        return floor((1 - asinh(tan(radians)) / PI) / 2 * (1 shl zoom)).toInt()
    }
}
