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
package org.meshtastic.core.demo.screen

import kotlinx.coroutines.flow.StateFlow
import org.meshtastic.core.model.diagnostic.LoraSetting
import org.meshtastic.core.repository.DemoMode
import org.meshtastic.core.repository.ExpectedLoraPrefs
import org.meshtastic.core.repository.RadioActivityProvider

/** The screens' [ExpectedLoraPrefs]: the demo's setting while demo mode is on, the real one otherwise, never both. */
class ScreenExpectedLoraPrefs(
    private val real: ExpectedLoraPrefs,
    private val demo: ExpectedLoraPrefs,
    demoMode: DemoMode,
) : ExpectedLoraPrefs {
    private val switch = ScreenSwitch(demoMode)

    override val expected: StateFlow<LoraSetting?> = switch.state(real.expected, demo.expected)

    override fun setExpected(setting: LoraSetting?) = switch.pick({ real }, { demo }).setExpected(setting)
}

/** The screens' [RadioActivityProvider]: the demo radio's last packet while demo mode is on, the real one otherwise. */
class ScreenRadioActivityProvider(real: RadioActivityProvider, demo: RadioActivityProvider, demoMode: DemoMode) :
    RadioActivityProvider {
    override val lastPacketFromRadioMillis: StateFlow<Long?> =
        ScreenSwitch(demoMode).state(real.lastPacketFromRadioMillis, demo.lastPacketFromRadioMillis)
}
