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

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import okio.FileSystem
import okio.Path

/** The remote archive as read at the start: its header, root directory and metadata (still compressed). */
class SourceArchive(val header: PmTilesArchiveHeader, val root: List<PmTilesEntry>, val metadata: ByteArray) {
    /** The metadata as text (JSON), for the schema and credit checks. */
    fun metadataText(): String = PmTilesCompression.decompress(metadata, header.internalCompression).decodeToString()
}

/** Everything known before the first tile is downloaded: the exact size, the ranges to fetch, the head to write. */
class ExtractionPlan(
    val bounds: GeoBounds,
    val minZoom: Int,
    val maxZoom: Int,
    /** Where the tile data starts in the source: the ranges are relative to it. */
    val sourceTileDataOffset: Long,
    val tiles: ExtractPlan,
    val directories: PmTilesDirectories,
    val metadata: ByteArray,
    val layout: ArchiveLayout,
    val header: PmTilesArchiveHeader,
    val ranges: List<FetchRange>,
) {
    /** The exact size of the finished file. */
    val fileSize: Long
        get() = layout.totalSize

    /** What the tile download transfers, overfetch included (the directories already read are not counted). */
    val transferBytes: Long
        get() = ranges.sumOf { it.length }
}

/** Bytes of tile data received so far, and the total to receive. */
data class DownloadProgress(val bytesDone: Long, val bytesTotal: Long)

/**
 * Extracts a region of a remote PMTiles v3 archive into a local one: the steps of `pmtiles extract`
 * (docs/PLAN-CARTE-TELECHARGEMENT.md §4) over a [RangeSource].
 *
 * [readSource] takes three small requests: header, root directory (16 KB at most, by the spec) and metadata. [plan]
 * reads the leaf directories the region needs, at most [parallelism] at a time, and keeps them (compressed) for the
 * next plan: a second estimate nearby costs nothing (about 1.1 MB for the first, probe of 11 November 2026). [download]
 * fetches the ranges [parallelism] at a time, writes each at its place, notes it in the journal, and checks the
 * finished file. Nothing here knows about Android, the edition or the user interface.
 */
class MapExtractor(private val source: RangeSource, private val parallelism: Int = DEFAULT_PARALLELISM) {

    private val leafCache = HashMap<Long, ByteArray>()
    private val cacheLock = Mutex()

    /**
     * Reads and checks the source's header, root directory and metadata: three small requests (the header alone first,
     * so that a source smaller than the 16 KB the spec allows for header and root still answers).
     */
    suspend fun readSource(): SourceArchive {
        val header = unsupportedIfCorrupt {
            PmTilesArchiveHeader.parse(source.read(0, PmTilesArchiveHeader.SIZE.toLong()))
        }
        checkSupported(header)
        val root = unsupportedIfCorrupt { decode(source.read(header.rootOffset, header.rootLength), header) }
        val metadata = source.read(header.metadataOffset, header.metadataLength)
        return SourceArchive(header, root, metadata)
    }

    /**
     * Plans the extract of [bounds] from [minZoom] to [maxZoom] (lowered to the source's maximum). Refuses an area of
     * more than [maxTiles] tiles before reading anything.
     */
    suspend fun plan(
        archive: SourceArchive,
        bounds: GeoBounds,
        minZoom: Int,
        maxZoom: Int,
        maxTiles: Long = DEFAULT_MAX_TILES,
    ): ExtractionPlan {
        val header = archive.header
        val top = minOf(maxZoom, header.maxZoom)
        val bottom = maxOf(minZoom, header.minZoom)
        require(bottom <= top) { "zooms $minZoom..$maxZoom outside the source's ${header.minZoom}..${header.maxZoom}" }
        val count = TileCover.count(bounds, bottom, top)
        if (count > maxTiles) throw TooManyTiles(count, maxTiles)

        val found = ArrayList<PmTilesEntry>()
        var pending = listOf(archive.root to TileCover.tileIds(bounds, bottom, top))
        while (pending.isNotEmpty()) {
            val requests = ArrayList<LeafRequest>()
            for ((directory, wanted) in pending) {
                val relevant = ExtractPlanner.relevant(directory, wanted)
                found += relevant.tiles
                requests += relevant.leaves
            }
            pending = readLeaves(header, requests)
        }
        val tiles = ExtractPlanner.plan(found)
        val directories =
            PmTilesDirectoryBuilder.build(tiles.entries.ifEmpty { throw NoTiles() }, header.internalCompression)
        val layout =
            ArchiveLayout(
                directories.root.size.toLong(),
                archive.metadata.size.toLong(),
                directories.leaves.size.toLong(),
                tiles.tileDataLength,
            )
        val newHeader = ExtractHeader.of(header, bounds, bottom, top, tiles, layout, header.internalCompression)
        val ranges = RangeMerger.merge(tiles.copies)
        return ExtractionPlan(
            bounds = bounds,
            minZoom = bottom,
            maxZoom = top,
            sourceTileDataOffset = header.tileDataOffset,
            tiles = tiles,
            directories = directories,
            metadata = archive.metadata,
            layout = layout,
            header = newHeader,
            ranges = ranges,
        )
    }

