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
package org.meshtastic.app.map.offline

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.map_attribution_osm_protomaps
import org.meshtastic.core.resources.offline_map_default_name
import org.meshtastic.core.ui.theme.TactColors
import org.meshtastic.feature.map.layers.getFileName
import org.meshtastic.feature.map.maplibre.component.OfflineMapFile
import org.meshtastic.feature.map.maplibre.style.Basemap
import org.meshtastic.feature.map.offline.exportFileName
import org.meshtastic.feature.map.offline.importedCredit
import org.meshtastic.feature.map.offline.offlineMapName
import org.meshtastic.feature.map.offline.offlineMapStyle

/**
 * The offline maps stored on this phone, as basemaps.
 *
 * The style follows the theme chosen in the app (light or dark background, the test [TactColors] uses), not the
 * system's. Empty while no map is installed, which leaves the basemap menu exactly as it was; null until the maps on
 * disk have been listed, so a map stored as the chosen basemap is never replaced by a built-in one for a moment.
 */
@Composable
fun offlineMapBasemaps(): List<Basemap>? {
    val library: OfflineMapLibrary = koinInject()
    val ready by library.ready.collectAsStateWithLifecycle()
    val maps by library.installedMaps.collectAsStateWithLifecycle()
    val assets by library.assets.collectAsStateWithLifecycle()
    val flavor = if (TactColors.isDarkBackground(MaterialTheme.colorScheme.background)) "dark" else "light"
    val protomapsCredit = stringResource(Res.string.map_attribution_osm_protomaps)

    val basemaps =
        remember(maps, assets, flavor, protomapsCredit) {
            val installed = (assets as? OfflineMapAssetsState.Installed)?.assets ?: return@remember emptyList()
            val template = installed.styleTemplates.getValue(flavor)
            maps.map { map ->
                // The app's credit only for a Protomaps basemap. Any other file keeps what its metadata declares, or
                // nothing: the style then declares an empty credit instead of falling back on ours.
                val credit = importedCredit(map.metadata, protomapsCredit)
                Basemap.LocalVector(
                    id = OfflineMapLibrary.basemapId(map.file.id),
                    label = map.file.name,
                    styleJson =
                    offlineMapStyle(
                        template = template,
                        archivePath = map.path,
                        assetsDir = installed.dir.absolutePath,
                        flavor = flavor,
                        attribution = credit.orEmpty(),
                    ),
                    attribution = credit,
                )
            }
        }
    return basemaps.takeIf { ready }
}

/**
 * Opens the system file picker for a .pmtiles file and hands the result to [library], which copies it.
 *
 * Any MIME type: file managers and cloud providers report a .pmtiles file as application/octet-stream, or not at all.
 */
@Composable
fun rememberPmTilesImport(library: OfflineMapLibrary): () -> Unit {
    val context = LocalContext.current
    val fallbackName = stringResource(Res.string.offline_map_default_name)
    val picker =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) { result ->
            val uri = result.data?.data
            if (result.resultCode == Activity.RESULT_OK && uri != null) {
                library.startImport(uri, offlineMapName(uri.getFileName(context), fallbackName))
            }
        }
    return {
        picker.launch(
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            },
        )
    }
}

/**
 * Opens the system "save as" picker for a map (D12, docs/PLAN-CARTE-TELECHARGEMENT.md) and hands the document the user
 * created to [library], which copies the map into it. The user picks the place; the app writes nowhere else.
 */
@Composable
fun rememberPmTilesExport(library: OfflineMapLibrary): (OfflineMapFile) -> Unit {
    val fallbackName = stringResource(Res.string.offline_map_default_name)
    // The map being exported, kept across a configuration change while the picker is open.
    var exporting by rememberSaveable { mutableStateOf<Pair<String, String>?>(null) }
    val picker =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) { result ->
            val uri = result.data?.data
            val map = exporting
            if (result.resultCode == Activity.RESULT_OK && uri != null && map != null) {
                library.startExport(map.first, map.second, uri)
            }
            exporting = null
        }
    return { map ->
        exporting = map.id to map.name
        picker.launch(
            Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_TITLE, exportFileName(map.name, fallbackName))
            },
        )
    }
}
