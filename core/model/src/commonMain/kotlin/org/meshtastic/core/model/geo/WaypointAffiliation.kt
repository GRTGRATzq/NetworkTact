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
 * The APP-6(C) standard identity of a waypoint, the only thing its frame shows (lot 1 of point 3).
 *
 * It travels in the waypoint's existing `icon` field, as one code point of Unicode's Private Use Area, so neither the
 * protocol nor the firmware changes. A code point is what the field already carries, so these pass the receive-side
 * check that clears an invalid icon; another Meshtastic app shows them as an empty box. Anything else in the field, 0
 * included, is shown as [UNKNOWN].
 */
@Suppress("MagicNumber") // The four codes are the wire format itself.
enum class WaypointAffiliation(val code: Int) {
    FRIEND(0xE001),
    HOSTILE(0xE002),
    NEUTRAL(0xE003),
    UNKNOWN(0xE004),
    ;

    companion object {
        /** What a new waypoint starts as, and what an automatic one without a better guess gets. */
        val DEFAULT = UNKNOWN

        /** The affiliation a received or stored `icon` value stands for; [UNKNOWN] for anything not recognised. */
        fun fromIcon(icon: Int?): WaypointAffiliation = entries.firstOrNull { it.code == icon } ?: UNKNOWN
    }
}
