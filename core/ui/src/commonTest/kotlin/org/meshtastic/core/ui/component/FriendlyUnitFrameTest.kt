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
package org.meshtastic.core.ui.component

import androidx.compose.ui.unit.Density
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FriendlyFrameLabelTest {

    @Test
    fun `a short name of four characters or fewer is kept whole`() {
        assertEquals("ALFA", friendlyFrameLabel("ALFA"))
        assertEquals("PC", friendlyFrameLabel("PC"))
    }

    @Test
    fun `a longer name is cut to four characters`() {
        assertEquals("DRON", friendlyFrameLabel("DRONE"))
    }

    @Test
    fun `surrounding spaces are dropped`() {
        assertEquals("TIR", friendlyFrameLabel("  TIR "))
    }

    @Test
    fun `a radio without a name shows question marks`() {
        assertEquals("???", friendlyFrameLabel(""))
        assertEquals("???", friendlyFrameLabel("   "))
    }

    @Test
    fun `a surrogate pair counts as one character and is never split`() {
        assertEquals("🐰", friendlyFrameLabel("🐰"))
        assertEquals("A🐰BC", friendlyFrameLabel("A🐰BCD"))
    }
}

class DashRunsTest {

    private fun assertWellFormed(runs: List<IntRange>, length: Int, dash: Int, gap: Int) {
        assertEquals(0, runs.first().first, "first dash starts on the corner")
        assertEquals(length - 1, runs.last().last, "last dash ends on the corner")
        runs.forEach { assertEquals(dash, it.last - it.first + 1, "every dash is exactly $dash px") }
        runs.zipWithNext { a, b ->
            val space = b.first - a.last - 1
            assertTrue(space in gap..(gap + 1 + dash), "gap $space px out of range")
        }
    }

    @Test
    fun `sides of the frame at the common densities start and end on a dash`() {
        // Frame 48 × 32 dp, dash 5 dp, gap 3 dp, at mdpi, xhdpi, xxhdpi (typical and exact) and xxxhdpi.
        listOf(1f, 2f, 2.625f, 3f, 3.5f, 4f).forEach { scale ->
            val g = FriendlyFrameGeometry(Density(scale))
            assertWellFormed(dashRuns(g.frameWidthPx, g.dashPx, g.gapPx), g.frameWidthPx, g.dashPx, g.gapPx)
            assertWellFormed(dashRuns(g.frameHeightPx, g.dashPx, g.gapPx), g.frameHeightPx, g.dashPx, g.gapPx)
        }
    }

    @Test
    fun `gaps differ by one pixel at most`() {
        val runs = dashRuns(length = 126, dash = 13, gap = 8)
        val gaps = runs.zipWithNext { a, b -> b.first - a.last - 1 }
        assertTrue(gaps.max() - gaps.min() <= 1, "gaps $gaps")
    }

    @Test
    fun `a side too short for two dashes is drawn solid`() {
        assertEquals(listOf(0 until 10), dashRuns(length = 10, dash = 5, gap = 3))
    }

    @Test
    fun `dashes are never thinner than the edge`() {
        listOf(1f, 2.625f, 4f).forEach { scale ->
            val g = FriendlyFrameGeometry(Density(scale))
            assertTrue(g.dashPx >= g.edgePx)
            assertTrue(g.edgePx >= 2)
        }
    }
}
