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
package org.meshtastic.core.model.orgchart

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CallsignSuggestionsTest {

    private val chart =
        OrgChart(
            author = "PC-0",
            createdSeconds = 0,
            commandPost = listOf(OrgMember("PC-0", "Chef PC")),
            teams = listOf(OrgTeam("Alpha", listOf(OrgMember("alpha-1")))),
        )

    @Test
    fun a_radio_is_offered_by_its_call_sign_without_the_team() {
        assertEquals(
            listOf("BRAVO-2", "CHARLIE-3"),
            Callsign.suggestions(listOf("BRAVO-2 [Bravo]", "CHARLIE-3 (MQTT)"), chart = null),
        )
    }

    @Test
    fun without_a_chart_every_radio_is_offered_in_the_order_given() {
        assertEquals(
            listOf("PC-0", "ALPHA-1", "BRAVO-2"),
            Callsign.suggestions(listOf("PC-0", "ALPHA-1 [Alpha]", "BRAVO-2 [Bravo]"), chart = null),
        )
    }

    @Test
    fun members_already_in_the_chart_are_left_out_whatever_their_case() {
        assertEquals(
            listOf("BRAVO-2"),
            Callsign.suggestions(listOf("PC-0", "ALPHA-1 [Alpha]", "BRAVO-2 [Bravo]"), chart),
        )
    }

    @Test
    fun a_call_sign_is_offered_once_and_blank_names_are_dropped() {
        assertEquals(
            listOf("BRAVO-2"),
            Callsign.suggestions(listOf("BRAVO-2 [Bravo]", "bravo-2", " ", ""), chart = null),
        )
    }

    @Test
    fun a_name_the_chart_would_refuse_for_its_size_is_not_offered() {
        val tooLong = "A".repeat(OrgChart.MAX_CALLSIGN_BYTES + 1)
        val justRight = "B".repeat(OrgChart.MAX_CALLSIGN_BYTES)
        assertEquals(listOf(justRight), Callsign.suggestions(listOf(tooLong, justRight), chart = null))
    }

    @Test
    fun nothing_is_offered_when_there_is_no_radio() {
        assertTrue(Callsign.suggestions(emptyList(), chart).isEmpty())
    }
}
