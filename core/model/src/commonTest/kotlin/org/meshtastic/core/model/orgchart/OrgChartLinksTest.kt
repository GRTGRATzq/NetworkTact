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

class OrgChartLinksTest {

    /** A radio as the node database knows it: a hardware number and a long name. */
    private data class Radio(val num: Int, val longName: String)

    private val chart =
        OrgChart(
            author = "PC-0",
            createdSeconds = 0,
            commandPost = listOf(OrgMember("PC-0", "Chef PC")),
            teams =
            listOf(
                OrgTeam("Alpha", listOf(OrgMember("ALPHA-1", "Chef d'équipe"), OrgMember("CHARLIE-3", "Radio"))),
                OrgTeam("Bravo", listOf(OrgMember("BRAVO-2"))),
            ),
        )

    private fun link(vararg radios: Radio) = OrgChartLinks.link(chart, radios.toList()) { it.longName }

    private fun ChartLinks<Radio>.of(callsign: String) = members.single { it.member.callsign == callsign }

    @Test
    fun ties_a_member_to_the_radio_carrying_its_call_sign_whatever_its_team_suffix() {
        val links = link(Radio(1, "ALPHA-1 [Alpha]"), Radio(2, "pc-0"))
        assertEquals(listOf(Radio(1, "ALPHA-1 [Alpha]")), links.of("ALPHA-1").radios.map { it.radio })
        assertEquals(listOf(2), links.of("PC-0").radios.map { it.radio.num })
    }

    @Test
    fun a_replaced_radio_with_the_same_call_sign_takes_the_same_place() {
        val before = link(Radio(0x11, "ALPHA-1 [Alpha]")).of("ALPHA-1")
        val after = link(Radio(0x99, "ALPHA-1 [Alpha]")).of("ALPHA-1")
        assertEquals(before.team, after.team)
        assertEquals(before.rank, after.rank)
        assertEquals(listOf(0x99), after.radios.map { it.radio.num })
    }

    @Test
    fun a_member_without_any_radio_has_none() {
        val links = link(Radio(1, "ALPHA-1 [Alpha]"))
        assertTrue(links.of("BRAVO-2").radios.isEmpty())
        assertTrue(links.of("CHARLIE-3").radios.isEmpty())
    }

    @Test
    fun several_radios_with_one_call_sign_are_all_kept_in_their_order() {
        val links = link(Radio(2, "BRAVO-2 [Bravo]"), Radio(1, "Bravo-2"))
        assertEquals(listOf(2, 1), links.of("BRAVO-2").radios.map { it.radio.num })
    }

    @Test
    fun ranks_follow_the_order_within_each_team() {
        val links = link()
        assertEquals(listOf(null, "Alpha", "Alpha", "Bravo"), links.members.map { it.team })
        assertEquals(listOf(1, 1, 2, 1), links.members.map { it.rank })
    }

    @Test
    fun tells_whether_the_radio_declares_the_members_team() {
        val links =
            link(
                Radio(1, "ALPHA-1 [alpha]"),
                Radio(3, "CHARLIE-3 [Bravo]"),
                Radio(4, "BRAVO-2"),
                Radio(5, "PC-0 [Alpha]"),
            )
        assertTrue(links.of("ALPHA-1").radios.single().teamMatches)
        val charlie = links.of("CHARLIE-3").radios.single()
        assertFalse(charlie.teamMatches)
        assertEquals("Bravo", charlie.declaredTeam)
        assertFalse(links.of("BRAVO-2").radios.single().teamMatches)
        assertFalse(links.of("PC-0").radios.single().teamMatches)
        assertTrue(link(Radio(6, "PC-0")).of("PC-0").radios.single().teamMatches)
    }

    @Test
    fun lists_the_radios_no_member_claims() {
        val links = link(Radio(1, "ALPHA-1 [Alpha]"), Radio(7, "DELTA-4 [Alpha]"), Radio(8, " "))
        assertEquals(listOf(7), links.unlisted.map { it.num })
    }
}
