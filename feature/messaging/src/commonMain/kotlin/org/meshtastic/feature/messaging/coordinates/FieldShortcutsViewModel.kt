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
package org.meshtastic.feature.messaging.coordinates

import androidx.lifecycle.ViewModel
import org.koin.core.annotation.KoinViewModel
import org.koin.core.annotation.Named
import org.meshtastic.core.common.util.nowSeconds
import org.meshtastic.core.common.util.systemTimeZone
import org.meshtastic.core.repository.DemoMode
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.repository.PhonePositionSource
import org.meshtastic.core.repository.SCREEN_DATA

/** Reads what the field shortcuts need. Builds text only: sending stays with the user. */
@KoinViewModel
class FieldShortcutsViewModel(
    @Named(SCREEN_DATA) private val nodeRepository: NodeRepository,
    private val phonePositionSource: PhonePositionSource,
    private val demoMode: DemoMode,
) : ViewModel() {

    /** The `[POS]` message for my radio, or for the phone when the radio has no position. */
    suspend fun myPosition(): MyPositionResult {
        val ourNode = nodeRepository.ourNodeInfo.value
        // The phone's own fix is real data: a demo position message never uses it.
        val usePhone = ourNode != null && ourNode.validPosition == null && !demoMode.isActive.value
        val phone = if (usePhone) phonePositionSource.lastFix() else null
        return myPositionMessage(ourNode, phone, nowSeconds, systemTimeZone)
    }
}
