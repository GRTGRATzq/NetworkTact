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
package org.meshtastic.core.model.geo

import org.meshtastic.core.model.Node
import org.meshtastic.proto.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.minutes

class RadioPositionBroadcastTest {
    private val now = 1_760_000_000L

    private fun radio(fixAgeSeconds: Long?, time: Int = 0, hasPosition: Boolean = true): Node {
        val position =
            Position.Builder()
                .also { wb ->
                    if (hasPosition) {
                        wb.latitude_i = 488_584_000
                        wb.longitude_i = 23_470_000
                    }
                    wb.timestamp = fixAgeSeconds?.let { (now - it).toInt() } ?: 0
                    wb.time = time
                }
                .build()
        return Node(num = 1, position = position)
    }

    private fun check(
        node: Node?,
        connected: Boolean = true,
        demo: Boolean = false,
        lastBroadcast: Long? = null,
    ): BroadcastCheck = RadioPositionBroadcast.check(node, connected, demo, now, lastBroadcast)

    @Test
    fun freshPositionIsReadyWithItsFixTime() {
        val ready = assertIs<BroadcastCheck.Ready>(check(radio(fixAgeSeconds = 60)))
        assertEquals(now - 60, ready.fixEpochSeconds)
        assertEquals(1.minutes, ready.age)
        assertEquals(false, ready.old)
    }

    @Test
    fun oldPositionAsksForAConfirmationThatSaysHowOld() {
        val ready = assertIs<BroadcastCheck.Ready>(check(radio(fixAgeSeconds = 25 * 60)))
        assertEquals(true, ready.old)
        assertEquals(25.minutes, ready.age)
    }

    @Test
    fun unknownFixTimeIsRefused() {
        assertEquals(BroadcastCheck.Refused(BroadcastRefusal.FIX_TIME_UNKNOWN), check(radio(fixAgeSeconds = null)))
        // Position.time alone is only a lower bound (the reception time when the radio left it empty): still refused.
        assertEquals(
            BroadcastCheck.Refused(BroadcastRefusal.FIX_TIME_UNKNOWN),
            check(radio(fixAgeSeconds = null, time = (now - 30).toInt())),
        )
    }

    @Test
    fun fixTimeInTheFutureIsRefused() {
        assertEquals(
            BroadcastCheck.Refused(BroadcastRefusal.FIX_TIME_INCONSISTENT),
            check(radio(fixAgeSeconds = -10 * 60)),
        )
    }

    @Test
    fun withoutRadioPositionTheBroadcastIsRefusedAndThePhoneIsNeverUsed() {
        assertEquals(BroadcastCheck.Refused(BroadcastRefusal.NO_RADIO_POSITION), check(null))
        assertEquals(
            BroadcastCheck.Refused(BroadcastRefusal.NO_RADIO_POSITION),
            check(radio(fixAgeSeconds = 60, hasPosition = false)),
        )
    }

    @Test
    fun disconnectedRadioIsRefused() {
        assertEquals(
            BroadcastCheck.Refused(BroadcastRefusal.NOT_CONNECTED),
            check(radio(fixAgeSeconds = 60), connected = false),
        )
    }

    @Test
    fun demoModeIsAlwaysRefused() {
        assertEquals(BroadcastCheck.Refused(BroadcastRefusal.DEMO), check(radio(fixAgeSeconds = 60), demo = true))
    }

    @Test
    fun aSecondBroadcastWaitsThirtySeconds() {
        assertEquals(BroadcastCheck.CoolingDown(30), check(radio(fixAgeSeconds = 60), lastBroadcast = now))
        assertEquals(BroadcastCheck.CoolingDown(1), check(radio(fixAgeSeconds = 60), lastBroadcast = now - 29))
        assertIs<BroadcastCheck.Ready>(check(radio(fixAgeSeconds = 60), lastBroadcast = now - 30))
    }

    @Test
    fun clockSetBackKeepsTheFullWait() {
        assertEquals(30, RadioPositionBroadcast.cooldownRemaining(lastBroadcastEpochSeconds = now + 600, now))
    }

    @Test
    fun refusalsComeBeforeTheCountdown() {
        assertEquals(
            BroadcastCheck.Refused(BroadcastRefusal.FIX_TIME_UNKNOWN),
            check(radio(fixAgeSeconds = null), lastBroadcast = now),
        )
        assertEquals(
            BroadcastCheck.Refused(BroadcastRefusal.NOT_CONNECTED),
            check(radio(fixAgeSeconds = 60), connected = false, lastBroadcast = now),
        )
    }
}
