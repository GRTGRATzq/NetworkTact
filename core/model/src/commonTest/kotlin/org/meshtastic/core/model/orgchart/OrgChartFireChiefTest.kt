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
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OrgChartFireChiefTest {

    private val chart =
        OrgChart(
            author = "PC-0",
            createdSeconds = 1_791_720_000L,
            commandPost = listOf(OrgMember("PC-0", "Chef PC")),
            teams = listOf(OrgTeam("Alpha", listOf(OrgMember("ALPHA-1"), OrgMember("CDS-TIR", "Chef de section")))),
        )

    private val withChief = chart.copy(fireChief = "CDS-TIR")

    private fun changed(edit: OrgChartEdit, base: OrgChart = chart) =
        assertIs<EditResult.Changed>(OrgChartEditor.apply(base, edit, "PC-0", 2L)).chart

    @Test
    fun without_a_chief_the_code_is_the_first_version_byte_for_byte() {
        val code = OrgChartCode.encode(chart)
        assertEquals("NTORGA1", code.lines().first())
        assertFalse(code.lines().any { it.startsWith("F|") })
    }

    @Test
    fun a_chief_takes_the_new_header_and_one_line_after_the_author() {
        val lines = OrgChartCode.encode(withChief).lines()
        assertEquals(listOf("NTORGA3", "A|PC-0|1791720000", "F|CDS-TIR"), lines.take(3))
        assertEquals(OrgChartCode.encode(chart).lines().drop(2), lines.drop(3))
        assertEquals(withChief, OrgChartCode.decode(lines.joinToString("\n")))
        assertTrue(OrgChartCode.isOrgChartCode(lines.joinToString("\n")))
    }

    @Test
    fun the_chief_line_ignores_case_and_keeps_the_member_spelling() {
        val code = OrgChartCode.encode(withChief).replace("F|CDS-TIR", "F|cds-tir")
        assertEquals("CDS-TIR", OrgChartCode.decode(code)?.fireChief)
    }

    @Test
    fun refuses_a_chief_who_is_not_a_member_or_a_misplaced_chief_line() {
        val code = OrgChartCode.encode(withChief)
        assertNull(OrgChartCode.decode(code.replace("F|CDS-TIR", "F|BRAVO-2")))
        assertNull(OrgChartCode.decode(code.replace("F|CDS-TIR", "F|")))
        // The first version knows no F line, and the new one requires it in third place.
        assertNull(OrgChartCode.decode(code.replace("NTORGA3", "NTORGA1")))
        assertNull(OrgChartCode.decode(OrgChartCode.encode(chart).replace("NTORGA1", "NTORGA3")))
        assertNull(OrgChartCode.decode(code.replace("NTORGA3", "NTORGA2")))
    }

    @Test
    fun designating_replaces_the_previous_chief_and_can_be_taken_back() {
        val first = changed(OrgChartEdit.SetFireChief(null, 0, chief = true))
        assertEquals("PC-0", first.fireChief)
        val second = changed(OrgChartEdit.SetFireChief("Alpha", 1, chief = true), first)
        assertEquals("CDS-TIR", second.fireChief)
        assertTrue(second.isFireChief("cds-tir"))
        assertEquals("", changed(OrgChartEdit.SetFireChief("Alpha", 1, chief = false), second).fireChief)
        // Taking it back from someone who is not the chief changes nothing.
        assertIs<EditResult.Unchanged>(
            OrgChartEditor.apply(second, OrgChartEdit.SetFireChief("Alpha", 0, chief = false), "PC-0", 2L),
        )
    }

    @Test
    fun the_designation_follows_a_renamed_chief_and_leaves_with_a_removed_one() {
        assertEquals("CDS-1", changed(OrgChartEdit.Update("Alpha", 1, "CDS-1", ""), withChief).fireChief)
        assertEquals("CDS-TIR", changed(OrgChartEdit.Update("Alpha", 0, "ALPHA-9", ""), withChief).fireChief)
        assertEquals("", changed(OrgChartEdit.Remove("Alpha", 1), withChief).fireChief)
        assertEquals("CDS-TIR", changed(OrgChartEdit.Remove("Alpha", 0), withChief).fireChief)
    }

    @Test
    fun a_chart_stored_before_the_chief_reads_without_one() {
        val json = kotlinx.serialization.json.Json.encodeToString(OrgChart.serializer(), chart)
        assertFalse(json.contains("fireChief"))
        assertEquals("", kotlinx.serialization.json.Json.decodeFromString(OrgChart.serializer(), json).fireChief)
    }

    @Test
    fun a_chief_change_is_a_new_structure() {
        assertFalse(withChief.sameStructureAs(chart))
    }
}
