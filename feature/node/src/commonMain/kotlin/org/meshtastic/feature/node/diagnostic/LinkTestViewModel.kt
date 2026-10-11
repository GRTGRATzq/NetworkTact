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
package org.meshtastic.feature.node.diagnostic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import org.koin.core.annotation.Named
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.model.ConnectionState
import org.meshtastic.core.repository.ConnectionStateProvider
import org.meshtastic.core.repository.DemoMode
import org.meshtastic.core.repository.SCREEN_DATA
import org.meshtastic.core.ui.viewmodel.stateInWhileSubscribed
import org.meshtastic.feature.node.component.COOL_DOWN_TIME_MS
import org.meshtastic.feature.node.detail.NodeRequestActions
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** The last test sent from this screen. */
data class LinkTestSent(val target: TestTarget, val atMillis: Long, val simulated: Boolean)

data class LinkTestUiState(
    val connected: Boolean,
    val demo: Boolean,
    /** Seconds before another test is allowed; 0 when allowed. */
    val waitSeconds: Int,
    val lastSent: LinkTestSent?,
) {
    val canTest: Boolean
        get() = (connected || demo) && waitSeconds == 0
}

/**
 * "Test the link": one traceroute to a node the user picked, after a confirmation that warns of the airtime. Manual
 * only, never automatic nor periodic. It goes through the existing request function ([NodeRequestActions], the node
 * page's traceroute), whose reply shows the usual way. A position request is not offered: it would attach this radio's
 * position stamped with the phone's current time instead of its fix time.
 *
 * The wait between tests is the node page's ([LINK_TEST_COOLDOWN_MS], from the last traceroute sent anywhere in the
 * app). In demo mode the test is simulated: nothing reaches the radio.
 */
@KoinViewModel
class LinkTestViewModel(
    private val nodeRequestActions: NodeRequestActions,
    @Named(SCREEN_DATA) private val connectionStateProvider: ConnectionStateProvider,
    private val demoMode: DemoMode,
    private val clockMillis: () -> Long = { nowMillis },
) : ViewModel() {

    private val lastSent = MutableStateFlow<LinkTestSent?>(null)
    private val lastSimulatedMillis = MutableStateFlow<Long?>(null)

    private val ticker: Flow<Long> = flow {
        while (true) {
            emit(clockMillis())
            delay(1.seconds)
        }
    }

    private val lastTestMillis =
        combine(demoMode.isActive, nodeRequestActions.lastTracerouteTime, lastSimulatedMillis) {
                demo,
                real,
                simulated,
            ->
            if (demo) simulated else real
        }

    val uiState: StateFlow<LinkTestUiState> =
        combine(connectionStateProvider.connectionState, demoMode.isActive, lastTestMillis, lastSent, ticker) {
                connection,
                demo,
                lastTest,
                sent,
                now,
            ->
            LinkTestUiState(
                connected = connection == ConnectionState.Connected,
                demo = demo,
                waitSeconds = waitSeconds(lastTest, now),
                lastSent = sent?.takeIf { it.simulated == demo },
            )
        }
            .stateInWhileSubscribed(initialValue = LinkTestUiState(false, false, 0, null))

    private var sending = false

    /**
     * Sends one traceroute to [target], unless not connected, still waiting or already sending. Returns whether it was
     * started. [LinkTestUiState.lastSent] is set only once the radio took the request: a refusal is reported by the
     * request function's own message, as on the node page.
     */
    fun traceroute(target: TestTarget): Boolean {
        val now = clockMillis()
        val demo = demoMode.isActive.value
        val lastTest = if (demo) lastSimulatedMillis.value else nodeRequestActions.lastTracerouteTime.value
        val connected = connectionStateProvider.connectionState.value == ConnectionState.Connected
        val allowed = !sending && (connected || demo) && waitSeconds(lastTest, now) == 0
        if (allowed) {
            if (demo) simulate(target, now) else send(target)
        }
        return allowed
    }

    private fun simulate(target: TestTarget, now: Long) {
        lastSimulatedMillis.value = now
        lastSent.value = LinkTestSent(target, now, simulated = true)
    }

    private fun send(target: TestTarget) {
        sending = true
        viewModelScope.launch {
            try {
                val before = nodeRequestActions.lastTracerouteTime.value
                nodeRequestActions.requestTraceroute(target.num, target.name)
                val after = nodeRequestActions.lastTracerouteTime.value
                if (after != null && after != before) lastSent.value = LinkTestSent(target, after, simulated = false)
            } finally {
                sending = false
            }
        }
    }

    companion object {
        /** The node page's traceroute cooldown. */
        const val LINK_TEST_COOLDOWN_MS = COOL_DOWN_TIME_MS

        fun waitSeconds(lastTestMillis: Long?, nowMillis: Long): Int {
            val remaining = (lastTestMillis ?: return 0) + LINK_TEST_COOLDOWN_MS - nowMillis
            return if (remaining <= 0) 0 else remaining.milliseconds.inWholeSeconds.toInt() + 1
        }
    }
}
