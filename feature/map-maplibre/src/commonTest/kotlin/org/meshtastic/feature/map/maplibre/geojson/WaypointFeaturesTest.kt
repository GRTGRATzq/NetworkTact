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
package org.meshtastic.feature.map.maplibre.geojson

import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import org.meshtastic.core.model.DataPacket
import org.meshtastic.core.model.NodeAddress
import org.meshtastic.core.model.geo.WaypointAffiliation
import org.meshtastic.proto.Waypoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WaypointFeaturesTest {

    private fun packet(waypoint: Waypoint) = DataPacket(to = NodeAddress.ID_BROADCAST, channel = 0, waypoint = waypoint)

    @Test
    fun `a waypoint with both coordinates becomes a feature`() {
        val features =
            waypointsToFeatureCollection(
                listOf(
                    packet(
                        Waypoint.Builder()
                            .also { wb ->
                                wb.id = 1
                                wb.latitude_i = 416_000_000
                                wb.longitude_i = -936_000_000
                            }
                            .build(),
                    ),
                ),
            )

        assertEquals(1, features.features.size)
    }

    @Test
    fun `a waypoint missing one coordinate is dropped rather than pinned to the equator`() {
        // Substituting 0 for the absent ordinate put a confident marker at the wrong place, which is worse than none.
        val latitudeOnly =
            packet(
                Waypoint.Builder()
                    .also { wb ->
                        wb.id = 2
                        wb.latitude_i = 416_000_000
                    }
                    .build(),
            )
        val longitudeOnly =
            packet(
                Waypoint.Builder()
                    .also { wb ->
                        wb.id = 3
                        wb.longitude_i = -936_000_000
                    }
                    .build(),
            )

        assertTrue(waypointsToFeatureCollection(listOf(latitudeOnly, longitudeOnly)).features.isEmpty())
    }

    @Test
    fun `null island is still dropped`() {
        assertTrue(
            waypointsToFeatureCollection(
                listOf(
                    packet(
                        Waypoint.Builder()
                            .also { wb ->
                                wb.id = 4
                                wb.latitude_i = 0
                                wb.longitude_i = 0
                            }
                            .build(),
                    ),
                ),
            )
                .features
                .isEmpty(),
        )
    }

    private fun located(id: Int, icon: Int) = packet(
        Waypoint.Builder()
            .also { wb ->
                wb.id = id
                wb.latitude_i = 416_000_000
                wb.longitude_i = -936_000_000
                wb.icon = icon
            }
            .build(),
    )

    private fun affiliationOf(icon: Int): String? = waypointsToFeatureCollection(listOf(located(1, icon)))
        .features
        .single()
        .properties
        ?.get(WaypointFeatureKeys.AFFILIATION)
        ?.jsonPrimitive
        ?.content

    @Test
    fun `each affiliation code becomes its affiliation`() {
        WaypointAffiliation.entries.forEach { assertEquals(it.name, affiliationOf(it.code)) }
    }

    @Test
    fun `an unset, emoji or broken icon is shown as unknown`() {
        // Whatever the sending node put in the field, an old emoji included.
        listOf(0, 0x1F4CD, 0x1F680, 'A'.code, -1, 0xD800, 0x110000).forEach {
            assertEquals(WaypointAffiliation.UNKNOWN.name, affiliationOf(it), "icon $it")
        }
    }

    @Test
    fun `only the waypoint whose card is open is selected`() {
        val code = WaypointAffiliation.FRIEND.code
        val features = waypointsToFeatureCollection(listOf(located(1, code), located(2, code)), selectedWaypointId = 2)
        val selected: Map<Int?, Boolean?> =
            features.features.associate { feature ->
                feature.properties?.get(WaypointFeatureKeys.WAYPOINT_ID)?.jsonPrimitive?.int to
                    feature.properties?.get(WaypointFeatureKeys.IS_SELECTED)?.jsonPrimitive?.boolean
            }
        assertEquals(mapOf<Int?, Boolean?>(1 to false, 2 to true), selected)
    }

    @Test
    fun `nothing is selected without an open card`() {
        val features = waypointsToFeatureCollection(listOf(located(1, 0)))
        assertEquals(
            false,
            features.features.single().properties?.get(WaypointFeatureKeys.IS_SELECTED)?.jsonPrimitive?.boolean,
        )
    }
}
