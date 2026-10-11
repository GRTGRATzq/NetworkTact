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
package org.meshtastic.feature.node.diagnostic

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.meshtastic.core.model.ConnectionState
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.diagnostic.BatteryFact
import org.meshtastic.core.model.diagnostic.DiagnosticSuggestion
import org.meshtastic.core.model.diagnostic.HeardCounts
import org.meshtastic.core.model.diagnostic.LoraCheck
import org.meshtastic.core.model.diagnostic.LoraSetting
import org.meshtastic.core.repository.RadioActivityProvider
import org.meshtastic.core.testing.FakeExpectedLoraPrefs
import org.meshtastic.core.testing.FakeNodeRepository
import org.meshtastic.core.testing.FakeRadioConfigRepository
import org.meshtastic.core.testing.FakeServiceRepository
import org.meshtastic.core.testing.FakeUiPrefs
import org.meshtastic.proto.Config
import org.meshtastic.proto.DeviceMetrics
import org.meshtastic.proto.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
class DiagnosticViewModelTest {

    private val nowSeconds = 1_800_000_000L
    private val nowMillis = nowSeconds * 1000
    private val euLongFast = LoraSetting("EU_868", "LONG_FAST")

    private fun node(num: Int, longName: String, heardMinutesAgo: Int?, battery: Int? = null) = Node(
        num = num,
        user = User.Builder().also { it.long_name = longName }.build(),
        lastHeard = heardMinutesAgo?.let { (nowSeconds - it * 60L).toInt() } ?: 0,
        deviceMetrics = DeviceMetrics.Builder().also { it.battery_level = battery }.build(),
    )

    private val pc0 = node(1, "PC-0", 0, battery = 15)
    private val nodes =
        listOf(pc0, node(2, "ALPHA-1 [Alpha]", 18), node(3, "BRAVO-2 [Bravo]", 40), node(4, "CHARLIE-3", null))

    private val radio = RadioFacts(ConnectionState.Connected, nowMillis - 5_000, euLongFast)

    @Test
    fun noNodeHeardRecentlyGivesFactsWithoutTheTeamSuffixAndSuggestions() {
        val state = buildDiagnosticUiState(radio, euLongFast, false, nodes, pc0, nowMillis)
        val diagnostic = state.diagnostic
        assertEquals(HeardCounts(0, 0, 2), diagnostic.counts)
        assertEquals("ALPHA-1", diagnostic.lastHeard?.name)
        assertEquals(18.minutes, diagnostic.lastHeard?.age)
        assertEquals(LoraCheck.Same(euLongFast), diagnostic.lora)
        assertEquals(BatteryFact.Level(15, low = true), diagnostic.battery)
        assertEquals(
            listOf(
                DiagnosticSuggestion.CHARGE_BATTERY,
                DiagnosticSuggestion.MOVE_HIGHER,
                DiagnosticSuggestion.CHECK_ANTENNA,
            ),
            diagnostic.suggestions,
        )
    }

    @Test
    fun testTargetsAreHeardNodesOtherThanMineMostRecentFirst() {
        val state = buildDiagnosticUiState(radio, null, false, nodes, pc0, nowMillis)
        assertEquals(listOf("ALPHA-1", "BRAVO-2"), state.targets.map { it.name })
    }

    @Test
    fun ignoredNodesAreLeftOut() {
        val ignored = nodes + node(5, "DELTA-4", 1).copy(isIgnored = true)
        val state = buildDiagnosticUiState(radio, null, false, ignored, pc0, nowMillis)
        assertEquals(0, state.diagnostic.counts.within5Min)
        assertTrue(state.targets.none { it.num == 5 })
    }

    @Test
    fun customModemParametersHaveNoPreset() {
        val custom =
            Config.LoRaConfig.Builder()
                .also {
                    it.region = Config.LoRaConfig.RegionCode.EU_868
                    it.modem_preset = Config.LoRaConfig.ModemPreset.LONG_FAST
                    it.use_preset = false
                }
                .build()
        assertEquals(LoraSetting("EU_868", null), custom.toLoraSetting())
    }

    @Test
    fun choicesLeaveOutTheUnsetRegion() {
        assertFalse("UNSET" in DiagnosticViewModel.REGION_CHOICES)
        assertTrue("EU_868" in DiagnosticViewModel.REGION_CHOICES)
        assertTrue("LONG_FAST" in DiagnosticViewModel.PRESET_CHOICES)
    }

    @Test
    fun theExpectedSettingChangesInCommandPostModeOnly() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            val uiPrefs = FakeUiPrefs()
            val expected = FakeExpectedLoraPrefs()
            val viewModel =
                DiagnosticViewModel(
                    FakeNodeRepository(),
                    FakeServiceRepository(),
                    FakeRadioConfigRepository(),
                    expected,
                    object : RadioActivityProvider {
                        override val lastPacketFromRadioMillis = MutableStateFlow<Long?>(null)
                    },
                    uiPrefs,
                    clockMillis = { nowMillis },
                )
            uiPrefs.setCommandPostMode(false)
            assertFalse(viewModel.setExpected(euLongFast))
            assertNull(expected.expected.value)

            uiPrefs.setCommandPostMode(true)
            assertTrue(viewModel.setExpected(euLongFast))
            assertEquals(euLongFast, expected.expected.value)
            assertTrue(viewModel.setExpected(null))
            assertNull(expected.expected.value)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
