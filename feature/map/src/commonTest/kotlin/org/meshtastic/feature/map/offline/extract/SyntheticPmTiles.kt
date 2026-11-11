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
 * A small PMTiles v3 archive built in memory for the tests, laid out like the Protomaps builds the probe read: tile
 * data first, then metadata and leaf directories at the end, gzip directories, clustered contents, some contents shared
 * by several tiles and one long run (the sea).
 */
class SyntheticPmTiles(tiles: Map<Long, ByteArray>, val metadata: ByteArray, leafSize: Int) {
    val entries: List<PmTilesEntry>
    val bytes: ByteArray
    val header: PmTilesArchiveHeader

    init {
        val tileData = ArrayList<Byte>()
        val placed = HashMap<String, Long>()
        val list = ArrayList<PmTilesEntry>()
        for ((id, content) in tiles.toSortedMap()) {
            val key = content.decodeToString()
            val offset = placed.getOrPut(key) { tileData.size.toLong().also { tileData.addAll(content.toList()) } }
            val last = list.lastOrNull()
            if (last != null && last.offset == offset && last.tileId + last.runLength == id) {
                list[list.size - 1] = last.copy(runLength = last.runLength + 1)
            } else {
                list += PmTilesEntry(id, offset, content.size.toLong(), 1)
            }
        }
        entries = list
        val gzip = PmTilesCompression.GZIP
        val leaves = ArrayList<Byte>()
        val root = ArrayList<PmTilesEntry>()
        for (chunk in list.chunked(leafSize)) {
            val leaf = PmTilesCompression.compress(PmTilesDirectory.encode(chunk), gzip)
            root += PmTilesEntry(chunk.first().tileId, leaves.size.toLong(), leaf.size.toLong(), 0)
            leaves.addAll(leaf.toList())
        }
        val rootBytes = PmTilesCompression.compress(PmTilesDirectory.encode(root), gzip)
        val meta = PmTilesCompression.compress(metadata, gzip)
        val tileDataOffset = PmTilesArchiveHeader.SIZE.toLong() + rootBytes.size
        val metadataOffset = tileDataOffset + tileData.size
        header =
            PmTilesArchiveHeader(
                rootOffset = PmTilesArchiveHeader.SIZE.toLong(),
                rootLength = rootBytes.size.toLong(),
                metadataOffset = metadataOffset,
                metadataLength = meta.size.toLong(),
                leafDirectoriesOffset = metadataOffset + meta.size,
                leafDirectoriesLength = leaves.size.toLong(),
                tileDataOffset = tileDataOffset,
                tileDataLength = tileData.size.toLong(),
                addressedTiles = tiles.size.toLong(),
                tileEntries = list.size.toLong(),
                tileContents = placed.size.toLong(),
                clustered = true,
                internalCompression = gzip,
                tileCompression = gzip,
                tileType = 1,
                minZoom = 0,
                maxZoom = 15,
                minLonE7 = -1_800_000_000,
                minLatE7 = -850_511_287,
                maxLonE7 = 1_800_000_000,
                maxLatE7 = 850_511_287,
                centerZoom = 0,
                centerLonE7 = 0,
                centerLatE7 = 0,
            )
        bytes = header.toBytes() + rootBytes + tileData.toByteArray() + meta + leaves.toByteArray()
    }

    fun read(offset: Long, length: Long): ByteArray = bytes.copyOfRange(offset.toInt(), (offset + length).toInt())

    companion object {
        /** Reads the tile [id] of a complete archive held in [bytes], or null when it has none. */
        fun lookUp(bytes: ByteArray, id: Long): ByteArray? {
            val header = PmTilesArchiveHeader.parse(bytes.copyOf(PmTilesArchiveHeader.SIZE))
            fun slice(offset: Long, length: Long) = bytes.copyOfRange(offset.toInt(), (offset + length).toInt())
            var directory =
                PmTilesDirectory.decode(
                    PmTilesCompression.decompress(
                        slice(header.rootOffset, header.rootLength),
                        header.internalCompression,
                    ),
                )
            while (true) {
                val entry = directory.lastOrNull { it.tileId <= id } ?: return null
                if (!entry.isLeaf) {
                    return if (id in entry) slice(header.tileDataOffset + entry.offset, entry.length) else null
                }
                directory =
                    PmTilesDirectory.decode(
                        PmTilesCompression.decompress(
                            slice(header.leafDirectoriesOffset + entry.offset, entry.length),
                            header.internalCompression,
                        ),
                    )
            }
        }
    }
}
