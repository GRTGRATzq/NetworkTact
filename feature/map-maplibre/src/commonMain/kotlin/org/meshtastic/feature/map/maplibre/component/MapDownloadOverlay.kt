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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.maplibre.compose.camera.CameraAnimation
import org.maplibre.compose.camera.CameraUpdate
import org.maplibre.compose.map.MapState
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.offline_map_download_corner
import org.meshtastic.core.ui.util.KeepScreenOn
import org.meshtastic.feature.map.SharedMapViewModel
import org.meshtastic.feature.map.offline.extract.DownloadArea
import org.meshtastic.feature.map.offline.extract.DownloadFrame
import org.meshtastic.feature.map.offline.extract.FrameCorner
import org.meshtastic.feature.map.offline.extract.MapDownloadController
import org.meshtastic.feature.map.offline.extract.MapDownloadState
import kotlin.math.roundToInt

/** The download frame and panel over the map, while the controller says the frame is open. */
@Composable
internal fun BoxScope.MapDownloadSlot(controller: MapDownloadController, mapState: MapState) {
    val open by controller.frameOpen.collectAsStateWithLifecycle()
    if (!open) return
    val viewModel: SharedMapViewModel = koinViewModel()
    val demo by viewModel.isDemoActive.collectAsStateWithLifecycle()
    MapDownloadOverlay(controller = controller, mapState = mapState, demo = demo, onClose = controller::closeFrame)
}

/**
 * The download screen (docs/PLAN-CARTE-TELECHARGEMENT.md §6): a frame over the map, resized by its four corners, and a
 * panel with its size in km, the maximum zoom, the size (rough, then exact), the source and what it can see, and the
 * download itself. The map stays usable under the frame, which keeps its place on the screen: the user pans and zooms
 * to choose the area. The map is put back north up and flat, without which the frame would not be a rectangle on the
 * ground.
 */
@Composable
private fun BoxScope.MapDownloadOverlay(
    controller: MapDownloadController,
    mapState: MapState,
    demo: Boolean,
    onClose: () -> Unit,
) {
    LaunchedEffect(Unit) { mapState.animateCamera(CameraUpdate(bearing = 0.0, tilt = 0.0), CameraAnimation.Ease()) }
    val state by controller.state.collectAsStateWithLifecycle()
    KeepScreenOn(state is MapDownloadState.Downloading)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val mapWidth = maxWidth.value
        val frameHeight = maxHeight.value * FRAME_AREA_SHARE
        var frame by remember(mapWidth, frameHeight) { mutableStateOf(initialFrame(mapWidth, frameHeight)) }
        // Read so the bounds follow the map as it moves under the frame.
        val camera = mapState.cameraPosition
        val bounds =
            remember(frame, camera) {
                DownloadArea.bounds(
                    mapState.positionFromScreenLocation(DpOffset(frame.left.dp, frame.top.dp))?.let {
                        it.longitude to it.latitude
                    },
                    mapState.positionFromScreenLocation(DpOffset(frame.right.dp, frame.bottom.dp))?.let {
                        it.longitude to it.latitude
                    },
                )
            }

        FrameOverlay(frame)
        FrameCorner.entries.forEach { corner ->
            CornerHandle(frame, corner) { dx, dy ->
                frame = frame.dragCorner(corner, dx, dy, mapWidth, frameHeight, MIN_FRAME_DP)
            }
        }

        DownloadPanel(
            controller = controller,
            state = state,
            bounds = bounds,
            demo = demo,
            onUseView = { frame = DownloadFrame.wholeView(mapWidth, frameHeight) },
            onClose = onClose,
            modifier =
            Modifier.align(Alignment.BottomCenter)
                .fillMaxWidth()
                .heightIn(max = maxHeight * (1 - FRAME_AREA_SHARE))
                .padding(8.dp),
        )
    }
}

private fun initialFrame(width: Float, height: Float) = DownloadFrame.initial(width, height)

/** Dims the map outside the frame and outlines it. */
@Composable
private fun FrameOverlay(frame: DownloadFrame) {
    val outline = MaterialTheme.colorScheme.primary
    val density = LocalDensity.current.density
    Canvas(modifier = Modifier.fillMaxSize()) {
        val left = frame.left * density
        val top = frame.top * density
        val right = frame.right * density
        val bottom = frame.bottom * density
        val shade = Color.Black.copy(alpha = SHADE_ALPHA)
        drawRect(shade, Offset.Zero, Size(size.width, top))
        drawRect(shade, Offset(0f, bottom), Size(size.width, size.height - bottom))
        drawRect(shade, Offset(0f, top), Size(left, bottom - top))
        drawRect(shade, Offset(right, top), Size(size.width - right, bottom - top))
        drawRect(outline, Offset(left, top), Size(right - left, bottom - top), style = Stroke(OUTLINE_DP * density))
    }
}

/** A 48 dp touch target centred on [corner], drawn as a dot; dragging it moves that corner only. */
@Composable
private fun CornerHandle(frame: DownloadFrame, corner: FrameCorner, onDrag: (Float, Float) -> Unit) {
    val density = LocalDensity.current.density
    val x =
        when (corner) {
            FrameCorner.TopLeft,
            FrameCorner.BottomLeft,
            -> frame.left

            else -> frame.right
        }
    val y =
        when (corner) {
            FrameCorner.TopLeft,
            FrameCorner.TopRight,
            -> frame.top

            else -> frame.bottom
        }
    val label = stringResource(Res.string.offline_map_download_corner)
    Box(
        modifier =
        Modifier.offset {
            IntOffset(
                ((x - HANDLE_DP / 2) * density).roundToInt(),
                ((y - HANDLE_DP / 2) * density).roundToInt(),
            )
        }
            .size(HANDLE_DP.dp)
            .semantics { contentDescription = label }
            .pointerInput(corner) {
                detectDragGestures { change, drag ->
                    change.consume()
                    onDrag(drag.x / density, drag.y / density)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
            Modifier.size(DOT_DP.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.onPrimary, CircleShape),
        )
    }
}

/** Share of the map's height the frame may use; the panel takes the rest. */
private const val FRAME_AREA_SHARE = 0.55f
private const val MIN_FRAME_DP = 48f
private const val HANDLE_DP = 48f
private const val DOT_DP = 18
private const val OUTLINE_DP = 2f
private const val SHADE_ALPHA = 0.35f
