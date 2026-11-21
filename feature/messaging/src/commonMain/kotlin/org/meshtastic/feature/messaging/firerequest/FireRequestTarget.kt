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

/**
 * The conversations [request] goes to and the copy each gets, computed again from the request sent rather than from the
 * preview flow, which may lag one change behind; null when the radio chosen is no longer known.
 */
internal fun sendBatches(
    to: SendTarget,
    request: FireRequest,
    nodes: Map<Int, Node>,
    ours: Node?,
): List<Pair<String, FireRequest>>? = when (to) {
    is SendTarget.ToRadio -> nodes[to.radio.num]?.let { node -> listOf(directContactKey(node, ours) to request) }
    is SendTarget.OnChannels -> to.channels.map { it.contactKey to request.copy(recipient = it.name) }
}

/** [this] as it goes to [target]: on a channel, « Destinataire » is that channel's name. */
internal fun FireRequest.addressedTo(target: SendTarget?): FireRequest =
    (target as? SendTarget.OnChannels)?.let { copy(recipient = it.channels.first().name) } ?: this

/** A channel of the radio: its [index] and the [name] written as « Destinataire » (« Principal » when unnamed). */
data class ChannelOption(val index: Int, val name: String) {
    /** The conversation of the channel. */
    val contactKey: String
        get() = "$index${NodeAddress.ID_BROADCAST}"
}

/** Where « Envoyer à » sends the request. */
sealed interface SendTarget {
    /** On a channel, or on two for « Les deux » (each copy names its own channel as « Destinataire »). */
    data class OnChannels(val channels: List<ChannelOption>) : SendTarget

    /** In direct messages to one radio, with acknowledgement (v1). */
    data class ToRadio(val radio: RecipientOption) : SendTarget
}

/**
 * The channels « Envoyer à » offers: the fire team's channel (« Équipe TIR »), when the radio has one, and the primary
 * channel (« Principal »).
 */
data class SendChannels(val fireTeam: ChannelOption?, val primary: ChannelOption) {

    /** « Équipe TIR » when that channel exists, otherwise « Principal ». */
    val default: SendTarget
        get() = SendTarget.OnChannels(listOf(fireTeam ?: primary))

    /** « Les deux », offered only when there are two channels. */
    val both: SendTarget?
        get() = fireTeam?.let { SendTarget.OnChannels(listOf(it, primary)) }

    companion object {
        const val PRIMARY = "Principal"

        /**
         * The choices among the radio's channel names, in channel order: the primary first, null when the radio has not
         * given its channels yet. The fire team's is the first secondary channel named « Équipe TIR » or « TIR », case,
         * accents and spaces ignored.
         */
        fun of(names: List<String>): SendChannels? {
            val primary = names.firstOrNull()?.let { ChannelOption(0, it.trim().ifEmpty { PRIMARY }) } ?: return null
            val fireTeam =
                names
                    .withIndex()
                    .drop(1)
                    .firstOrNull { isFireTeam(it.value) }
                    ?.let { ChannelOption(it.index, it.value.trim()) }
            return SendChannels(fireTeam, primary)
        }

        internal fun isFireTeam(name: String): Boolean {
            val key =
                name.uppercase().filterNot { it.isWhitespace() }.map { if (it in "ÉÈÊË") 'E' else it }.joinToString("")
            return key == "EQUIPETIR" || key == "TIR"
        }
    }
}
