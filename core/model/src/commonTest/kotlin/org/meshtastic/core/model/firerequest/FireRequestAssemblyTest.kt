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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FireRequestAssemblyTest {

    private val parts = FireRequestExamples.specificationParts
    private val minute = 60_000L

    private var nextId = 1L

    private fun received(text: String, atMinute: Long, sender: String = "!oa") =
        FireRequestMessage(nextId++, fromLocal = false, sender = sender, timeMillis = atMinute * minute, text = text)

    private fun sent(text: String, atMinute: Long) =
        FireRequestMessage(nextId++, fromLocal = true, sender = "!me", timeMillis = atMinute * minute, text = text)

    @Test
    fun three_parts_in_order_make_one_complete_request() {
        val messages = parts.mapIndexed { i, text -> received(text, i.toLong()) }
        val assembly = FireRequestAssembly.of(messages)
        val thread = assembly.threads.single()
        assertTrue(thread.isComplete)
        assertEquals(FireRequestExamples.specification, thread.read().request)
        assertEquals(messages.last().id, thread.anchor.id)
        assertEquals(setOf(messages[0].id, messages[1].id), assembly.hiddenIds)
        assertEquals(thread, assembly.threadAt(messages.last().id))
        assertNull(assembly.threadAt(messages.first().id))
    }

    @Test
    fun order_of_arrival_does_not_matter() {
        val orders =
            listOf(listOf(0, 1, 2), listOf(0, 2, 1), listOf(1, 0, 2), listOf(1, 2, 0), listOf(2, 0, 1), listOf(2, 1, 0))
        for (order in orders) {
            val messages = order.mapIndexed { minute, i -> received(parts[i], minute.toLong()) }
            val thread = FireRequestAssembly.of(messages).threads.single()
            assertEquals(FireRequestExamples.specification, thread.read().request, "$order")
            assertEquals(messages.last().id, thread.anchor.id)
        }
    }

    @Test
    fun a_part_received_twice_counts_once() {
        val messages =
            listOf(received(parts[0], 0), received(parts[1], 1), received(parts[1], 3), received(parts[2], 4))
        val assembly = FireRequestAssembly.of(messages)
        val thread = assembly.threads.single()
        assertEquals(listOf(1, 2, 3), thread.received)
        assertEquals(2, thread.parts.getValue(2).size)
        assertEquals(FireRequestExamples.specification, thread.read().request)
        assertEquals(messages.map { it.id }.toSet() - messages.last().id, assembly.hiddenIds)
    }

    @Test
    fun a_missing_part_is_named_and_the_other_boxes_read() {
        val thread = FireRequestAssembly.of(listOf(received(parts[0], 0), received(parts[1], 1))).threads.single()
        assertEquals(listOf(3), thread.missing)
        val read = thread.read()
        assertEquals(setOf(1, 2, 3, 4, 5, 6, 7, 8), read.boxes)
        assertEquals("2", read.request.charge.launcher)
    }

    @Test
    fun a_part_resent_later_completes_the_request() {
        val messages = listOf(received(parts[0], 0), received(parts[2], 1), received(parts[1], 40))
        assertTrue(FireRequestAssembly.of(messages).threads.single().isComplete)
    }

    @Test
    fun the_same_number_renewed_makes_a_second_request() {
        val renewed = parts[0].replace("112250 loc NOV 26", "120910 loc NOV 26")
        val messages =
            parts.mapIndexed { i, text -> received(text, i.toLong()) } +
                listOf(received(renewed, 10), received(parts[1], 11), received(parts[2], 12))
        val threads = FireRequestAssembly.of(messages).threads
        assertEquals(2, threads.size)
        assertEquals("120910 loc NOV 26", threads[1].read().request.requestGdh)
        assertTrue(threads.all { it.isComplete })
    }

    @Test
    fun the_same_number_after_two_hours_is_a_new_request() {
        val messages = listOf(received(parts[0], 0), received(parts[1], 121))
        assertEquals(2, FireRequestAssembly.of(messages).threads.size)
        val within = listOf(received(parts[0], 0), received(parts[1], 120))
        assertEquals(1, FireRequestAssembly.of(within).threads.size)
    }

    @Test
    fun another_part_count_is_another_request() {
        val messages = listOf(received(parts[0], 0), received("[DT AB1234 2/2]\n5 non rempli", 1))
        assertEquals(2, FireRequestAssembly.of(messages).threads.size)
    }

    @Test
    fun two_senders_with_the_same_number_are_two_requests() {
        val messages = listOf(received(parts[0], 0, "!oa"), received(parts[1], 1, "!drone"))
        assertEquals(2, FireRequestAssembly.of(messages).threads.size)
    }

    @Test
    fun my_request_and_theirs_with_the_same_number_stay_apart() {
        val messages = listOf(sent(parts[0], 0), received(parts[1], 1))
        val threads = FireRequestAssembly.of(messages).threads
        assertEquals(listOf(true, false), threads.map { it.fromLocal })
    }

    @Test
    fun date_time_groups_go_to_the_request_of_the_other_side() {
        val messages =
            parts.mapIndexed { i, text -> sent(text, i.toLong()) } +
                listOf(
                    received("[DT AB1234] GDH attribution 112252 loc NOV 26", 5),
                    received("[DT AB1234] GDH fin 112310 loc NOV 26", 20),
                )
        val assembly = FireRequestAssembly.of(messages)
        val thread = assembly.threads.single()
        assertEquals("112252 loc NOV 26", thread.allocation?.gdh)
        assertEquals("112310 loc NOV 26", thread.end?.gdh)
        assertEquals(messages.last().id, thread.anchor.id)
        assertEquals(emptyList(), assembly.orphanReplies)
    }

    @Test
    fun a_date_time_group_goes_to_the_latest_request_sent_before_it() {
        val renewed = parts[0].replace("112250", "120910")
        val messages =
            listOf(
                sent(parts[0], 0),
                received("[DT AB1234] GDH attribution 112252 loc NOV 26", 5),
                sent(renewed, 600),
                received("[DT AB1234] GDH attribution 120912 loc NOV 26", 605),
            )
        val threads = FireRequestAssembly.of(messages).threads
        assertEquals(listOf("112252 loc NOV 26", "120912 loc NOV 26"), threads.map { it.allocation?.gdh })
    }

    @Test
    fun the_latest_date_time_group_of_a_kind_is_shown() {
        val messages =
            listOf(
                sent(parts[0], 0),
                received("[DT AB1234] GDH attribution 112252 loc NOV 26", 5),
                received("[DT AB1234] GDH attribution 112255 loc NOV 26", 8),
            )
        assertEquals("112255 loc NOV 26", FireRequestAssembly.of(messages).threads.single().allocation?.gdh)
    }

    @Test
    fun a_date_time_group_before_the_parts_still_finds_its_request() {
        val messages = listOf(received("[DT AB1234] GDH attribution 112252 loc NOV 26", 0), sent(parts[0], 1))
        assertEquals("112252 loc NOV 26", FireRequestAssembly.of(messages).threads.single().allocation?.gdh)
    }

    @Test
    fun a_date_time_group_without_request_is_an_orphan() {
        val reply = received("[DT ZZ9] GDH fin 112310Z NOV 26", 0)
        val assembly = FireRequestAssembly.of(listOf(reply, sent(parts[0], 1)))
        assertEquals(listOf(reply), assembly.orphanReplies)
        assertNull(assembly.threads.single().end)
    }

    @Test
    fun ordinary_messages_are_left_alone() {
        val messages =
            listOf(
                received("Reçu, je prépare une demande.", 0),
                received("Voir [DT AB1234 1/3]\n1 AB1234", 1),
                received("[DT AB1234 1/10]\n1 AB1234", 2),
            )
        val assembly = FireRequestAssembly.of(messages)
        assertEquals(emptyList(), assembly.threads)
        assertEquals(emptySet(), assembly.hiddenIds)
    }
}
