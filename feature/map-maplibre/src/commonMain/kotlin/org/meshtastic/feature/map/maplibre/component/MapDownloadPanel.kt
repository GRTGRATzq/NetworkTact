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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.util.NumberFormatter
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.cancel
import org.meshtastic.core.resources.close
import org.meshtastic.core.resources.offline_map_download
import org.meshtastic.core.resources.offline_map_download_area
import org.meshtastic.core.resources.offline_map_download_cancelled
import org.meshtastic.core.resources.offline_map_download_compute
import org.meshtastic.core.resources.offline_map_download_confirm_size
import org.meshtastic.core.resources.offline_map_download_discard
import org.meshtastic.core.resources.offline_map_download_downloading
import org.meshtastic.core.resources.offline_map_download_error_tiny
import org.meshtastic.core.resources.offline_map_download_exact
import org.meshtastic.core.resources.offline_map_download_exact_file
import org.meshtastic.core.resources.offline_map_download_max_zoom
import org.meshtastic.core.resources.offline_map_download_name
import org.meshtastic.core.resources.offline_map_download_paused
import org.meshtastic.core.resources.offline_map_download_pending
import org.meshtastic.core.resources.offline_map_download_resume
import org.meshtastic.core.resources.offline_map_download_rough
import org.meshtastic.core.resources.offline_map_download_start
import org.meshtastic.core.resources.offline_map_download_use_view
import org.meshtastic.core.resources.offline_map_imported
import org.meshtastic.core.resources.offline_map_progress
import org.meshtastic.core.resources.okay
import org.meshtastic.core.ui.component.MeshtasticDialog
import org.meshtastic.feature.map.offline.extract.AreaCheck
import org.meshtastic.feature.map.offline.extract.DownloadArea
import org.meshtastic.feature.map.offline.extract.GeoBounds
import org.meshtastic.feature.map.offline.extract.MapDownloadController
import org.meshtastic.feature.map.offline.extract.MapDownloadEstimate
import org.meshtastic.feature.map.offline.extract.MapDownloadState
import org.meshtastic.feature.map.offline.extract.MapSource
import org.meshtastic.feature.map.offline.extract.PendingDownload
import org.meshtastic.feature.map.offline.extract.SizeEstimate

/**
 * The panel under the download frame: what the source can see, the source itself, the area's size, the maximum zoom,
 * the size (rough, then exact), and the download with its progress or outcome (docs/PLAN-CARTE-TELECHARGEMENT.md §6).
 */
