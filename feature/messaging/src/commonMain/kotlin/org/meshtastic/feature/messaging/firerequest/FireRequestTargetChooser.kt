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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.firereq_preview_subtitle
import org.meshtastic.core.resources.firereq_preview_subtitle_channel
import org.meshtastic.core.resources.firereq_recipient_choose
import org.meshtastic.core.resources.firereq_target_both
import org.meshtastic.core.resources.firereq_target_both_warning
import org.meshtastic.core.resources.firereq_target_channel_note
import org.meshtastic.core.resources.firereq_target_no_fire_team
import org.meshtastic.core.resources.firereq_target_radio_note
import org.meshtastic.core.resources.firereq_target_title

/**
 * « Envoyer à » in the preview: the fire team's channel (when the radio has one), the primary channel, both (with the
 * warning that the traffic doubles), or one radio from a menu, in direct messages as in v1.
 */
@Composable
internal fun TargetChooser(
    target: SendTarget?,
    channels: SendChannels?,
    radios: List<RecipientOption>,
    onChoose: (SendTarget) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(Res.string.firereq_target_title), style = MaterialTheme.typography.labelLarge)
            Choices {
                channels?.fireTeam?.let { channel -> ChannelChoice(channel, target, onChoose) }
                channels?.primary?.let { channel -> ChannelChoice(channel, target, onChoose) }
                channels?.both?.let { both ->
                    Choice(stringResource(Res.string.firereq_target_both), target == both) { onChoose(both) }
                }
                RadioChoice(target as? SendTarget.ToRadio, radios, onChoose)
            }
            when {
                target is SendTarget.ToRadio -> Help(stringResource(Res.string.firereq_target_radio_note))

                target is SendTarget.OnChannels && target.channels.size > 1 ->
                    Help(stringResource(Res.string.firereq_target_both_warning), isError = true)

                target is SendTarget.OnChannels -> Help(stringResource(Res.string.firereq_target_channel_note))
            }
            if (channels != null && channels.fireTeam == null) {
                Help(stringResource(Res.string.firereq_target_no_fire_team))
            }
        }
    }
}

@Composable
private fun ChannelChoice(channel: ChannelOption, target: SendTarget?, onChoose: (SendTarget) -> Unit) {
    val only = SendTarget.OnChannels(listOf(channel))
    Choice(channel.name, target == only) { onChoose(only) }
}

/** « Une radio »: the radio chosen, or the menu of the radios of the network to choose one. */
@Composable
private fun RadioChoice(chosen: SendTarget.ToRadio?, radios: List<RecipientOption>, onChoose: (SendTarget) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Choice(chosen?.radio?.name ?: stringResource(Res.string.firereq_recipient_choose), chosen != null) {
            open = true
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            radios.forEach { radio ->
                DropdownMenuItem(
                    text = { Text(radio.name) },
                    onClick = {
                        open = false
                        onChoose(SendTarget.ToRadio(radio))
                    },
                )
            }
        }
    }
}

/** The subtitle of the preview: where the request goes, and how. */
@Composable
internal fun targetSubtitle(target: SendTarget?, fallback: String): String = when (target) {
    is SendTarget.ToRadio -> stringResource(Res.string.firereq_preview_subtitle, target.radio.name)

    is SendTarget.OnChannels ->
        stringResource(Res.string.firereq_preview_subtitle_channel, target.channels.joinToString(" + ") { it.name })

    null -> fallback
}
