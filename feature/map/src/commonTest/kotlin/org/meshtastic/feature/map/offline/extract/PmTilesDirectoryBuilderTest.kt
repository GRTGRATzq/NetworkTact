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

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PmTilesDirectoryBuilderTest {

    /** [count] contiguous tiles from [firstId] with irregular sizes, like real tiles. */
    private fun tiles(count: Int, firstId: Long = 1_000L): List<PmTilesEntry> {
        val random = Random(7)
        var offset = 0L
        return List(count) { i ->
            val length = 200L + random.nextInt(40_000)
            PmTilesEntry(firstId + i * 3L, offset, length, 1).also { offset += length }
        }
    }

    private fun gunzip(bytes: ByteArray) = PmTilesCompression.decompress(bytes, PmTilesCompression.GZIP)

    @Test
    fun aSmallRegionFitsInTheRoot() {
        val entries = tiles(5_485)
        val built = PmTilesDirectoryBuilder.build(entries, PmTilesCompression.GZIP)
        assertEquals(0, built.leafCount)
        assertEquals(0, built.leaves.size)
        assertTrue(built.root.size <= PmTilesDirectoryBuilder.MAX_ROOT_BYTES, "root ${built.root.size} bytes")
        assertEquals(entries, PmTilesDirectory.decode(gunzip(built.root)))
    }

    @Test
    fun aLargeRegionGetsLeavesAndEveryEntryIsFoundAgain() {
        val entries = tiles(200_000)
        val built = PmTilesDirectoryBuilder.build(entries, PmTilesCompression.GZIP)
        assertTrue(built.leafCount > 1, "${built.leafCount} leaves")
        assertTrue(built.root.size <= PmTilesDirectoryBuilder.MAX_ROOT_BYTES, "root ${built.root.size} bytes")
        val root = PmTilesDirectory.decode(gunzip(built.root))
        assertTrue(root.all { it.isLeaf })
        val found =
            root.flatMap { leaf ->
                val bytes = built.leaves.copyOfRange(leaf.offset.toInt(), (leaf.offset + leaf.length).toInt())
                PmTilesDirectory.decode(gunzip(bytes)).also { assertEquals(leaf.tileId, it.first().tileId) }
            }
        assertEquals(entries, found)
    }
}
