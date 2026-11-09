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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MapOrnamentsTest {

    private val fromFile = listOf("© OpenStreetMap")
    private val credit = "© contributeurs OpenStreetMap · Protomaps"

    @Test
    fun anImportedArchiveIsCreditedWithTheAppsTextWhateverItsMetadataSays() {
        assertEquals(listOf(credit), shownAttributions(fromFile, credit))
        assertEquals(listOf(credit), shownAttributions(emptyList(), credit))
        assertEquals(listOf(credit), shownAttributions(listOf("<a href=\"https://x\">Protomaps</a>", "© OSM"), credit))
    }

    @Test
    fun otherMapsKeepTheCreditsTheirStyleDeclares() {
        assertEquals(fromFile, shownAttributions(fromFile, null))
        assertEquals(fromFile, shownAttributions(fromFile, "  "))
    }

    @Test
    fun aLocalVectorMapCarriesItsCredit() {
        assertEquals(
            credit,
            Basemap.LocalVector("pmtiles:a1", "Var", styleJson = "{}", attribution = credit).attribution,
        )
        assertNull(Basemap.LocalVector("pmtiles:a1", "Var", styleJson = "{}").attribution)
    }
}
