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
package org.meshtastic.feature.messaging.coordinates

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import org.koin.core.annotation.Single
import org.meshtastic.core.common.hasLocationPermission

/**
 * The most recent of the fixes Android already holds (GPS, network, fused). No location request is started, so nothing
 * runs in the background and the fix time is the provider's own: an old fix stays old and the message says so.
 */
@Single
class AndroidPhonePositionSource(private val context: Context) : PhonePositionSource {

    // The permission is checked just before; a revoked one in between is caught as SecurityException.
    @SuppressLint("MissingPermission")
    override suspend fun lastFix(): PhoneFix? {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (locationManager == null || !context.hasLocationPermission()) return null
        val latest =
            try {
                locationManager.allProviders
                    .mapNotNull { provider -> locationManager.getLastKnownLocation(provider) }
                    .maxByOrNull(Location::getTime)
            } catch (_: SecurityException) {
                null
            }
        return latest?.let { PhoneFix(it.latitude, it.longitude, it.time / MILLIS_PER_SECOND) }
    }

    private companion object {
        const val MILLIS_PER_SECOND = 1000L
    }
}
