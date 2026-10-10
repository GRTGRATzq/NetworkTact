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
package org.meshtastic.core.repository

/**
 * The phone's last position fix: where, when the fix was taken (epoch seconds, from the location provider) and, when
 * the provider gives one, its estimated horizontal accuracy in metres.
 */
data class PhoneFix(
    val latitude: Double,
    val longitude: Double,
    val fixEpochSeconds: Long,
    val accuracyMeters: Float? = null,
)

/**
 * The phone's own last known position, used by "Ma position" only when the radio has none, and shown beside the radio's
 * on the map to compare them. Implementations read what the system already holds and never start tracking; they return
 * null without permission or without any fix.
 */
fun interface PhonePositionSource {
    suspend fun lastFix(): PhoneFix?
}
