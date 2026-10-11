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
import kotlin.test.assertIs
import kotlin.test.assertTrue

class OrgChartEditorTest {

    private val now = 1_791_720_000L

    private val chart =
        OrgChart(
            author = "PC-0",
            createdSeconds = 1L,
            commandPost = listOf(OrgMember("PC-0", "Chef PC")),
            teams = listOf(OrgTeam("Alpha", listOf(OrgMember("ALPHA-1"), OrgMember("CHARLIE-3")))),
        )

    private fun apply(edit: OrgChartEdit, base: OrgChart? = chart) = OrgChartEditor.apply(base, edit, "PC-0", now)

    private fun changed(edit: OrgChartEdit, base: OrgChart? = chart) =
        assertIs<EditResult.Changed>(apply(edit, base)).chart

    @Test
    fun adding_to_a_new_team_creates_it_and_stamps_author_and_time() {
        val result = changed(OrgChartEdit.Add("Bravo", " BRAVO-2 ", "radio"))
        assertEquals(OrgTeam("Bravo", listOf(OrgMember("BRAVO-2", "radio"))), result.teams.last())
        assertEquals("PC-0", result.author)
        assertEquals(now, result.createdSeconds)
    }

    @Test
    fun the_first_member_starts_a_chart() {
        val result = changed(OrgChartEdit.Add(null, "PC-0", "Chef PC"), base = null)
        assertEquals(listOf(OrgMember("PC-0", "Chef PC")), result.commandPost)
        assertTrue(result.teams.isEmpty())
    }

    @Test
    fun refuses_a_call_sign_already_in_the_chart_even_in_another_team() {
        assertEquals(
            EditResult.Invalid(MemberInput.DuplicateCallsign("alpha-1")),
            apply(OrgChartEdit.Add(null, "alpha-1", "")),
        )
    }

    @Test
    fun an_update_may_keep_its_own_call_sign() {
        val result = changed(OrgChartEdit.Update("Alpha", 0, "ALPHA-1", "Chef d'équipe"))
        assertEquals(OrgMember("ALPHA-1", "Chef d'équipe"), result.membersOf("Alpha").first())
        assertEquals(
            EditResult.Invalid(MemberInput.DuplicateCallsign("CHARLIE-3")),
            apply(OrgChartEdit.Update("Alpha", 0, "CHARLIE-3", "")),
        )
    }

    @Test
    fun moves_a_member_one_rank_and_not_past_the_ends() {
        assertEquals(
            listOf("CHARLIE-3", "ALPHA-1"),
            changed(OrgChartEdit.Move("Alpha", 1, up = true)).membersOf("Alpha").map { it.callsign },
        )
        assertEquals(EditResult.Unchanged, apply(OrgChartEdit.Move("Alpha", 0, up = true)))
        assertEquals(EditResult.Unchanged, apply(OrgChartEdit.Move("Alpha", 1, up = false)))
    }

    @Test
    fun removing_the_last_member_drops_the_team() {
        val once = changed(OrgChartEdit.Remove("Alpha", 0))
        assertEquals(listOf("CHARLIE-3"), once.membersOf("Alpha").map { it.callsign })
        assertTrue(changed(OrgChartEdit.Remove("Alpha", 0), base = once).teams.isEmpty())
        assertEquals(EditResult.Unchanged, apply(OrgChartEdit.Remove("Bravo", 0)))
        assertEquals(EditResult.Unchanged, apply(OrgChartEdit.Update("Alpha", 5, "X", "")))
    }

    @Test
    fun refuses_the_member_that_would_make_the_code_unreadable() {
        fun member(index: Int) = OrgChartEdit.Add(
            team = "Team${index % 3}".padEnd(16, 'z'),
            callsign = "M$index".padEnd(OrgChart.MAX_CALLSIGN_BYTES, 'X'),
            role = "R$index".padEnd(OrgChart.MAX_ROLE_BYTES, 'y'),
        )
        var current: OrgChart? = null
        var result: EditResult = EditResult.Unchanged
        var index = 0
        while (result !is EditResult.TooLarge) {
            result = apply(member(index++), base = current)
            if (result is EditResult.Changed) current = result.chart
        }
        val accepted = current?.memberCount ?: 0
        assertTrue(accepted >= 9, "only $accepted members fit")
        assertTrue((result as EditResult.TooLarge).length > OrgChartCode.MAX_CODE_CHARS)
        assertTrue(OrgChartCode.size(current ?: OrgChart.EMPTY).fits)
    }

    @Test
    fun refuses_a_member_past_the_member_limit() {
        var current: OrgChart? = null
        repeat(OrgChart.MAX_MEMBERS) { current = changed(OrgChartEdit.Add(null, "M$it", ""), base = current) }
        assertIs<EditResult.TooLarge>(apply(OrgChartEdit.Add(null, "LAST", ""), base = current))
    }
}
