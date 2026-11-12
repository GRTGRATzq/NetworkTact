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

import org.meshtastic.core.model.Message

/** What a message of a fire request reads as in a conversation notification, instead of its raw text. */
sealed interface FireRequestNotificationLine {

    /**
     * The parts of request [number]: [received] of its [count] parts are here. [fromLocal] when this phone sent it, the
     * line then reads as the sender's card does.
     */
    data class Request(val number: String, val received: Int, val count: Int, val fromLocal: Boolean) :
        FireRequestNotificationLine

    /** A date-time group sent back, in one line: `DT AB1234 · GDH attribution 112252 loc NOV 26`. */
    data class Reply(val reply: FireRequestReply) : FireRequestNotificationLine {
        val text: String
            get() = "${FireRequestSheet.TAG} ${reply.number} · ${reply.kind.label} ${reply.gdh}"
    }
}

/**
 * One notification per fire request (`docs/PLAN-DEMANDE-DE-TIR.md` § 3.10): in the conversation shown by the
 * notification, the parts of a request make one line, at its latest part; and only its first part alerts, the next ones
 * update the notification in silence.
 */
data class FireRequestNotification(
    /** Parts shown through the line of their request, at another message: left out of the notification. */
    val hidden: Set<Long>,
    /** The line each remaining message of a request reads as, by message id. */
    val lines: Map<Long, FireRequestNotificationLine>,
) {
    companion object {
        /** [messages]: the whole conversation, in any order. */
        fun of(messages: List<FireRequestMessage>): FireRequestNotification {
            val assembly = FireRequestAssembly.of(messages)
            val hidden = mutableSetOf<Long>()
            val lines = mutableMapOf<Long, FireRequestNotificationLine>()
            assembly.threads.forEach { thread ->
                val parts = thread.parts.values.flatten().sortedBy { it.timeMillis }
                val latest = parts.last()
                hidden += parts.map { it.id } - latest.id
                lines[latest.id] =
                    FireRequestNotificationLine.Request(
                        thread.number,
                        thread.received.size,
                        thread.count,
                        thread.fromLocal,
                    )
            }
            (assembly.threads.flatMap { it.replies } + assembly.orphanReplies).forEach { message ->
                FireRequestReply.parse(message.text)?.let { lines[message.id] = FireRequestNotificationLine.Reply(it) }
            }
            return FireRequestNotification(hidden, lines)
        }

        /**
         * Whether message [id] of [messages], a part received, belongs to a request one of whose parts had already
         * come: it then updates the notification without alerting again. A part sent again counts as already come.
         */
        fun isFollowUp(messages: List<FireRequestMessage>, id: Long): Boolean {
            val message = messages.firstOrNull { it.id == id && !it.fromLocal } ?: return false
            val thread =
                FireRequestAssembly.of(messages).threads.firstOrNull { thread ->
                    thread.parts.values.any { copies -> copies.any { it.id == id } }
                }
            return thread?.parts?.values?.flatten()?.any { it.id != id && it.timeMillis <= message.timeMillis } == true
        }
    }
}

/** This message as the fire request assembly reads it. */
fun Message.toFireRequestMessage(): FireRequestMessage =
    FireRequestMessage(uuid, fromLocal, node.user.id, displayTime, text)
