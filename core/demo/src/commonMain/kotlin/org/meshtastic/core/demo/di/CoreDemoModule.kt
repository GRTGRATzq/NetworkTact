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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.koin.core.annotation.Module
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import org.meshtastic.core.demo.DemoModeController
import org.meshtastic.core.demo.repository.DemoNodeRepository
import org.meshtastic.core.demo.repository.DemoPacketRepository
import org.meshtastic.core.demo.repository.DemoPhonePositionSource
import org.meshtastic.core.demo.repository.DemoRadioConfigRepository
import org.meshtastic.core.demo.repository.DemoTeamRosterPrefs
import org.meshtastic.core.demo.send.DemoMessagingController
import org.meshtastic.core.demo.send.DemoSendMessageUseCase
import org.meshtastic.core.demo.store.DemoStore
import org.meshtastic.core.di.CoroutineDispatchers
import org.meshtastic.core.repository.DemoMode
import org.meshtastic.core.repository.PacketRepository
import org.meshtastic.core.repository.SCREEN_DATA
import org.meshtastic.core.repository.UiPrefs

/**
 * Demo mode's bindings. The demo implementations are bound under their own class only, never under the interfaces, so
 * nothing outside this module can get them by accident. The screens' sources are bound under [SCREEN_DATA]; every
 * unqualified binding (the one the radio service uses) stays the real implementation.
 */
@Module(includes = [ScreenDataModule::class])
class CoreDemoModule {
    @Single fun demoStore(): DemoStore = DemoStore()

    @Single
    fun demoMode(
        store: DemoStore,
        @Provided realPackets: PacketRepository,
        @Provided realUiPrefs: UiPrefs,
        @Provided dispatchers: CoroutineDispatchers,
    ): DemoMode = DemoModeController(store, realPackets, realUiPrefs, dispatchers)

    @Single fun demoNodeRepository(store: DemoStore): DemoNodeRepository = DemoNodeRepository(store)

    @Single fun demoPacketRepository(store: DemoStore): DemoPacketRepository = DemoPacketRepository(store)

    @Single
    fun demoRadioConfigRepository(store: DemoStore): DemoRadioConfigRepository = DemoRadioConfigRepository(store)

    @Single fun demoTeamRosterPrefs(store: DemoStore): DemoTeamRosterPrefs = DemoTeamRosterPrefs(store)

    @Single
    fun demoSendMessageUseCase(
        store: DemoStore,
        packets: DemoPacketRepository,
        @Provided dispatchers: CoroutineDispatchers,
    ): DemoSendMessageUseCase =
        DemoSendMessageUseCase(store, packets, CoroutineScope(SupervisorJob() + dispatchers.default))

    @Single fun demoMessagingController(): DemoMessagingController = DemoMessagingController()

    @Single fun demoPhonePositionSource(): DemoPhonePositionSource = DemoPhonePositionSource()
}
