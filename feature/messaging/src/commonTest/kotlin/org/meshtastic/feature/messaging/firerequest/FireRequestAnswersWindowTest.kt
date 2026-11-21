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
import org.meshtastic.core.model.MessageStatus
import org.meshtastic.core.testing.TestDataFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

/** The direct answers of a channel's requests are read from the oldest request on, 7 days back at most. */
class FireRequestAnswersWindowTest {

    private val node = TestDataFactory.createTestNode(num = 2, userId = "!00000002", longName = "OA")
    private val now = 1_800_000_000_000L

    private fun message(uuid: Long, text: String, receivedTime: Long) = Message(
        uuid = uuid,
        receivedTime = receivedTime,
        node = node,
        text = text,
        fromLocal = true,
        time = "22:50",
        read = true,
        status = MessageStatus.RECEIVED,
        routingError = 0,
        packetId = uuid.toInt(),
        emojis = emptyList(),
        snr = null,
        rssi = null,
        hopsAway = 0,
        replyId = null,
    )

    @Test
    fun the_window_starts_at_the_first_part_of_the_oldest_request() {
        val old = now - 30.hours.inWholeMilliseconds
        val messages =
            listOf(
                message(1, "bonjour", now - 2.days.inWholeMilliseconds),
                message(2, "[DT AB1234 1/2]\n1 AB1234", old),
                message(3, "[DT AB1234 2/2]\n9 non rempli", old + 1_000),
                message(4, "[DT CD5678 1/1]\n1 CD5678", now - 1.hours.inWholeMilliseconds),
                // An answer is not a part: it does not move the window.
                message(5, "[DT ZZ0001] Accusé DT 112251 loc NOV 26", now - 3.days.inWholeMilliseconds),
            )
        val first = FireRequestView.firstPartTime(messages)
        assertEquals(old, first)
        // Older than 12 h: the answers of that request are still read.
        assertEquals(old, FireRequestView.answersSince(old, now))
    }

    @Test
    fun seven_days_back_at_most_and_nothing_without_a_request() {
        val tenDays = now - 10.days.inWholeMilliseconds
        assertEquals(now - 7.days.inWholeMilliseconds, FireRequestView.answersSince(tenDays, now))
        assertNull(FireRequestView.firstPartTime(listOf(message(1, "bonjour", now))))
    }
}
