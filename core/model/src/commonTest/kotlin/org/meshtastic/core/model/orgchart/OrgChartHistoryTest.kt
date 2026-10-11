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
import kotlin.test.assertNull

class OrgChartHistoryTest {

    private fun record(author: String, source: OrgChartRecord.Source = OrgChartRecord.Source.EDITED) =
        OrgChartRecord(OrgChart(author, 1, listOf(OrgMember(author)), emptyList()), source, timeSeconds = 2)

    private val mine = record("PC-0")
    private val scanned = record("PC-1", OrgChartRecord.Source.SCANNED)

    @Test
    fun adopting_keeps_the_chart_it_replaces() {
        val history = OrgChartHistory(current = mine).adopted(scanned)
        assertEquals(scanned, history.current)
        assertEquals(mine, history.previous)
    }

    @Test
    fun going_back_swaps_the_two_so_it_can_be_undone() {
        val history = OrgChartHistory(current = mine).adopted(scanned).reverted()
        assertEquals(mine, history.current)
        assertEquals(scanned, history.previous)
        assertEquals(scanned, history.reverted().current)
    }

    @Test
    fun a_change_on_this_phone_keeps_the_previous_chart() {
        val edited = record("PC-1 bis")
        val history = OrgChartHistory(current = mine).adopted(scanned).saved(edited)
        assertEquals(edited, history.current)
        assertEquals(mine, history.previous)
    }

    @Test
    fun nothing_to_go_back_to_at_first() {
        assertEquals(OrgChartHistory(current = mine), OrgChartHistory(current = mine).reverted())
        val first = OrgChartHistory().adopted(scanned)
        assertNull(first.previous)
    }
}
