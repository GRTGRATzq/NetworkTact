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
package org.meshtastic.core.model.firerequest

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class FireRequestTest {

    @Test
    fun the_sheet_has_eleven_boxes() {
        assertEquals(FireRequestSheet.BOX_COUNT, FireRequestSheet.BOX_TITLES.size)
    }

    @Test
    fun a_filled_request_survives_storage() {
        // The draft and the last request sent are kept as JSON on the phone.
        val request =
            FireRequest(
                origin = "OA",
                number = "AB1234",
                fireType = FireType(lines = setOf(ImpactLine.G), firstRound = true, setUp = "2"),
                targets = Targets(pickup = true, pickupMotion = Motion.MOVING),
                location = Location(g = LruLine("35", "T", "LL", "33000", "90300", "610"), tle = "2"),
                shape = Shape(kind = ShapeKind.CIRCULAR, radius = "100"),
                command = Command(whenReady = true, limitedStart = "14H30"),
            )
        val json = Json.encodeToString(FireRequest.serializer(), request)
        assertEquals(request, Json.decodeFromString(FireRequest.serializer(), json))
    }
}
