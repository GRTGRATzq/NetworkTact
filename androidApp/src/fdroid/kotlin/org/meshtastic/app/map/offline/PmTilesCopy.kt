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
package org.meshtastic.app.map.offline

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.meshtastic.feature.map.maplibre.component.OfflineMapImportFailure
import org.meshtastic.feature.map.offline.PmTilesCheck
import org.meshtastic.feature.map.offline.PmTilesHeader
import org.meshtastic.feature.map.offline.hasRoomForImport
import org.meshtastic.feature.map.offline.requiredFreeBytes
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

private const val BUFFER_BYTES = 256 * 1024
private const val PROGRESS_STEP_BYTES = 4L * 1024 * 1024

/**
 * Copies a picked .pmtiles file from [input] into [target], checking it on the way.
 *
 * The header is read and checked before a single byte is written, so a wrong file is refused at once; the free space of
 * [target]'s directory is checked against [declaredSize] next; the copy is then checked for length. Any failure throws
 * [PmTilesCopyException] and the caller deletes [target]: it is a temporary file outside the maps directory, so nothing
 * half-copied is ever listed as a map. Cancelling the calling coroutine stops the copy between two buffers.
 *
 * @param onProgress Bytes copied so far, reported every few megabytes.
 * @return the checked header.
 */
internal suspend fun copyPmTiles(
    input: InputStream,
    target: File,
    declaredSize: Long?,
    onProgress: (Long) -> Unit,
): PmTilesHeader {
    val headerBytes = ByteArray(PmTilesHeader.SIZE)
    val header = checkedHeader(headerBytes.copyOf(input.readFully(headerBytes)), declaredSize)
    checkFreeSpace(checkNotNull(target.parentFile), declaredSize)

    var copied = 0L
    FileOutputStream(target).use { output ->
        output.write(headerBytes)
        copied += headerBytes.size
        val buffer = ByteArray(BUFFER_BYTES)
        var reported = 0L
        while (true) {
            currentCoroutineContext().ensureActive()
            val read = input.read(buffer)
            if (read < 0) break
            output.write(buffer, 0, read)
            copied += read
            if (copied - reported >= PROGRESS_STEP_BYTES) {
                reported = copied
                onProgress(copied)
            }
        }
        // On disk before the file is moved into place, so a map listed after a power cut is a whole one.
        output.fd.sync()
    }
    onProgress(copied)

    val truncated = copied < header.minimumFileSize || (declaredSize != null && copied != declaredSize)
    if (truncated) throw PmTilesCopyException(OfflineMapImportFailure.Incomplete)
    return header
}

/** The header of a PMTiles v3 vector map, or the reason the file is not one. */
private fun checkedHeader(bytes: ByteArray, declaredSize: Long?): PmTilesHeader {
    val failure =
        when (val check = PmTilesHeader.check(bytes)) {
            is PmTilesCheck.Valid ->
                if (declaredSize != null && declaredSize < check.header.minimumFileSize) {
                    OfflineMapImportFailure.Incomplete
                } else {
                    return check.header
                }

            PmTilesCheck.NotPmTiles -> OfflineMapImportFailure.NotPmTiles

            PmTilesCheck.UnsupportedVersion -> OfflineMapImportFailure.UnsupportedVersion

            PmTilesCheck.NotVectorTiles -> OfflineMapImportFailure.NotVectorTiles
        }
    throw PmTilesCopyException(failure)
}

private fun checkFreeSpace(directory: File, declaredSize: Long?) {
    val free = directory.usableSpace
    if (declaredSize != null && !hasRoomForImport(declaredSize, free)) {
        throw PmTilesCopyException(
            OfflineMapImportFailure.NoSpace,
            neededBytes = requiredFreeBytes(declaredSize),
            freeBytes = free,
        )
    }
}

/** Reads until [buffer] is full or the stream ends; returns the bytes read. */
private fun InputStream.readFully(buffer: ByteArray): Int {
    var total = 0
    while (total < buffer.size) {
        val read = read(buffer, total, buffer.size - total)
        if (read < 0) break
        total += read
    }
    return total
}
