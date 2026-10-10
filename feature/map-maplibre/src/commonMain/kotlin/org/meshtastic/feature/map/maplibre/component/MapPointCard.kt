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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.geo.CoordinateFormat
import org.meshtastic.core.model.geo.MapPointCoordinate
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.close
import org.meshtastic.core.resources.coords_not_covered
import org.meshtastic.core.resources.map_point_card_hint
import org.meshtastic.core.resources.map_point_card_title
import org.meshtastic.core.resources.map_point_copied
import org.meshtastic.core.resources.map_point_copy
import org.meshtastic.core.resources.map_point_create_waypoint
import org.meshtastic.core.resources.map_point_insert
import org.meshtastic.core.resources.map_point_open_converter
import org.meshtastic.core.ui.icon.Close
import org.meshtastic.core.ui.icon.Copy
import org.meshtastic.core.ui.icon.Edit
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.PinDrop
import org.meshtastic.core.ui.icon.Place
import org.meshtastic.core.ui.icon.Send

/**
 * The coordinate of the point long-pressed on the map, in MGRS to 1 m, UTM and DMS, with what can be done with it.
 * Local display only: nothing is sent from here, and the point carries no time since it is a place, not a GPS position.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MapPointCard(point: MapPointCoordinate, copied: Boolean, actions: MapPointActions) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 3.dp,
        modifier = Modifier.widthIn(max = CARD_MAX_WIDTH.dp),
    ) {
        Column(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = MeshtasticIcons.Place,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    stringResource(Res.string.map_point_card_title),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = actions.onDismiss) {
                    Icon(MeshtasticIcons.Close, contentDescription = stringResource(Res.string.close))
                }
            }
            SelectionContainer {
                Column(modifier = Modifier.padding(end = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    CoordinateFormat.entries.forEach { format ->
                        CoordinateLine(format, point.formatted.format(format))
                    }
                }
            }
            Text(
                stringResource(Res.string.map_point_card_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp),
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val copyLabel = if (copied) Res.string.map_point_copied else Res.string.map_point_copy
                PointAction(MeshtasticIcons.Copy, stringResource(copyLabel), actions.onCopy)
                PointAction(MeshtasticIcons.Send, stringResource(Res.string.map_point_insert), actions.onInsert)
                PointAction(
                    MeshtasticIcons.Edit,
                    stringResource(Res.string.map_point_open_converter),
                    actions.onOpenConverter,
                )
                actions.onCreateWaypoint?.let { onCreate ->
                    PointAction(MeshtasticIcons.PinDrop, stringResource(Res.string.map_point_create_waypoint), onCreate)
                }
            }
        }
    }
}

@Composable
private fun CoordinateLine(format: CoordinateFormat, value: String?) {
    Column {
        Text(
            format.name,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value ?: stringResource(Res.string.coords_not_covered),
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = if (value != null) FontFamily.Monospace else null,
        )
    }
}

@Composable
private fun PointAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Text(label, modifier = Modifier.padding(start = 6.dp))
    }
}

private const val CARD_MAX_WIDTH = 480
