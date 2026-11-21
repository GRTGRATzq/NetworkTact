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
package org.meshtastic.feature.messaging.firerequest

import org.meshtastic.core.model.firerequest.FireRequestMessage
import org.meshtastic.core.model.firerequest.FireRequestThread
import kotlin.test.Test
import kotlin.test.assertEquals

class FireRequestAllocationTest {

    private fun thread(fromLocal: Boolean) = FireRequestThread(
        number = "AB1234",
        count = 1,
        fromLocal = fromLocal,
        sender = if (fromLocal) "!chief" else "!writer",
        parts = mapOf(1 to listOf(FireRequestMessage(1, fromLocal, "!writer", 0, "[DT AB1234 1/1] …"))),
    )

    private val cds = AllocationRadio("!cds", "CDS", "CDS TIR")
    private val gdh = "112252 loc NOV 26"

    @Test
    fun `the chief tells the writer and the designated radio, in direct messages`() {
        val messages = allocationMessages(thread(fromLocal = false), cds, "OA", gdh, ourId = "!chief")

        assertEquals(
            listOf(
                "[DT AB1234] GDH attribution 112252 loc NOV 26 · Tir attribué à CDS" to "!writer",
                "[DT AB1234] Tir attribué à vous par OA · GDH attribution 112252 loc NOV 26" to "!cds",
            ),
            messages,
        )
    }

    @Test
    fun `a chief who wrote the request sends one message, to the designated radio`() {
        val messages = allocationMessages(thread(fromLocal = true), cds, "OA", gdh, ourId = "!chief")

        assertEquals(listOf("!cds"), messages.map { it.second })
    }

    @Test
    fun `allocated to the writer's or the chief's own radio, nobody else is told`() {
        val toWriter = allocationMessages(thread(false), AllocationRadio("!writer", "OA", ""), "OA", gdh, "!chief")
        val toChief = allocationMessages(thread(false), AllocationRadio("!chief", "CT", ""), "OA", gdh, "!chief")

        assertEquals(listOf("!writer"), toWriter.map { it.second })
        assertEquals(listOf("!writer"), toChief.map { it.second })
    }

    private fun designated(requester: String) = thread(fromLocal = false)
        .copy(
            replies =
            listOf(
                FireRequestMessage(
                    2,
                    false,
                    "!chief",
                    1,
                    "[DT AB1234] Tir attribué à vous par $requester · GDH attribution $gdh",
                ),
            ),
        )

    @Test
    fun `GDH fin goes to the radio named by the allocation when only one has that short name`() {
        val radios = listOf(AllocationRadio("!oa", "OA", "OA 1"), cds)

        assertEquals("!oa", endRecipient(designated("OA"), radios))
    }

    @Test
    fun `GDH fin goes to the sheet's sender when the name is shared or unknown`() {
        val twins = listOf(AllocationRadio("!oa1", "OA", ""), AllocationRadio("!oa2", "OA", ""))

        assertEquals("!writer", endRecipient(designated("OA"), twins))
        assertEquals("!writer", endRecipient(designated("XY"), twins))
    }
}
