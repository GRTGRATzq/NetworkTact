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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.util.NumberFormatter
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.cancel
import org.meshtastic.core.resources.offline_map_download
import org.meshtastic.core.resources.offline_map_download_computing
import org.meshtastic.core.resources.offline_map_download_error_busy
import org.meshtastic.core.resources.offline_map_download_error_demo
import org.meshtastic.core.resources.offline_map_download_error_edition_gone
import org.meshtastic.core.resources.offline_map_download_error_file_missing
import org.meshtastic.core.resources.offline_map_download_error_incomplete
import org.meshtastic.core.resources.offline_map_download_error_no_edition
import org.meshtastic.core.resources.offline_map_download_error_no_tiles
import org.meshtastic.core.resources.offline_map_download_error_schema
import org.meshtastic.core.resources.offline_map_download_error_server
import org.meshtastic.core.resources.offline_map_download_error_space
import org.meshtastic.core.resources.offline_map_download_error_too_big
import org.meshtastic.core.resources.offline_map_download_error_unexpected
import org.meshtastic.core.resources.offline_map_download_error_unreachable
import org.meshtastic.core.resources.offline_map_download_error_unsupported
import org.meshtastic.core.resources.offline_map_download_privacy
import org.meshtastic.core.resources.offline_map_download_source
import org.meshtastic.core.resources.offline_map_download_source_change
import org.meshtastic.core.resources.offline_map_download_source_field
import org.meshtastic.core.resources.offline_map_download_source_https
import org.meshtastic.core.resources.offline_map_size_mb
import org.meshtastic.core.resources.okay
import org.meshtastic.core.ui.component.MeshtasticDialog
import org.meshtastic.feature.map.offline.extract.DownloadArea
import org.meshtastic.feature.map.offline.extract.GeoBounds
import org.meshtastic.feature.map.offline.extract.MapDownloadFailure
import org.meshtastic.feature.map.offline.extract.MapDownloadState
import org.meshtastic.feature.map.offline.extract.MapSource

@Composable
internal fun failureText(failed: MapDownloadState.Failed, host: String): String = when (failed.reason) {
    MapDownloadFailure.Unreachable -> stringResource(Res.string.offline_map_download_error_unreachable, host)

    MapDownloadFailure.NoEdition -> stringResource(Res.string.offline_map_download_error_no_edition, host)

    MapDownloadFailure.FileMissing -> stringResource(Res.string.offline_map_download_error_file_missing, host)

    MapDownloadFailure.EditionGone -> stringResource(Res.string.offline_map_download_error_edition_gone)

    MapDownloadFailure.RangesUnsupported -> stringResource(Res.string.offline_map_download_error_server)

    MapDownloadFailure.ServerBusy -> stringResource(Res.string.offline_map_download_error_busy)

    MapDownloadFailure.UnsupportedSource -> stringResource(Res.string.offline_map_download_error_unsupported)

    MapDownloadFailure.IncompatibleSchema -> stringResource(Res.string.offline_map_download_error_schema)

    MapDownloadFailure.TooManyTiles -> stringResource(Res.string.offline_map_download_error_too_big)

    MapDownloadFailure.NoTiles -> stringResource(Res.string.offline_map_download_error_no_tiles)

    MapDownloadFailure.NoSpace ->
        stringResource(
            Res.string.offline_map_download_error_space,
            megabytes(failed.neededBytes),
            megabytes(failed.freeBytes),
        )

    MapDownloadFailure.Incomplete -> stringResource(Res.string.offline_map_download_error_incomplete)

    MapDownloadFailure.DemoMode -> stringResource(Res.string.offline_map_download_error_demo)

    MapDownloadFailure.Unexpected -> stringResource(Res.string.offline_map_download_error_unexpected)
}

@Composable
internal fun megabytes(bytes: Long): String = stringResource(Res.string.offline_map_size_mb, bytes.megabytes())

/** `AAAAMMJJ` as `AAAA-MM-JJ`, the same in every language. */
internal fun isoDate(date: String): String =
    "${date.substring(0, YEAR_END)}-${date.substring(YEAR_END, MONTH_END)}-${date.substring(MONTH_END)}"

/** "45.917 N 24.800 E · 60 × 60 km · z15", from the frame's centre and size. */
internal fun defaultMapName(bounds: GeoBounds?, maxZoom: Int): String {
    if (bounds == null) return "z$maxZoom"
    val lat = (bounds.south + bounds.north) / 2
    val lon = (bounds.west + bounds.east) / 2
    val width = NumberFormatter.format(DownloadArea.widthMeters(bounds) / METERS_PER_KM, 0)
    val height = NumberFormatter.format(DownloadArea.heightMeters(bounds) / METERS_PER_KM, 0)
    val latText = NumberFormatter.format(kotlin.math.abs(lat), DEGREE_DECIMALS) + if (lat >= 0) " N" else " S"
    val lonText = NumberFormatter.format(kotlin.math.abs(lon), DEGREE_DECIMALS) + if (lon >= 0) " E" else " W"
    return "$latText $lonText · $width × $height km · z$maxZoom"
}

/** The title, what the source sees (D8: in full, every time) and which source it is. */
@Composable
internal fun SourceLines(host: String, enabled: Boolean, onChange: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = stringResource(Res.string.offline_map_download), style = MaterialTheme.typography.titleMedium)
        Text(
            text = stringResource(Res.string.offline_map_download_privacy),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(Res.string.offline_map_download_source, host),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onChange, enabled = enabled) {
                Text(stringResource(Res.string.offline_map_download_source_change))
            }
        }
    }
}

/** The source address, https only (plan §9). [onDone] gets null when cancelled. */
@Composable
internal fun SourceDialog(current: MapSource, onDone: (MapSource?) -> Unit) {
    val initial =
        when (current) {
            is MapSource.Editions -> current.base
            is MapSource.SingleFile -> current.url
        }
    var text by remember { mutableStateOf(initial) }
    val parsed = MapSource.parse(text)
    MeshtasticDialog(
        title = stringResource(Res.string.offline_map_download_source_change),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(Res.string.offline_map_download_privacy),
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(Res.string.offline_map_download_source_field)) },
                    isError = parsed == null,
                    supportingText =
                    if (parsed == null) {
                        { Text(stringResource(Res.string.offline_map_download_source_https)) }
                    } else {
                        null
                    },
                    singleLine = true,
                )
            }
        },
        confirmText = stringResource(Res.string.okay),
        onConfirm = { parsed?.let { onDone(it) } },
        dismissText = stringResource(Res.string.cancel),
        onDismiss = { onDone(null) },
    )
}

/** "Calculating the size…" with a spinner. */
@Composable
internal fun Computing() {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(modifier = Modifier.size(PROGRESS_INDICATOR_DP.dp))
        Text(stringResource(Res.string.offline_map_download_computing))
    }
}

private const val PROGRESS_INDICATOR_DP = 20
private const val YEAR_END = 4
private const val MONTH_END = 6
private const val DEGREE_DECIMALS = 3
private const val METERS_PER_KM = 1_000.0
