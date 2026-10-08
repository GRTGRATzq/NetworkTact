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

import android.content.Context
import android.net.Uri
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.annotation.Single
import org.meshtastic.core.common.di.ApplicationCoroutineScope
import org.meshtastic.core.common.util.safeCatching
import org.meshtastic.core.repository.MapTileProviderPrefs
import org.meshtastic.feature.map.maplibre.component.OfflineMapFile
import org.meshtastic.feature.map.maplibre.component.OfflineMapFiles
import org.meshtastic.feature.map.maplibre.component.OfflineMapImport
import org.meshtastic.feature.map.maplibre.component.OfflineMapImportFailure
import org.meshtastic.feature.map.offline.PMTILES_EXTENSION
import org.meshtastic.feature.map.offline.PmTilesCheck
import org.meshtastic.feature.map.offline.PmTilesHeader
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.RandomAccessFile
import kotlin.uuid.Uuid

/** Whether the bundled style, fonts and sprites are on disk. */
sealed interface OfflineMapAssetsState {
    data object NotInstalled : OfflineMapAssetsState

    data class Installed(val assets: OfflineMapAssets) : OfflineMapAssetsState

    /** The copy from the APK failed: offline maps are not offered, and local archives keep the online fonts. */
    data object Failed : OfflineMapAssetsState
}

/** An installed map, with the path MapLibre opens it by. */
data class InstalledOfflineMap(val file: OfflineMapFile, val path: String)

/**
 * The offline maps (.pmtiles) stored on this phone, for the F-Droid flavor's MapLibre map.
 *
 * Layout under the app's private files: `pmtiles/<id>.pmtiles` with its display name beside it in `<id>.name`, and
 * `pmtiles-import/` for the copy in progress. A copy is moved into `pmtiles/` only once it is complete and checked, so
 * that directory never holds a partial file; `pmtiles-import/` is emptied before every import and on start.
 *
 * Nothing here touches the network: the file comes from the system file picker, and the style, fonts and sprites from
 * the APK ([OfflineMapAssets]).
 */
