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

import org.meshtastic.feature.map.maplibre.style.Basemap
import org.meshtastic.feature.map.maplibre.style.Basemaps
import org.meshtastic.feature.map.tiles.RasterTileSpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BasemapResolutionTest {

    private val onlineSource =
        Basemap.Raster("url1", "Serveur", RasterTileSpec(tiles = listOf("https://tiles.example.org/{z}/{x}/{y}.png")))
    private val mbtiles =
        Basemap.Raster("mb1", "Camp", RasterTileSpec(tiles = listOf("mbtiles:///data/mbtiles/camp.mbtiles")))
    private val pmtiles = Basemap.LocalVector("pmtiles:a1", "Var", styleJson = "{}")

    @Test
    fun withoutLocalMapTheStoredBuiltInStyleIsKept() {
        assertEquals(Basemaps.all[1], resolveBasemap(storedCustomId = null, styleIndex = 1, customs = emptyList()))
        assertEquals(Basemaps.all[1], resolveBasemap(null, 1, listOf(onlineSource)))
    }

    @Test
    fun builtInStyleChosenExplicitlyIsNotOverriddenByLocalMap() {
        assertEquals(Basemaps.all[2], resolveBasemap(storedCustomId = null, styleIndex = 2, customs = listOf(pmtiles)))
    }

    @Test
    fun storedLocalMapIsUsedWhateverItsFormat() {
        assertEquals(pmtiles, resolveBasemap("pmtiles:a1", 0, listOf(mbtiles, pmtiles)))
        assertEquals(mbtiles, resolveBasemap("mb1", 0, listOf(mbtiles, pmtiles)))
    }

    @Test
    fun deletedMapFallsBackToAnotherLocalMap() {
        assertEquals(mbtiles, resolveBasemap("pmtiles:gone", 0, listOf(onlineSource, mbtiles)))
    }

    @Test
    fun deletedMapWithoutAnyOtherLocalMapFallsBackToBuiltInStyle() {
        assertEquals(Basemaps.all[0], resolveBasemap("pmtiles:gone", 0, listOf(onlineSource)))
    }

    @Test
    fun unknownStyleIndexFallsBackToDefault() {
        assertEquals(Basemaps.default, resolveBasemap(null, 99, emptyList()))
    }

    @Test
    fun onlyArchivesOnTheDeviceCountAsLocal() {
        assertTrue(mbtiles.isLocal)
        assertTrue(pmtiles.isLocal)
        assertFalse(onlineSource.isLocal)
        assertTrue(Basemaps.all.none { it.isLocal })
    }
}
