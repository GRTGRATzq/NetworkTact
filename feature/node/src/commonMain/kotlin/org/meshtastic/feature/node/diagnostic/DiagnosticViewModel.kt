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

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.KoinViewModel
import org.koin.core.annotation.Named
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.model.ConnectionState
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.diagnostic.DiagnosticInput
import org.meshtastic.core.model.diagnostic.HeardNode
import org.meshtastic.core.model.diagnostic.LinkDiagnostic
import org.meshtastic.core.model.diagnostic.LinkDiagnostics
import org.meshtastic.core.model.diagnostic.LoraSetting
import org.meshtastic.core.model.freshness.FreshnessThresholds
import org.meshtastic.core.model.team.TeamSuffix
import org.meshtastic.core.repository.ConnectionStateProvider
import org.meshtastic.core.repository.ExpectedLoraPrefs
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.repository.RadioActivityProvider
import org.meshtastic.core.repository.RadioConfigRepository
import org.meshtastic.core.repository.SCREEN_DATA
import org.meshtastic.core.repository.UiPrefs
import org.meshtastic.core.ui.viewmodel.stateInWhileSubscribed
import org.meshtastic.proto.Config

/** A node the link test can be sent to: one already heard, other than my radio. */
data class TestTarget(val num: Int, val name: String, val lastHeardSeconds: Int)

data class DiagnosticUiState(
    val diagnostic: LinkDiagnostic,
    /** My radio's LoRa setting, null until its configuration was received. */
    val localLora: LoraSetting?,
    val expected: LoraSetting?,
    /** Only command post mode may change the expected setting. */
    val commandPostMode: Boolean,
    /** Heard nodes, most recent first. */
    val targets: List<TestTarget>,
)

/**
 * The link diagnostic: local facts only, read through the screens' sources so demo mode shows fictitious data. Nothing
 * here transmits; the manual link test lives in [LinkTestViewModel].
 */
@KoinViewModel
class DiagnosticViewModel(
    @Named(SCREEN_DATA) nodeRepository: NodeRepository,
    @Named(SCREEN_DATA) connectionStateProvider: ConnectionStateProvider,
    @Named(SCREEN_DATA) radioConfigRepository: RadioConfigRepository,
    @Named(SCREEN_DATA) private val expectedLoraPrefs: ExpectedLoraPrefs,
    @Named(SCREEN_DATA) radioActivityProvider: RadioActivityProvider,
    @Named(SCREEN_DATA) private val uiPrefs: UiPrefs,
    private val clockMillis: () -> Long = { nowMillis },
) : ViewModel() {

    private val thresholds: FreshnessThresholds = FreshnessThresholds.Default

    private val clock: Flow<Long> = flow {
        while (true) {
            emit(clockMillis())
            delay(thresholds.refreshInterval)
        }
    }

    private val localLora: Flow<LoraSetting?> = radioConfigRepository.localConfigFlow.map { it.lora?.toLoraSetting() }

    private val radio =
        combine(
            connectionStateProvider.connectionState,
            radioActivityProvider.lastPacketFromRadioMillis,
            localLora,
            ::RadioFacts,
        )

    private val settings = combine(expectedLoraPrefs.expected, uiPrefs.commandPostMode, ::Pair)

    private val nodes = combine(nodeRepository.nodeDBbyNum, nodeRepository.ourNodeInfo, ::Pair)

    val uiState: StateFlow<DiagnosticUiState?> =
        combine(radio, settings, nodes, clock) { radio, (expected, commandPostMode), (nodes, ourNode), now ->
            buildDiagnosticUiState(radio, expected, commandPostMode, nodes.values, ourNode, now, thresholds)
        }
            .stateInWhileSubscribed(initialValue = null)

    /** Saves the expected setting; refused outside command post mode. */
    fun setExpected(setting: LoraSetting?): Boolean {
        if (!uiPrefs.commandPostMode.value) return false
        expectedLoraPrefs.setExpected(setting)
        return true
    }

    companion object {
        /** The regions offered for the expected setting, by proto name; UNSET is not a setting a network can use. */
        val REGION_CHOICES: List<String> =
            Config.LoRaConfig.RegionCode.entries.filter { it != Config.LoRaConfig.RegionCode.UNSET }.map { it.name }

        /** The presets offered for the expected setting, by proto name. */
        val PRESET_CHOICES: List<String> = Config.LoRaConfig.ModemPreset.entries.map { it.name }
    }
}

/** What the phone knows of its radio, apart from the node database. */
data class RadioFacts(val connection: ConnectionState, val lastPacketMillis: Long?, val localLora: LoraSetting?)

/** A radio's LoRa setting by proto names; custom modem parameters (`use_preset` off) have no preset. */
fun Config.LoRaConfig.toLoraSetting(): LoraSetting =
    LoraSetting(region = region.name, preset = modem_preset.name.takeIf { use_preset })

fun buildDiagnosticUiState(
    radio: RadioFacts,
    expected: LoraSetting?,
    commandPostMode: Boolean,
    nodes: Collection<Node>,
    ourNode: Node?,
    nowMillis: Long,
    thresholds: FreshnessThresholds = FreshnessThresholds.Default,
): DiagnosticUiState {
    val heard = nodes.filter { !it.isIgnored }.map { HeardNode(it.num, it.displayName(), it.lastHeard) }
    val diagnostic =
        LinkDiagnostics.build(
            DiagnosticInput(
                connection = radio.connection,
                lastRadioPacketMillis = radio.lastPacketMillis,
                myNodeNum = ourNode?.num,
                nodes = heard,
                localLora = radio.localLora,
                expectedLora = expected,
                batteryLevel = ourNode?.batteryLevel,
                nowMillis = nowMillis,
            ),
            thresholds,
        )
    val targets =
        heard
            .filter { it.num != ourNode?.num && it.lastHeardSeconds != 0 }
            .sortedWith(compareByDescending<HeardNode> { it.lastHeardSeconds.toUInt() }.thenBy { it.num })
            .map { TestTarget(it.num, it.name, it.lastHeardSeconds) }
    return DiagnosticUiState(diagnostic, radio.localLora, expected, commandPostMode, targets)
}

/** The callsign (the long name without its team suffix), or the short name, or the node id when it has neither. */
private fun Node.displayName(): String = TeamSuffix.displayName(user.long_name)
    .ifBlank { user.short_name }
    .ifBlank { user.id }
    .ifBlank { "!" + num.toUInt().toString(HEX) }

private const val HEX = 16
