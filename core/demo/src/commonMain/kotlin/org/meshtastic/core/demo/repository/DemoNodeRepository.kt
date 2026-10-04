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
package org.meshtastic.core.demo.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import org.meshtastic.core.demo.store.DemoStore
import org.meshtastic.core.model.MyNodeInfo
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.NodeAddress
import org.meshtastic.core.model.NodeSortOption
import org.meshtastic.proto.DeviceMetadata
import org.meshtastic.proto.LocalStats
import org.meshtastic.proto.User

/** [NodeRepository][org.meshtastic.core.repository.NodeRepository] over the demo's nodes. Writes stay in memory. */
@Suppress("TooManyFunctions") // Tous les membres sont imposés par l'interface.
class DemoNodeRepository(private val store: DemoStore) : org.meshtastic.core.repository.NodeRepository {
    override val myNodeInfo: StateFlow<MyNodeInfo?> = store.myNodeInfo
    override val ourNodeInfo: StateFlow<Node?> = store.ourNodeInfo
    override val myId: StateFlow<String?> = store.myId

    private val stats = MutableStateFlow(LocalStats.Builder().build())
    override val localStats: StateFlow<LocalStats> = stats

    override val nodeDBbyNum: StateFlow<Map<Int, Node>> = store.nodes
    override val onlineNodeCount: Flow<Int> = store.nodes.map { nodes -> nodes.values.count { it.isOnline } }
    override val totalNodeCount: Flow<Int> = store.nodes.map { it.size }

    override fun updateLocalStats(stats: LocalStats) {
        this.stats.value = stats
    }

    override fun effectiveLogNodeId(nodeNum: Int): Flow<Int> = flowOf(nodeNum)

    override fun getNode(userId: String): Node = store.nodes.value.values.find { it.user.id == userId }
        ?: Node(num = NodeAddress.idToNum(userId) ?: 0, user = getUser(userId))

    override fun getUser(nodeNum: Int): User = getUser(NodeAddress.numToDefaultId(nodeNum))

    override fun getUser(userId: String): User = store.nodes.value.values.find { it.user.id == userId }?.user
        ?: User.Builder()
            .also {
                it.id = userId
                it.long_name = userId
                it.short_name = userId.takeLast(UNKNOWN_SHORT_NAME_LENGTH)
            }
            .build()

    override fun getNodes(
        sort: NodeSortOption,
        filter: String,
        includeUnknown: Boolean,
        onlyOnline: Boolean,
        onlyDirect: Boolean,
    ): Flow<List<Node>> = store.nodes.map { nodes ->
        nodes.values
            .filter { node ->
                (
                    filter.isBlank() ||
                        node.user.long_name.contains(filter, ignoreCase = true) ||
                        node.user.short_name.contains(filter, ignoreCase = true)
                    ) &&
                    (!onlyOnline || node.isOnline) &&
                    (!onlyDirect || node.hopsAway == 0)
            }
            .sortedByDescending { it.lastHeard }
    }

    override suspend fun getNodesOlderThan(lastHeard: Int): List<Node> =
        store.nodes.value.values.filter { it.lastHeard < lastHeard }

    override suspend fun getUnknownNodes(): List<Node> = emptyList()

    override suspend fun getNodeDbSnapshot(): Map<Int, Node> = store.nodes.value

    override suspend fun clearNodeDB(preserveFavorites: Boolean) {
        val ours = store.myNodeInfo.value?.myNodeNum
        store.nodes.update { nodes ->
            nodes.filter { (num, node) -> num == ours || (preserveFavorites && node.isFavorite) }
        }
    }

    override suspend fun clearMyNodeInfo() = Unit

    override suspend fun deleteNode(num: Int) {
        store.nodes.update { it - num }
    }

    override suspend fun deleteNodes(nodeNums: List<Int>) {
        store.nodes.update { it - nodeNums.toSet() }
    }

    override suspend fun setNodeNotes(num: Int, notes: String) {
        store.updateNode(num) { it.copy(notes = notes) }
    }

    override suspend fun markAllHeardOnCurrentLora() = Unit

    override suspend fun updatePowerChannelLabel(num: Int, channelIndex: Int, label: String) = Unit

    override suspend fun upsert(node: Node) {
        store.nodes.update { it + (node.num to node) }
    }

    override suspend fun installConfig(mi: MyNodeInfo, nodes: List<Node>): List<Int> = emptyList()

    override suspend fun insertMetadata(nodeNum: Int, metadata: DeviceMetadata) {
        store.updateNode(nodeNum) { it.copy(metadata = metadata) }
    }

    private companion object {
        const val UNKNOWN_SHORT_NAME_LENGTH = 4
    }
}
