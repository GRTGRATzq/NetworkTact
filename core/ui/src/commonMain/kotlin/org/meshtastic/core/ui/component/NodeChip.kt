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
package org.meshtastic.core.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.meshtastic.core.model.Node
import org.meshtastic.core.ui.theme.AppTheme
import org.meshtastic.proto.EnvironmentMetrics
import org.meshtastic.proto.Paxcount
import org.meshtastic.proto.User

/**
 * A radio of the network, drawn in the APP-6(C) friend frame ([FriendlyUnitFrame]) with its short name only — the same
 * frame for every radio, whatever its node colour.
 *
 * @param offline dashes the frame. Defaults to the node list's own online test; pass `false` for this phone's own
 *   radio, whose "last heard" says nothing about whether it is there.
 */
@Composable
fun NodeChip(
    modifier: Modifier = Modifier,
    node: Node,
    onClick: ((Node) -> Unit)? = null,
    offline: Boolean = !node.isOnline,
    selected: Boolean = false,
) {
    FriendlyUnitFrame(
        shortName = node.user.short_name,
        modifier = modifier,
        offline = offline,
        selected = selected,
        struckThrough = node.isIgnored,
        onClick = onClick?.let { click -> { click(node) } },
    )
}

@Suppress("MagicNumber")
@Preview
@Composable
fun NodeChipPreview() {
    val user =
        User.Builder()
            .also { wb ->
                wb.short_name = "JD"
                wb.long_name = "John Doe"
            }
            .build()
    val node =
        Node(
            num = 13444,
            user = user,
            isIgnored = false,
            paxcounter =
            Paxcount.Builder()
                .also { wb ->
                    wb.ble = 10
                    wb.wifi = 5
                }
                .build(),
            environmentMetrics =
            EnvironmentMetrics.Builder()
                .also { wb ->
                    wb.temperature = 25f
                    wb.relative_humidity = 60f
                }
                .build(),
        )
    AppTheme { NodeChip(node = node) }
}
