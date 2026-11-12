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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FireRequestNotificationTest {

    private val parts = FireRequestExamples.specificationParts
    private val minute = 60_000L

    private fun received(id: Long, text: String, at: Long = id * minute, from: String = "!oa") =
        FireRequestMessage(id, fromLocal = false, sender = from, timeMillis = at, text = text)

    private fun sent(id: Long, text: String) =
        FireRequestMessage(id, fromLocal = true, sender = "!me", timeMillis = id * minute, text = text)

    private val allocation = "[DT AB1234] GDH attribution 112252 loc NOV 26"

    @Test
    fun `the parts received make one line at the latest, counting the parts here`() {
        val messages = listOf(received(1, "Bonjour"), received(2, parts[0]), received(3, parts[2]))

        val notification = FireRequestNotification.of(messages)

        assertEquals(setOf(2L), notification.hidden)
        assertEquals(
            mapOf(3L to FireRequestNotificationLine.Request("AB1234", received = 2, count = 3, fromLocal = false)),
            notification.lines,
        )
    }

    @Test
    fun `the line is updated when the last part comes, in any order`() {
        val messages = listOf(received(1, parts[2]), received(2, parts[0]), received(3, parts[1]))

        val notification = FireRequestNotification.of(messages)

        assertEquals(setOf(1L, 2L), notification.hidden)
        assertEquals(FireRequestNotificationLine.Request("AB1234", 3, 3, false), notification.lines[3L])
    }

    @Test
    fun `a request sent reads as the sender's card and the date-time group sent back in one line`() {
        val messages = parts.mapIndexed { i, part -> sent(i + 1L, part) } + received(4, allocation)

        val notification = FireRequestNotification.of(messages)

        assertEquals(setOf(1L, 2L), notification.hidden)
        assertEquals(FireRequestNotificationLine.Request("AB1234", 3, 3, fromLocal = true), notification.lines[3L])
        val reply = notification.lines[4L] as FireRequestNotificationLine.Reply
        assertEquals("DT AB1234 · GDH attribution 112252 loc NOV 26", reply.text)
    }

    @Test
    fun `a date-time group with no request still reads in one line`() {
        val notification = FireRequestNotification.of(listOf(received(1, "[DT XY9] GDH fin 112310Z NOV 26")))

        assertEquals(emptySet(), notification.hidden)
        assertEquals(
            "DT XY9 · GDH fin 112310Z NOV 26",
            (notification.lines[1L] as FireRequestNotificationLine.Reply).text,
        )
    }

    @Test
    fun `other messages are left as they are`() {
        val notification =
            FireRequestNotification.of(listOf(received(1, "[URG] Blessé léger"), received(2, "texte [DT AB 1/3]")))

        assertEquals(FireRequestNotification(emptySet(), emptyMap()), notification)
    }

    @Test
    fun `only the first part of a request alerts`() {
        val messages = listOf(received(1, parts[1]), received(2, parts[0]), received(3, parts[2]))

        assertFalse(FireRequestNotification.isFollowUp(messages.take(1), 1))
        assertTrue(FireRequestNotification.isFollowUp(messages.take(2), 2))
        assertTrue(FireRequestNotification.isFollowUp(messages, 3))
    }

    @Test
    fun `a part received twice does not alert again`() {
        val messages = listOf(received(1, parts[0]), received(2, parts[0]))

        assertTrue(FireRequestNotification.isFollowUp(messages, 2))
    }

    @Test
    fun `a new request with the same number alerts again`() {
        val later = FireRequestAssembly.NEW_REQUEST_AFTER.inWholeMilliseconds + 10 * minute
        val messages = listOf(received(1, parts[0]), received(2, parts[0], at = later))

        assertFalse(FireRequestNotification.isFollowUp(messages, 2))
    }

    @Test
    fun `another sender, a date-time group or an ordinary message always alerts`() {
        val messages =
            listOf(
                received(1, parts[0]),
                received(2, parts[1], from = "!other"),
                received(3, allocation),
                received(4, "Bien reçu"),
            )

        assertFalse(FireRequestNotification.isFollowUp(messages, 2))
        assertFalse(FireRequestNotification.isFollowUp(messages, 3))
        assertFalse(FireRequestNotification.isFollowUp(messages, 4))
        assertFalse(FireRequestNotification.isFollowUp(messages, 99))
    }
}
