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

import okio.FileSystem
import okio.Path
import org.meshtastic.feature.map.offline.PmTilesCheck
import org.meshtastic.feature.map.offline.PmTilesHeader

/**
 * The last check of a downloaded map, before it is moved among the maps: the exact length, a header the import would
 * accept, every directory decodable, and a few tiles that are what the header says (gzip starts with 1f 8b). Throws
 * [MapDownloadException.Incomplete] otherwise, and the caller deletes the file.
 */
object ArchiveCheck {
    private const val GZIP_MAGIC_1 = 0x1f
    private const val GZIP_MAGIC_2 = 0x8b
    private const val BYTE_MASK = 0xFF
    private const val SAMPLES = 3

    fun verify(fileSystem: FileSystem, path: Path, expectedSize: Long) {
        try {
            fileSystem.openReadOnly(path).use { handle ->
                val size = handle.size()
                requireComplete(size == expectedSize) { "$size bytes, $expectedSize expected" }
                fun read(offset: Long, length: Long): ByteArray {
                    val bytes = ByteArray(length.toInt())
                    var done = 0
                    while (done < bytes.size) {
                        val n = handle.read(offset + done, bytes, done, bytes.size - done)
                        requireComplete(n > 0) { "short read at ${offset + done}" }
                        done += n
                    }
                    return bytes
                }
                val headerBytes = read(0, PmTilesArchiveHeader.SIZE.toLong())
                val light = PmTilesHeader.check(headerBytes)
                requireComplete(light is PmTilesCheck.Valid && light.header.minimumFileSize == size) {
                    "header refused: $light"
                }
                val header = PmTilesArchiveHeader.parse(headerBytes)
                val tiles = ArrayList<PmTilesEntry>()
                val root = decode(read(header.rootOffset, header.rootLength), header)
                for (entry in root) {
                    if (entry.isLeaf) {
                        tiles += decode(read(header.leafDirectoriesOffset + entry.offset, entry.length), header)
                    } else {
                        tiles += entry
                    }
                }
                requireComplete(tiles.isNotEmpty() && tiles.none { it.isLeaf }) { "no tiles, or nested leaves" }
                requireComplete(tiles.sumOf { it.runLength } == header.addressedTiles) { "tile count differs" }
                val samples = listOf(tiles.first(), tiles[tiles.size / 2], tiles.last()).take(SAMPLES)
                for (tile in samples) {
                    val start = read(header.tileDataOffset + tile.offset, minOf(tile.length, 2L))
                    val gzip =
                        header.tileCompression != PmTilesCompression.GZIP ||
                            (
                                start.size == 2 &&
                                    (start[0].toInt() and BYTE_MASK) == GZIP_MAGIC_1 &&
                                    (start[1].toInt() and BYTE_MASK) == GZIP_MAGIC_2
                                )
                    requireComplete(gzip) { "tile ${tile.tileId} is not gzip" }
                }
            }
        } catch (e: PmTilesFormatException) {
            throw MapDownloadException.Incomplete(e.message ?: "corrupt", e)
        }
    }

    private fun decode(bytes: ByteArray, header: PmTilesArchiveHeader): List<PmTilesEntry> =
        PmTilesDirectory.decode(PmTilesCompression.decompress(bytes, header.internalCompression))

    private inline fun requireComplete(condition: Boolean, detail: () -> String) {
        if (!condition) throw MapDownloadException.Incomplete(detail())
    }
}
