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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.cancel
import org.meshtastic.core.resources.delete
import org.meshtastic.core.resources.offline_map_cancelled
import org.meshtastic.core.resources.offline_map_copying
import org.meshtastic.core.resources.offline_map_delete_message
import org.meshtastic.core.resources.offline_map_delete_title
import org.meshtastic.core.resources.offline_map_detail
import org.meshtastic.core.resources.offline_map_error_copy
import org.meshtastic.core.resources.offline_map_error_incomplete
import org.meshtastic.core.resources.offline_map_error_not_pmtiles
import org.meshtastic.core.resources.offline_map_error_raster
import org.meshtastic.core.resources.offline_map_error_read
import org.meshtastic.core.resources.offline_map_error_space
import org.meshtastic.core.resources.offline_map_error_version
import org.meshtastic.core.resources.offline_map_files
import org.meshtastic.core.resources.offline_map_files_empty
import org.meshtastic.core.resources.offline_map_files_hint
import org.meshtastic.core.resources.offline_map_import
import org.meshtastic.core.resources.offline_map_imported
import org.meshtastic.core.resources.offline_map_progress
import org.meshtastic.core.resources.offline_map_size_mb
import org.meshtastic.core.resources.okay
import org.meshtastic.core.ui.component.MeshtasticDialog
import org.meshtastic.core.ui.icon.Delete
import org.meshtastic.core.ui.icon.MeshtasticIcons

/** An offline map file stored on the device. */
data class OfflineMapFile(val id: String, val name: String, val sizeBytes: Long, val minZoom: Int, val maxZoom: Int)

/** Where an import stands. Every outcome but [Done] leaves nothing behind. */
sealed interface OfflineMapImport {
    data object Idle : OfflineMapImport

    /** [totalBytes] is null when the file provider does not say how big the file is. */
    data class Copying(val name: String, val copiedBytes: Long, val totalBytes: Long?) : OfflineMapImport

    data class Done(val name: String) : OfflineMapImport

    data object Cancelled : OfflineMapImport

    /** [neededBytes] and [freeBytes] are only meaningful for [OfflineMapImportFailure.NoSpace]. */
    data class Failed(val reason: OfflineMapImportFailure, val neededBytes: Long = 0, val freeBytes: Long = 0) :
        OfflineMapImport
}

enum class OfflineMapImportFailure {
    NotPmTiles,
    UnsupportedVersion,
    NotVectorTiles,
    NoSpace,
    Incomplete,
    Unreadable,
    CopyFailed,
}

/** The host's store of offline map files: what is installed, how an import is going, and how to change either. */
interface OfflineMapFiles {
    val maps: StateFlow<List<OfflineMapFile>>
    val importStatus: StateFlow<OfflineMapImport>

    fun cancelImport()

    /** Clears a finished import's outcome once the user has read it. */
    fun clearImportOutcome()

    fun delete(id: String)
}

