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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.minutes

class LinkAlertsTest {

    private val nowSeconds = 1_800_000_000L
    private val pc0 = 1
    private val connected = ConnectionEpochs(completedHandshakes = 1)

    private fun bravoHeardAgo(minutes: Int) = listOf(
        HeardNode(pc0, "PC-0", nowSeconds.toInt()),
        HeardNode(3, "BRAVO-2", (nowSeconds - minutes * 60L).toInt()),
    )

    private fun alert(
        nodes: List<HeardNode>,
        connection: ConnectionState = ConnectionState.Connected,
        epochs: ConnectionEpochs = connected,
        atSeconds: Long = nowSeconds,
    ) = LinkAlerts.current(connection, epochs, nodes, pc0, atSeconds * 1000)

    @Test
    fun noBannerWhileANodeWasHeardRecently() {
        assertNull(alert(bravoHeardAgo(14)))
        assertNull(alert(bravoHeardAgo(15)))
    }

    @Test
    fun bannerPastTheRecentContactThresholdWithTheAge() {
        val banner = alert(bravoHeardAgo(18))
        assertIs<LinkAlert.NoNodeHeard>(banner)
        assertEquals(18.minutes, banner.since)
    }

    @Test
    fun myOwnRadioDoesNotCountAsHeard() {
        val banner = alert(listOf(HeardNode(pc0, "PC-0", nowSeconds.toInt())))
        assertIs<LinkAlert.NoNodeHeard>(banner)
        assertNull(banner.since)
    }

    @Test
    fun disconnectedRadioGivesRadioNotConnectedInsteadOfNoNodeHeard() {
        assertIs<LinkAlert.RadioNotConnected>(alert(bravoHeardAgo(40), connection = ConnectionState.Disconnected))
    }

    @Test
    fun connectingAndSleepingShowNoBanner() {
        assertNull(alert(bravoHeardAgo(40), connection = ConnectionState.Connecting))
        assertNull(alert(bravoHeardAgo(40), connection = ConnectionState.DeviceSleep))
    }

    @Test
    fun closedBannerStaysClosedWithinTheSameStep() {
        val first = alert(bravoHeardAgo(18))
        val later = alert(bravoHeardAgo(18), atSeconds = nowSeconds + 7 * 60)
        assertEquals(first?.key, later?.key)
        assertNull(LinkAlerts.visible(later, dismissedKey = first?.key))
    }

    @Test
    fun closedBannerComesBackAtTheNextThreshold() {
        val first = alert(bravoHeardAgo(18))
        val next = alert(bravoHeardAgo(18), atSeconds = nowSeconds + 13 * 60)
        assertNotEquals(first?.key, next?.key)
        assertEquals(next, LinkAlerts.visible(next, dismissedKey = first?.key))
    }

    @Test
    fun closedBannerComesBackWhenTheLinkIsLostThenBack() {
        val before = alert(bravoHeardAgo(18))
        val reconnected = alert(bravoHeardAgo(18), epochs = ConnectionEpochs(departures = 1, completedHandshakes = 2))
        assertNotEquals(before?.key, reconnected?.key)
        val lost = alert(bravoHeardAgo(18), connection = ConnectionState.Disconnected)
        val lostAgain =
            alert(
                bravoHeardAgo(18),
                connection = ConnectionState.Disconnected,
                epochs = ConnectionEpochs(departures = 2, completedHandshakes = 2),
            )
        assertNotEquals(lost?.key, lostAgain?.key)
    }

    @Test
    fun closedBannerComesBackWhenANodeIsHeardThenLostAgain() {
        val first = alert(bravoHeardAgo(18))
        val muchLater = alert(bravoHeardAgo(18), atSeconds = nowSeconds + 60 * 60)
        val heardAgainThenSilent =
            LinkAlerts.current(
                ConnectionState.Connected,
                connected,
                listOf(HeardNode(3, "BRAVO-2", (nowSeconds + 40 * 60).toInt())),
                pc0,
                (nowSeconds + 58 * 60) * 1000,
            )
        assertNotEquals(first?.key, muchLater?.key)
        assertNotEquals(first?.key, heardAgainThenSilent?.key)
    }

    @Test
    fun stepsAreRecentContactThirtyMinutesOneHourThenEveryHour() {
        val recent = 15.minutes
        assertEquals(0, LinkAlerts.step(15.minutes, recent))
        assertEquals(1, LinkAlerts.step(16.minutes, recent))
        assertEquals(2, LinkAlerts.step(31.minutes, recent))
        assertEquals(3, LinkAlerts.step(61.minutes, recent))
        assertEquals(4, LinkAlerts.step(121.minutes, recent))
        assertEquals(5, LinkAlerts.step(181.minutes, recent))
    }
}
