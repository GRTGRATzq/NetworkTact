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
package org.meshtastic.core.model.firerequest

// Reading one element of a box back: the inverse of FireRequestElements.kt.

/** The value after [label] in [element]: `""` for the label alone, null when the element is not this label's. */
internal fun valueOf(element: String, label: String): String? = when {
    element == label -> ""
    element.startsWith("$label ") -> element.substring(label.length + 1)
    else -> null
}

/** The value of `label value unit` (`Front 400 m` gives `400`), or null when [element] is not written so. */
internal fun measureOf(element: String, label: String, unit: String): String? =
    valueOf(element, label)?.takeIf { it.endsWith(" $unit") }?.dropLast(unit.length + 1)

/** `label a / b unit` with either value empty (`Nb de cps / 6`), or null. */
internal fun slashPairOf(element: String, label: String, unit: String?): Pair<String, String>? {
    val unitPattern = unit?.let { " " + Regex.escape(it) }.orEmpty()
    val pattern = Regex("^" + Regex.escape(label) + """(?: (\S+))? /(?: (\S+))?""" + unitPattern + "$")
    val match = pattern.find(element) ?: return null
    return match.groupValues[1] to match.groupValues[2]
}

/** The two codes of `1 / 3`, `1`, `/ 3` (or with « et »), as written by box 3 and box 5. */
internal fun codePairOf(value: String, joint: String): Pair<String, String> = when {
    value.startsWith("$joint ") -> "" to value.removePrefix("$joint ")
    value.contains(" $joint ") -> value.substringBefore(" $joint ") to value.substringAfter(" $joint ")
    else -> value to ""
}

/** The direction and metres of a Rep shift (`Plus à l'est 200 m`), or null. */
internal fun shiftOf(element: String): Pair<Shift, String>? = Shift.entries
    .sortedByDescending { it.label.length }
    .firstNotNullOfOrNull { shift ->
        when {
            element == shift.label -> shift to ""
            else -> measureOf(element, shift.label, FireRequestSheet.METRES)?.let { shift to it }
        }
    }

/** The elements of a box line without its number: `5 Lanceurs 2 · Mode 1` gives `Lanceurs 2`, `Mode 1`. */
internal fun elementsOf(content: String): List<String> =
    if (content == FireRequestSheet.NOT_FILLED) emptyList() else content.split(FireRequestSheet.SEPARATOR)
