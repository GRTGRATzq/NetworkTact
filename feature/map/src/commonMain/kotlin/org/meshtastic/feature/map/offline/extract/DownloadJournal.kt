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
import okio.FileSystem
import okio.Path

/**
 * What a download needs to resume after a cut: the identity of the source and of the region, the expected size, and
 * which fetch ranges are already in the file. The ranges themselves are not stored: on resume the plan is computed
 * again from the same edition (same [etag]), which gives the same ranges in the same order (`ExtractPlanner` and
 * `RangeMerger` are deterministic), and [totalSize] confirms it.
 *
 * Nothing here is secret, but the region is sensitive: the journal stays in the app's private files, next to the
 * partial map, and is deleted with it.
 */
@Serializable
data class DownloadJournal(
    val sourceUrl: String,
    val etag: String,
    val west: Double,
    val south: Double,
    val east: Double,
    val north: Double,
    val minZoom: Int,
    val maxZoom: Int,
    val totalSize: Long,
    val rangeCount: Int,
    val doneRanges: List<Int> = emptyList(),
    val startedAtEpochSeconds: Long,
    /** The map's id once finished (`pmtiles/<id>.pmtiles`) and the name it will be listed under. */
    val mapId: String = "",
    val name: String = "",
    val version: Int = VERSION,
) {
    val bounds: GeoBounds
        get() = GeoBounds(west, south, east, north)

    val isComplete: Boolean
        get() = doneRanges.size == rangeCount

    fun withDone(index: Int): DownloadJournal {
        require(index in 0 until rangeCount) { "range $index of $rangeCount" }
        return if (index in doneRanges) this else copy(doneRanges = (doneRanges + index).sorted())
    }

    companion object {
        const val VERSION = 1
        private val json = Json { ignoreUnknownKeys = true }

        /** Writes [journal] to [path] through a temporary file, so a cut never leaves half a journal. */
        fun save(fileSystem: FileSystem, path: Path, journal: DownloadJournal) {
            val temporary = path.parent!! / (path.name + ".tmp")
            fileSystem.write(temporary) { writeUtf8(json.encodeToString(serializer(), journal)) }
            fileSystem.atomicMove(temporary, path)
        }

        /** The journal at [path], or null when it is missing, unreadable or of another version. */
        fun load(fileSystem: FileSystem, path: Path): DownloadJournal? {
            if (!fileSystem.exists(path)) return null
            // Unreadable, not JSON or not a journal: it is ignored and the download starts again from scratch.
            return runCatching { json.decodeFromString(serializer(), fileSystem.read(path) { readUtf8() }) }
                .getOrNull()
                ?.takeIf { it.version == VERSION }
        }
    }
}
