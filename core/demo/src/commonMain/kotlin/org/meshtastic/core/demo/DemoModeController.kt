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
package org.meshtastic.core.demo

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.demo.data.DemoDataSet
import org.meshtastic.core.demo.store.DemoStore
import org.meshtastic.core.di.CoroutineDispatchers
import org.meshtastic.core.repository.DemoMode
import org.meshtastic.core.repository.PacketRepository
import org.meshtastic.core.repository.UiPrefs

/**
 * Demo mode's switch, in memory only: a new process always starts with it off.
 *
 * [realPackets] and [realUiPrefs] are the real implementations. The first is only read, to count real messages arriving
 * during the demo; the second seeds the demo's own Terrain/PC choice, which is then kept apart from it.
 */
class DemoModeController(
    private val store: DemoStore,
    private val realPackets: PacketRepository,
    private val realUiPrefs: UiPrefs,
    dispatchers: CoroutineDispatchers,
) : DemoMode {
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.default)
    private var counting: Job? = null

    private val active = MutableStateFlow(false)
    override val isActive: StateFlow<Boolean> = active.asStateFlow()

    private val realMessages = MutableStateFlow(0)
    override val realMessagesSinceActivation: StateFlow<Int> = realMessages.asStateFlow()

    override fun activate() {
        if (active.value) return
        // Data first, switch second: a screen that reads right after the switch already finds the demo data.
        store.load(DemoDataSet.create(nowMillis), commandPostMode = realUiPrefs.commandPostMode.value)
        realMessages.value = 0
        counting = scope.launch { countRealMessages() }
        active.value = true
    }

    override fun deactivate() {
        if (!active.value) return
        // Switch first, data second: no screen reads an emptied demo store while still on it.
        active.value = false
        counting?.cancel()
        counting = null
        realMessages.value = 0
        store.clear()
    }

    /**
     * Every rise of the real unread total is a real message that arrived unread. Nothing of the real conversations is
     * opened during a demo, so the total only falls when a notification's own action marks something read; a fall is
     * not a message and is not counted.
     */
    private suspend fun countRealMessages() {
        var previous: Int? = null
        realPackets.getUnreadCountTotal().collect { total ->
            val last = previous
            if (last != null && total > last) realMessages.update { it + (total - last) }
            previous = total
        }
    }
}
