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
package org.meshtastic.core.model.orgchart

import kotlinx.serialization.Serializable

/** A chart as this phone keeps it: where it comes from and when it was saved or adopted here. */
@Serializable
data class OrgChartRecord(val chart: OrgChart, val source: Source, val timeSeconds: Long) {
    enum class Source {
        /** Made or changed on this phone, in command post mode. */
        EDITED,

        /** Scanned from another phone's QR code and adopted after confirmation. */
        SCANNED,
    }
}

/**
 * The chart in use and the one before it. Adopting a scanned chart keeps the one it replaces, so a mistaken adoption
 * can be undone; going back swaps the two, so it can be undone too. A change made on this phone replaces the chart in
 * use only.
 */
data class OrgChartHistory(val current: OrgChartRecord? = null, val previous: OrgChartRecord? = null) {
    fun saved(record: OrgChartRecord): OrgChartHistory = copy(current = record)

    fun adopted(record: OrgChartRecord): OrgChartHistory = OrgChartHistory(record, current ?: previous)

    fun reverted(): OrgChartHistory = if (previous == null) this else OrgChartHistory(previous, current)
}
