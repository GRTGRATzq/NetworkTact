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
package org.meshtastic.core.ui.freshness

import androidx.compose.runtime.compositionLocalOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import org.meshtastic.core.common.util.nowSeconds
import org.meshtastic.core.model.freshness.FreshnessSettings
import org.meshtastic.core.model.freshness.FreshnessThresholds
import org.meshtastic.core.model.freshness.NetworkFreshness
import org.meshtastic.core.model.freshness.NetworkFreshnessState
import org.meshtastic.core.repository.FreshnessPrefs
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.repository.RadioConfigRepository
import org.meshtastic.core.repository.SCREEN_DATA

/**
 * NetworkTact: the threshold in force for other radios' positions and contacts ([NetworkFreshness]), from the freshness
 * settings, the nodes heard and my radio's preset, all read through the screens' sources so a demo uses its own. Every
 * screen that judges another radio's freshness reads it here, so they all agree.
 */
@Single
class NetworkFreshnessSource(
    @Named(SCREEN_DATA) private val nodeRepository: NodeRepository,
    @Named(SCREEN_DATA) private val radioConfigRepository: RadioConfigRepository,
    @Named(SCREEN_DATA) private val freshnessPrefs: FreshnessPrefs,
) {
    /** The state at each tick of [nowSeconds], and whenever the nodes, the preset or the settings change. */
    fun state(nowSeconds: Flow<Long>): Flow<NetworkFreshnessState> = combine(
        freshnessPrefs.settings,
        nodeRepository.nodeDBbyNum,
        radioConfigRepository.localConfigFlow,
        nowSeconds,
    ) { settings, nodes, config, now ->
        NetworkFreshness.state(settings, nodes.values, config.lora, now)
    }

    /** Saves new settings; the callers check command post mode first. */
    fun setSettings(settings: FreshnessSettings) = freshnessPrefs.setSettings(settings)
}

/** This phone's clock in epoch seconds, now and then every [FreshnessThresholds.refreshInterval]. */
fun freshnessTicks(clockSeconds: () -> Long = { nowSeconds }): Flow<Long> = flow {
    while (true) {
        emit(clockSeconds())
        delay(FreshnessThresholds.Default.refreshInterval)
    }
}

/**
 * The threshold in force, provided once by the app shell for the screens that compute freshness in composition (map,
 * node detail), like [org.meshtastic.core.ui.util.LocalModemPreset], rather than threaded through every map and detail
 * composable. Before anything is known, and in previews: the 35-minute default.
 */
@Suppress("CompositionLocalAllowlist")
val LocalNetworkFreshness = compositionLocalOf { NetworkFreshness.DEFAULT_STATE }
