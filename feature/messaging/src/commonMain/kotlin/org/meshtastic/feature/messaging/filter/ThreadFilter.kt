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

import org.meshtastic.core.model.team.TeamSuffix
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
 * @property team keep only messages whose sender declares this team in its long name (see [TeamSuffix]), ignoring case;
 *   null shows every sender. The team is what the sender declares, not a verified fact.
 */
data class ThreadFilter(
    val priorities: Set<MessagePriority> = emptySet(),
    val unackedOnly: Boolean = false,
    val team: String? = null,
) {
    val isActive: Boolean
        get() = priorities.isNotEmpty() || unackedOnly || team != null

    fun togglePriority(priority: MessagePriority): ThreadFilter =
        copy(priorities = if (priority in priorities) priorities - priority else priorities + priority)

    /** Selects [name], or clears the team filter when [name] is already the one selected. */
    fun toggleTeam(name: String): ThreadFilter = copy(team = if (team.equals(name, ignoreCase = true)) null else name)

    /**
     * Whether a message with [text] and [sentStatus], sent by a node named [senderLongName], is shown; see
     * [sentStatusOf] for how the status is derived.
     */
    fun matches(
        text: String,
        fromLocal: Boolean,
        sentStatus: SentStatus,
        isDirectMessage: Boolean,
        senderLongName: String = "",
    ): Boolean {
        val priorityOk = priorities.isEmpty() || MessagePriority.of(text) in priorities
        val teamOk = team == null || TeamSuffix.teamOf(senderLongName).equals(team, ignoreCase = true)
        val ackOk = !unackedOnly || isAwaitingRecipientAck(sentStatus, fromLocal, isDirectMessage)
        return priorityOk && teamOk && ackOk
    }
}
