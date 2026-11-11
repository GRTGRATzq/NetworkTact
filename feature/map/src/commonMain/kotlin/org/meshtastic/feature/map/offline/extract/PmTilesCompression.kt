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

import okio.Buffer
import okio.GzipSink
import okio.GzipSource
import okio.buffer
import okio.use

/** The compression codes of a PMTiles v3 header (spec §3.3). */
object PmTilesCompression {
    const val UNKNOWN = 0
    const val NONE = 1
    const val GZIP = 2
    const val BROTLI = 3
    const val ZSTD = 4

    /** Directories and metadata larger than this once inflated are refused: a guard against a corrupt archive. */
    const val MAX_INFLATED_BYTES = 64L * 1024 * 1024

    /** Undoes [compression] on [bytes]. Only none and gzip exist in the Protomaps builds (probe, 11 November 2026). */
    fun decompress(bytes: ByteArray, compression: Int): ByteArray = when (compression) {
        NONE -> bytes
        GZIP -> gunzip(bytes)
        else -> throw PmTilesFormatException("unsupported compression $compression")
    }

    fun compress(bytes: ByteArray, compression: Int): ByteArray = when (compression) {
        NONE -> bytes
        GZIP -> gzip(bytes)
        else -> throw PmTilesFormatException("unsupported compression $compression")
    }

    private fun gunzip(bytes: ByteArray): ByteArray {
        val out = Buffer()
        try {
            GzipSource(Buffer().write(bytes)).buffer().use { source ->
                while (source.read(out, CHUNK) != -1L) {
                    if (out.size > MAX_INFLATED_BYTES) {
                        throw PmTilesFormatException("inflates to more than $MAX_INFLATED_BYTES bytes")
                    }
                }
            }
        } catch (e: okio.IOException) {
            throw PmTilesFormatException("not valid gzip: ${e.message}", e)
        }
        return out.readByteArray()
    }

    private fun gzip(bytes: ByteArray): ByteArray {
        val out = Buffer()
        GzipSink(out).buffer().use { it.write(bytes) }
        return out.readByteArray()
    }

    private const val CHUNK = 64L * 1024
}
