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

/**
 * What the app can honestly claim about a message this device sent, derived only from the persisted [MessageStatus],
 * routing error and ack proof. Nothing here is inferred beyond what those fields establish:
 * - `ENROUTE` is set when the phone hands the packet to the radio link (`PacketHandlerImpl.dispatchToRadio`).
 * - `DELIVERED` is any ack whose sender is not the addressed node, i.e. an implicit ack from a relay
 *   (`MeshDataHandlerImpl.handleAckNak`).
 * - `RECEIVED` on an outgoing packet means the ack's sender is the addressed node. A channel message is addressed to
 *   the broadcast address, which no node can match, so it never legitimately reaches this state.
 * - The ack proof, when the firmware carries one, is the radio's cryptographic verdict on that ack.
 */
sealed interface SentStatus {
    /** Stored locally, not yet handed to the radio. */
    data object AwaitingRadio : SentStatus

    /** Handed to this device's radio; nothing heard back yet. */
    data object HandedToRadio : SentStatus

    /** Some node on the mesh acknowledged or relayed it; the recipient has not confirmed. */
    data object RelayedByMesh : SentStatus

    /** Direct message acknowledged by the addressed node; [verified] when the radio validated the ack proof. */
    data class AckedByRecipient(val verified: Boolean) : SentStatus

    /** An ack carried a proof that did not verify: a possible forgery, never shown as delivered. */
    data object InvalidProof : SentStatus

    /** Not delivered; [routingError] is the firmware's reason (or the app's timeout stamp). */
    data class Failed(val routingError: Int) : SentStatus

    /** No status recorded. */
    data object Unknown : SentStatus
}

fun sentStatusOf(status: MessageStatus?, routingError: Int, ackProofStatus: Int, isDirectMessage: Boolean): SentStatus {
    val proof = MeshPacket.AckProofStatus.fromValue(ackProofStatus)
    if (proof == MeshPacket.AckProofStatus.ACK_PROOF_INVALID) return SentStatus.InvalidProof
    return when (status) {
        MessageStatus.QUEUED -> SentStatus.AwaitingRadio

        MessageStatus.ENROUTE,
        MessageStatus.SFPP_ROUTING,
        -> SentStatus.HandedToRadio

        MessageStatus.DELIVERED,
        MessageStatus.SFPP_CONFIRMED,
        -> SentStatus.RelayedByMesh

        MessageStatus.RECEIVED ->
            if (isDirectMessage) {
                SentStatus.AckedByRecipient(verified = proof == MeshPacket.AckProofStatus.ACK_PROOF_VALID)
            } else {
                SentStatus.RelayedByMesh
            }

        MessageStatus.ERROR -> SentStatus.Failed(routingError)

        MessageStatus.UNKNOWN,
        null,
        -> SentStatus.Unknown
    }
}

/** True for a direct message this device sent that the addressed node has not (validly) acknowledged yet. */
fun isAwaitingRecipientAck(sentStatus: SentStatus, fromLocal: Boolean, isDirectMessage: Boolean): Boolean =
    fromLocal && isDirectMessage && sentStatus !is SentStatus.AckedByRecipient
