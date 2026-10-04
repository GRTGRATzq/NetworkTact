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
package org.meshtastic.core.demo.screen

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.meshtastic.core.model.MyNodeInfo
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.NodeSortOption
import org.meshtastic.core.repository.DemoMode
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.proto.DeviceMetadata
import org.meshtastic.proto.LocalStats
import org.meshtastic.proto.User

/** The screens' [NodeRepository]: [real] while demo mode is off, [demo] while it is on, reads and writes alike. */
@Suppress("TooManyFunctions") // Tous les membres sont imposés par l'interface.
class ScreenNodeRepository(private val real: NodeRepository, private val demo: NodeRepository, demoMode: DemoMode) :
    NodeRepository {
    private val switch = ScreenSwitch(demoMode)

    private fun current(): NodeRepository = switch.pick({ real }, { demo })

    override val myNodeInfo: StateFlow<MyNodeInfo?> = switch.state(real.myNodeInfo, demo.myNodeInfo)
    override val ourNodeInfo: StateFlow<Node?> = switch.state(real.ourNodeInfo, demo.ourNodeInfo)
    override val myId: StateFlow<String?> = switch.state(real.myId, demo.myId)
    override val localStats: StateFlow<LocalStats> = switch.state(real.localStats, demo.localStats)
    override val nodeDBbyNum: StateFlow<Map<Int, Node>> = switch.state(real.nodeDBbyNum, demo.nodeDBbyNum)
    override val onlineNodeCount: Flow<Int> = switch.flow({ real.onlineNodeCount }, { demo.onlineNodeCount })
    override val totalNodeCount: Flow<Int> = switch.flow({ real.totalNodeCount }, { demo.totalNodeCount })

    override fun updateLocalStats(stats: LocalStats) = current().updateLocalStats(stats)

    override fun effectiveLogNodeId(nodeNum: Int): Flow<Int> =
        switch.flow({ real.effectiveLogNodeId(nodeNum) }, { demo.effectiveLogNodeId(nodeNum) })

    override fun getNode(userId: String): Node = current().getNode(userId)

    override fun getUser(nodeNum: Int): User = current().getUser(nodeNum)

    override fun getUser(userId: String): User = current().getUser(userId)

    override fun getNodes(
        sort: NodeSortOption,
        filter: String,
        includeUnknown: Boolean,
        onlyOnline: Boolean,
        onlyDirect: Boolean,
    ): Flow<List<Node>> = switch.flow(
        { real.getNodes(sort, filter, includeUnknown, onlyOnline, onlyDirect) },
        { demo.getNodes(sort, filter, includeUnknown, onlyOnline, onlyDirect) },
    )

    override suspend fun getNodesOlderThan(lastHeard: Int): List<Node> = current().getNodesOlderThan(lastHeard)

    override suspend fun getUnknownNodes(): List<Node> = current().getUnknownNodes()

    override suspend fun getNodeDbSnapshot(): Map<Int, Node> = current().getNodeDbSnapshot()

    override suspend fun clearNodeDB(preserveFavorites: Boolean) = current().clearNodeDB(preserveFavorites)

    override suspend fun clearMyNodeInfo() = current().clearMyNodeInfo()

    override suspend fun deleteNode(num: Int) = current().deleteNode(num)

    override suspend fun deleteNodes(nodeNums: List<Int>) = current().deleteNodes(nodeNums)

    override suspend fun setNodeNotes(num: Int, notes: String) = current().setNodeNotes(num, notes)

    override suspend fun markAllHeardOnCurrentLora() = current().markAllHeardOnCurrentLora()

    override suspend fun updatePowerChannelLabel(num: Int, channelIndex: Int, label: String) =
        current().updatePowerChannelLabel(num, channelIndex, label)

    override suspend fun upsert(node: Node) = current().upsert(node)

    override suspend fun installConfig(mi: MyNodeInfo, nodes: List<Node>): List<Int> =
        current().installConfig(mi, nodes)

    override suspend fun insertMetadata(nodeNum: Int, metadata: DeviceMetadata) =
        current().insertMetadata(nodeNum, metadata)
}
