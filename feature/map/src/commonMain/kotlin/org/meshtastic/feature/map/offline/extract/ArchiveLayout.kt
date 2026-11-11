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

import kotlin.math.roundToInt

/**
 * Where each section of a new archive goes. The order is the spec's: header, root directory, metadata, leaf
 * directories, tile data. It is known before a single tile is downloaded, which is what gives the exact size up front
 * and lets a download write each range in place and resume after a cut.
 */
class ArchiveLayout(rootLength: Long, val metadataLength: Long, leafDirectoriesLength: Long, val tileDataLength: Long) {
    val rootOffset: Long = PmTilesArchiveHeader.SIZE.toLong()
    val rootLength: Long = rootLength
    val metadataOffset: Long = rootOffset + rootLength
    val leafDirectoriesOffset: Long = metadataOffset + metadataLength
    val leafDirectoriesLength: Long = leafDirectoriesLength
    val tileDataOffset: Long = leafDirectoriesOffset + leafDirectoriesLength

    /** The exact length of the finished file. */
    val totalSize: Long = tileDataOffset + tileDataLength
}

/** Builds the header of the new archive: the source's tile format, the region's bounds and zooms, the plan's counts. */
object ExtractHeader {
    private const val E7 = 10_000_000.0

    fun of(
        source: PmTilesArchiveHeader,
        bounds: GeoBounds,
        minZoom: Int,
        maxZoom: Int,
        plan: ExtractPlan,
        layout: ArchiveLayout,
        internalCompression: Int,
    ): PmTilesArchiveHeader = PmTilesArchiveHeader(
        rootOffset = layout.rootOffset,
        rootLength = layout.rootLength,
        metadataOffset = layout.metadataOffset,
        metadataLength = layout.metadataLength,
        leafDirectoriesOffset = layout.leafDirectoriesOffset,
        leafDirectoriesLength = layout.leafDirectoriesLength,
        tileDataOffset = layout.tileDataOffset,
        tileDataLength = layout.tileDataLength,
        addressedTiles = plan.addressedTiles,
        tileEntries = plan.tileEntries,
        tileContents = plan.tileContents,
        clustered = true,
        internalCompression = internalCompression,
        tileCompression = source.tileCompression,
        tileType = source.tileType,
        minZoom = minZoom,
        maxZoom = maxZoom,
        minLonE7 = e7(bounds.west),
        minLatE7 = e7(bounds.south),
        maxLonE7 = e7(bounds.east),
        maxLatE7 = e7(bounds.north),
        centerZoom = source.centerZoom.coerceIn(minZoom, maxZoom),
        centerLonE7 = e7((bounds.west + bounds.east) / 2),
        centerLatE7 = e7((bounds.south + bounds.north) / 2),
    )

    private fun e7(degrees: Double): Int = (degrees * E7).roundToInt()
}
