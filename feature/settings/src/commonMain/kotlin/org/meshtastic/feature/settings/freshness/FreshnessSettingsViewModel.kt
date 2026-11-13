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
package org.meshtastic.feature.settings.freshness

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.annotation.KoinViewModel
import org.koin.core.annotation.Named
import org.meshtastic.core.common.util.nowSeconds
import org.meshtastic.core.model.freshness.FreshnessSettings
import org.meshtastic.core.model.freshness.NetworkFreshness
import org.meshtastic.core.model.freshness.NetworkFreshnessState
import org.meshtastic.core.repository.SCREEN_DATA
import org.meshtastic.core.repository.UiPrefs
import org.meshtastic.core.ui.freshness.NetworkFreshnessSource
import org.meshtastic.core.ui.freshness.freshnessTicks
import org.meshtastic.core.ui.viewmodel.stateInWhileSubscribed

/**
 * NetworkTact: the "Freshness of positions" card of the settings. Shown to everyone with the threshold in force;
 * changed in command post mode only. Stored on this phone, nothing is sent.
 */
@KoinViewModel
class FreshnessSettingsViewModel(
    private val freshnessSource: NetworkFreshnessSource,
    @Named(SCREEN_DATA) private val uiPrefs: UiPrefs,
    clockSeconds: () -> Long = { nowSeconds },
) : ViewModel() {

    val state: StateFlow<NetworkFreshnessState> =
        freshnessSource
            .state(freshnessTicks(clockSeconds))
            .stateInWhileSubscribed(initialValue = NetworkFreshness.DEFAULT_STATE)

    val commandPostMode: StateFlow<Boolean> = uiPrefs.commandPostMode

    /** Saves [settings]; refused outside command post mode. */
    fun update(settings: FreshnessSettings): Boolean {
        if (!uiPrefs.commandPostMode.value) return false
        freshnessSource.setSettings(settings)
        return true
    }
}
