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
import kotlin.time.Duration

/**
 * A LoRa setting compared by name: the proto enum names of the region and of the modem preset (`EU_868`, `LONG_FAST`).
 * Names rather than proto types, so that the expected setting is stored as plain text.
 *
 * @property preset null when the radio uses custom modem parameters (`use_preset` off) instead of a preset.
 */
data class LoraSetting(val region: String, val preset: String?) {
    companion object {
        const val UNSET_REGION = "UNSET"
    }
}

/** A node other than my own radio, as the node database holds it. */
data class HeardNode(val num: Int, val name: String, val lastHeardSeconds: Int)

/** Everything the diagnostic reads. Nothing in it is obtained by transmitting. */
data class DiagnosticInput(
    val connection: ConnectionState,
    /** When my radio last passed a mesh packet to this phone, since the app started; null if never. */
    val lastRadioPacketMillis: Long?,
    val myNodeNum: Int?,
    val nodes: List<HeardNode>,
    /** The radio's LoRa setting, null until the radio has sent its configuration. */
    val localLora: LoraSetting?,
    /** The setting the command post entered, null when none was entered. */
    val expectedLora: LoraSetting?,
    /** `DeviceMetrics.battery_level` of my radio: null when not reported, above 100 when on external power. */
    val batteryLevel: Int?,
    val nowMillis: Long,
)

/** The node heard most recently, other than my radio. */
data class LastHeard(val name: String, val heardSeconds: Long, val age: Duration)

/** How many other nodes were heard within each window. */
data class HeardCounts(val within5Min: Int, val within15Min: Int, val within60Min: Int)

/** The radio's LoRa setting against the expected one. Facts only: a match does not prove the network works. */
sealed interface LoraCheck {
    /** The radio has not sent its configuration (not connected yet, or still loading). */
    data object NotReceived : LoraCheck

    data class NoExpected(val local: LoraSetting) : LoraCheck

    data class Same(val local: LoraSetting) : LoraCheck

    data class Different(
        val local: LoraSetting,
        val expected: LoraSetting,
        val regionDiffers: Boolean,
        val presetDiffers: Boolean,
    ) : LoraCheck
}

sealed interface BatteryFact {
    data object NotReported : BatteryFact

    data object ExternalPower : BatteryFact

    data class Level(val percent: Int, val low: Boolean) : BatteryFact
}

/** Something to try. Suggestions, never conclusions: the app cannot tell why nothing is heard. */
enum class DiagnosticSuggestion {
    /** The radio is not connected over Bluetooth: check it is on and close to the phone. */
    CHECK_RADIO_LINK,

    /**
     * The radio's region or preset differs from the expected one: radios on different settings never hear each other.
     */
    CHECK_LORA_SETTINGS,

    /** The radio's region is not set: it does not transmit at all. */
    SET_REGION,

    /** The radio's battery is low. */
    CHARGE_BATTERY,

    /** No other node heard recently: move to a high point, away from obstacles. */
    MOVE_HIGHER,

    /** No other node heard recently: check the antenna is screwed on and undamaged. */
    CHECK_ANTENNA,
}

/**
 * The local diagnostic: facts read from the phone and the radio, and suggestions. It never states that the network
 * works or that the radio is isolated: hearing nobody can mean nobody is in range, or that everyone is silent.
 */
data class LinkDiagnostic(
    val connection: ConnectionState,
    /** Age of the last packet my radio passed to the phone, null if none since the app started. */
    val lastRadioPacketAge: Duration?,
    val lastRadioPacketMillis: Long?,
    val counts: HeardCounts,
    /** Null when no other node was ever heard (or only with an inconsistent time). */
    val lastHeard: LastHeard?,
    /** True when no other node was heard within the "recent contact" threshold. */
    val noRecentContact: Boolean,
    val lora: LoraCheck,
    val battery: BatteryFact,
    val suggestions: List<DiagnosticSuggestion>,
)
