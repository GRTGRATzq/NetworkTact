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
import kotlin.math.max
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * NetworkTact: how long another radio's position (and its last contact) stays current, set in command post mode and
 * stored on this phone only.
 *
 * @property expectedInterval the position broadcast interval the radios were given (900 s in `PREPARER-LES-RADIOS.md`).
 * @property autoScale whether the threshold follows the size of the network, as the firmware stretches its intervals.
 * @property fixedThreshold the threshold used when [autoScale] is off.
 */
data class FreshnessSettings(
    val expectedInterval: Duration = DEFAULT_EXPECTED_INTERVAL,
    val autoScale: Boolean = true,
    val fixedThreshold: Duration = DEFAULT_FIXED_THRESHOLD,
) {
    companion object {
        val DEFAULT_EXPECTED_INTERVAL = 15.minutes
        val DEFAULT_FIXED_THRESHOLD = 35.minutes

        /** The intervals offered for [expectedInterval]. */
        val INTERVAL_CHOICES = listOf(5.minutes, 15.minutes, 30.minutes, 1.hours, 2.hours, 3.hours)

        /** The thresholds offered for [fixedThreshold]. */
        val THRESHOLD_CHOICES = listOf(10.minutes, 20.minutes, 35.minutes, 1.hours, 2.hours, 3.hours, 6.hours, 12.hours)

        val Default = FreshnessSettings()
    }
}

/** The modem parameters the firmware's congestion scaling reads: spreading factor and bandwidth in kHz. */
data class ModemParams(val spreadFactor: Int, val bandwidthKHz: Double) {
    companion object {
        /** LONG_FAST: what the firmware falls back to for an unknown preset. */
        val LONG_FAST = ModemParams(11, 250.0)

        /**
         * The parameters of [lora], as the firmware's `modemPresetToParams` and `bwCodeToKHz` give them (2.8.2,
         * `MeshRadio.h`; 2.4 GHz wide LoRa left out). LONG_FAST when the configuration is not known yet.
         */
        fun of(lora: Config.LoRaConfig?): ModemParams = when {
            lora == null -> LONG_FAST
            lora.use_preset -> ofPreset(lora.modem_preset.name)
            else -> custom(lora.spread_factor, lora.bandwidth)
        }

        /** The parameters of the preset named [name] (proto enum name); LONG_FAST for an unknown name. */
        fun ofPreset(name: String?): ModemParams = PRESETS[name] ?: LONG_FAST

        private fun custom(spreadFactor: Int, bandwidthCode: Int): ModemParams {
            val sf = spreadFactor.takeIf { it in MIN_SF..MAX_SF } ?: LONG_FAST.spreadFactor
            val bw =
                BANDWIDTH_CODES[bandwidthCode] ?: bandwidthCode.toDouble().takeIf { it > 0 } ?: LONG_FAST.bandwidthKHz
            return ModemParams(sf, bw)
        }

        private const val MIN_SF = 5
        private const val MAX_SF = 12

        private val PRESETS =
            mapOf(
                "SHORT_TURBO" to ModemParams(7, 500.0),
                "SHORT_FAST" to ModemParams(7, 250.0),
                "SHORT_SLOW" to ModemParams(8, 250.0),
                "MEDIUM_FAST" to ModemParams(9, 250.0),
                "MEDIUM_SLOW" to ModemParams(10, 250.0),
                "MEDIUM_TURBO" to ModemParams(9, 500.0),
                "LONG_TURBO" to ModemParams(11, 500.0),
                "LONG_FAST" to LONG_FAST,
                "LONG_MODERATE" to ModemParams(11, 125.0),
                "LONG_SLOW" to ModemParams(12, 125.0),
                "LITE_FAST" to ModemParams(9, 125.0),
                "LITE_SLOW" to ModemParams(10, 125.0),
                "NARROW_FAST" to ModemParams(7, 62.5),
                "NARROW_SLOW" to ModemParams(8, 62.5),
                "TINY_FAST" to ModemParams(7, 15.6),
                "TINY_SLOW" to ModemParams(8, 15.6),
            )

        private val BANDWIDTH_CODES =
            mapOf(
                8 to 7.8,
                10 to 10.4,
                16 to 15.6,
                21 to 20.8,
                31 to 31.25,
                42 to 41.7,
                62 to 62.5,
                200 to 203.125,
                400 to 406.25,
                800 to 812.5,
                1600 to 1625.0,
            )
    }
}

