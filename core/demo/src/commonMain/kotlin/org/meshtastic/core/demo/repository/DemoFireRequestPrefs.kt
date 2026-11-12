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
import org.meshtastic.core.model.firerequest.FireRequest
import org.meshtastic.core.model.firerequest.GdhZone
import org.meshtastic.core.repository.FireRequestPrefs

/** [FireRequestPrefs] of the demo: draft, last request and zone in memory only, gone when the demo ends. */
class DemoFireRequestPrefs(private val store: DemoStore) : FireRequestPrefs {
    override val gdhZone: StateFlow<GdhZone> = store.fireRequestZone

    override val draft: StateFlow<FireRequest?> = store.fireRequestDraft

    override val lastSent: StateFlow<FireRequest?> = store.lastFireRequest

    override fun setGdhZone(zone: GdhZone) {
        store.fireRequestZone.value = zone
    }

    override fun saveDraft(request: FireRequest?) {
        store.fireRequestDraft.value = request
    }

    override fun recordSent(request: FireRequest) {
        store.lastFireRequest.value = request
        store.fireRequestDraft.value = null
    }
}
