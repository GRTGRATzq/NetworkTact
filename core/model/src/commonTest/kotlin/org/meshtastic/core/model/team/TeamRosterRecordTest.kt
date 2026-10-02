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
package org.meshtastic.core.model.team

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TeamRosterRecordTest {

    private val adopted = TeamRosterRecord(listOf("Alpha", "Bravo"), TeamRosterRecord.Source.MANUAL, timeSeconds = 100)

    private fun received(vararg teams: String) = TeamRosterRecord(
        teams.toList(),
        TeamRosterRecord.Source.RADIO,
        senderNum = 0x1234abcd,
        senderName = "PC-0",
        timeSeconds = 200,
    )

    @Test
    fun a_different_list_awaits_confirmation() {
        val record = received("Alpha", "Bravo", "Charlie")
        assertEquals(record, TeamRosterRecord.pendingAfter(adopted, record))
    }

    @Test
    fun the_first_list_ever_received_awaits_confirmation() {
        val record = received("Alpha")
        assertEquals(record, TeamRosterRecord.pendingAfter(null, record))
    }

    @Test
    fun the_same_teams_ask_nothing_whoever_sent_them() {
        assertNull(TeamRosterRecord.pendingAfter(adopted, received("Alpha", "Bravo")))
    }

    @Test
    fun a_new_order_or_spelling_is_a_different_list() {
        assertEquals(received("Bravo", "Alpha"), TeamRosterRecord.pendingAfter(adopted, received("Bravo", "Alpha")))
        assertEquals(received("ALPHA", "Bravo"), TeamRosterRecord.pendingAfter(adopted, received("ALPHA", "Bravo")))
    }

    @Test
    fun stored_form_round_trips() {
        val record = received("Équipe Été", "Bravo")
        val json = Json.encodeToString(TeamRosterRecord.serializer(), record)
        assertEquals(record, Json.decodeFromString(TeamRosterRecord.serializer(), json))
    }
}
