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
package org.meshtastic.core.model.freshness

import org.meshtastic.core.model.Node
import org.meshtastic.proto.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class NetworkFreshnessTest {
    private val now = 1_800_000_000L
    private val longFast = ModemParams.ofPreset("LONG_FAST")
    private val mediumSlow = ModemParams.ofPreset("MEDIUM_SLOW")
    private val mediumFast = ModemParams.ofPreset("MEDIUM_FAST")

    /** Whole minutes, as the screens show the threshold. */
    private fun Duration.minutesOnly() = inWholeMinutes

    private fun auto(nodes: Int, params: ModemParams) =
        NetworkFreshness.threshold(FreshnessSettings.Default, nodes, params).minutesOnly()

    @Test
    fun up_to_40_nodes_the_default_threshold_is_35_minutes_whatever_the_preset() {
        assertEquals(35, auto(0, longFast))
        assertEquals(35, auto(40, longFast))
        assertEquals(35, auto(40, mediumFast))
        assertEquals(35.minutes, NetworkFreshness.DEFAULT_STATE.threshold)
    }

    @Test
    fun the_coefficient_is_the_firmware_one() {
        assertEquals(1.0, NetworkFreshness.coefficient(40, longFast))
        assertEquals(1.8192, NetworkFreshness.coefficient(50, longFast), 1e-9)
        assertEquals(1.4096, NetworkFreshness.coefficient(50, mediumSlow), 1e-9)
        assertEquals(1.2048, NetworkFreshness.coefficient(50, mediumFast), 1e-9)
    }

    @Test
    fun thresholds_validated_for_15_minutes_follow_the_size_of_the_network() {
        // 2 × 15 min × coefficient + 5 min, rounded to the nearest minute (table of 13 November 2026).
        assertEquals(listOf(60L, 182L, 428L), listOf(50, 100, 200).map { auto(it, longFast) })
        assertEquals(listOf(47L, 109L, 232L), listOf(50, 100, 200).map { auto(it, mediumSlow) })
        assertEquals(listOf(41L, 72L, 133L), listOf(50, 100, 200).map { auto(it, mediumFast) })
    }

    @Test
    fun the_default_firmware_interval_of_an_hour_gives_2_h_05() {
        val hourly = FreshnessSettings(expectedInterval = 1.hours)
        assertEquals(125, NetworkFreshness.threshold(hourly, 10, longFast).minutesOnly())
    }

    @Test
    fun the_fixed_threshold_ignores_the_network_size() {
        val fixed = FreshnessSettings(autoScale = false, fixedThreshold = 2.hours)
        assertEquals(2.hours, NetworkFreshness.threshold(fixed, 200, longFast))
        assertEquals(35.minutes, FreshnessSettings.Default.fixedThreshold)
    }

    private fun custom(spreadFactor: Int, bandwidth: Int) = Config.LoRaConfig.Builder()
        .also {
            it.use_preset = false
            it.spread_factor = spreadFactor
            it.bandwidth = bandwidth
        }
        .build()

    private fun preset(preset: Config.LoRaConfig.ModemPreset) = Config.LoRaConfig.Builder()
        .also {
            it.use_preset = true
            it.modem_preset = preset
        }
        .build()

    @Test
    fun presets_and_custom_parameters_read_as_the_firmware_does() {
        assertEquals(ModemParams(10, 125.0), ModemParams.of(custom(10, 125)))
        assertEquals(ModemParams(12, 62.5), ModemParams.of(custom(12, 62)))
        assertEquals(ModemParams.LONG_FAST, ModemParams.of(custom(0, 0)))
        assertEquals(ModemParams.LONG_FAST, ModemParams.of(null))
        assertEquals(ModemParams.LONG_FAST, ModemParams.ofPreset("SOMETHING_NEW"))
        assertEquals(mediumFast, ModemParams.of(preset(Config.LoRaConfig.ModemPreset.MEDIUM_FAST)))
    }

    @Test
    fun online_nodes_are_those_heard_within_two_hours() {
        val nodes =
            listOf(
                Node(num = 1, lastHeard = (now - 60).toInt()),
                Node(num = 2, lastHeard = (now - 2 * 3600 + 1).toInt()),
                Node(num = 3, lastHeard = (now - 2 * 3600).toInt()),
                Node(num = 4, lastHeard = 0),
            )
        assertEquals(2, NetworkFreshness.onlineNodes(nodes, now))
    }

    @Test
    fun the_state_gives_the_same_threshold_to_position_and_contact() {
        val nodes = (1..50).map { Node(num = it, lastHeard = (now - 300).toInt()) }
        val lora = preset(Config.LoRaConfig.ModemPreset.LONG_FAST)
        val state = NetworkFreshness.state(FreshnessSettings.Default, nodes, lora, now)
        assertEquals(50, state.onlineNodes)
        assertEquals("LONG_FAST", state.presetName)
        assertEquals(60, state.threshold.minutesOnly())
        assertEquals(state.threshold, state.thresholds.stalePosition)
        assertEquals(state.threshold, state.thresholds.recentContact)
        assertEquals(FreshnessThresholds.Default.clockTolerance, state.thresholds.clockTolerance)
        assertNull(NetworkFreshness.state(FreshnessSettings.Default, nodes, null, now).presetName)
    }

    @Test
    fun my_radio_keeps_its_ten_minutes() {
        assertEquals(10.minutes, FreshnessThresholds.Default.stalePosition)
        assertTrue(NetworkFreshness.DEFAULT_STATE.thresholds.stalePosition > FreshnessThresholds.Default.stalePosition)
    }
}
