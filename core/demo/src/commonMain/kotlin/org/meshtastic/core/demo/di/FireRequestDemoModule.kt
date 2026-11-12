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
import org.meshtastic.core.demo.repository.DemoFireRequestPrefs
import org.meshtastic.core.demo.screen.ScreenFireRequestPrefs
import org.meshtastic.core.demo.store.DemoStore
import org.meshtastic.core.repository.DemoMode
import org.meshtastic.core.repository.FireRequestPrefs
import org.meshtastic.core.repository.SCREEN_DATA

/**
 * The fire request form's preferences in demo mode, following [CoreDemoModule]'s rules: the demo implementation under
 * its own class only, the screens' facade under [SCREEN_DATA].
 */
@Module
class FireRequestDemoModule {
    @Single fun demoFireRequestPrefs(store: DemoStore): DemoFireRequestPrefs = DemoFireRequestPrefs(store)

    @Single
    @Named(SCREEN_DATA)
    fun screenFireRequestPrefs(
        @Provided real: FireRequestPrefs,
        demo: DemoFireRequestPrefs,
        demoMode: DemoMode,
    ): FireRequestPrefs = ScreenFireRequestPrefs(real, demo, demoMode)
}
