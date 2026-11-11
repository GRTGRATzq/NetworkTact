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

import org.meshtastic.feature.map.offline.PmTilesCheck
import org.meshtastic.feature.map.offline.PmTilesHeader
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The whole extract in memory: the same steps the downloader will take, against a [SyntheticPmTiles] source. */
class ExtractPlannerTest {

    private val cincu = GeoBounds(24.4126, 45.6475, 25.1874, 46.1865)
    private val wider = GeoBounds(23.9, 45.3, 25.7, 46.5)

    /**
     * Every tile of [wider] from zoom 0 to 12, plus a far-away tile; at zoom 12 the western columns are "sea" (one
     * shared content, a run in the directory), and a few tiles are missing as in a real build.
     */
    private fun source(leafSize: Int = 50): SyntheticPmTiles {
        val tiles = HashMap<Long, ByteArray>()
        for (zoom in 0..12) {
            val range = TileCover.rangeAt(wider, zoom)
            for (x in range.minX..range.maxX) {
                for (y in range.minY..range.maxY) {
                    if (zoom == 12 && (x + y) % 17 == 0) continue
                    val sea = zoom == 12 && x < range.minX + 3
                    val content = if (sea) "sea" else "tile $zoom/$x/$y " + "x".repeat((x * 7 + y * 13) % 300)
                    tiles[PmTilesTileId.of(zoom, x, y)] = content.encodeToByteArray()
                }
            }
        }
        tiles[PmTilesTileId.of(12, 100, 100)] = "far away".encodeToByteArray()
        return SyntheticPmTiles(
            tiles,
            """{"name":"Protomaps Basemap","version":"4.15.2"}""".encodeToByteArray(),
            leafSize,
        )
    }

    private class Extracted(
        val bytes: ByteArray,
        val plan: ExtractPlan,
        val ranges: List<FetchRange>,
        val leafReads: Int,
    )

    private fun extract(source: SyntheticPmTiles, bounds: GeoBounds, minZoom: Int, maxZoom: Int): Extracted {
        val header = source.header
        fun directory(offset: Long, length: Long) = PmTilesDirectory.decode(
            PmTilesCompression.decompress(source.read(offset, length), header.internalCompression),
        )
        val found = ArrayList<PmTilesEntry>()
        var pending =
            listOf(directory(header.rootOffset, header.rootLength) to TileCover.tileIds(bounds, minZoom, maxZoom))
        var leafReads = 0
        while (pending.isNotEmpty()) {
            val next = ArrayList<Pair<List<PmTilesEntry>, LongArray>>()
            for ((dir, wanted) in pending) {
                val relevant = ExtractPlanner.relevant(dir, wanted)
                found += relevant.tiles
                for (request in relevant.leaves) {
                    leafReads++
                    next +=
                        directory(header.leafDirectoriesOffset + request.leaf.offset, request.leaf.length) to
                        request.wanted
                }
            }
            pending = next
        }
        val plan = ExtractPlanner.plan(found)
        val directories = PmTilesDirectoryBuilder.build(plan.entries, header.internalCompression)
        val metadata = source.read(header.metadataOffset, header.metadataLength)
        val layout =
            ArchiveLayout(
                directories.root.size.toLong(),
                metadata.size.toLong(),
                directories.leaves.size.toLong(),
                plan.tileDataLength,
            )
        val out = ByteArray(layout.totalSize.toInt())
        ExtractHeader.of(header, bounds, minZoom, maxZoom, plan, layout, header.internalCompression)
            .toBytes()
            .copyInto(out)
        directories.root.copyInto(out, layout.rootOffset.toInt())
        metadata.copyInto(out, layout.metadataOffset.toInt())
        directories.leaves.copyInto(out, layout.leafDirectoriesOffset.toInt())
        val ranges = RangeMerger.merge(plan.copies)
        for (range in ranges) {
            val fetched = source.read(header.tileDataOffset + range.sourceOffset, range.length)
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
        return Extracted(out, plan, ranges, leafReads)
    }

    @Test
    fun everyWantedTileReadsTheSameAsInTheSourceAndNothingElseIsKept() {
        val source = source()
        val result = extract(source, cincu, 0, 12)
        for (id in TileCover.tileIds(cincu, 0, 12)) {
            val expected = SyntheticPmTiles.lookUp(source.bytes, id)
            val actual = SyntheticPmTiles.lookUp(result.bytes, id)
            if (expected == null) assertNull(actual, "tile $id") else assertContentEquals(expected, actual, "tile $id")
        }
        assertNotNull(SyntheticPmTiles.lookUp(source.bytes, PmTilesTileId.of(12, 100, 100)))
        assertNull(SyntheticPmTiles.lookUp(result.bytes, PmTilesTileId.of(12, 100, 100)))
        val westernmostOfWider = TileCover.rangeAt(wider, 12).minX
        assertNull(
            SyntheticPmTiles.lookUp(
                result.bytes,
                PmTilesTileId.of(12, westernmostOfWider, TileCover.rangeAt(cincu, 12).minY),
            ),
        )
    }

    @Test
    fun theFileIsWhatTheImportCheckExpects() {
        val result = extract(source(), cincu, 0, 12)
        val header = PmTilesArchiveHeader.parse(result.bytes.copyOf(PmTilesArchiveHeader.SIZE))
        assertTrue(header.clustered)
        assertEquals(0, header.minZoom)
        assertEquals(12, header.maxZoom)
        assertEquals(result.plan.addressedTiles, header.addressedTiles)
        val checked = assertIs<PmTilesCheck.Valid>(PmTilesHeader.check(result.bytes.copyOf(PmTilesHeader.SIZE))).header
        assertEquals(result.bytes.size.toLong(), checked.minimumFileSize)
        assertEquals(24.4126, checked.minLongitude, 1e-7)
        assertEquals(46.1865, checked.maxLatitude, 1e-7)
    }

    @Test
    fun sharedContentIsCopiedOnceAndRunsStayRuns() {
        val plan = extract(source(), wider, 12, 12).plan
        assertTrue(
            plan.tileContents < plan.addressedTiles,
            "${plan.tileContents} contents, ${plan.addressedTiles} tiles",
        )
        assertTrue(plan.entries.any { it.runLength > 1 }, "the sea stays a run")
        assertEquals(plan.copies.sumOf { it.length }, plan.tileDataLength)
    }

    @Test
    fun onlyTheLeavesHoldingWantedTilesAreRead() {
        val source = source(leafSize = 50)
        val leafCount = (source.entries.size + 49) / 50
        val small = extract(source, GeoBounds(24.79, 45.91, 24.81, 45.92), 12, 12)
        assertTrue(small.leafReads in 1..2, "${small.leafReads} of $leafCount leaves read")
    }

    @Test
    fun fewRequestsAndLittleOverfetch() {
        val result = extract(source(), cincu, 0, 12)
        val useful = result.plan.tileDataLength
        val fetched = result.ranges.sumOf { it.length }
        assertTrue(
            result.ranges.size < result.plan.copies.size / 5,
            "${result.ranges.size} requests for ${result.plan.copies.size} contents",
        )
        assertTrue(fetched <= useful + useful / 20 + 1, "$fetched bytes fetched for $useful useful")
    }

    @Test
    fun aRegionWithoutTilesGivesAnEmptyPlan() {
        val plan = ExtractPlanner.plan(emptyList())
        assertEquals(0L, plan.tileDataLength)
        assertTrue(RangeMerger.merge(plan.copies).isEmpty())
    }
}
