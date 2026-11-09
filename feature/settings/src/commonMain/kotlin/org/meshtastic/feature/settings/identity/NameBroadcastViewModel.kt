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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import org.meshtastic.core.model.identity.NameBroadcastCheck
import org.meshtastic.core.model.identity.NameBroadcastRefusal
import org.meshtastic.core.ui.viewmodel.stateInWhileSubscribed
import org.meshtastic.proto.User
import kotlin.time.Duration.Companion.seconds

data class NameBroadcastUiState(
    val check: NameBroadcastCheck = NameBroadcastCheck.Refused(NameBroadcastRefusal.NOT_CONNECTED),
    val event: NameBroadcastEvent? = null,
)

/** The "Diffuser mon nom" card: [NameBroadcaster]'s state, refreshed every second for the countdown. */
@KoinViewModel
class NameBroadcastViewModel(private val broadcaster: NameBroadcaster) : ViewModel() {

    private val ticks = flow {
        while (true) {
            emit(Unit)
            delay(1.seconds)
        }
    }

    val uiState: StateFlow<NameBroadcastUiState> =
        combine(ticks, broadcaster.event) { _, event -> NameBroadcastUiState(broadcaster.check(), event) }
            .stateInWhileSubscribed(initialValue = NameBroadcastUiState())

    /** One broadcast, once the user has confirmed it. */
    fun broadcast() {
        viewModelScope.launch { broadcaster.broadcastNow() }
    }

    /** [sent] was just written as my radio's owner over [before]: see [NameBroadcaster.ownerWritten]. */
    fun ownerWritten(before: User, sent: User) = broadcaster.ownerWritten(before, sent)
}
