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

import org.meshtastic.core.model.firerequest.Command
import org.meshtastic.core.model.firerequest.FireRequest
import org.meshtastic.core.model.firerequest.FireType
import org.meshtastic.core.model.firerequest.ImpactLine
import org.meshtastic.core.model.firerequest.Location
import org.meshtastic.core.model.firerequest.ReferenceLine
import org.meshtastic.core.model.firerequest.Shift
import org.meshtastic.core.model.utf8Size
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FireRequestFormTest {

    private val ready = FireRequest(number = "AB1234", command = Command(whenReady = true))

    private fun block(request: FireRequest, recipient: Boolean = true, connected: Boolean = true) =
        sendBlock(request, FireRequestPreview.of(request), recipient, connected)

    @Test
    fun a_request_with_a_number_and_a_recipient_may_be_sent() {
        assertNull(block(ready))
    }

    @Test
    fun the_fire_number_is_the_only_box_required() {
        assertEquals(SendBlock.NO_NUMBER, block(FireRequest()))
        assertNull(block(FireRequest(number = "A1")))
    }

    @Test
    fun reasons_come_in_order() {
        assertEquals(SendBlock.NO_NUMBER, block(FireRequest(), recipient = false, connected = false))
        assertEquals(SendBlock.NO_RECIPIENT, block(ready, recipient = false, connected = false))
        assertEquals(SendBlock.DISCONNECTED, block(ready, connected = false))
    }

    @Test
    fun a_rep_shift_needs_its_direction() {
        val withoutDirection =
            ready.copy(location = Location(reference = ReferenceLine(first = "R1", nearFarM = "100")))
        assertEquals(SendBlock.REFERENCE_DIRECTION, block(withoutDirection))
        val withDirection =
            withoutDirection.copy(
                location = Location(reference = ReferenceLine(nearFar = Shift.NEARER, nearFarM = "100")),
            )
        assertNull(block(withDirection))
        assertFalse(ReferenceLine(eastWest = Shift.EAST).hasShiftWithoutDirection())
    }

    @Test
    fun more_than_nine_parts_cannot_be_sent() {
        val long = "X".repeat(MAX_FREE_BYTES)
        val request =
            ready.copy(
                origin = long,
                recipient = long,
                fireType = FireType(ImpactLine.entries.toSet(), true, long, long, true, long, true),
                command = Command(true, true, long, long, long, long, long, long, long),
                location = Location(reference = ReferenceLine(long, long)),
            )
        val preview = FireRequestPreview.of(request)
        assertTrue(preview.parts.size <= 9, "${preview.parts.size}")
        val tooLong = FireRequestPreview(List(10) { "p" }, 10)
        assertTrue(tooLong.tooManyParts)
        assertEquals(SendBlock.TOO_MANY_PARTS, sendBlock(ready, tooLong, true, true))
    }

    @Test
    fun preview_counts_the_bytes_of_every_part() {
        val preview = FireRequestPreview.of(ready)
        assertEquals(preview.parts.sumOf { it.utf8Size() }, preview.bytes)
        assertTrue(preview.parts.first().startsWith("[DT AB1234 1/"))
    }

    @Test
    fun fire_number_keeps_capitals_and_digits_only() {
        assertEquals("AB1234", numberInput("ab 12-34"))
        assertEquals("ABCDEFGHIJ", numberInput("abcdefghijklmn"))
        assertEquals("", numberInput("é·/"))
    }

    @Test
    fun free_boxes_stop_at_24_bytes_without_cutting_a_character() {
        assertEquals(24, freeInput("a".repeat(30)).utf8Size())
        val accents = freeInput("é".repeat(20))
        assertEquals(12, accents.length)
        assertEquals("Pont détruit", freeInput("Pont·\ndétruit"))
    }

    @Test
    fun number_boxes_keep_digits_only() {
        assertEquals("1600", digitsInput("16 00 m"))
        assertEquals("1234567", digitsInput("123456789"))
    }

    @Test
    fun boxes_sent_as_one_word_say_what_will_go() {
        assertEquals("R1", withoutSpaces("R 1"))
        assertEquals("14H30", withoutSpaces("14 H30"))
        assertNull(withoutSpaces("14H30"))
        assertNull(withoutSpaces(" 14H30 "))
    }
}
