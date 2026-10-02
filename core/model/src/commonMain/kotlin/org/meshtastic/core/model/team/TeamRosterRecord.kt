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
package org.meshtastic.core.model.team

import kotlinx.serialization.Serializable

/**
 * A team list as this phone knows it, with where it came from and when.
 *
 * @property teams the team names, already normalized by [TeamRoster].
 * @property source how the list reached this phone.
 * @property senderNum node number of the radio that sent it, for [Source.RADIO] only.
 * @property senderName long name of that node when the list arrived, if it was known.
 * @property timeSeconds when the list was received, typed or broadcast, in epoch seconds of this phone's clock.
 */
@Serializable
data class TeamRosterRecord(
    val teams: List<String>,
    val source: Source,
    val senderNum: Int? = null,
    val senderName: String? = null,
    val timeSeconds: Long,
) {
    val roster: TeamRoster
        get() = TeamRoster(teams)

    enum class Source {
        /** Received over the mesh in a `[EQUIPES]` message. */
        RADIO,

        /** Typed on this phone. */
        MANUAL,

        /** Broadcast from this phone (command post). */
        BROADCAST,
    }

    companion object {
        /**
         * The list to put to the user after [received] arrives: [received] itself, unless it holds the same teams in
         * the same order as the [adopted] list, in which case there is nothing to confirm.
         */
        fun pendingAfter(adopted: TeamRosterRecord?, received: TeamRosterRecord): TeamRosterRecord? =
            received.takeIf { it.teams != adopted?.teams }
    }
}
