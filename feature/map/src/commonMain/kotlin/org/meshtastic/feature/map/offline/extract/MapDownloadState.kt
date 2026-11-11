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

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Why a map download did not end with a map. Each one has its own message (docs/PLAN-CARTE-TELECHARGEMENT.md §12). */
enum class MapDownloadFailure {
    /** No answer from the source: no network, name not resolved, TLS refused, timeout. */
    Unreachable,

    /** The source answers, but none of the last 8 days has an edition. */
    NoEdition,

    /** The single file set as source answers 404. */
    FileMissing,

    /** The edition being downloaded was removed or replaced (404 or 412): start again. */
    EditionGone,

    /** The server sends the whole file instead of parts of it. */
    RangesUnsupported,

    /** The server answers 429 or 5xx after every retry. */
    ServerBusy,

    /** The source is not a PMTiles v3 vector archive this app can extract from. */
    UnsupportedSource,

    /** The tiles do not suit the style shipped in the app (schema other than 4.x, or missing layers). */
    IncompatibleSchema,

    /** The area holds more tiles than allowed. */
    TooManyTiles,

    /** The source has no tile in the area. */
    NoTiles,

    /** Not enough free space for the file. */
    NoSpace,

    /** The finished file failed its check. */
    Incomplete,

    /** Demo mode is on: nothing is downloaded. */
    DemoMode,

    /** Anything else (a disk error, for instance). */
    Unexpected,
}

/** What the screen shows before the download: the exact figures of [MapExtractor.plan]. */
data class MapDownloadEstimate(
    val bounds: GeoBounds,
    val minZoom: Int,
    val maxZoom: Int,
    val tiles: Long,
    val fileSize: Long,
    val transferBytes: Long,
    /** `AAAAMMJJ`, or null for a source that is a single file. */
    val editionDate: String?,
    val sourceHost: String,
)

/** A download that stopped on a network cut and can be resumed. */
data class PendingDownload(
    val name: String,
    /** Fetch ranges already in the file, of [rangesTotal]: the share done, without the byte count of each. */
    val rangesDone: Int,
    val rangesTotal: Int,
    val sourceHost: String,
    val startedAtEpochSeconds: Long,
)

sealed interface MapDownloadState {
    data object Idle : MapDownloadState

    /** Reading the source's header and directories (about 1.1 MB for Cincu, probe of 11 November 2026). */
    data object Estimating : MapDownloadState

    data class Estimated(val estimate: MapDownloadEstimate) : MapDownloadState

    data class Downloading(val name: String, val bytesDone: Long, val bytesTotal: Long) : MapDownloadState

    /** Stopped on a network problem; the partial file is kept and [MapDownloadController.resume] continues it. */
    data class Paused(val name: String, val reason: MapDownloadFailure) : MapDownloadState

    data class Done(val name: String) : MapDownloadState

    /** Cancelled by the user: nothing was kept. */
    data object Cancelled : MapDownloadState

    /** Nothing was kept. [neededBytes] and [freeBytes] only for [MapDownloadFailure.NoSpace]. */
    data class Failed(val reason: MapDownloadFailure, val neededBytes: Long = 0, val freeBytes: Long = 0) :
        MapDownloadState
}

/** What is written beside a downloaded map (`pmtiles/<id>.json`), for the list: imported maps have none. */
@Serializable
data class DownloadedMapInfo(
    val sourceHost: String,
    val editionDate: String? = null,
    val west: Double,
    val south: Double,
    val east: Double,
    val north: Double,
    val minZoom: Int,
    val maxZoom: Int,
    val downloadedAtEpochSeconds: Long,
) {
    val bounds: GeoBounds
        get() = GeoBounds(west, south, east, north)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        /** The description in [text], or null when it is not one: the map is then listed like an imported one. */
        fun decode(text: String): DownloadedMapInfo? =
            runCatching { json.decodeFromString(serializer(), text) }.getOrNull()
    }
}
