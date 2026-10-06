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
package org.meshtastic.core.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * The hues NetworkTact gives a meaning to: red for an urgent message, amber for a report, green for a fresh position.
 * Red and amber are used for nothing else.
 */
enum class TactHue {
    URGENT,
    REPORT,
    FRESH,
}

/**
 * Fixed tones of each [TactHue], one list per theme, in order of preference. The first tone is the normal one; the
 * following ones are deeper (light theme) or paler (dark theme) and only serve when a dynamic system palette puts a
 * background where the first tone no longer reads.
 *
 * Measured on the static schemes (card `#D5D6E0` / `#3D3E50`, screen `#F5F6FA` / `#1A1B26`), first tone: urgent 4.52 /
 * 6.05 (light), 4.59 / 7.49 (dark); report 4.38 / 5.86, 5.64 / 9.19; fresh 4.51 / 6.03, 6.88 / 11.21.
 */
object TactColors {
    private val urgentLight = listOf(Color(0xFFB3261E), Color(0xFF8C1D18), Color(0xFF5C0F0B))
    private val urgentDark = listOf(Color(0xFFFF8A80), Color(0xFFFFB4AB), Color(0xFFFFDAD6))
    private val reportLight = listOf(Color(0xFF8A5300), Color(0xFF6B4100), Color(0xFF452A00))
    private val reportDark = listOf(Color(0xFFF2B33D), Color(0xFFFFD08A), Color(0xFFFFE6C2))
    private val freshLight = listOf(Color(0xFF1E6B3C), Color(0xFF18603A), Color(0xFF0B3D20))
    private val freshDark = listOf(Color(0xFF67EA94), Color(0xFFB5F5CE), Color(0xFFDDFBE8))

    /** The fixed tones of [hue] for a light ([dark] false) or dark background, in order of preference. */
    fun tones(hue: TactHue, dark: Boolean): List<Color> = when (hue) {
        TactHue.URGENT -> if (dark) urgentDark else urgentLight
        TactHue.REPORT -> if (dark) reportDark else reportLight
        TactHue.FRESH -> if (dark) freshDark else freshLight
    }

    /**
     * The tone of [hue] to draw on [background]: the first tone of the background's theme that reaches [minRatio]. If
     * none does, which only an unusual dynamic palette can cause, the fixed tone of [hue] with the highest contrast on
     * [background], whichever theme it belongs to: the hue, and so its meaning, is kept.
     */
    fun legible(hue: TactHue, background: Color, minRatio: Float = MIN_TEXT_CONTRAST): Color {
        val preferred = tones(hue, dark = isDarkBackground(background))
        val strongest = (tones(hue, dark = false) + tones(hue, dark = true)).maxBy { contrastRatio(it, background) }
        return pickLegible(candidates = preferred, background = background, fallback = strongest, minRatio = minRatio)
    }

    /** True for a dark background, whatever drove it: the app's choice, the system, or a dynamic palette. */
    fun isDarkBackground(background: Color): Boolean = background.luminance() < DARK_LUMINANCE

    /** Midpoint luminance separating a light background from a dark one, as in [StatusColors]. */
    private const val DARK_LUMINANCE = 0.5f
}
