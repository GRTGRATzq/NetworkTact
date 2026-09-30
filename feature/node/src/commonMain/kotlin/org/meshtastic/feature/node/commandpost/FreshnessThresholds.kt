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
package org.meshtastic.feature.node.commandpost

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Every threshold the command post view uses, in one place.
 *
 * @property recentContact a node heard within this window is "seen recently".
 * @property stalePosition a position whose fix is older than this is an OLD POSITION.
 * @property clockTolerance how far into the future a sender's timestamp may be before it is inconsistent; covers small
 *   clock drift between radios and phones.
 * @property refreshInterval how often the view recomputes ages without waiting for a new packet.
 */
data class FreshnessThresholds(
    val recentContact: Duration = RECENT_CONTACT,
    val stalePosition: Duration = STALE_POSITION,
    val clockTolerance: Duration = CLOCK_TOLERANCE,
    val refreshInterval: Duration = REFRESH_INTERVAL,
) {
    companion object {
        private val RECENT_CONTACT = 15.minutes
        private val STALE_POSITION = 10.minutes
        private val CLOCK_TOLERANCE = 2.minutes
        private val REFRESH_INTERVAL = 15.seconds

        val Default = FreshnessThresholds()
    }
}
