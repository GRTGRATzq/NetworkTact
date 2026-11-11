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
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DownloadedMapInfoTest {

    @Test
    fun theDescriptionWrittenAfterADownloadIsReadBack() {
        val text =
            """{"sourceHost":"build.protomaps.com","editionDate":"20261111","west":24.4,"south":45.6,""" +
                """"east":25.2,"north":46.2,"minZoom":0,"maxZoom":15,"downloadedAtEpochSeconds":1762862400}"""
        val info = DownloadedMapInfo.decode(text)
        assertEquals("build.protomaps.com", info?.sourceHost)
        assertEquals("20261111", info?.editionDate)
        assertEquals(GeoBounds(24.4, 45.6, 25.2, 46.2), info?.bounds)
        assertEquals(15, info?.maxZoom)
    }

    @Test
    fun aSingleFileSourceHasNoEditionDateAndUnknownKeysAreIgnored() {
        val text =
            """{"sourceHost":"maps.example.org","west":1.0,"south":2.0,"east":3.0,"north":4.0,""" +
                """"minZoom":0,"maxZoom":14,"downloadedAtEpochSeconds":1,"later":"field"}"""
        val info = DownloadedMapInfo.decode(text)
        assertNull(info?.editionDate)
        assertEquals("maps.example.org", info?.sourceHost)
    }

    @Test
    fun anythingElseIsNotADescription() {
        assertNull(DownloadedMapInfo.decode(""))
        assertNull(DownloadedMapInfo.decode("not json"))
        assertNull(DownloadedMapInfo.decode("""{"sourceHost":"build.protomaps.com"}"""))
    }
}
