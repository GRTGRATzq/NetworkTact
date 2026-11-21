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
package org.meshtastic.feature.messaging.firerequest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.firerequest.FireRequestAck
import org.meshtastic.core.model.firerequest.FireRequestReply
import org.meshtastic.core.model.firerequest.FireRequestThread
import org.meshtastic.core.model.firerequest.ReplyKind
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.firereq_ack_incomplete
import org.meshtastic.core.resources.firereq_ack_note
import org.meshtastic.core.resources.firereq_ack_send
import org.meshtastic.core.resources.firereq_ack_sent
import org.meshtastic.core.resources.firereq_designated_to_you
import org.meshtastic.core.resources.firereq_reply_resend

/**
 * « Accusé DT » on a sheet received on a channel: sent by hand, in a direct message to the writer only, once the sheet
 * is complete (N / N). Once per sheet and post: afterwards the button stays greyed, « Accusé envoyé <GDH> ».
 */
@Composable
internal fun AckButton(thread: FireRequestThread, canSend: Boolean, actions: FireRequestCardActions) =
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        val sent = thread.ownAcknowledgement
        OutlinedButton(
            onClick = { actions.sendDirect(FireRequestAck(thread.number, actions.gdhNow()).text(), thread.sender) },
            enabled = canSend && sent == null && thread.isComplete,
        ) {
            Text(
                if (sent != null) {
                    stringResource(Res.string.firereq_ack_sent, sent.gdh)
                } else {
                    stringResource(Res.string.firereq_ack_send)
                },
            )
        }
        when {
            sent != null -> Unit

            !thread.isComplete ->
                Help(stringResource(Res.string.firereq_ack_incomplete, thread.received.size, thread.count))

            else -> Help(stringResource(Res.string.firereq_ack_note))
        }
    }

/**
 * On the radio the fire chief designated: « Tir attribué à vous par <émetteur> » and « GDH fin », sent in a direct
 * message to the writer, as in v1 ([endRecipient]).
 */
@Composable
internal fun DesignatedPart(
    thread: FireRequestThread,
    radios: List<AllocationRadio>,
    canSend: Boolean,
    actions: FireRequestCardActions,
) {
    val (_, designation) = thread.designation ?: return
    var asking by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            stringResource(Res.string.firereq_designated_to_you, designation.requester, designation.gdh),
            style = MaterialTheme.typography.bodySmall,
        )
        if (canSend) {
            val kind = ReplyKind.END
            if (thread.end != null) {
                FilledTonalButton(onClick = { asking = true }) {
                    Text(stringResource(Res.string.firereq_reply_resend, kind.label))
                }
            } else {
                OutlinedButton(onClick = { asking = true }) { Text(kind.label) }
            }
        }
    }
    if (asking) {
        ReplyDialog(ReplyKind.END, onDismiss = { asking = false }) { gdh ->
            asking = false
            actions.sendDirect(FireRequestReply(thread.number, ReplyKind.END, gdh).text(), endRecipient(thread, radios))
        }
    }
}

/**
 * Where « GDH fin » goes: the radio whose short name is the writer named by the allocation, when only one has it,
 * otherwise the sender of the sheet.
 */
internal fun endRecipient(thread: FireRequestThread, radios: List<AllocationRadio>): String {
    val requester = thread.designation?.second?.requester.orEmpty()
    return radios.filter { it.shortName.isNotBlank() && it.shortName == requester }.singleOrNull()?.id ?: thread.sender
}
