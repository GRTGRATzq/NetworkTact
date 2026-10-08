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
package org.meshtastic.feature.map

import org.koin.core.annotation.KoinViewModel
import org.koin.core.annotation.Named
import org.meshtastic.core.common.util.LocaleUnitsProvider
import org.meshtastic.core.network.repository.NetworkRepository
import org.meshtastic.core.repository.DemoMode
import org.meshtastic.core.repository.MapPrefs
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.repository.NotificationPrefs
import org.meshtastic.core.repository.PacketRepository
import org.meshtastic.core.repository.RadioConfigRepository
import org.meshtastic.core.repository.RadioController
import org.meshtastic.core.repository.SCREEN_DATA

/**
 * The view model of the shared map screens. Its node, packet and radio-configuration data come through the screens'
 * sources ([SCREEN_DATA]): the real ones, or the demo's fictitious ones while demo mode is on, so the map can be shown
 * in a demo without mixing in real data, and sends nothing then.
 */
@KoinViewModel
class SharedMapViewModel(
    mapPrefs: MapPrefs,
    @Named(SCREEN_DATA) nodeRepository: NodeRepository,
    @Named(SCREEN_DATA) packetRepository: PacketRepository,
    radioController: RadioController,
    @Named(SCREEN_DATA) radioConfigRepository: RadioConfigRepository,
    notificationPrefs: NotificationPrefs,
    localeUnitsProvider: LocaleUnitsProvider,
    networkRepository: NetworkRepository,
    demoMode: DemoMode,
) : BaseMapViewModel(
    mapPrefs,
    nodeRepository,
    packetRepository,
    radioController,
    radioConfigRepository,
    notificationPrefs,
    localeUnitsProvider,
    networkRepository,
    demoMode,
)
