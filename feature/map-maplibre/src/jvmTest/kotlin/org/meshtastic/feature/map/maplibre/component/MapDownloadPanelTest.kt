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

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import org.jetbrains.compose.resources.StringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.cancel
import org.meshtastic.core.resources.close
import org.meshtastic.core.resources.getString
import org.meshtastic.core.resources.offline_map_download_compute
import org.meshtastic.core.resources.offline_map_download_error_tiny
import org.meshtastic.core.resources.offline_map_download_privacy
import org.meshtastic.core.resources.offline_map_download_resume
import org.meshtastic.core.resources.offline_map_download_start
import org.meshtastic.core.resources.offline_map_download_use_view
import org.meshtastic.feature.map.offline.extract.DownloadJournal
import org.meshtastic.feature.map.offline.extract.GeoBounds
import org.meshtastic.feature.map.offline.extract.MapDownloadController
import org.meshtastic.feature.map.offline.extract.MapDownloadEstimate
import org.meshtastic.feature.map.offline.extract.MapDownloadState
import org.meshtastic.feature.map.offline.extract.MapSourceClient
import org.meshtastic.feature.map.offline.extract.RangeSource
import org.meshtastic.feature.map.offline.extract.RemoteFileInfo
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * The download panel's buttons in each state (docs/PLAN-CARTE-TELECHARGEMENT.md §6). Nothing here reaches a network:
 * the state is passed in, and the controller's client fails if asked. The frame's handles need a MapLibre map and are
 * left to the phone test (TESTS-A-FAIRE.md §17); their logic is in DownloadAreaTest.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class MapDownloadPanelTest {

    private val fileSystem = FakeFileSystem()
    private val downloadDir = "/files/pmtiles-download".toPath()

    /** Cincu, about 60 × 60 km. */
    private val cincu = GeoBounds(24.4, 45.6, 25.2, 46.2)

    private val noNetwork =
        object : MapSourceClient {
            override suspend fun head(url: String): RemoteFileInfo = error("no network in this test")

            override fun ranges(url: String, etag: String?): RangeSource = error("no network in this test")
        }

    private fun controller() = MapDownloadController(
        scope = CoroutineScope(Dispatchers.Unconfined),
        fileSystem = fileSystem,
        downloadDir = downloadDir,
        mapsDir = "/files/pmtiles".toPath(),
        client = noNetwork,
        isDemoActive = { false },
        freeBytes = { Long.MAX_VALUE },
        nowEpochMillis = { NOW_SECONDS * 1_000 },
        newId = { "map1" },
        onInstalled = { _, _ -> },
    )

    private fun estimated(fileSize: Long) = MapDownloadState.Estimated(
        MapDownloadEstimate(
            bounds = cincu,
            minZoom = 0,
            maxZoom = 15,
            tiles = 6_822,
            fileSize = fileSize,
            transferBytes = fileSize,
            editionDate = "20261111",
            sourceHost = "build.protomaps.com",
        ),
    )

    private fun text(resource: StringResource) = getString(resource)

    // collectAsStateWithLifecycle wants a Main dispatcher, which a desktop test has none of.
    @BeforeTest
    fun setMain() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun resetMain() {
        Dispatchers.resetMain()
    }

    @Test
    fun `a valid area can be measured but not yet downloaded, and the privacy notice is shown`() = runComposeUiTest {
        setContent {
            DownloadPanel(controller(), MapDownloadState.Idle, cincu, demo = false, onUseView = {}, onClose = {})
        }

        onNodeWithText(text(Res.string.offline_map_download_privacy)).assertIsDisplayed()
        onNodeWithText(text(Res.string.offline_map_download_compute)).assertIsEnabled()
        onNodeWithText(text(Res.string.offline_map_download_start)).assertIsNotEnabled()
        onNodeWithText(text(Res.string.offline_map_download_use_view)).assertIsEnabled()
    }

    @Test
    fun `an area too small says so and cannot be measured`() = runComposeUiTest {
        setContent {
            DownloadPanel(controller(), MapDownloadState.Idle, null, demo = false, onUseView = {}, onClose = {})
        }

        onNodeWithText(text(Res.string.offline_map_download_error_tiny)).assertIsDisplayed()
        onNodeWithText(text(Res.string.offline_map_download_compute)).assertIsNotEnabled()
    }

    @Test
    fun `demo mode turns the download off`() = runComposeUiTest {
        setContent {
            DownloadPanel(controller(), estimated(15_000_000), cincu, demo = true, onUseView = {}, onClose = {})
        }

        onNodeWithText(text(Res.string.offline_map_download_compute)).assertIsNotEnabled()
        onNodeWithText(text(Res.string.offline_map_download_start)).assertIsNotEnabled()
    }

    @Test
    fun `once measured the download can start`() = runComposeUiTest {
        setContent {
            DownloadPanel(controller(), estimated(15_000_000), cincu, demo = false, onUseView = {}, onClose = {})
        }

        onNodeWithText(text(Res.string.offline_map_download_compute)).assertIsNotEnabled()
        onNodeWithText(text(Res.string.offline_map_download_start)).assertIsEnabled()
    }

    @Test
    fun `above 50 MB the download asks first`() = runComposeUiTest {
        setContent {
            DownloadPanel(controller(), estimated(60_000_000), cincu, demo = false, onUseView = {}, onClose = {})
        }

        onNodeWithText(text(Res.string.offline_map_download_start)).performScrollTo().performClick()

        // Nothing in the panel says "Cancel" before a download starts: this is the confirmation dialog.
        onNodeWithText(text(Res.string.cancel)).assertIsDisplayed()
    }

    @Test
    fun `while downloading only cancel is offered`() = runComposeUiTest {
        setContent {
            DownloadPanel(
                controller(),
                MapDownloadState.Downloading("Cincu", 1_000_000, 15_000_000),
                cincu,
                demo = false,
                onUseView = {},
                onClose = {},
            )
        }

        onNodeWithText(text(Res.string.cancel)).assertIsEnabled()
        onNodeWithText(text(Res.string.offline_map_download_use_view)).assertIsNotEnabled()
        onNodeWithText(text(Res.string.offline_map_download_start)).assertIsNotEnabled()
        onNodeWithText(text(Res.string.close)).assertIsNotEnabled()
    }

    @Test
    fun `a download interrupted in an earlier run can be resumed`() = runComposeUiTest {
        fileSystem.createDirectories(downloadDir)
        fileSystem.write(downloadDir / "map1.part") { writeUtf8("partial") }
        DownloadJournal.save(
            fileSystem,
            downloadDir / "map1.journal.json",
            DownloadJournal(
                sourceUrl = "https://build.protomaps.com/20261111.pmtiles",
                etag = "\"abc\"",
                west = cincu.west,
                south = cincu.south,
                east = cincu.east,
                north = cincu.north,
                minZoom = 0,
                maxZoom = 15,
                totalSize = 15_000_000,
                rangeCount = 10,
                doneRanges = listOf(0, 1, 2, 3),
                startedAtEpochSeconds = NOW_SECONDS - 3_600,
                mapId = "map1",
                name = "Cincu",
            ),
        )
        val controller = controller().also { it.scanPending() }
        setContent {
            DownloadPanel(controller, MapDownloadState.Idle, cincu, demo = false, onUseView = {}, onClose = {})
        }

        onNodeWithText(text(Res.string.offline_map_download_resume)).performScrollTo().assertIsEnabled()
    }

    private companion object {
        const val NOW_SECONDS = 1_762_862_400L
    }
}
