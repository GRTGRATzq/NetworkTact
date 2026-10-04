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
package org.meshtastic.core.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.koin.compose.koinInject
import org.meshtastic.core.model.noiseFloorOrNull
import org.meshtastic.core.navigation.MultiBackstack
import org.meshtastic.core.navigation.NodeDetailRoute
import org.meshtastic.core.navigation.NodesRoute
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.repository.RadioConfigRepository
import org.meshtastic.core.ui.util.LocalMeshActivity
import org.meshtastic.core.ui.util.LocalModemPreset
import org.meshtastic.core.ui.util.LocalNoiseFloor
import org.meshtastic.core.ui.viewmodel.UIViewModel

/**
 * Shared shell for setting up global UI logic across platforms (Android, Desktop).
 *
 * This component handles deep linking, shared dialogs (via [MeshtasticCommonAppSetup]), and provides the global
 * [MeshtasticSnackbarProvider]. Platform entry points should wrap their navigation layout inside this shell.
 */
@Composable
fun MeshtasticAppShell(
    multiBackstack: MultiBackstack,
    uiViewModel: UIViewModel,
    hostModifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    LaunchedEffect(uiViewModel) {
        uiViewModel.navigationDeepLink.collect { navKeys ->
            multiBackstack.handleDeepLink(navKeys)
            uiViewModel.onDeepLinkHandled()
        }
    }

    MeshtasticCommonAppSetup(
        uiViewModel = uiViewModel,
        onNavigateToTracerouteMap = { destNum, requestId, logUuid ->
            multiBackstack.handleDeepLink(
                listOf(
                    NodesRoute.Nodes,
                    NodeDetailRoute.TracerouteMap(destNum = destNum, requestId = requestId, logUuid = logUuid),
                ),
            )
        },
    )

    // Connected device's modem preset, provided once here so signal-quality rating is preset-relative across all
    // screens without per-screen plumbing. Distinct so the value only changes when the preset itself does.
    val radioConfigRepository = koinInject<RadioConfigRepository>()
    val modemPreset by
        remember(radioConfigRepository) {
            radioConfigRepository.localConfigFlow.map { it.lora?.modem_preset }.distinctUntilChanged()
        }
            .collectAsStateWithLifecycle(initialValue = null)

    // Connected device's own noise floor (dBm, from its LocalStats telemetry), provided once here so signal-quality
    // rating can blend it in (design#15) without per-screen plumbing. See [noiseFloorOrNull] for the "no reading yet"
    // sentinel normalization.
    val nodeRepository = koinInject<NodeRepository>()
    val noiseFloor by
        remember(nodeRepository) { nodeRepository.localStats.map { it.noiseFloorOrNull }.distinctUntilChanged() }
            .collectAsStateWithLifecycle(initialValue = null)

    val demoActive by uiViewModel.demoActive.collectAsStateWithLifecycle()
    val realMessagesSinceDemo by uiViewModel.realMessagesSinceDemo.collectAsStateWithLifecycle()
    DemoTabsReset(multiBackstack = multiBackstack, demoActive = demoActive)

    MeshtasticSnackbarProvider(snackbarManager = uiViewModel.snackbarManager, hostModifier = hostModifier) {
        // Provide the activity FLOW (stable ref) — not a collected value — so it costs no recomposition; only the
        // local-node connection badge collects it, animating in the draw phase. See LocalMeshActivity.
        CompositionLocalProvider(
            LocalModemPreset provides modemPreset,
            LocalNoiseFloor provides noiseFloor,
            LocalMeshActivity provides uiViewModel.meshActivity,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (demoActive) DemoBanner(realMessages = realMessagesSinceDemo, onExit = uiViewModel::exitDemo)
                Box(
                    // The banner already sits under the status bar: the screens below must not pad for it again.
                    modifier =
                    Modifier.weight(1f)
                        .then(if (demoActive) Modifier.consumeWindowInsets(WindowInsets.statusBars) else Modifier),
                ) {
                    content()
                }
            }
        }
    }
}

/**
 * Switching demo mode on or off puts every tab back on its root, so that no screen opened on one side stays on screen
 * on the other. In demo mode the node list is unavailable, so the Nodes tab starts on the command post view.
 */
@Composable
private fun DemoTabsReset(multiBackstack: MultiBackstack, demoActive: Boolean) {
    var shownFor by remember { mutableStateOf(demoActive) }
    LaunchedEffect(demoActive) {
        if (demoActive == shownFor) return@LaunchedEffect
        shownFor = demoActive
        val startPaths =
            if (demoActive) {
                mapOf<NavKey, List<NavKey>>(NodesRoute.Nodes to listOf(NodesRoute.Nodes, NodesRoute.CommandPost))
            } else {
                emptyMap()
            }
        multiBackstack.resetAllTabs(startPaths)
    }
}
