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

/**
 * A point picked on the map by a long press: its coordinate in the three formats, and the text that "Insérer dans un
 * message" puts in the input.
 *
 * A picked point is a place, not a GPS position: it has no fix time and the text says it was chosen on the map, `Point
 * carte · MGRS 31U DQ 48251 11932 · UTM 31U 448251 5411932 · DMS 48°51'24"N 002°21'03"E`. MGRS comes first, to 1 m, so
 * [MessageCoordinate] reads the point back from a received message. Beyond 84° N and 80° S, where MGRS and UTM are
 * undefined, only DMS is given. The words are a fixed French convention, like the other messages of this fork.
 */
data class MapPointCoordinate(val formatted: FormattedCoordinates) {
    val point: LatLon
        get() = formatted.point

    /** The text inserted into a message; nothing is sent until the user sends it. */
    val messageText: String
        get() =
            listOfNotNull(
                PREFIX,
                formatted.mgrs?.let { "MGRS $it" },
                formatted.utm?.let { "UTM $it" },
                "DMS ${formatted.dms}",
            )
                .joinToString(SEPARATOR)

    companion object {
        const val PREFIX = "Point carte"
        private const val SEPARATOR = " · "
        private const val FULL_TURN = 360.0

        /**
         * The coordinate of the point at [latitude], [longitude] as the map reports it, or null when it is not a point
         * on Earth. A map scrolled past the antimeridian reports a longitude beyond ±180°: it is brought back into
         * range.
         */
        fun of(latitude: Double, longitude: Double): MapPointCoordinate? =
            if (latitude in -LatLon.MAX_LATITUDE..LatLon.MAX_LATITUDE && longitude.isFinite()) {
                MapPointCoordinate(FormattedCoordinates.of(LatLon(latitude, normalizeLongitude(longitude))))
            } else {
                null
            }

        /** [longitude] in [-180, 180). */
        internal fun normalizeLongitude(longitude: Double): Double {
            val shifted = (longitude + LatLon.MAX_LONGITUDE) % FULL_TURN
            return (if (shifted < 0) shifted + FULL_TURN else shifted) - LatLon.MAX_LONGITUDE
        }
    }
}
