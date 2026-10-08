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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.map_attribution_osm
import org.meshtastic.feature.map.maplibre.style.Basemap
import org.meshtastic.feature.map.maplibre.style.LOCAL_ARCHIVE_SCHEME
import org.meshtastic.feature.map.offline.archiveAttribution
import org.meshtastic.feature.map.tiles.CustomTileProviderConfig
import org.meshtastic.feature.map.tiles.CustomTileProviderRepository
import org.meshtastic.feature.map.tiles.RasterTileSpec

/**
 * MapLibre's own scheme for an MBTiles archive: the native source opens the path with SQLite and takes no
 * `{z}/{x}/{y}`.
 */
internal fun mbTilesUrl(absolutePath: String): String = "$LOCAL_ARCHIVE_SCHEME$absolutePath"

/**
 * The user's own tile sources, as MapLibre basemaps.
 *
 * Every map that offers a basemap menu shows these, so a source added once appears in all of them — on every platform,
 * since the store this reads is common code. Null until the store has been read.
 *
 * @param resolveLocalArchive Turns a stored `file://` URI into an absolute path, or returns null when the archive is
 *   gone or this platform cannot open one. Only Android supplies it: there is no file picker elsewhere yet, and the
 *   desktop renderer aborts the process on the native MBTiles source rather than failing softly.
 */
@Composable
fun customRasterBasemaps(resolveLocalArchive: (String) -> String? = { null }): List<Basemap.Raster>? {
    val tileProviders: CustomTileProviderRepository = koinInject()
    // Null until the store has been read from disk: an empty list before that would let the map open on a built-in
    // style and swap to a stored custom source (an imported map) a moment later.
    val configs by
        produceState<List<CustomTileProviderConfig>?>(initialValue = null, tileProviders) {
            tileProviders.awaitCustomTileProviders()
            tileProviders.getCustomTileProviders().collect { value = it }
        }
    val openStreetMap = stringResource(Res.string.map_attribution_osm)
    return configs?.mapNotNull { config ->
        val tiles = config.tileUrl(resolveLocalArchive) ?: return@mapNotNull null
        // An imported archive shows its own credit, or the OpenStreetMap one, which is what such a file almost always
        // holds and what its licence requires. A URL source's credit is its server's to give, as before.
        val attribution = if (config.isLocal) archiveAttribution(config.attribution, openStreetMap) else null
        Basemap.Raster(
            id = config.id,
            label = config.name,
            spec = RasterTileSpec(tiles = listOf(tiles), attributionHtml = attribution),
        )
    }
}

/**
 * The tile URL for a source, or null if it cannot be resolved.
 *
 * A missing archive has to be dropped rather than handed over: pointing MapLibre at a path where no file is does not
 * fail softly, it aborts the process from a native thread with no Kotlin frame to catch. An archive can disappear
 * between sessions, so the check is not paranoia; the Google flavour guards its own MBTiles provider the same way.
 */
private fun CustomTileProviderConfig.tileUrl(resolveLocalArchive: (String) -> String?): String? {
    val archiveUri = localUri
    return if (archiveUri != null) {
        resolveLocalArchive(archiveUri)?.let(::mbTilesUrl)
    } else {
        urlTemplate.takeIf { it.isNotBlank() }
    }
}
