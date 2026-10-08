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
package org.meshtastic.feature.map.offline

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PmTilesHeaderTest {

    /** A v3 header laid out as the spec says: magic, version, four offset/length pairs, then the tile description. */
    private fun header(version: Int = 3, tileType: Int = 1, tileDataEnd: Long = 5_000_000L): ByteArray {
        val bytes = ByteArray(PmTilesHeader.SIZE)
        "PMTiles".encodeToByteArray().copyInto(bytes)
        bytes[7] = version.toByte()
        putLong(bytes, 8, 127L) // root directory offset
        putLong(bytes, 16, 300L) // root directory length
        putLong(bytes, 24, 427L) // metadata offset
        putLong(bytes, 32, 200L) // metadata length
        putLong(bytes, 56, 1_000L) // tile data offset
        putLong(bytes, 64, tileDataEnd - 1_000L) // tile data length
        bytes[99] = tileType.toByte()
        bytes[100] = 0
        bytes[101] = 15
        putInt(bytes, 102, 23_200_000) // 2.32° E
        putInt(bytes, 106, 488_100_000) // 48.81° N
        putInt(bytes, 110, 24_700_000)
        putInt(bytes, 114, 489_100_000)
        return bytes
    }

    private fun putLong(bytes: ByteArray, offset: Int, value: Long) {
        for (i in 0 until 8) bytes[offset + i] = (value shr (8 * i)).toByte()
    }

    private fun putInt(bytes: ByteArray, offset: Int, value: Int) {
        for (i in 0 until 4) bytes[offset + i] = (value shr (8 * i)).toByte()
    }

    @Test
    fun readsZoomBoundsAndExpectedSizeOfVectorArchive() {
        val result = assertIs<PmTilesCheck.Valid>(PmTilesHeader.check(header()))
        assertEquals(0, result.header.minZoom)
        assertEquals(15, result.header.maxZoom)
        assertEquals(2.32, result.header.minLongitude, 1e-9)
        assertEquals(48.81, result.header.minLatitude, 1e-9)
        assertEquals(2.47, result.header.maxLongitude, 1e-9)
        assertEquals(48.91, result.header.maxLatitude, 1e-9)
        assertEquals(5_000_000L, result.header.minimumFileSize)
    }

    @Test
    fun readsNegativeCoordinatesWestAndSouth() {
        val bytes = header()
        putInt(bytes, 102, -17_500_000)
        putInt(bytes, 106, -335_000_000)
        val result = assertIs<PmTilesCheck.Valid>(PmTilesHeader.check(bytes))
        assertEquals(-1.75, result.header.minLongitude, 1e-9)
        assertEquals(-33.5, result.header.minLatitude, 1e-9)
    }

    @Test
    fun refusesFileThatIsNotPmTiles() {
        val zip = ByteArray(PmTilesHeader.SIZE).also { "PK".encodeToByteArray().copyInto(it) }
        assertEquals(PmTilesCheck.NotPmTiles, PmTilesHeader.check(zip))
    }

    @Test
    fun refusesFileShorterThanHeader() {
        assertEquals(PmTilesCheck.NotPmTiles, PmTilesHeader.check(header().copyOf(60)))
    }

    @Test
    fun refusesOtherFormatVersion() {
        assertEquals(PmTilesCheck.UnsupportedVersion, PmTilesHeader.check(header(version = 2)))
    }

    @Test
    fun refusesRasterArchive() {
        assertEquals(PmTilesCheck.NotVectorTiles, PmTilesHeader.check(header(tileType = 2)))
    }

    @Test
    fun refusesHeaderWithImpossibleSectionSize() {
        val bytes = header()
        putLong(bytes, 64, Long.MAX_VALUE)
        assertEquals(PmTilesCheck.NotPmTiles, PmTilesHeader.check(bytes))
    }
}
