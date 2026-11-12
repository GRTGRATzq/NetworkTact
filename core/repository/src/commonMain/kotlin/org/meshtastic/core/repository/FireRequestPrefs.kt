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
package org.meshtastic.core.repository

import kotlinx.coroutines.flow.StateFlow
import org.meshtastic.core.model.firerequest.FireRequest
import org.meshtastic.core.model.firerequest.GdhZone

/**
 * What the fire request form keeps on the phone: the time zone chosen for date-time groups, the request being written
 * (one draft for the phone), and the last request sent, which « Renouveler » brings back.
 */
interface FireRequestPrefs {
    /** The zone of date-time groups; the phone's local time until the operator chooses another. */
    val gdhZone: StateFlow<GdhZone>

    /** The request being written, or null. */
    val draft: StateFlow<FireRequest?>

    /** The last request sent, or null. */
    val lastSent: StateFlow<FireRequest?>

    fun setGdhZone(zone: GdhZone)

    /** Keeps [request] as the draft; null throws the draft away. */
    fun saveDraft(request: FireRequest?)

    /** [request] has been sent: it becomes the last request sent, and the draft is thrown away. */
    fun recordSent(request: FireRequest)
}