@Composable
internal fun DownloadPanel(
    controller: MapDownloadController,
    state: MapDownloadState,
    bounds: GeoBounds?,
    demo: Boolean,
    onUseView: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val source by controller.source.collectAsStateWithLifecycle()
    val pending by controller.pending.collectAsStateWithLifecycle()
    var maxZoom by remember { mutableStateOf(DEFAULT_MAX_ZOOM) }
    var name by remember { mutableStateOf("") }
    var editingSource by remember { mutableStateOf(false) }
    var confirmSize by remember { mutableStateOf<Long?>(null) }
    val check = DownloadArea.check(bounds)
    val busy = state is MapDownloadState.Estimating || state is MapDownloadState.Downloading
    val estimate =
        (state as? MapDownloadState.Estimated)?.estimate?.takeIf { it.bounds == bounds && it.maxZoom == maxZoom }
    val mapName = name.ifEmpty { defaultMapName(bounds, maxZoom) }

    Card(modifier = modifier) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SourceLines(host = source.host, enabled = !busy) { editingSource = true }
            AreaLine(bounds, check)
            ZoomChips(maxZoom = maxZoom, enabled = !busy) { maxZoom = it }
            SizeLines(bounds = bounds.takeIf { check == AreaCheck.Ok }, maxZoom = maxZoom, estimate = estimate)
            StateLines(state, source.host, controller)
            if (state !is MapDownloadState.Downloading && state !is MapDownloadState.Paused) {
                pending?.let { PendingLines(it, controller, enabled = !busy && !demo) }
            }
            if (estimate != null && !busy) {
                OutlinedTextField(
                    value = mapName,
                    onValueChange = { name = it },
                    label = { Text(stringResource(Res.string.offline_map_download_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Actions(
                busy = busy,
                canCompute = !busy && !demo && check == AreaCheck.Ok && estimate == null,
                canStart = !busy && !demo && estimate != null,
                canClose = state !is MapDownloadState.Downloading,
                onUseView = onUseView,
                onCompute = { bounds?.let { controller.estimate(it, maxZoom) } },
                onStart = {
                    val size = estimate?.fileSize ?: 0L
                    if (size > CONFIRM_ABOVE_BYTES) confirmSize = size else controller.start(mapName)
                },
                onClose = {
                    controller.clearOutcome()
                    onClose()
                },
            )
        }
    }

    PanelDialogs(
        confirmSize = confirmSize,
        editingSource = editingSource,
        source = source,
        onConfirm = {
            confirmSize = null
            controller.start(mapName)
        },
        onDismissConfirm = { confirmSize = null },
        onSourcePick = { picked ->
            editingSource = false
            picked?.let(controller::setSource)
        },
    )
}

/** The size confirmation above 50 MB (D5) and the source address dialog. */
@Composable
private fun PanelDialogs(
    confirmSize: Long?,
    editingSource: Boolean,
    source: MapSource,
    onConfirm: () -> Unit,
    onDismissConfirm: () -> Unit,
    onSourcePick: (MapSource?) -> Unit,
) {
    if (confirmSize != null) {
        MeshtasticDialog(
            title = stringResource(Res.string.offline_map_download),
            message = stringResource(Res.string.offline_map_download_confirm_size, megabytes(confirmSize)),
            confirmText = stringResource(Res.string.offline_map_download_start),
            onConfirm = onConfirm,
            dismissText = stringResource(Res.string.cancel),
            onDismiss = onDismissConfirm,
        )
    }
    if (editingSource) SourceDialog(current = source, onDone = onSourcePick)
}

@Composable
private fun AreaLine(bounds: GeoBounds?, check: AreaCheck) {
    if (bounds == null || check != AreaCheck.Ok) {
        Text(
            text = stringResource(Res.string.offline_map_download_error_tiny),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
    } else {
        Text(
            text =
            stringResource(
                Res.string.offline_map_download_area,
                NumberFormatter.format(DownloadArea.widthMeters(bounds) / METERS_PER_KM, 1),
                NumberFormatter.format(DownloadArea.heightMeters(bounds) / METERS_PER_KM, 1),
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/** The maximum zoom, 12 to 15 (Protomaps stops at 15), 15 by default (D4). */
@Composable
private fun ZoomChips(maxZoom: Int, enabled: Boolean, onPick: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = stringResource(Res.string.offline_map_download_max_zoom),
            style = MaterialTheme.typography.labelMedium,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (zoom in MIN_MAX_ZOOM..DEFAULT_MAX_ZOOM) {
                FilterChip(
                    selected = zoom == maxZoom,
                    onClick = { onPick(zoom) },
                    enabled = enabled,
                    label = { Text(zoom.toString()) },
                )
            }
        }
    }
}

/** The rough size at once (no network), the exact one once calculated for this area and zoom. */
@Composable
private fun SizeLines(bounds: GeoBounds?, maxZoom: Int, estimate: MapDownloadEstimate?) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (bounds != null) {
            val rough = remember(bounds, maxZoom) { SizeEstimate.rough(bounds, 0, maxZoom) }
            Text(
                text =
                stringResource(Res.string.offline_map_download_rough, megabytes(rough.low), megabytes(rough.high)),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (estimate != null) {
            val size = megabytes(estimate.fileSize)
            val tiles = NumberFormatter.format(estimate.tiles.toDouble(), 0)
            val date = estimate.editionDate
            Text(
                text =
                if (date == null) {
                    stringResource(Res.string.offline_map_download_exact_file, size, tiles)
                } else {
                    stringResource(Res.string.offline_map_download_exact, size, tiles, isoDate(date))
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/** What is running or how it ended. */
@Composable
private fun StateLines(state: MapDownloadState, host: String, controller: MapDownloadController) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        when (state) {
            MapDownloadState.Estimating -> Computing()

            is MapDownloadState.Downloading -> Progress(state, controller::cancel)

            is MapDownloadState.Paused -> {
                Text(stringResource(Res.string.offline_map_download_paused, state.name))
                ResumeButtons(controller, enabled = true)
            }

            is MapDownloadState.Done -> Outcome(stringResource(Res.string.offline_map_imported, state.name), controller)

            MapDownloadState.Cancelled -> Outcome(stringResource(Res.string.offline_map_download_cancelled), controller)

            is MapDownloadState.Failed -> Outcome(failureText(state, host), controller, error = true)

            MapDownloadState.Idle,
            is MapDownloadState.Estimated,
            -> Unit
        }
    }
}

@Composable
private fun Progress(state: MapDownloadState.Downloading, onCancel: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(Res.string.offline_map_download_downloading, state.name))
        val total = state.bytesTotal.coerceAtLeast(1)
        LinearProgressIndicator(
            progress = { (state.bytesDone.toFloat() / total).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text =
            stringResource(
                Res.string.offline_map_progress,
                megabytes(state.bytesDone),
                megabytes(state.bytesTotal),
            ),
            style = MaterialTheme.typography.bodySmall,
        )
        OutlinedButton(onClick = onCancel) { Text(stringResource(Res.string.cancel)) }
    }
}

/** A download stopped by a cut, possibly in an earlier run: resume or discard it. */
@Composable
private fun PendingLines(pending: PendingDownload, controller: MapDownloadController, enabled: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val percent = if (pending.rangesTotal > 0) pending.rangesDone * PERCENT / pending.rangesTotal else 0
        Text(text = stringResource(Res.string.offline_map_download_pending, pending.name, "$percent %"))
        ResumeButtons(controller, enabled)
    }
}

@Composable
private fun ResumeButtons(controller: MapDownloadController, enabled: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = controller::resume, enabled = enabled) {
            Text(stringResource(Res.string.offline_map_download_resume))
        }
        OutlinedButton(onClick = controller::discardPending, enabled = enabled) {
            Text(stringResource(Res.string.offline_map_download_discard))
        }
    }
}

@Composable
private fun Outcome(text: String, controller: MapDownloadController, error: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = text, color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
        OutlinedButton(onClick = controller::clearOutcome) { Text(stringResource(Res.string.okay)) }
    }
}

@Suppress("LongParameterList")
@Composable
private fun Actions(
    busy: Boolean,
    canCompute: Boolean,
    canStart: Boolean,
    canClose: Boolean,
    onUseView: () -> Unit,
    onCompute: () -> Unit,
    onStart: () -> Unit,
    onClose: () -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onUseView, enabled = !busy) {
            Text(stringResource(Res.string.offline_map_download_use_view))
        }
        OutlinedButton(onClick = onCompute, enabled = canCompute) {
            Text(stringResource(Res.string.offline_map_download_compute))
        }
        Button(onClick = onStart, enabled = canStart) { Text(stringResource(Res.string.offline_map_download_start)) }
        TextButton(onClick = onClose, enabled = canClose) { Text(stringResource(Res.string.close)) }
    }
}

private const val PROGRESS_DP = 20
private const val MIN_MAX_ZOOM = 12
private const val DEFAULT_MAX_ZOOM = 15
private const val PERCENT = 100
private const val METERS_PER_KM = 1_000.0

/** D5: above 50 MB, the same on Wi-Fi and mobile data. */
private const val CONFIRM_ABOVE_BYTES = 50_000_000L
