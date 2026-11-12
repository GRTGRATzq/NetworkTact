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

/** A line of the message: [box] 1 to 11, or [HEADER] for the header line, and its [text] as sent. */
data class BoxLine(val box: Int, val text: String) {
    companion object {
        const val HEADER = 0
    }
}

/**
 * Writes a [FireRequest] as the lines of its message, before it is split into parts: the header line, then one line per
 * box, `<n> <elements separated by « · »>`, with the words and codes of the sheet. The 11 boxes are always there, an
 * empty one as `<n> non rempli`. Box 7 has one line per line of the sheet, each starting `7 <letter>`.
 *
 * An element whose boxes are all empty is left out. Free text loses any « · » and line break, which would break the
 * reading of the message.
 */
object FireRequestText {

    /** Boxes 1 to 11 but 7, each as its elements; box 7 has lines of its own. */
    private val boxes: List<(FireRequest) -> List<String>> =
        listOf(
            { listOfNotNull(cleanText(it.number).ifEmpty { null }) },
            { fireType(it.fireType) },
            { effect(it.effect) },
            { observation(it.observation) },
            { charge(it.charge) },
            { targets(it.targets) },
            { emptyList() },
            { shape(it.shape) },
            { adjustment(it.adjustment) },
            { friends(it.friends) },
            { command(it.command) },
        )

    private const val LOCATION_BOX = 7

    fun write(request: FireRequest): List<BoxLine> = buildList {
        header(request)?.let { add(BoxLine(BoxLine.HEADER, it)) }
        boxes.forEachIndexed { index, elements ->
            val box = index + 1
            val lines =
                if (box == LOCATION_BOX) {
                    location(request.location)
                } else {
                    listOfNotNull(elements(request).takeIf { it.isNotEmpty() }?.joinToString(Sheet.SEPARATOR))
                }
            if (lines.isEmpty()) add(BoxLine(box, "$box ${Sheet.NOT_FILLED}"))
            lines.forEach { add(BoxLine(box, "$box $it")) }
        }
    }

    /** [text] as it may go into the message: on one line, without « · », trimmed. */
    fun clean(text: String): String = cleanText(text)

    private fun header(request: FireRequest): String? = listOfNotNull(
        labelled(Sheet.ORIGIN, request.origin),
        labelled(Sheet.RECIPIENT, request.recipient),
        labelled(Sheet.REQUEST_GDH, request.requestGdh),
    )
        .takeIf { it.isNotEmpty() }
        ?.joinToString(Sheet.SEPARATOR)
}
