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
package org.meshtastic.core.model.geo

import org.meshtastic.core.model.isModifiableBy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SharedPointWaypointTest {

    private val point =
        SharedPoint(
            id = 123_456,
            name = "FO 14:05 ALPHA-1",
            description = "fumée",
            point = LatLon(48.856_613, 2.352_222),
            expireEpochSeconds = 1_791_383_400L,
            lockedTo = 0x0A1A0001,
            icon = SharedPoint.ICON_OBSERVED_FACT,
        )

    @Test
    fun everyFieldGoesIntoTheWaypoint() {
        val waypoint = point.toWaypoint()
        assertEquals(123_456, waypoint.id)
        assertEquals(488_566_130, waypoint.latitude_i)
        assertEquals(23_522_220, waypoint.longitude_i)
        assertEquals(1_791_383_400, waypoint.expire)
        assertEquals(0x0A1A0001, waypoint.locked_to)
        assertEquals("FO 14:05 ALPHA-1", waypoint.name)
        assertEquals("fumée", waypoint.description)
        assertEquals(SharedPoint.ICON_OBSERVED_FACT, waypoint.icon)
    }

    @Test
    fun onlyTheSenderMayChangeOrRemoveIt() {
        val waypoint = point.toWaypoint()
        assertTrue(waypoint.isModifiableBy(0x0A1A0001))
        assertFalse(waypoint.isModifiableBy(0x0B2B0002))
    }
}
