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
package org.meshtastic.core.demo.screen

import kotlinx.coroutines.flow.StateFlow
import org.meshtastic.core.model.team.TeamRosterRecord
import org.meshtastic.core.repository.DemoMode
import org.meshtastic.core.repository.TeamRosterPrefs

/**
 * The screens' [TeamRosterPrefs]. While demo mode is on, the screens see the demo list only: a real `[EQUIPES]` list
 * the radio service records meanwhile stays pending in the real preferences, and its prompt appears once demo mode is
 * switched off.
 */
class ScreenTeamRosterPrefs(private val real: TeamRosterPrefs, private val demo: TeamRosterPrefs, demoMode: DemoMode) :
    TeamRosterPrefs {
    private val switch = ScreenSwitch(demoMode)

    private fun current(): TeamRosterPrefs = switch.pick({ real }, { demo })

    override val roster: StateFlow<TeamRosterRecord?> = switch.state(real.roster, demo.roster)
    override val pendingRoster: StateFlow<TeamRosterRecord?> = switch.state(real.pendingRoster, demo.pendingRoster)

    override fun offerReceived(record: TeamRosterRecord) = current().offerReceived(record)

    override fun adopt(record: TeamRosterRecord) = current().adopt(record)

    override fun acceptPending(expected: TeamRosterRecord) = current().acceptPending(expected)

    override fun dismissPending(expected: TeamRosterRecord) = current().dismissPending(expected)
}