@Single
class OfflineMapLibrary(
    private val context: Context,
    private val scope: ApplicationCoroutineScope,
    private val tilePrefs: MapTileProviderPrefs,
) : OfflineMapFiles {

    private val mapsDir = File(context.filesDir, MAPS_DIR)
    private val importDir = File(context.filesDir, IMPORT_DIR)
    private val mutex = Mutex()
    private var importJob: Job? = null

    private val installed = MutableStateFlow<List<InstalledOfflineMap>>(emptyList())
    private val assetsState = MutableStateFlow<OfflineMapAssetsState>(OfflineMapAssetsState.NotInstalled)
    private val listed = MutableStateFlow(false)
    private val _maps = MutableStateFlow<List<OfflineMapFile>>(emptyList())
    private val _importStatus = MutableStateFlow<OfflineMapImport>(OfflineMapImport.Idle)

    /** The installed maps, with their paths. */
    val installedMaps: StateFlow<List<InstalledOfflineMap>> = installed.asStateFlow()

    /** The bundled style, fonts and sprites: installed once a map stored on the device (of any format) exists. */
    val assets: StateFlow<OfflineMapAssetsState> = assetsState.asStateFlow()

    /** False until the maps on disk have been listed once (and the assets installed if any map exists). */
    val ready: StateFlow<Boolean> = listed.asStateFlow()

    override val maps: StateFlow<List<OfflineMapFile>> = _maps.asStateFlow()
    override val importStatus: StateFlow<OfflineMapImport> = _importStatus.asStateFlow()

    init {
        scope.launch {
            mutex.withLock {
                importDir.deleteRecursively()
                refresh()
            }
        }
    }

    /** Copies the picked [uri] into the library under [name]. One import at a time; the button is off meanwhile. */
    fun startImport(uri: Uri, name: String) {
        if (importJob?.isActive == true) return
        _importStatus.value = OfflineMapImport.Copying(name, copiedBytes = 0, totalBytes = null)
        importJob =
            scope.launch {
                try {
                    mutex.withLock { runImport(uri, name) }
                } finally {
                    // Cancelled before or during the copy: runImport has already removed whatever it wrote.
                    if (_importStatus.value is OfflineMapImport.Copying) {
                        _importStatus.value = OfflineMapImport.Cancelled
                    }
                }
            }
    }

    /**
     * Installs the bundled fonts if they are not yet: an MBTiles archive on the device needs them for its labels too,
     * though the .pmtiles maps listed here are the ones that install them otherwise.
     */
    fun ensureAssets() {
        if (assetsState.value != OfflineMapAssetsState.NotInstalled) return
        scope.launch { mutex.withLock { installAssets() } }
    }

    override fun cancelImport() {
        importJob?.cancel()
    }

    override fun clearImportOutcome() {
        if (_importStatus.value !is OfflineMapImport.Copying) _importStatus.value = OfflineMapImport.Idle
    }

    override fun delete(id: String) {
        scope.launch {
            mutex.withLock {
                File(mapsDir, "$id.$PMTILES_EXTENSION").delete()
                File(mapsDir, "$id$NAME_SUFFIX").delete()
                refresh()
            }
        }
    }

    private suspend fun runImport(uri: Uri, name: String) {
        val size = context.contentResolver.declaredSize(uri)
        _importStatus.value = OfflineMapImport.Copying(name, copiedBytes = 0, totalBytes = size)
        importDir.deleteRecursively()
        importDir.mkdirs()
        mapsDir.mkdirs()
        val id = Uuid.random().toString()
        val temporary = File(importDir, "$id.$PMTILES_EXTENSION")
        val permission = context.contentResolver.holdReadPermission(uri)
        try {
            val input =
                context.contentResolver.openInputStream(uri)
                    ?: throw PmTilesCopyException(OfflineMapImportFailure.Unreadable)
            input.use {
                copyPmTiles(it, temporary, size) { copied ->
                    _importStatus.value = OfflineMapImport.Copying(name, copied, size)
                }
            }
            File(mapsDir, "$id$NAME_SUFFIX").writeText(name)
            if (!temporary.renameTo(File(mapsDir, "$id.$PMTILES_EXTENSION"))) {
                throw PmTilesCopyException(OfflineMapImportFailure.CopyFailed)
            }
            _importStatus.value = OfflineMapImport.Done(name)
            // A map just imported is the one wanted: it becomes the basemap until the user picks another.
            tilePrefs.setSelectedCustomTileProviderId(basemapId(id))
        } catch (e: PmTilesCopyException) {
            _importStatus.value = OfflineMapImport.Failed(e.reason, e.neededBytes, e.freeBytes)
        } catch (e: FileNotFoundException) {
            Logger.withTag(TAG).w(e) { "Could not open the picked offline map" }
            _importStatus.value = OfflineMapImport.Failed(OfflineMapImportFailure.Unreadable)
        } catch (e: IOException) {
            Logger.withTag(TAG).w(e) { "Offline map copy failed" }
            _importStatus.value = OfflineMapImport.Failed(OfflineMapImportFailure.CopyFailed)
        } catch (e: SecurityException) {
            // The picker's grant can be gone by the time the copy runs.
            Logger.withTag(TAG).w(e) { "Lost permission to read the picked offline map" }
            _importStatus.value = OfflineMapImport.Failed(OfflineMapImportFailure.Unreadable)
        } finally {
            importDir.deleteRecursively()
            if (permission) context.contentResolver.releaseReadPermission(uri)
            refresh()
        }
    }

    /** Lists the maps, drops what is not one (an orphan name file, an unreadable archive), installs the assets. */
    private fun refresh() {
        val files = mapsDir.listFiles().orEmpty()
        val maps =
            files
                .filter { it.extension == PMTILES_EXTENSION }
                .mapNotNull { file ->
                    val header = readHeader(file) ?: return@mapNotNull null
                    val id = file.nameWithoutExtension
                    val name = File(mapsDir, "$id$NAME_SUFFIX").takeIf { it.exists() }?.readText() ?: id
                    InstalledOfflineMap(
                        OfflineMapFile(id, name, file.length(), header.minZoom, header.maxZoom),
                        file.absolutePath,
                    )
                }
                .sortedBy { it.file.name.lowercase() }
        val ids = maps.map { it.file.id }.toSet()
        files
            .filter { it.name.endsWith(NAME_SUFFIX) && it.name.removeSuffix(NAME_SUFFIX) !in ids }
            .forEach { it.delete() }

        if (maps.isNotEmpty()) installAssets()
        installed.value = maps
        _maps.value = maps.map { it.file }
        listed.value = true
    }

    /** Installs the bundled assets unless done or failed already. Call with [mutex] held. */
    private fun installAssets() {
        if (assetsState.value != OfflineMapAssetsState.NotInstalled) return
        assetsState.value =
            safeCatching { OfflineMapAssets.install(context) }
                .onFailure { Logger.withTag(TAG).e(it) { "Could not install the offline map style" } }
                .map { OfflineMapAssetsState.Installed(it) }
                .getOrDefault(OfflineMapAssetsState.Failed)
    }

    private fun readHeader(file: File): PmTilesHeader? = safeCatching {
        val bytes = ByteArray(PmTilesHeader.SIZE)
        RandomAccessFile(file, "r").use { it.readFully(bytes) }
        (PmTilesHeader.check(bytes) as? PmTilesCheck.Valid)?.header?.takeIf {
            file.length() >= it.minimumFileSize
        }
    }
        .getOrNull()

    companion object {
        /** The basemap id of the stored map [mapId], kept apart from the custom tile sources' ids. */
        fun basemapId(mapId: String): String = "pmtiles:$mapId"

        private const val TAG = "OfflineMapLibrary"
        private const val MAPS_DIR = "pmtiles"
        private const val IMPORT_DIR = "pmtiles-import"
        private const val NAME_SUFFIX = ".name"
    }
}
