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
package org.meshtastic.core.demo.di

import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import org.meshtastic.core.demo.repository.DemoNodeRepository
import org.meshtastic.core.demo.repository.DemoPacketRepository
import org.meshtastic.core.demo.repository.DemoRadioConfigRepository
import org.meshtastic.core.demo.repository.DemoTeamRosterPrefs
import org.meshtastic.core.demo.screen.ScreenConnectionStateProvider
import org.meshtastic.core.demo.screen.ScreenMessagingController
import org.meshtastic.core.demo.screen.ScreenNodeRepository
import org.meshtastic.core.demo.screen.ScreenPacketRepository
import org.meshtastic.core.demo.screen.ScreenRadioConfigRepository
import org.meshtastic.core.demo.screen.ScreenRadioConfigUseCase
import org.meshtastic.core.demo.screen.ScreenSendMessageUseCase
import org.meshtastic.core.demo.screen.ScreenTeamRosterPrefs
import org.meshtastic.core.demo.screen.ScreenUiPrefs
import org.meshtastic.core.demo.send.DemoMessagingController
import org.meshtastic.core.demo.send.DemoSendMessageUseCase
import org.meshtastic.core.demo.store.DemoStore
import org.meshtastic.core.domain.usecase.settings.RadioConfigUseCase
import org.meshtastic.core.repository.ConnectionStateProvider
import org.meshtastic.core.repository.DemoMode
import org.meshtastic.core.repository.MessagingController
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.repository.PacketRepository
import org.meshtastic.core.repository.RadioConfigRepository
import org.meshtastic.core.repository.RadioController
import org.meshtastic.core.repository.SCREEN_DATA
import org.meshtastic.core.repository.TeamRosterPrefs
import org.meshtastic.core.repository.UiPrefs
import org.meshtastic.core.repository.usecase.SendMessageUseCase

/**
 * The screens' data sources, under [SCREEN_DATA]: each is the real implementation while demo mode is off and the demo
 * one while it is on. Only the view models of the screens available in demo mode take them.
 */
@Module
class ScreenDataModule {
    @Single
    @Named(SCREEN_DATA)
    fun screenNodeRepository(
        @Provided real: NodeRepository,
        demo: DemoNodeRepository,
        demoMode: DemoMode,
    ): NodeRepository = ScreenNodeRepository(real, demo, demoMode)

    @Single
    @Named(SCREEN_DATA)
    fun screenPacketRepository(
        @Provided real: PacketRepository,
        demo: DemoPacketRepository,
        demoMode: DemoMode,
    ): PacketRepository = ScreenPacketRepository(real, demo, demoMode)

    @Single
    @Named(SCREEN_DATA)
    fun screenRadioConfigRepository(
        @Provided real: RadioConfigRepository,
        demo: DemoRadioConfigRepository,
        demoMode: DemoMode,
    ): RadioConfigRepository = ScreenRadioConfigRepository(real, demo, demoMode)

    @Single
    @Named(SCREEN_DATA)
    fun screenTeamRosterPrefs(
        @Provided real: TeamRosterPrefs,
        demo: DemoTeamRosterPrefs,
        demoMode: DemoMode,
    ): TeamRosterPrefs = ScreenTeamRosterPrefs(real, demo, demoMode)

    @Single
    @Named(SCREEN_DATA)
    fun screenSendMessageUseCase(
        @Provided real: SendMessageUseCase,
        demo: DemoSendMessageUseCase,
        demoMode: DemoMode,
    ): SendMessageUseCase = ScreenSendMessageUseCase(real, demo, demoMode)

    @Single
    @Named(SCREEN_DATA)
    fun screenMessagingController(
        @Provided real: MessagingController,
        demo: DemoMessagingController,
        demoMode: DemoMode,
    ): MessagingController = ScreenMessagingController(real, demo, demoMode)

    @Single
    @Named(SCREEN_DATA)
    fun screenConnectionStateProvider(
        @Provided real: ConnectionStateProvider,
        demoMode: DemoMode,
    ): ConnectionStateProvider = ScreenConnectionStateProvider(real, demoMode)

    @Single
    @Named(SCREEN_DATA)
    fun screenUiPrefs(@Provided real: UiPrefs, store: DemoStore, demoMode: DemoMode): UiPrefs =
        ScreenUiPrefs(real, store, demoMode)

    @Single
    @Named(SCREEN_DATA)
    fun screenRadioConfigUseCase(
        @Provided radioController: RadioController,
        store: DemoStore,
        demoMode: DemoMode,
    ): RadioConfigUseCase = ScreenRadioConfigUseCase(radioController, store, demoMode)
}
