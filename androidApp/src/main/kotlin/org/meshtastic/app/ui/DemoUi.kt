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
package org.meshtastic.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import org.koin.compose.koinInject
import org.meshtastic.app.MAP_AVAILABLE_IN_DEMO
import org.meshtastic.core.demo.DemoRoutes
import org.meshtastic.core.repository.DemoMode
import org.meshtastic.core.ui.component.demoGatedEntryProvider

/** What the app's frame needs from demo mode: whether it is on, and the gate on the screens it does not cover. */
@Stable
class DemoUi(private val demoMode: DemoMode, val active: State<Boolean>) {
    fun gate(entryProvider: (key: NavKey) -> NavEntry<NavKey>): (key: NavKey) -> NavEntry<NavKey> =
        demoGatedEntryProvider(
            entryProvider = entryProvider,
            demoActive = active,
            isAvailableInDemo = { key -> DemoRoutes.isAvailable(key, mapAvailable = MAP_AVAILABLE_IN_DEMO) },
            onExitDemo = demoMode::deactivate,
        )
}

@Composable
fun rememberDemoUi(): DemoUi {
    val demoMode: DemoMode = koinInject()
    val active = demoMode.isActive.collectAsStateWithLifecycle()
    return remember(demoMode, active) { DemoUi(demoMode, active) }
}
