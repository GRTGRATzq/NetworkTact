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

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.meshtastic.core.model.MessageStatus
import org.meshtastic.core.repository.OutgoingOutcome
import org.meshtastic.proto.Routing
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** « Envoi trop rapide » (D12): the parts go one at a time, and a part refused for that is sent again. */
class FireRequestSenderTest {

    private val tooFast = Routing.Error.RATE_LIMIT_EXCEEDED.value

    /** A radio where each packet id (1, 2, 3… in sending order) has the outcome [script] gives it. */
    private class Radio(val script: (Int) -> OutgoingOutcome?) {
        val sent = mutableListOf<String>()
        val sentAt = mutableListOf<Long>()
        val discarded = mutableListOf<Long>()
    }

    private fun outcome(id: Int, status: MessageStatus, error: Int = -1) = OutgoingOutcome(id * 100L, status, error)

    @Test
    fun `parts accepted by the radio go in order, spaced by the minimum`() = runTest {
        val radio = Radio { outcome(it, MessageStatus.ENROUTE) }
        val sender = sender(radio)

        sender.sendAll(listOf("a", "b", "c"))

        assertEquals(listOf("a", "b", "c"), radio.sent)
        assertTrue(radio.sentAt[1] - radio.sentAt[0] >= 2_000)
        assertTrue(radio.sentAt[2] - radio.sentAt[1] >= 2_000)
        assertTrue(radio.discarded.isEmpty())
        assertNull(sender.progress.value)
    }

    @Test
    fun `the next part waits until the previous one has left the queue`() = runTest {
        var polls = 0
        val radio = Radio { id ->
            if (id == 1 && polls++ < 40) outcome(id, MessageStatus.QUEUED) else outcome(id, MessageStatus.ENROUTE)
        }
        val sender = sender(radio)

        sender.sendAll(listOf("a", "b"))

        assertTrue(radio.sentAt[1] - radio.sentAt[0] >= 40 * 250L, "second part left after ${radio.sentAt}")
    }

    @Test
    fun `a part refused as too fast is deleted and sent again after a wait`() = runTest {
        // Ids: 1 = "a", 2 = "b" refused, 3 = "b" again, 4 = "c".
        val radio = Radio { id ->
            if (id == 2) outcome(id, MessageStatus.ERROR, tooFast) else outcome(id, MessageStatus.DELIVERED)
        }
        val sender = sender(radio)

        sender.sendAll(listOf("a", "b", "c"))

        assertEquals(listOf("a", "b", "b", "c"), radio.sent)
        assertEquals(listOf(200L), radio.discarded)
        assertTrue(radio.sentAt[2] - radio.sentAt[1] >= 5_000, "first wait is 5 s")
        // After a refusal the parts that follow are spaced as widely.
        assertTrue(radio.sentAt[3] - radio.sentAt[2] >= 5_000)
    }

    @Test
    fun `after three refused tries the part stays failed for the manual resend`() = runTest {
        // "a" is refused every time: ids 1 to 4. "b" is id 5.
        val radio = Radio { id ->
            if (id <= 4) outcome(id, MessageStatus.ERROR, tooFast) else outcome(id, MessageStatus.DELIVERED)
        }
        val sender = sender(radio)

        sender.sendAll(listOf("a", "b"))

        assertEquals(listOf("a", "a", "a", "a", "b"), radio.sent)
        // The first three failures are deleted as they are replaced, the last stays for « Renvoyer ».
        assertEquals(listOf(100L, 200L, 300L), radio.discarded)
        // The waits grow: 5, 10 and 20 seconds.
        assertTrue(radio.sentAt[1] - radio.sentAt[0] >= 5_000)
        assertTrue(radio.sentAt[2] - radio.sentAt[1] >= 10_000)
        assertTrue(radio.sentAt[3] - radio.sentAt[2] >= 20_000)
    }

    @Test
    fun `another failure is not retried`() = runTest {
        val radio = Radio { id ->
            if (id == 1) {
                outcome(id, MessageStatus.ERROR, Routing.Error.MAX_RETRANSMIT.value)
            } else {
                outcome(id, MessageStatus.DELIVERED)
            }
        }
        val sender = sender(radio)

        sender.sendAll(listOf("a", "b"))

        assertEquals(listOf("a", "b"), radio.sent)
        assertTrue(radio.discarded.isEmpty())
    }

    @Test
    fun `nothing held for the packet, as in demo mode, does not wait`() = runTest {
        val radio = Radio { null }
        val sender = sender(radio)

        sender.sendAll(listOf("a", "b", "c"))

        assertEquals(listOf("a", "b", "c"), radio.sent)
        assertEquals(0L, currentTime)
    }

    @Test
    fun `progress tells the part being sent and the retry being waited for`() = runTest {
        val seen = mutableListOf<FireRequestProgress?>()
        val radio = Radio { id ->
            if (id == 1) outcome(id, MessageStatus.ERROR, tooFast) else outcome(id, MessageStatus.DELIVERED)
        }
        lateinit var sender: FireRequestSender
        sender = sender(radio) { seen += sender.progress.value }

        sender.sendAll(listOf("a"))

        assertEquals(FireRequestProgress(part = 1, count = 1, retry = 0), seen.first())
        assertEquals(FireRequestProgress(part = 1, count = 1, retry = 1), seen.last())
        assertNull(sender.progress.value)
    }

    private fun TestScope.sender(radio: Radio, onSend: () -> Unit = {}): FireRequestSender {
        var nextId = 0
        return FireRequestSender(
            send = { text ->
                radio.sent += text
                radio.sentAt += currentTime
                onSend()
                ++nextId
            },
            outcome = { radio.script(it) },
            discard = { radio.discarded += it },
        )
    }
}
