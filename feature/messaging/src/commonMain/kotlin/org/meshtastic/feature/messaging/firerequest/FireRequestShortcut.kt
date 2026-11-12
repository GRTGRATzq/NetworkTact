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
package org.meshtastic.feature.messaging.firerequest

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.firerequest.FireRequestSheet
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.firereq_shortcut

/** The DT badge of fire requests, the same on the shortcut and on the requests in a conversation. */
@Composable
internal fun FireRequestBadge(modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.tertiary, modifier = modifier) {
        Text(
            text = FireRequestSheet.TAG,
            color = MaterialTheme.colorScheme.onTertiary,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
        )
    }
}

/** « Demande de tir », on its own line under the other shortcuts, outlined in red as on the mock-up. */
@Composable
internal fun FireRequestShortcut(onClick: () -> Unit, modifier: Modifier = Modifier) {
    AssistChip(
        onClick = onClick,
        modifier = modifier.padding(horizontal = 8.dp),
        label = { Text(stringResource(Res.string.firereq_shortcut), color = MaterialTheme.colorScheme.error) },
        leadingIcon = { FireRequestBadge() },
        border =
        BorderStroke(AssistChipDefaults.assistChipBorder(enabled = true).width, MaterialTheme.colorScheme.error),
    )
}
