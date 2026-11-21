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

import org.meshtastic.core.model.util.isValidCodePoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WaypointAffiliationTest {

    @Test
    fun codesAreTheFourPrivateUseCodePoints() {
        assertEquals(0xE001, WaypointAffiliation.FRIEND.code)
        assertEquals(0xE002, WaypointAffiliation.HOSTILE.code)
        assertEquals(0xE003, WaypointAffiliation.NEUTRAL.code)
        assertEquals(0xE004, WaypointAffiliation.UNKNOWN.code)
    }

    @Test
    fun everyCodeSurvivesTheReceiveSideCheck() {
        WaypointAffiliation.entries.forEach { assertTrue(it.code.isValidCodePoint(), it.name) }
    }

    @Test
    fun eachCodeReadsBackAsItsAffiliation() {
        WaypointAffiliation.entries.forEach { assertEquals(it, WaypointAffiliation.fromIcon(it.code)) }
    }

    @Test
    fun anythingElseIsUnknown() {
        listOf(null, 0, -1, 0x1F4CD, 0x1F441, 0x1F4CC, 0xE000, 0xE005, 0x110000).forEach {
            assertEquals(WaypointAffiliation.UNKNOWN, WaypointAffiliation.fromIcon(it), "icon $it")
        }
    }

    @Test
    fun newWaypointsStartUnknown() {
        assertEquals(WaypointAffiliation.UNKNOWN, WaypointAffiliation.DEFAULT)
    }
}
