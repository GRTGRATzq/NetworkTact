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
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** One corner of the download frame. */
enum class FrameCorner {
    TopLeft,
    TopRight,
    BottomLeft,
    BottomRight,
}

/**
 * The download frame drawn over the map, in dp from the map's top-left corner (docs/PLAN-CARTE-TELECHARGEMENT.md §6).
 * It stays where it is on the screen while the map moves under it; its corners are dragged to resize it.
 */
data class DownloadFrame(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float
        get() = right - left

    val height: Float
        get() = bottom - top

    /**
     * Moves [corner] by ([dx], [dy]), keeping the frame inside a [mapWidth] × [mapHeight] map and at least [minSize]
     * wide and high: a corner dragged past the opposite one stops there instead of turning the frame over.
     */
    fun dragCorner(
        corner: FrameCorner,
        dx: Float,
        dy: Float,
        mapWidth: Float,
        mapHeight: Float,
        minSize: Float,
    ): DownloadFrame {
        val movesLeft = corner == FrameCorner.TopLeft || corner == FrameCorner.BottomLeft
        val movesTop = corner == FrameCorner.TopLeft || corner == FrameCorner.TopRight
        val newLeft = if (movesLeft) (left + dx).coerceIn(0f, right - minSize) else left
        val newRight = if (movesLeft) right else (right + dx).coerceIn(left + minSize, mapWidth)
        val newTop = if (movesTop) (top + dy).coerceIn(0f, bottom - minSize) else top
        val newBottom = if (movesTop) bottom else (bottom + dy).coerceIn(top + minSize, mapHeight)
        return DownloadFrame(newLeft, newTop, newRight, newBottom)
    }

    companion object {
        /** The frame shown first: the visible map less [margin] of its width and height on every side. */
        fun initial(mapWidth: Float, mapHeight: Float, margin: Float = DEFAULT_MARGIN): DownloadFrame =
            DownloadFrame(mapWidth * margin, mapHeight * margin, mapWidth * (1 - margin), mapHeight * (1 - margin))

        /** The whole visible map ("Use the view"). */
        fun wholeView(mapWidth: Float, mapHeight: Float): DownloadFrame = DownloadFrame(0f, 0f, mapWidth, mapHeight)

        const val DEFAULT_MARGIN = 0.1f
    }
}

/** Whether an area can be downloaded. */
enum class AreaCheck {
    Ok,

    /** Narrower or lower than [DownloadArea.MIN_SIDE_METERS]. */
    TooSmall,

    /** The frame's corners could not be placed on the map (no viewport yet) or the area crosses the antimeridian. */
    Invalid,
}

/** The geographic side of the frame: its bounds from two projected corners, its size in kilometres, its validity. */
object DownloadArea {
    const val MIN_SIDE_METERS = 200.0
    private const val EARTH_RADIUS_METERS = 6_371_008.8
    private const val HALF_TURN = 180.0

    /**
     * The rectangle between the projected top-left and bottom-right corners of the frame ([lon, lat] each), or null
     * when the map could not project them or the area crosses the antimeridian (the map is north up while the frame is
     * shown, so these two corners are its north-west and south-east).
     */
    fun bounds(topLeft: Pair<Double, Double>?, bottomRight: Pair<Double, Double>?): GeoBounds? {
        if (topLeft == null || bottomRight == null) return null
        val (west, north) = topLeft
        val (east, south) = bottomRight
        val valid = west < east && south < north && west >= -HALF_TURN && east <= HALF_TURN
        return if (valid) {
            GeoBounds(west, south.coerceAtLeast(-MAX_LATITUDE), east, north.coerceAtMost(MAX_LATITUDE))
        } else {
            null
        }
    }

    /** East–west size at the centre latitude, in metres. */
    fun widthMeters(bounds: GeoBounds): Double {
        val latitude = (bounds.south + bounds.north) / 2
        return distanceMeters(latitude, bounds.west, latitude, bounds.east)
    }

    /** North–south size, in metres. */
    fun heightMeters(bounds: GeoBounds): Double = distanceMeters(bounds.south, bounds.west, bounds.north, bounds.west)

    fun check(bounds: GeoBounds?): AreaCheck = when {
        bounds == null -> AreaCheck.Invalid
        widthMeters(bounds) < MIN_SIDE_METERS || heightMeters(bounds) < MIN_SIDE_METERS -> AreaCheck.TooSmall
        else -> AreaCheck.Ok
    }

    /** Great-circle distance (haversine). */
    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val p1 = lat1 * PI / HALF_TURN
        val p2 = lat2 * PI / HALF_TURN
        val dp = p2 - p1
        val dl = (lon2 - lon1) * PI / HALF_TURN
        val h = sin(dp / 2) * sin(dp / 2) + cos(p1) * cos(p2) * sin(dl / 2) * sin(dl / 2)
        return 2 * EARTH_RADIUS_METERS * asin(sqrt(h.coerceIn(0.0, 1.0)))
    }

    private const val MAX_LATITUDE = 85.0511287798066
}
