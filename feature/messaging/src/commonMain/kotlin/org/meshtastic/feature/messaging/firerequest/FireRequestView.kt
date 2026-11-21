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

import org.meshtastic.core.model.ContactKey
import org.meshtastic.core.model.Message
import org.meshtastic.core.model.firerequest.FireRequestAck
import org.meshtastic.core.model.firerequest.FireRequestAssembly
import org.meshtastic.core.model.firerequest.FireRequestDesignation
import org.meshtastic.core.model.firerequest.FireRequestMessage
import org.meshtastic.core.model.firerequest.FireRequestPart
import org.meshtastic.core.model.firerequest.FireRequestReply
import org.meshtastic.core.model.firerequest.FireRequestSheet
import org.meshtastic.core.ui.util.MapFocusPoint
import kotlin.time.Duration.Companion.days

/**
 * What a request's card can do: send a text in the conversation, resend a part, open the map on a point, send several
 * parts one after the other, spaced as the form sends them, to the conversation [contactKey] names ([sendParts]), and
 * send a text in a direct message to one radio ([sendDirect]).
 */
internal class FireRequestCardActions(
    val send: (String) -> Unit,
    val resend: (Message) -> Unit,
    val showOnMap: ((MapFocusPoint) -> Unit)?,
    val sendParts: (texts: List<String>, contactKey: String) -> Unit = { _, _ -> },
    val contactKey: String = "",
    /** Sends a text in a direct message to the node whose id is given (« Accusé DT », attribution, GDH fin). */
    val sendDirect: (text: String, nodeId: String) -> Unit = { _, _ -> },
    /** The date-time group of now, in the zone the operator chose. */
    val gdhNow: () -> String = { "" },
)

/**
 * The fire requests of the conversation on screen: their [assembly], and the stored [messages] behind them, by id, for
 * the state of each part sent and for « Voir les messages bruts ».
 */
data class FireRequestView(val assembly: FireRequestAssembly, val messages: Map<Long, Message>) {

    /** Messages shown inside a request's card rather than on their own. */
    val hiddenIds: Set<Long>
        get() = assembly.hiddenIds

    companion object {
        val EMPTY = FireRequestView(FireRequestAssembly(emptyList(), emptyList()), emptyMap())

        private val TAG = "[${FireRequestSheet.TAG} "

        /**
         * The requests among [messages], any conversation's history; ordinary messages are left alone. On a channel,
         * [direct] adds the acknowledgements and allocations that came in direct messages (with the key of their
         * conversation), for the requests of this channel only: a request's parts are never taken from them.
         */
        fun of(messages: List<Message>, direct: List<Pair<String, Message>> = emptyList()): FireRequestView {
            val tagged = messages.filter { it.text.startsWith(TAG) }
            if (tagged.isEmpty()) return EMPTY
            val ids = tagged.map { it.uuid }.toSet()
            val answers = direct.filter { (_, it) -> it.uuid !in ids && isAnswer(it.text) }
            val assembly =
                FireRequestAssembly.of(
                    tagged.map { FireRequestMessage(it.uuid, it.fromLocal, it.node.user.id, it.displayTime, it.text) } +
                        answers.map { (key, it) ->
                            val to = if (it.fromLocal) ContactKey(key).addressString else ""
                            FireRequestMessage(it.uuid, it.fromLocal, it.node.user.id, it.displayTime, it.text, to)
                        },
                )
            return FireRequestView(assembly, (tagged + answers.map { it.second }).associateBy { it.uuid })
        }

        /** How far back a channel's requests look for their answers, at most. */
        val ANSWERS_MAX_AGE = 7.days

        /**
         * When this phone stored the first part of the oldest request among [messages] (sent or received: the stored
         * time the query compares, not the radio's clock); null when they hold none.
         */
        fun firstPartTime(messages: List<Message>): Long? = messages
            .filter { it.text.startsWith(TAG) && FireRequestPart.parse(it.text) != null }
            .minOfOrNull { it.receivedTime }

        /**
         * Since when the answers in direct messages are read: from the first part of the oldest request ([firstPart]),
         * so a writer's card keeps its acknowledgements however old the request, but [ANSWERS_MAX_AGE] back at most.
         */
        fun answersSince(firstPart: Long, nowMillis: Long): Long =
            maxOf(firstPart, nowMillis - ANSWERS_MAX_AGE.inWholeMilliseconds)

        private fun isAnswer(text: String): Boolean = FireRequestReply.parse(text) != null ||
            FireRequestAck.parse(text) != null ||
            FireRequestDesignation.parse(text) != null
    }
}
