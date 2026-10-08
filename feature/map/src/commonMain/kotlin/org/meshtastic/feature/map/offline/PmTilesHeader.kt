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
package org.meshtastic.feature.map.offline

/**
 * The fixed 127-byte header at the start of a PMTiles v3 archive (https://github.com/protomaps/PMTiles, spec v3).
 *
 * Read before an import copies anything, so a file that is not a vector map is refused up front rather than after a
 * copy of several hundred megabytes, and checked again on the copy, so a truncated copy is never kept.
 */
data class PmTilesHeader(
    val tileType: Int,
    val minZoom: Int,
    val maxZoom: Int,
    val minLongitude: Double,
    val minLatitude: Double,
    val maxLongitude: Double,
    val maxLatitude: Double,
    /** Where the archive's last section ends: a complete file is at least this long. */
    val minimumFileSize: Long,
) {
    companion object {
        /** Bytes to read from the start of a file before calling [check]. */
        const val SIZE = 127

        /** Tile type of Mapbox Vector Tiles, the only kind the bundled Protomaps style can draw. */
        const val TILE_TYPE_MVT = 1

        private const val MAGIC = "PMTiles"
        private const val SPEC_VERSION = 3
        private const val VERSION_OFFSET = 7
        private const val ROOT_DIRECTORY_OFFSET = 8
        private const val TILE_TYPE_OFFSET = 99
        private const val MIN_ZOOM_OFFSET = 100
        private const val MAX_ZOOM_OFFSET = 101
        private const val MIN_LONGITUDE_OFFSET = 102
        private const val MIN_LATITUDE_OFFSET = 106
        private const val MAX_LONGITUDE_OFFSET = 110
        private const val MAX_LATITUDE_OFFSET = 114

        /** Offset/length pairs of the root directory, metadata, leaf directories and tile data. */
        private const val SECTION_COUNT = 4
        private const val SECTION_FIELD_BYTES = 16
        private const val LENGTH_FIELD_OFFSET = 8
        private const val COORDINATE_SCALE = 10_000_000.0
        private const val BYTE_MASK = 0xFF
        private const val BITS_PER_BYTE = 8
        private const val INT_BYTES = 4
        private const val LONG_BYTES = 8

        /** Reads [bytes], the first [SIZE] bytes of a file, and says whether it is a PMTiles v3 vector map. */
        fun check(bytes: ByteArray): PmTilesCheck {
            val complete = bytes.size >= SIZE
            val sectionEnds = if (complete) sectionEnds(bytes) else emptyList()
            return when {
                !complete || bytes.decodeToString(0, MAGIC.length) != MAGIC -> PmTilesCheck.NotPmTiles
                bytes[VERSION_OFFSET].toInt() != SPEC_VERSION -> PmTilesCheck.UnsupportedVersion
                (bytes[TILE_TYPE_OFFSET].toInt() and BYTE_MASK) != TILE_TYPE_MVT -> PmTilesCheck.NotVectorTiles
                sectionEnds.any { it < 0 } -> PmTilesCheck.NotPmTiles
                else -> PmTilesCheck.Valid(read(bytes, minimumFileSize = maxOf(SIZE.toLong(), sectionEnds.max())))
            }
        }

        /** Where each of the root directory, metadata, leaf directories and tile data ends; negative on overflow. */
        private fun sectionEnds(bytes: ByteArray): List<Long> = (0 until SECTION_COUNT).map { index ->
            val offset = ROOT_DIRECTORY_OFFSET + index * SECTION_FIELD_BYTES
            bytes.uint64(offset) + bytes.uint64(offset + LENGTH_FIELD_OFFSET)
        }

        private fun read(bytes: ByteArray, minimumFileSize: Long) = PmTilesHeader(
            tileType = bytes[TILE_TYPE_OFFSET].toInt() and BYTE_MASK,
            minZoom = bytes[MIN_ZOOM_OFFSET].toInt() and BYTE_MASK,
            maxZoom = bytes[MAX_ZOOM_OFFSET].toInt() and BYTE_MASK,
            minLongitude = bytes.int32(MIN_LONGITUDE_OFFSET) / COORDINATE_SCALE,
            minLatitude = bytes.int32(MIN_LATITUDE_OFFSET) / COORDINATE_SCALE,
            maxLongitude = bytes.int32(MAX_LONGITUDE_OFFSET) / COORDINATE_SCALE,
            maxLatitude = bytes.int32(MAX_LATITUDE_OFFSET) / COORDINATE_SCALE,
            minimumFileSize = minimumFileSize,
        )

        private fun ByteArray.uint64(offset: Int): Long {
            var value = 0L
            for (i in LONG_BYTES - 1 downTo 0) {
                value = (value shl BITS_PER_BYTE) or (this[offset + i].toLong() and BYTE_MASK.toLong())
            }
            return value
        }

        private fun ByteArray.int32(offset: Int): Int {
            var value = 0
            for (i in INT_BYTES - 1 downTo 0) {
                value = (value shl BITS_PER_BYTE) or (this[offset + i].toInt() and BYTE_MASK)
            }
            return value
        }
    }
}

/** What [PmTilesHeader.check] found. */
sealed interface PmTilesCheck {
    data class Valid(val header: PmTilesHeader) : PmTilesCheck

    /** Not a PMTiles file at all, or too short to hold the header. */
    data object NotPmTiles : PmTilesCheck

    /** A PMTiles file of an older or newer format than v3. */
    data object UnsupportedVersion : PmTilesCheck

    /** A raster or terrain archive: the bundled style draws vector tiles only. */
    data object NotVectorTiles : PmTilesCheck
}
