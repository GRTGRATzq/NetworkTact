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
 * NetworkTact: when my radio last passed a mesh packet to this phone. Read by the link diagnostic.
 *
 * Kept in memory only: null after each start of the app until the first packet. Every mesh packet the radio hands over
 * counts (from another node, or my radio's own), whatever its port.
 */
interface RadioActivityProvider {
    /** Epoch milliseconds of the last mesh packet received from my radio, or null if none since the app started. */
    val lastPacketFromRadioMillis: StateFlow<Long?>
}
