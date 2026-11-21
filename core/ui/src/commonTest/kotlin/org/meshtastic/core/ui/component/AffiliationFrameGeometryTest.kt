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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.meshtastic.core.model.geo.WaypointAffiliation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AffiliationFrameGeometryTest {

    @Test
    fun imagesReserveTheHaloAndTheRingAroundEachShape() {
        assertEquals(DpSize(44.dp, 32.dp), AffiliationFrames.outerSize(WaypointAffiliation.FRIEND))
        assertEquals(DpSize(48.dp, 48.dp), AffiliationFrames.outerSize(WaypointAffiliation.HOSTILE))
        assertEquals(DpSize(36.dp, 36.dp), AffiliationFrames.outerSize(WaypointAffiliation.NEUTRAL))
        assertEquals(DpSize(40.dp, 40.dp), AffiliationFrames.outerSize(WaypointAffiliation.UNKNOWN))
    }

    @Test
    fun friendFrameIsThreeByTwoAndSmallerThanARadio() {
        val size = AffiliationFrames.FriendSize
        assertEquals(size.width.value * 2, size.height.value * 3)
        assertTrue(size.width < FriendlyFrame.FrameWidth)
        assertTrue(size.height < FriendlyFrame.FrameHeight)
    }

    @Test
    fun geometryIsInWholePixelsAtEveryDensity() {
        listOf(1f, 1.5f, 2f, 2.625f, 3f, 3.5f).forEach { scale ->
            WaypointAffiliation.entries.forEach { affiliation ->
                val g = AffiliationFrameGeometry(Density(scale), affiliation)
                assertEquals(g.shapeWidthPx + 2 * (g.haloPx + g.ringPx), g.outerWidthPx, "$affiliation x$scale")
                assertEquals(g.shapeHeightPx + 2 * (g.haloPx + g.ringPx), g.outerHeightPx, "$affiliation x$scale")
                assertTrue(g.edgePx >= 2, "$affiliation x$scale")
            }
        }
    }

    @Test
    fun selectorCellFitsTheLargestFrame() {
        assertEquals(DpSize(48.dp, 48.dp), AffiliationFrames.SelectorCellSize)
    }

    @Test
    fun selectorOffersTheFourAffiliationsOnce() {
        assertEquals(WaypointAffiliation.entries.toSet(), AffiliationFrames.SelectorOrder.toSet())
        assertEquals(WaypointAffiliation.entries.size, AffiliationFrames.SelectorOrder.size)
    }
}
