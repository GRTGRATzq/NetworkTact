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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.model.freshness.FreshnessThresholds
import org.meshtastic.core.model.freshness.MessageAge
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.tactmsg_age_days
import org.meshtastic.core.resources.tactmsg_age_hours
import org.meshtastic.core.resources.tactmsg_age_inconsistent
import org.meshtastic.core.resources.tactmsg_age_just_now
import org.meshtastic.core.resources.tactmsg_age_minutes
import org.meshtastic.core.resources.tactmsg_age_unknown
import org.meshtastic.core.resources.tactmsg_received
import org.meshtastic.core.ui.icon.Download
import org.meshtastic.core.ui.icon.MeshtasticIcons
import kotlin.time.Duration

internal const val MESSAGE_RECEIVED_LABEL_TEST_TAG = "message_received_label"

/**
 * The current time, re-read every [period] so the ages a conversation shows keep growing while no packet arrives. The
 * period is the command post view's, so both screens age at the same pace.
 */
@Composable
internal fun rememberCurrentTimeMillis(period: Duration = FreshnessThresholds.Default.refreshInterval): State<Long> =
    produceState(initialValue = nowMillis, period) {
        while (true) {
            delay(period)
            value = nowMillis
        }
    }

@Composable
internal fun messageAgeText(age: MessageAge): String = when (age) {
    MessageAge.Unknown -> stringResource(Res.string.tactmsg_age_unknown)

    MessageAge.JustNow -> stringResource(Res.string.tactmsg_age_just_now)

    is MessageAge.Minutes -> stringResource(Res.string.tactmsg_age_minutes, age.minutes.toString())

    is MessageAge.Hours -> stringResource(Res.string.tactmsg_age_hours, age.hours.toString())

    is MessageAge.Days -> stringResource(Res.string.tactmsg_age_days, age.days.toString())

    is MessageAge.InconsistentTimestamp ->
        stringResource(Res.string.tactmsg_age_inconsistent, age.ahead.inWholeMinutes.toString())
}

/**
 * Status line of a received card: `Received · 2 min ago`. [currentTimeMillis] is read here, inside this small scope, so
 * the clock ticking recomposes this line and not the whole card.
 *
 * @param receivedAtMillis when the message reached this node (mesh time when known), epoch milliseconds.
 */
@Composable
internal fun ReceivedStatusLabel(receivedAtMillis: Long, currentTimeMillis: () -> Long, modifier: Modifier = Modifier) {
    val age = MessageAge.of(timestampMillis = receivedAtMillis, nowMillis = currentTimeMillis())
    Row(
        modifier = modifier.testTag(MESSAGE_RECEIVED_LABEL_TEST_TAG),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            imageVector = MeshtasticIcons.Download,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(Res.string.tactmsg_received, messageAgeText(age)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
