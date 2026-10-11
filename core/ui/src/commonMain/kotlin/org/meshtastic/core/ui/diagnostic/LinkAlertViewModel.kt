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
package org.meshtastic.core.ui.diagnostic

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import org.koin.core.annotation.KoinViewModel
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.model.diagnostic.HeardNode
import org.meshtastic.core.model.diagnostic.LinkAlert
import org.meshtastic.core.model.diagnostic.LinkAlerts
import org.meshtastic.core.model.freshness.FreshnessThresholds
import org.meshtastic.core.repository.ConnectionStateProvider
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.repository.SCREEN_DATA
import org.meshtastic.core.ui.viewmodel.stateInWhileSubscribed

/**
 * NetworkTact: the situation the user closed the link banner on, shared by the Messages and Network tabs. In memory
 * only: a new launch shows the banner again if the situation still holds.
 */
@Single
class LinkAlertDismissals {
    private val dismissed = MutableStateFlow<String?>(null)
    val dismissedKey: StateFlow<String?> = dismissed.asStateFlow()

    fun dismiss(key: String) {
        dismissed.value = key
    }
}

/**
 * The banner offering the link diagnostic: "radio not connected" or "no node heard for N min", computed from the
 * screens' sources (fictitious data in demo mode), recomputed every 15 s. Closed, it comes back only when the situation
 * changes ([LinkAlert.key]). Nothing here transmits.
 */
@KoinViewModel
class LinkAlertViewModel(
    @Named(SCREEN_DATA) nodeRepository: NodeRepository,
    @Named(SCREEN_DATA) connectionStateProvider: ConnectionStateProvider,
    private val dismissals: LinkAlertDismissals,
    private val clockMillis: () -> Long = { nowMillis },
) : ViewModel() {

    private val thresholds = FreshnessThresholds.Default

    private val clock: Flow<Long> = flow {
        while (true) {
            emit(clockMillis())
            delay(thresholds.refreshInterval)
        }
    }

    private val nodes =
        combine(nodeRepository.nodeDBbyNum, nodeRepository.ourNodeInfo) { nodes, ourNode ->
            nodes.values.filter { !it.isIgnored }.map { HeardNode(it.num, it.user.long_name, it.lastHeard) } to
                ourNode?.num
        }

    val alert: StateFlow<LinkAlert?> =
        combine(connectionStateProvider.connectionLifecycle, nodes, dismissals.dismissedKey, clock) {
                lifecycle,
                (heard, myNodeNum),
                dismissedKey,
                now,
            ->
            val current = LinkAlerts.current(lifecycle.state, lifecycle.epochs, heard, myNodeNum, now, thresholds)
            LinkAlerts.visible(current, dismissedKey)
        }
            .stateInWhileSubscribed(initialValue = null)

    /** Closes the banner until the situation changes. */
    fun dismiss(alert: LinkAlert) = dismissals.dismiss(alert.key)
}
