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
import org.meshtastic.core.model.orgchart.OrgChartRecord
import org.meshtastic.core.repository.DemoMode
import org.meshtastic.core.repository.OrgChartPrefs

/** The screens' [OrgChartPrefs]: the demo's chart while demo mode is on, the real one otherwise, never both. */
class ScreenOrgChartPrefs(private val real: OrgChartPrefs, private val demo: OrgChartPrefs, demoMode: DemoMode) :
    OrgChartPrefs {
    private val switch = ScreenSwitch(demoMode)

    private fun side(): OrgChartPrefs = switch.pick({ real }, { demo })

    override val current: StateFlow<OrgChartRecord?> = switch.state(real.current, demo.current)
    override val previous: StateFlow<OrgChartRecord?> = switch.state(real.previous, demo.previous)

    override fun save(record: OrgChartRecord) = side().save(record)

    override fun adopt(record: OrgChartRecord) = side().adopt(record)

    override fun revertToPrevious() = side().revertToPrevious()
}
