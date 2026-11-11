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

import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import org.meshtastic.feature.map.offline.extract.ExtractFixture.Companion.cincu
import org.meshtastic.feature.map.offline.extract.ExtractFixture.Companion.source
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ArchiveWriterTest {

    private val fileSystem = FakeFileSystem()
    private val dir = "/files/pmtiles-download".toPath()
    private val part = dir / "map.part"
    private val journalPath = dir / "map.journal.json"
    private val fixture = ExtractFixture(source(), cincu, 0, 12)

    init {
        fileSystem.createDirectories(dir)
    }

    @AfterTest fun noFileLeftOpen() = fileSystem.checkNoOpenFiles()

    private fun journal() = DownloadJournal(
        sourceUrl = "https://maps.example/20261111.pmtiles",
        etag = "\"e1\"",
        west = cincu.west,
        south = cincu.south,
        east = cincu.east,
        north = cincu.north,
        minZoom = 0,
        maxZoom = 12,
        totalSize = fixture.layout.totalSize,
        rangeCount = fixture.ranges.size,
        startedAtEpochSeconds = 1_794_000_000,
    )

    @Test
    fun rangesWrittenInAnyOrderGiveTheSameFileAsInMemory() {
        ArchiveWriter.open(fileSystem, part, fixture.layout).use { writer ->
            writer.writeHead(fixture.header, fixture.directories, fixture.metadata)
            for (range in fixture.ranges.reversed()) writer.writeRange(range, fixture.fetch(range))
            writer.flush()
        }
        assertContentEquals(fixture.bytes(), fileSystem.read(part) { readByteArray() })
        ArchiveCheck.verify(fileSystem, part, fixture.layout.totalSize)
    }

    @Test
    fun aDownloadCutHalfwayResumesWithTheJournalAndEndsIdentical() {
        val half = fixture.ranges.size / 2
        var journal = journal()
        ArchiveWriter.open(fileSystem, part, fixture.layout).use { writer ->
            writer.writeHead(fixture.header, fixture.directories, fixture.metadata)
            for (i in 0 until half) {
                writer.writeRange(fixture.ranges[i], fixture.fetch(fixture.ranges[i]))
                writer.flush()
                journal = journal.withDone(i)
                DownloadJournal.save(fileSystem, journalPath, journal)
            }
        }
        assertFailsWith<MapDownloadException.Incomplete> { ArchiveCheck.verify(fileSystem, part, journal.totalSize) }

        val reloaded = DownloadJournal.load(fileSystem, journalPath)!!
        assertEquals((0 until half).toList(), reloaded.doneRanges)
        assertEquals(cincu, reloaded.bounds)
        ArchiveWriter.open(fileSystem, part, fixture.layout).use { writer ->
            for (i in fixture.ranges.indices.filter { it !in reloaded.doneRanges }) {
                writer.writeRange(fixture.ranges[i], fixture.fetch(fixture.ranges[i]))
            }
        }
        assertContentEquals(fixture.bytes(), fileSystem.read(part) { readByteArray() })
        ArchiveCheck.verify(fileSystem, part, fixture.layout.totalSize)
    }

    @Test
    fun theJournalIsCompleteOnlyWithEveryRange() {
        var journal = journal()
        fixture.ranges.indices.forEach { journal = journal.withDone(it).withDone(it) }
        assertTrue(journal.isComplete)
        assertEquals(fixture.ranges.size, journal.doneRanges.size)
        assertFailsWith<IllegalArgumentException> { journal.withDone(fixture.ranges.size) }
    }

    @Test
    fun aMissingOrBrokenJournalIsIgnored() {
        assertNull(DownloadJournal.load(fileSystem, journalPath))
        fileSystem.write(journalPath) { writeUtf8("{not json") }
        assertNull(DownloadJournal.load(fileSystem, journalPath))
        fileSystem.write(journalPath) { writeUtf8("""{"version":99}""") }
        assertNull(DownloadJournal.load(fileSystem, journalPath))
        DownloadJournal.save(fileSystem, journalPath, journal())
        assertEquals(journal(), DownloadJournal.load(fileSystem, journalPath))
        assertEquals(listOf(journalPath), fileSystem.list(dir).filter { it.name.startsWith("map.journal") })
    }

    @Test
    fun theCheckRefusesTruncatedOrDamagedFiles() {
        val bytes = fixture.bytes()
        fileSystem.write(part) { write(bytes.copyOf(bytes.size - 1)) }
        assertFailsWith<MapDownloadException.Incomplete> { ArchiveCheck.verify(fileSystem, part, bytes.size.toLong()) }

        val badRoot = bytes.copyOf().also { it[fixture.layout.rootOffset.toInt() + 3] = 0x55 }
        fileSystem.write(part) { write(badRoot) }
        assertFailsWith<MapDownloadException.Incomplete> { ArchiveCheck.verify(fileSystem, part, bytes.size.toLong()) }

        val zeroTiles = bytes.copyOf().also { it.fill(0, fixture.layout.tileDataOffset.toInt(), it.size) }
        fileSystem.write(part) { write(zeroTiles) }
        assertFailsWith<MapDownloadException.Incomplete> { ArchiveCheck.verify(fileSystem, part, bytes.size.toLong()) }

        fileSystem.write(part) { write(bytes) }
        ArchiveCheck.verify(fileSystem, part, bytes.size.toLong())
    }
}
