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
 * Every field of the 127-byte PMTiles v3 header (spec §3), to read a remote archive and to write a local one.
 *
 * [org.meshtastic.feature.map.offline.PmTilesHeader] stays the import's light check; this one also carries the
 * directory and tile-data sections, the counts and the clustered flag that an extract needs. Positions are in degrees
 * times 10 000 000, as stored.
 */
data class PmTilesArchiveHeader(
    val rootOffset: Long,
    val rootLength: Long,
    val metadataOffset: Long,
    val metadataLength: Long,
    val leafDirectoriesOffset: Long,
    val leafDirectoriesLength: Long,
    val tileDataOffset: Long,
    val tileDataLength: Long,
    val addressedTiles: Long,
    val tileEntries: Long,
    val tileContents: Long,
    val clustered: Boolean,
    val internalCompression: Int,
    val tileCompression: Int,
    val tileType: Int,
    val minZoom: Int,
    val maxZoom: Int,
    val minLonE7: Int,
    val minLatE7: Int,
    val maxLonE7: Int,
    val maxLatE7: Int,
    val centerZoom: Int,
    val centerLonE7: Int,
    val centerLatE7: Int,
) {
    fun toBytes(): ByteArray {
        val bytes = ByteArray(SIZE)
        MAGIC.encodeToByteArray().copyInto(bytes)
        bytes[VERSION_AT] = VERSION.toByte()
        val longs =
            longArrayOf(
                rootOffset,
                rootLength,
                metadataOffset,
                metadataLength,
                leafDirectoriesOffset,
                leafDirectoriesLength,
                tileDataOffset,
                tileDataLength,
                addressedTiles,
                tileEntries,
                tileContents,
            )
        longs.forEachIndexed { i, value -> bytes.putLong(ROOT_OFFSET_AT + i * LONG_BYTES, value) }
        bytes[CLUSTERED_AT] = if (clustered) 1 else 0
        bytes[INTERNAL_COMPRESSION_AT] = internalCompression.toByte()
        bytes[TILE_COMPRESSION_AT] = tileCompression.toByte()
        bytes[TILE_TYPE_AT] = tileType.toByte()
        bytes[MIN_ZOOM_AT] = minZoom.toByte()
        bytes[MAX_ZOOM_AT] = maxZoom.toByte()
        bytes.putInt(MIN_POSITION_AT, minLonE7)
        bytes.putInt(MIN_POSITION_AT + INT_BYTES, minLatE7)
        bytes.putInt(MAX_POSITION_AT, maxLonE7)
        bytes.putInt(MAX_POSITION_AT + INT_BYTES, maxLatE7)
        bytes[CENTER_ZOOM_AT] = centerZoom.toByte()
        bytes.putInt(CENTER_POSITION_AT, centerLonE7)
        bytes.putInt(CENTER_POSITION_AT + INT_BYTES, centerLatE7)
        return bytes
    }

    companion object {
        const val SIZE = 127
        const val VERSION = 3
        private const val MAGIC = "PMTiles"
        private const val VERSION_AT = 7
        private const val ROOT_OFFSET_AT = 8
        private const val LONG_FIELDS = 11
        private const val CLUSTERED_AT = 96
        private const val INTERNAL_COMPRESSION_AT = 97
        private const val TILE_COMPRESSION_AT = 98
        private const val TILE_TYPE_AT = 99
        private const val MIN_ZOOM_AT = 100
        private const val MAX_ZOOM_AT = 101
        private const val MIN_POSITION_AT = 102
        private const val MAX_POSITION_AT = 110
        private const val CENTER_ZOOM_AT = 118
        private const val CENTER_POSITION_AT = 119
        private const val LONG_BYTES = 8
        private const val INT_BYTES = 4
        private const val BYTE_MASK = 0xFF
        private const val LONG_BYTE_MASK = 0xFFL
        private const val BITS_PER_BYTE = 8

        /** Reads the first [SIZE] bytes of an archive; refuses anything but a PMTiles v3 header. */
        fun parse(bytes: ByteArray): PmTilesArchiveHeader {
            requireFormat(bytes.size >= SIZE && bytes.decodeToString(0, MAGIC.length) == MAGIC) {
                "not a PMTiles archive"
            }
            val version = bytes[VERSION_AT].toInt() and BYTE_MASK
            requireFormat(version == VERSION) { "PMTiles version $version, 3 expected" }
            val longs = LongArray(LONG_FIELDS) { bytes.getLong(ROOT_OFFSET_AT + it * LONG_BYTES) }
            requireFormat(longs.none { it < 0 }) { "header field beyond 2^63" }
            return PmTilesArchiveHeader(
                rootOffset = longs[0],
                rootLength = longs[1],
                metadataOffset = longs[2],
                metadataLength = longs[3],
                leafDirectoriesOffset = longs[4],
                leafDirectoriesLength = longs[5],
                tileDataOffset = longs[6],
                tileDataLength = longs[7],
                addressedTiles = longs[8],
                tileEntries = longs[9],
                tileContents = longs[10],
                clustered = bytes[CLUSTERED_AT].toInt() == 1,
                internalCompression = bytes[INTERNAL_COMPRESSION_AT].toInt() and BYTE_MASK,
                tileCompression = bytes[TILE_COMPRESSION_AT].toInt() and BYTE_MASK,
                tileType = bytes[TILE_TYPE_AT].toInt() and BYTE_MASK,
                minZoom = bytes[MIN_ZOOM_AT].toInt() and BYTE_MASK,
                maxZoom = bytes[MAX_ZOOM_AT].toInt() and BYTE_MASK,
                minLonE7 = bytes.getInt(MIN_POSITION_AT),
                minLatE7 = bytes.getInt(MIN_POSITION_AT + INT_BYTES),
                maxLonE7 = bytes.getInt(MAX_POSITION_AT),
                maxLatE7 = bytes.getInt(MAX_POSITION_AT + INT_BYTES),
                centerZoom = bytes[CENTER_ZOOM_AT].toInt() and BYTE_MASK,
                centerLonE7 = bytes.getInt(CENTER_POSITION_AT),
                centerLatE7 = bytes.getInt(CENTER_POSITION_AT + INT_BYTES),
            )
        }

        private fun ByteArray.putLong(at: Int, value: Long) {
            for (i in 0 until LONG_BYTES) this[at + i] = (value ushr (BITS_PER_BYTE * i)).toByte()
        }

        private fun ByteArray.putInt(at: Int, value: Int) {
            for (i in 0 until INT_BYTES) this[at + i] = (value ushr (BITS_PER_BYTE * i)).toByte()
        }

        private fun ByteArray.getLong(at: Int): Long {
            var value = 0L
            for (i in LONG_BYTES - 1 downTo 0) {
                value = (value shl BITS_PER_BYTE) or (this[at + i].toLong() and LONG_BYTE_MASK)
            }
            return value
        }

        private fun ByteArray.getInt(at: Int): Int {
            var value = 0
            for (i in INT_BYTES - 1 downTo 0) value = (value shl BITS_PER_BYTE) or (this[at + i].toInt() and BYTE_MASK)
            return value
        }
    }
}
