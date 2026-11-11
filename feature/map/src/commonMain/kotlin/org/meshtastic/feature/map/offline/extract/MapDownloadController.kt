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

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import org.meshtastic.feature.map.offline.PMTILES_EXTENSION
import org.meshtastic.feature.map.offline.requiredFreeBytes

/**
 * Downloads offline maps: finds the edition, estimates, downloads, pauses on a network cut, resumes, cancels. One
 * operation at a time; the state is in [state], a download stopped by the network in [pending].
 *
 * Files: the download in progress lives in [downloadDir] (`<id>.part` and `<id>.journal.json`), never among the maps;
 * once checked it moves to [mapsDir] as `<id>.pmtiles` with `<id>.name` and `<id>.json`, the layout of an imported map
 * plus the download's description. Cancelled or failed: both files are deleted. Paused: both stay, for at most
 * [PENDING_MAX_AGE_SECONDS] and while their edition exists.
 *
 * Nothing here knows about Android: the host passes the directories, the HTTP client, the clock and the free space.
 *
 * @param onInstalled Called once a map is in [mapsDir], to list it and make it the basemap.
 */
@Suppress("LongParameterList", "TooManyFunctions")
class MapDownloadController(
    private val scope: CoroutineScope,
    private val fileSystem: FileSystem,
    private val downloadDir: Path,
    private val mapsDir: Path,
    private val client: MapSourceClient,
    private val isDemoActive: () -> Boolean,
    private val freeBytes: (Path) -> Long,
    private val nowEpochMillis: () -> Long,
    private val newId: () -> String,
    private val onInstalled: suspend (id: String, name: String) -> Unit,
) {
    private val _state = MutableStateFlow<MapDownloadState>(MapDownloadState.Idle)
    private val _pending = MutableStateFlow<PendingDownload?>(null)
    private val _source = MutableStateFlow(MapSource.DEFAULT)
    private val mutex = Mutex()
    private var job: Job? = null
    private var session: Session? = null
    private var planned: Pair<MapDownloadEstimate, ExtractionPlan>? = null
    private var running: Path? = null

    val state: StateFlow<MapDownloadState> = _state.asStateFlow()
    val pending: StateFlow<PendingDownload?> = _pending.asStateFlow()
    val source: StateFlow<MapSource> = _source.asStateFlow()

    /** An edition found and read once, kept while the source does not change (its directories are cached too). */
    private class Session(
        val source: MapSource,
        val url: String,
        val etag: String?,
        val date: String?,
        val extractor: MapExtractor,
        val archive: SourceArchive,
        val foundAtMillis: Long,
    )

    /** Lists the download left by an earlier run, drops it when too old. Call once at start. */
    fun scanPending() {
        launchExclusive { refreshPending() }
    }

    fun setSource(source: MapSource) {
        if (busy()) return
        _source.value = source
        session = null
        planned = null
        if (_state.value is MapDownloadState.Estimated) _state.value = MapDownloadState.Idle
    }

    /** Computes the exact size of [bounds] from zoom 0 to [maxZoom]: about 1.1 MB read the first time. */
    fun estimate(bounds: GeoBounds, maxZoom: Int) {
        if (busy()) return
        if (isDemoActive()) return fail(MapDownloadFailure.DemoMode)
        _state.value = MapDownloadState.Estimating
        launchExclusive {
            reporting {
                val current = session()
                val plan = current.extractor.plan(current.archive, bounds, 0, maxZoom)
                val estimate =
                    MapDownloadEstimate(
                        bounds = bounds,
                        minZoom = plan.minZoom,
                        maxZoom = plan.maxZoom,
                        tiles = plan.tiles.addressedTiles,
                        fileSize = plan.fileSize,
                        transferBytes = plan.transferBytes,
                        editionDate = current.date,
                        sourceHost = current.source.host,
                    )
                planned = estimate to plan
                _state.value = MapDownloadState.Estimated(estimate)
            }
        }
    }

    /** Downloads the last estimate under [name]. */
    fun start(name: String) {
        val ready = planned
        val current = session
        when {
            ready == null || current == null || busy() || _state.value !is MapDownloadState.Estimated -> Unit
            isDemoActive() -> fail(MapDownloadFailure.DemoMode)
            else -> begin(name, ready.first, ready.second, current)
        }
    }

    private fun begin(name: String, estimate: MapDownloadEstimate, plan: ExtractionPlan, current: Session) {
        if (_pending.value != null) dropPendingFiles()
        fileSystem.createDirectories(downloadDir)
        val needed = requiredFreeBytes(plan.fileSize)
        val free = freeBytes(downloadDir)
        if (needed > free) {
            _state.value = MapDownloadState.Failed(MapDownloadFailure.NoSpace, needed, free)
            return
        }
        val id = newId()
        val journal =
            DownloadJournal(
                sourceUrl = current.url,
                etag = current.etag.orEmpty(),
                west = estimate.bounds.west,
                south = estimate.bounds.south,
                east = estimate.bounds.east,
                north = estimate.bounds.north,
                minZoom = plan.minZoom,
                maxZoom = plan.maxZoom,
                totalSize = plan.fileSize,
                rangeCount = plan.ranges.size,
                startedAtEpochSeconds = nowEpochMillis() / MILLIS_PER_SECOND,
                mapId = id,
                name = name,
            )
        _state.value = MapDownloadState.Downloading(name, 0, plan.transferBytes)
        launchExclusive {
            reporting { download(current.extractor, plan, journal, estimate.editionDate, estimate.sourceHost) }
        }
    }

    /** Continues the [pending] download: same edition, same ranges, only the missing ones. */
    fun resume() {
        when {
            busy() -> Unit
            isDemoActive() -> fail(MapDownloadFailure.DemoMode)
            else -> pendingJournal()?.let(::resumeFrom)
        }
    }

    private fun resumeFrom(journal: DownloadJournal) {
        _state.value = MapDownloadState.Downloading(journal.name, 0, journal.totalSize)
        launchExclusive {
            reporting {
                val info = client.head(journal.sourceUrl)
                val sameEdition = info.status == HTTP_OK && (journal.etag.isEmpty() || info.etag == journal.etag)
                if (!sameEdition) throw MapDownloadException.EditionChanged()
                val extractor = MapExtractor(client.ranges(journal.sourceUrl, journal.etag.ifEmpty { null }))
                val plan = extractor.plan(extractor.readSource(), journal.bounds, journal.minZoom, journal.maxZoom)
                if (plan.fileSize != journal.totalSize || plan.ranges.size != journal.rangeCount) {
                    throw MapDownloadException.EditionChanged()
                }
                val host = MapSource.parse(journal.sourceUrl)?.host.orEmpty()
                download(extractor, plan, journal, editionDateOf(journal.sourceUrl), host)
            }
        }
    }

    /** Stops what is running. A download in progress is deleted (a paused one stays: see [discardPending]). */
    fun cancel() {
        val downloading = running != null
        job?.cancel()
        scope.launch {
            mutex.withLock {
                if (downloading) dropPendingFiles()
                _state.value = if (downloading) MapDownloadState.Cancelled else MapDownloadState.Idle
            }
        }
    }

    /** Deletes the paused download. */
    fun discardPending() {
        if (busy()) return
        launchExclusive {
            dropPendingFiles()
            _state.value = MapDownloadState.Idle
        }
    }

    /** Back to [MapDownloadState.Idle] once the user has read an outcome. */
    fun clearOutcome() {
        if (!busy()) _state.value = MapDownloadState.Idle
    }

    // A disk error must not leave a partial file either, hence the generic catch.
    @Suppress("TooGenericExceptionCaught")
    private suspend fun download(
        extractor: MapExtractor,
        plan: ExtractionPlan,
        journal: DownloadJournal,
        editionDate: String?,
        host: String,
    ) {
        val part = partPath(journal.mapId)
        val journalPath = journalPath(journal.mapId)
        running = part
        try {
            extractor.download(plan, fileSystem, part, journalPath, journal) { progress ->
                _state.value = MapDownloadState.Downloading(journal.name, progress.bytesDone, progress.bytesTotal)
            }
            install(journal, editionDate, host)
            _pending.value = null
            _state.value = MapDownloadState.Done(journal.name)
            onInstalled(journal.mapId, journal.name)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (e is MapDownloadException.Unreachable || e is MapDownloadException.ServerBusy) {
                refreshPending()
                _state.value = MapDownloadState.Paused(journal.name, failureOf(e))
            } else {
                deleteDownload(journal.mapId)
                throw e
            }
        } finally {
            running = null
        }
    }

    private fun install(journal: DownloadJournal, editionDate: String?, host: String) {
        fileSystem.createDirectories(mapsDir)
        val id = journal.mapId
        fileSystem.atomicMove(partPath(id), mapsDir / "$id.$PMTILES_EXTENSION")
        fileSystem.write(mapsDir / "$id$NAME_SUFFIX") { writeUtf8(journal.name) }
        val info =
            DownloadedMapInfo(
                sourceHost = host,
                editionDate = editionDate,
                west = journal.west,
                south = journal.south,
                east = journal.east,
                north = journal.north,
                minZoom = journal.minZoom,
                maxZoom = journal.maxZoom,
                downloadedAtEpochSeconds = nowEpochMillis() / MILLIS_PER_SECOND,
            )
        fileSystem.write(mapsDir / "$id$INFO_SUFFIX") {
            writeUtf8(json.encodeToString(DownloadedMapInfo.serializer(), info))
        }
        fileSystem.delete(journalPath(id))
    }

    private suspend fun session(): Session {
        val source = _source.value
        val cached = session
        if (
            cached != null &&
            cached.source == source &&
            nowEpochMillis() - cached.foundAtMillis < SESSION_MAX_AGE_MILLIS
        ) {
            return cached
        }
        val result = EditionFinder(client::head).find(source, nowEpochMillis())
        val found = result as? EditionResult.Found ?: throw editionFailure(result)
        val extractor = MapExtractor(client.ranges(found.url, found.info.etag))
        val archive = extractor.readSource()
        if (BasemapSchema.check(archive.metadataText()) != SchemaCheck.Compatible) throw IncompatibleSchema()
        return Session(source, found.url, found.info.etag, found.date, extractor, archive, nowEpochMillis()).also {
            session = it
        }
    }

    /** Runs [block] and turns what it throws into [MapDownloadState.Failed]; cancellation passes through. */
    @Suppress("TooGenericExceptionCaught") // Any failure must end as a state, never as a crash of the app scope.
    private suspend fun reporting(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (e is MapDownloadException.EditionChanged || e is MapDownloadException.NotFound) session = null
            if (e is MapDownloadException.EditionChanged) dropPendingFiles()
            _state.value = MapDownloadState.Failed(failureOf(e))
        }
    }

    private fun editionFailure(result: EditionResult): Exception = when (result) {
        EditionResult.Unreachable -> MapDownloadException.Unreachable()
        EditionResult.FileMissing -> MapDownloadException.NotFound(HTTP_NOT_FOUND)
        else -> NoEditionFound()
    }

    private fun failureOf(e: Exception): MapDownloadFailure = when (e) {
        is MapDownloadException.Unreachable -> MapDownloadFailure.Unreachable
        is NoEditionFound -> MapDownloadFailure.NoEdition
        is MapDownloadException.NotFound -> notFound()
        is MapDownloadException.EditionChanged -> MapDownloadFailure.EditionGone
        is MapDownloadException.RangesUnsupported -> MapDownloadFailure.RangesUnsupported
        is MapDownloadException.ServerBusy -> MapDownloadFailure.ServerBusy
        is MapDownloadException.UnsupportedSource -> MapDownloadFailure.UnsupportedSource
        is IncompatibleSchema -> MapDownloadFailure.IncompatibleSchema
        is MapExtractor.TooManyTiles -> MapDownloadFailure.TooManyTiles
        is MapExtractor.NoTiles -> MapDownloadFailure.NoTiles
        is MapDownloadException.Incomplete -> MapDownloadFailure.Incomplete
        else -> MapDownloadFailure.Unexpected
    }

    /** A 404: the single file set as source is missing, or the edition was removed. */
    private fun notFound() =
        if (_source.value is MapSource.SingleFile) MapDownloadFailure.FileMissing else MapDownloadFailure.EditionGone

    private fun refreshPending() {
        val journal = pendingJournal()
        _pending.value =
            journal?.let {
                PendingDownload(
                    name = it.name,
                    rangesDone = if (fileSystem.exists(partPath(it.mapId))) it.doneRanges.size else 0,
                    rangesTotal = it.rangeCount,
                    sourceHost = MapSource.parse(it.sourceUrl)?.host.orEmpty(),
                    startedAtEpochSeconds = it.startedAtEpochSeconds,
                )
            }
    }

    /** The newest journal in [downloadDir]; every other file there, and a journal too old, is deleted. */
    private fun pendingJournal(): DownloadJournal? {
        if (!fileSystem.exists(downloadDir)) return null
        val nowSeconds = nowEpochMillis() / MILLIS_PER_SECOND
        val journals =
            fileSystem
                .list(downloadDir)
                .filter { it.name.endsWith(JOURNAL_SUFFIX) }
                .mapNotNull { DownloadJournal.load(fileSystem, it) }
                .filter { it.mapId.isNotEmpty() && nowSeconds - it.startedAtEpochSeconds <= PENDING_MAX_AGE_SECONDS }
                .sortedByDescending { it.startedAtEpochSeconds }
        val kept = journals.firstOrNull()
        val keep = kept?.let { setOf(partPath(it.mapId), journalPath(it.mapId)) }.orEmpty() + setOfNotNull(running)
        fileSystem.list(downloadDir).filter { it !in keep }.forEach { fileSystem.delete(it) }
        return kept
    }

    private fun dropPendingFiles() {
        if (fileSystem.exists(downloadDir)) fileSystem.list(downloadDir).forEach { fileSystem.delete(it) }
        _pending.value = null
    }

    private fun deleteDownload(id: String) {
        fileSystem.delete(partPath(id))
        fileSystem.delete(journalPath(id))
        _pending.value = null
    }

    private fun partPath(id: String) = downloadDir / "$id$PART_SUFFIX"

    private fun journalPath(id: String) = downloadDir / "$id$JOURNAL_SUFFIX"

    private fun busy() = job?.isActive == true

    private fun fail(reason: MapDownloadFailure) {
        _state.value = MapDownloadState.Failed(reason)
    }

    private fun launchExclusive(block: suspend () -> Unit) {
        job = scope.launch { mutex.withLock { block() } }
    }

    private class NoEditionFound : Exception("no edition")

    private class IncompatibleSchema : Exception("incompatible schema")

    companion object {
        const val NAME_SUFFIX = ".name"
        const val INFO_SUFFIX = ".json"
        private const val PART_SUFFIX = ".part"
        private const val JOURNAL_SUFFIX = ".journal.json"
        private const val MILLIS_PER_SECOND = 1_000L
        private const val HTTP_OK = 200
        private const val HTTP_NOT_FOUND = 404
        private const val DATE_LENGTH = 8

        /** D7: a download stopped by the network is kept a week at most (its edition lasts about that long). */
        const val PENDING_MAX_AGE_SECONDS = 7L * 24 * 3600

        /** A found edition is looked for again after an hour: a new one may have come out. */
        private const val SESSION_MAX_AGE_MILLIS = 3_600_000L
        private val json = Json { ignoreUnknownKeys = true }

        /** `AAAAMMJJ` of an edition address, or null. */
        fun editionDateOf(url: String): String? =
            url.substringAfterLast('/').removeSuffix(".$PMTILES_EXTENSION").takeIf {
                it.length == DATE_LENGTH && it.all(Char::isDigit)
            }
    }
}
