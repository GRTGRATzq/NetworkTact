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

    private val known = setOf("!writer", "!chief", "!cds")

    @Test
    fun `an allocation to known ids is ready with the same messages`() {
        val check = checkAllocation(thread(fromLocal = false), cds, "OA", gdh, "!chief", known)

        assertEquals(
            AllocationCheck.Ready(allocationMessages(thread(fromLocal = false), cds, "OA", gdh, "!chief")),
            check,
        )
    }

    @Test
    fun `an allocation to a radio no longer in the network is refused`() {
        val gone = AllocationRadio("!gone", "CDS", "CDS TIR")

        assertEquals(
            AllocationCheck.Refused("!gone"),
            checkAllocation(thread(fromLocal = false), gone, "OA", gdh, "!chief", known),
        )
    }

    @Test
    fun `an allocation whose writer is unknown is refused, nothing goes to the designated radio`() {
        assertEquals(
            AllocationCheck.Refused("!writer"),
            checkAllocation(thread(fromLocal = false), cds, "OA", gdh, "!chief", setOf("!chief", "!cds")),
        )
    }

    private fun own(text: String, to: String) = FireRequestMessage(3, true, "!chief", 2, text, to)

    @Test
    fun `the allocation made names the designated radio by its id, never by its short name`() {
        val twins = listOf(AllocationRadio("!cds1", "CDS", "CDS 1"), AllocationRadio("!cds2", "CDS", "CDS 2"))
        val message = own("[DT AB1234] Tir attribué à vous par OA · GDH attribution $gdh", to = "!cds2")

        val made = madeAllocation(message, twins)

        assertEquals(MadeAllocation("CDS · CDS 2", gdh, twins[1]), made)
    }

    @Test
    fun `an allocation made to an unknown id shows the id and offers no resend`() {
        val message = own("[DT AB1234] Tir attribué à vous par OA · GDH attribution $gdh", to = "!gone")

        assertEquals(MadeAllocation("!gone", gdh, null), madeAllocation(message, listOf(cds)))
    }

    @Test
    fun `an allocation told to the writer only is shown as sent, without looking up a radio by name`() {
        val message = own("[DT AB1234] GDH attribution $gdh · Tir attribué à CDS", to = "!writer")

        assertEquals(MadeAllocation("CDS", gdh, null), madeAllocation(message, listOf(cds)))
    }

    private fun designated(
        requester: String,
        from: String = "!chief",
        sheetFrom: String = "!writer",
    ): FireRequestThread {
        val text = "[DT AB1234] Tir attribué à vous par $requester · GDH attribution $gdh"
        return thread(fromLocal = false)
            .copy(sender = sheetFrom, replies = listOf(FireRequestMessage(2, false, from, 1, text)))
    }

    private val writer = AllocationRadio("!writer", "OA", "OA 1")
    private val chief = AllocationRadio("!chief", "CT", "CHEF TIR")

    @Test
    fun `GDH fin goes to the sheet's sender by its id, even when another radio has the writer's short name`() {
        val radios = listOf(AllocationRadio("!other", "OA", "OA 2"), writer, chief)

        assertEquals(EndRecipient.Ready("!writer"), endRecipient(designated("OA"), radios))
    }

    @Test
    fun `GDH fin goes to the chief who wrote the sheet himself`() {
        val sheet = designated("CT", from = "!chief", sheetFrom = "!chief")

        assertEquals(EndRecipient.Ready("!chief"), endRecipient(sheet, listOf(writer, chief)))
    }

    @Test
    fun `GDH fin is refused when the sheet's sender is not a radio of the network`() {
        assertEquals(EndRecipient.UnknownSender("!writer"), endRecipient(designated("OA"), listOf(chief)))
    }

    @Test
    fun `GDH fin is refused for a sheet resent by the chief, never sent to a radio found by name`() {
        val resent = designated("OA", from = "!chief", sheetFrom = "!chief")

        assertEquals(EndRecipient.SheetFromChief("OA"), endRecipient(resent, listOf(writer, chief)))
    }
}
