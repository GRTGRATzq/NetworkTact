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
package org.meshtastic.feature.messaging.priority

import kotlin.test.Test
import kotlin.test.assertEquals

class MessagePriorityTest {

    @Test
    fun urgentPrefixAtStartIsUrgent() {
        assertEquals(MessagePriority.URGENT, MessagePriority.of("[URG] ALPHA-1 demande appui"))
    }

    @Test
    fun reportPrefixAtStartIsReport() {
        assertEquals(MessagePriority.REPORT, MessagePriority.of("[CR] BRAVO-2 en position"))
    }

    @Test
    fun textWithoutPrefixIsInfo() {
        assertEquals(MessagePriority.INFO, MessagePriority.of("PC-0 : point de situation à 14h"))
        assertEquals(MessagePriority.INFO, MessagePriority.of(""))
        assertEquals(MessagePriority.INFO, MessagePriority.of("   "))
    }

    @Test
    fun leadingWhitespaceIsIgnored() {
        assertEquals(MessagePriority.URGENT, MessagePriority.of("   [URG] ALPHA-1"))
        assertEquals(MessagePriority.REPORT, MessagePriority.of("\n\t[CR] BRAVO-2"))
    }

    @Test
    fun prefixIsCaseInsensitive() {
        assertEquals(MessagePriority.URGENT, MessagePriority.of("[urg] ALPHA-1"))
        assertEquals(MessagePriority.URGENT, MessagePriority.of("[Urg] ALPHA-1"))
        assertEquals(MessagePriority.REPORT, MessagePriority.of("[cr] BRAVO-2"))
    }

    @Test
    fun prefixWithoutFollowingSpaceStillCounts() {
        assertEquals(MessagePriority.URGENT, MessagePriority.of("[URG]ALPHA-1"))
        assertEquals(MessagePriority.URGENT, MessagePriority.of("[URG]"))
    }

    @Test
    fun prefixInTheMiddleDoesNotCount() {
        assertEquals(MessagePriority.INFO, MessagePriority.of("ALPHA-1 [URG] demande appui"))
        assertEquals(MessagePriority.INFO, MessagePriority.of("Reçu le [CR] de BRAVO-2"))
        assertEquals(MessagePriority.INFO, MessagePriority.of("x[URG]"))
    }

    @Test
    fun malformedTagsDoNotCount() {
        assertEquals(MessagePriority.INFO, MessagePriority.of("[ URG ] ALPHA-1"))
        assertEquals(MessagePriority.INFO, MessagePriority.of("URG ALPHA-1"))
        assertEquals(MessagePriority.INFO, MessagePriority.of("[URGENT] ALPHA-1"))
        assertEquals(MessagePriority.INFO, MessagePriority.of("(CR) BRAVO-2"))
    }

    @Test
    fun withPriorityPrependsPrefixAndOneSpace() {
        assertEquals(
            "[URG] ALPHA-1 demande appui",
            MessagePriority.withPriority("ALPHA-1 demande appui", MessagePriority.URGENT),
        )
        assertEquals(
            "[CR] BRAVO-2 en position",
            MessagePriority.withPriority("BRAVO-2 en position", MessagePriority.REPORT),
        )
        assertEquals("[URG] ", MessagePriority.withPriority("", MessagePriority.URGENT))
    }

    @Test
    fun withPriorityReplacesAnExistingPrefix() {
        assertEquals("[CR] ALPHA-1", MessagePriority.withPriority("[URG] ALPHA-1", MessagePriority.REPORT))
        assertEquals("[URG] BRAVO-2", MessagePriority.withPriority("  [cr]   BRAVO-2", MessagePriority.URGENT))
    }

    @Test
    fun withPriorityInfoRemovesThePrefix() {
        assertEquals("ALPHA-1", MessagePriority.withPriority("[URG] ALPHA-1", MessagePriority.INFO))
        assertEquals("PC-0 reçu", MessagePriority.withPriority("PC-0 reçu", MessagePriority.INFO))
    }

    @Test
    fun withPriorityLeavesMidTextTagsAlone() {
        assertEquals(
            "[URG] voir [CR] précédent",
            MessagePriority.withPriority("voir [CR] précédent", MessagePriority.URGENT),
        )
    }

    @Test
    fun withPriorityIsIdempotent() {
        val once = MessagePriority.withPriority("ALPHA-1", MessagePriority.URGENT)
        assertEquals(once, MessagePriority.withPriority(once, MessagePriority.URGENT))
    }
}
