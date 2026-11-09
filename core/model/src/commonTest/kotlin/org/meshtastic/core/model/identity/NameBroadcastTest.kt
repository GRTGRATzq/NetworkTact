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
package org.meshtastic.core.model.identity

import okio.ByteString.Companion.encodeUtf8
import org.meshtastic.core.model.NodeAddress
import org.meshtastic.proto.HardwareModel
import org.meshtastic.proto.PortNum
import org.meshtastic.proto.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class NameBroadcastTest {
    private val now = 1_790_000_000L

    private val user =
        User.Builder()
            .also { wb ->
                wb.id = "!0badcafe"
                wb.long_name = "CDS TIR [Alpha]"
                wb.short_name = "CDST"
                wb.hw_model = HardwareModel.TBEAM
                wb.public_key = "0123456789abcdef0123456789abcdef".encodeUtf8()
            }
            .build()

    private fun User.withLongName(name: String): User = newBuilder().also { it.long_name = name }.build()

    private fun User.withShortName(name: String): User = newBuilder().also { it.short_name = name }.build()

    @Test
    fun refusedWhenNoRadioIsConnected() {
        assertEquals(
            NameBroadcastCheck.Refused(NameBroadcastRefusal.NOT_CONNECTED),
            NameBroadcast.check(user, connected = false, nowEpochSeconds = now, lastBroadcastEpochSeconds = null),
        )
    }

    @Test
    fun refusedWhileMyRadioNameIsUnknown() {
        assertEquals(
            NameBroadcastCheck.Refused(NameBroadcastRefusal.NO_LOCAL_NODE),
            NameBroadcast.check(null, connected = true, nowEpochSeconds = now, lastBroadcastEpochSeconds = null),
        )
        val blank = user.withLongName(" ")
        assertEquals(
            NameBroadcastCheck.Refused(NameBroadcastRefusal.NO_LOCAL_NODE),
            NameBroadcast.check(blank, connected = true, nowEpochSeconds = now, lastBroadcastEpochSeconds = null),
        )
    }

    @Test
    fun readyWithTheNamesMyRadioReports() {
        assertEquals(
            NameBroadcastCheck.Ready("CDS TIR [Alpha]", "CDST"),
            NameBroadcast.check(user, connected = true, nowEpochSeconds = now, lastBroadcastEpochSeconds = null),
        )
    }

    @Test
    fun waitsThirtySecondsAfterABroadcast() {
        assertEquals(
            NameBroadcastCheck.CoolingDown(30),
            NameBroadcast.check(user, connected = true, nowEpochSeconds = now, lastBroadcastEpochSeconds = now),
        )
        assertEquals(
            NameBroadcastCheck.CoolingDown(1),
            NameBroadcast.check(user, connected = true, nowEpochSeconds = now + 29, lastBroadcastEpochSeconds = now),
        )
        assertEquals(
            NameBroadcastCheck.Ready("CDS TIR [Alpha]", "CDST"),
            NameBroadcast.check(user, connected = true, nowEpochSeconds = now + 30, lastBroadcastEpochSeconds = now),
        )
    }

    @Test
    fun aClockSetBackCountsAsNoTimeElapsed() {
        assertEquals(30L, NameBroadcast.cooldownRemaining(lastBroadcastEpochSeconds = now, nowEpochSeconds = now - 600))
    }

    @Test
    fun theDisconnectionIsReportedBeforeTheCooldown() {
        assertEquals(
            NameBroadcastCheck.Refused(NameBroadcastRefusal.NOT_CONNECTED),
            NameBroadcast.check(user, connected = false, nowEpochSeconds = now, lastBroadcastEpochSeconds = now),
        )
    }

    @Test
    fun onlyANameChangeCallsForABroadcast() {
        assertTrue(NameBroadcast.namesChanged(user.withLongName("CDS TIR"), user))
        assertTrue(NameBroadcast.namesChanged(user, user.withShortName("CDS")))
        val otherFieldOnly = user.newBuilder().also { it.is_unmessagable = true }.build()
        assertFalse(NameBroadcast.namesChanged(user, otherFieldOnly))
    }

    @Test
    fun appliedOnlyOnceMyRadioReportsBothNewNames() {
        val expected = user.withLongName("CDS TIR [Bravo]").withShortName("CDSB")
        assertFalse(NameBroadcast.isApplied(null, expected))
        assertFalse(NameBroadcast.isApplied(user, expected))
        assertFalse(NameBroadcast.isApplied(user.withLongName("CDS TIR [Bravo]"), expected))
        assertTrue(NameBroadcast.isApplied(expected, expected))
    }

    @Test
    fun packetIsAStandardNodeInfoBroadcastOnThePrimaryChannelWithoutAck() {
        val packet = NameBroadcast.packet(user)
        assertEquals(NodeAddress.ID_BROADCAST, packet.to)
        assertEquals(PortNum.NODEINFO_APP.value, packet.dataType)
        assertEquals(0, packet.channel)
        assertFalse(packet.wantAck)
    }

    @Test
    fun payloadIsMyRadioUserWithItsPublicKey() {
        val sent = User.ADAPTER.decode(assertNotNull(NameBroadcast.packet(user).bytes))
        assertEquals(user, sent)
        assertEquals(user.public_key, sent.public_key)
    }
}
