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

import org.meshtastic.core.repository.DemoMode
import org.meshtastic.core.repository.PhoneFix
import org.meshtastic.core.repository.PhonePositionSource

/**
 * The screens' [PhonePositionSource]: the phone's real last fix while demo mode is off, the fictitious one while on.
 */
class ScreenPhonePositionSource(
    private val real: PhonePositionSource,
    private val demo: PhonePositionSource,
    demoMode: DemoMode,
) : PhonePositionSource {
    private val switch = ScreenSwitch(demoMode)

    override suspend fun lastFix(): PhoneFix? = switch.pick({ real }, { demo }).lastFix()
}
