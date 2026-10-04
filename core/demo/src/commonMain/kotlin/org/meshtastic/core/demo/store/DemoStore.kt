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
package org.meshtastic.core.demo.store

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.meshtastic.core.demo.data.DemoData
import org.meshtastic.core.demo.data.DemoPacket
import org.meshtastic.core.model.ContactSettings
import org.meshtastic.core.model.MyNodeInfo
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.team.TeamRosterRecord
import org.meshtastic.proto.ChannelSet

/**
 * The demo's whole state, in memory only. Loaded from a fresh data set at each activation and emptied when demo mode is
 * switched off, so nothing of a previous demo survives.
 */
class DemoStore {
    val myNodeInfo = MutableStateFlow<MyNodeInfo?>(null)
    val nodes = MutableStateFlow<Map<Int, Node>>(emptyMap())

    /** PC-0, kept in step with [nodes] synchronously so a read right after a change is never stale. */
    val ourNodeInfo = MutableStateFlow<Node?>(null)
    val myId = MutableStateFlow<String?>(null)
    val channelSet = MutableStateFlow(ChannelSet.Builder().build())
    val roster = MutableStateFlow<TeamRosterRecord?>(null)
    val packets = MutableStateFlow<List<DemoPacket>>(emptyList())
    val contactSettings = MutableStateFlow<Map<String, ContactSettings>>(emptyMap())

    /** The Terrain/PC choice while the demo runs, seeded from the real one and never written back. */
    val commandPostMode = MutableStateFlow(false)

    fun load(data: DemoData, commandPostMode: Boolean) {
        myNodeInfo.value = data.myNodeInfo
        nodes.value = data.nodes.associateBy { it.num }
        refreshOurNode()
        channelSet.value = data.channelSet
        roster.value = data.roster
        packets.value = data.packets
        contactSettings.value = emptyMap()
        this.commandPostMode.value = commandPostMode
    }

    fun clear() {
        myNodeInfo.value = null
        nodes.value = emptyMap()
        refreshOurNode()
        channelSet.value = ChannelSet.Builder().build()
        roster.value = null
        packets.value = emptyList()
        contactSettings.value = emptyMap()
        commandPostMode.value = false
    }

    /** This phone's own demo node, PC-0. */
    val ourNode: Node?
        get() = ourNodeInfo.value

    fun updateNode(num: Int, transform: (Node) -> Node) {
        nodes.update { all -> all[num]?.let { all + (num to transform(it)) } ?: all }
        refreshOurNode()
    }

    private fun refreshOurNode() {
        val ours = myNodeInfo.value?.let { nodes.value[it.myNodeNum] }
        ourNodeInfo.value = ours
        myId.value = ours?.user?.id
    }

    fun nextUuid(): Long = (packets.value.maxOfOrNull { it.uuid } ?: 0L) + 1L

    fun contactSettings(contactKey: String): ContactSettings =
        contactSettings.value[contactKey] ?: ContactSettings(contactKey = contactKey)

    fun updateContactSettings(contactKey: String, transform: (ContactSettings) -> ContactSettings) {
        contactSettings.update { all ->
            all + (contactKey to transform(all[contactKey] ?: ContactSettings(contactKey)))
        }
    }
}
