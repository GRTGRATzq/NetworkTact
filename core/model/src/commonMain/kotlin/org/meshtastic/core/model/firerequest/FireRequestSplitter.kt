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
import org.meshtastic.core.model.firerequest.FireRequestSheet as Sheet

/**
 * Splits the lines of a request into the parts sent one after the other, each at most [MAX_PART_BYTES] bytes of UTF-8
 * and starting with its tag `[DT <number> <k>/<N>]`. Lines are packed in order, as many per part as fit; a part is cut
 * between two boxes, never inside one, unless a box alone does not fit in a part: it is then cut at a « · », and goes
 * on in the next part on a line starting again with the box number, with no word added.
 */
object FireRequestSplitter {

    /** The largest part, in bytes of UTF-8 (the fork's limit for a text message). */
    const val MAX_PART_BYTES = 200

    /** The most parts a request may take; past this, it is too long to send. */
    const val MAX_PARTS = 9

    /** The tag that starts part [k] of [count] of request [number]. */
    fun tag(number: String, k: Int, count: Int): String = "[${Sheet.TAG} $number $k/$count]"

    /** The parts of [lines] for request [number]; more than [MAX_PARTS] means the request is too long. */
    fun split(number: String, lines: List<BoxLine>): List<String> {
        // The tag's size depends on how many digits k and N take: pack with one digit, and again if N needs more.
        var count = 1
        var bodies = pack(lines, tag(number, count, count).utf8Size())
        while (bodies.size.toString().length > count.toString().length) {
            count = bodies.size
            bodies = pack(lines, tag(number, count, count).utf8Size())
        }
        return bodies.mapIndexed { index, body ->
            (listOf(tag(number, index + 1, bodies.size)) + body).joinToString("\n")
        }
    }

    /** [lines] packed in order into bodies that each fit after a tag of [tagBytes] bytes. */
    private fun pack(lines: List<BoxLine>, tagBytes: Int): List<List<String>> {
        val room = MAX_PART_BYTES - tagBytes - 1
        val bodies = mutableListOf<MutableList<String>>(mutableListOf())
        fun left(): Int = room - bodies.last().sumOf { it.utf8Size() + 1 } + 1
        for (line in lines) {
            val pieces = if (line.text.utf8Size() <= room) listOf(line.text) else cut(line, left() - 1, room)
            for (piece in pieces) {
                if (piece.utf8Size() + 1 > left()) bodies.add(mutableListOf())
                bodies.last().add(piece)
            }
        }
        return bodies.filter { it.isNotEmpty() }
    }

    /**
     * A box longer than [room] bytes, cut into the longest pieces that fit, each cut at a « · » and each starting with
     * the box number: the first fills the [firstRoom] bytes left in the current part, the next ones whole parts. A
     * single element too long for a part stays whole (the form's limits keep that from happening).
     */
    private fun cut(line: BoxLine, firstRoom: Int, room: Int): List<String> {
        val prefix = if (line.box == BoxLine.HEADER) "" else "${line.box} "
        val elements = line.text.removePrefix(prefix).split(Sheet.SEPARATOR)
        val pieces = mutableListOf<String>()
        var current = ""
        var limit = firstRoom
        for (element in elements) {
            val next = if (current.isEmpty()) prefix + element else current + Sheet.SEPARATOR + element
            if (next.utf8Size() <= limit) {
                current = next
            } else {
                if (current.isNotEmpty()) pieces.add(current)
                current = prefix + element
                limit = room
            }
        }
        if (current.isNotEmpty()) pieces.add(current)
        return pieces
    }

    /** The longest request number, in characters. */
    const val MAX_NUMBER = 10
}
