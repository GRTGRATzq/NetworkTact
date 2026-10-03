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
package org.meshtastic.feature.node.commandpost

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import org.meshtastic.core.model.freshness.ContactState
import org.meshtastic.core.model.freshness.PositionState
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.command_post_contact_never
import org.meshtastic.core.resources.command_post_contact_recent
import org.meshtastic.core.resources.command_post_duration_minutes
import org.meshtastic.core.resources.command_post_empty
import org.meshtastic.core.resources.command_post_position_fix_unknown
import org.meshtastic.core.resources.command_post_position_fresh
import org.meshtastic.core.resources.command_post_position_no_time
import org.meshtastic.core.resources.command_post_position_none
import org.meshtastic.core.resources.command_post_position_stale
import org.meshtastic.core.resources.command_post_position_stale_at_least
import org.meshtastic.core.resources.getString
import org.meshtastic.core.resources.message
import org.meshtastic.core.ui.theme.AppTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalTestApi::class)
class CommandPostContentTest {

    private fun row(
        num: Int,
        name: String,
        contact: ContactState = ContactState.SeenRecently(1.minutes),
        position: PositionState = PositionState.NoPosition,
        messageKey: String? = null,
    ) = CommandPostRow(
        num = num,
        shortName = name,
        longName = name,
        lastHeard = 0,
        contact = contact,
        position = position,
        latitude = null,
        longitude = null,
        directMessageKey = messageKey,
    )

    private fun minutesText(duration: Duration) =
        getString(Res.string.command_post_duration_minutes, duration.inWholeMinutes.toString())

    private fun ComposeUiTest.show(
        rows: List<CommandPostRow>,
        onOpenNode: (Int) -> Unit = {},
        onOpenMessages: (String) -> Unit = {},
    ) = setContent {
        AppTheme { CommandPostContent(rows = rows, onOpenNode = onOpenNode, onOpenMessages = onOpenMessages) }
    }

    @Test
    fun emptyDatabaseSaysSo() = runComposeUiTest {
        show(emptyList())
        onNodeWithText(getString(Res.string.command_post_empty)).assertExists()
    }

    // Three rows per test so every card fits in the test window: LazyColumn only composes what is visible.
    @Test
    fun positionsWithAFixTimeShowTheirAge() = runComposeUiTest {
        show(
            listOf(
                row(1, "ALPHA-1", position = PositionState.Fresh(3.minutes)),
                row(2, "BRAVO-2", position = PositionState.Stale(25.minutes)),
                row(3, "CHARLIE-3", contact = ContactState.NeverHeard),
            ),
        )

        onNodeWithText(getString(Res.string.command_post_position_fresh, minutesText(3.minutes))).assertExists()
        onNodeWithText(getString(Res.string.command_post_position_stale, minutesText(25.minutes))).assertExists()
        onNodeWithText(getString(Res.string.command_post_position_none)).assertExists()
        onNodeWithText(getString(Res.string.command_post_contact_never)).assertExists()
    }

    @Test
    fun positionsWithoutAFixTimeAreNeverFresh() = runComposeUiTest {
        show(
            listOf(
                row(4, "DELTA-4", position = PositionState.StaleAtLeast(12.minutes)),
                row(5, "ECHO-5", position = PositionState.ReceivedFixTimeUnknown(4.minutes)),
                row(6, "FOXTROT-6", position = PositionState.NoFixTime),
            ),
        )

        onNodeWithText(getString(Res.string.command_post_position_stale_at_least, minutesText(12.minutes)))
            .assertExists()
        onNodeWithText(getString(Res.string.command_post_position_fix_unknown, minutesText(4.minutes))).assertExists()
        onNodeWithText(getString(Res.string.command_post_position_no_time)).assertExists()
        onNodeWithText(getString(Res.string.command_post_position_fresh, minutesText(4.minutes))).assertDoesNotExist()
        onAllNodesWithText(getString(Res.string.command_post_contact_recent, minutesText(1.minutes)))
            .assertCountEquals(3)
    }

    @Test
    fun tappingARowOpensTheNode() = runComposeUiTest {
        var opened: Int? = null
        show(listOf(row(7, "PC-0")), onOpenNode = { opened = it })

        onNodeWithTag("command_post_row_7").performClick()
        runOnIdle { assertEquals(7, opened) }
    }

    @Test
    fun messageButtonOpensTheDirectConversation() = runComposeUiTest {
        var openedKey: String? = null
        show(listOf(row(1, "ALPHA-1", messageKey = "8!00000001")), onOpenMessages = { openedKey = it })

        onNodeWithText(getString(Res.string.message)).performClick()
        runOnIdle { assertEquals("8!00000001", openedKey) }
    }

    @Test
    fun noMessageButtonWithoutAKey() = runComposeUiTest {
        show(listOf(row(2, "BRAVO-2")))
        onNodeWithText(getString(Res.string.message)).assertDoesNotExist()
    }
}
