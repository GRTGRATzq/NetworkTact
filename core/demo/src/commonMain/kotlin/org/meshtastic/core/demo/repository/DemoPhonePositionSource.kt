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

import org.meshtastic.core.common.util.nowSeconds
import org.meshtastic.core.demo.data.DemoDataSet
import org.meshtastic.core.repository.PhoneFix
import org.meshtastic.core.repository.PhonePositionSource

/**
 * The phone's position in demo mode: a fictitious fix near PC-0, always [DemoDataSet.PHONE_FIX_AGE] old, so the map's
 * comparison of the radio and the phone has two positions to show. The real phone's location is never read.
 */
class DemoPhonePositionSource(private val now: () -> Long = { nowSeconds }) : PhonePositionSource {
    override suspend fun lastFix(): PhoneFix = PhoneFix(
        latitude = DemoDataSet.PHONE_POINT.latitude,
        longitude = DemoDataSet.PHONE_POINT.longitude,
        fixEpochSeconds = now() - DemoDataSet.PHONE_FIX_AGE.inWholeSeconds,
        accuracyMeters = DemoDataSet.PHONE_ACCURACY_METERS,
    )
}
