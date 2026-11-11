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
import org.meshtastic.feature.map.offline.extract.ExtractFixture.Companion.cincu
import org.meshtastic.feature.map.offline.extract.ExtractFixture.Companion.source
import org.meshtastic.feature.map.offline.extract.ExtractFixture.Companion.wider
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The whole extract in memory: the same steps the downloader takes, against a [SyntheticPmTiles] source. */
class ExtractPlannerTest {

    @Test
    fun everyWantedTileReadsTheSameAsInTheSourceAndNothingElseIsKept() {
        val source = source()
        val bytes = ExtractFixture(source, cincu, 0, 12).bytes()
        for (id in TileCover.tileIds(cincu, 0, 12)) {
            val expected = SyntheticPmTiles.lookUp(source.bytes, id)
            val actual = SyntheticPmTiles.lookUp(bytes, id)
            if (expected == null) assertNull(actual, "tile $id") else assertContentEquals(expected, actual, "tile $id")
        }
        assertNotNull(SyntheticPmTiles.lookUp(source.bytes, PmTilesTileId.of(12, 100, 100)))
        assertNull(SyntheticPmTiles.lookUp(bytes, PmTilesTileId.of(12, 100, 100)))
        val outside = PmTilesTileId.of(12, TileCover.rangeAt(wider, 12).minX, TileCover.rangeAt(cincu, 12).minY)
        assertNotNull(SyntheticPmTiles.lookUp(source.bytes, outside))
        assertNull(SyntheticPmTiles.lookUp(bytes, outside))
    }

    @Test
    fun theFileIsWhatTheImportCheckExpects() {
        val fixture = ExtractFixture(source(), cincu, 0, 12)
        val bytes = fixture.bytes()
        val header = PmTilesArchiveHeader.parse(bytes.copyOf(PmTilesArchiveHeader.SIZE))
        assertTrue(header.clustered)
        assertEquals(0, header.minZoom)
        assertEquals(12, header.maxZoom)
        assertEquals(fixture.plan.addressedTiles, header.addressedTiles)
        val checked = assertIs<PmTilesCheck.Valid>(PmTilesHeader.check(bytes.copyOf(PmTilesHeader.SIZE))).header
        assertEquals(bytes.size.toLong(), checked.minimumFileSize)
        assertEquals(24.4126, checked.minLongitude, 1e-7)
        assertEquals(46.1865, checked.maxLatitude, 1e-7)
    }

    @Test
    fun sharedContentIsCopiedOnceAndRunsStayRuns() {
        val plan = ExtractFixture(source(), wider, 12, 12).plan
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
        val small = ExtractFixture(source, GeoBounds(24.79, 45.91, 24.81, 45.92), 12, 12)
        assertTrue(small.leafReads in 1..2, "${small.leafReads} of $leafCount leaves read")
    }

    @Test
    fun fewRequestsAndLittleOverfetch() {
        val fixture = ExtractFixture(source(), cincu, 0, 12)
        val useful = fixture.plan.tileDataLength
        val fetched = fixture.ranges.sumOf { it.length }
        assertTrue(
            fixture.ranges.size < fixture.plan.copies.size / 5,
            "${fixture.ranges.size} requests for ${fixture.plan.copies.size} contents",
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
