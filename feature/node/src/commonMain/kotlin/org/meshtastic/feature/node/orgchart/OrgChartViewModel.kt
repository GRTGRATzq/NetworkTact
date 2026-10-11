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
package org.meshtastic.feature.node.orgchart

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import org.koin.core.annotation.KoinViewModel
import org.koin.core.annotation.Named
import org.meshtastic.core.common.util.nowSeconds
import org.meshtastic.core.model.freshness.FreshnessThresholds
import org.meshtastic.core.model.orgchart.Callsign
import org.meshtastic.core.model.orgchart.EditResult
import org.meshtastic.core.model.orgchart.OrgChartEdit
import org.meshtastic.core.model.orgchart.OrgChartEditor
import org.meshtastic.core.model.orgchart.OrgChartRecord
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.repository.OrgChartPrefs
import org.meshtastic.core.repository.SCREEN_DATA
import org.meshtastic.core.repository.TeamRosterPrefs
import org.meshtastic.core.repository.UiPrefs
import org.meshtastic.core.ui.viewmodel.stateInWhileSubscribed

/**
 * The organisation chart over the node database. As in the command post view, a ticker re-evaluates every radio each
 * [FreshnessThresholds.refreshInterval], so ages keep growing while no packet arrives. Everything comes through the
 * screens' sources ([SCREEN_DATA]): in demo mode, the demo's chart and nodes.
 */
@KoinViewModel
class OrgChartViewModel(
    @Named(SCREEN_DATA) private val nodeRepository: NodeRepository,
    @Named(SCREEN_DATA) private val orgChartPrefs: OrgChartPrefs,
    @Named(SCREEN_DATA) teamRosterPrefs: TeamRosterPrefs,
    @Named(SCREEN_DATA) private val uiPrefs: UiPrefs,
) : ViewModel() {

    private val thresholds: FreshnessThresholds = FreshnessThresholds.Default

    private val clock: Flow<Long> = flow {
        while (true) {
            emit(nowSeconds)
            delay(thresholds.refreshInterval)
        }
    }

    private val charts = combine(orgChartPrefs.current, orgChartPrefs.previous, ::Pair)

    private val radios = combine(nodeRepository.nodeDBbyNum, nodeRepository.ourNodeInfo, ::Pair)

    val uiState: StateFlow<OrgChartUiState> =
        combine(charts, teamRosterPrefs.roster, radios, uiPrefs.commandPostMode, clock) {
                (record, previous),
                roster,
                (nodes, ourNode),
                commandPostMode,
                now,
            ->
            buildOrgChartState(
                record = record,
                previous = previous,
                teamList = roster?.teams.orEmpty(),
                nodes = nodes.values,
                ourNode = ourNode,
                nowSeconds = now,
                commandPostMode = commandPostMode,
                thresholds = thresholds,
            )
        }
            .stateInWhileSubscribed(initialValue = OrgChartUiState())

    /**
     * Command post only: applies [edit] and saves the result as this phone's chart, signed with my radio's call sign
     * and dated now. Returns why it was refused, if it was, for the screen to say. Nothing is sent.
     */
    fun edit(edit: OrgChartEdit): EditResult {
        if (!uiPrefs.commandPostMode.value) return EditResult.Unchanged
        val author = nodeRepository.ourNodeInfo.value?.user?.long_name?.let(Callsign::ofLongName).orEmpty()
        val now = nowSeconds
        val result = OrgChartEditor.apply(orgChartPrefs.current.value?.chart, edit, author, now)
        if (result is EditResult.Changed) {
            orgChartPrefs.save(OrgChartRecord(result.chart, OrgChartRecord.Source.EDITED, timeSeconds = now))
        }
        return result
    }

    /** Swaps the chart in use and the previous one; nothing is sent. */
    fun revertToPrevious() = orgChartPrefs.revertToPrevious()
}
