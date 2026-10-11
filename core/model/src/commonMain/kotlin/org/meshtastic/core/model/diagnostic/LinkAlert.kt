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
package org.meshtastic.core.model.diagnostic

import org.meshtastic.core.model.ConnectionEpochs
import org.meshtastic.core.model.ConnectionState
import org.meshtastic.core.model.freshness.FreshnessThresholds
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * The banner that offers the diagnostic. [key] identifies the situation: a banner closed by the user comes back only
 * when the key changes (another threshold crossed, a node heard again then lost, the link lost then back).
 */
sealed interface LinkAlert {
    val key: String

    /** My radio is not connected to the phone over Bluetooth. */
    data class RadioNotConnected(override val key: String) : LinkAlert

    /**
     * The radio is connected and no other node was heard within the "recent contact" threshold.
     *
     * @property since how long ago the last other node was heard; null when no other node was ever heard.
     */
    data class NoNodeHeard(val since: Duration?, override val key: String) : LinkAlert
}

object LinkAlerts {

    /** The thresholds after the "recent contact" one at which a closed banner comes back; then every hour. */
    private val LATER_STEPS = listOf(30.minutes, 1.hours)

    /**
     * The banner to show, or null. Only [ConnectionState.Disconnected] reads as "not connected": connecting and the
     * radio's light sleep are normal, passing states.
     */
    fun current(
        connection: ConnectionState,
        epochs: ConnectionEpochs,
        nodes: List<HeardNode>,
        myNodeNum: Int?,
        nowMillis: Long,
        thresholds: FreshnessThresholds = FreshnessThresholds.Default,
    ): LinkAlert? = when (connection) {
        ConnectionState.Disconnected -> LinkAlert.RadioNotConnected(key = "disconnected:${epochs.departures}")
        ConnectionState.Connected -> noNodeHeard(epochs, nodes, myNodeNum, nowMillis, thresholds)
        else -> null
    }

    private fun noNodeHeard(
        epochs: ConnectionEpochs,
        nodes: List<HeardNode>,
        myNodeNum: Int?,
        nowMillis: Long,
        thresholds: FreshnessThresholds,
    ): LinkAlert? {
        val nowSeconds = nowMillis / MILLIS_PER_SECOND
        val latest = LinkDiagnostics.heardAges(nodes, myNodeNum, nowSeconds, thresholds).minByOrNull { it.second }
        val alert =
            if (latest == null) {
                LinkAlert.NoNodeHeard(since = null, key = "none:${epochs.completedHandshakes}")
            } else {
                val (node, age) = latest
                val step = step(age, thresholds.recentContact)
                LinkAlert.NoNodeHeard(
                    since = age,
                    key = "silent:${epochs.completedHandshakes}:${node.lastHeardEpoch()}:$step",
                )
                    .takeIf { step > 0 }
            }
        return alert
    }

    /** [alert] unless the user closed that same situation ([dismissedKey]). */
    fun visible(alert: LinkAlert?, dismissedKey: String?): LinkAlert? = alert?.takeIf { it.key != dismissedKey }

    /**
     * How many thresholds [age] has crossed: 0 under the "recent contact" one, 1 past it, then one more at 30 min, at 1
     * h and every hour after that.
     */
    fun step(age: Duration, recentContact: Duration): Int {
        val steps = listOf(recentContact) + LATER_STEPS.filter { it > recentContact }
        val crossed = steps.count { age > it }
        val extraHours = if (age > LATER_STEPS.last()) ((age - LATER_STEPS.last()) / 1.hours).toInt() else 0
        return crossed + extraHours
    }

    private const val MILLIS_PER_SECOND = 1000L
}
