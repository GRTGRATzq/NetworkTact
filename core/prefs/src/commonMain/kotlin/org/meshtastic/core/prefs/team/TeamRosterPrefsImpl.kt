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
package org.meshtastic.core.prefs.team

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
import org.meshtastic.core.model.team.TeamRosterRecord
import org.meshtastic.core.prefs.di.UiDataStore
import org.meshtastic.core.repository.TeamRosterPrefs

/** [TeamRosterPrefs] in the app-wide UI preferences store, one JSON value per list. */
@Single
class TeamRosterPrefsImpl(private val dataStore: UiDataStore, dispatchers: CoroutineDispatchers) : TeamRosterPrefs {
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.default)

    override val roster: StateFlow<TeamRosterRecord?> =
        dataStore.data.map { it.record(KEY_ROSTER) }.stateIn(scope, SharingStarted.Eagerly, null)

    override val pendingRoster: StateFlow<TeamRosterRecord?> =
        dataStore.data.map { it.record(KEY_PENDING_ROSTER) }.stateIn(scope, SharingStarted.Eagerly, null)

    // Every change is a read-modify-write inside edit{}, which is transactional: a list received while the user is
    // confirming another cannot be lost or confirmed in its place.
    override fun offerReceived(record: TeamRosterRecord) {
        scope.launch {
            dataStore.edit { prefs ->
                prefs.write(KEY_PENDING_ROSTER, TeamRosterRecord.pendingAfter(prefs.record(KEY_ROSTER), record))
            }
        }
    }

    override fun adopt(record: TeamRosterRecord) {
        scope.launch {
            dataStore.edit { prefs ->
                prefs.write(KEY_ROSTER, record)
                prefs.write(KEY_PENDING_ROSTER, null)
            }
        }
    }

    override fun acceptPending(expected: TeamRosterRecord) {
        scope.launch {
            dataStore.edit { prefs ->
                if (prefs.record(KEY_PENDING_ROSTER) == expected) {
                    prefs.write(KEY_ROSTER, expected)
                    prefs.write(KEY_PENDING_ROSTER, null)
                }
            }
        }
    }

    override fun dismissPending(expected: TeamRosterRecord) {
        scope.launch {
            dataStore.edit { prefs ->
                if (prefs.record(KEY_PENDING_ROSTER) == expected) prefs.write(KEY_PENDING_ROSTER, null)
            }
        }
    }

    companion object {
        val KEY_ROSTER = stringPreferencesKey("team-roster")
        val KEY_PENDING_ROSTER = stringPreferencesKey("team-roster-pending")

        private val json = Json { ignoreUnknownKeys = true }

        // An unreadable value (older or damaged) reads as no list rather than failing every read of the store.
        private fun Preferences.record(key: Preferences.Key<String>): TeamRosterRecord? = this[key]?.let { raw ->
            try {
                json.decodeFromString(TeamRosterRecord.serializer(), raw)
            } catch (_: IllegalArgumentException) {
                // SerializationException is an IllegalArgumentException.
                null
            }
        }

        private fun MutablePreferences.write(key: Preferences.Key<String>, record: TeamRosterRecord?) {
            if (record == null) {
                remove(key)
            } else {
                this[key] = json.encodeToString(TeamRosterRecord.serializer(), record)
            }
        }
    }
}
