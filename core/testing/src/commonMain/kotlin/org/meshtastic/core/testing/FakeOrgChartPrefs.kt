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
import org.meshtastic.core.model.orgchart.OrgChartHistory
import org.meshtastic.core.model.orgchart.OrgChartRecord
import org.meshtastic.core.repository.OrgChartPrefs

/** In-memory [OrgChartPrefs] with the same rules as the real one, applied synchronously. */
class FakeOrgChartPrefs : OrgChartPrefs {
    override val current = MutableStateFlow<OrgChartRecord?>(null)

    override val previous = MutableStateFlow<OrgChartRecord?>(null)

    override fun save(record: OrgChartRecord) = change { it.saved(record) }

    override fun adopt(record: OrgChartRecord) = change { it.adopted(record) }

    override fun revertToPrevious() = change { it.reverted() }

    private fun change(transform: (OrgChartHistory) -> OrgChartHistory) {
        val next = transform(OrgChartHistory(current.value, previous.value))
        current.value = next.current
        previous.value = next.previous
    }
}
