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
package org.meshtastic.core.prefs.diagnostic

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import org.meshtastic.core.di.CoroutineDispatchers
import org.meshtastic.core.model.diagnostic.LoraSetting
import org.meshtastic.core.prefs.di.UiDataStore
import org.meshtastic.core.repository.ExpectedLoraPrefs

/**
 * [ExpectedLoraPrefs] in the app-wide UI preferences store: the proto enum names of the region and of the preset, as
 * plain text. An empty preset means custom modem parameters.
 */
@Single
class ExpectedLoraPrefsImpl(private val dataStore: UiDataStore, dispatchers: CoroutineDispatchers) : ExpectedLoraPrefs {
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.default)

    override val expected: StateFlow<LoraSetting?> =
        dataStore.data.map { it.setting() }.stateIn(scope, SharingStarted.Eagerly, null)

    override fun setExpected(setting: LoraSetting?) {
        scope.launch {
            dataStore.edit { prefs ->
                if (setting == null) {
                    prefs.remove(KEY_REGION)
                    prefs.remove(KEY_PRESET)
                } else {
                    prefs[KEY_REGION] = setting.region
                    prefs[KEY_PRESET] = setting.preset.orEmpty()
                }
            }
        }
    }

    companion object {
        val KEY_REGION = stringPreferencesKey("expected-lora-region")
        val KEY_PRESET = stringPreferencesKey("expected-lora-preset")

        private fun Preferences.setting(): LoraSetting? {
            val region = this[KEY_REGION]?.takeIf { it.isNotBlank() } ?: return null
            return LoraSetting(region, this[KEY_PRESET]?.takeIf { it.isNotBlank() })
        }
    }
}
