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

import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Screens the map asks to open, such as the coordinate converter or the choice of a conversation for a point of the
 * map. The map's navigation entry, which holds the back stack, opens them; the map providers stay unaware of routes. A
 * request made while no map is shown is dropped. An object rather than an injected class: the navigation graph is
 * assembled without dependency injection, in tests too.
 */
object MapNavigationRequests {
    private val _requests =
        MutableSharedFlow<NavKey>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val requests: SharedFlow<NavKey> = _requests.asSharedFlow()

    fun open(route: NavKey) {
        _requests.tryEmit(route)
    }
}
