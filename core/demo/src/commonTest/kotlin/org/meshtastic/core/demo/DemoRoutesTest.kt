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

import org.meshtastic.core.navigation.ChannelsRoute
import org.meshtastic.core.navigation.ConnectionsRoute
import org.meshtastic.core.navigation.ContactsRoute
import org.meshtastic.core.navigation.MapRoute
import org.meshtastic.core.navigation.NodeDetailRoute
import org.meshtastic.core.navigation.NodesRoute
import org.meshtastic.core.navigation.SettingsRoute
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DemoRoutesTest {

    @Test
    fun theDemoScreensAreAvailable() {
        listOf(
            ContactsRoute.Contacts,
            ContactsRoute.Messages(contactKey = "0^all"),
            ContactsRoute.CoordinateConverter,
            NodesRoute.CommandPost,
            SettingsRoute.Settings(),
            SettingsRoute.Teams,
            SettingsRoute.About,
            SettingsRoute.Acknowledgements,
            SettingsRoute.HelpDocs,
        )
            .forEach { assertTrue(DemoRoutes.isAvailable(it), "$it") }
    }

    @Test
    fun realDataAndRadioConfigurationScreensAreNot() {
        listOf(
            NodesRoute.Nodes,
            NodesRoute.NodeDetail(destNum = 1),
            NodeDetailRoute.PositionLog(destNum = 1),
            MapRoute.Map(),
            ChannelsRoute.Channels,
            ConnectionsRoute.Connections(),
            ContactsRoute.QuickChat,
            ContactsRoute.Share(message = "x"),
            SettingsRoute.LoRa,
            SettingsRoute.ChannelConfig,
            SettingsRoute.Security,
            SettingsRoute.Administration,
            SettingsRoute.FilterSettings,
            SettingsRoute.DebugPanel,
            SettingsRoute.CleanNodeDb,
        )
            .forEach { assertFalse(DemoRoutes.isAvailable(it), "$it") }
    }

    @Test
    fun administeringAnotherNodeIsNot() {
        assertFalse(DemoRoutes.isAvailable(SettingsRoute.Settings(destNum = 42)))
    }
}
