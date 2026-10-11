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

import org.meshtastic.core.model.team.TeamSuffix

/** Call signs as the organisation chart compares them: trimmed, inner spaces collapsed, case ignored. */
object Callsign {
    private const val MQTT_MARK = " (MQTT)"

    /** The call sign a radio's long name carries: the name without its team suffix (`ALPHA-1 [Alpha]` → `ALPHA-1`). */
    fun ofLongName(longName: String): String = OrgChart.clean(TeamSuffix.displayName(longName).removeSuffix(MQTT_MARK))

    /** The form two call signs are compared in. */
    fun key(callsign: String): String = OrgChart.clean(callsign).uppercase()

    fun sameAs(first: String, second: String): Boolean = key(first) == key(second)
}
