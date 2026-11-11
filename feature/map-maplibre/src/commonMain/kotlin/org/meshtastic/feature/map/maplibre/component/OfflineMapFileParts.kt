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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.util.NumberFormatter
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.cancel
import org.meshtastic.core.resources.delete
import org.meshtastic.core.resources.offline_map_delete_message
import org.meshtastic.core.resources.offline_map_delete_title
import org.meshtastic.core.resources.offline_map_download
import org.meshtastic.core.resources.offline_map_download_pending
import org.meshtastic.core.resources.offline_map_download_resume
import org.meshtastic.core.resources.offline_map_downloaded_edition
import org.meshtastic.core.resources.offline_map_downloaded_file
import org.meshtastic.core.resources.offline_map_export_failed
import org.meshtastic.core.resources.offline_map_exported
import org.meshtastic.core.resources.offline_map_exporting
import org.meshtastic.core.resources.offline_map_import
import org.meshtastic.core.resources.okay
import org.meshtastic.core.ui.component.MeshtasticDialog
import org.meshtastic.feature.map.offline.extract.DownloadArea
import org.meshtastic.feature.map.offline.extract.DownloadedMapInfo
import org.meshtastic.feature.map.offline.extract.PendingDownload

/*
 * Parts of the "Maps on this phone" sheet (OfflineMapFiles.kt): its buttons, the delete dialog, and what concerns
 * downloads and exports.
 */

/** "Import a file", and "Download a map" where the host downloads ([onDownload] not null). */
@Composable
internal fun SheetButtons(enabled: Boolean, onImport: () -> Unit, onDownload: (() -> Unit)?) {
    Column {
        Button(onClick = onImport, enabled = enabled, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(stringResource(Res.string.offline_map_import))
        }
        if (onDownload != null) {
            OutlinedButton(
                onClick = onDownload,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            ) {
                Text(stringResource(Res.string.offline_map_download))
            }
        }
    }
}

@Composable
internal fun DeleteDialog(map: OfflineMapFile, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    MeshtasticDialog(
        title = stringResource(Res.string.offline_map_delete_title),
        message = stringResource(Res.string.offline_map_delete_message, map.name),
        confirmText = stringResource(Res.string.delete),
        onConfirm = onConfirm,
        dismissText = stringResource(Res.string.cancel),
        onDismiss = onDismiss,
    )
}

/** A download stopped by a cut, possibly in an earlier run: "Resume" opens the frame, where it goes on. */
@Composable
internal fun PendingRow(pending: PendingDownload, onResume: () -> Unit) {
    val percent = if (pending.rangesTotal > 0) pending.rangesDone * PERCENT / pending.rangesTotal else 0
    ListItem(
        headlineContent = {
            Text(text = stringResource(Res.string.offline_map_download_pending, pending.name, "$percent %"))
        },
        trailingContent = {
            Button(onClick = onResume) { Text(stringResource(Res.string.offline_map_download_resume)) }
        },
    )
}

/** An export going on or how it ended: the copy is quick (a local file), so no progress bar beyond "copying". */
@Composable
internal fun ExportStatus(status: OfflineMapExport, onClear: () -> Unit) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (status) {
            is OfflineMapExport.Copying -> {
                Text(text = stringResource(Res.string.offline_map_exporting, status.name))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            is OfflineMapExport.Done -> {
                Text(text = stringResource(Res.string.offline_map_exported, status.name))
                OutlinedButton(onClick = onClear) { Text(stringResource(Res.string.okay)) }
            }

            else -> {
                Text(
                    text = stringResource(Res.string.offline_map_export_failed),
                    color = MaterialTheme.colorScheme.error,
                )
                OutlinedButton(onClick = onClear) { Text(stringResource(Res.string.okay)) }
            }
        }
    }
}

/** Where a downloaded map comes from and what it covers: "Edition of 2026-11-11, build.protomaps.com · 60 × 60 km". */
@Composable
internal fun DownloadedLine(info: DownloadedMapInfo) {
    val width = NumberFormatter.format(DownloadArea.widthMeters(info.bounds) / METERS_PER_KM, 0)
    val height = NumberFormatter.format(DownloadArea.heightMeters(info.bounds) / METERS_PER_KM, 0)
    val date = info.editionDate
    Text(
        text =
        if (date == null) {
            stringResource(Res.string.offline_map_downloaded_file, info.sourceHost, width, height)
        } else {
            stringResource(Res.string.offline_map_downloaded_edition, isoDate(date), info.sourceHost, width, height)
        },
        style = MaterialTheme.typography.bodySmall,
    )
}

private const val METERS_PER_KM = 1_000.0
private const val PERCENT = 100
