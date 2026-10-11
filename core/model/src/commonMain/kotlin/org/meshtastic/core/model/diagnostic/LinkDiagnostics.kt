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
package org.meshtastic.core.model.diagnostic

import org.meshtastic.core.model.ConnectionState
import org.meshtastic.core.model.freshness.FreshnessThresholds
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** Builds a [LinkDiagnostic] from local data only. */
object LinkDiagnostics {

    val WINDOW_SHORT = 5.minutes
    val WINDOW_MEDIUM = 15.minutes
    val WINDOW_LONG = 60.minutes

    /** Under this level the battery is flagged, as in the node list (`MaterialBatteryInfo`). */
    const val LOW_BATTERY_PERCENT = 20

    /** Battery levels above this mean external power (the firmware reports 101). */
    const val FULL_BATTERY_PERCENT = 100

    fun build(input: DiagnosticInput, thresholds: FreshnessThresholds = FreshnessThresholds.Default): LinkDiagnostic {
        val nowSeconds = input.nowMillis / MILLIS_PER_SECOND
        val ages = heardAges(input.nodes, input.myNodeNum, nowSeconds, thresholds)
        val lastHeard =
            ages.minByOrNull { it.second }?.let { (node, age) -> LastHeard(node.name, node.lastHeardEpoch(), age) }
        val noRecentContact = lastHeard == null || lastHeard.age > thresholds.recentContact
        val lora = loraCheck(input.localLora, input.expectedLora)
        val battery = batteryFact(input.batteryLevel)
        return LinkDiagnostic(
            connection = input.connection,
            lastRadioPacketAge =
            input.lastRadioPacketMillis?.let { (input.nowMillis - it).milliseconds.coerceAtLeast(Duration.ZERO) },
            lastRadioPacketMillis = input.lastRadioPacketMillis,
            counts =
            HeardCounts(
                within5Min = ages.count { it.second <= WINDOW_SHORT },
                within15Min = ages.count { it.second <= WINDOW_MEDIUM },
                within60Min = ages.count { it.second <= WINDOW_LONG },
            ),
            lastHeard = lastHeard,
            noRecentContact = noRecentContact,
            lora = lora,
            battery = battery,
            suggestions = suggestions(input.connection, noRecentContact, lora, battery),
        )
    }

    /**
     * Other nodes with a usable `lastHeard`, and how long ago. A time further in the future than the clock tolerance is
     * left out; a smaller one counts as heard now.
     */
    internal fun heardAges(
        nodes: List<HeardNode>,
        myNodeNum: Int?,
        nowSeconds: Long,
        thresholds: FreshnessThresholds,
    ): List<Pair<HeardNode, Duration>> = nodes
        .filter { it.num != myNodeNum && it.lastHeardEpoch() > 0L }
        .map { it to (nowSeconds - it.lastHeardEpoch()).seconds }
        .filter { -it.second <= thresholds.clockTolerance }
        .map { it.first to it.second.coerceAtLeast(Duration.ZERO) }

    fun loraCheck(local: LoraSetting?, expected: LoraSetting?): LoraCheck = when {
        local == null -> LoraCheck.NotReceived

        expected == null -> LoraCheck.NoExpected(local)

        local == expected -> LoraCheck.Same(local)

        else ->
            LoraCheck.Different(
                local = local,
                expected = expected,
                regionDiffers = local.region != expected.region,
                presetDiffers = local.preset != expected.preset,
            )
    }

    fun batteryFact(level: Int?): BatteryFact = when {
        level == null || level < 0 -> BatteryFact.NotReported
        level > FULL_BATTERY_PERCENT -> BatteryFact.ExternalPower
        else -> BatteryFact.Level(level, low = level < LOW_BATTERY_PERCENT)
    }

    private fun suggestions(
        connection: ConnectionState,
        noRecentContact: Boolean,
        lora: LoraCheck,
        battery: BatteryFact,
    ): List<DiagnosticSuggestion> = buildList {
        if (connection != ConnectionState.Connected) add(DiagnosticSuggestion.CHECK_RADIO_LINK)
        val local =
            when (lora) {
                is LoraCheck.NoExpected -> lora.local
                is LoraCheck.Same -> lora.local
                is LoraCheck.Different -> lora.local
                LoraCheck.NotReceived -> null
            }
        if (local?.region == LoraSetting.UNSET_REGION) add(DiagnosticSuggestion.SET_REGION)
        if (lora is LoraCheck.Different) add(DiagnosticSuggestion.CHECK_LORA_SETTINGS)
        if (battery is BatteryFact.Level && battery.low) add(DiagnosticSuggestion.CHARGE_BATTERY)
        if (noRecentContact) {
            add(DiagnosticSuggestion.MOVE_HIGHER)
            add(DiagnosticSuggestion.CHECK_ANTENNA)
        }
    }

    private const val MILLIS_PER_SECOND = 1000L
}

// Protobuf fixed32 fields arrive as signed Ints; read them unsigned so dates past 2038 stay positive.
internal fun HeardNode.lastHeardEpoch(): Long = lastHeardSeconds.toUInt().toLong()
