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

import androidx.compose.ui.graphics.Color
import org.meshtastic.core.model.freshness.ContactState
import org.meshtastic.core.model.freshness.PositionState
import org.meshtastic.core.ui.theme.MIN_TEXT_CONTRAST
import org.meshtastic.core.ui.theme.TactColors
import org.meshtastic.core.ui.theme.TactHue
import org.meshtastic.core.ui.theme.contrastRatio
import org.meshtastic.core.ui.theme.surfaceContainerHighestDark
import org.meshtastic.core.ui.theme.surfaceContainerHighestLight
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

class FreshnessToneTest {

    @Test
    fun onlyAFreshFixTimeIsGreen() {
        assertEquals(FreshnessTone.FRESH, PositionState.Fresh(2.minutes).tone())
        assertEquals(FreshnessTone.NEUTRAL, PositionState.ReceivedFixTimeUnknown(2.minutes).tone())
        assertEquals(FreshnessTone.NEUTRAL, PositionState.NoFixTime.tone())
        assertEquals(FreshnessTone.NEUTRAL, PositionState.NoPosition.tone())
    }

    @Test
    fun oldOrInconsistentPositionsAreAlerts() {
        assertEquals(FreshnessTone.ALERT, PositionState.Stale(25.minutes).tone())
        assertEquals(FreshnessTone.ALERT, PositionState.StaleAtLeast(25.minutes).tone())
        assertEquals(FreshnessTone.ALERT, PositionState.InconsistentTimestamp(5.minutes).tone())
    }

    @Test
    fun contactTones() {
        assertEquals(FreshnessTone.NORMAL, ContactState.SeenRecently(3.minutes).tone())
        assertEquals(FreshnessTone.NEUTRAL, ContactState.NotHeardSince(40.minutes).tone())
        assertEquals(FreshnessTone.NEUTRAL, ContactState.NeverHeard.tone())
        assertEquals(FreshnessTone.ALERT, ContactState.InconsistentTimestamp(5.minutes).tone())
    }

    @Test
    fun freshGreenReadsOnTheStaticCards() {
        listOf(surfaceContainerHighestLight to false, surfaceContainerHighestDark to true).forEach { (card, dark) ->
            val green = freshColor(card)
            assertEquals(TactColors.tones(TactHue.FRESH, dark).first(), green)
            assertTrue(contrastRatio(green, card) >= MIN_TEXT_CONTRAST, "fresh reads too low (dark=$dark)")
        }
    }

    @Test
    fun freshGreenReadsOnDynamicPaletteCards() {
        val allGreens = TactColors.tones(TactHue.FRESH, dark = false) + TactColors.tones(TactHue.FRESH, dark = true)
        dynamicCards.forEach { card ->
            val green = freshColor(card)
            val ratio = contrastRatio(green, card)
            assertTrue(ratio >= MIN_TEXT_CONTRAST, "fresh reads $ratio:1 on $card")
            assertTrue(green in allGreens, "fresh left its hue on $card")
        }
    }

    @Test
    fun freshGreenFallsBackOnItsStrongestFixedTone() {
        // A mid-grey card counts as dark, and no pale green reaches 4.5:1 on it: the deepest green of the light theme
        // wins.
        val card = Color(0xFF888888)
        val allGreens = TactColors.tones(TactHue.FRESH, dark = false) + TactColors.tones(TactHue.FRESH, dark = true)
        TactColors.tones(TactHue.FRESH, dark = true).forEach { assertTrue(contrastRatio(it, card) < MIN_TEXT_CONTRAST) }
        assertEquals(allGreens.maxBy { contrastRatio(it, card) }, freshColor(card))
    }

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
