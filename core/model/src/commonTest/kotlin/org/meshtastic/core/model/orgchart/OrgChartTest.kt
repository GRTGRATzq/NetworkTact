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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OrgChartTest {

    @Test
    fun a_call_sign_is_the_long_name_without_its_team() {
        assertEquals("ALPHA-1", Callsign.ofLongName("ALPHA-1 [Alpha]"))
        assertEquals("ALPHA-1", Callsign.ofLongName("  ALPHA-1   [Alpha]"))
        assertEquals("BRAVO-2", Callsign.ofLongName("BRAVO-2"))
        assertEquals("CHARLIE 3", Callsign.ofLongName("CHARLIE   3 [Alpha] (MQTT)"))
    }

    @Test
    fun call_signs_compare_without_case_or_extra_spaces() {
        assertTrue(Callsign.sameAs("alpha-1", " ALPHA-1 "))
        assertTrue(Callsign.sameAs("PC  0", "pc 0"))
        assertFalse(Callsign.sameAs("ALPHA-1", "ALPHA-11"))
    }

    @Test
    fun a_valid_member_is_cleaned() {
        assertEquals(
            MemberInput.Valid(OrgMember("ALPHA-1", "chef d'équipe")),
            OrgChart.checkMember("  ALPHA-1 ", " chef   d'équipe ", listOf("BRAVO-2")),
        )
    }

    @Test
    fun a_member_needs_a_unique_call_sign_within_the_limits() {
        assertEquals(MemberInput.EmptyCallsign, OrgChart.checkMember("  ", "radio", emptyList()))
        assertEquals(MemberInput.DuplicateCallsign("alpha-1"), OrgChart.checkMember("alpha-1", "", listOf("ALPHA-1")))
        assertEquals(MemberInput.CallsignTooLong(40), OrgChart.checkMember("A".repeat(40), "", emptyList()))
        assertEquals(MemberInput.RoleTooLong(26), OrgChart.checkMember("A1", "é".repeat(13), emptyList()))
        assertTrue(OrgChart.checkMember("A".repeat(39), "r".repeat(24), emptyList()) is MemberInput.Valid)
    }

    @Test
    fun lists_every_member_with_its_team_in_rank_order() {
        val chart =
            OrgChart(
                author = "PC-0",
                createdSeconds = 0,
                commandPost = listOf(OrgMember("PC-0")),
                teams = listOf(OrgTeam("Alpha", listOf(OrgMember("ALPHA-1"), OrgMember("CHARLIE-3")))),
            )
        assertEquals(3, chart.memberCount)
        assertEquals(listOf(null, "Alpha", "Alpha"), chart.allMembers.map { it.first })
        assertEquals(listOf("ALPHA-1", "CHARLIE-3"), chart.membersOf("Alpha").map { it.callsign })
        assertEquals(emptyList(), chart.membersOf("Bravo"))
    }

    @Test
    fun the_same_structure_ignores_author_and_time() {
        val chart = OrgChart("PC-0", 10, listOf(OrgMember("PC-0")), emptyList())
        assertTrue(chart.sameStructureAs(chart.copy(author = "PC-1", createdSeconds = 20)))
        assertFalse(chart.sameStructureAs(chart.copy(commandPost = emptyList())))
        assertFalse(chart.sameStructureAs(null))
    }
}
