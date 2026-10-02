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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ThreadFilterTest {

    private val relayed = SentStatus.RelayedByMesh
    private val acked = SentStatus.AckedByRecipient(verified = false)

    @Test
    fun inactiveFilterShowsEverything() {
        val filter = ThreadFilter()
        assertFalse(filter.isActive)
        assertTrue(
            filter.matches("PC-0 : RAS", fromLocal = false, sentStatus = SentStatus.Unknown, isDirectMessage = false),
        )
        assertTrue(filter.matches("[URG] ALPHA-1", fromLocal = true, sentStatus = relayed, isDirectMessage = true))
    }

    @Test
    fun priorityFilterKeepsOnlySelectedPriorities() {
        val filter = ThreadFilter(priorities = setOf(MessagePriority.URGENT, MessagePriority.REPORT))
        assertTrue(filter.matches("[URG] ALPHA-1", false, SentStatus.Unknown, false))
        assertTrue(filter.matches(" [cr] BRAVO-2", false, SentStatus.Unknown, false))
        assertFalse(filter.matches("PC-0 : RAS", false, SentStatus.Unknown, false))
        assertFalse(filter.matches("PC-0 : voir [URG] plus haut", false, SentStatus.Unknown, false))
    }

    @Test
    fun infoFilterKeepsUnprefixedMessages() {
        val filter = ThreadFilter(priorities = setOf(MessagePriority.INFO))
        assertTrue(filter.matches("PC-0 : RAS", false, SentStatus.Unknown, false))
        assertFalse(filter.matches("[URG] ALPHA-1", false, SentStatus.Unknown, false))
    }

    @Test
    fun unackedKeepsSentDirectMessagesWithoutRecipientAck() {
        val filter = ThreadFilter(unackedOnly = true)
        assertTrue(filter.matches("ALPHA-1", fromLocal = true, sentStatus = relayed, isDirectMessage = true))
        assertTrue(
            filter.matches("ALPHA-1", fromLocal = true, sentStatus = SentStatus.Failed(0), isDirectMessage = true),
        )
        assertFalse(filter.matches("ALPHA-1", fromLocal = true, sentStatus = acked, isDirectMessage = true))
        assertFalse(
            filter.matches("BRAVO-2", fromLocal = false, sentStatus = SentStatus.Unknown, isDirectMessage = true),
        )
    }

    @Test
    fun unackedMatchesNothingInAChannel() {
        val filter = ThreadFilter(unackedOnly = true)
        assertFalse(filter.matches("ALPHA-1", fromLocal = true, sentStatus = relayed, isDirectMessage = false))
    }

    @Test
    fun filtersCombine() {
        val filter = ThreadFilter(priorities = setOf(MessagePriority.URGENT), unackedOnly = true)
        assertTrue(filter.matches("[URG] ALPHA-1", true, relayed, true))
        assertFalse(filter.matches("ALPHA-1", true, relayed, true))
        assertFalse(filter.matches("[URG] ALPHA-1", true, acked, true))
    }

    @Test
    fun togglePriorityAddsThenRemoves() {
        val on = ThreadFilter().togglePriority(MessagePriority.REPORT)
        assertEquals(setOf(MessagePriority.REPORT), on.priorities)
        assertEquals(emptySet(), on.togglePriority(MessagePriority.REPORT).priorities)
    }

    @Test
    fun teamFilterKeepsOnlyThatTeamsSenders() {
        val filter = ThreadFilter(team = "Alpha")
        assertTrue(filter.isActive)
        assertTrue(filter.matches("info", false, SentStatus.Unknown, false, senderLongName = "ALPHA-1 [Alpha]"))
        assertTrue(filter.matches("info", false, SentStatus.Unknown, false, senderLongName = "ALPHA-2 [alpha] (MQTT)"))
        assertFalse(filter.matches("info", false, SentStatus.Unknown, false, senderLongName = "BRAVO-2 [Bravo]"))
        assertFalse(filter.matches("info", false, SentStatus.Unknown, false, senderLongName = "CHARLIE-3"))
    }

    @Test
    fun teamFilterCombinesWithPriority() {
        val filter = ThreadFilter(priorities = setOf(MessagePriority.URGENT), team = "Alpha")
        assertTrue(filter.matches("[URG] contact", false, SentStatus.Unknown, false, "ALPHA-1 [Alpha]"))
        assertFalse(filter.matches("contact", false, SentStatus.Unknown, false, "ALPHA-1 [Alpha]"))
        assertFalse(filter.matches("[URG] contact", false, SentStatus.Unknown, false, "BRAVO-2 [Bravo]"))
    }

    @Test
    fun toggleTeamSelectsThenClears() {
        val on = ThreadFilter().toggleTeam("Alpha")
        assertEquals("Alpha", on.team)
        assertEquals("Bravo", on.toggleTeam("Bravo").team)
        assertEquals(null, on.toggleTeam("ALPHA").team)
    }
}
