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

import org.meshtastic.core.model.firerequest.FireRequestSheet as Sheet

private val WHITESPACE = Regex("""\s+""")

/** [text] as it may go into the message: on one line, without « · », trimmed. */
internal fun cleanText(text: String): String = text.replace("·", " ").replace(WHITESPACE, " ").trim()

/** [text] as one word: what the message separates with spaces (Rep boxes, times, coordinate pieces) has none. */
internal fun token(text: String): String = cleanText(text).replace(" ", "")

/** `label value [unit]`, or null when [value] is empty. */
internal fun labelled(label: String, value: String, unit: String? = null): String? {
    val text = cleanText(value)
    if (text.isEmpty()) return null
    return listOfNotNull(label, text, unit).joinToString(" ")
}

/** A ticked box with an optional value: `label`, `label value`, or null when not ticked. */
internal fun ticked(label: String, value: String?): String? = value?.let { labelled(label, it) ?: label }

internal fun flag(label: String, ticked: Boolean): String? = label.takeIf { ticked }

/** Two values either side of « / », an empty one left empty: `2 / 6`, `/ 6`, `10 /`; null when both are empty. */
internal fun slashPair(label: String, first: String, second: String, unit: String?): String? {
    val a = cleanText(first)
    val b = cleanText(second)
    if (a.isEmpty() && b.isEmpty()) return null
    return buildString {
        append(label)
        if (a.isNotEmpty()) append(" ").append(a)
        append(" /")
        if (b.isNotEmpty()) append(" ").append(b)
        if (unit != null) append(" ").append(unit)
    }
}

/** Two codes as box 5 writes them: `1 / 3`, `1`, `/ 3` or with « et »: `2 et 3`, `2`, `et 3`. */
internal fun codePair(label: String, first: String, second: String, joint: String): String? {
    val a = cleanText(first)
    val b = cleanText(second)
    if (a.isEmpty() && b.isEmpty()) return null
    return listOf(label, a, if (b.isEmpty()) "" else "$joint $b").filter { it.isNotEmpty() }.joinToString(" ")
}

internal fun target(label: String, ticked: Boolean, state: String?): String? =
    if (ticked || state != null) listOfNotNull(label, state).joinToString(" ") else null

internal fun lineOf(head: String, elements: List<String>): String? =
    elements.takeIf { it.isNotEmpty() }?.let { "$head ${it.joinToString(Sheet.SEPARATOR)}" }

internal fun shift(direction: Shift?, metres: String): String? =
    direction?.let { labelled(it.label, metres, Sheet.METRES) ?: it.label }
