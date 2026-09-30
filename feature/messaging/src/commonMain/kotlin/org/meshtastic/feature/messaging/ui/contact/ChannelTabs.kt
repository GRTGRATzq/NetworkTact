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
package org.meshtastic.feature.messaging.ui.contact

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.ContactKey
import org.meshtastic.core.model.util.getChannel
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.tactmsg_filter_all_channels
import org.meshtastic.core.resources.tactmsg_filter_channels
import org.meshtastic.proto.ChannelSet

/**
 * Whether the conversation [contactKey] belongs under the channel tab [channelIndex]; null is the "All" tab. A
 * conversation belongs to the channel index its key was stored under, which covers the channel's own conversation and
 * direct messages sent on that slot. A retired conversation has no live slot and only appears under "All".
 */
fun isInChannelTab(contactKey: String, channelIndex: Int?): Boolean =
    channelIndex == null || ContactKey(contactKey).channelOrNull == channelIndex

/** "All" followed by one tab per channel configured on the radio. [selected] is a channel index, or null for "All". */
@Composable
internal fun ChannelTabs(
    channels: ChannelSet,
    selected: Int?,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val channelCount = channels.settings.size
    val selectedTab = selected?.takeIf { it in 0 until channelCount }?.let { it + 1 } ?: 0
    val description = stringResource(Res.string.tactmsg_filter_channels)
    PrimaryScrollableTabRow(
        selectedTabIndex = selectedTab,
        modifier = modifier.fillMaxWidth().semantics { contentDescription = description },
        edgePadding = 8.dp,
    ) {
        Tab(
            selected = selectedTab == 0,
            onClick = { onSelect(null) },
            text = { Text(stringResource(Res.string.tactmsg_filter_all_channels)) },
        )
        repeat(channelCount) { index ->
            Tab(
                selected = selectedTab == index + 1,
                onClick = { onSelect(index) },
                text = { Text(channels.getChannel(index)?.name?.ifBlank { null } ?: index.toString()) },
            )
        }
    }
}
