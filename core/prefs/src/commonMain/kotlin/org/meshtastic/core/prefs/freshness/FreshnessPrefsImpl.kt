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
package org.meshtastic.core.prefs.freshness

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import org.meshtastic.core.di.CoroutineDispatchers
import org.meshtastic.core.model.freshness.FreshnessSettings
import org.meshtastic.core.prefs.di.UiDataStore
import org.meshtastic.core.repository.FreshnessPrefs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * [FreshnessPrefs] in the app-wide UI preferences store: durations in seconds. A missing or non-positive duration reads
 * as its default, so a damaged value can never give a zero threshold.
 */
@Single
class FreshnessPrefsImpl(private val dataStore: UiDataStore, dispatchers: CoroutineDispatchers) : FreshnessPrefs {
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.default)

    override val settings: StateFlow<FreshnessSettings> =
        dataStore.data.map { it.settings() }.stateIn(scope, SharingStarted.Eagerly, FreshnessSettings.Default)

    override fun setSettings(settings: FreshnessSettings) {
        scope.launch {
            dataStore.edit { prefs ->
                prefs[KEY_INTERVAL] = settings.expectedInterval.inWholeSeconds
                prefs[KEY_AUTO] = settings.autoScale
                prefs[KEY_FIXED] = settings.fixedThreshold.inWholeSeconds
            }
        }
    }

    companion object {
        val KEY_INTERVAL = longPreferencesKey("freshness-expected-interval-seconds")
        val KEY_AUTO = booleanPreferencesKey("freshness-auto-scale")
        val KEY_FIXED = longPreferencesKey("freshness-fixed-threshold-seconds")

        private fun Preferences.settings(): FreshnessSettings = FreshnessSettings(
            expectedInterval = this[KEY_INTERVAL].positive() ?: FreshnessSettings.DEFAULT_EXPECTED_INTERVAL,
            autoScale = this[KEY_AUTO] ?: true,
            fixedThreshold = this[KEY_FIXED].positive() ?: FreshnessSettings.DEFAULT_FIXED_THRESHOLD,
        )

        private fun Long?.positive(): Duration? = this?.takeIf { it > 0 }?.seconds
    }
}
