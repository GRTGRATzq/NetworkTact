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
package org.meshtastic.feature.settings.freshness

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.meshtastic.core.model.freshness.FreshnessSettings
import org.meshtastic.core.testing.FakeFreshnessPrefs
import org.meshtastic.core.testing.FakeNodeRepository
import org.meshtastic.core.testing.FakeRadioConfigRepository
import org.meshtastic.core.testing.FakeUiPrefs
import org.meshtastic.core.ui.freshness.NetworkFreshnessSource
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
class FreshnessSettingsViewModelTest {
    private val prefs = FakeFreshnessPrefs()
    private val uiPrefs = FakeUiPrefs()
    private lateinit var viewModel: FreshnessSettingsViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val nodes = FakeNodeRepository()
        viewModel =
            FreshnessSettingsViewModel(
                NetworkFreshnessSource(nodes, FakeRadioConfigRepository(), prefs),
                uiPrefs,
                clockSeconds = { 1_790_000_000L },
            )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun the_defaults_give_35_minutes() = runTest {
        val collector = launch { viewModel.state.collect {} }
        assertEquals(35.minutes, viewModel.state.first().threshold)
        assertEquals(FreshnessSettings.Default, viewModel.state.value.settings)
        collector.cancel()
    }

    @Test
    fun nothing_changes_outside_command_post_mode() {
        uiPrefs.setCommandPostMode(false)
        assertFalse(viewModel.update(FreshnessSettings(expectedInterval = 1.hours)))
        assertEquals(FreshnessSettings.Default, prefs.settings.value)
    }

    @Test
    fun the_command_post_changes_the_settings_and_the_threshold_follows() = runTest {
        uiPrefs.setCommandPostMode(true)
        val collector = launch { viewModel.state.collect {} }
        assertTrue(viewModel.update(FreshnessSettings(expectedInterval = 1.hours)))
        assertEquals(1.hours, prefs.settings.value.expectedInterval)
        assertEquals(125.minutes, viewModel.state.first { it.settings.expectedInterval == 1.hours }.threshold)
        collector.cancel()
    }
}
