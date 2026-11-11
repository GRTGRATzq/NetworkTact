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

import org.meshtastic.feature.map.offline.PmTilesCheck
import org.meshtastic.feature.map.offline.PmTilesHeader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class PmTilesArchiveHeaderTest {

    /** The header of the Protomaps world build of 11 November 2026, as the probe read it. */
    private val world =
        PmTilesArchiveHeader(
            rootOffset = 127,
            rootLength = 15_567,
            metadataOffset = 139_052_524_263,
            metadataLength = 1_179,
            leafDirectoriesOffset = 139_052_525_442,
            leafDirectoriesLength = 353_548_914,
            tileDataOffset = 16_384,
            tileDataLength = 139_052_507_879,
            addressedTiles = 1_431_655_765,
            tileEntries = 178_630_665,
            tileContents = 136_716_699,
            clustered = true,
            internalCompression = PmTilesCompression.GZIP,
            tileCompression = PmTilesCompression.GZIP,
            tileType = 1,
            minZoom = 0,
            maxZoom = 15,
            minLonE7 = -1_800_000_000,
            minLatE7 = -850_511_287,
            maxLonE7 = 1_800_000_000,
            maxLatE7 = 850_511_287,
            centerZoom = 0,
            centerLonE7 = 0,
            centerLatE7 = 0,
        )

    @Test
    fun roundTripsEveryField() {
        val bytes = world.toBytes()
        assertEquals(PmTilesArchiveHeader.SIZE, bytes.size)
        assertEquals(world, PmTilesArchiveHeader.parse(bytes))
    }

    @Test
    fun theImportCheckReadsAWrittenHeaderTheSameWay() {
        val local =
            world.copy(
                rootLength = 9_000,
                metadataOffset = 9_127,
                leafDirectoriesOffset = 10_306,
                leafDirectoriesLength = 0,
                tileDataOffset = 10_306,
                tileDataLength = 12_716_161,
                minLonE7 = 244_126_000,
                minLatE7 = 456_475_000,
                maxLonE7 = 251_874_000,
                maxLatE7 = 461_865_000,
            )
        val checked = assertIs<PmTilesCheck.Valid>(PmTilesHeader.check(local.toBytes())).header
        assertEquals(10_306L + 12_716_161L, checked.minimumFileSize)
        assertEquals(9_127L, checked.metadataOffset)
        assertEquals(1_179L, checked.metadataLength)
        assertEquals(0, checked.minZoom)
        assertEquals(15, checked.maxZoom)
        assertEquals(24.4126, checked.minLongitude, 1e-7)
        assertEquals(46.1865, checked.maxLatitude, 1e-7)
    }

    @Test
    fun refusesOtherFiles() {
        assertFailsWith<PmTilesFormatException> { PmTilesArchiveHeader.parse(ByteArray(127)) }
        assertFailsWith<PmTilesFormatException> { PmTilesArchiveHeader.parse(world.toBytes().copyOf(100)) }
        val v2 = world.toBytes().also { it[7] = 2 }
        assertFailsWith<PmTilesFormatException> { PmTilesArchiveHeader.parse(v2) }
    }
}
