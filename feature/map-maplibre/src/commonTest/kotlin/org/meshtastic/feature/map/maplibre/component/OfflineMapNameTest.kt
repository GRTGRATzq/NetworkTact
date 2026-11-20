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
package org.meshtastic.feature.map.maplibre.component

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** A4: the name a map is given from « Cartes sur le téléphone ». */
class OfflineMapNameTest {

    @Test
    fun `a name is tidied, its blanks reduced to single spaces`() {
        assertEquals("Cincu nord", offlineMapName("  Cincu   nord "))
        assertEquals("Cincu nord", offlineMapName("Cincu\tnord\n"))
        assertEquals("Camp de Cincu (z14)", offlineMapName("Camp de Cincu (z14)"))
    }

    @Test
    fun `an empty name or one that is too long keeps the map's name`() {
        assertNull(offlineMapName(""))
        assertNull(offlineMapName("   "))
        assertNull(offlineMapName("x".repeat(MAX_OFFLINE_MAP_NAME_CHARS + 1)))
        assertEquals("x".repeat(MAX_OFFLINE_MAP_NAME_CHARS), offlineMapName("x".repeat(MAX_OFFLINE_MAP_NAME_CHARS)))
    }
}
