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
package org.meshtastic.feature.settings.identity

import co.touchlab.kermit.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import org.meshtastic.core.common.di.ServiceScope
import org.meshtastic.core.common.util.nowSeconds
import org.meshtastic.core.common.util.safeCatching
import org.meshtastic.core.model.ConnectionState
import org.meshtastic.core.model.identity.NameBroadcast
import org.meshtastic.core.model.identity.NameBroadcastCheck
import org.meshtastic.core.repository.ConnectionStateProvider
import org.meshtastic.core.repository.MessagingController
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.repository.SCREEN_DATA
import org.meshtastic.proto.User
import kotlin.time.Duration.Companion.seconds

private val APPLY_TIMEOUT = NameBroadcast.APPLY_TIMEOUT_SECONDS.seconds

/** What the last name broadcast, automatic or manual, came to. */
sealed interface NameBroadcastEvent {
    /** A new name was written to my radio: it is broadcast once the radio reports it. */
    data class Waiting(val longName: String) : NameBroadcastEvent

    /** [longName] was handed to my radio for broadcast. */
    data class Sent(val longName: String, val automatic: Boolean) : NameBroadcastEvent

    /** The packet was not handed to my radio. */
    data object Failed : NameBroadcastEvent

    /** My radio did not report [longName] in time after the change: nothing was broadcast. */
    data class NotApplied(val longName: String) : NameBroadcastEvent
}

/**
 * Broadcasts my radio's name to the whole network ([NameBroadcast]): once automatically after a name or team change
 * made from this phone, and on demand from "Diffuser mon nom". Shared by the User settings and Teams screens, so the
 * 30 s wait after a broadcast holds whichever screen made it.
 *
 * The automatic broadcast waits until my radio reports the new names (the firmware applies a new owner, then
 * restarts), so it never carries the old ones. Nothing is sent while the radio is not connected.
 */
@Single
class NameBroadcaster(
    @Named(SCREEN_DATA) private val nodeRepository: NodeRepository,
    @Named(SCREEN_DATA) private val connectionStateProvider: ConnectionStateProvider,
    @Named(SCREEN_DATA) private val messagingController: MessagingController,
    private val scope: ServiceScope,
) {
    /** The clock, in epoch seconds; replaced in tests. */
    internal var clock: () -> Long = { nowSeconds }

    private val sendLock = Mutex()
    private var lastBroadcastEpochSeconds: Long? = null
    private var pending: Job? = null

    private val _event = MutableStateFlow<NameBroadcastEvent?>(null)
    val event: StateFlow<NameBroadcastEvent?> = _event.asStateFlow()

    /** Whether "Diffuser mon nom" can broadcast now. */
    fun check(): NameBroadcastCheck = NameBroadcast.check(
        ownUser = nodeRepository.ourNodeInfo.value?.user,
        connected = connected(),
        nowEpochSeconds = clock(),
        lastBroadcastEpochSeconds = lastBroadcastEpochSeconds,
    )

    /** "Diffuser mon nom", after the user's confirmation: one broadcast, if [check] allows it now. */
    suspend fun broadcastNow(): Boolean = sendLock.withLock {
        val user = nodeRepository.ourNodeInfo.value?.user
        if (check() !is NameBroadcastCheck.Ready || user == null) return@withLock false
        send(user, automatic = false)
    }

    /**
     * Called once [sent] has been written as my radio's owner over [before]. When a name changed, waits for my radio to
     * report the new names, then broadcasts them once; gives up after [NameBroadcast.APPLY_TIMEOUT_SECONDS]. A newer
     * change replaces a wait still running. Licensed (ham) mode is left alone: the firmware composes its long name.
     */
    fun ownerWritten(before: User, sent: User) {
        if (!NameBroadcast.namesChanged(before, sent) || sent.is_licensed) return
        pending?.cancel()
        _event.value = NameBroadcastEvent.Waiting(sent.long_name)
        pending = scope.launch { broadcastOnceApplied(sent) }
    }

    private suspend fun broadcastOnceApplied(expected: User) {
        val applied =
            withTimeoutOrNull(APPLY_TIMEOUT) {
                reportedWhileConnected().first { NameBroadcast.isApplied(it, expected) }
            }
        if (applied == null) {
            _event.value = NameBroadcastEvent.NotApplied(expected.long_name)
            return
        }
        // A manual broadcast a moment ago still holds the channel back.
        lastBroadcastEpochSeconds?.let { delay(NameBroadcast.cooldownRemaining(it, clock()).seconds) }
        sendLock.withLock { send(applied, automatic = true) }
    }

    /** My radio's `User` as it reports it, null while it is unknown or not connected. */
    private fun reportedWhileConnected(): Flow<User?> =
        combine(nodeRepository.ourNodeInfo, connectionStateProvider.connectionState) { node, connection ->
            node?.user?.takeIf { connection is ConnectionState.Connected }
        }

    /** Hands the packet to my radio if it is still connected. */
    private suspend fun send(user: User, automatic: Boolean): Boolean {
        val now = clock()
        val event =
            when {
                !connected() -> NameBroadcastEvent.Failed
                else -> handOver(user, automatic)
            }
        val done = event != NameBroadcastEvent.Failed
        if (done) lastBroadcastEpochSeconds = now
        _event.value = event
        return done
    }

    private suspend fun handOver(user: User, automatic: Boolean): NameBroadcastEvent {
        val sent = safeCatching { messagingController.sendMessage(NameBroadcast.packet(user)) }
        sent.exceptionOrNull()?.let { Logger.w(it) { "Name broadcast was not handed to the radio" } }
        return if (sent.isSuccess) NameBroadcastEvent.Sent(user.long_name, automatic) else NameBroadcastEvent.Failed
    }

    private fun connected() = connectionStateProvider.connectionState.value is ConnectionState.Connected
}
