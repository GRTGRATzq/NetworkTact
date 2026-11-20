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

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.meshtastic.core.model.MessageStatus
import org.meshtastic.core.repository.OutgoingOutcome
import org.meshtastic.proto.Routing
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** Where the sending of a request stands: [part] of [count], and, after a refusal, the [retry] being waited for. */
data class FireRequestProgress(val part: Int, val count: Int, val retry: Int = 0, val waiting: Boolean = false)

/** The pauses of [FireRequestSender]. The radio's own limit is not read anywhere: these are cautious, and grow. */
data class FireRequestSpacing(
    /** Least time between the end of one part and the next, whatever the radio said. */
    val minimum: Duration = 2.seconds,
    /** How long a part may stay in the phone's queue before the next one goes anyway. */
    val leaveQueueTimeout: Duration = 30.seconds,
    /** Waits before each new try of a part the radio refused as « too fast »: three tries at most. */
    val backoffs: List<Duration> = listOf(5.seconds, 10.seconds, 20.seconds),
    val poll: Duration = 250.milliseconds,
)

/**
 * Sends the parts of a fire request one after the other. The next part goes only once the previous one has left the
 * phone's queue (the radio took it), after at least [FireRequestSpacing.minimum]. A part the radio refuses as «
 * RATE_LIMIT_EXCEEDED » (« envoi trop rapide ») is deleted and sent again after a growing wait, three times at most,
 * and the parts after it are spaced as widely; then it stays failed, with « Renvoyer » as for any other failure. Other
 * failures are not retried.
 *
 * [outcome] returning null (nothing held for that packet: the demo) ends the wait at once.
 */
class FireRequestSender(
    private val send: suspend (String) -> Int,
    private val outcome: suspend (Int) -> OutgoingOutcome?,
    private val discard: suspend (Long) -> Unit,
    private val spacing: FireRequestSpacing = FireRequestSpacing(),
) {
    private val _progress = MutableStateFlow<FireRequestProgress?>(null)

    /** The part being sent; null when nothing is being sent. */
    val progress: StateFlow<FireRequestProgress?> = _progress.asStateFlow()

    /** Sends [parts] in order. */
    suspend fun sendAll(parts: List<String>) {
        try {
            var gap = spacing.minimum
            parts.forEachIndexed { index, text ->
                val refused = sendOne(text, index + 1, parts.size, gap)
                if (refused > 0) {
                    gap = maxOf(gap, spacing.backoffs[(refused - 1).coerceAtMost(spacing.backoffs.lastIndex)])
                }
            }
        } finally {
            _progress.value = null
        }
    }

    /** Sends one part, again after each refusal for sending too fast; returns how many times it was refused. */
    private suspend fun sendOne(text: String, part: Int, count: Int, gap: Duration): Int {
        var retry = 0
        while (true) {
            _progress.value = FireRequestProgress(part, count, retry)
            // After a refusal the part sent again is followed by as wide a pause as the wait that preceded it.
            val pause = if (retry > 0) maxOf(gap, spacing.backoffs[retry - 1]) else gap
            val last = settle(send(text), pause)
            val tooFast = last?.takeIf { it.isTooFast() }
            if (tooFast == null || retry >= spacing.backoffs.size) return retry
            discard(tooFast.uuid)
            _progress.value = FireRequestProgress(part, count, retry + 1, waiting = true)
            delay(spacing.backoffs[retry])
            retry++
        }
    }

    /**
     * Waits for the packet to leave the phone's queue, then for [gap], or less when the radio refuses it meanwhile (an
     * acknowledgement does not shorten it: the limit is the radio's own). Returns the last outcome seen.
     */
    private suspend fun settle(packetId: Int, gap: Duration): OutgoingOutcome? {
        // Null (nothing held for the packet) ends both waits at once.
        var last = outcome(packetId)
        var waited = Duration.ZERO
        while (last?.status == MessageStatus.QUEUED && waited < spacing.leaveQueueTimeout) {
            delay(spacing.poll)
            waited += spacing.poll
            last = outcome(packetId)
        }
        var dwelt = Duration.ZERO
        while (last != null && last.status != MessageStatus.ERROR && dwelt < gap) {
            delay(spacing.poll)
            dwelt += spacing.poll
            last = outcome(packetId)
        }
        return last
    }

    private fun OutgoingOutcome.isTooFast(): Boolean =
        status == MessageStatus.ERROR && routingError == Routing.Error.RATE_LIMIT_EXCEEDED.value
}
