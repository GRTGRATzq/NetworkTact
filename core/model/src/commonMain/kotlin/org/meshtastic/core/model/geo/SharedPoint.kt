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

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.meshtastic.core.model.team.clipToUtf8Bytes
import org.meshtastic.core.model.utf8Size
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/**
 * The point a sent message is shared as on the map, as an ordinary Meshtastic waypoint that every client shows:
 * - `[CR] FAIT OBSERVÉ`: `FO 14:05 ALPHA-1`, the description of the fact;
 * - `[POS]`: `POS 14:32 ALPHA-1`, or `POS ANC 14:07 ALPHA-1` for an old position and `POS --:-- ALPHA-1` without a fix
 *   time, the time part of the message as description, so a shared position never passes for a current one;
 * - any other message with a coordinate: `PT ALPHA-1`, `envoyé 14:05 · ` and the text.
 *
 * The [id] is derived from the sender and the text: sharing the same message again updates the same waypoint instead of
 * adding a second one. The waypoint is locked to its sender, who alone can move or remove it, and expires after
 * [VALIDITY].
 */
data class SharedPoint(
    val id: Int,
    val name: String,
    val description: String,
    val point: LatLon,
    val expireEpochSeconds: Long,
    val lockedTo: Int,
    val icon: Int,
) {
    companion object {
        /** Usable bytes of `Waypoint.name` (`max_size:30`, one of which is the NUL terminator). */
        const val NAME_MAX_BYTES = 29

        /** Usable bytes of `Waypoint.description` (`max_size:100`, one of which is the NUL terminator). */
        const val DESCRIPTION_MAX_BYTES = 99

        /** How long a shared point stays on the maps of the mesh. */
        val VALIDITY = 24.hours

        /** 👁 observed fact, 📍 position, 📌 any other point. */
        const val ICON_OBSERVED_FACT = 0x1F441
        const val ICON_POSITION = 0x1F4CD
        const val ICON_POINT = 0x1F4CC

        private const val SEPARATOR = " · "
        private const val ELLIPSIS = "…"
        private const val UNKNOWN_TIME = "--:--"
        private const val CLOCK_DIGITS = 2
        private const val FNV_OFFSET = -0x7ee3623b // 0x811C9DC5
        private const val FNV_PRIME = 0x01000193
        private val CLOCK = Regex("""\d{2}:\d{2}""")

        /**
         * The point [message], sent by node [senderNum] whose call sign is [callsign] at [sentAtEpochSeconds], is
         * shared as; null when the message holds no complete coordinate. Clock times are shown in [timeZone].
         */
        fun of(
            message: String,
            callsign: String,
            senderNum: Int,
            sentAtEpochSeconds: Long,
            timeZone: TimeZone,
        ): SharedPoint? {
            val found = MessageCoordinate.find(message) ?: return null
            val text = message.trim()
            val (kind, description) =
                when {
                    ObservedFactMessage.isObservedFact(text) -> observedFact(text)
                    text.startsWith(PositionMessage.PREFIX) -> position(text)
                    else -> "PT" to "envoyé ${clock(sentAtEpochSeconds, timeZone)}$SEPARATOR$text"
                }
            val icon =
                when {
                    kind.startsWith("FO") -> ICON_OBSERVED_FACT
                    kind.startsWith("POS") -> ICON_POSITION
                    else -> ICON_POINT
                }
            return SharedPoint(
                id = idOf(senderNum, text),
                name = name(kind, callsign),
                description = description.ellipsized(DESCRIPTION_MAX_BYTES),
                point = found.point,
                expireEpochSeconds = sentAtEpochSeconds + VALIDITY.inWholeSeconds,
                lockedTo = senderNum,
                icon = icon,
            )
        }

        /** `FO 14:05`, or `FO --:--` for a report in the earlier format, with the description of the fact. */
        private fun observedFact(text: String): Pair<String, String> {
            val time = ObservedFactMessage.observedAt(text)
            val parts = text.split(SEPARATOR)
            val place = parts.indexOfFirst { it.startsWith("Lieu ") }
            val fact = if (place < 0) "" else parts.drop(place + 1).joinToString(SEPARATOR)
            val description = if (time == null) "heure d'observation inconnue$SEPARATOR$fact" else fact
            return "FO ${time ?: UNKNOWN_TIME}" to description.removeSuffix(SEPARATOR)
        }

        /** `POS 14:32`, `POS ANC 14:07` or `POS --:--`, with the time part of the message as description. */
        private fun position(text: String): Pair<String, String> {
            val parts = text.split(SEPARATOR)
            val time = parts.last()
            val old = time.startsWith("POSITION ANCIENNE")
            val known = time.startsWith("relevée ") || (old && "relevée il y a" in time)
            val clock = if (known) CLOCK.findAll(time).lastOrNull()?.value else null
            val kind = listOfNotNull("POS", "ANC".takeIf { old }, clock ?: UNKNOWN_TIME).joinToString(" ")
            val fromPhone = parts.first().endsWith("(téléphone)")
            val description = if (fromPhone) "$time${SEPARATOR}position du téléphone" else time
            return kind to description
        }

        /** [kind] then the call sign, which is shortened when the whole passes [NAME_MAX_BYTES]. */
        private fun name(kind: String, callsign: String): String {
            val room = NAME_MAX_BYTES - "$kind ".utf8Size()
            val clipped = callsign.trim().clipToUtf8Bytes(room).trimEnd()
            return if (clipped.isEmpty()) kind else "$kind $clipped"
        }

        /** 32-bit FNV-1a of sender and text, never 0 (an id of 0 is a waypoint not sent yet). */
        internal fun idOf(senderNum: Int, text: String): Int {
            var hash = FNV_OFFSET
            "$senderNum|$text".encodeToByteArray().forEach { byte ->
                hash = (hash xor (byte.toInt() and BYTE_MASK)) * FNV_PRIME
            }
            return if (hash == 0) 1 else hash
        }

        private const val BYTE_MASK = 0xFF

        private fun clock(epochSeconds: Long, timeZone: TimeZone): String {
            val time = Instant.fromEpochSeconds(epochSeconds).toLocalDateTime(timeZone).time
            return "${time.hour.toString().padStart(CLOCK_DIGITS, '0')}:" +
                time.minute.toString().padStart(CLOCK_DIGITS, '0')
        }

        private fun String.ellipsized(maxBytes: Int): String = if (utf8Size() <= maxBytes) {
            this
        } else {
            clipToUtf8Bytes(maxBytes - ELLIPSIS.utf8Size()).trimEnd() + ELLIPSIS
        }
    }
}