    /**
     * Downloads [plan] into [part], skipping the ranges [journal] marks as done, and saving the journal at
     * [journalPath] after each range. Returns once the file has passed [ArchiveCheck]. On failure or cancellation the
     * part file and the journal stay, for a resume; the caller decides whether to delete them.
     */
    suspend fun download(
        plan: ExtractionPlan,
        fileSystem: FileSystem,
        part: Path,
        journalPath: Path,
        journal: DownloadJournal,
        onProgress: (DownloadProgress) -> Unit = {},
    ) {
        require(journal.totalSize == plan.fileSize && journal.rangeCount == plan.ranges.size) {
            "the journal does not match this plan"
        }
        val total = plan.transferBytes
        var state = journal
        var done = journal.doneRanges.sumOf { plan.ranges[it].length }
        val lock = Mutex()
        val permits = Semaphore(parallelism)
        DownloadJournal.save(fileSystem, journalPath, state)
        onProgress(DownloadProgress(done, total))
        ArchiveWriter.open(fileSystem, part, plan.layout).use { writer ->
            writer.writeHead(plan.header, plan.directories, plan.metadata)
            coroutineScope {
                plan.ranges.indices
                    .filter { it !in journal.doneRanges }
                    .map { index ->
                        async {
                            permits.withPermit {
                                val range = plan.ranges[index]
                                val bytes = source.read(plan.sourceTileDataOffset + range.sourceOffset, range.length)
                                lock.withLock {
                                    writer.writeRange(range, bytes)
                                    writer.flush()
                                    state = state.withDone(index)
                                    DownloadJournal.save(fileSystem, journalPath, state)
                                    done += range.length
                                    onProgress(DownloadProgress(done, total))
                                }
                            }
                        }
                    }
                    .awaitAll()
            }
        }
        ArchiveCheck.verify(fileSystem, part, plan.fileSize)
    }

    private suspend fun readLeaves(
        header: PmTilesArchiveHeader,
        requests: List<LeafRequest>,
    ): List<Pair<List<PmTilesEntry>, LongArray>> = coroutineScope {
        val permits = Semaphore(parallelism)
        requests
            .map { request ->
                async {
                    permits.withPermit {
                        val leaf = request.leaf
                        val cached = cacheLock.withLock { leafCache[leaf.offset] }
                        val bytes =
                            cached
                                ?: source.read(header.leafDirectoriesOffset + leaf.offset, leaf.length).also {
                                    cacheLock.withLock { leafCache[leaf.offset] = it }
                                }
                        unsupportedIfCorrupt { decode(bytes, header) } to request.wanted
                    }
                }
            }
            .awaitAll()
    }

    private fun checkSupported(header: PmTilesArchiveHeader) {
        val problem =
            when {
                !header.clustered -> "not clustered"

                header.internalCompression !in SUPPORTED_COMPRESSIONS ->
                    "directories compressed with ${header.internalCompression}"

                header.tileType != MVT -> "tile type ${header.tileType}, vector tiles expected"

                else -> null
            }
        if (problem != null) throw MapDownloadException.UnsupportedSource(problem)
    }

    private fun decode(bytes: ByteArray, header: PmTilesArchiveHeader) =
        PmTilesDirectory.decode(PmTilesCompression.decompress(bytes, header.internalCompression))

    private inline fun <T> unsupportedIfCorrupt(block: () -> T): T = try {
        block()
    } catch (e: PmTilesFormatException) {
        throw MapDownloadException.UnsupportedSource(e.message ?: "corrupt archive", e)
    }

    /** The area holds more tiles than allowed: the user reduces the rectangle or the zoom. */
    class TooManyTiles(val count: Long, val limit: Long) : Exception("$count tiles, $limit at most")

    /** The source has no tile in the area (outside its bounds). */
    class NoTiles : Exception("no tile in this area")

    companion object {
        const val DEFAULT_PARALLELISM = 4

        /** Plan §10: the entries of 3 000 000 tiles stay well within a phone's memory. */
        const val DEFAULT_MAX_TILES = 3_000_000L
        private const val MVT = 1
        private val SUPPORTED_COMPRESSIONS = setOf(PmTilesCompression.NONE, PmTilesCompression.GZIP)
    }
}
