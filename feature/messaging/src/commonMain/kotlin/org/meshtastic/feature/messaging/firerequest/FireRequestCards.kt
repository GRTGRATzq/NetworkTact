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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.firerequest.FireRequestReply
import org.meshtastic.core.model.firerequest.FireRequestThread
import org.meshtastic.core.model.firerequest.ReplyKind
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.firereq_card_sent
import org.meshtastic.core.resources.firereq_card_title
import org.meshtastic.core.resources.firereq_part_status
import org.meshtastic.core.resources.firereq_raw
import org.meshtastic.core.resources.firereq_received_count
import org.meshtastic.core.resources.firereq_reply_received
import org.meshtastic.core.resources.firereq_reply_waiting
import org.meshtastic.core.resources.firereq_resend_part
import org.meshtastic.feature.messaging.component.sentStatusText
import org.meshtastic.feature.messaging.status.SentStatus
import org.meshtastic.feature.messaging.status.sentStatusOf
import org.meshtastic.core.model.firerequest.FireRequestSheet as Sheet

/**
 * A fire request in a conversation, in place of its messages: the sender's card with the state of each part, or the
 * sheet rebuilt on the recipient's phone. Never the red of `[URG]`: its colours are the tertiary ones, with the DT
 * badge.
 */
@Composable
internal fun FireRequestCard(
    thread: FireRequestThread,
    view: FireRequestView,
    recipientName: String?,
    canSend: Boolean,
    actions: FireRequestCardActions,
    modifier: Modifier = Modifier,
) {
    var showRaw by remember { mutableStateOf(false) }
    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (thread.fromLocal) {
                SentRequest(thread, view, recipientName, canSend, actions)
            } else {
                ReceivedRequest(thread, canSend, actions)
            }
            TextButton(onClick = { showRaw = true }) {
                Text(stringResource(Res.string.firereq_raw, thread.messages.size))
            }
        }
    }
    if (showRaw) RawMessagesDialog(thread) { showRaw = false }
}

/** The sender's card: « DT AB1234 · 3 messages », the state of each part, the date-time groups sent back. */
@Composable
private fun SentRequest(
    thread: FireRequestThread,
    view: FireRequestView,
    recipientName: String?,
    canSend: Boolean,
    actions: FireRequestCardActions,
) = Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FireRequestBadge()
        Text(
            stringResource(Res.string.firereq_card_sent, thread.number, thread.count),
            style = MaterialTheme.typography.titleSmall,
        )
    }
    for (k in 1..thread.count) {
        // A part resent replaces the failed one: its latest message tells its state.
        val message = thread.parts[k]?.lastOrNull()?.let { view.messages[it.id] } ?: continue
        val status =
            sentStatusOf(message.status, message.routingError, message.ackProofStatus, isDirectMessage = true)
        Text(
            stringResource(Res.string.firereq_part_status, k, thread.count, sentStatusText(status, recipientName)),
            style = MaterialTheme.typography.bodySmall,
            color =
            if (status is SentStatus.Failed) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
        if (status is SentStatus.Failed && canSend) {
            TextButton(onClick = { actions.resend(message) }) {
                Text(stringResource(Res.string.firereq_resend_part, k))
            }
        }
    }
    ReplyState(ReplyKind.ALLOCATION, thread.allocation)
    ReplyState(ReplyKind.END, thread.end)
}

@Composable
private fun ReplyState(kind: ReplyKind, reply: FireRequestReply?) {
    Text(
        text =
        if (reply != null) {
            stringResource(Res.string.firereq_reply_received, kind.label, reply.gdh)
        } else {
            stringResource(Res.string.firereq_reply_waiting, kind.label)
        },
        style = MaterialTheme.typography.bodySmall,
    )
}

/** The recipient's sheet: header, then boxes 1 to 11 in clear, and the two date-time groups to send back. */
@Composable
private fun ReceivedRequest(thread: FireRequestThread, canSend: Boolean, actions: FireRequestCardActions) =
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val read = thread.read()
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FireRequestBadge()
            Text(
                stringResource(Res.string.firereq_card_title, thread.number),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            Text(
                stringResource(Res.string.firereq_received_count, thread.received.size, thread.count),
                style = MaterialTheme.typography.labelMedium,
            )
        }
        HeaderLine(Sheet.ORIGIN, read.request.origin)
        HeaderLine(Sheet.RECIPIENT, read.request.recipient)
        HeaderLine(Sheet.REQUEST_GDH, read.request.requestGdh)
        HeaderLine(Sheet.ALLOCATION_GDH, thread.allocation?.gdh.orEmpty())
        HeaderLine(Sheet.END_GDH, thread.end?.gdh.orEmpty())
        if (canSend) ReplyButtons(thread, actions.send)
        MissingParts(thread, read)
        SheetBoxes(read, actions.showOnMap)
    }

@Composable
private fun HeaderLine(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value.ifEmpty { "—" }, style = MaterialTheme.typography.bodyMedium)
    }
}
