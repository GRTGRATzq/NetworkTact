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

import org.meshtastic.core.model.firerequest.FireRequestSheet as Sheet

/**
 * A request read back from the lines received: [request], the [boxes] whose line arrived (1 to 11; a box of a missing
 * part is absent, which is not the same as `non rempli`), and the elements that did not read, per box ([BoxLine.HEADER]
 * for the header and for lines that are no box), to be shown as they came.
 */
data class ReadRequest(
    val request: FireRequest,
    val boxes: Set<Int>,
    val unread: Map<Int, List<String>>,
    val hasHeader: Boolean,
)

/**
 * Reads the lines of the parts received, in part order, back into a [FireRequest]: the inverse of [FireRequestText] and
 * [FireRequestSplitter]. Lines of a box cut across two parts are joined again. Reading is lenient: what does not read
 * is kept in [ReadRequest.unread], never dropped.
 */
object FireRequestReader {

    private val BOX = Regex("""^(1[01]|[1-9]) (.+)$""")
    private const val LOCATION_BOX = 7

    private val boxReaders: Map<Int, (FireRequest, List<String>, MutableList<String>) -> FireRequest> =
        mapOf(
            2 to { r, e, u -> r.copy(fireType = readFireType(e, u)) },
            3 to { r, e, u -> r.copy(effect = readEffect(e, u)) },
            4 to { r, e, u -> r.copy(observation = readObservation(e, u)) },
            5 to { r, e, u -> r.copy(charge = readCharge(e, u)) },
            6 to { r, e, u -> r.copy(targets = readTargets(e, u)) },
            8 to { r, e, u -> r.copy(shape = readShape(e, u)) },
            9 to { r, e, u -> r.copy(adjustment = readAdjustment(e, u)) },
            10 to { r, e, u -> r.copy(friends = readFriends(e, u)) },
            11 to { r, e, u -> r.copy(command = readCommand(e, u)) },
        )

    fun read(lines: List<String>): ReadRequest {
        val unread = mutableMapOf<Int, MutableList<String>>()
        fun unreadOf(box: Int) = unread.getOrPut(box) { mutableListOf() }
        val contents = mutableMapOf<Int, MutableList<String>>()
        var request = FireRequest()
        var hasHeader = false
        var previousBox: Int? = null
        for (line in lines) {
            val match = BOX.find(line)
            if (match == null) {
                val header = readHeader(line, request)
                if (header != null) request = header.also { hasHeader = true } else unreadOf(BoxLine.HEADER) += line
                previousBox = null
                continue
            }
            val box = match.groupValues[1].toInt()
            val content = match.groupValues[2]
            val boxContents = contents.getOrPut(box) { mutableListOf() }
            // A box cut across two parts goes on in a line with the same number: join it back, except box 7, whose
            // lines are the lines of the sheet.
            if (box == previousBox && box != LOCATION_BOX) {
                boxContents[boxContents.lastIndex] += Sheet.SEPARATOR + content
            } else {
                boxContents += content
            }
            previousBox = box
        }
        for ((box, boxContents) in contents) {
            request = readBox(request, box, boxContents, unreadOf(box))
        }
        return ReadRequest(request, contents.keys, unread.filterValues { it.isNotEmpty() }, hasHeader)
    }

    private fun readBox(request: FireRequest, box: Int, contents: List<String>, unread: MutableList<String>) =
        when (box) {
            1 -> contents.first().takeIf { it != Sheet.NOT_FILLED }?.let { request.copy(number = it) } ?: request

            LOCATION_BOX ->
                contents
                    .fold(request.location) { location, line -> readLocationLine(line, location, unread) }
                    .let { request.copy(location = it) }

            else -> boxReaders.getValue(box)(request, contents.flatMap { elementsOf(it) }, unread)
        }

    /** The header line read into [request], or null when [line] is not the header. */
    private fun readHeader(line: String, request: FireRequest): FireRequest? {
        val elements = line.split(Sheet.SEPARATOR)
        if (elements.none { e -> HEADER_LABELS.any { valueOf(e, it) != null } }) return null
        return elements.fold(request) { r, element ->
            valueOf(element, Sheet.ORIGIN)?.let { r.copy(origin = it) }
                ?: valueOf(element, Sheet.RECIPIENT)?.let { r.copy(recipient = it) }
                ?: valueOf(element, Sheet.REQUEST_GDH)?.let { r.copy(requestGdh = it) }
                ?: r
        }
    }

    private val HEADER_LABELS = listOf(Sheet.ORIGIN, Sheet.RECIPIENT, Sheet.REQUEST_GDH)
}
