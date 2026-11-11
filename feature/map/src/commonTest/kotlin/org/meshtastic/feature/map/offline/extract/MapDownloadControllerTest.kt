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
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import org.meshtastic.feature.map.offline.extract.ExtractFixture.Companion.cincu
import org.meshtastic.feature.map.offline.extract.ExtractFixture.Companion.metadata
import org.meshtastic.feature.map.offline.extract.ExtractFixture.Companion.source
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MapDownloadControllerTest {

    /** 11 November 2026, 15:00 UTC. */
    private val now = 1_794_409_200_000L
    private val edition = "https://build.protomaps.com/20261111.pmtiles"
    private val fileSystem = FakeFileSystem()
    private val downloadDir = "/files/pmtiles-download".toPath()
    private val mapsDir = "/files/pmtiles".toPath()
    private val installed = ArrayList<Pair<String, String>>()

    /** A server holding [archive] at [edition]; [cutAfter] tile reads, the network goes down. */
    private inner class FakeServer(
        var archive: SyntheticPmTiles = source(),
        var etag: String = "\"e1\"",
        var cutAfter: Int = Int.MAX_VALUE,
        var gate: CompletableDeferred<Unit>? = null,
    ) : MapSourceClient {
        var reads = 0
        var down = false

        override suspend fun head(url: String): RemoteFileInfo {
            if (down) throw MapDownloadException.Unreachable()
            val status = if (url == edition) 200 else 404
            return RemoteFileInfo(status, etag, archive.bytes.size.toLong(), true, "Wed, 11 Nov 2026 15:00:00 GMT")
        }

        override fun ranges(url: String, etag: String?) = RangeSource { offset, length ->
            gate?.await()
            if (down || reads++ >= cutAfter) {
                down = true
                throw MapDownloadException.Unreachable()
            }
            if (etag != null && etag != this.etag) throw MapDownloadException.EditionChanged()
            archive.read(offset, length)
        }
    }

    @AfterTest fun noFileLeftOpen() = fileSystem.checkNoOpenFiles()

    private fun TestScope.controller(
        server: MapSourceClient,
        demo: Boolean = false,
        free: Long = Long.MAX_VALUE,
        clock: () -> Long = { now },
        maxFileBytes: Long = MapDownloadController.MAX_FILE_BYTES,
    ) = MapDownloadController(
        scope = this,
        fileSystem = fileSystem,
        downloadDir = downloadDir,
        mapsDir = mapsDir,
        client = server,
        isDemoActive = { demo },
        freeBytes = { free },
        nowEpochMillis = clock,
        newId = { "map1" },
        onInstalled = { id, name -> installed += id to name },
        maxFileBytes = maxFileBytes,
    )

    private fun files(dir: okio.Path) =
        if (fileSystem.exists(dir)) fileSystem.list(dir).map { it.name }.sorted() else emptyList()

    @Test
    fun estimatesThenDownloadsTheSameFileAsTheExtractInMemory() = runTest {
        val controller = controller(FakeServer())
        controller.estimate(cincu, 12)
        advanceUntilIdle()
        val estimate = assertIs<MapDownloadState.Estimated>(controller.state.value).estimate
        val fixture = ExtractFixture(source(), cincu, 0, 12)
        assertEquals(fixture.layout.totalSize, estimate.fileSize)
        assertEquals("20261111", estimate.editionDate)
        assertEquals("build.protomaps.com", estimate.sourceHost)
        assertEquals(TileCover.count(cincu, 0, 12), TileCover.count(estimate.bounds, 0, 12))

        controller.start("Cincu")
        advanceUntilIdle()
        assertEquals(MapDownloadState.Done("Cincu"), controller.state.value)
        assertContentEquals(fixture.bytes(), fileSystem.read(mapsDir / "map1.pmtiles") { readByteArray() })
        assertEquals("Cincu", fileSystem.read(mapsDir / "map1.name") { readUtf8() })
        assertTrue(fileSystem.read(mapsDir / "map1.json") { readUtf8() }.contains("\"editionDate\":\"20261111\""))
        assertEquals(emptyList(), files(downloadDir))
        assertEquals(listOf("map1" to "Cincu"), installed)
        assertNull(controller.pending.value)
    }

    @Test
    fun aNetworkCutPausesAndResumeFinishesWithTheMissingRangesOnly() = runTest {
        val server = FakeServer()
        val controller = controller(server)
        controller.estimate(cincu, 12)
        advanceUntilIdle()
        val head = server.reads
        val tileRanges = ExtractFixture(source(), cincu, 0, 12).ranges.size
        server.cutAfter = head + tileRanges / 2
        controller.start("Cincu")
        advanceUntilIdle()
        assertEquals(MapDownloadState.Paused("Cincu", MapDownloadFailure.Unreachable), controller.state.value)
        val pending = assertNotNull(controller.pending.value)
        assertEquals("Cincu", pending.name)
        assertTrue(pending.rangesDone in 1 until tileRanges, "$pending")
        assertEquals(listOf("map1.journal.json", "map1.part"), files(downloadDir))
        assertEquals(emptyList(), files(mapsDir))

        server.down = false
        server.cutAfter = Int.MAX_VALUE
        controller.resume()
        advanceUntilIdle()
        assertEquals(MapDownloadState.Done("Cincu"), controller.state.value)
        val fixture = ExtractFixture(source(), cincu, 0, 12)
        assertContentEquals(fixture.bytes(), fileSystem.read(mapsDir / "map1.pmtiles") { readByteArray() })
        assertEquals(emptyList(), files(downloadDir))
    }

    @Test
    fun aDownloadLeftByAnEarlierRunIsFoundAgainThenExpires() = runTest {
        val server = FakeServer()
        val first = controller(server)
        first.estimate(cincu, 12)
        advanceUntilIdle()
        server.cutAfter = server.reads + 3
        first.start("Cincu")
        advanceUntilIdle()

        val again = controller(FakeServer())
        again.scanPending()
        advanceUntilIdle()
        assertEquals("Cincu", again.pending.value?.name)

        val weekLater = controller(FakeServer(), clock = { now + 8 * 86_400_000L })
        weekLater.scanPending()
        advanceUntilIdle()
        assertNull(weekLater.pending.value)
        assertEquals(emptyList(), files(downloadDir))
    }

    @Test
    fun aReplacedEditionCannotBeResumedAndNothingIsKept() = runTest {
        val server = FakeServer()
        val controller = controller(server)
        controller.estimate(cincu, 12)
        advanceUntilIdle()
        server.cutAfter = server.reads + 2
        controller.start("Cincu")
        advanceUntilIdle()
        server.down = false
        server.cutAfter = Int.MAX_VALUE
        server.etag = "\"e2\""
        controller.resume()
        advanceUntilIdle()
        assertEquals(MapDownloadState.Failed(MapDownloadFailure.EditionGone), controller.state.value)
        assertEquals(emptyList(), files(downloadDir))
        assertNull(controller.pending.value)
    }

    @Test
    fun cancellingADownloadKeepsNothing() = runTest {
        val gate = CompletableDeferred<Unit>()
        val server = FakeServer()
        val controller = controller(server)
        controller.estimate(cincu, 12)
        advanceUntilIdle()
        server.gate = gate
        controller.start("Cincu")
        advanceUntilIdle()
        assertIs<MapDownloadState.Downloading>(controller.state.value)
        controller.cancel()
        advanceUntilIdle()
        assertEquals(MapDownloadState.Cancelled, controller.state.value)
        assertEquals(emptyList(), files(downloadDir))
        assertEquals(emptyList(), files(mapsDir))
    }

    @Test
    fun notEnoughSpaceIsRefusedBeforeWriting() = runTest {
        val controller = controller(FakeServer(), free = 10_000_000)
        controller.estimate(cincu, 12)
        advanceUntilIdle()
        controller.start("Cincu")
        advanceUntilIdle()
        val failed = assertIs<MapDownloadState.Failed>(controller.state.value)
        assertEquals(MapDownloadFailure.NoSpace, failed.reason)
        assertEquals(10_000_000L, failed.freeBytes)
        assertTrue(failed.neededBytes > failed.freeBytes)
        assertEquals(emptyList(), files(downloadDir))
    }

    @Test
    fun demoModeDownloadsNothing() = runTest {
        val server = FakeServer()
        val controller = controller(server, demo = true)
        controller.estimate(cincu, 12)
        advanceUntilIdle()
        assertEquals(MapDownloadState.Failed(MapDownloadFailure.DemoMode), controller.state.value)
        assertEquals(0, server.reads)
    }

    @Test
    fun aMapOverTheSizeCapIsRefusedAtTheEstimate() = runTest {
        val controller = controller(FakeServer(), maxFileBytes = 1_000)
        controller.estimate(cincu, 12)
        advanceUntilIdle()
        assertEquals(MapDownloadState.Failed(MapDownloadFailure.TooManyTiles), controller.state.value)
        controller.start("Cincu")
        advanceUntilIdle()
        assertEquals(emptyList(), files(downloadDir))
        assertEquals(emptyList(), files(mapsDir))
    }

    @Test
    fun anIncompatibleSchemaIsRefused() = runTest {
        val controller = controller(FakeServer(archive = source(metadata = metadata(version = "5.0.0"))))
        controller.estimate(cincu, 12)
        advanceUntilIdle()
        assertEquals(MapDownloadState.Failed(MapDownloadFailure.IncompatibleSchema), controller.state.value)
    }

    @Test
    fun noNetworkAndNoEditionAreTold() = runTest {
        val down = FakeServer().also { it.down = true }
        val controller = controller(down)
        controller.estimate(cincu, 12)
        advanceUntilIdle()
        assertEquals(MapDownloadState.Failed(MapDownloadFailure.Unreachable), controller.state.value)

        val elsewhere = controller(FakeServer())
        elsewhere.setSource(MapSource.Editions("https://mirror.example/maps"))
        elsewhere.estimate(cincu, 12)
        advanceUntilIdle()
        assertEquals(MapDownloadState.Failed(MapDownloadFailure.NoEdition), elsewhere.state.value)

        val single = controller(FakeServer())
        single.setSource(MapSource.SingleFile("https://mirror.example/cincu.pmtiles"))
        single.estimate(cincu, 12)
        advanceUntilIdle()
        assertEquals(MapDownloadState.Failed(MapDownloadFailure.FileMissing), single.state.value)
    }

    @Test
    fun theEditionDateIsReadFromTheAddress() {
        assertEquals("20261111", MapDownloadController.editionDateOf(edition))
        assertNull(MapDownloadController.editionDateOf("https://mirror.example/cincu.pmtiles"))
    }
}
