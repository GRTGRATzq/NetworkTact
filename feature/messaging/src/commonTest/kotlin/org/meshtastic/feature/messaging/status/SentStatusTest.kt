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
package org.meshtastic.feature.messaging.status

import org.meshtastic.core.model.MessageStatus
import org.meshtastic.proto.MeshPacket
import org.meshtastic.proto.Routing
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SentStatusTest {

    private val absent = MeshPacket.AckProofStatus.ACK_PROOF_ABSENT.value
    private val valid = MeshPacket.AckProofStatus.ACK_PROOF_VALID.value
    private val invalid = MeshPacket.AckProofStatus.ACK_PROOF_INVALID.value
    private val noKey = MeshPacket.AckProofStatus.ACK_PROOF_NO_KEY.value

    private fun direct(status: MessageStatus?, proof: Int = absent, routingError: Int = 0) =
        sentStatusOf(status, routingError, proof, isDirectMessage = true)

    private fun channel(status: MessageStatus?, proof: Int = absent, routingError: Int = 0) =
        sentStatusOf(status, routingError, proof, isDirectMessage = false)

    @Test
    fun queuedIsAwaitingRadio() {
        assertEquals(SentStatus.AwaitingRadio, direct(MessageStatus.QUEUED))
        assertEquals(SentStatus.AwaitingRadio, channel(MessageStatus.QUEUED))
    }

    @Test
    fun enrouteIsHandedToRadio() {
        assertEquals(SentStatus.HandedToRadio, direct(MessageStatus.ENROUTE))
        assertEquals(SentStatus.HandedToRadio, channel(MessageStatus.ENROUTE))
        assertEquals(SentStatus.HandedToRadio, channel(MessageStatus.SFPP_ROUTING))
    }

    @Test
    fun channelMessageMarkedDeliveredIsOnlyRelayed() {
        assertEquals(SentStatus.RelayedByMesh, channel(MessageStatus.DELIVERED))
        assertEquals(SentStatus.RelayedByMesh, channel(MessageStatus.SFPP_CONFIRMED))
    }

    @Test
    fun channelMessageNeverReadsAsAcknowledged() {
        // Defensive: even a RECEIVED status, with or without a valid proof, stays "relayed" on a channel.
        MessageStatus.entries.forEach { status ->
            listOf(absent, valid, noKey).forEach { proof ->
                assertFalse(channel(status, proof) is SentStatus.AckedByRecipient, "$status / $proof")
            }
        }
        assertEquals(SentStatus.RelayedByMesh, channel(MessageStatus.RECEIVED, valid))
    }

    @Test
    fun directImplicitAckIsOnlyRelayed() {
        assertEquals(SentStatus.RelayedByMesh, direct(MessageStatus.DELIVERED))
    }

    @Test
    fun directAckFromRecipientWithoutProofIsAcknowledgedUnverified() {
        assertEquals(SentStatus.AckedByRecipient(verified = false), direct(MessageStatus.RECEIVED, absent))
        assertEquals(SentStatus.AckedByRecipient(verified = false), direct(MessageStatus.RECEIVED, noKey))
    }

    @Test
    fun directAckWithValidProofIsVerified() {
        assertEquals(SentStatus.AckedByRecipient(verified = true), direct(MessageStatus.RECEIVED, valid))
    }

    @Test
    fun validProofWithoutRecipientAckIsNotVerifiedDelivery() {
        assertEquals(SentStatus.RelayedByMesh, direct(MessageStatus.DELIVERED, valid))
    }

    @Test
    fun invalidProofIsAWarningWhateverTheStatus() {
        assertEquals(SentStatus.InvalidProof, direct(MessageStatus.RECEIVED, invalid))
        assertEquals(SentStatus.InvalidProof, direct(MessageStatus.DELIVERED, invalid))
        assertEquals(SentStatus.InvalidProof, channel(MessageStatus.DELIVERED, invalid))
    }

    @Test
    fun errorCarriesTheRoutingReason() {
        val failed = direct(MessageStatus.ERROR, routingError = Routing.Error.MAX_RETRANSMIT.value)
        assertIs<SentStatus.Failed>(failed)
        assertEquals(Routing.Error.MAX_RETRANSMIT.value, failed.routingError)
        assertEquals(
            SentStatus.Failed(Routing.Error.TIMEOUT.value),
            channel(MessageStatus.ERROR, routingError = Routing.Error.TIMEOUT.value),
        )
    }

    @Test
    fun missingStatusIsUnknown() {
        assertEquals(SentStatus.Unknown, direct(null))
        assertEquals(SentStatus.Unknown, channel(MessageStatus.UNKNOWN))
    }

    @Test
    fun unackedFilterKeepsSentDirectMessagesWithoutRecipientAck() {
        assertTrue(isAwaitingRecipientAck(SentStatus.RelayedByMesh, fromLocal = true, isDirectMessage = true))
        assertTrue(isAwaitingRecipientAck(SentStatus.HandedToRadio, fromLocal = true, isDirectMessage = true))
        assertTrue(isAwaitingRecipientAck(SentStatus.Failed(0), fromLocal = true, isDirectMessage = true))
        assertTrue(isAwaitingRecipientAck(SentStatus.InvalidProof, fromLocal = true, isDirectMessage = true))
        assertFalse(
            isAwaitingRecipientAck(SentStatus.AckedByRecipient(false), fromLocal = true, isDirectMessage = true),
        )
    }

    @Test
    fun unackedFilterExcludesChannelAndReceivedMessages() {
        assertFalse(isAwaitingRecipientAck(SentStatus.RelayedByMesh, fromLocal = true, isDirectMessage = false))
        assertFalse(isAwaitingRecipientAck(SentStatus.Unknown, fromLocal = false, isDirectMessage = true))
    }
}
