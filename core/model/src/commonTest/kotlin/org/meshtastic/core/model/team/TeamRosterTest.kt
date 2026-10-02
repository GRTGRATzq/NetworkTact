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

import org.meshtastic.core.model.utf8Size
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TeamRosterTest {

    @Test
    fun parses_a_plain_list_in_order() {
        assertEquals(
            TeamRoster(listOf("Alpha", "Bravo", "Charlie")),
            TeamRoster.parseMessage("[EQUIPES] Alpha;Bravo;Charlie"),
        )
    }

    @Test
    fun trims_spaces_and_collapses_inner_whitespace() {
        assertEquals(
            TeamRoster(listOf("Alpha", "Bravo Nord")),
            TeamRoster.parseMessage("  [EQUIPES]   Alpha ;  Bravo   Nord  "),
        )
    }

    @Test
    fun prefix_is_matched_ignoring_case() {
        assertEquals(TeamRoster(listOf("Alpha")), TeamRoster.parseMessage("[equipes] Alpha"))
    }

    @Test
    fun drops_empty_entries() {
        assertEquals(TeamRoster(listOf("Alpha", "Bravo")), TeamRoster.parseMessage("[EQUIPES] ;Alpha;;  ;Bravo;"))
    }

    @Test
    fun keeps_the_first_spelling_of_a_duplicate() {
        assertEquals(TeamRoster(listOf("Alpha", "Bravo")), TeamRoster.parseMessage("[EQUIPES] Alpha;Bravo;ALPHA;alpha"))
    }

    @Test
    fun keeps_accents() {
        val roster = TeamRoster.parseMessage("[EQUIPES] Équipe Été;Ça va")
        assertEquals(TeamRoster(listOf("Équipe Été", "Ça va")), roster)
    }

    @Test
    fun ignores_text_that_is_not_a_team_list() {
        assertNull(TeamRoster.parseMessage("Alpha;Bravo"))
        assertNull(TeamRoster.parseMessage("[CR] [EQUIPES] Alpha"))
        assertNull(TeamRoster.parseMessage(""))
    }

    @Test
    fun rejects_a_list_without_any_name() {
        assertNull(TeamRoster.parseMessage("[EQUIPES]"))
        assertNull(TeamRoster.parseMessage("[EQUIPES] ; ;"))
    }

    @Test
    fun rejects_a_malformed_list_whole() {
        // A bracket would break the long-name suffix, so the list is not adopted at all, not even its valid names.
        assertNull(TeamRoster.parseMessage("[EQUIPES] Alpha;Bra]vo"))
        assertNull(TeamRoster.parseMessage("[EQUIPES] Alpha;[Bravo]"))
        assertNull(TeamRoster.parseMessage("[EQUIPES] Alpha;Un nom beaucoup trop long"))
    }

    @Test
    fun message_round_trips() {
        val roster = TeamRoster(listOf("Alpha", "Équipe Été"))
        assertEquals("[EQUIPES] Alpha;Équipe Été", roster.toMessage())
        assertEquals(roster, TeamRoster.parseMessage(roster.toMessage()))
    }

    @Test
    fun input_accepts_semicolons_and_new_lines() {
        assertEquals(
            RosterInput.Valid(TeamRoster(listOf("Alpha", "Bravo", "Charlie"))),
            TeamRoster.parseInput("Alpha\nBravo; Charlie\n"),
        )
    }

    @Test
    fun input_reports_each_problem() {
        assertEquals(RosterInput.Empty, TeamRoster.parseInput(" ;\n "))
        assertEquals(RosterInput.InvalidName("Al[pha"), TeamRoster.parseInput("Al[pha"))
        assertEquals(RosterInput.NameTooLong("Dix-sept octets!!"), TeamRoster.parseInput("Dix-sept octets!!"))
    }

    @Test
    fun team_name_limit_is_counted_in_utf8_bytes() {
        // 8 characters but 16 bytes: accepted. One more accented letter: 18 bytes, refused.
        assertTrue(TeamRoster.parseInput("éééééééé") is RosterInput.Valid)
        assertEquals(RosterInput.NameTooLong("ééééééééé"), TeamRoster.parseInput("ééééééééé"))
    }

    @Test
    fun input_refuses_a_message_over_the_size_limit() {
        val names = (1..20).map { "Équipe $it" }
        val result = TeamRoster.parseInput(names.joinToString(";"))
        val expectedBytes = TeamRoster(names).toMessage().utf8Size()
        assertTrue(expectedBytes > TeamRoster.MAX_MESSAGE_BYTES)
        assertEquals(RosterInput.MessageTooLong(expectedBytes), result)
    }

    @Test
    fun contains_ignores_case() {
        val roster = TeamRoster(listOf("Alpha"))
        assertTrue(roster.contains("ALPHA"))
        assertFalse(roster.contains("Bravo"))
    }
}
