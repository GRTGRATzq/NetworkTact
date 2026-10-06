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
package org.meshtastic.core.model

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NodeColorsTest {

    @Test
    fun everyPairReadsAtLeastAa() {
        nodePalette.forEach { (foreground, background) ->
            val ratio = contrast(foreground, background)
            assertTrue(ratio >= 4.5, "${hex(background)} reads $ratio:1")
        }
    }

    @Test
    fun paletteHoldsNoRedPinkOrangeAmberYellowOrBrown() {
        // Those hues sit from 330 degrees round to 70 degrees; they are reserved for urgent and report.
        nodePalette.forEach { (_, background) ->
            val hue = hue(background)
            assertTrue(hue in 70.0..330.0, "${hex(background)} has hue $hue")
        }
    }

    @Test
    fun colourIsStableForANodeNumber() {
        assertEquals(nodeColorsFromNum(0x12345678), nodeColorsFromNum(0x12345678))
        // Pinned indexes: the same node has the same colour on every phone and on iOS and desktop builds.
        assertEquals(7, nodePaletteIndex(1))
        assertEquals(6, nodePaletteIndex(2))
        assertEquals(12, nodePaletteIndex(0x123456))
        assertEquals(9, nodePaletteIndex(-1))
        assertEquals(8, nodePaletteIndex(Int.MAX_VALUE))
    }

    @Test
    fun neighbouringNodeNumbersUseTheWholePalette() {
        val used = (1..1000).map { nodePaletteIndex(it) }.toSet()
        assertEquals(nodePalette.indices.toSet(), used)
    }

    @Test
    fun nodeColorsComeFromThePalette() {
        listOf(0, 1, 2, 0x123456, -1, Int.MIN_VALUE, Int.MAX_VALUE).forEach { num ->
            assertTrue(Node(num = num).colors in nodePalette, "node $num left the palette")
        }
    }

    private fun channel(argb: Int, shift: Int): Double = ((argb shr shift) and 0xFF) / 255.0

    private fun linear(c: Double): Double = if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)

    private fun luminance(argb: Int): Double =
        0.2126 * linear(channel(argb, 16)) + 0.7152 * linear(channel(argb, 8)) + 0.0722 * linear(channel(argb, 0))

    private fun contrast(a: Int, b: Int): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    private fun hue(argb: Int): Double {
        val r = channel(argb, 16)
        val g = channel(argb, 8)
        val b = channel(argb, 0)
        val hi = max(r, max(g, b))
        val delta = hi - min(r, min(g, b))
        if (delta == 0.0) return 0.0
        val sector =
            when (hi) {
                r -> ((g - b) / delta).mod(6.0)
                g -> (b - r) / delta + 2
                else -> (r - g) / delta + 4
            }
        return sector * 60
    }

    private fun hex(argb: Int): String = (argb.toLong() and 0xFFFFFF).toString(16).padStart(6, '0')
}
