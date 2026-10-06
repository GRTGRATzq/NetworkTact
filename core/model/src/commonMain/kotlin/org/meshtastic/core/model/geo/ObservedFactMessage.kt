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
package org.meshtastic.core.model.geo

/**
 * The observed fact report: a `[CR]` message, so it is filed as a report with no new prefix, giving the time of the
 * observation, the place in the format it was typed in, plus MGRS, then the free description:
 *
 * `[CR] FAIT OBSERVÉ · 14:05 · Lieu DMS 48°51'24"N 002°21'03"E = MGRS 31U DQ 52382 11725 · deux véhicules arrêtés`
 *
 * A time later than the current minute is the day before: `· 23:50 (veille) ·`. A place typed in MGRS is given once.
 * Beyond 84° N and 80° S, where MGRS is undefined, only DMS is given. Reports in the earlier format, without the time,
 * are still recognised: they carry the same header.
 */
object ObservedFactMessage {
    const val HEADER = "[CR] FAIT OBSERVÉ"

    private const val SEPARATOR = " · "
    private val WHITESPACE = Regex("""\s+""")
    private val OBSERVED_AT = Regex("""^\[CR] FAIT OBSERVÉ · (\d{2}:\d{2}(?: \(veille\))?) · """)

    /**
     * The report for [place] typed as [typedAs], observed at [observedAt], with [description] on one line. Nothing is
     * shortened. Without [observedAt], the report has the earlier format, with no time.
     */
    fun build(
        place: FormattedCoordinates,
        typedAs: CoordinateFormat,
        description: String,
        observedAt: ObservedTime? = null,
    ): String {
        val typed = place.format(typedAs)?.let { "${typedAs.name} $it" }
        val mgrs = place.mgrs?.let { "MGRS $it" }
        val location =
            when {
                typed == null -> "DMS ${place.dms}"
                typedAs == CoordinateFormat.MGRS || mgrs == null -> typed
                else -> "$typed = $mgrs"
            }
        val text = description.replace(WHITESPACE, " ").trim()
        return listOfNotNull(HEADER, observedAt?.label(), "Lieu $location", text.takeIf { it.isNotEmpty() })
            .joinToString(SEPARATOR)
    }

    /** Whether [text] is an observed fact report, in either format. */
    fun isObservedFact(text: String): Boolean = text.trimStart().startsWith(HEADER)

    /** The time of observation [text] gives (`14:05`, `23:50 (veille)`), or null for a report without one. */
    fun observedAt(text: String): String? = OBSERVED_AT.find(text.trimStart())?.groupValues?.get(1)
}
