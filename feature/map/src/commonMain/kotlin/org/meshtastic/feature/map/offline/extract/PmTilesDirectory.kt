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

/**
 * One entry of a PMTiles v3 directory (spec §4.1).
 *
 * A tile entry ([runLength] ≥ 1) says that the [runLength] tiles from [tileId] on share the bytes at [offset] (from the
 * start of the tile data section) for [length] bytes. A leaf entry ([runLength] = 0) points instead to a leaf directory
 * at [offset] in the leaf directories section, whose first tile is [tileId].
 */
data class PmTilesEntry(val tileId: Long, val offset: Long, val length: Long, val runLength: Long) {
    val isLeaf: Boolean
        get() = runLength == 0L

    /** Whether this tile entry holds [id]. Always false for a leaf entry. */
    operator fun contains(id: Long): Boolean = runLength > 0 && id >= tileId && id - tileId < runLength
}

/** A directory or header that does not follow the spec: the archive is corrupt or not a PMTiles v3 archive. */
class PmTilesFormatException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Throws [PmTilesFormatException] with [message] unless [condition] holds. */
internal inline fun requireFormat(condition: Boolean, message: () -> String) {
    if (!condition) throw PmTilesFormatException(message())
}

/**
 * Encoding and decoding of PMTiles v3 directories (spec §4.2 and §4.3), uncompressed: the caller applies the archive's
 * internal compression ([PmTilesCompression]).
 *
 * Layout: the number of entries, then every tile id as a delta from the previous one, every run length, every length,
 * and every offset as `offset + 1`, or 0 when the entry starts where the previous one ended.
 */
object PmTilesDirectory {
    /** A guard against a corrupt count: a real leaf directory holds tens of thousands of entries, not billions. */
    private const val MAX_ENTRIES = 10_000_000L

    fun decode(bytes: ByteArray): List<PmTilesEntry> {
        val reader = VarintReader(bytes)
        val count = reader.next()
        requireFormat(count in 1..MAX_ENTRIES) { "directory with $count entries" }
        val size = count.toInt()
        val ids = LongArray(size)
        var last = 0L
        for (i in 0 until size) {
            last += reader.next()
            ids[i] = last
        }
        val runs = LongArray(size) { reader.next() }
        val lengths = LongArray(size) { reader.next() }
        val offsets = LongArray(size)
        for (i in 0 until size) {
            val value = reader.next()
            requireFormat(value > 0 || i > 0) { "first entry without an offset" }
            offsets[i] = if (value > 0) value - 1 else offsets[i - 1] + lengths[i - 1]
        }
        requireFormat(reader.atEnd) { "${bytes.size - reader.position} bytes after the directory" }
        return List(size) { i ->
            requireFormat(lengths[i] > 0) { "entry $i has length ${lengths[i]}" }
            PmTilesEntry(ids[i], offsets[i], lengths[i], runs[i])
        }
    }

    fun encode(entries: List<PmTilesEntry>): ByteArray {
        require(entries.isNotEmpty()) { "a directory holds at least one entry" }
        val out = VarintWriter()
        out.write(entries.size.toLong())
        var last = 0L
        for (entry in entries) {
            require(entry.tileId >= last) { "tile ids must ascend: ${entry.tileId} after $last" }
            out.write(entry.tileId - last)
            last = entry.tileId
        }
        entries.forEach { out.write(it.runLength) }
        entries.forEach {
            require(it.length > 0) { "entry ${it.tileId} has length ${it.length}" }
            out.write(it.length)
        }
        entries.forEachIndexed { i, entry ->
            val previous = entries.getOrNull(i - 1)
            val contiguous = previous != null && entry.offset == previous.offset + previous.length
            out.write(if (contiguous) 0 else entry.offset + 1)
        }
        return out.toByteArray()
    }
}

/** Little-endian base-128 varints, as protobuf writes them. */
internal class VarintReader(private val bytes: ByteArray) {
    var position = 0
        private set

    val atEnd: Boolean
        get() = position == bytes.size

    fun next(): Long {
        var result = 0L
        var shift = 0
        while (true) {
            requireFormat(position < bytes.size) { "directory ends inside a number" }
            requireFormat(shift < MAX_SHIFT) { "number longer than 64 bits" }
            val byte = bytes[position++].toInt() and BYTE_MASK
            result = result or ((byte and PAYLOAD_MASK).toLong() shl shift)
            if (byte and CONTINUATION_BIT == 0) return result
            shift += BITS_PER_BYTE
        }
    }

    private companion object {
        const val BYTE_MASK = 0xFF
        const val PAYLOAD_MASK = 0x7F
        const val CONTINUATION_BIT = 0x80
        const val BITS_PER_BYTE = 7
        const val MAX_SHIFT = 64
    }
}

internal class VarintWriter {
    private var buffer = ByteArray(INITIAL_SIZE)
    private var size = 0

    fun write(value: Long) {
        require(value >= 0) { "negative value $value" }
        var rest = value
        while (rest >= CONTINUATION_BIT) {
            append(((rest and PAYLOAD_MASK) or CONTINUATION_BIT).toByte())
            rest = rest ushr BITS_PER_BYTE
        }
        append(rest.toByte())
    }

    fun toByteArray(): ByteArray = buffer.copyOf(size)

    private fun append(byte: Byte) {
        if (size == buffer.size) buffer = buffer.copyOf(buffer.size * 2)
        buffer[size++] = byte
    }

    private companion object {
        const val INITIAL_SIZE = 1024
        const val PAYLOAD_MASK = 0x7FL
        const val CONTINUATION_BIT = 0x80L
        const val BITS_PER_BYTE = 7
    }
}
