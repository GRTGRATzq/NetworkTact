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

import org.meshtastic.core.model.Node
import org.meshtastic.core.model.NodeAddress
import org.meshtastic.core.model.firerequest.FireRequest
import org.meshtastic.core.model.firerequest.FireRequestSplitter
import org.meshtastic.core.model.firerequest.FireRequestText
import org.meshtastic.core.model.firerequest.ReferenceLine
import org.meshtastic.core.model.utf8Size

/** The parts a request would be sent as, and its total size in bytes. */
data class FireRequestPreview(val parts: List<String>, val bytes: Int) {
    val tooManyParts: Boolean
        get() = parts.size > FireRequestSplitter.MAX_PARTS

    companion object {
        fun of(request: FireRequest): FireRequestPreview {
            val parts = FireRequestSplitter.split(request.number, FireRequestText.write(request))
            return FireRequestPreview(parts, parts.sumOf { it.utf8Size() })
        }
    }
}

/** Why « Envoyer la demande » is greyed, first reason first; none when the request may go. */
enum class SendBlock {
    /** Box 1 is the only box required: its number tags every part and ties the date-time groups back. */
    NO_NUMBER,
    NO_RECIPIENT,

    /** A shift of the Rep line has metres but no direction. */
    REFERENCE_DIRECTION,
    TOO_MANY_PARTS,
    DISCONNECTED,
}

fun sendBlock(
    request: FireRequest,
    preview: FireRequestPreview,
    hasRecipient: Boolean,
    connected: Boolean,
): SendBlock? = when {
    request.number.isBlank() -> SendBlock.NO_NUMBER
    !hasRecipient -> SendBlock.NO_RECIPIENT
    request.location.reference.hasShiftWithoutDirection() -> SendBlock.REFERENCE_DIRECTION
    preview.tooManyParts -> SendBlock.TOO_MANY_PARTS
    !connected -> SendBlock.DISCONNECTED
    else -> null
}

/** Metres typed for a shift whose direction is not chosen: they would not be written. */
fun ReferenceLine.hasShiftWithoutDirection(): Boolean = listOf(
    eastWest to eastWestM,
    northSouth to northSouthM,
    rightLeft to rightLeftM,
    nearFar to nearFarM,
    upDown to upDownM,
)
    .any { (direction, metres) -> direction == null && metres.isNotBlank() }

/** The direct conversation with [node]: on the PKI channel when both radios have keys, as everywhere in the app. */
fun directContactKey(node: Node, ourNode: Node?): String {
    val hasPkc = ourNode?.hasPKC == true && node.hasPKC
    return "${if (hasPkc) NodeAddress.PKC_CHANNEL_INDEX else node.channel}${node.user.id}"
}

/** Box 1 as typed: capital letters and digits only, [FireRequestSplitter.MAX_NUMBER] at most. */
fun numberInput(text: String): String =
    text.uppercase().filter { it in 'A'..'Z' || it in '0'..'9' }.take(FireRequestSplitter.MAX_NUMBER)

/** The longest free text of a box, in bytes. */
const val MAX_FREE_BYTES = 24

/** The longest number typed in a box (bearings, distances, coordinates…). */
const val MAX_DIGITS = 7

/** A free box as typed: cut at [MAX_FREE_BYTES] bytes, never inside a character, and without « · ». */
fun freeInput(text: String): String {
    val clean = text.replace("·", "").replace("\n", " ")
    var end = clean.length
    while (clean.substring(0, end).utf8Size() > MAX_FREE_BYTES) end--
    return clean.substring(0, end)
}

/** A number box as typed: digits only, [MAX_DIGITS] at most. */
fun digitsInput(text: String): String = text.filter { it in '0'..'9' }.take(MAX_DIGITS)

/** What a box that is sent as one word becomes: [text] without its spaces, or null when it has none to lose. */
fun withoutSpaces(text: String): String? = text.trim().takeIf { it.contains(' ') }?.replace(" ", "")
