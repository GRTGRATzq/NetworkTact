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
package org.meshtastic.feature.messaging.firerequest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.KoinViewModel
import org.koin.core.annotation.Named
import org.meshtastic.core.common.util.nowSeconds
import org.meshtastic.core.common.util.systemTimeZone
import org.meshtastic.core.model.ConnectionState
import org.meshtastic.core.model.ContactKey
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.NodeAddress
import org.meshtastic.core.model.firerequest.FireRequest
import org.meshtastic.core.model.firerequest.Gdh
import org.meshtastic.core.model.firerequest.GdhZone
import org.meshtastic.core.model.team.TeamSuffix
import org.meshtastic.core.repository.ConnectionStateProvider
import org.meshtastic.core.repository.FireRequestPrefs
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.repository.PacketRepository
import org.meshtastic.core.repository.RadioConfigRepository
import org.meshtastic.core.repository.SCREEN_DATA
import org.meshtastic.core.repository.usecase.SendMessageUseCase

/** A node the request can be sent to: its [id], and its [name] without the team suffix. */
data class RecipientOption(val num: Int, val id: String, val name: String)

/**
 * The fire request form: the request being written, kept as the draft at every change; the recipient; the parts it
 * would be sent as; and the sending itself, one direct message per part, with acknowledgement, through the screens'
 * send use case (simulated in demo mode: nothing leaves the phone).
 */
