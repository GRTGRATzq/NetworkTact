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

import okio.FileHandle
import okio.FileSystem
import okio.Path

/**
 * Writes a new archive into [path], section by section, each at its final place: the file is sized to
 * [ArchiveLayout.totalSize] when created, then the head (header, root, metadata, leaves) and every downloaded range are
 * written where the layout puts them, in any order. That is what allows parallel requests and a resume that only
 * fetches the ranges still missing.
 */
class ArchiveWriter private constructor(private val handle: FileHandle, private val layout: ArchiveLayout) :
    AutoCloseable {

    /** Writes everything but the tile data. */
    fun writeHead(header: PmTilesArchiveHeader, directories: PmTilesDirectories, metadata: ByteArray) {
        require(metadata.size.toLong() == layout.metadataLength) { "metadata of ${metadata.size} bytes" }
        write(0, header.toBytes())
        write(layout.rootOffset, directories.root)
        write(layout.metadataOffset, metadata)
        if (directories.leaves.isNotEmpty()) write(layout.leafDirectoriesOffset, directories.leaves)
    }

    /** Places every copy of [range], whose source bytes are [bytes], into the tile data section. */
    fun writeRange(range: FetchRange, bytes: ByteArray) {
        require(bytes.size.toLong() == range.length) { "${bytes.size} bytes for a range of ${range.length}" }
        for (copy in range.copies) {
            val from = (copy.sourceOffset - range.sourceOffset).toInt()
            handle.write(layout.tileDataOffset + copy.targetOffset, bytes, from, copy.length.toInt())
        }
    }

    /** Pushes what was written to the storage, so a range noted in the journal survives a power cut. */
    fun flush() = handle.flush()

    override fun close() = handle.close()

    private fun write(at: Long, bytes: ByteArray) = handle.write(at, bytes, 0, bytes.size)

    companion object {
        /** Creates [path], or reopens it to resume, sized to [layout]'s total. */
        fun open(fileSystem: FileSystem, path: Path, layout: ArchiveLayout): ArchiveWriter {
            val handle = fileSystem.openReadWrite(path)
            try {
                if (handle.size() != layout.totalSize) handle.resize(layout.totalSize)
            } catch (e: okio.IOException) {
                handle.close()
                throw e
            }
            return ArchiveWriter(handle, layout)
        }
    }
}
