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
package org.meshtastic.core.service

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.core.annotation.Single
import org.meshtastic.core.common.util.nowSeconds
import org.meshtastic.core.common.util.safeCatching
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.team.TeamRoster
import org.meshtastic.core.model.team.TeamRosterRecord
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.repository.ServiceRepository
import org.meshtastic.core.repository.TeamRosterPrefs
import org.meshtastic.proto.MeshPacket
import org.meshtastic.proto.PortNum

/**
 * Watches received text messages for a `[EQUIPES]` team list and hands it to [TeamRosterPrefs], where it waits for the
 * user's confirmation.
 *
 * Strictly passive: it only reads [ServiceRepository.meshPacketFlow], a copy of the inbound traffic, and never sends
 * anything. The message itself is stored and shown by the normal messaging path, untouched. A packet that cannot be
 * read is logged (without its content) and skipped, so it can neither stop this collector nor reach the packet
 * processing that feeds the flow.
 */
@Single
class TeamRosterListener(
    private val serviceRepository: ServiceRepository,
    private val nodeRepository: NodeRepository,
    private val teamRosterPrefs: TeamRosterPrefs,
) {
    fun start(scope: CoroutineScope): Job = serviceRepository.meshPacketFlow
        .onEach { packet ->
            safeCatching { handle(packet) }
                .onFailure { Logger.w(it) { "Skipped an unreadable team list candidate" } }
        }
        .launchIn(scope)

    internal fun handle(packet: MeshPacket) {
        val record =
            teamRosterRecordOf(
                packet = packet,
                myNodeNum = nodeRepository.myNodeInfo.value?.myNodeNum,
                sender = nodeRepository.nodeDBbyNum.value[packet.from],
                nowSeconds = nowSeconds,
            ) ?: return
        teamRosterPrefs.offerReceived(record)
    }
}

/**
 * The team list [packet] carries, or null when it is not a plain text message holding a valid `[EQUIPES]` list. Lists
 * from this phone's own radio and from ignored nodes are left out.
 */
internal fun teamRosterRecordOf(
    packet: MeshPacket,
    myNodeNum: Int?,
    sender: Node?,
    nowSeconds: Long,
): TeamRosterRecord? {
    val decoded =
        packet.decoded?.takeIf {
            it.portnum == PortNum.TEXT_MESSAGE_APP &&
                it.emoji == 0 &&
                packet.from != myNodeNum &&
                sender?.isIgnored != true
        }
    val roster = decoded?.let { TeamRoster.parseMessage(it.payload.utf8()) }
    return roster?.let {
        TeamRosterRecord(
            teams = it.teams,
            source = TeamRosterRecord.Source.RADIO,
            senderNum = packet.from,
            senderName = sender?.user?.long_name?.takeIf(String::isNotBlank),
            timeSeconds = nowSeconds,
        )
    }
}
