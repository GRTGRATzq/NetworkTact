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

class TeamSuffixTest {

    @Test
    fun reads_the_team_from_the_suffix() {
        assertEquals("Alpha", TeamSuffix.teamOf("ALPHA-1 [Alpha]"))
        assertEquals("Équipe Été", TeamSuffix.teamOf("ALPHA-1 [Équipe Été]"))
    }

    @Test
    fun a_name_without_suffix_has_no_team() {
        assertNull(TeamSuffix.teamOf("ALPHA-1"))
        assertNull(TeamSuffix.teamOf(""))
        assertNull(TeamSuffix.teamOf("ALPHA-1 []"))
    }

    @Test
    fun a_bracket_inside_the_name_is_not_a_suffix() {
        assertNull(TeamSuffix.teamOf("ALPHA [x] 1"))
        assertNull(TeamSuffix.teamOf("ALPHA-1[Alpha]"))
    }

    @Test
    fun the_mqtt_mark_is_looked_through() {
        assertEquals("Alpha", TeamSuffix.teamOf("ALPHA-1 [Alpha] (MQTT)"))
        assertEquals("ALPHA-1 (MQTT)", TeamSuffix.displayName("ALPHA-1 [Alpha] (MQTT)"))
    }

    @Test
    fun display_name_drops_the_suffix() {
        assertEquals("ALPHA-1", TeamSuffix.displayName("ALPHA-1 [Alpha]"))
        assertEquals("ALPHA-1", TeamSuffix.displayName("ALPHA-1"))
        assertEquals("ALPHA-1", TeamSuffix.displayName("ALPHA-1 [Alpha] "))
    }

    @Test
    fun display_name_is_never_blank() {
        assertEquals("[Alpha]", TeamSuffix.displayName("[Alpha]"))
    }

    @Test
    fun adds_a_suffix() {
        assertEquals(TeamNameChange("ALPHA-1 [Alpha]", truncated = false), TeamSuffix.withTeam("ALPHA-1", "Alpha"))
    }

    @Test
    fun replaces_an_existing_suffix() {
        assertEquals(
            TeamNameChange("ALPHA-1 [Bravo]", truncated = false),
            TeamSuffix.withTeam("ALPHA-1 [Alpha]", "Bravo"),
        )
    }

    @Test
    fun removes_the_suffix_when_no_team_is_given() {
        assertEquals(TeamNameChange("ALPHA-1", truncated = false), TeamSuffix.withTeam("ALPHA-1 [Alpha]", null))
    }

    @Test
    fun an_exact_fit_is_not_truncated() {
        // 31 + " [Alpha]" (8) = 39 bytes.
        val name = "A".repeat(31)
        val change = TeamSuffix.withTeam(name, "Alpha")
        assertEquals(TeamSuffix.LONG_NAME_MAX_BYTES, change.longName.utf8Size())
        assertFalse(change.truncated)
    }

    @Test
    fun truncates_the_name_never_the_team() {
        val change = TeamSuffix.withTeam("A".repeat(35), "Alpha")
        assertEquals("A".repeat(31) + " [Alpha]", change.longName)
        assertTrue(change.truncated)
    }

    @Test
    fun truncation_is_counted_in_utf8_bytes() {
        // 20 characters but 40 bytes: only 15 accented letters fit in front of " [Alpha]" (8 bytes).
        val change = TeamSuffix.withTeam("é".repeat(20), "Alpha")
        assertEquals("é".repeat(15) + " [Alpha]", change.longName)
        assertTrue(change.longName.utf8Size() <= TeamSuffix.LONG_NAME_MAX_BYTES)
        assertTrue(change.truncated)
    }

    @Test
    fun truncation_never_splits_a_surrogate_pair() {
        // Each 📡 is 4 bytes; 31 bytes leave room for 7 of them, never for half of the 8th.
        val change = TeamSuffix.withTeam("📡".repeat(10), "Alpha")
        assertEquals("📡".repeat(7) + " [Alpha]", change.longName)
    }

    @Test
    fun suffix_round_trips() {
        val change = TeamSuffix.withTeam("BRAVO-2", "Équipe Été")
        assertEquals("Équipe Été", TeamSuffix.teamOf(change.longName))
        assertEquals("BRAVO-2", TeamSuffix.displayName(change.longName))
    }
}
