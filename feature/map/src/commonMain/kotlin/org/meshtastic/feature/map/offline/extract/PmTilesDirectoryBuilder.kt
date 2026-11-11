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

/** The compressed root directory and leaf directories of a new archive, ready to be written. */
class PmTilesDirectories(val root: ByteArray, val leaves: ByteArray, val leafCount: Int)

/**
 * Lays out the directories of a new archive from its sorted tile entries, the way `pmtiles` writers do: a single root
 * directory when it fits, otherwise leaf directories of [INITIAL_LEAF_ENTRIES] entries, grown by a fifth until the root
 * that points to them fits.
 *
 * The spec caps header + compressed root at 16 384 bytes, so a reader gets both in one request. The probe measured that
 * the world build's root (2 917 entries) takes 15 567 bytes; a 5 500-tile region fits in a root of its own, a 20
 * 000-tile one needs leaves.
 */
object PmTilesDirectoryBuilder {
    const val MAX_ROOT_BYTES = 16_384 - PmTilesArchiveHeader.SIZE
    private const val INITIAL_LEAF_ENTRIES = 4096
    private const val GROWTH_NUMERATOR = 6
    private const val GROWTH_DENOMINATOR = 5

    fun build(entries: List<PmTilesEntry>, compression: Int): PmTilesDirectories {
        require(entries.isNotEmpty()) { "an archive holds at least one tile" }
        require(entries.none { it.isLeaf }) { "tile entries only" }
        val single = PmTilesCompression.compress(PmTilesDirectory.encode(entries), compression)
        if (single.size <= MAX_ROOT_BYTES) return PmTilesDirectories(single, ByteArray(0), 0)

        var leafSize = INITIAL_LEAF_ENTRIES
        while (true) {
            val chunks = entries.chunked(leafSize)
            val leaves = ArrayList<ByteArray>(chunks.size)
            val rootEntries = ArrayList<PmTilesEntry>(chunks.size)
            var offset = 0L
            for (chunk in chunks) {
                val leaf = PmTilesCompression.compress(PmTilesDirectory.encode(chunk), compression)
                rootEntries += PmTilesEntry(chunk.first().tileId, offset, leaf.size.toLong(), runLength = 0)
                leaves += leaf
                offset += leaf.size
            }
            val root = PmTilesCompression.compress(PmTilesDirectory.encode(rootEntries), compression)
            if (root.size <= MAX_ROOT_BYTES) {
                val joined = ByteArray(offset.toInt())
                var at = 0
                for (leaf in leaves) {
                    leaf.copyInto(joined, at)
                    at += leaf.size
                }
                return PmTilesDirectories(root, joined, leaves.size)
            }
            leafSize = leafSize * GROWTH_NUMERATOR / GROWTH_DENOMINATOR
        }
    }
}
