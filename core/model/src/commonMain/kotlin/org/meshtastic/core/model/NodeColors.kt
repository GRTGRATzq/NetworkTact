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

/**
 * Fixed node colours (background, foreground) as ARGB, picked by the node number.
 *
 * NetworkTact reserves red for urgent messages and amber for reports, so the palette holds no red, pink, orange, amber,
 * yellow or brown: blues, cyans, teals, greens, violets and a slate, dark ones with white text and pale ones with black
 * text. Every pair reads at 4.5:1 or more (lowest 4.64:1). The earlier colour, the node number's own RGB bytes, could
 * be any hue.
 */
private val NODE_PALETTE: List<Pair<Int, Int>> =
    listOf(
        0xFF1565C0 to WHITE_ARGB,
        0xFF0D47A1 to WHITE_ARGB,
        0xFF00838F to BLACK_ARGB,
        0xFF00695C to WHITE_ARGB,
        0xFF2E7D32 to WHITE_ARGB,
        0xFF33691E to WHITE_ARGB,
        0xFF4527A0 to WHITE_ARGB,
        0xFF6A1B9A to WHITE_ARGB,
        0xFF283593 to WHITE_ARGB,
        0xFF37474F to WHITE_ARGB,
        0xFF4FC3F7 to BLACK_ARGB,
        0xFF80CBC4 to BLACK_ARGB,
        0xFFA5D6A7 to BLACK_ARGB,
        0xFFB39DDB to BLACK_ARGB,
        0xFF90CAF9 to BLACK_ARGB,
        0xFFC5E1A5 to BLACK_ARGB,
    )
        .map { (background, foreground) -> background.toInt() to foreground.toInt() }

private const val WHITE_ARGB = 0xFFFFFFFF
private const val BLACK_ARGB = 0xFF000000

// MurmurHash3 finalizer: spreads neighbouring node numbers over the palette.
private const val MIX_SHIFT_1 = 16
private const val MIX_SHIFT_2 = 13
private const val MIX_MULTIPLIER_1 = 0x85EBCA6B
private const val MIX_MULTIPLIER_2 = 0xC2B2AE35
private const val UNSIGNED_INT_MASK = 0xFFFFFFFFL

/** Palette index of [nodeNum]: the same on every phone and platform. */
internal fun nodePaletteIndex(nodeNum: Int): Int {
    var h = nodeNum
    h = h xor (h ushr MIX_SHIFT_1)
    h *= MIX_MULTIPLIER_1.toInt()
    h = h xor (h ushr MIX_SHIFT_2)
    h *= MIX_MULTIPLIER_2.toInt()
    h = h xor (h ushr MIX_SHIFT_1)
    return ((h.toLong() and UNSIGNED_INT_MASK) % NODE_PALETTE.size).toInt()
}

/** The full node palette as (foreground, background) pairs, for tests. */
internal val nodePalette: List<Pair<Int, Int>>
    get() = NODE_PALETTE.map { (background, foreground) -> foreground to background }

/** Derives a stable color pair from a node number. Returns (foreground, background) as @ColorInt. */
fun nodeColorsFromNum(nodeNum: Int): Pair<Int, Int> {
    val (background, foreground) = NODE_PALETTE[nodePaletteIndex(nodeNum)]
    return foreground to background
}
