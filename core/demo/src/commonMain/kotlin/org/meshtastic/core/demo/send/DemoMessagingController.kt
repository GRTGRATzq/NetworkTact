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
package org.meshtastic.core.demo.send

import org.meshtastic.core.model.DataPacket
import org.meshtastic.core.repository.MessagingController
import org.meshtastic.proto.SharedContact

/**
 * [MessagingController] for the demo screens: reactions, shared contacts and packets are accepted and dropped. Nothing
 * reaches a radio; the demo history shows no reaction rather than an invented delivery state.
 */
class DemoMessagingController : MessagingController {
    override suspend fun sendMessage(packet: DataPacket) = Unit

    override suspend fun sendReaction(emoji: String, replyId: Int, contactKey: String) = Unit

    override suspend fun importContact(contact: SharedContact) = Unit

    override suspend fun sendSharedContact(nodeNum: Int): Boolean = false
}
