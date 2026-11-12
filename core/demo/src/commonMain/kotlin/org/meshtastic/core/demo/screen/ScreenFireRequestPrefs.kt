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
import org.meshtastic.core.model.firerequest.FireRequest
import org.meshtastic.core.model.firerequest.GdhZone
import org.meshtastic.core.repository.DemoMode
import org.meshtastic.core.repository.FireRequestPrefs

/** The screens' [FireRequestPrefs]: the demo's while demo mode is on, the real ones otherwise, never both. */
class ScreenFireRequestPrefs(
    private val real: FireRequestPrefs,
    private val demo: FireRequestPrefs,
    demoMode: DemoMode,
) : FireRequestPrefs {
    private val switch = ScreenSwitch(demoMode)

    private fun side(): FireRequestPrefs = switch.pick({ real }, { demo })

    override val gdhZone: StateFlow<GdhZone> = switch.state(real.gdhZone, demo.gdhZone)
    override val draft: StateFlow<FireRequest?> = switch.state(real.draft, demo.draft)
    override val lastSent: StateFlow<FireRequest?> = switch.state(real.lastSent, demo.lastSent)

    override fun setGdhZone(zone: GdhZone) = side().setGdhZone(zone)

    override fun saveDraft(request: FireRequest?) = side().saveDraft(request)

    override fun recordSent(request: FireRequest) = side().recordSent(request)
}
