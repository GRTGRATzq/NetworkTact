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
 * The rough size of a map before anything is asked of the network (plan §7, level 1): the exact number of tiles of the
 * rectangle, zoom by zoom, times the bytes per tile the probe measured on the 11 November 2026 edition in two places,
 * the countryside around Cincu (low bound) and the town of Draguignan (high bound). Low zooms weigh a lot whatever the
 * area (one tile of zoom 7 holds 220 to 490 KB), high zooms follow the density.
 *
 * Two places only: a big city can exceed the high bound, so the screen calls it a rough estimate; the exact size comes
 * from [MapExtractor.plan].
 */
object SizeEstimate {
    /** Bytes per tile at zooms 0 to 15, countryside (Cincu, 30 km). */
    private val COUNTRYSIDE =
        longArrayOf(
            66_800,
            77_400,
            126_500,
            117_400,
            102_800,
            88_600,
            142_600,
            486_000,
            352_000,
            194_600,
            79_000,
            27_400,
            12_200,
            4_900,
            2_100,
            1_000,
        )

    /** Bytes per tile at zooms 0 to 15, town (Draguignan, 8 km). */
    private val TOWN =
        longArrayOf(
            66_800,
            77_400,
            126_500,
            117_400,
            127_100,
            100_200,
            60_000,
            224_800,
            218_900,
            121_100,
            60_300,
            24_300,
            23_300,
            13_500,
            8_500,
            6_400,
        )

    /** Size bounds in bytes. */
    data class Range(val low: Long, val high: Long)

    fun rough(bounds: GeoBounds, minZoom: Int, maxZoom: Int): Range {
        require(maxZoom < COUNTRYSIDE.size) { "measured up to zoom ${COUNTRYSIDE.size - 1}" }
        var low = 0L
        var high = 0L
        TileCover.countsByZoom(bounds, minZoom, maxZoom).forEachIndexed { i, count ->
            val zoom = minZoom + i
            low += count * minOf(COUNTRYSIDE[zoom], TOWN[zoom])
            high += count * maxOf(COUNTRYSIDE[zoom], TOWN[zoom])
        }
        return Range(low, high)
    }
}
