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
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OrgChartCodeTest {

    private val chart =
        OrgChart(
            author = "PC-0",
            createdSeconds = 1_791_720_000L,
            commandPost = listOf(OrgMember("PC-0", "Chef PC")),
            teams =
            listOf(
                OrgTeam("Alpha", listOf(OrgMember("ALPHA-1", "Chef d'équipe"), OrgMember("CHARLIE-3", "Radio"))),
                OrgTeam("Bravo", listOf(OrgMember("BRAVO-2"))),
            ),
        )

    @Test
    fun encodes_one_readable_line_per_entry() {
        assertEquals(
            listOf(
                "NTORGA1",
                "A|PC-0|1791720000",
                "P|PC-0|Chef PC",
                "T|Alpha",
                "M|ALPHA-1|Chef d'%C3%A9quipe",
                "M|CHARLIE-3|Radio",
                "T|Bravo",
                "M|BRAVO-2|",
            ),
            OrgChartCode.encode(chart).split('\n'),
        )
    }

    @Test
    fun round_trips_a_chart() {
        assertEquals(chart, OrgChartCode.decode(OrgChartCode.encode(chart)))
    }

    @Test
    fun the_code_is_plain_ascii() {
        val code =
            OrgChartCode.encode(chart.copy(author = "PC-0 ⚑", commandPost = listOf(OrgMember("Échelon|1", "50%"))))
        assertTrue(code.all { it == '\n' || it.code in 0x20..0x7E })
        assertEquals("PC-0 ⚑", OrgChartCode.decode(code)?.author)
        assertEquals(OrgMember("Échelon|1", "50%"), OrgChartCode.decode(code)?.commandPost?.single())
    }

    @Test
    fun round_trips_an_empty_chart_and_one_without_author() {
        val empty = OrgChart.EMPTY.copy(createdSeconds = 5)
        assertEquals(empty, OrgChartCode.decode(OrgChartCode.encode(empty)))
        val teamOnly = chart.copy(author = "", commandPost = emptyList())
        assertEquals(teamOnly, OrgChartCode.decode(OrgChartCode.encode(teamOnly)))
    }

    @Test
    fun keeps_an_empty_team() {
        val withEmptyTeam = chart.copy(teams = chart.teams + OrgTeam("Charlie", emptyList()))
        assertEquals(withEmptyTeam, OrgChartCode.decode(OrgChartCode.encode(withEmptyTeam)))
    }

    @Test
    fun rejects_any_other_text() {
        assertNull(OrgChartCode.decode(""))
        assertNull(OrgChartCode.decode("https://meshtastic.org/e/#CgMSAQESBggBQANIAQ"))
        assertNull(OrgChartCode.decode("NTORGA1"))
        assertNull(OrgChartCode.decode("NTORGA2\nA|PC-0|1"))
        assertFalse(OrgChartCode.isOrgChartCode("[EQUIPES] Alpha;Bravo"))
        assertTrue(OrgChartCode.isOrgChartCode(OrgChartCode.encode(chart)))
    }

    @Test
    fun rejects_the_whole_chart_when_one_line_is_damaged() {
        val good = OrgChartCode.encode(chart)
        listOf(
            good.replace("A|PC-0|1791720000", "A|PC-0|17x1"),
            good.replace("A|PC-0|1791720000", "A|PC-0"),
            good.replace("M|BRAVO-2|", "M|BRAVO-2"),
            good.replace("M|BRAVO-2|", "M||Radio"),
            good.replace("M|BRAVO-2|", "X|BRAVO-2|"),
            good.replace("%C3%A9", "%C3"),
            good.replace("%C3%A9", "%G1"),
            good.replace("%C3%A9", "%+1"),
            good.replace("T|Bravo", "T|Br[a]vo"),
            good.replace("T|Bravo", "T|"),
            good + "\n",
            good + "\nP|LATE-1|",
        )
            .forEach { assertNull(OrgChartCode.decode(it), it) }
    }

    @Test
    fun rejects_a_member_before_any_team() {
        assertNull(OrgChartCode.decode("NTORGA1\nA|PC-0|1\nM|ALPHA-1|"))
    }

    @Test
    fun rejects_one_call_sign_twice_or_one_team_twice() {
        assertNull(OrgChartCode.decode("NTORGA1\nA|PC-0|1\nP|PC-0|\nT|Alpha\nM|pc-0|"))
        assertNull(OrgChartCode.decode("NTORGA1\nA|PC-0|1\nT|Alpha\nT|ALPHA"))
    }

    @Test
    fun rejects_fields_over_their_limits() {
        val longSign = "A".repeat(OrgChart.MAX_CALLSIGN_BYTES + 1)
        val longRole = "r".repeat(OrgChart.MAX_ROLE_BYTES + 1)
        assertNull(OrgChartCode.decode("NTORGA1\nA|PC-0|1\nP|$longSign|"))
        assertNull(OrgChartCode.decode("NTORGA1\nA|PC-0|1\nP|PC-0|$longRole"))
        assertNull(OrgChartCode.decode("NTORGA1\nA|$longSign|1"))
    }

    /** Nine members, call signs and roles at their longest, in three teams with the longest names. */
    private fun largestNineMemberChart(): OrgChart {
        fun member(index: Int) = OrgMember(
            callsign = "M$index".padEnd(OrgChart.MAX_CALLSIGN_BYTES, 'X'),
            role = "R$index".padEnd(OrgChart.MAX_ROLE_BYTES, 'y'),
        )
        return OrgChart(
            author = "A".repeat(OrgChart.MAX_CALLSIGN_BYTES),
            createdSeconds = 9_999_999_999L,
            commandPost = listOf(member(0)),
            teams =
            (0 until 3).map { team ->
                OrgTeam("Team$team".padEnd(16, 'z'), (1..if (team == 2) 2 else 3).map { member(team * 3 + it) })
            },
        )
    }

    @Test
    fun nine_members_at_the_longest_fit_in_a_readable_code() {
        val largest = largestNineMemberChart()
        assertEquals(9, largest.memberCount)
        val size = OrgChartCode.size(largest)
        assertTrue(size.fits, "${size.length} > ${OrgChartCode.MAX_CODE_CHARS}")
        assertEquals(largest, OrgChartCode.decode(OrgChartCode.encode(largest)))
    }

    @Test
    fun a_chart_too_large_for_a_readable_code_is_flagged_and_not_decoded() {
        val largest = largestNineMemberChart()
        val tooLarge = largest.copy(teams = largest.teams + OrgTeam("Extra", listOf(OrgMember("X".repeat(39), "y"))))
        val size = OrgChartCode.size(tooLarge)
        assertFalse(size.fits)
        assertEquals(OrgChartCode.encode(tooLarge).length, size.length)
        assertNull(OrgChartCode.decode(OrgChartCode.encode(tooLarge)))
    }
}
