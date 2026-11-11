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

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import org.meshtastic.feature.map.offline.extract.ExtractFixture.Companion.cincu
import org.meshtastic.feature.map.offline.extract.ExtractFixture.Companion.source
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MapExtractorTest {

    private val synthetic = source()
    private val fixture = ExtractFixture(synthetic, cincu, 0, 12)
    private val fileSystem = FakeFileSystem()
    private val dir = "/files/pmtiles-download".toPath()
    private val part = dir / "map.part"
    private val journalPath = dir / "map.journal.json"
    private val reads = ArrayList<Pair<Long, Long>>()

    init {
        fileSystem.createDirectories(dir)
    }

    @AfterTest fun noFileLeftOpen() = fileSystem.checkNoOpenFiles()

    /** Serves the synthetic archive; [failAfter] reads, every read throws. */
    private fun rangeSource(bytes: ByteArray = synthetic.bytes, failAfter: Int = Int.MAX_VALUE) =
        RangeSource { offset, length ->
            reads += offset to length
            if (reads.size > failAfter) throw MapDownloadException.Unreachable()
            bytes.copyOfRange(offset.toInt(), (offset + length).toInt())
        }

    private fun journal(plan: ExtractionPlan) = DownloadJournal(
        sourceUrl = "https://maps.example/20261111.pmtiles",
        etag = "\"e1\"",
        west = plan.bounds.west,
        south = plan.bounds.south,
        east = plan.bounds.east,
        north = plan.bounds.north,
        minZoom = plan.minZoom,
        maxZoom = plan.maxZoom,
        totalSize = plan.fileSize,
        rangeCount = plan.ranges.size,
        startedAtEpochSeconds = 1_794_000_000,
    )

    @Test
    fun readsTheSourceInThreeSmallRequests() = runTest {
        val archive = MapExtractor(rangeSource()).readSource()
        assertEquals(synthetic.header, archive.header)
        assertEquals(3, reads.size)
        assertTrue(archive.metadataText().contains("Protomaps Basemap"))
    }

    @Test
    fun thePlanIsTheOneComputedInMemory() = runTest {
        val extractor = MapExtractor(rangeSource())
        val plan = extractor.plan(extractor.readSource(), cincu, 0, 12)
        assertEquals(fixture.layout.totalSize, plan.fileSize)
        assertEquals(fixture.ranges, plan.ranges)
        assertEquals(fixture.ranges.sumOf { it.length }, plan.transferBytes)
    }

    @Test
    fun aSecondEstimateNearbyReadsNoDirectoryAgain() = runTest {
        val extractor = MapExtractor(rangeSource())
        val archive = extractor.readSource()
        extractor.plan(archive, cincu, 0, 12)
        val before = reads.size
        extractor.plan(archive, GeoBounds(24.5, 45.7, 25.1, 46.1), 0, 12)
        assertEquals(before, reads.size)
    }

    @Test
    fun downloadsTheSameFileAsInMemoryAndReportsProgress() = runTest {
        val extractor = MapExtractor(rangeSource())
        val plan = extractor.plan(extractor.readSource(), cincu, 0, 12)
        val progress = ArrayList<DownloadProgress>()
        extractor.download(plan, fileSystem, part, journalPath, journal(plan)) { progress += it }
        assertContentEquals(fixture.bytes(), fileSystem.read(part) { readByteArray() })
        assertTrue(DownloadJournal.load(fileSystem, journalPath)!!.isComplete)
        assertEquals(DownloadProgress(0, plan.transferBytes), progress.first())
        assertEquals(DownloadProgress(plan.transferBytes, plan.transferBytes), progress.last())
        assertEquals(progress.map { it.bytesDone }.sorted(), progress.map { it.bytesDone })
    }

    @Test
    fun aCutDownloadResumesWithOnlyTheMissingRanges() = runTest {
        val probe = MapExtractor(rangeSource())
        val plan = probe.plan(probe.readSource(), cincu, 0, 12)
        reads.clear()
        val cutAfter = plan.ranges.size / 2
        val failing = MapExtractor(rangeSource(failAfter = cutAfter), parallelism = 1)
        assertFailsWith<MapDownloadException.Unreachable> {
            failing.download(plan, fileSystem, part, journalPath, journal(plan))
        }
        val saved = DownloadJournal.load(fileSystem, journalPath)!!
        assertEquals(cutAfter, saved.doneRanges.size)

        reads.clear()
        MapExtractor(rangeSource()).download(plan, fileSystem, part, journalPath, saved)
        assertEquals(plan.ranges.size - cutAfter, reads.size)
        assertContentEquals(fixture.bytes(), fileSystem.read(part) { readByteArray() })
    }

    @Test
    fun cancellingLeavesThePartAndTheJournalForTheCaller() = runTest {
        val extractor = MapExtractor(rangeSource())
        val plan = extractor.plan(extractor.readSource(), cincu, 0, 12)
        val blocked = CompletableDeferred<Unit>()
        val stuck =
            MapExtractor(
                RangeSource { _, _ ->
                    blocked.await()
                    ByteArray(0)
                },
            )
        val job = launch { stuck.download(plan, fileSystem, part, journalPath, journal(plan)) }
        testScheduler.advanceUntilIdle()
        job.cancel()
        job.join()
        assertTrue(fileSystem.exists(part))
        assertEquals(0, DownloadJournal.load(fileSystem, journalPath)!!.doneRanges.size)
    }

    @Test
    fun aTooLargeAreaIsRefusedBeforeReadingAnything() = runTest {
        val extractor = MapExtractor(rangeSource())
        val archive = extractor.readSource()
        reads.clear()
        val error = assertFailsWith<MapExtractor.TooManyTiles> { extractor.plan(archive, cincu, 0, 12, maxTiles = 100) }
        assertEquals(TileCover.count(cincu, 0, 12), error.count)
        assertEquals(0, reads.size)
    }

    @Test
    fun unsupportedSourcesAreRefused() = runTest {
        fun withHeader(change: (PmTilesArchiveHeader) -> PmTilesArchiveHeader): ByteArray {
            val bytes = synthetic.bytes.copyOf()
            change(synthetic.header).toBytes().copyInto(bytes)
            return bytes
        }
        assertFailsWith<MapDownloadException.UnsupportedSource> {
            MapExtractor(rangeSource(withHeader { it.copy(clustered = false) })).readSource()
        }
        assertFailsWith<MapDownloadException.UnsupportedSource> {
            MapExtractor(rangeSource(withHeader { it.copy(tileType = 2) })).readSource()
        }
        assertFailsWith<MapDownloadException.UnsupportedSource> {
            MapExtractor(rangeSource(withHeader { it.copy(internalCompression = PmTilesCompression.BROTLI) }))
                .readSource()
        }
        assertFailsWith<MapDownloadException.UnsupportedSource> {
            MapExtractor(rangeSource(ByteArray(200))).readSource()
        }
    }

    @Test
    fun anAreaOutsideTheSourceHasNoTiles() = runTest {
        val extractor = MapExtractor(rangeSource())
        assertFailsWith<MapExtractor.NoTiles> {
            extractor.plan(extractor.readSource(), GeoBounds(-60.0, -30.0, -59.0, -29.0), 5, 12)
        }
    }
}
