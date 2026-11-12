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
package org.meshtastic.core.prefs.firerequest

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
import org.meshtastic.core.model.firerequest.FireRequest
import org.meshtastic.core.model.firerequest.GdhZone
import org.meshtastic.core.prefs.di.UiDataStore
import org.meshtastic.core.repository.FireRequestPrefs

/** [FireRequestPrefs] in the app-wide UI preferences store: the zone by name, each request as one JSON value. */
@Single
class FireRequestPrefsImpl(private val dataStore: UiDataStore, dispatchers: CoroutineDispatchers) : FireRequestPrefs {
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.default)

    override val gdhZone: StateFlow<GdhZone> =
        dataStore.data
            .map { prefs -> GdhZone.entries.firstOrNull { it.name == prefs[KEY_ZONE] } ?: GdhZone.LOCAL }
            .stateIn(scope, SharingStarted.Eagerly, GdhZone.LOCAL)

    override val draft: StateFlow<FireRequest?> =
        dataStore.data.map { it.request(KEY_DRAFT) }.stateIn(scope, SharingStarted.Eagerly, null)

    override val lastSent: StateFlow<FireRequest?> =
        dataStore.data.map { it.request(KEY_LAST_SENT) }.stateIn(scope, SharingStarted.Eagerly, null)

    override fun setGdhZone(zone: GdhZone) = change { it[KEY_ZONE] = zone.name }

    override fun saveDraft(request: FireRequest?) = change { it.write(KEY_DRAFT, request) }

    // One transaction: the request sent and the draft thrown away together.
    override fun recordSent(request: FireRequest) = change {
        it.write(KEY_LAST_SENT, request)
        it.write(KEY_DRAFT, null)
    }

    private fun change(transform: (MutablePreferences) -> Unit) {
        scope.launch { dataStore.edit(transform) }
    }

    companion object {
        val KEY_ZONE = stringPreferencesKey("fire-request-gdh-zone")
        val KEY_DRAFT = stringPreferencesKey("fire-request-draft")
        val KEY_LAST_SENT = stringPreferencesKey("fire-request-last-sent")

        private val json = Json { ignoreUnknownKeys = true }

        // An unreadable value (older or damaged) reads as no request rather than failing every read of the store.
        private fun Preferences.request(key: Preferences.Key<String>): FireRequest? = this[key]?.let { raw ->
            try {
                json.decodeFromString(FireRequest.serializer(), raw)
            } catch (_: IllegalArgumentException) {
                // SerializationException is an IllegalArgumentException.
                null
            }
        }

        private fun MutablePreferences.write(key: Preferences.Key<String>, request: FireRequest?) {
            if (request == null) {
                remove(key)
            } else {
                this[key] = json.encodeToString(FireRequest.serializer(), request)
            }
        }
    }
}
