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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.tactdemo_exit
import org.meshtastic.core.resources.tactdemo_unavailable
import org.meshtastic.core.resources.tactdemo_unavailable_body
import org.meshtastic.core.ui.icon.Info
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.theme.AppTheme

/**
 * Wraps [entryProvider] so that, while demo mode is on, every route [isAvailableInDemo] rejects shows
 * [DemoUnavailableScreen] instead of its own screen, which is then not even composed (its view model is not created
 * either). The check is made at composition, so switching demo mode on or off updates a screen already open.
 */
fun demoGatedEntryProvider(
    entryProvider: (key: NavKey) -> NavEntry<NavKey>,
    demoActive: State<Boolean>,
    isAvailableInDemo: (NavKey) -> Boolean,
    onExitDemo: () -> Unit,
): (key: NavKey) -> NavEntry<NavKey> = { key ->
    val entry = entryProvider(key)
    if (isAvailableInDemo(key)) {
        entry
    } else {
        NavEntry(key = key, contentKey = entry.contentKey, metadata = entry.metadata) {
            if (demoActive.value) DemoUnavailableScreen(onExitDemo = onExitDemo) else entry.Content()
        }
    }
}

/** The one screen of its own demo mode has: it stands in for every screen that would show or change real data. */
@Composable
fun DemoUnavailableScreen(onExitDemo: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = MeshtasticIcons.Info,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(Res.string.tactdemo_unavailable),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(Res.string.tactdemo_unavailable_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        OutlinedButton(onClick = onExitDemo) { Text(stringResource(Res.string.tactdemo_exit)) }
    }
}

@PreviewLightDark
@Composable
private fun DemoUnavailableScreenPreview() {
    AppTheme { DemoUnavailableScreen(onExitDemo = {}) }
}
