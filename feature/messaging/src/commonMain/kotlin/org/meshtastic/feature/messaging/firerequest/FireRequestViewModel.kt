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
) : ViewModel() {

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

    val preview: StateFlow<FireRequestPreview> =
        _request
            .map { FireRequestPreview.of(it) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, FireRequestPreview.of(_request.value))

    private val connected: StateFlow<Boolean> =
        connectionStateProvider.connectionState
            .map { it is ConnectionState.Connected }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val sendBlock: StateFlow<SendBlock?> =
        combine(_request, preview, _recipient, connected) { request, preview, recipient, connected ->
            sendBlock(request, preview, recipient != null, connected)
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
        if (_request.value.recipient.isBlank()) edit { it.copy(recipient = option.name) }
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
     * Sends the parts one after the other in direct messages to the recipient, then keeps the request as the last one
     * sent. Returns the direct conversation, or null when sending is refused ([sendBlock]).
     */
    suspend fun send(): String? {
        val node =
            _recipient.value?.takeIf { sendBlock.value == null }?.let { nodeRepository.nodeDBbyNum.value[it.num] }
                ?: return null
        val contactKey = directContactKey(node, nodeRepository.ourNodeInfo.value)
        val request = _request.value
        // Computed again from the request sent, not from the preview flow, which may lag one change behind.
        FireRequestPreview.of(request).parts.forEach { sendMessageUseCase(it, contactKey) }
        prefs.recordSent(request)
        return contactKey
    }

    private fun gdhNow(zone: GdhZone): String = Gdh.of(nowSeconds, zone, systemTimeZone).format()

    private fun Node.displayName(): String =
        TeamSuffix.displayName(user.long_name).ifBlank { user.short_name.ifBlank { user.id } }
}
