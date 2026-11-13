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
package org.meshtastic.feature.node.commandpost

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.freshness.FreshnessSettings
import org.meshtastic.core.model.freshness.PositionState
import org.meshtastic.core.testing.FakeFreshnessPrefs
import org.meshtastic.core.testing.FakeNodeRepository
import org.meshtastic.core.testing.FakeRadioConfigRepository
import org.meshtastic.core.ui.freshness.NetworkFreshnessSource
import org.meshtastic.proto.Position
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.minutes

/** The command post view judges positions by the network threshold, not by a fixed 10 minutes. */
@OptIn(ExperimentalCoroutinesApi::class)
class CommandPostViewModelTest {
    private val now = 1_790_000_000L
    private val nodes = FakeNodeRepository()
    private val prefs = FakeFreshnessPrefs()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = CommandPostViewModel(
        nodes,
        NetworkFreshnessSource(nodes, FakeRadioConfigRepository(), prefs),
        clockSeconds = { now },
    )

    /** [count] radios heard 5 minutes ago; radio 1 has a position fixed 40 minutes ago. */
    private fun network(count: Int) = nodes.setNodes(
        (1..count).map { num ->
            Node(
                num = num,
                lastHeard = (now - 300).toInt(),
                position =
                Position.Builder()
                    .also {
                        if (num == 1) {
                            it.latitude_i = 488_566_000
                            it.longitude_i = 23_522_000
                            it.timestamp = (now - 40 * 60).toInt()
                        }
                    }
                    .build(),
            )
        },
    )

    /** Radio 1's position once the rows show [T]: the rows are first built with the default threshold. */
    private suspend inline fun <reified T : PositionState> CommandPostViewModel.firstRowPosition(): T =
        rows.first { rows -> rows.any { it.num == 1 && it.position is T } }.first { it.num == 1 }.position as T

    @Test
    fun up_to_40_radios_a_position_of_40_minutes_is_old() = runTest {
        network(10)
        val viewModel = viewModel()
        val collector = launch { viewModel.rows.collect {} }
        assertEquals(40.minutes, viewModel.firstRowPosition<PositionState.Stale>().age)
        assertEquals(35.minutes, viewModel.freshness.first { it.onlineNodes == 10 }.threshold)
        collector.cancel()
    }

    @Test
    fun at_50_radios_on_long_fast_the_threshold_is_an_hour_and_the_age_still_shows() = runTest {
        network(50)
        val viewModel = viewModel()
        val collector = launch { viewModel.rows.collect {} }
        assertEquals(40.minutes, viewModel.firstRowPosition<PositionState.Fresh>().age)
        assertEquals(60.minutes, viewModel.freshness.first { it.onlineNodes == 50 }.threshold)
        collector.cancel()
    }

    @Test
    fun a_fixed_threshold_set_by_hand_wins() = runTest {
        prefs.setSettings(FreshnessSettings(autoScale = false, fixedThreshold = 45.minutes))
        network(10)
        val viewModel = viewModel()
        val collector = launch { viewModel.rows.collect {} }
        assertIs<PositionState.Fresh>(viewModel.firstRowPosition<PositionState.Fresh>())
        assertEquals(45.minutes, viewModel.freshness.first { !it.settings.autoScale }.threshold)
        collector.cancel()
    }
}
