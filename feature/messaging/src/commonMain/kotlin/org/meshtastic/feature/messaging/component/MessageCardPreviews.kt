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
package org.meshtastic.feature.messaging.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import org.meshtastic.core.model.Message
import org.meshtastic.core.model.MessageStatus
import org.meshtastic.core.model.Node
import org.meshtastic.core.ui.component.preview.NodePreviewParameterProvider
import org.meshtastic.core.ui.theme.AppTheme
import org.meshtastic.proto.Routing

// Fictitious identities only. A fixed clock keeps every age the same from one render to the next.
private const val PREVIEW_RECEIVED_AT = 1_790_000_000_000L
private const val PREVIEW_NOW = PREVIEW_RECEIVED_AT + 2 * 60 * 1000L

private fun previewNode(base: Node, longName: String, shortName: String): Node = base.copy(
    user =
    base.user
        .newBuilder()
        .also {
            it.long_name = longName
            it.short_name = shortName
        }
        .build(),
)

private fun previewMessage(
    uuid: Long,
    text: String,
    node: Node,
    fromLocal: Boolean,
    status: MessageStatus,
    routingError: Int = 0,
) = Message(
    text = text,
    time = "14:32",
    fromLocal = fromLocal,
    status = status,
    snr = 6.5f,
    rssi = -95,
    hopsAway = if (fromLocal) 0 else 1,
    uuid = uuid,
    receivedTime = PREVIEW_RECEIVED_AT,
    node = node,
    read = true,
    routingError = routingError,
    packetId = uuid.toInt(),
    emojis = listOf(),
    replyId = null,
    viaMqtt = false,
)

@Composable
private fun PreviewThread(content: @Composable () -> Unit) {
    AppTheme {
        Column(
            modifier = Modifier.fillMaxWidth().background(
                MaterialTheme.colorScheme.background,
            ).padding(vertical = 8.dp),
        ) {
            content()
        }
    }
}

/** Received cards on a channel: information, report, urgent, a [POS] position and an observed fact. */
@PreviewLightDark
@Composable
private fun ReceivedCardsPreview() {
    val nodes = NodePreviewParameterProvider()
    val alpha1 = previewNode(nodes.minnieMouse, "ALPHA-1 [Alpha]", "A1")
    val charlie3 = previewNode(nodes.minnieMouse, "CHARLIE-3 [Alpha]", "C3")
    val pc0 = previewNode(nodes.mickeyMouse, "PC-0", "PC0")
    val received =
        listOf(
            previewMessage(1L, "Point de regroupement atteint", alpha1, false, MessageStatus.RECEIVED),
            previewMessage(2L, "[CR] Secteur nord reconnu, RAS", charlie3, false, MessageStatus.RECEIVED),
            previewMessage(3L, "[URG] Blessé léger, demande appui", alpha1, false, MessageStatus.RECEIVED),
            previewMessage(
                4L,
                "[POS] ALPHA-1 · MGRS 31U DQ 48251 11932 · relevée 14:30",
                alpha1,
                false,
                MessageStatus.RECEIVED,
            ),
            previewMessage(
                5L,
                "[CR] FAIT OBSERVÉ · Lieu DMS 48°51'24\"N 2°21'03\"E = MGRS 31U DQ 52357 12345 · " + "véhicule arrêté",
                charlie3,
                false,
                MessageStatus.RECEIVED,
            ),
        )
    PreviewThread {
        received.forEach { message ->
            MessageItem(
                message = message,
                node = message.node,
                ourNode = pc0,
                selected = false,
                conversationName = "Général",
                currentTimeMillis = { PREVIEW_NOW },
            )
        }
    }
}

/** Sent cards in every state: on a channel (never "acknowledged"), then direct to BRAVO-2. */
@PreviewLightDark
@Composable
private fun SentCardsPreview() {
    val pc0 = previewNode(NodePreviewParameterProvider().mickeyMouse, "PC-0", "PC0")
    val onChannel =
        listOf(
            previewMessage(11L, "Point de situation à 15:00", pc0, true, MessageStatus.QUEUED),
            previewMessage(12L, "[CR] Itinéraire B dégagé", pc0, true, MessageStatus.ENROUTE),
            previewMessage(13L, "[URG] Regroupement immédiat au PC", pc0, true, MessageStatus.DELIVERED),
        )
    val direct =
        listOf(
            previewMessage(14L, "Reçu, je transmets", pc0, true, MessageStatus.RECEIVED),
            previewMessage(
                15L,
                "Confirmez votre position",
                pc0,
                true,
                MessageStatus.ERROR,
                routingError = Routing.Error.MAX_RETRANSMIT.value,
            ),
        )
    PreviewThread {
        onChannel.forEach { message ->
            MessageItem(
                message = message,
                node = pc0,
                ourNode = pc0,
                selected = false,
                isDirectMessage = false,
                conversationName = "Général",
            )
        }
        direct.forEach { message ->
            MessageItem(
                message = message,
                node = pc0,
                ourNode = pc0,
                selected = false,
                isDirectMessage = true,
                recipientName = "BRAVO-2",
            )
        }
    }
}
