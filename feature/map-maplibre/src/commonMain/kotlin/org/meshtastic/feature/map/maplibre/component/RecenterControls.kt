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
package org.meshtastic.feature.map.maplibre.component

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.maplibre.compose.camera.CameraAnimation
import org.maplibre.compose.camera.CameraUpdate
import org.maplibre.compose.map.MapState
import org.maplibre.spatialk.geojson.Position
import org.meshtastic.core.common.util.DateFormatter
import org.meshtastic.core.common.util.nowSeconds
import org.meshtastic.core.model.geo.BroadcastCheck
import org.meshtastic.core.model.geo.LatLon
import org.meshtastic.core.repository.RecenterTarget
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.cancel
import org.meshtastic.core.resources.recenter_button
import org.meshtastic.core.resources.recenter_confirm
import org.meshtastic.core.resources.recenter_confirm_old
import org.meshtastic.core.resources.recenter_confirm_text
import org.meshtastic.core.resources.recenter_confirm_title
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.MyLocation
import org.meshtastic.feature.map.recenter.RecenterViewModel
import kotlin.time.Duration.Companion.seconds

/** Whether the "Recaler" card is open, and the broadcast confirmation with it. */
@Stable
internal class RecenterControls(
    private val viewModel: RecenterViewModel,
    private val mapState: MapState,
    private val scope: CoroutineScope,
) {
    var open by mutableStateOf(false)
        private set

    var confirming by mutableStateOf<BroadcastCheck.Ready?>(null)

    /** Opens the card and centres the map on the chosen position, at least at [DETAIL_ZOOM]. */
    fun recenter() {
        open = true
        scope.launch { centreOn(viewModel.refresh().centre) }
    }

    fun changeTarget(target: RecenterTarget) {
        viewModel.setTarget(target)
        scope.launch { centreOn(viewModel.refresh().centre) }
    }

    fun dismiss() {
        open = false
        confirming = null
    }

    private suspend fun centreOn(point: LatLon?) {
        point ?: return
        mapState.animateCamera(
            CameraUpdate(
                target = Position(longitude = point.longitude, latitude = point.latitude),
                zoom = maxOf(DETAIL_ZOOM, mapState.cameraPosition.zoom),
            ),
            CameraAnimation.Ease(),
        )
    }
}

@Composable
internal fun rememberRecenterControls(mapState: MapState): RecenterControls {
    val viewModel: RecenterViewModel = koinViewModel()
    val scope = rememberCoroutineScope()
    return remember(viewModel, mapState) { RecenterControls(viewModel, mapState, scope) }
}

/** The "Recaler" button, at the top end of the map under the toolbar; available in demo mode too. */
@Composable
internal fun BoxScope.RecenterButton(controls: RecenterControls) {
    ExtendedFloatingActionButton(
        onClick = controls::recenter,
        icon = { Icon(MeshtasticIcons.MyLocation, contentDescription = null) },
        text = { Text(stringResource(Res.string.recenter_button)) },
        modifier = Modifier.align(Alignment.TopEnd).padding(top = BUTTON_TOP.dp, end = 8.dp),
    )
}

/**
 * The card while it is open, refreshed every second (ages, countdown, the phone's position). A broadcast is only made
 * from the confirmation, which warns about air time and, for an old position, says how old it is.
 */
@Composable
internal fun RecenterSlot(controls: RecenterControls) {
    if (!controls.open) return
    val viewModel: RecenterViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var now by remember { mutableStateOf(nowSeconds) }
    LaunchedEffect(Unit) {
        while (true) {
            now = nowSeconds
            viewModel.refresh()
            delay(1.seconds)
        }
    }
    val current = state ?: return
    RecenterCard(
        state = current,
        nowEpochSeconds = now,
        onTargetChange = controls::changeTarget,
        onBroadcast = { controls.confirming = current.broadcast as? BroadcastCheck.Ready },
        onDismiss = controls::dismiss,
    )
    controls.confirming?.let { ready ->
        BroadcastConfirmation(
            ready = ready,
            onConfirm = {
                controls.confirming = null
                scope.launch { viewModel.broadcast() }
            },
            onDismiss = { controls.confirming = null },
        )
    }
}

@Composable
private fun BroadcastConfirmation(ready: BroadcastCheck.Ready, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.recenter_confirm_title)) },
        text = {
            Column {
                Text(
                    stringResource(
                        Res.string.recenter_confirm_text,
                        DateFormatter.formatTime(ready.fixEpochSeconds * MILLIS_PER_SECOND),
                    ),
                )
                if (ready.old) {
                    Text(
                        stringResource(Res.string.recenter_confirm_old, formatAge(ready.age)),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(Res.string.recenter_confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
    )
}

/** Below the toolbar, which is centred along the top edge. */
private const val BUTTON_TOP = 72
private const val MILLIS_PER_SECOND = 1000L
