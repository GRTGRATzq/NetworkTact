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
package org.meshtastic.core.model.geo

import org.meshtastic.core.common.util.latLongToMeter
import org.meshtastic.core.model.util.precisionRadiusMetersOrNull
import kotlin.math.roundToInt

/**
 * The two positions of this user, side by side on the map: the radio's, the one the network receives, from the radio's
 * GPS; and the phone's, the blue dot, from the phone's own GPS. Neither is corrected by the other, and broadcasting
 * does not make either more accurate: accuracy depends on the satellites each GPS receives.
 */
object PositionComparison {
    /** Distance between the radio's and the phone's positions, in whole metres. */
    fun gapMeters(radio: LatLon, phone: LatLon): Int =
        latLongToMeter(radio.latitude, radio.longitude, phone.latitude, phone.longitude).roundToInt()
}

/**
 * What the radio says about the quality of its fix, each part null when the radio did not send it: satellites in view,
 * and HDOP and PDOP, which the radio sends in hundredths. Dilution of precision has no unit: 1 is ideal, under 2 good,
 * past 5 poor. [channelPrecisionMeters] is set when the channel coarsens positions, so the network only receives the
 * position to within that radius.
 */
data class RadioFixQuality(
    val satellites: Int?,
    val hdop: Double?,
    val pdop: Double?,
    val channelPrecisionMeters: Double?,
) {
    val isEmpty: Boolean
        get() = satellites == null && hdop == null && pdop == null && channelPrecisionMeters == null

    companion object {
        private const val DOP_SCALE = 100.0

        /** From the radio's position fields, where 0 means "not sent". */
        fun of(satsInView: Int, hdopHundredths: Int, pdopHundredths: Int, precisionBits: Int): RadioFixQuality =
            RadioFixQuality(
                satellites = satsInView.takeIf { it > 0 },
                hdop = hdopHundredths.takeIf { it > 0 }?.let { it / DOP_SCALE },
                pdop = pdopHundredths.takeIf { it > 0 }?.let { it / DOP_SCALE },
                channelPrecisionMeters = precisionRadiusMetersOrNull(precisionBits),
            )
    }
}
