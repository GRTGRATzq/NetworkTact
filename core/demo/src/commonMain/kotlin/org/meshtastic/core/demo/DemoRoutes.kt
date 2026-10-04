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
package org.meshtastic.core.demo

import androidx.navigation3.runtime.NavKey
import org.meshtastic.core.navigation.ContactsRoute
import org.meshtastic.core.navigation.NodesRoute
import org.meshtastic.core.navigation.SettingsRoute

/**
 * The screens available in demo mode; every other route shows "unavailable in demo mode". The list is closed: a screen
 * added later is unavailable until it is checked and added here, so none can mix real and demo data by default.
 */
object DemoRoutes {
    fun isAvailable(key: NavKey): Boolean = when (key) {
        // Conversations, a conversation and the coordinate converter (pure computation).
        is ContactsRoute.Contacts,
        is ContactsRoute.Messages,
        is ContactsRoute.CoordinateConverter,
        // The command post view; in demo mode the Nodes tab opens on it, the node list itself is unavailable.
        is NodesRoute.CommandPost,
        // Teams, and the pages holding neither radio nor personal data.
        is SettingsRoute.Teams,
        is SettingsRoute.About,
        is SettingsRoute.Acknowledgements,
        is SettingsRoute.HelpDocs,
        is SettingsRoute.HelpDocPage,
        -> true

        // The settings of this phone only: administering another node is radio configuration.
        is SettingsRoute.Settings -> key.destNum == null

        else -> false
    }
}
