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
package org.meshtastic.feature.messaging.coordinates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.geo.LatLon
import org.meshtastic.core.model.geo.SharedPoint
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.coords_share_action
import org.meshtastic.core.resources.coords_share_body
import org.meshtastic.core.resources.coords_share_demo
import org.meshtastic.core.resources.coords_share_dismiss
import org.meshtastic.core.resources.coords_share_title
import org.meshtastic.core.ui.theme.AppTheme

/**
 * Offered after a message holding a coordinate has been sent: the point goes on the map only if the user shares it. The
 * waypoint's name is shown, so the user sees what every map of the mesh will show.
 *
 * @param canShare whether the radio is connected; the offer stays visible but cannot be accepted otherwise.
 * @param isDemo whether a demo is running, in which case sharing is simulated and the prompt says so.
 */
@Composable
internal fun SharePointPrompt(
    point: SharedPoint,
    canShare: Boolean,
    isDemo: Boolean,
    onShare: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(Res.string.coords_share_title), style = MaterialTheme.typography.titleSmall)
            Text(point.name, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
            Text(
                stringResource(Res.string.coords_share_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (isDemo) {
                Text(
                    stringResource(Res.string.coords_share_demo),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                TextButton(onClick = onDismiss) { Text(stringResource(Res.string.coords_share_dismiss)) }
                FilledTonalButton(onClick = onShare, enabled = canShare) {
                    Text(stringResource(Res.string.coords_share_action))
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun SharePointPromptPreview() {
    AppTheme {
        SharePointPrompt(
            point =
            SharedPoint(
                id = 1,
                name = "FO 14:05 ALPHA-1",
                description = "véhicule arrêté au carrefour",
                point = LatLon(48.856_6, 2.352_2),
                expireEpochSeconds = 0L,
                lockedTo = 1,
                icon = SharedPoint.ICON_OBSERVED_FACT,
            ),
            canShare = true,
            isDemo = false,
            onShare = {},
            onDismiss = {},
        )
    }
}