@KoinViewModel
class FireRequestViewModel(
    @Named(SCREEN_DATA) private val nodeRepository: NodeRepository,
    @Named(SCREEN_DATA) private val prefs: FireRequestPrefs,
    @Named(SCREEN_DATA) private val sendMessageUseCase: SendMessageUseCase,
    @Named(SCREEN_DATA) connectionStateProvider: ConnectionStateProvider,
    @Named(SCREEN_DATA) private val packetRepository: PacketRepository,
    @Named(SCREEN_DATA) radioConfigRepository: RadioConfigRepository,
) : ViewModel() {

    /** Spaces the parts out and sends again those the radio refuses for sending too fast. */
    private val sender =
        FireRequestSender(
            send = { text -> sendMessageUseCase(text, sendingTo) },
            outcome = packetRepository::getOutgoingOutcome,
            discard = { uuid -> packetRepository.deleteMessages(listOf(uuid)) },
        )

    private var sendingTo = ""

    /** The part being sent (« partie 2/3 en attente »); null when nothing is being sent. */
    val progress: StateFlow<FireRequestProgress?> = sender.progress

    private val _request = MutableStateFlow(prefs.draft.value ?: FireRequest(requestGdh = gdhNow(prefs.gdhZone.value)))
    val request: StateFlow<FireRequest> = _request.asStateFlow()

    private val _recipient = MutableStateFlow<RecipientOption?>(null)
    val recipient: StateFlow<RecipientOption?> = _recipient.asStateFlow()

    val gdhZone: StateFlow<GdhZone> = prefs.gdhZone

    /** Whether a request was sent before, for « Renouveler ». */
    val canRenew: StateFlow<Boolean> =
        prefs.lastSent.map { it != null }.stateIn(viewModelScope, SharingStarted.Eagerly, prefs.lastSent.value != null)

    val recipients: StateFlow<List<RecipientOption>> =
        combine(nodeRepository.nodeDBbyNum, nodeRepository.ourNodeInfo) { nodes, ours ->
            nodes.values
                .filter { it.num != ours?.num && it.user.id.isNotBlank() }
                .map { RecipientOption(it.num, it.user.id, it.displayName()) }
                .sortedBy { it.name.lowercase() }
        }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** The channels « Envoyer à » offers; null until the radio has given its channels. */
    val channels: StateFlow<SendChannels?> =
        radioConfigRepository.channelSetFlow
            .map { set -> SendChannels.of(set.settings.map { it.name }) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val chosenTarget = MutableStateFlow<SendTarget?>(null)

    /** Where the request goes: the target chosen, or by default the fire team's channel (else the primary one). */
    val target: StateFlow<SendTarget?> =
        combine(chosenTarget, channels) { chosen, channels -> chosen ?: channels?.default }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val preview: StateFlow<FireRequestPreview> =
        combine(_request, target) { request, target -> FireRequestPreview.of(request.addressedTo(target)) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, FireRequestPreview.of(_request.value))

    private val connected: StateFlow<Boolean> =
        connectionStateProvider.connectionState
            .map { it is ConnectionState.Connected }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val sendBlock: StateFlow<SendBlock?> =
        combine(_request, preview, target, connected) { request, preview, target, connected ->
            sendBlock(request, preview, target != null, connected)
        }
            .stateIn(viewModelScope, SharingStarted.Eagerly, SendBlock.NO_NUMBER)

    /**
     * The form opened from conversation [contactKey]: a direct one gives the recipient, and its node's name fills the «
     * Destinataire » box when that is still empty.
     */
    fun start(contactKey: String) {
        val address = contactKey.takeIf { it.isNotEmpty() }?.let { ContactKey(it).addressString }
        val node =
            nodeRepository.nodeDBbyNum.value.values.firstOrNull {
                address != null && address != NodeAddress.ID_BROADCAST && it.user.id == address
            }
        if (_recipient.value == null && node != null) {
            chooseRecipient(RecipientOption(node.num, node.user.id, node.displayName()))
        }
    }

    fun chooseRecipient(option: RecipientOption) {
        _recipient.value = option
        chosenTarget.value = SendTarget.ToRadio(option)
        if (_request.value.recipient.isBlank()) edit { it.copy(recipient = option.name) }
    }

    /**
     * « Envoyer à »: a channel, both, or one radio. « Destinataire » becomes the channel's name, or the radio's (kept
     * editable in the form, as in v1).
     */
    fun chooseTarget(target: SendTarget) {
        chosenTarget.value = target
        when (target) {
            is SendTarget.ToRadio -> {
                _recipient.value = target.radio
                edit { it.copy(recipient = target.radio.name) }
            }

            is SendTarget.OnChannels -> edit { it.copy(recipient = target.channels.first().name) }
        }
    }

    /** Changes the request and keeps it as the draft. */
    fun edit(transform: (FireRequest) -> FireRequest) {
        _request.update(transform)
        prefs.saveDraft(_request.value)
    }

    /** Chooses the zone of date-time groups, kept for the next requests, and sets the request's GDH to now in it. */
    fun chooseZone(zone: GdhZone) {
        prefs.setGdhZone(zone)
        edit { it.copy(requestGdh = gdhNow(zone)) }
    }

    /** « Renouveler »: the last request sent, back in the form with its date-time group at now. */
    fun renew() {
        val last = prefs.lastSent.value ?: return
        edit { last.copy(requestGdh = gdhNow(prefs.gdhZone.value)) }
    }

    /** Whether the form holds something « Renouveler » would replace. */
    fun hasDraft(): Boolean = _request.value.copy(requestGdh = "") != FireRequest()

    /**
     * Sends the parts one after the other: in direct messages to the radio chosen, with acknowledgement, or on the
     * channel (each of the two for « Les deux », one after the other), then keeps the request as the last one sent.
     * Returns the conversation to show (the first channel's), or null when sending is refused ([sendBlock]).
     */
    suspend fun send(): String? {
        val request = _request.value
        val batches =
            target.value
                ?.takeIf { sendBlock.value == null }
                ?.let { sendBatches(it, request, nodeRepository.nodeDBbyNum.value, nodeRepository.ourNodeInfo.value) }
                ?: return null
        // In the view model's scope: leaving the screen must not drop the parts still to send.
        viewModelScope
            .async {
                batches.forEach { (contactKey, copy) ->
                    sendingTo = contactKey
                    sender.sendAll(FireRequestPreview.of(copy).parts)
                }
            }
            .await()
        prefs.recordSent(request)
        return batches.first().first
    }

    private fun gdhNow(zone: GdhZone): String = Gdh.of(nowSeconds, zone, systemTimeZone).format()

    private fun Node.displayName(): String =
        TeamSuffix.displayName(user.long_name).ifBlank { user.short_name.ifBlank { user.id } }
}
