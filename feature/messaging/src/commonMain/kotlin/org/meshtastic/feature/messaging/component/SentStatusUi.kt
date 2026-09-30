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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.getMessageRoutingErrorStringResFrom
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.action_show_message_status
import org.meshtastic.core.resources.tactmsg_reason_duty_cycle
import org.meshtastic.core.resources.tactmsg_reason_encryption
import org.meshtastic.core.resources.tactmsg_reason_nak
import org.meshtastic.core.resources.tactmsg_reason_no_channel
import org.meshtastic.core.resources.tactmsg_reason_no_confirmation
import org.meshtastic.core.resources.tactmsg_reason_no_interface
import org.meshtastic.core.resources.tactmsg_reason_no_route
import org.meshtastic.core.resources.tactmsg_reason_not_accepted
import org.meshtastic.core.resources.tactmsg_reason_rate_limit
import org.meshtastic.core.resources.tactmsg_reason_timeout
import org.meshtastic.core.resources.tactmsg_reason_too_large
import org.meshtastic.core.resources.tactmsg_status_acked_by
import org.meshtastic.core.resources.tactmsg_status_acked_by_verified
import org.meshtastic.core.resources.tactmsg_status_awaiting_radio
import org.meshtastic.core.resources.tactmsg_status_failed
import org.meshtastic.core.resources.tactmsg_status_handed_to_radio
import org.meshtastic.core.resources.tactmsg_status_invalid_proof
import org.meshtastic.core.resources.tactmsg_status_relayed
import org.meshtastic.core.resources.tactmsg_status_unknown
import org.meshtastic.core.ui.icon.Acknowledged
import org.meshtastic.core.ui.icon.CloudUpload
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.MessageEnroute
import org.meshtastic.core.ui.icon.MqttDelivered
import org.meshtastic.core.ui.icon.Warning
import org.meshtastic.feature.messaging.status.SentStatus
import org.meshtastic.proto.Routing

/** Short failure reason for the "Failed: <reason>" label; 0 means the send never got past this device's radio. */
internal fun failureReasonRes(routingError: Int): StringResource = when (routingError) {
    Routing.Error.NONE.value -> Res.string.tactmsg_reason_not_accepted

    Routing.Error.TIMEOUT.value -> Res.string.tactmsg_reason_timeout

    Routing.Error.MAX_RETRANSMIT.value -> Res.string.tactmsg_reason_no_confirmation

    Routing.Error.GOT_NAK.value -> Res.string.tactmsg_reason_nak

    Routing.Error.NO_ROUTE.value -> Res.string.tactmsg_reason_no_route

    Routing.Error.NO_CHANNEL.value -> Res.string.tactmsg_reason_no_channel

    Routing.Error.TOO_LARGE.value -> Res.string.tactmsg_reason_too_large

    Routing.Error.DUTY_CYCLE_LIMIT.value -> Res.string.tactmsg_reason_duty_cycle

    Routing.Error.RATE_LIMIT_EXCEEDED.value -> Res.string.tactmsg_reason_rate_limit

    Routing.Error.NO_INTERFACE.value -> Res.string.tactmsg_reason_no_interface

    Routing.Error.PKI_FAILED.value,
    Routing.Error.PKI_UNKNOWN_PUBKEY.value,
    Routing.Error.PKI_SEND_FAIL_PUBLIC_KEY.value,
    -> Res.string.tactmsg_reason_encryption

    else -> getMessageRoutingErrorStringResFrom(routingError)
}

/** True for the states shown as a high-contrast alert rather than a quiet status line. */
internal fun SentStatus.isAlert(): Boolean = this is SentStatus.Failed || this == SentStatus.InvalidProof

/** The label for [sentStatus]; [recipientName] names the addressed node of a direct message. */
@Composable
internal fun sentStatusText(sentStatus: SentStatus, recipientName: String?): String = when (sentStatus) {
    SentStatus.AwaitingRadio -> stringResource(Res.string.tactmsg_status_awaiting_radio)

    SentStatus.HandedToRadio -> stringResource(Res.string.tactmsg_status_handed_to_radio)

    SentStatus.RelayedByMesh -> stringResource(Res.string.tactmsg_status_relayed)

    is SentStatus.AckedByRecipient -> {
        val name = recipientName.orEmpty().ifBlank { "?" }
        if (sentStatus.verified) {
            stringResource(Res.string.tactmsg_status_acked_by_verified, name)
        } else {
            stringResource(Res.string.tactmsg_status_acked_by, name)
        }
    }

    SentStatus.InvalidProof -> stringResource(Res.string.tactmsg_status_invalid_proof)

    is SentStatus.Failed ->
        stringResource(Res.string.tactmsg_status_failed, stringResource(failureReasonRes(sentStatus.routingError)))

    SentStatus.Unknown -> stringResource(Res.string.tactmsg_status_unknown)
}

private fun SentStatus.icon(): ImageVector = when (this) {
    SentStatus.AwaitingRadio -> MeshtasticIcons.CloudUpload

    SentStatus.HandedToRadio -> MeshtasticIcons.MessageEnroute

    SentStatus.RelayedByMesh -> MeshtasticIcons.MqttDelivered

    is SentStatus.AckedByRecipient -> MeshtasticIcons.Acknowledged

    SentStatus.InvalidProof,
    is SentStatus.Failed,
    SentStatus.Unknown,
    -> MeshtasticIcons.Warning
}

/**
 * The state of a message this device sent: always an icon and a text, never colour alone. Failures and an invalid ack
 * proof use the high-contrast [AlertPill]; every other state is a neutral line.
 */
@Composable
internal fun SentStatusLabel(
    sentStatus: SentStatus,
    recipientName: String?,
    onStatusClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val text = sentStatusText(sentStatus, recipientName)
    val clickModifier =
        modifier
            .testTag(MESSAGE_STATUS_LABEL_TEST_TAG)
            .semantics { this[SentStatusAlertKey] = sentStatus.isAlert() }
            .clickable(
                onClickLabel = stringResource(Res.string.action_show_message_status),
                role = Role.Button,
                onClick = onStatusClick,
            )
    if (sentStatus.isAlert()) {
        AlertPill(text = text, modifier = clickModifier)
    } else {
        Row(
            modifier = clickModifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Icon(
                imageVector = sentStatus.icon(),
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Test hook: true when the status is drawn as an alert. */
internal val SentStatusAlertKey = SemanticsPropertyKey<Boolean>("SentStatusAlert")
