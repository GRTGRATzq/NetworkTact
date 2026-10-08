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
 * What a message holding a coordinate says about when its point was taken, read from the conventions the app sends: the
 * time of a `[CR] FAIT OBSERVÉ` and the time part of a `[POS]`. Any other message gives no such time.
 *
 * Used where the point is shown, so a position is never shown without its fix time or the mention that it is unknown,
 * and an old one never passes for a current one.
 *
 * @property clock `14:05`, `23:50 (veille)`; null when the message gives none.
 * @property old the `[POS]` says `POSITION ANCIENNE`.
 * @property inconsistent the `[POS]` says its fix time is inconsistent.
 * @property fromPhone the `[POS]` gives the phone's position, the radio having none.
 */
data class MessagePoint(
    val kind: Kind,
    val clock: String? = null,
    val old: Boolean = false,
    val inconsistent: Boolean = false,
    val fromPhone: Boolean = false,
) {
    enum class Kind {
        OBSERVED_FACT,
        POSITION,
        POINT,
    }

    companion object {
        private const val SEPARATOR = " · "
        private const val OLD = "POSITION ANCIENNE"
        private const val TAKEN = "relevée "
        private const val TAKEN_AGO = "relevée il y a"
        private const val INCONSISTENT = "heure de relevé incohérente"
        private const val FROM_PHONE = "(téléphone)"
        private val CLOCK = Regex("""\d{2}:\d{2}""")

        /** Reads [text], a message holding a coordinate. */
        fun of(text: String): MessagePoint {
            val trimmed = text.trim()
            return when {
                ObservedFactMessage.isObservedFact(trimmed) ->
                    MessagePoint(Kind.OBSERVED_FACT, clock = ObservedFactMessage.observedAt(trimmed))

                trimmed.startsWith(PositionMessage.PREFIX) -> position(trimmed)

                else -> MessagePoint(Kind.POINT)
            }
        }

        /** The time part is the last one: `relevée 14:32`, `POSITION ANCIENNE, relevée il y a 25 min (14:07)`, … */
        private fun position(text: String): MessagePoint {
            val parts = text.split(SEPARATOR)
            val time = parts.last()
            val old = time.startsWith(OLD)
            val known = time.startsWith(TAKEN) || (old && TAKEN_AGO in time)
            return MessagePoint(
                kind = Kind.POSITION,
                clock = if (known) CLOCK.findAll(time).lastOrNull()?.value else null,
                old = old,
                inconsistent = time.startsWith(INCONSISTENT),
                fromPhone = parts.first().endsWith(FROM_PHONE),
            )
        }
    }
}
