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
package org.meshtastic.app.map

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toFile
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.touchlab.kermit.Logger
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import org.meshtastic.app.map.offline.OfflineMapAssetsState
import org.meshtastic.app.map.offline.OfflineMapLibrary
import org.meshtastic.app.map.offline.offlineMapBasemaps
import org.meshtastic.core.common.util.ioDispatcher
import org.meshtastic.core.common.util.safeCatching
import org.meshtastic.core.repository.MapTileProviderPrefs
import org.meshtastic.feature.map.layers.getFileName
import org.meshtastic.feature.map.maplibre.component.customRasterBasemaps
import org.meshtastic.feature.map.maplibre.style.Basemap
import org.meshtastic.feature.map.tiles.CustomTileProviderConfig
import org.meshtastic.feature.map.tiles.CustomTileProviderRepository
import org.meshtastic.feature.map.tiles.CustomTileProviderSaveResult
import java.io.File
import kotlin.uuid.Uuid

/**
 * The absolute path of a stored MBTiles archive, or null if it has gone.
 *
 * `localUri` is a `file://` URI of a copy in app storage, written when the archive was picked, so it resolves to a real
 * path. Android is the only platform that supplies this: the others have no file picker wired up yet.
 */
internal fun androidTileArchivePath(localUri: String): String? {
    val archive = safeCatching { Uri.parse(localUri).toFile() }.getOrNull()
    return if (archive != null && archive.exists()) {
        archive.absolutePath
    } else {
        Logger.withTag("CustomBasemaps").w { "Skipping a local tile source whose archive is gone" }
        null
    }
}

/**
 * Opens a picker for an MBTiles archive and stores what it returns as a custom tile source.
 *
 * The picked archive is copied into app storage first: MapLibre opens an MBTiles file by path, and a document picker
 * hands back a `content://` URI into another app's provider.
 */
@Composable
internal fun rememberMbTilesImport(): () -> Unit {
    val repository: CustomTileProviderRepository = koinInject()
    val tilePrefs: MapTileProviderPrefs = koinInject()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val picker =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) { result ->
            val uri = result.data?.data
            if (result.resultCode == Activity.RESULT_OK && uri != null) {
                scope.launch {
                    val name = uri.getFileName(context)
                    val stored = importMbTiles(context, uri, "mbtiles_${Uuid.random()}.mbtiles")
                    if (stored != null) {
                        val attribution = withContext(ioDispatcher) { readMbTilesAttribution(stored) }
                        val config =
                            CustomTileProviderConfig(
                                name = name.substringBeforeLast('.'),
                                urlTemplate = "",
                                localUri = Uri.fromFile(File(stored)).toString(),
                                attribution = attribution,
                            )
                        // A map just imported is the one wanted: it becomes the basemap until the user picks another.
                        if (repository.addCustomTileProvider(config) == CustomTileProviderSaveResult.SAVED) {
                            tilePrefs.setSelectedCustomTileProviderId(config.id)
                        }
                    }
                }
            }
        }

    return {
        // Any MIME type: providers hand back application/octet-stream for a perfectly good .mbtiles.
        picker.launch(
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            },
        )
    }
}

/**
 * The user's custom sources with Android's local-archive support wired in.
 *
 * [customRasterBasemaps] is common and handles URL templates on every platform; this is the one seam Android adds, with
 * the fonts an archive on the device draws its labels with. Null until the sources and those fonts are ready.
 */
@Composable
internal fun androidCustomRasterBasemaps(): List<Basemap.Raster>? {
    val library: OfflineMapLibrary = koinInject()
    val assets by library.assets.collectAsStateWithLifecycle()
    val rasters =
        customRasterBasemaps(
            resolveLocalArchive = ::androidTileArchivePath,
            // Labels over an archive on the device read the fonts the APK carries, so they need no network either.
            localGlyphsUrl = (assets as? OfflineMapAssetsState.Installed)?.assets?.glyphsUrl,
        )
    val needsFonts = rasters?.any { it.isLocal } == true
    LaunchedEffect(needsFonts) { if (needsFonts) library.ensureAssets() }
    // Until the fonts are in place, wait rather than open on the online ones and swap the style a moment later.
    return rasters.takeUnless { needsFonts && assets == OfflineMapAssetsState.NotInstalled }
}

/**
 * Every basemap of the user's own on Android: custom tile sources, MBTiles archives and offline .pmtiles maps. Null
 * until both stores have been read.
 */
@Composable
internal fun androidCustomBasemaps(): List<Basemap>? {
    val rasters = androidCustomRasterBasemaps()
    val offline = offlineMapBasemaps()
    return if (rasters == null || offline == null) null else rasters + offline
}
