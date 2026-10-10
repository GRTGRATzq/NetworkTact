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
package org.meshtastic.feature.map.maplibre.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.util.DateFormatter
import org.meshtastic.core.common.util.NumberFormatter
import org.meshtastic.core.model.geo.BroadcastCheck
import org.meshtastic.core.model.geo.BroadcastRefusal
import org.meshtastic.core.model.geo.RadioFixQuality
import org.meshtastic.core.repository.RecenterTarget
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.close
import org.meshtastic.core.resources.recenter_broadcast
import org.meshtastic.core.resources.recenter_broadcast_wait
import org.meshtastic.core.resources.recenter_centre_on
import org.meshtastic.core.resources.recenter_centred_on_phone_instead
import org.meshtastic.core.resources.recenter_centred_on_radio_instead
import org.meshtastic.core.resources.recenter_channel_precision
import org.meshtastic.core.resources.recenter_explanation
import org.meshtastic.core.resources.recenter_gap
import org.meshtastic.core.resources.recenter_hdop
import org.meshtastic.core.resources.recenter_no_phone_position
import org.meshtastic.core.resources.recenter_no_radio_position
import org.meshtastic.core.resources.recenter_pdop
import org.meshtastic.core.resources.recenter_phone_accuracy
import org.meshtastic.core.resources.recenter_phone_accuracy_unknown
import org.meshtastic.core.resources.recenter_phone_source
import org.meshtastic.core.resources.recenter_phone_title
import org.meshtastic.core.resources.recenter_quality_not_sent
import org.meshtastic.core.resources.recenter_radio_source
import org.meshtastic.core.resources.recenter_radio_title
import org.meshtastic.core.resources.recenter_refused_demo
import org.meshtastic.core.resources.recenter_refused_no_position
import org.meshtastic.core.resources.recenter_refused_not_connected
import org.meshtastic.core.resources.recenter_refused_time_inconsistent
import org.meshtastic.core.resources.recenter_refused_time_unknown
import org.meshtastic.core.resources.recenter_satellites
import org.meshtastic.core.resources.recenter_send_failed
import org.meshtastic.core.resources.recenter_sent
import org.meshtastic.core.resources.recenter_target_phone
import org.meshtastic.core.resources.recenter_target_radio
import org.meshtastic.core.resources.recenter_title
import org.meshtastic.core.ui.icon.Close
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.feature.map.recenter.RecenterSide
import org.meshtastic.feature.map.recenter.RecenterState
import kotlin.time.Instant

/**
 * The "Recaler" card: where the map is centred and on what, the radio's position (the one the network receives) beside
 * the phone's (the blue dot), each with its fix time and accuracy, the gap between them, and the one manual broadcast.
 * It says where each position comes from, and that broadcasting does not make the GPS more accurate.
 */
@Composable
internal fun RecenterCard(
    state: RecenterState,
    nowEpochSeconds: Long,
    onTargetChange: (RecenterTarget) -> Unit,
    onBroadcast: () -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 3.dp,
        modifier = Modifier.widthIn(max = CARD_MAX_WIDTH.dp),
    ) {
        Column(
            modifier =
            Modifier.heightIn(max = CARD_MAX_HEIGHT.dp)
                .verticalScroll(rememberScrollState())
                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(Res.string.recenter_title),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(MeshtasticIcons.Close, contentDescription = stringResource(Res.string.close))
                }
            }
            TargetChoice(state, onTargetChange)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    SideHeader(Res.string.recenter_radio_title, Res.string.recenter_radio_source)
                    SideDetails(state.radio, nowEpochSeconds, Res.string.recenter_no_radio_position) {
                        RadioQualityLines(state.radioQuality)
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    SideHeader(Res.string.recenter_phone_title, Res.string.recenter_phone_source)
                    SideDetails(state.phone, nowEpochSeconds, Res.string.recenter_no_phone_position) {
                        SmallText(phoneAccuracyText(state.phoneAccuracyMeters))
                    }
                }
            }
            state.gapMeters?.let { gap ->
                Text(
                    stringResource(Res.string.recenter_gap, gap.toString()),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            SmallText(stringResource(Res.string.recenter_explanation))
            BroadcastSection(state, onBroadcast)
        }
    }
}

@Composable
private fun TargetChoice(state: RecenterState, onTargetChange: (RecenterTarget) -> Unit) {
    Text(stringResource(Res.string.recenter_centre_on), style = MaterialTheme.typography.labelLarge)
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        TARGETS.forEachIndexed { index, (target, label) ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index, TARGETS.size),
                onClick = { onTargetChange(target) },
                selected = state.target == target,
                label = { Text(stringResource(label)) },
            )
        }
    }
    if (state.centredOnOther) {
        val note =
            when (state.target) {
                RecenterTarget.RADIO -> Res.string.recenter_centred_on_phone_instead
                RecenterTarget.PHONE -> Res.string.recenter_centred_on_radio_instead
            }
        SmallText(stringResource(note))
    }
}

