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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.firerequest.FireRequestAck
import org.meshtastic.core.model.firerequest.FireRequestThread
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.firereq_ack_incomplete
import org.meshtastic.core.resources.firereq_ack_note
import org.meshtastic.core.resources.firereq_ack_send
import org.meshtastic.core.resources.firereq_ack_sent

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
