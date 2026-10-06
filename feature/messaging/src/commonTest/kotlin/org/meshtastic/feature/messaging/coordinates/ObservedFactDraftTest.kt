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
package org.meshtastic.feature.messaging.coordinates

import kotlinx.datetime.TimeZone
import org.meshtastic.core.model.geo.CoordinateFormat
import org.meshtastic.core.model.geo.CoordinateParser
import org.meshtastic.core.model.geo.ObservedTime
import org.meshtastic.feature.messaging.priority.MessagePriority
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ObservedFactDraftTest {

    // 14:30 UTC on 6 October 2026.
    private val now = 1_791_297_000L

    private fun draft(place: String, format: CoordinateFormat, description: String, time: String = "14:05") =
        observedFactDraft(CoordinateParser.parse(place, format), format, description, time, now, TimeZone.UTC)

    @Test
    fun valid_place_and_description_give_a_report_filed_as_cr() {
        val draft = draft("31U 448251 5411932", CoordinateFormat.UTM, "deux véhicules arrêtés")
        assertEquals(
            "[CR] FAIT OBSERVÉ · 14:05 · Lieu UTM 31U 448251 5411932 = MGRS 31U DQ 48251 11932 · deux véhicules arrêtés",
            draft.message,
        )
        assertTrue(draft.canInsert)
        assertEquals(MessagePriority.REPORT, MessagePriority.of(checkNotNull(draft.message)))
    }

    @Test
    fun invalid_place_gives_nothing() {
        val draft = draft("31U 448251", CoordinateFormat.UTM, "fumée")
        assertNull(draft.message)
        assertFalse(draft.canInsert)
    }

    @Test
    fun description_is_required() {
        assertFalse(draft("31U DQ 48251 11932", CoordinateFormat.MGRS, "   ").canInsert)
    }

    @Test
    fun over_200_bytes_is_blocked_not_truncated() {
        val draft = draft("31U DQ 48251 11932", CoordinateFormat.MGRS, "é".repeat(100))
        assertTrue(draft.bytes > 200)
        assertFalse(draft.canInsert)
        assertTrue(checkNotNull(draft.message).endsWith("é".repeat(100)))
    }

    @Test
    fun observationTimeGoesIntoTheReport() {
        val draft = draft("31U DQ 48251 11932", CoordinateFormat.MGRS, "fumée", time = "9h05")
        assertEquals("[CR] FAIT OBSERVÉ · 09:05 · Lieu MGRS 31U DQ 48251 11932 · fumée", draft.message)
        assertEquals(ObservedTime(9, 5, previousDay = false), draft.observedAt)
        assertTrue(draft.canInsert)
    }

    @Test
    fun observationTimeLaterThanNowIsTheDayBefore() {
        val draft = draft("31U DQ 48251 11932", CoordinateFormat.MGRS, "fumée", time = "14:31")
        assertEquals("[CR] FAIT OBSERVÉ · 14:31 (veille) · Lieu MGRS 31U DQ 48251 11932 · fumée", draft.message)
        assertTrue(checkNotNull(draft.observedAt).previousDay)
        assertTrue(draft.canInsert)
    }

    @Test
    fun invalidObservationTimeGivesNothing() {
        listOf("", "14", "25:00", "14:60", "14:5", "midi").forEach { time ->
            val draft = draft("31U DQ 48251 11932", CoordinateFormat.MGRS, "fumée", time = time)
            assertNull(draft.observedAt, time)
            assertNull(draft.message, time)
            assertFalse(draft.canInsert, time)
        }
    }

    @Test
    fun clockIsReadInUsualForms() {
        listOf("14:05", "14h05", "14 05", "1405", " 14:05 ").forEach { assertEquals(14 to 5, parseClock(it), it) }
        assertEquals(9 to 5, parseClock("9:05"))
    }
}
