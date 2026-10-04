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
package org.meshtastic.core.demo.screen

import org.meshtastic.core.demo.store.DemoStore
import org.meshtastic.core.domain.usecase.settings.RadioConfigUseCase
import org.meshtastic.core.repository.DemoMode
import org.meshtastic.core.repository.RadioController
import org.meshtastic.proto.User

/**
 * The Teams screen's [RadioConfigUseCase]. While demo mode is on, choosing a team renames the demo node in memory: no
 * owner write reaches a radio. The Teams screen uses [setOwner] only; it is the one write this class takes over.
 */
class ScreenRadioConfigUseCase(radioController: RadioController, private val store: DemoStore, demoMode: DemoMode) :
    RadioConfigUseCase(radioController) {
    private val switch = ScreenSwitch(demoMode)

    override suspend fun setOwner(destNum: Int, user: User, onRequestId: (Int) -> Unit): Int {
        if (!switch.isDemo) return super.setOwner(destNum, user, onRequestId)
        store.updateNode(destNum) { it.copy(user = user) }
        return 0
    }
}
