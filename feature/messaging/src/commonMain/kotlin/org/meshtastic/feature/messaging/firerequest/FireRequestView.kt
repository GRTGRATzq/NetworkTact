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

import org.meshtastic.core.model.Message
import org.meshtastic.core.model.firerequest.FireRequestAssembly
import org.meshtastic.core.model.firerequest.FireRequestMessage
import org.meshtastic.core.model.firerequest.FireRequestSheet
import org.meshtastic.core.ui.util.MapFocusPoint

/** What a request's card can do: send a text in the conversation, resend a part, open the map on a point. */
internal class FireRequestCardActions(
    val send: (String) -> Unit,
    val resend: (Message) -> Unit,
    val showOnMap: ((MapFocusPoint) -> Unit)?,
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

        /** The requests among [messages], any conversation's history; ordinary messages are left alone. */
        fun of(messages: List<Message>): FireRequestView {
            val tagged = messages.filter { it.text.startsWith(TAG) }
            if (tagged.isEmpty()) return EMPTY
            val assembly =
                FireRequestAssembly.of(
                    tagged.map { FireRequestMessage(it.uuid, it.fromLocal, it.node.user.id, it.displayTime, it.text) },
                )
            return FireRequestView(assembly, tagged.associateBy { it.uuid })
        }
    }
}
