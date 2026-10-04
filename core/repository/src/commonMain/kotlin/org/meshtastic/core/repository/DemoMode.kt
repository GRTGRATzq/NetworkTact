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

/**
 * Koin qualifier of the data sources the screens use: the real ones, or the demo's while demo mode is on. The radio
 * service never takes these, so real packets keep being received, stored and notified during a demo.
 */
const val SCREEN_DATA = "screenData"

/**
 * Demo mode: the same screens over fictitious data held in memory, never written to the real stores. It is not
 * persisted, so every launch starts with it off.
 */
interface DemoMode {
    val isActive: StateFlow<Boolean>

    /** Real text messages received since demo mode was last switched on; 0 while it is off. */
    val realMessagesSinceActivation: StateFlow<Int>

    /** Loads a fresh fictitious data set, then switches the screens over to it. */
    fun activate()

    /** Switches the screens back to the real data, then drops the fictitious data set. */
    fun deactivate()
}
