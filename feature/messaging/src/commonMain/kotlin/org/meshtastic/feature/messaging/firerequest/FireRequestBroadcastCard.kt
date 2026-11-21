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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.firerequest.FireRequestThread
import org.meshtastic.core.model.firerequest.ReplyKind
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.firereq_ack_line
import org.meshtastic.core.resources.firereq_ack_none
import org.meshtastic.core.resources.firereq_broadcast_on
import org.meshtastic.core.resources.firereq_card_sent
import org.meshtastic.core.resources.firereq_resend_part
import org.meshtastic.core.resources.firereq_resend_sheet

/**
 * The writer's card of a request broadcast on a channel: « diffusée sur <canal> » rather than a state per part (nobody
 * acknowledges a broadcast), « Renvoyer la fiche » or one part on the channel, one line « Accusé DT : <nom court> <GDH>
 * » per post, and the date-time groups sent back.
 */
@Composable
internal fun BroadcastRequest(
    thread: FireRequestThread,
    view: FireRequestView,
    channelName: String,
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
    Text(stringResource(Res.string.firereq_broadcast_on, channelName), style = MaterialTheme.typography.bodySmall)
    val acknowledgements = thread.acknowledgements
    if (acknowledgements.isEmpty()) {
        Help(stringResource(Res.string.firereq_ack_none))
    }
    acknowledgements.forEach { (message, ack) ->
        val name = view.messages[message.id]?.node?.user?.short_name?.ifBlank { null } ?: message.sender
        Text(stringResource(Res.string.firereq_ack_line, name, ack.gdh), style = MaterialTheme.typography.bodySmall)
    }
    ReplyState(ReplyKind.ALLOCATION, thread.allocation)
    ReplyState(ReplyKind.END, thread.end)
    if (canSend) {
        val texts = partTexts(thread)
        TextButton(onClick = { actions.sendParts(texts.map { it.second }, actions.contactKey) }) {
            Text(stringResource(Res.string.firereq_resend_sheet))
        }
        if (texts.size > 1) {
            Choices {
                texts.forEach { (k, text) ->
                    TextButton(onClick = { actions.sendParts(listOf(text), actions.contactKey) }) {
                        Text(stringResource(Res.string.firereq_resend_part, k))
                    }
                }
            }
        }
    }
}

/** The text of each part of [thread], in order, as it was first sent. */
internal fun partTexts(thread: FireRequestThread): List<Pair<Int, String>> = thread.parts.entries
    .sortedBy { it.key }
    .mapNotNull { (k, messages) -> messages.firstOrNull()?.let { k to it.text } }
