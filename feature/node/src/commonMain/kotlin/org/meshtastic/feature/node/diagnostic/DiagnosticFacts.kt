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

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.util.DateFormatter
import org.meshtastic.core.model.ConnectionState
import org.meshtastic.core.model.diagnostic.BatteryFact
import org.meshtastic.core.model.diagnostic.HeardCounts
import org.meshtastic.core.model.diagnostic.LastHeard
import org.meshtastic.core.model.diagnostic.LinkDiagnostic
import org.meshtastic.core.model.diagnostic.LoraCheck
import org.meshtastic.core.model.diagnostic.LoraSetting
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.diag_battery_external
import org.meshtastic.core.resources.diag_battery_level
import org.meshtastic.core.resources.diag_battery_low
import org.meshtastic.core.resources.diag_battery_unknown
import org.meshtastic.core.resources.diag_expected_value
import org.meshtastic.core.resources.diag_heard_counts
import org.meshtastic.core.resources.diag_last_node
import org.meshtastic.core.resources.diag_last_packet
import org.meshtastic.core.resources.diag_last_packet_none
import org.meshtastic.core.resources.diag_link_connected
import org.meshtastic.core.resources.diag_link_connecting
import org.meshtastic.core.resources.diag_link_disconnected
import org.meshtastic.core.resources.diag_link_sleeping
import org.meshtastic.core.resources.diag_lora_custom
import org.meshtastic.core.resources.diag_lora_different
import org.meshtastic.core.resources.diag_lora_no_expected
import org.meshtastic.core.resources.diag_lora_not_received
import org.meshtastic.core.resources.diag_lora_region_unset
import org.meshtastic.core.resources.diag_lora_same
import org.meshtastic.core.resources.diag_no_node_ever
import org.meshtastic.core.resources.diag_no_node_since
import org.meshtastic.core.ui.util.formatElapsed

// The words of each fact. Times of day are the phone's local time, with the date when not within the last 24 h.

@Composable
internal fun linkText(connection: ConnectionState): String = stringResource(
    when (connection) {
        ConnectionState.Connected -> Res.string.diag_link_connected
        ConnectionState.Connecting -> Res.string.diag_link_connecting
        ConnectionState.DeviceSleep -> Res.string.diag_link_sleeping
        ConnectionState.Disconnected -> Res.string.diag_link_disconnected
    },
)

@Composable
internal fun lastPacketText(diagnostic: LinkDiagnostic): String {
    val millis = diagnostic.lastRadioPacketMillis
    val age = diagnostic.lastRadioPacketAge
    return if (millis == null || age == null) {
        stringResource(Res.string.diag_last_packet_none)
    } else {
        stringResource(Res.string.diag_last_packet, DateFormatter.formatShortDate(millis), formatElapsed(age))
    }
}

@Composable
internal fun countsText(counts: HeardCounts): String =
    stringResource(Res.string.diag_heard_counts, counts.within5Min, counts.within15Min, counts.within60Min)

@Composable
internal fun lastHeardText(lastHeard: LastHeard?, noRecentContact: Boolean): String = when {
    lastHeard == null -> stringResource(Res.string.diag_no_node_ever)

    noRecentContact ->
        stringResource(
            Res.string.diag_no_node_since,
            formatElapsed(lastHeard.age),
            lastHeard.name,
            DateFormatter.formatShortDate(lastHeard.heardSeconds * MILLIS_PER_SECOND),
        )

    else ->
        stringResource(
            Res.string.diag_last_node,
            lastHeard.name,
            DateFormatter.formatShortDate(lastHeard.heardSeconds * MILLIS_PER_SECOND),
            formatElapsed(lastHeard.age),
        )
}

@Composable
internal fun loraText(lora: LoraCheck): String = when (lora) {
    LoraCheck.NotReceived -> stringResource(Res.string.diag_lora_not_received)

    is LoraCheck.NoExpected ->
        stringResource(Res.string.diag_lora_no_expected, regionText(lora.local), presetText(lora.local))

    is LoraCheck.Same -> stringResource(Res.string.diag_lora_same, regionText(lora.local), presetText(lora.local))

    is LoraCheck.Different ->
        stringResource(
            Res.string.diag_lora_different,
            regionText(lora.local),
            presetText(lora.local),
            regionText(lora.expected),
            presetText(lora.expected),
        )
}

/** "EU_868 · LONG_FAST", the names the radio's LoRa settings use. */
@Composable
internal fun loraSettingText(setting: LoraSetting): String =
    stringResource(Res.string.diag_expected_value, regionText(setting), presetText(setting))

@Composable
private fun regionText(setting: LoraSetting): String = if (setting.region == LoraSetting.UNSET_REGION) {
    stringResource(Res.string.diag_lora_region_unset)
} else {
    setting.region
}

@Composable
private fun presetText(setting: LoraSetting): String = setting.preset ?: stringResource(Res.string.diag_lora_custom)

@Composable
internal fun batteryText(battery: BatteryFact): String = when (battery) {
    BatteryFact.NotReported -> stringResource(Res.string.diag_battery_unknown)

    BatteryFact.ExternalPower -> stringResource(Res.string.diag_battery_external)

    is BatteryFact.Level ->
        stringResource(
            if (battery.low) Res.string.diag_battery_low else Res.string.diag_battery_level,
            battery.percent,
        )
}

private const val MILLIS_PER_SECOND = 1000L
