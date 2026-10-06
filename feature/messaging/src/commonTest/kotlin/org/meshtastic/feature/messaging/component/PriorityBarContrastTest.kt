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

import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import org.meshtastic.core.ui.theme.AppTheme
import org.meshtastic.core.ui.theme.MIN_GRAPHICAL_CONTRAST
import org.meshtastic.core.ui.theme.MIN_TEXT_CONTRAST
import org.meshtastic.core.ui.theme.TactColors
import org.meshtastic.core.ui.theme.TactHue
import org.meshtastic.core.ui.theme.contrastRatio
import org.meshtastic.feature.messaging.priority.MessagePriority
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The priority bar and the secondary text of a card, measured on the colours a card really paints: the app's static
 * light and dark schemes, then card backgrounds a dynamic system palette can produce.
 */
@OptIn(ExperimentalTestApi::class)
class PriorityBarContrastTest {

    private class Painted(
        val card: Color,
        val bars: Map<MessagePriority, Color>,
        val washes: Map<MessagePriority, Color>,
        val secondary: Color,
        val primary: Color,
    )

    private fun ComposeUiTest.paint(darkTheme: Boolean): Painted {
        var painted: Painted? = null
        setContent {
            AppTheme(darkTheme = darkTheme, dynamicColor = false) {
                val scheme = MaterialTheme.colorScheme
                painted =
                    Painted(
                        card = CardDefaults.cardColors().containerColor,
                        bars = MessagePriority.entries.associateWith { priorityAccent(it) },
                        washes = MessagePriority.entries.associateWith { priorityCardBackground(it) },
                        secondary = scheme.onSurfaceVariant,
                        primary = scheme.onSurface,
                    )
            }
        }
        waitForIdle()
        return checkNotNull(painted)
    }

    private fun ComposeUiTest.assertBarsReadOnTheCard(darkTheme: Boolean) {
        val painted = paint(darkTheme)
        listOf(MessagePriority.URGENT, MessagePriority.REPORT).forEach { priority ->
            val ratio = contrastRatio(painted.bars.getValue(priority), painted.card)
            assertTrue(ratio >= MIN_GRAPHICAL_CONTRAST, "$priority bar reads $ratio:1 (dark=$darkTheme)")
        }
        assertEquals(TactColors.tones(TactHue.URGENT, darkTheme).first(), painted.bars.getValue(MessagePriority.URGENT))
        assertEquals(TactColors.tones(TactHue.REPORT, darkTheme).first(), painted.bars.getValue(MessagePriority.REPORT))
    }

    private fun ComposeUiTest.assertSecondaryTextReads(darkTheme: Boolean) {
        val painted = paint(darkTheme)
        painted.washes.forEach { (priority, background) ->
            val secondary = legibleSecondary(painted.secondary, painted.primary, background)
            val ratio = contrastRatio(secondary, background)
            assertTrue(ratio >= MIN_TEXT_CONTRAST, "secondary text on $priority reads $ratio:1 (dark=$darkTheme)")
        }
    }

    @Test fun barsReadOnTheCardInTheLightScheme() = runComposeUiTest { assertBarsReadOnTheCard(darkTheme = false) }

    @Test fun barsReadOnTheCardInTheDarkScheme() = runComposeUiTest { assertBarsReadOnTheCard(darkTheme = true) }

    @Test
    fun secondaryTextReadsOnEveryCardInTheLightScheme() = runComposeUiTest {
        assertSecondaryTextReads(darkTheme = false)
    }

    @Test
    fun secondaryTextReadsOnEveryCardInTheDarkScheme() = runComposeUiTest { assertSecondaryTextReads(darkTheme = true) }

    @Test
    fun barsReadOnDynamicPaletteCards() {
        dynamicCards.forEach { card ->
            listOf(MessagePriority.URGENT, MessagePriority.REPORT).forEach { priority ->
                val bar = priorityAccent(priority, card, neutral = Color.Gray)
                val ratio = contrastRatio(bar, card)
                assertTrue(ratio >= MIN_GRAPHICAL_CONTRAST, "$priority bar reads $ratio:1 on $card")
                assertTrue(bar in allTones(priority.hue()), "$priority bar left its hue on $card")
            }
        }
    }

    @Test
    fun barFallsBackOnItsStrongestFixedToneWhenNoToneReads() {
        // A mid-grey card counts as dark, and no pale tone reaches 3:1 on it: the most contrasted fixed tone of the
        // same
        // hue, here a deep one of the light theme, wins.
        val card = Color(0xFF888888)
        listOf(MessagePriority.URGENT, MessagePriority.REPORT).forEach { priority ->
            val hue = priority.hue()
            TactColors.tones(hue, dark = true).forEach { assertTrue(contrastRatio(it, card) < MIN_GRAPHICAL_CONTRAST) }
            val strongest = allTones(hue).maxBy { contrastRatio(it, card) }
            val bar = priorityAccent(priority, card, neutral = Color.Gray)
            assertEquals(strongest, bar)
            assertTrue(contrastRatio(bar, card) >= MIN_GRAPHICAL_CONTRAST, "$priority fallback reads too low")
        }
    }

    @Test
    fun infoBarStaysNeutral() {
        assertEquals(Color.Gray, priorityAccent(MessagePriority.INFO, Color.White, neutral = Color.Gray))
    }

    @Test
    fun secondaryTextFallsBackOnTheMainTextColour() {
        val background = Color(0xFFD0BDC5)
        val secondary = Color(0xFF5C5E78)
        val primary = Color(0xFF2C2D3C)
        assertEquals(primary, legibleSecondary(secondary, primary, background))
        assertEquals(secondary, legibleSecondary(secondary, primary, Color.White))
    }

    private fun MessagePriority.hue(): TactHue = if (this == MessagePriority.URGENT) TactHue.URGENT else TactHue.REPORT

    private fun allTones(hue: TactHue): List<Color> =
        TactColors.tones(hue, dark = false) + TactColors.tones(hue, dark = true)

    private companion object {
        /** Card colours (surfaceContainerHighest) of Material You palettes from several wallpapers, light and dark. */
        val dynamicCards =
            listOf(
                Color(0xFFE6E0E9),
                Color(0xFFF0DEDD),
                Color(0xFFDDE5DA),
                Color(0xFFDCE3EE),
                Color(0xFFEDE1CF),
                Color(0xFF36343B),
                Color(0xFF3B3130),
                Color(0xFF323630),
                Color(0xFF2F3540),
                Color(0xFF3A342B),
            )
    }
}
