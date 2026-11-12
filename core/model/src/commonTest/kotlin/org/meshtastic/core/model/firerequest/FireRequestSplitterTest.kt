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
import kotlin.test.assertTrue

class FireRequestSplitterTest {

    private fun split(request: FireRequest) = FireRequestSplitter.split(request.number, FireRequestText.write(request))

    @Test
    fun specification_example_gives_the_three_validated_parts() {
        val parts = split(FireRequestExamples.specification)
        assertEquals(FireRequestExamples.specificationParts, parts)
        assertEquals(listOf(166, 154, 168), parts.map { it.utf8Size() })
        assertEquals(488, parts.sumOf { it.utf8Size() })
    }

    @Test
    fun a_part_of_exactly_200_bytes_is_kept_whole() {
        // Tag "[DT AB1234 1/1]" is 15 bytes; each line costs its size plus a line break.
        val lines = listOf(BoxLine(1, "1 " + "a".repeat(88)), BoxLine(2, "2 " + "b".repeat(91)))
        val parts = FireRequestSplitter.split("AB1234", lines)
        assertEquals(1, parts.size)
        assertEquals(200, parts.single().utf8Size())
    }

    @Test
    fun one_byte_more_opens_a_second_part_between_two_boxes() {
        val lines = listOf(BoxLine(1, "1 " + "a".repeat(88)), BoxLine(2, "2 " + "b".repeat(92)))
        val parts = FireRequestSplitter.split("AB1234", lines)
        assertEquals(listOf("[DT AB1234 1/2]\n1 " + "a".repeat(88), "[DT AB1234 2/2]\n2 " + "b".repeat(92)), parts)
    }

    @Test
    fun accents_and_separators_count_two_bytes() {
        // « é » and « · » are two bytes each: 44 of them make 88 bytes, so this part is 200 bytes too.
        val lines = listOf(BoxLine(1, "1 " + "é".repeat(44)), BoxLine(2, "2 " + "·".repeat(45) + "b"))
        val parts = FireRequestSplitter.split("AB1234", lines)
        assertEquals(1, parts.size)
        assertEquals(200, parts.single().utf8Size())
    }

    @Test
    fun a_box_too_long_for_a_part_is_cut_at_a_separator_and_keeps_its_number() {
        val elements = (1..12).map { "élément numéro $it" }
        val line = BoxLine(6, "6 " + elements.joinToString(" · "))
        val parts = FireRequestSplitter.split("AB1234", listOf(BoxLine(5, "5 non rempli"), line))
        assertEquals(2, parts.size)
        val pieces = parts.flatMap { it.lines().drop(1) }.filter { it.startsWith("6 ") }
        assertEquals(2, pieces.size)
        assertEquals(elements, pieces.flatMap { it.removePrefix("6 ").split(" · ") })
        parts.forEach { assertTrue(it.utf8Size() <= 200, it) }
    }

    @Test
    fun every_part_starts_with_its_tag() {
        val parts = split(FireRequestExamples.longest)
        parts.forEachIndexed { index, part ->
            assertTrue(part.startsWith("[DT ABCDE12345 ${index + 1}/${parts.size}]\n"), part)
        }
    }

    @Test
    fun every_part_of_the_longest_request_is_at_most_200_bytes() {
        split(FireRequestExamples.longest).forEach {
            assertTrue(it.utf8Size() <= FireRequestSplitter.MAX_PART_BYTES, "${it.utf8Size()}: $it")
        }
    }

    @Test
    fun the_longest_request_is_too_long_to_send() {
        // Every box filled to its limit takes 14 parts: past MAX_PARTS, the form refuses to send it.
        assertEquals(14, split(FireRequestExamples.longest).size)
    }

    @Test
    fun a_line_of_box_7_never_needs_cutting() {
        // A piece of a box 7 line would lose its line letter, so each line must fit in a part on its own.
        val room = FireRequestSplitter.MAX_PART_BYTES - "[DT ABCDE12345 1/9]\n".utf8Size()
        FireRequestText.write(FireRequestExamples.longest)
            .filter { it.box == 7 }
            .forEach { assertTrue(it.text.utf8Size() <= room, "${it.text.utf8Size()}: ${it.text}") }
    }

    @Test
    fun a_cut_box_of_the_longest_request_reads_back_whole() {
        val written = FireRequestText.write(FireRequestExamples.longest).first { it.box == 2 }.text
        val pieces = split(FireRequestExamples.longest).flatMap { it.lines().drop(1) }.filter { it.startsWith("2 ") }
        assertTrue(pieces.size > 1)
        assertEquals(written, "2 " + pieces.joinToString(" · ") { it.removePrefix("2 ") })
    }

    @Test
    fun an_empty_request_fits_in_one_part() {
        val parts = FireRequestSplitter.split("A1", FireRequestText.write(FireRequest(number = "A1")))
        assertEquals(1, parts.size)
        assertTrue(parts.single().startsWith("[DT A1 1/1]\n1 A1\n2 non rempli"))
    }
}
