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
package org.meshtastic.core.model.firerequest

import org.meshtastic.core.model.utf8Size
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FireRequestNoticeTest {

    @Test
    fun the_three_messages_are_written_as_the_specification_says() {
        assertEquals("[DT AB1234] Accusé DT 112251 loc NOV 26", FireRequestAck("AB1234", "112251 loc NOV 26").text())
        assertEquals(
            "[DT AB1234] GDH attribution 112252 loc NOV 26 · Tir attribué à CDS",
            FireRequestReply("AB1234", ReplyKind.ALLOCATION, "112252 loc NOV 26", designated = "CDS").text(),
        )
        assertEquals(
            "[DT AB1234] Tir attribué à vous par OA · GDH attribution 112252 loc NOV 26",
            FireRequestDesignation("AB1234", "OA", "112252 loc NOV 26").text(),
        )
    }

    @Test
    fun the_messages_stay_short() {
        // Longest number (10), longest short names (4) and the longest date-time group.
        val gdh = "112252 fra NOV 26"
        assertTrue(FireRequestAck("ABCDE12345", gdh).text().utf8Size() <= 50)
        val toWriter = FireRequestReply("ABCDE12345", ReplyKind.ALLOCATION, gdh, designated = "CDS1").text()
        assertTrue(toWriter.utf8Size() <= 80)
        assertTrue(FireRequestDesignation("ABCDE12345", "OA12", gdh).text().utf8Size() <= 90)
    }

    @Test
    fun each_message_reads_back() {
        val ack = FireRequestAck("AB1234", "112251 loc NOV 26")
        assertEquals(ack, FireRequestAck.parse(ack.text()))
        val reply = FireRequestReply("AB1234", ReplyKind.ALLOCATION, "112252 loc NOV 26", designated = "CDS")
        assertEquals(reply, FireRequestReply.parse(reply.text()))
        val designation = FireRequestDesignation("AB1234", "OA", "112252Z NOV 26")
        assertEquals(designation, FireRequestDesignation.parse(designation.text()))
    }

    @Test
    fun a_direct_reply_still_names_nobody_and_the_messages_are_not_mistaken() {
        val direct = FireRequestReply.parse("[DT AB1234] GDH attribution 112252 loc NOV 26")
        assertEquals(FireRequestReply("AB1234", ReplyKind.ALLOCATION, "112252 loc NOV 26"), direct)
        assertNull(FireRequestReply.parse(FireRequestAck("AB1234", "112251 loc NOV 26").text()))
        assertNull(FireRequestReply.parse(FireRequestDesignation("AB1234", "OA", "112252 loc NOV 26").text()))
        assertNull(FireRequestAck.parse("[DT AB1234 1/3]\n1 AB1234"))
        assertNull(FireRequestDesignation.parse("[DT AB1234] GDH attribution 112252 loc NOV 26 · Tir attribué à CDS"))
    }

    private fun message(id: Long, fromLocal: Boolean, sender: String, time: Long, text: String, to: String = "") =
        FireRequestMessage(id, fromLocal, sender, time, text, to)

    private val part = "[DT AB1234 1/1]\nOrigine OA · Destinataire Équipe TIR · GDH demande 112250 loc NOV 26\n1 AB1234"

    @Test
    fun the_writer_sees_one_acknowledgement_per_post_and_the_allocation() {
        val assembly =
            FireRequestAssembly.of(
                listOf(
                    message(1, true, "!oa", 1_000, part),
                    message(2, false, "!cds", 2_000, FireRequestAck("AB1234", "112251 loc NOV 26").text()),
                    message(3, false, "!pc", 3_000, FireRequestAck("AB1234", "112251 loc NOV 26").text()),
                    message(4, false, "!cds", 4_000, FireRequestAck("AB1234", "112252 loc NOV 26").text()),
                    message(
                        5,
                        false,
                        "!pc",
                        5_000,
                        FireRequestReply("AB1234", ReplyKind.ALLOCATION, "112253 loc NOV 26", "CDS").text(),
                    ),
                ),
            )
        val thread = assembly.threads.single()
        assertEquals(listOf("!pc", "!cds"), thread.acknowledgements.map { it.first.sender })
        assertEquals("112252 loc NOV 26", thread.acknowledgements.last().second.gdh)
        assertEquals("CDS", thread.allocation?.designated)
    }

    @Test
    fun the_designated_radio_and_the_chief_find_the_allocation_on_the_request_received() {
        val toDesignated = FireRequestDesignation("AB1234", "OA", "112252 loc NOV 26").text()
        val atDesignated =
            FireRequestAssembly.of(
                listOf(message(1, false, "!oa", 1_000, part), message(2, false, "!pc", 2_000, toDesignated)),
            )
                .threads
                .single()
        assertEquals("!pc", atDesignated.designation?.first?.sender)
        val atChief =
            FireRequestAssembly.of(
                listOf(
                    message(1, false, "!oa", 1_000, part),
                    message(2, true, "!pc", 2_000, toDesignated, to = "!cds"),
                    message(3, true, "!pc", 2_100, FireRequestAck("AB1234", "112251 loc NOV 26").text()),
                ),
            )
                .threads
                .single()
        assertEquals("!cds", atChief.ownAllocation?.to)
        assertEquals("112251 loc NOV 26", atChief.ownAcknowledgement?.gdh)
        // A chief who wrote the request keeps their own allocation on it.
        val chiefWriter =
            FireRequestAssembly.of(
                listOf(message(1, true, "!pc", 1_000, part), message(2, true, "!pc", 2_000, toDesignated, "!cds")),
            )
                .threads
                .single()
        assertEquals("!cds", chiefWriter.ownAllocation?.to)
    }
}
