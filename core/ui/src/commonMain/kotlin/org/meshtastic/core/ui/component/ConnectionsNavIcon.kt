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

import androidx.compose.animation.Crossfade
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.meshtastic.core.model.ConnectionState
import org.meshtastic.core.model.DeviceType
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.bluetooth
import org.meshtastic.core.resources.ic_tactnav_radio_connected
import org.meshtastic.core.resources.ic_tactnav_radio_connecting
import org.meshtastic.core.resources.ic_tactnav_radio_disconnected
import org.meshtastic.core.resources.ic_tactnav_radio_sleeping
import org.meshtastic.core.resources.network
import org.meshtastic.core.resources.tactnav_radio_connected
import org.meshtastic.core.resources.tactnav_radio_connected_via
import org.meshtastic.core.resources.tactnav_radio_connecting
import org.meshtastic.core.resources.tactnav_radio_disconnected
import org.meshtastic.core.resources.tactnav_radio_sleeping
import org.meshtastic.core.resources.usb
import org.meshtastic.core.ui.theme.TactColors

/**
 * The state of my radio, drawn with the NetworkTact radio icons. The shape carries the state: two waves each side
 * (connected), one wave (connecting), a crescent moon (asleep), struck through with no wave (disconnected). Only a
 * connected radio is coloured, in the NetworkTact green; the other states keep the surrounding content colour. A lost
 * link also gets an alert badge in inverted colours, never red or amber, which stay reserved for message priorities.
 *
 * [contentDescription] defaults to the state, with the link type when it is known.
 */
@Composable
fun ConnectionsNavIcon(
    modifier: Modifier = Modifier,
    connectionState: ConnectionState,
    deviceType: DeviceType?,
    contentDescription: String? = null,
) {
    val description = contentDescription ?: radioStateDescription(connectionState, deviceType)
    val tint =
        if (connectionState == ConnectionState.Connected) {
            TactColors.navigationActive(colorScheme.secondaryContainer)
        } else {
            LocalContentColor.current
        }

    BadgedBox(modifier = modifier, badge = { if (connectionState == ConnectionState.Disconnected) RadioLostBadge() }) {
        Crossfade(targetState = radioIcon(connectionState), label = "ConnectionIcon") { icon ->
            Icon(imageVector = vectorResource(icon), contentDescription = description, tint = tint)
        }
    }
}

/** Alert badge of a lost link, in the app's alert style: inverted colours, no red, no amber. */
@Composable
private fun RadioLostBadge() {
    Badge(containerColor = colorScheme.inverseSurface, contentColor = colorScheme.inverseOnSurface) {
        Text(text = "!", fontWeight = FontWeight.Bold)
    }
}

private fun radioIcon(connectionState: ConnectionState): DrawableResource = when (connectionState) {
    ConnectionState.Connected -> Res.drawable.ic_tactnav_radio_connected
    ConnectionState.Connecting -> Res.drawable.ic_tactnav_radio_connecting
    ConnectionState.DeviceSleep -> Res.drawable.ic_tactnav_radio_sleeping
    ConnectionState.Disconnected -> Res.drawable.ic_tactnav_radio_disconnected
}

@Composable
private fun radioStateDescription(connectionState: ConnectionState, deviceType: DeviceType?): String =
    when (connectionState) {
        ConnectionState.Connected ->
            linkName(deviceType)?.let { stringResource(Res.string.tactnav_radio_connected_via, stringResource(it)) }
                ?: stringResource(Res.string.tactnav_radio_connected)

        ConnectionState.Connecting -> stringResource(Res.string.tactnav_radio_connecting)

        ConnectionState.DeviceSleep -> stringResource(Res.string.tactnav_radio_sleeping)

        ConnectionState.Disconnected -> stringResource(Res.string.tactnav_radio_disconnected)
    }

private fun linkName(deviceType: DeviceType?): StringResource? = when (deviceType) {
    DeviceType.BLE -> Res.string.bluetooth
    DeviceType.TCP -> Res.string.network
    DeviceType.USB -> Res.string.usb
    null -> null
}
