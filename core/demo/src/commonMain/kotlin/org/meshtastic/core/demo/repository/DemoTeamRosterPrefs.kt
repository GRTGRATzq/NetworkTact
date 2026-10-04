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
package org.meshtastic.core.demo.repository

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.meshtastic.core.demo.store.DemoStore
import org.meshtastic.core.model.team.TeamRosterRecord
import org.meshtastic.core.repository.TeamRosterPrefs

/**
 * [TeamRosterPrefs] over the demo's team list. A real `[EQUIPES]` list received during the demo goes to the real
 * preferences, which the radio service keeps writing: its prompt waits there until demo mode is switched off.
 */
class DemoTeamRosterPrefs(private val store: DemoStore) : TeamRosterPrefs {
    override val roster: StateFlow<TeamRosterRecord?> = store.roster

    private val pending = store.pendingRoster
    override val pendingRoster: StateFlow<TeamRosterRecord?> = pending.asStateFlow()

    override fun offerReceived(record: TeamRosterRecord) {
        if (record.teams != store.roster.value?.teams) pending.value = record
    }

    override fun adopt(record: TeamRosterRecord) {
        store.roster.value = record
        pending.value = null
    }

    override fun acceptPending(expected: TeamRosterRecord) {
        if (pending.value == expected) adopt(expected)
    }

    override fun dismissPending(expected: TeamRosterRecord) {
        if (pending.value == expected) pending.value = null
    }
}
