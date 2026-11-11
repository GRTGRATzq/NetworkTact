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

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PmTilesDirectoryTest {

    @Test
    fun encodesTheExamplesOfTheSpecification() {
        // Spec §4.2: ids 5, 42, 69 are written as 5, 37, 27; contiguous offsets as 0.
        val entries =
            listOf(
                PmTilesEntry(tileId = 5, offset = 0, length = 10, runLength = 1),
                PmTilesEntry(tileId = 42, offset = 10, length = 20, runLength = 3),
                PmTilesEntry(tileId = 69, offset = 100, length = 5, runLength = 1),
            )
        val bytes = PmTilesDirectory.encode(entries)
        assertContentEquals(byteArrayOf(3, 5, 37, 27, 1, 3, 1, 10, 20, 5, 1, 0, 101), bytes)
        assertEquals(entries, PmTilesDirectory.decode(bytes))
    }

    @Test
    fun roundTripsLargeNumbersRunsAndLeaves() {
        val entries =
            listOf(
                PmTilesEntry(tileId = 0, offset = 0, length = 66_800, runLength = 1),
                PmTilesEntry(tileId = 1_267_142_209, offset = 139_052_507_000, length = 37_796, runLength = 1),
                PmTilesEntry(tileId = 1_267_142_210, offset = 139_052_544_796, length = 300, runLength = 400_000_000),
                PmTilesEntry(tileId = 1_431_655_000, offset = 353_000_000, length = 120_000, runLength = 0),
            )
        assertEquals(entries, PmTilesDirectory.decode(PmTilesDirectory.encode(entries)))
        assertEquals(true, entries[3].isLeaf)
        assertEquals(true, 1_267_142_210L + 399_999_999 in entries[2])
        assertEquals(false, 1_267_142_210L + 400_000_000 in entries[2])
        assertEquals(false, 1_431_655_000L in entries[3])
    }

    @Test
    fun aSharedTileKeepsItsOffset() {
        // Deduplicated content: two entries pointing to the same bytes, the second one before the end of the first.
        val entries =
            listOf(
                PmTilesEntry(tileId = 10, offset = 0, length = 50, runLength = 1),
                PmTilesEntry(tileId = 11, offset = 50, length = 30, runLength = 1),
                PmTilesEntry(tileId = 12, offset = 0, length = 50, runLength = 1),
            )
        assertEquals(entries, PmTilesDirectory.decode(PmTilesDirectory.encode(entries)))
    }

    @Test
    fun refusesCorruptDirectories() {
        assertFailsWith<PmTilesFormatException> { PmTilesDirectory.decode(byteArrayOf()) }
        assertFailsWith<PmTilesFormatException> { PmTilesDirectory.decode(byteArrayOf(0)) }
        // One entry, cut before its offset.
        assertFailsWith<PmTilesFormatException> { PmTilesDirectory.decode(byteArrayOf(1, 5, 1, 10)) }
        // First offset written as "contiguous".
        assertFailsWith<PmTilesFormatException> { PmTilesDirectory.decode(byteArrayOf(1, 5, 1, 10, 0)) }
        // Length zero.
        assertFailsWith<PmTilesFormatException> { PmTilesDirectory.decode(byteArrayOf(1, 5, 1, 0, 1)) }
        // Trailing bytes.
        assertFailsWith<PmTilesFormatException> { PmTilesDirectory.decode(byteArrayOf(1, 5, 1, 10, 1, 7)) }
        // A number that never ends.
        assertFailsWith<PmTilesFormatException> { PmTilesDirectory.decode(ByteArray(12) { -1 }) }
    }

    @Test
    fun refusesToEncodeUnsortedOrEmptyDirectories() {
        assertFailsWith<IllegalArgumentException> { PmTilesDirectory.encode(emptyList()) }
        assertFailsWith<IllegalArgumentException> {
            PmTilesDirectory.encode(listOf(PmTilesEntry(9, 0, 1, 1), PmTilesEntry(8, 1, 1, 1)))
        }
    }

    @Test
    fun gzipRoundTripsAndRefusesGarbage() {
        val bytes = PmTilesDirectory.encode(List(1000) { PmTilesEntry(it.toLong(), it * 10L, 10, 1) })
        val packed = PmTilesCompression.compress(bytes, PmTilesCompression.GZIP)
        assertEquals(0x1f, packed[0].toInt() and 0xFF)
        assertEquals(0x8b, packed[1].toInt() and 0xFF)
        assertContentEquals(bytes, PmTilesCompression.decompress(packed, PmTilesCompression.GZIP))
        assertContentEquals(bytes, PmTilesCompression.decompress(bytes, PmTilesCompression.NONE))
        assertFailsWith<PmTilesFormatException> { PmTilesCompression.decompress(bytes, PmTilesCompression.GZIP) }
        assertFailsWith<PmTilesFormatException> { PmTilesCompression.decompress(packed, PmTilesCompression.BROTLI) }
    }
}
