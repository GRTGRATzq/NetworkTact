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
package org.meshtastic.core.demo.repository

import kotlinx.coroutines.flow.StateFlow
import org.meshtastic.core.demo.store.DemoStore
import org.meshtastic.core.model.diagnostic.LoraSetting
import org.meshtastic.core.repository.ExpectedLoraPrefs

/** [ExpectedLoraPrefs] over the demo's expected setting: changed in memory only, gone when the demo ends. */
class DemoExpectedLoraPrefs(private val store: DemoStore) : ExpectedLoraPrefs {
    override val expected: StateFlow<LoraSetting?> = store.expectedLora

    override fun setExpected(setting: LoraSetting?) {
        store.expectedLora.value = setting
    }
}