/**
 * The threshold in force for other radios: one value for "old position" and "seen recently", since a radio's most
 * frequent packet is its position.
 *
 * @property threshold a position older than this is OLD, a radio silent for longer is no longer seen recently.
 * @property onlineNodes nodes heard within [NetworkFreshness.ONLINE_WINDOW], as the firmware counts them.
 * @property coefficient the firmware's congestion scaling at that count (1 up to 40 nodes).
 * @property presetName my radio's preset (proto name), or null for custom modem parameters or an unknown setting.
 */
data class NetworkFreshnessState(
    val settings: FreshnessSettings,
    val threshold: Duration,
    val onlineNodes: Int,
    val coefficient: Double,
    val presetName: String?,
) {
    /** The rules of the command post view with this threshold; clock tolerance and refresh are unchanged. */
    val thresholds: FreshnessThresholds
        get() = FreshnessThresholds.Default.copy(recentContact = threshold, stalePosition = threshold)
}

/**
 * Old position and recent contact of other radios, set from the firmware's own behaviour (2.8.2, `Default.h`,
 * `congestionScalingCoefficient`): a radio broadcasts its position every expected interval, stretched by `1 + (N - 40)
 * × 2^SF / (BW × 100)` once more than 40 nodes were heard within 2 h. The threshold is twice that interval, which
 * tolerates one lost broadcast, plus 5 minutes. My own radio's position keeps its fixed 10 minutes
 * ([FreshnessThresholds.Default]): the radio hands it to the phone every minute.
 */
object NetworkFreshness {
    /** The firmware's `NUM_ONLINE_SECS`: a node heard within this window is online. */
    val ONLINE_WINDOW = 2.hours

    /** The node count up to which the firmware does not stretch its intervals. */
    const val SCALING_START_NODES = 40

    private const val MISSED_BROADCASTS = 2
    private val MARGIN = 5.minutes
    private const val SCALING_DIVISOR = 100.0

    /** The firmware's congestion coefficient for [onlineNodes] nodes on [params]. */
    fun coefficient(onlineNodes: Int, params: ModemParams): Double {
        if (onlineNodes <= SCALING_START_NODES) return 1.0
        val factor = (1 shl params.spreadFactor) / (params.bandwidthKHz * SCALING_DIVISOR)
        return 1.0 + (onlineNodes - SCALING_START_NODES) * factor
    }

    /**
     * The threshold for [settings]: `2 × interval × coefficient + 5 min`, rounded to the nearest minute, or the fixed
     * one when scaling is off.
     */
    fun threshold(settings: FreshnessSettings, onlineNodes: Int, params: ModemParams): Duration =
        if (settings.autoScale) {
            val raw = settings.expectedInterval * (MISSED_BROADCASTS * coefficient(onlineNodes, params)) + MARGIN
            ((raw.inWholeSeconds + HALF_MINUTE_SECONDS) / SECONDS_PER_MINUTE).minutes
        } else {
            settings.fixedThreshold
        }

    private const val HALF_MINUTE_SECONDS = 30
    private const val SECONDS_PER_MINUTE = 60

    /** Nodes heard within [ONLINE_WINDOW] of [nowSeconds], this phone's radio included, as the firmware counts. */
    fun onlineNodes(nodes: Collection<Node>, nowSeconds: Long): Int = nodes.count { node ->
        val heard = node.lastHeard.toUInt().toLong()
        heard > 0 && max(0L, nowSeconds - heard) < ONLINE_WINDOW.inWholeSeconds
    }

    /** Everything at once, for the screens. */
    fun state(
        settings: FreshnessSettings,
        nodes: Collection<Node>,
        lora: Config.LoRaConfig?,
        nowSeconds: Long,
    ): NetworkFreshnessState {
        val params = ModemParams.of(lora)
        val online = onlineNodes(nodes, nowSeconds)
        return NetworkFreshnessState(
            settings = settings,
            threshold = threshold(settings, online, params),
            onlineNodes = online,
            coefficient = coefficient(online, params),
            presetName = lora?.takeIf { it.use_preset }?.modem_preset?.name,
        )
    }

    /** The threshold before anything is known: 15-minute interval, at most 40 nodes, i.e. 35 minutes. */
    val DEFAULT_STATE: NetworkFreshnessState =
        NetworkFreshnessState(
            settings = FreshnessSettings.Default,
            threshold = threshold(FreshnessSettings.Default, 0, ModemParams.LONG_FAST),
            onlineNodes = 0,
            coefficient = 1.0,
            presetName = null,
        )
}