/**
 * The basemap menu's entry for the maps stored on the device, and the sheet it opens.
 *
 * @param onImport Opens the platform's file picker; the host starts the copy when a file comes back.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineMapFilesMenuItem(files: OfflineMapFiles, onImport: () -> Unit, modifier: Modifier = Modifier) {
    var sheetVisible by remember { mutableStateOf(false) }

    DropdownMenuItem(
        text = { Text(text = stringResource(Res.string.offline_map_files)) },
        onClick = { sheetVisible = true },
        modifier = modifier,
    )

    if (sheetVisible) {
        ModalBottomSheet(onDismissRequest = { sheetVisible = false }) {
            val maps by files.maps.collectAsStateWithLifecycle()
            val status by files.importStatus.collectAsStateWithLifecycle()
            OfflineMapFilesSheet(
                maps = maps,
                status = status,
                onImport = onImport,
                onCancelImport = files::cancelImport,
                onClearOutcome = files::clearImportOutcome,
                onDelete = { map -> files.delete(map.id) },
            )
        }
    }
}

@Composable
private fun OfflineMapFilesSheet(
    maps: List<OfflineMapFile>,
    status: OfflineMapImport,
    onImport: () -> Unit,
    onCancelImport: () -> Unit,
    onClearOutcome: () -> Unit,
    onDelete: (OfflineMapFile) -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<OfflineMapFile?>(null) }
    pendingDelete?.let { map ->
        MeshtasticDialog(
            title = stringResource(Res.string.offline_map_delete_title),
            message = stringResource(Res.string.offline_map_delete_message, map.name),
            confirmText = stringResource(Res.string.delete),
            onConfirm = {
                onDelete(map)
                pendingDelete = null
            },
            dismissText = stringResource(Res.string.cancel),
            onDismiss = { pendingDelete = null },
        )
    }

    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            SheetHeader()
            HorizontalDivider()
        }

        if (status != OfflineMapImport.Idle) {
            item {
                ImportStatus(status = status, onCancel = onCancelImport, onClear = onClearOutcome)
                HorizontalDivider()
            }
        }

        if (maps.isEmpty()) {
            item {
                Text(
                    text = stringResource(Res.string.offline_map_files_empty),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            items(maps, key = { it.id }) { map ->
                OfflineMapRow(map = map, onDelete = { pendingDelete = map })
                HorizontalDivider()
            }
        }

        item {
            Button(
                onClick = onImport,
                enabled = status !is OfflineMapImport.Copying,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) {
                Text(stringResource(Res.string.offline_map_import))
            }
        }
    }
}

@Composable
private fun SheetHeader() {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = stringResource(Res.string.offline_map_files), style = MaterialTheme.typography.headlineSmall)
        Text(text = stringResource(Res.string.offline_map_files_hint), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun OfflineMapRow(map: OfflineMapFile, onDelete: () -> Unit) {
    ListItem(
        headlineContent = { Text(map.name) },
        supportingContent = {
            Text(
                text =
                stringResource(
                    Res.string.offline_map_detail,
                    megabytesLabel(map.sizeBytes),
                    map.minZoom,
                    map.maxZoom,
                ),
                style = MaterialTheme.typography.bodySmall,
            )
        },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(MeshtasticIcons.Delete, contentDescription = stringResource(Res.string.delete))
            }
        },
    )
}

@Composable
private fun ImportStatus(status: OfflineMapImport, onCancel: () -> Unit, onClear: () -> Unit) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (status) {
            is OfflineMapImport.Copying -> {
                Text(text = stringResource(Res.string.offline_map_copying, status.name))
                val total = status.totalBytes
                if (total != null && total > 0) {
                    LinearProgressIndicator(
                        progress = { (status.copiedBytes.toFloat() / total).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text =
                        stringResource(
                            Res.string.offline_map_progress,
                            megabytesLabel(status.copiedBytes),
                            megabytesLabel(total),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(text = megabytesLabel(status.copiedBytes), style = MaterialTheme.typography.bodySmall)
                }
                OutlinedButton(onClick = onCancel) { Text(stringResource(Res.string.cancel)) }
            }

            else -> {
                Text(text = outcomeText(status))
                OutlinedButton(onClick = onClear) { Text(stringResource(Res.string.okay)) }
            }
        }
    }
}

@Composable
private fun outcomeText(status: OfflineMapImport): String = when (status) {
    is OfflineMapImport.Done -> stringResource(Res.string.offline_map_imported, status.name)
    is OfflineMapImport.Failed -> failureText(status)
    else -> stringResource(Res.string.offline_map_cancelled)
}

@Composable
private fun failureText(failure: OfflineMapImport.Failed): String = when (failure.reason) {
    OfflineMapImportFailure.NotPmTiles -> stringResource(Res.string.offline_map_error_not_pmtiles)

    OfflineMapImportFailure.UnsupportedVersion -> stringResource(Res.string.offline_map_error_version)

    OfflineMapImportFailure.NotVectorTiles -> stringResource(Res.string.offline_map_error_raster)

    OfflineMapImportFailure.NoSpace ->
        stringResource(
            Res.string.offline_map_error_space,
            megabytesLabel(failure.neededBytes),
            megabytesLabel(failure.freeBytes),
        )

    OfflineMapImportFailure.Incomplete -> stringResource(Res.string.offline_map_error_incomplete)

    OfflineMapImportFailure.Unreadable -> stringResource(Res.string.offline_map_error_read)

    OfflineMapImportFailure.CopyFailed -> stringResource(Res.string.offline_map_error_copy)
}

@Composable
private fun megabytesLabel(bytes: Long): String = stringResource(Res.string.offline_map_size_mb, bytes.megabytes())
