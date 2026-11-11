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
 * The steps of an extract computed in memory against a [SyntheticPmTiles] source, shared by the tests: wanted tiles,
 * directories read level by level, plan, new directories, layout, header and fetch ranges.
 */
class ExtractFixture(val source: SyntheticPmTiles, val bounds: GeoBounds, val minZoom: Int, val maxZoom: Int) {
    val plan: ExtractPlan
    val leafReads: Int
    val directories: PmTilesDirectories
    val metadata: ByteArray
    val layout: ArchiveLayout
    val header: PmTilesArchiveHeader
    val ranges: List<FetchRange>

    init {
        val sourceHeader = source.header
        fun directory(offset: Long, length: Long) = PmTilesDirectory.decode(
            PmTilesCompression.decompress(source.read(offset, length), sourceHeader.internalCompression),
        )
        val found = ArrayList<PmTilesEntry>()
        var pending =
            listOf(
                directory(sourceHeader.rootOffset, sourceHeader.rootLength) to
                    TileCover.tileIds(bounds, minZoom, maxZoom),
            )
        var reads = 0
        while (pending.isNotEmpty()) {
            val next = ArrayList<Pair<List<PmTilesEntry>, LongArray>>()
            for ((dir, wanted) in pending) {
                val relevant = ExtractPlanner.relevant(dir, wanted)
                found += relevant.tiles
                for (request in relevant.leaves) {
                    reads++
                    next +=
                        directory(sourceHeader.leafDirectoriesOffset + request.leaf.offset, request.leaf.length) to
                        request.wanted
                }
            }
            pending = next
        }
        leafReads = reads
        plan = ExtractPlanner.plan(found)
        directories = PmTilesDirectoryBuilder.build(plan.entries, sourceHeader.internalCompression)
        metadata = source.read(sourceHeader.metadataOffset, sourceHeader.metadataLength)
        layout =
            ArchiveLayout(
                directories.root.size.toLong(),
                metadata.size.toLong(),
                directories.leaves.size.toLong(),
                plan.tileDataLength,
            )
        header =
            ExtractHeader.of(sourceHeader, bounds, minZoom, maxZoom, plan, layout, sourceHeader.internalCompression)
        ranges = RangeMerger.merge(plan.copies)
    }

    /** The source bytes of [range]. */
    fun fetch(range: FetchRange): ByteArray =
        source.read(source.header.tileDataOffset + range.sourceOffset, range.length)

    /** The finished archive, assembled in memory. */
    fun bytes(): ByteArray {
        val out = ByteArray(layout.totalSize.toInt())
        header.toBytes().copyInto(out)
        directories.root.copyInto(out, layout.rootOffset.toInt())
        metadata.copyInto(out, layout.metadataOffset.toInt())
        directories.leaves.copyInto(out, layout.leafDirectoriesOffset.toInt())
        for (range in ranges) {
            val fetched = fetch(range)
            for (copy in range.copies) {
                val from = (copy.sourceOffset - range.sourceOffset).toInt()
                fetched.copyInto(
                    out,
                    (layout.tileDataOffset + copy.targetOffset).toInt(),
                    from,
                    from + copy.length.toInt(),
                )
            }
        }
        return out
    }

    companion object {
        val cincu = GeoBounds(24.4126, 45.6475, 25.1874, 46.1865)
        val wider = GeoBounds(23.9, 45.3, 25.7, 46.5)

        /**
         * Every tile of [wider] from zoom 0 to 12, plus a far-away tile; at zoom 12 the western columns are "sea" (one
         * shared content, a run in the directory), and a few tiles are missing as in a real build.
         */
        fun source(leafSize: Int = 50): SyntheticPmTiles {
            val tiles = HashMap<Long, ByteArray>()
            for (zoom in 0..12) {
                val range = TileCover.rangeAt(wider, zoom)
                for (x in range.minX..range.maxX) {
                    for (y in range.minY..range.maxY) {
                        if (zoom == 12 && (x + y) % 17 == 0) continue
                        val sea = zoom == 12 && x < range.minX + 3
                        val content = if (sea) "sea" else "tile $zoom/$x/$y " + "x".repeat((x * 7 + y * 13) % 300)
                        tiles[PmTilesTileId.of(zoom, x, y)] = gzipped(content)
                    }
                }
            }
            tiles[PmTilesTileId.of(12, 100, 100)] = gzipped("far away")
            val metadata = """{"name":"Protomaps Basemap","version":"4.15.2"}""".encodeToByteArray()
            return SyntheticPmTiles(tiles, metadata, leafSize)
        }

        /** Tiles are gzip in the Protomaps builds; the final check looks for it. */
        private fun gzipped(text: String) =
            PmTilesCompression.compress(text.encodeToByteArray(), PmTilesCompression.GZIP)
    }
}
