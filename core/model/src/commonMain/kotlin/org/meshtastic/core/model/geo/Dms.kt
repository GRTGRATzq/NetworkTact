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

import kotlin.math.abs
import kotlin.math.roundToLong

/** Degrees, minutes, seconds to the second: `48°51'24"N 002°21'03"E`. ASCII quotes, so any keyboard can type them. */
object Dms {
    private const val SECONDS_PER_DEGREE = 3600
    private const val SECONDS_PER_MINUTE = 60
    private const val LATITUDE_DEGREE_DIGITS = 2
    private const val LONGITUDE_DEGREE_DIGITS = 3
    private const val MINUTE_DIGITS = 2

    fun format(point: LatLon): String = "${formatLatitude(point.latitude)} ${formatLongitude(point.longitude)}"

    /** `48°51'24"N`; 0° is north. */
    fun formatLatitude(latitude: Double): String = formatAxis(latitude, LATITUDE_DEGREE_DIGITS, 'N', 'S')

    /** `002°21'03"E`; 0° is east. */
    fun formatLongitude(longitude: Double): String = formatAxis(longitude, LONGITUDE_DEGREE_DIGITS, 'E', 'W')

    /**
     * Seconds are rounded, so 59.6" carries into the next minute (and degree) rather than printing 60". A value that
     * rounds to zero takes the [positive] letter.
     */
    private fun formatAxis(value: Double, degreeDigits: Int, positive: Char, negative: Char): String {
        val totalSeconds = (abs(value) * SECONDS_PER_DEGREE).roundToLong()
        val hemisphere = if (value < 0 && totalSeconds > 0) negative else positive
        val degrees = totalSeconds / SECONDS_PER_DEGREE
        val minutes = totalSeconds % SECONDS_PER_DEGREE / SECONDS_PER_MINUTE
        val seconds = totalSeconds % SECONDS_PER_MINUTE
        return "${degrees.toString().padStart(degreeDigits, '0')}°" +
            "${minutes.toString().padStart(MINUTE_DIGITS, '0')}'" +
            "${seconds.toString().padStart(MINUTE_DIGITS, '0')}\"$hemisphere"
    }
}
