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
package org.meshtastic.core.prefs.orgchart

import androidx.datastore.preferences.core.MutablePreferences
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
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single
import org.meshtastic.core.di.CoroutineDispatchers
import org.meshtastic.core.model.orgchart.OrgChartHistory
import org.meshtastic.core.model.orgchart.OrgChartRecord
import org.meshtastic.core.prefs.di.UiDataStore
import org.meshtastic.core.repository.OrgChartPrefs

/** [OrgChartPrefs] in the app-wide UI preferences store, one JSON value per chart. */
@Single
class OrgChartPrefsImpl(private val dataStore: UiDataStore, dispatchers: CoroutineDispatchers) : OrgChartPrefs {
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.default)

    override val current: StateFlow<OrgChartRecord?> =
        dataStore.data.map { it.record(KEY_CURRENT) }.stateIn(scope, SharingStarted.Eagerly, null)

    override val previous: StateFlow<OrgChartRecord?> =
        dataStore.data.map { it.record(KEY_PREVIOUS) }.stateIn(scope, SharingStarted.Eagerly, null)

    override fun save(record: OrgChartRecord) = change { it.saved(record) }

    override fun adopt(record: OrgChartRecord) = change { it.adopted(record) }

    override fun revertToPrevious() = change { it.reverted() }

    // A read-modify-write inside edit{}, which is transactional: two changes in a row cannot lose one another.
    private fun change(transform: (OrgChartHistory) -> OrgChartHistory) {
        scope.launch {
            dataStore.edit { prefs ->
                val next = transform(OrgChartHistory(prefs.record(KEY_CURRENT), prefs.record(KEY_PREVIOUS)))
                prefs.write(KEY_CURRENT, next.current)
                prefs.write(KEY_PREVIOUS, next.previous)
            }
        }
    }

    companion object {
        val KEY_CURRENT = stringPreferencesKey("org-chart")
        val KEY_PREVIOUS = stringPreferencesKey("org-chart-previous")

        private val json = Json { ignoreUnknownKeys = true }

        // An unreadable value (older or damaged) reads as no chart rather than failing every read of the store.
        private fun Preferences.record(key: Preferences.Key<String>): OrgChartRecord? = this[key]?.let { raw ->
            try {
                json.decodeFromString(OrgChartRecord.serializer(), raw)
            } catch (_: IllegalArgumentException) {
                // SerializationException is an IllegalArgumentException.
                null
            }
        }

        private fun MutablePreferences.write(key: Preferences.Key<String>, record: OrgChartRecord?) {
            if (record == null) {
                remove(key)
            } else {
                this[key] = json.encodeToString(OrgChartRecord.serializer(), record)
            }
        }
    }
}
