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
package org.meshtastic.feature.messaging.filter

import org.meshtastic.feature.messaging.priority.MessagePriority
import org.meshtastic.feature.messaging.status.SentStatus
import org.meshtastic.feature.messaging.status.isAwaitingRecipientAck
import org.meshtastic.feature.messaging.status.sentStatusOf

/**
 * Display-only filter over one conversation's messages. Nothing is removed from storage; a message simply is not shown
 * while it does not match.
 *
 * @property priorities the priorities to show; empty shows every priority.
 * @property unackedOnly keep only direct messages this device sent that the recipient has not acknowledged. Meaningless
 *   in a channel, where no message can be acknowledged by a recipient, so it matches nothing there.
 */
data class ThreadFilter(val priorities: Set<MessagePriority> = emptySet(), val unackedOnly: Boolean = false) {
    val isActive: Boolean
        get() = priorities.isNotEmpty() || unackedOnly

    fun togglePriority(priority: MessagePriority): ThreadFilter =
        copy(priorities = if (priority in priorities) priorities - priority else priorities + priority)

    /** Whether a message with [text] and [sentStatus] is shown; see [sentStatusOf] for how the status is derived. */
    fun matches(text: String, fromLocal: Boolean, sentStatus: SentStatus, isDirectMessage: Boolean): Boolean {
        if (priorities.isNotEmpty() && MessagePriority.of(text) !in priorities) return false
        return !unackedOnly || isAwaitingRecipientAck(sentStatus, fromLocal, isDirectMessage)
    }
}
