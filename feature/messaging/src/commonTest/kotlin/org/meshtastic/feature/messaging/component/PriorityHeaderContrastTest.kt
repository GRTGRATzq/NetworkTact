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
package org.meshtastic.feature.messaging.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import org.meshtastic.core.ui.theme.AppTheme
import org.meshtastic.core.ui.theme.MIN_TEXT_CONTRAST
import org.meshtastic.core.ui.theme.contrastRatio
import org.meshtastic.feature.messaging.priority.MessagePriority
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The header colours are measured on the very background a card paints, in the app's static light and dark schemes. */
@OptIn(ExperimentalTestApi::class)
class PriorityHeaderContrastTest {

    private data class Measured(val priority: MessagePriority, val text: Color, val background: Color)

    private fun ComposeUiTest.measure(darkTheme: Boolean): Map<MessagePriority, Measured> {
        val measured = mutableMapOf<MessagePriority, Measured>()
        setContent {
            AppTheme(darkTheme = darkTheme, dynamicColor = false) {
                MessagePriority.entries.forEach { priority ->
                    val background = priorityCardBackground(priority)
                    measured[priority] = Measured(priority, priorityHeaderColor(priority, background), background)
                }
            }
        }
        waitForIdle()
        assertEquals(MessagePriority.entries.toSet(), measured.keys)
        return measured
    }

    private fun ComposeUiTest.assertLegible(darkTheme: Boolean) {
        measure(darkTheme).values.forEach { (priority, text, background) ->
            val ratio = contrastRatio(text, background)
            assertTrue(ratio >= MIN_TEXT_CONTRAST, "$priority header reads $ratio:1 (dark=$darkTheme)")
        }
    }

    private fun ComposeUiTest.assertOwnShades(darkTheme: Boolean) {
        val byPriority = measure(darkTheme)
        assertEquals(
            priorityHeaderShade(MessagePriority.URGENT, darkSurface = darkTheme),
            byPriority.getValue(MessagePriority.URGENT).text,
        )
        assertEquals(
            priorityHeaderShade(MessagePriority.REPORT, darkSurface = darkTheme),
            byPriority.getValue(MessagePriority.REPORT).text,
        )
    }

    @Test fun headerColoursReadAtLeastAaInTheLightScheme() = runComposeUiTest { assertLegible(darkTheme = false) }

    @Test fun headerColoursReadAtLeastAaInTheDarkScheme() = runComposeUiTest { assertLegible(darkTheme = true) }

    @Test
    fun urgentAndReportKeepTheirOwnShadeInTheLightScheme() = runComposeUiTest { assertOwnShades(darkTheme = false) }

    @Test fun urgentAndReportKeepTheirOwnShadeInTheDarkScheme() = runComposeUiTest { assertOwnShades(darkTheme = true) }
}
