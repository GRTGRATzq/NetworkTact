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
package org.meshtastic.feature.map.recenter

import androidx.lifecycle.ViewModel
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.KoinViewModel
import org.koin.core.annotation.Named
import org.meshtastic.core.common.util.nowSeconds
import org.meshtastic.core.common.util.safeCatching
import org.meshtastic.core.model.ConnectionState
import org.meshtastic.core.model.geo.BroadcastCheck
import org.meshtastic.core.model.geo.RadioPositionBroadcast
import org.meshtastic.core.model.geo.RadioPositionPacket
import org.meshtastic.core.repository.ConnectionStateProvider
import org.meshtastic.core.repository.DemoMode
import org.meshtastic.core.repository.MapPrefs
import org.meshtastic.core.repository.MessagingController
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.repository.PhoneFix
import org.meshtastic.core.repository.PhonePositionSource
import org.meshtastic.core.repository.RecenterTarget
import org.meshtastic.core.repository.SCREEN_DATA

/**
 * The map's "Recaler" card: this user's radio and phone positions side by side, and one manual broadcast of the radio's
 * position. Everything comes through the screens' sources ([SCREEN_DATA]): in demo mode the positions are the demo's
 * and the send goes to the demo's controller, which drops it; [RadioPositionBroadcast] refuses it there anyway.
 *
 * Nothing here runs on its own: the phone's position is read when the card asks, and the radio's position is broadcast
 * only by [broadcast], after the user's confirmation, never automatically nor periodically.
 */
@KoinViewModel
class RecenterViewModel(
    @Named(SCREEN_DATA) private val nodeRepository: NodeRepository,
    @Named(SCREEN_DATA) private val phonePositionSource: PhonePositionSource,
    @Named(SCREEN_DATA) private val messagingController: MessagingController,
    @Named(SCREEN_DATA) private val connectionStateProvider: ConnectionStateProvider,
    private val mapPrefs: MapPrefs,
    private val demoMode: DemoMode,
) : ViewModel() {
    /** The clock, in epoch seconds; replaced in tests. */
    internal var clock: () -> Long = { nowSeconds }

    private var phoneFix: PhoneFix? = null
    private var lastSent: SentBroadcast? = null
    private var sendFailed = false
    private var target: RecenterTarget? = null

    private val _state = MutableStateFlow<RecenterState?>(null)

    /** Null until the card has been opened once. */
    val state: StateFlow<RecenterState?> = _state.asStateFlow()

    /** Reads the phone's last fix and recomputes; on opening the card, then every second while it is open. */
    suspend fun refresh(): RecenterState {
        phoneFix = phonePositionSource.lastFix()
        return recompute()
    }

    fun setTarget(newTarget: RecenterTarget) {
        target = newTarget
        mapPrefs.setRecenterTarget(newTarget)
        recompute()
    }

    /**
     * Broadcasts the radio's position once, if [RadioPositionBroadcast] allows it now; the card has already asked for
     * the user's confirmation. Returns what was sent, or null when nothing was.
     */
    suspend fun broadcast(): SentBroadcast? {
        val now = clock()
        val node = nodeRepository.ourNodeInfo.value
        val check = check(now)
        // The check refuses a demo; this holds if it ever stopped doing so.
        if (check !is BroadcastCheck.Ready || node == null || demoMode.isActive.value) {
            recompute()
            return null
        }
        val sent = safeCatching { messagingController.sendMessage(RadioPositionPacket.of(node.position)) }
        sendFailed = sent.isFailure
        sent.exceptionOrNull()?.let { Logger.w(it) { "Position broadcast was not handed to the radio" } }
        if (sent.isSuccess) lastSent = SentBroadcast(sentEpochSeconds = now, fixEpochSeconds = check.fixEpochSeconds)
        recompute()
        return lastSent.takeIf { sent.isSuccess }
    }

    private fun check(now: Long): BroadcastCheck = RadioPositionBroadcast.check(
        ownNode = nodeRepository.ourNodeInfo.value,
        connected = connectionStateProvider.connectionState.value is ConnectionState.Connected,
        demo = demoMode.isActive.value,
        nowEpochSeconds = now,
        lastBroadcastEpochSeconds = lastSent?.sentEpochSeconds,
    )

    private fun recompute(): RecenterState {
        val now = clock()
        val node = nodeRepository.ourNodeInfo.value
        val state =
            RecenterState(
                radio = RecenterState.radioSide(node, now),
                radioQuality = RecenterState.radioQuality(node),
                phone = RecenterState.phoneSide(phoneFix, now),
                phoneAccuracyMeters = phoneFix?.accuracyMeters,
                target = target ?: mapPrefs.recenterTarget.value,
                broadcast = check(now),
                lastSent = lastSent,
                sendFailed = sendFailed,
            )
        _state.value = state
        return state
    }
}