@Composable
private fun SideHeader(title: StringResource, source: StringResource) {
    Text(stringResource(title), style = MaterialTheme.typography.labelLarge)
    SmallText(stringResource(source))
}

/** How old the position is in the command post view's words, when it was taken, and [accuracy]. */
@Composable
private fun SideDetails(
    side: RecenterSide?,
    nowEpochSeconds: Long,
    missing: StringResource,
    accuracy: @Composable () -> Unit,
) {
    if (side == null) {
        SmallText(stringResource(missing))
        return
    }
    PositionAge(side.state)
    SmallText(fixTimeText(side.fixEpochSeconds, sameDay(side.fixEpochSeconds, nowEpochSeconds)))
    accuracy()
}

@Composable
private fun RadioQualityLines(quality: RadioFixQuality?) {
    val parts =
        listOfNotNull(
            quality?.satellites?.let { stringResource(Res.string.recenter_satellites, it.toString()) },
            quality?.hdop?.let { stringResource(Res.string.recenter_hdop, NumberFormatter.format(it, 1)) },
            quality?.pdop?.let { stringResource(Res.string.recenter_pdop, NumberFormatter.format(it, 1)) },
        )
    SmallText(if (parts.isEmpty()) stringResource(Res.string.recenter_quality_not_sent) else parts.joinToString(" · "))
    quality?.channelPrecisionMeters?.let { radius ->
        SmallText(stringResource(Res.string.recenter_channel_precision, NumberFormatter.format(radius, 0)))
    }
}

@Composable
private fun phoneAccuracyText(accuracyMeters: Float?): String =
    accuracyMeters?.let { stringResource(Res.string.recenter_phone_accuracy, NumberFormatter.format(it, 0)) }
        ?: stringResource(Res.string.recenter_phone_accuracy_unknown)

/** The broadcast button, why it is refused or how long to wait, and the outcome of the last broadcast. */
@Composable
private fun BroadcastSection(state: RecenterState, onBroadcast: () -> Unit) {
    val check = state.broadcast
    Button(onClick = onBroadcast, enabled = check is BroadcastCheck.Ready, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(Res.string.recenter_broadcast))
    }
    when (check) {
        is BroadcastCheck.Refused -> SmallText(stringResource(check.reason.text))

        is BroadcastCheck.CoolingDown ->
            SmallText(stringResource(Res.string.recenter_broadcast_wait, check.remainingSeconds.toString()))

        is BroadcastCheck.Ready -> Unit
    }
    if (state.sendFailed) {
        SmallText(stringResource(Res.string.recenter_send_failed))
    } else {
        state.lastSent?.let { sent ->
            Text(
                stringResource(
                    Res.string.recenter_sent,
                    DateFormatter.formatTime(sent.sentEpochSeconds * MILLIS_PER_SECOND),
                    DateFormatter.formatTime(sent.fixEpochSeconds * MILLIS_PER_SECOND),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private val BroadcastRefusal.text: StringResource
    get() =
        when (this) {
            BroadcastRefusal.DEMO -> Res.string.recenter_refused_demo
            BroadcastRefusal.NOT_CONNECTED -> Res.string.recenter_refused_not_connected
            BroadcastRefusal.NO_RADIO_POSITION -> Res.string.recenter_refused_no_position
            BroadcastRefusal.FIX_TIME_UNKNOWN -> Res.string.recenter_refused_time_unknown
            BroadcastRefusal.FIX_TIME_INCONSISTENT -> Res.string.recenter_refused_time_inconsistent
        }

@Composable
private fun SmallText(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

private fun sameDay(fixEpochSeconds: Long?, nowEpochSeconds: Long): Boolean {
    fixEpochSeconds ?: return false
    val zone = TimeZone.currentSystemDefault()
    return Instant.fromEpochSeconds(fixEpochSeconds).toLocalDateTime(zone).date ==
        Instant.fromEpochSeconds(nowEpochSeconds).toLocalDateTime(zone).date
}

private val TARGETS =
    listOf(
        RecenterTarget.RADIO to Res.string.recenter_target_radio,
        RecenterTarget.PHONE to Res.string.recenter_target_phone,
    )

private const val CARD_MAX_WIDTH = 520
private const val CARD_MAX_HEIGHT = 440
private const val MILLIS_PER_SECOND = 1000L
