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
package org.meshtastic.core.ui.navigation

import org.jetbrains.compose.resources.DrawableResource
import org.meshtastic.core.navigation.TopLevelDestination
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.ic_tactnav_map
import org.meshtastic.core.resources.ic_tactnav_messages
import org.meshtastic.core.resources.ic_tactnav_network
import org.meshtastic.core.resources.ic_tactnav_radio_connected
import org.meshtastic.core.resources.ic_tactnav_settings

/**
 * Maps a shared [TopLevelDestination] to its NetworkTact icon [DrawableResource]. The radio tab shows its live state
 * through [org.meshtastic.core.ui.component.ConnectionsNavIcon]; this static icon only serves where no state applies
 * (the demo radio).
 */
val TopLevelDestination.icon: DrawableResource
    get() =
        when (this) {
            TopLevelDestination.Messages -> Res.drawable.ic_tactnav_messages
            TopLevelDestination.Nodes -> Res.drawable.ic_tactnav_network
            TopLevelDestination.Map -> Res.drawable.ic_tactnav_map
            TopLevelDestination.Settings -> Res.drawable.ic_tactnav_settings
            TopLevelDestination.Connect -> Res.drawable.ic_tactnav_radio_connected
        }
