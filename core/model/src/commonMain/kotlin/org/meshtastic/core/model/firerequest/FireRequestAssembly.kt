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

import kotlin.time.Duration.Companion.hours

/**
 * A text message of a conversation, as the assembly needs it: who wrote it ([sender], or this phone), when, and what.
 */
data class FireRequestMessage(
    val id: Long,
    val fromLocal: Boolean,
    val sender: String,
    val timeMillis: Long,
    val text: String,
)

/**
 * One fire request of a conversation, gathered from its parts: request [number] of [count] parts, written by this phone
 * ([fromLocal]) or by [sender]. [parts] holds, per part, every message that carried it: a part sent twice with the same
 * text (resent after a lost acknowledgement) counts once, both messages kept. [allocation] and [end] are the latest
 * date-time groups sent back for it by the other side.
 */
data class FireRequestThread(
    val number: String,
    val count: Int,
    val fromLocal: Boolean,
    val sender: String,
    val parts: Map<Int, List<FireRequestMessage>>,
    val replies: List<FireRequestMessage> = emptyList(),
) {
    /** The parts received, in order. */
    val received: List<Int>
        get() = parts.keys.sorted()

    /** The parts still missing, in order. */
    val missing: List<Int>
        get() = (1..count).filterNot { it in parts }

    val isComplete: Boolean
        get() = missing.isEmpty()

    val allocation: FireRequestReply?
        get() = latestReply(ReplyKind.ALLOCATION)

    val end: FireRequestReply?
        get() = latestReply(ReplyKind.END)

    /** Every message of this request, parts and replies, oldest first. */
    val messages: List<FireRequestMessage>
        get() = (parts.values.flatten() + replies).sortedBy { it.timeMillis }

    /** The message the request is shown at: the latest one, so it moves down as parts and replies arrive. */
    val anchor: FireRequestMessage
        get() = messages.last()

    /** The time of the first part received. */
    val firstTimeMillis: Long
        get() = parts.values.flatten().minOf { it.timeMillis }

    /** The request read from the parts received, each from its first message. */
    fun read(): ReadRequest = FireRequestReader.read(
        parts.toSortedMap().values.flatMap { messages ->
            FireRequestPart.parse(messages.first().text)?.lines.orEmpty()
        },
    )

    private fun latestReply(kind: ReplyKind): FireRequestReply? =
        replies.sortedBy { it.timeMillis }.mapNotNull { FireRequestReply.parse(it.text) }.lastOrNull { it.kind == kind }
}

/**
 * The fire requests of one conversation, and the date-time groups sent back that match none ([orphanReplies]).
 *
 * Parts are grouped by writer and request number. A part starts a new request, rather than joining the latest one of
 * that writer and number, when its part count differs, when that rank already came with another text, or when more than
 * [NEW_REQUEST_AFTER] passed since the latest part (« Renouveler » keeps the number). Order of arrival does not matter.
 * A date-time group goes to the latest request with that number written by the other side whose first part came before
 * it, or else to the latest one with that number.
 */
data class FireRequestAssembly(val threads: List<FireRequestThread>, val orphanReplies: List<FireRequestMessage>) {

    /** Messages that belong to a request but are not where it is shown: hidden from the list. */
    val hiddenIds: Set<Long> by lazy {
        threads.flatMap { thread -> thread.messages.map { it.id } - thread.anchor.id }.toSet()
    }

    /** The request shown at message [id], if any. */
    fun threadAt(id: Long): FireRequestThread? = threads.firstOrNull { it.anchor.id == id }

    companion object {
        val NEW_REQUEST_AFTER = 2.hours

        fun of(messages: List<FireRequestMessage>): FireRequestAssembly {
            val threads = mutableListOf<FireRequestThread>()
            val sorted = messages.sortedWith(compareBy({ it.timeMillis }, { it.id }))
            for (message in sorted) {
                val part = FireRequestPart.parse(message.text) ?: continue
                val index =
                    threads.indexOfLast {
                        it.fromLocal == message.fromLocal && it.sender == message.sender && it.number == part.number
                    }
                val current = threads.getOrNull(index)
                if (current != null && joins(current, part, message)) {
                    val sameRank = current.parts[part.k].orEmpty()
                    threads[index] = current.copy(parts = current.parts + (part.k to sameRank + message))
                } else {
                    threads +=
                        FireRequestThread(
                            part.number,
                            part.count,
                            message.fromLocal,
                            message.sender,
                            mapOf(part.k to listOf(message)),
                        )
                }
            }
            val orphans = mutableListOf<FireRequestMessage>()
            for (message in sorted) {
                val reply = FireRequestReply.parse(message.text) ?: continue
                val candidates =
                    threads.withIndex().filter { (_, t) ->
                        t.number == reply.number && t.fromLocal != message.fromLocal
                    }
                val target =
                    candidates.lastOrNull { (_, t) -> t.firstTimeMillis <= message.timeMillis }
                        ?: candidates.lastOrNull()
                if (target == null) {
                    orphans += message
                } else {
                    threads[target.index] = target.value.copy(replies = target.value.replies + message)
                }
            }
            return FireRequestAssembly(threads, orphans)
        }

        private fun joins(thread: FireRequestThread, part: FireRequestPart, message: FireRequestMessage): Boolean {
            val sameRank = thread.parts[part.k]
            val latest = thread.parts.values.flatten().maxOf { it.timeMillis }
            return thread.count == part.count &&
                (sameRank == null || sameRank.first().text == message.text) &&
                message.timeMillis - latest <= NEW_REQUEST_AFTER.inWholeMilliseconds
        }
    }
}
