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
package org.meshtastic.feature.settings.team

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import org.meshtastic.core.common.util.nowSeconds
import org.meshtastic.core.common.util.safeCatching
import org.meshtastic.core.domain.usecase.settings.RadioConfigUseCase
import org.meshtastic.core.model.ConnectionState
import org.meshtastic.core.model.NodeAddress
import org.meshtastic.core.model.team.TeamNameChange
import org.meshtastic.core.model.team.TeamRoster
import org.meshtastic.core.model.team.TeamRosterRecord
import org.meshtastic.core.model.team.TeamSuffix
import org.meshtastic.core.model.util.getChannel
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.repository.RadioConfigRepository
import org.meshtastic.core.repository.ServiceRepository
import org.meshtastic.core.repository.TeamRosterPrefs
import org.meshtastic.core.repository.UiPrefs
import org.meshtastic.core.repository.usecase.SendMessageUseCase
import org.meshtastic.core.ui.viewmodel.stateInWhileSubscribed
import org.meshtastic.proto.ChannelSet

data class TeamsUiState(
    val roster: TeamRosterRecord? = null,
    val pending: TeamRosterRecord? = null,
    /** My radio's long name as it reports it, or null while unknown. */
    val myLongName: String? = null,
    /** The team my radio's long name declares, or null. */
    val myTeam: String? = null,
    /** Why the team cannot be written to my radio, or null when it can. */
    val block: OwnerWriteBlock? = OwnerWriteBlock.NOT_CONNECTED,
    /** Command post (PC) mode: the list can be broadcast from this phone. */
    val commandPostMode: Boolean = false,
    /** Display names of my radio's channels, by index, for the broadcast. */
    val channels: List<String> = emptyList(),
    val connected: Boolean = false,
) {
    /** My radio declares a team that the adopted list no longer holds. It is kept; the screen says so. */
    val myTeamMissingFromList: Boolean
        get() = myTeam != null && roster != null && !roster.roster.contains(myTeam)
}

enum class TeamSendResult {
    SENT,
    FAILED,
}

sealed interface BroadcastResult {
    data class Sent(val channelName: String) : BroadcastResult

    data object Failed : BroadcastResult
}

/**
 * The team screen: the adopted list and the one awaiting confirmation, my radio's team, and a manual list as fallback.
 *
 * Choosing a team sends `set_owner` to my own radio only, with a copy of its current owner in which only the long name
 * changes ([ownerWithTeam]). Nothing is sent, and nothing is saved, while the radio is not connected.
 */
@KoinViewModel
class TeamsViewModel(
    private val teamRosterPrefs: TeamRosterPrefs,
    private val nodeRepository: NodeRepository,
    private val serviceRepository: ServiceRepository,
    private val radioConfigUseCase: RadioConfigUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    radioConfigRepository: RadioConfigRepository,
    uiPrefs: UiPrefs,
) : ViewModel() {

    private val radioState =
        combine(nodeRepository.ourNodeInfo, serviceRepository.connectionState, radioConfigRepository.channelSetFlow) {
                ourNode,
                connection,
                channelSet,
            ->
            Triple(ourNode, connection, channelSet.channelNames())
        }

    val uiState: StateFlow<TeamsUiState> =
        combine(teamRosterPrefs.roster, teamRosterPrefs.pendingRoster, radioState, uiPrefs.commandPostMode) {
                roster,
                pending,
                (ourNode, connection, channels),
                commandPostMode,
            ->
            val longName = ourNode?.user?.long_name?.takeIf { it.isNotBlank() }
            TeamsUiState(
                roster = roster,
                pending = pending,
                myLongName = longName,
                myTeam = longName?.let(TeamSuffix::teamOf),
                block = ownerWriteBlock(connection, ourNode),
                commandPostMode = commandPostMode,
                channels = channels,
                connected = connection == ConnectionState.Connected,
            )
        }
            .stateInWhileSubscribed(initialValue = TeamsUiState())

    private val _broadcastResult = MutableStateFlow<BroadcastResult?>(null)
    val broadcastResult: StateFlow<BroadcastResult?> = _broadcastResult.asStateFlow()

    private val _sendResult = MutableStateFlow<TeamSendResult?>(null)
    val sendResult: StateFlow<TeamSendResult?> = _sendResult.asStateFlow()

    /** The name change choosing [team] would make, or null when my radio cannot take it right now. */
    fun previewTeam(team: String?): TeamNameChange? = writableOwner()?.let { ownerWithTeam(it.second, team).second }

    /** Sends [team] to my radio. Re-checks the connection first: nothing is sent while it is down. */
    fun applyTeam(team: String?) {
        val (myNum, owner) = writableOwner() ?: return
        val user = ownerWithTeam(owner, team).first
        _sendResult.value = null
        viewModelScope.launch {
            _sendResult.value =
                safeCatching { radioConfigUseCase.setOwner(myNum, user) }
                    .fold(onSuccess = { TeamSendResult.SENT }, onFailure = { TeamSendResult.FAILED })
        }
    }

    fun clearSendResult() {
        _sendResult.value = null
    }

    fun acceptPending(record: TeamRosterRecord) = teamRosterPrefs.acceptPending(record)

    fun dismissPending(record: TeamRosterRecord) = teamRosterPrefs.dismissPending(record)

    /** Adopts a list typed on this phone, already validated by [TeamRoster.parseInput]. */
    fun adoptManual(roster: TeamRoster) {
        teamRosterPrefs.adopt(
            TeamRosterRecord(teams = roster.teams, source = TeamRosterRecord.Source.MANUAL, timeSeconds = nowSeconds),
        )
    }

    /**
     * Command post: sends [roster] as a `[EQUIPES]` text message on channel [channelIndex] through the normal messaging
     * path, where its sending status shows, then adopts it as this phone's list. Refused while my radio is not
     * connected. [roster] comes from [TeamRoster.parseInput], so the message already fits in one text message.
     */
    fun broadcast(roster: TeamRoster, channelIndex: Int) {
        if (serviceRepository.connectionState.value != ConnectionState.Connected) return
        val channelName = uiState.value.channels.getOrNull(channelIndex) ?: channelIndex.toString()
        _broadcastResult.value = null
        viewModelScope.launch {
            val sent = safeCatching {
                sendMessageUseCase(roster.toMessage(), "$channelIndex${NodeAddress.ID_BROADCAST}")
            }
            if (sent.isSuccess) {
                teamRosterPrefs.adopt(
                    TeamRosterRecord(
                        teams = roster.teams,
                        source = TeamRosterRecord.Source.BROADCAST,
                        timeSeconds = nowSeconds,
                    ),
                )
            }
            _broadcastResult.value = if (sent.isSuccess) BroadcastResult.Sent(channelName) else BroadcastResult.Failed
        }
    }

    fun clearBroadcastResult() {
        _broadcastResult.value = null
    }

    private fun writableOwner() = nodeRepository.ourNodeInfo.value
        ?.takeIf { ownerWriteBlock(serviceRepository.connectionState.value, it) == null }
        ?.let { it.num to it.user }
}

/** Channel names by index, the index itself standing in for an unnamed channel, as in the messaging tabs. */
private fun ChannelSet.channelNames(): List<String> =
    List(settings.size) { index -> getChannel(index)?.name?.ifBlank { null } ?: index.toString() }
