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
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.map_point_hide
import org.meshtastic.core.ui.icon.Close
import org.meshtastic.core.ui.icon.Map
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.util.MapFocusPoint
import org.meshtastic.core.ui.util.MapFocusRequests
import org.meshtastic.feature.map.SharedMapViewModel

/**
 * The cards over the foot of the main map: the point another screen asked to show, then the node whose chip was tapped.
 * Stacked, so neither hides the other; clear of the zoom buttons and the attribution along the bottom edge.
 */
@Composable
internal fun BoxScope.MapFootCards(
    focusRequests: MapFocusRequests,
    selectedNode: Int?,
    onNodeDetails: (Int) -> Unit,
    onNodeDismiss: () -> Unit,
) {
    val marked by focusRequests.marked.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier.align(Alignment.BottomCenter).padding(start = 16.dp, end = 16.dp, bottom = 72.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MarkedPointBanner(point = marked, onClear = focusRequests::clearMark)
        NodePositionCardSlot(nodeNum = selectedNode, onDetails = onNodeDetails, onDismiss = onNodeDismiss)
    }
}

/**
 * What the marked point is and when it was taken, or that its time is unknown, over the foot of the map, with a button
 * that removes the marker. Compose text rather than a map label, so it reads the same over any basemap, offline
 * included.
 */
@Composable
private fun MarkedPointBanner(point: MapFocusPoint?, onClear: () -> Unit) {
    val label = point?.label ?: return
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = MeshtasticIcons.Map,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f, fill = false),
            )
            IconButton(onClick = onClear) {
                Icon(
                    imageVector = MeshtasticIcons.Close,
                    contentDescription = stringResource(Res.string.map_point_hide),
                )
            }
        }
    }
}

/** The card of the node whose chip was tapped, while that node is still on the map. */
@Composable
private fun NodePositionCardSlot(nodeNum: Int?, onDetails: (Int) -> Unit, onDismiss: () -> Unit) {
    nodeNum ?: return
    val viewModel: SharedMapViewModel = koinViewModel()
    val nodes by viewModel.nodesWithPosition.collectAsStateWithLifecycle()
    val node = nodes.firstOrNull { it.num == nodeNum } ?: return
    NodePositionCard(node = node, onDetails = { onDetails(nodeNum) }, onDismiss = onDismiss)
}
