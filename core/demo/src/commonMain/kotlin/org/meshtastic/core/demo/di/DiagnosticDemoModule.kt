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
import org.meshtastic.core.demo.repository.DemoExpectedLoraPrefs
import org.meshtastic.core.demo.repository.DemoRadioActivityProvider
import org.meshtastic.core.demo.screen.ScreenExpectedLoraPrefs
import org.meshtastic.core.demo.screen.ScreenRadioActivityProvider
import org.meshtastic.core.demo.store.DemoStore
import org.meshtastic.core.repository.DemoMode
import org.meshtastic.core.repository.ExpectedLoraPrefs
import org.meshtastic.core.repository.RadioActivityProvider
import org.meshtastic.core.repository.SCREEN_DATA

/**
 * The link diagnostic's sources in demo mode, following [CoreDemoModule]'s rules: the demo implementations under their
 * own class only, the screens' facades under [SCREEN_DATA].
 */
@Module
class DiagnosticDemoModule {
    @Single fun demoExpectedLoraPrefs(store: DemoStore): DemoExpectedLoraPrefs = DemoExpectedLoraPrefs(store)

    @Single
    fun demoRadioActivityProvider(store: DemoStore): DemoRadioActivityProvider = DemoRadioActivityProvider(store)

    @Single
    @Named(SCREEN_DATA)
    fun screenExpectedLoraPrefs(
        @Provided real: ExpectedLoraPrefs,
        demo: DemoExpectedLoraPrefs,
        demoMode: DemoMode,
    ): ExpectedLoraPrefs = ScreenExpectedLoraPrefs(real, demo, demoMode)

    @Single
    @Named(SCREEN_DATA)
    fun screenRadioActivityProvider(
        @Provided real: RadioActivityProvider,
        demo: DemoRadioActivityProvider,
        demoMode: DemoMode,
    ): RadioActivityProvider = ScreenRadioActivityProvider(real, demo, demoMode)
}
