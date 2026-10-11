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
package org.meshtastic.core.ui.diagnostic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.meshtastic.core.model.diagnostic.LinkAlert
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.diag_banner_action
import org.meshtastic.core.resources.diag_banner_close
import org.meshtastic.core.resources.diag_banner_no_node_ever
import org.meshtastic.core.resources.diag_banner_no_node_since
import org.meshtastic.core.resources.diag_banner_not_connected
import org.meshtastic.core.ui.icon.Close
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.Warning
import org.meshtastic.core.ui.theme.AppTheme
import org.meshtastic.core.ui.util.formatElapsed
import kotlin.time.Duration.Companion.minutes

/** The link banner of [LinkAlertViewModel], or nothing. */
@Composable
fun LinkAlertBannerHost(
    onOpenDiagnostic: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LinkAlertViewModel = koinViewModel(),
) {
    val alert by viewModel.alert.collectAsStateWithLifecycle()
    alert?.let { current ->
        LinkAlertBanner(
            alert = current,
            onOpenDiagnostic = onOpenDiagnostic,
            onDismiss = { viewModel.dismiss(current) },
            modifier = modifier,
        )
    }
}

/**
 * "Radio not connected" or "No node heard for 18 min", with "Diagnostic" and a close button. Inverted colours, as the
 * other alerts of the app: never red (urgent messages) nor amber (reports). It states a fact, never a conclusion.
 */
@Composable
fun LinkAlertBanner(
    alert: LinkAlert,
    onOpenDiagnostic: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val content = colorScheme.inverseOnSurface
    Row(
        modifier = modifier.fillMaxWidth().background(colorScheme.inverseSurface).padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = MeshtasticIcons.Warning,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = alertText(alert),
            color = content,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onOpenDiagnostic, colors = ButtonDefaults.textButtonColors(contentColor = content)) {
            Text(text = stringResource(Res.string.diag_banner_action), fontWeight = FontWeight.Bold)
        }
        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = MeshtasticIcons.Close,
                contentDescription = stringResource(Res.string.diag_banner_close),
                tint = content,
            )
        }
    }
}

@Composable
private fun alertText(alert: LinkAlert): String = when (alert) {
    is LinkAlert.RadioNotConnected -> stringResource(Res.string.diag_banner_not_connected)

    is LinkAlert.NoNodeHeard ->
        alert.since?.let { stringResource(Res.string.diag_banner_no_node_since, formatElapsed(it)) }
            ?: stringResource(Res.string.diag_banner_no_node_ever)
}

@PreviewLightDark
@Composable
private fun LinkAlertBannerPreview() {
    AppTheme {
        Surface {
            LinkAlertBanner(
                alert = LinkAlert.NoNodeHeard(since = 18.minutes, key = "preview"),
                onOpenDiagnostic = {},
                onDismiss = {},
            )
        }
    }
}
