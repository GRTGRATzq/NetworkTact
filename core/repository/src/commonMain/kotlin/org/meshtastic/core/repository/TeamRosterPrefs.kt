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
package org.meshtastic.core.repository

import kotlinx.coroutines.flow.StateFlow
import org.meshtastic.core.model.team.TeamRosterRecord

/**
 * The team list kept on this phone. It is stored with the app's preferences, not in a radio's database, so it survives
 * a change of radio.
 *
 * A list received over the mesh is never adopted on its own: it waits in [pendingRoster] until the user confirms it,
 * since nothing authenticates its sender.
 */
interface TeamRosterPrefs {
    /** The adopted list, or null before any list has been adopted. */
    val roster: StateFlow<TeamRosterRecord?>

    /** The last list received over the mesh that differs from [roster] and awaits confirmation, or null. */
    val pendingRoster: StateFlow<TeamRosterRecord?>

    /** Records a received list: it replaces any pending one, unless it holds exactly the adopted teams. */
    fun offerReceived(record: TeamRosterRecord)

    /** Adopts [record] (typed by hand or broadcast from this phone) and drops any pending list. */
    fun adopt(record: TeamRosterRecord)

    /** Adopts the pending list, provided it is still [expected] and has not been replaced by a newer one meanwhile. */
    fun acceptPending(expected: TeamRosterRecord)

    /** Drops the pending list, provided it is still [expected]. */
    fun dismissPending(expected: TeamRosterRecord)
}
