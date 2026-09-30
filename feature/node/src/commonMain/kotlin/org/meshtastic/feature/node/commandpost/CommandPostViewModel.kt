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

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import org.koin.core.annotation.KoinViewModel
import org.meshtastic.core.common.util.nowSeconds
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.NodeAddress
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.ui.viewmodel.stateInWhileSubscribed
import org.meshtastic.feature.node.model.canDirectMessage

/** One node as the command post sees it. Everything here is derived from the node database; nothing is estimated. */
data class CommandPostRow(
    val num: Int,
    val shortName: String,
    val longName: String,
    val lastHeard: Int,
    val contact: ContactState,
    val position: PositionState,
    val latitude: Double?,
    val longitude: Double?,
    /** Contact key of the existing direct conversation, or null when a direct message cannot be delivered. */
    val directMessageKey: String?,
)

/**
 * Read-only command post view over [NodeRepository.nodeDBbyNum].
 *
 * A ticker re-evaluates every row each [FreshnessThresholds.refreshInterval] so ages keep growing while no packet
 * arrives: a position must never look current just because nothing new was heard.
 */
@KoinViewModel
class CommandPostViewModel(nodeRepository: NodeRepository) : ViewModel() {

    val thresholds: FreshnessThresholds = FreshnessThresholds.Default

    private val clock: Flow<Long> = flow {
        while (true) {
            emit(nowSeconds)
            delay(thresholds.refreshInterval)
        }
    }

    val rows: StateFlow<List<CommandPostRow>> =
        combine(nodeRepository.nodeDBbyNum, nodeRepository.ourNodeInfo, clock) { nodes, ourNode, now ->
            buildCommandPostRows(nodes.values, ourNode, now, thresholds)
        }
            .stateInWhileSubscribed(initialValue = emptyList())
}

/**
 * Builds the rows: the local radio and ignored nodes are left out, the most recently heard node comes first and nodes
 * never heard come last.
 */
fun buildCommandPostRows(
    nodes: Collection<Node>,
    ourNode: Node?,
    nowSeconds: Long,
    thresholds: FreshnessThresholds = FreshnessThresholds.Default,
): List<CommandPostRow> = nodes
    .filter { it.num != ourNode?.num && !it.isIgnored }
    .sortedWith(compareByDescending<Node> { it.lastHeard.toUInt() }.thenBy { it.num })
    .map { node ->
        val hasPosition = node.validPosition != null
        CommandPostRow(
            num = node.num,
            shortName = node.user.short_name,
            longName = node.user.long_name,
            lastHeard = node.lastHeard,
            contact = NodeFreshness.contact(node, nowSeconds, thresholds),
            position = NodeFreshness.position(node, nowSeconds, thresholds),
            latitude = node.latitude.takeIf { hasPosition },
            longitude = node.longitude.takeIf { hasPosition },
            directMessageKey = node.takeIf { it.canDirectMessage }?.let { directMessageKey(it, ourNode) },
        )
    }

/** Same contact key as NodeListViewModel.getDirectMessageRoute, so the button opens the existing conversation. */
internal fun directMessageKey(node: Node, ourNode: Node?): String {
    val hasPKC = ourNode?.hasPKC == true && node.hasPKC
    val channel = if (hasPKC) NodeAddress.PKC_CHANNEL_INDEX else node.channel
    return "${channel}${node.user.id}"
}
