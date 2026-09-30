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
package org.meshtastic.feature.messaging.ui.contact

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChannelTabsTest {

    @Test
    fun allTabShowsEveryConversation() {
        listOf("0^all", "1^all", "0!a1b2c3d4", "8!a1b2c3d4", "~tok^all", "!a1b2c3d4").forEach {
            assertTrue(isInChannelTab(it, null), it)
        }
    }

    @Test
    fun channelTabShowsItsChannelAndDirectMessagesOnThatSlot() {
        assertTrue(isInChannelTab("1^all", 1))
        assertTrue(isInChannelTab("1!a1b2c3d4", 1))
        assertFalse(isInChannelTab("0^all", 1))
        assertFalse(isInChannelTab("0!a1b2c3d4", 1))
    }

    @Test
    fun retiredAndUnprefixedConversationsOnlyShowUnderAll() {
        assertFalse(isInChannelTab("~tok^all", 0))
        assertFalse(isInChannelTab("!a1b2c3d4", 0))
    }
}
