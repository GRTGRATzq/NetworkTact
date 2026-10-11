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
import org.meshtastic.core.demo.store.DemoStore
import org.meshtastic.core.model.orgchart.OrgChartHistory
import org.meshtastic.core.model.orgchart.OrgChartRecord
import org.meshtastic.core.repository.OrgChartPrefs

/** [OrgChartPrefs] over the demo's chart: changed, adopted or reverted in memory only, gone when the demo ends. */
class DemoOrgChartPrefs(private val store: DemoStore) : OrgChartPrefs {
    override val current: StateFlow<OrgChartRecord?> = store.orgChart

    override val previous: StateFlow<OrgChartRecord?> = store.previousOrgChart

    override fun save(record: OrgChartRecord) = change { it.saved(record) }

    override fun adopt(record: OrgChartRecord) = change { it.adopted(record) }

    override fun revertToPrevious() = change { it.reverted() }

    private fun change(transform: (OrgChartHistory) -> OrgChartHistory) {
        val next = transform(OrgChartHistory(store.orgChart.value, store.previousOrgChart.value))
        store.orgChart.value = next.current
        store.previousOrgChart.value = next.previous
    }
}
