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
package org.meshtastic.core.ui.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.Single

/**
 * A point the map is asked to centre on, such as a coordinate read from a message. [label] says what the point is and
 * when it was taken (or that the time is unknown); null when the requesting screen has nothing to say.
 */
data class MapFocusPoint(val latitude: Double, val longitude: Double, val label: String? = null)

/**
 * The point the map should centre on the next time it is shown. Another screen [request]s it before opening the map;
 * the map centres on it, then [consume]s it, so it is applied once. The point stays [marked] on the map until the user
 * [clearMark]s it or another one is requested. Local display only: nothing is sent.
 */
@Single
class MapFocusRequests {
    private val _pending = MutableStateFlow<MapFocusPoint?>(null)
    val pending: StateFlow<MapFocusPoint?> = _pending.asStateFlow()

    private val _marked = MutableStateFlow<MapFocusPoint?>(null)

    /** The last point requested, shown on the map by a marker until cleared. */
    val marked: StateFlow<MapFocusPoint?> = _marked.asStateFlow()

    fun request(point: MapFocusPoint) {
        _marked.value = point
        _pending.value = point
    }

    /** Removes the marker; the map does not move. */
    fun clearMark() {
        _marked.value = null
    }

    /** Clears [point] once applied; a newer request made in between is kept. */
    fun consume(point: MapFocusPoint) {
        _pending.update { if (it == point) null else it }
    }
}
