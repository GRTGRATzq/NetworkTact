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

/** A leaf directory to read, with the wanted tile ids that fall in it (sorted). */
class LeafRequest(val leaf: PmTilesEntry, val wanted: LongArray)

/** What one directory says about the wanted tiles: entries found here, and leaf directories still to read. */
class RelevantEntries(val tiles: List<PmTilesEntry>, val leaves: List<LeafRequest>)

/** Copy [length] bytes from [sourceOffset] of the source tile data to [targetOffset] of the new tile data. */
data class TileCopy(val sourceOffset: Long, val targetOffset: Long, val length: Long)

/**
 * The tile part of a new archive: its directory entries (sorted, offsets in the new tile data) and the byte copies that
 * fill its tile data from the source. Each distinct content is copied once, even when several tiles share it (sea,
 * empty land), so [tileContents] is at most [tileEntries].
 */
class ExtractPlan(
    val entries: List<PmTilesEntry>,
    val copies: List<TileCopy>,
    val tileDataLength: Long,
    val addressedTiles: Long,
) {
    val tileEntries: Long
        get() = entries.size.toLong()

    val tileContents: Long
        get() = copies.size.toLong()
}

/**
 * Steps 2 to 4 of an extract (`pmtiles extract`, see docs/PLAN-CARTE-TELECHARGEMENT.md §4), without any network: the
 * caller reads the directories and passes them here.
 */
object ExtractPlanner {

    /**
     * Looks up the sorted [wanted] tile ids in [directory] (the root, or a leaf). A wanted id inside a tile entry's run
     * is found here; one under a leaf entry goes to that leaf's request; one in no entry is a tile the source does not
     * have, and is left out. Consecutive wanted ids of the same run become a single entry.
     */
    fun relevant(directory: List<PmTilesEntry>, wanted: LongArray): RelevantEntries {
        val ids = LongArray(directory.size) { directory[it].tileId }
        val tiles = ArrayList<PmTilesEntry>()
        val leafIds = LinkedHashMap<Int, ArrayList<Long>>()
        var open: PmTilesEntry? = null
        var openIndex = -1
        for (id in wanted) {
            val index = ids.binarySearchFloor(id)
            val entry = directory.getOrNull(index)
            val current = open
            when {
                entry == null || (!entry.isLeaf && id !in entry) -> Unit

                // the source has no such tile
                entry.isLeaf -> leafIds.getOrPut(index) { ArrayList() } += id

                current != null && openIndex == index && current.tileId + current.runLength == id ->
                    open = current.copy(runLength = current.runLength + 1)

                else -> {
                    current?.let { tiles += it }
                    open = PmTilesEntry(id, entry.offset, entry.length, runLength = 1)
                    openIndex = index
                }
            }
        }
        open?.let { tiles += it }
        val leaves = leafIds.map { (index, wantedHere) -> LeafRequest(directory[index], wantedHere.toLongArray()) }
        return RelevantEntries(tiles, leaves)
    }

    /**
     * Re-encodes the found [tiles] (all levels together, any order) for the new archive: sorted by tile id, contents
     * placed in the order they first appear so the archive stays clustered, a content shared by several tiles copied
     * once.
     */
    fun plan(tiles: List<PmTilesEntry>): ExtractPlan {
        val sorted = tiles.sortedBy { it.tileId }
        val placed = HashMap<Long, TileCopy>()
        val copies = ArrayList<TileCopy>()
        val entries = ArrayList<PmTilesEntry>(sorted.size)
        var tileData = 0L
        var addressed = 0L
        for (tile in sorted) {
            require(entries.isEmpty() || tile.tileId >= entries.last().tileId + entries.last().runLength) {
                "overlapping tile entries at ${tile.tileId}"
            }
            val copy =
                placed.getOrPut(tile.offset) {
                    TileCopy(tile.offset, tileData, tile.length).also {
                        copies += it
                        tileData += tile.length
                    }
                }
            requireFormat(copy.length == tile.length) { "two lengths for the content at ${tile.offset}" }
            val target = copy.targetOffset
            addressed += tile.runLength
            val last = entries.lastOrNull()
            if (last != null && last.offset == target && last.tileId + last.runLength == tile.tileId) {
                entries[entries.size - 1] = last.copy(runLength = last.runLength + tile.runLength)
            } else {
                entries += PmTilesEntry(tile.tileId, target, tile.length, tile.runLength)
            }
        }
        return ExtractPlan(entries, copies, tileData, addressed)
    }

    /** Index of the last element ≤ [value] in this ascending array, or -1. */
    private fun LongArray.binarySearchFloor(value: Long): Int {
        var low = 0
        var high = size - 1
        var found = -1
        while (low <= high) {
            val middle = (low + high) ushr 1
            if (this[middle] <= value) {
                found = middle
                low = middle + 1
            } else {
                high = middle - 1
            }
        }
        return found
    }
}
