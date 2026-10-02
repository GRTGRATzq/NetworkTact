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
package org.meshtastic.core.testing

import kotlinx.coroutines.flow.MutableStateFlow
import org.meshtastic.core.model.team.TeamRosterRecord
import org.meshtastic.core.repository.TeamRosterPrefs

/** In-memory [TeamRosterPrefs] with the same rules as the real one, applied synchronously. */
class FakeTeamRosterPrefs : TeamRosterPrefs {
    override val roster = MutableStateFlow<TeamRosterRecord?>(null)

    override val pendingRoster = MutableStateFlow<TeamRosterRecord?>(null)

    override fun offerReceived(record: TeamRosterRecord) {
        pendingRoster.value = TeamRosterRecord.pendingAfter(roster.value, record)
    }

    override fun adopt(record: TeamRosterRecord) {
        roster.value = record
        pendingRoster.value = null
    }

    override fun acceptPending(expected: TeamRosterRecord) {
        if (pendingRoster.value == expected) adopt(expected)
    }

    override fun dismissPending(expected: TeamRosterRecord) {
        if (pendingRoster.value == expected) pendingRoster.value = null
    }
}
