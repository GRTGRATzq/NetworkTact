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

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asinh
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.atanh
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt
import kotlin.math.tan

/** A WGS84 latitude/longitude in decimal degrees. */
data class LatLon(val latitude: Double, val longitude: Double) {
    init {
        require(latitude in -MAX_LATITUDE..MAX_LATITUDE) { "latitude out of range" }
        require(longitude in -MAX_LONGITUDE..MAX_LONGITUDE) { "longitude out of range" }
    }

    /** Whether UTM and MGRS cover this point: from 80° S to 84° N. The polar caps need UPS, which is not supported. */
    val isInUtmLimits: Boolean
        get() = latitude in Utm.MIN_LATITUDE..Utm.MAX_LATITUDE

    companion object {
        const val MAX_LATITUDE = 90.0
        const val MAX_LONGITUDE = 180.0
    }
}

/**
 * A UTM position: [zone] 1–60, latitude [band] letter (C–X without I and O, the MGRS convention, which also gives the
 * hemisphere: N and after is north), [easting] and [northing] in metres.
 */
data class UtmCoordinate(val zone: Int, val band: Char, val easting: Double, val northing: Double) {
    val isNorth: Boolean
        get() = band >= 'N'

    /** `31U 448252 5411933`: zone, band, easting and northing rounded to the metre. */
    fun format(): String = "$zone$band ${easting.roundToLong()} ${northing.roundToLong()}"
}

/**
 * WGS84 ↔ UTM, using Krüger's series to order n⁶ (Karney, "Transverse Mercator with an accuracy of a few nanometers",
 * J. Geodesy 85, 2011): accurate to well under a millimetre inside a zone.
 *
 * Limits: UTM is defined from 80° S to 84° N; the polar caps (UPS) are not covered. Zones follow the Norway (32V) and
 * Svalbard (31X, 33X, 35X, 37X) exceptions.
 */
object Utm {
    const val MIN_LATITUDE = -80.0
    const val MAX_LATITUDE = 84.0
    const val ZONE_COUNT = 60

    /** Slack for a point on a zone or band edge, about 100 m: rounding must not reject it. */
    private const val CELL_TOLERANCE_DEGREES = 0.001

    private const val A = 6_378_137.0
    private const val F = 1 / 298.257223563
    private const val K0 = 0.9996
    private const val FALSE_EASTING = 500_000.0
    private const val FALSE_NORTHING_SOUTH = 10_000_000.0
    private const val ZONE_WIDTH = 6
    private const val HALF_TURN = 180.0
    private const val FULL_TURN = 360.0
    private const val QUARTER_TURN = 90.0
    private const val BAND_HEIGHT = 8
    private const val NEWTON_TOLERANCE = 1e-12
    private const val NEWTON_MAX_ITERATIONS = 10

    /** Latitude bands from 80° S, 8° each; X covers 72°–84° N. */
    internal const val BANDS = "CDEFGHJKLMNPQRSTUVWX"

    private val E = sqrt(F * (2 - F))
    private const val N = F / (2 - F)

    private const val N2 = N * N
    private const val N3 = N2 * N
    private const val N4 = N3 * N
    private const val N5 = N4 * N
    private const val N6 = N5 * N

    private const val AA = A / (1 + N) * (1 + N2 / 4 + N4 / 64 + N6 / 256)

    private val ALPHA =
        doubleArrayOf(
            N / 2 - 2 * N2 / 3 + 5 * N3 / 16 + 41 * N4 / 180 - 127 * N5 / 288 + 7891 * N6 / 37800,
            13 * N2 / 48 - 3 * N3 / 5 + 557 * N4 / 1440 + 281 * N5 / 630 - 1983433 * N6 / 1935360,
            61 * N3 / 240 - 103 * N4 / 140 + 15061 * N5 / 26880 + 167603 * N6 / 181440,
            49561 * N4 / 161280 - 179 * N5 / 168 + 6601661 * N6 / 7257600,
            34729 * N5 / 80640 - 3418889 * N6 / 1995840,
            212378941 * N6 / 319334400,
        )

    private val BETA =
        doubleArrayOf(
            N / 2 - 2 * N2 / 3 + 37 * N3 / 96 - N4 / 360 - 81 * N5 / 512 + 96199 * N6 / 604800,
            N2 / 48 + N3 / 15 - 437 * N4 / 1440 + 46 * N5 / 105 - 1118711 * N6 / 3870720,
            17 * N3 / 480 - 37 * N4 / 840 - 209 * N5 / 4480 + 5569 * N6 / 90720,
            4397 * N4 / 161280 - 11 * N5 / 504 - 830251 * N6 / 7257600,
            4583 * N5 / 161280 - 108847 * N6 / 3991680,
            20648693 * N6 / 638668800,
        )

    /** The latitude band letter of [latitude], or null outside 80° S–84° N. */
    fun bandOf(latitude: Double): Char? {
        if (latitude !in MIN_LATITUDE..MAX_LATITUDE) return null
        val index = floor((latitude - MIN_LATITUDE) / BAND_HEIGHT).toInt().coerceAtMost(BANDS.length - 1)
        return BANDS[index]
    }

    /** Southern and northern latitude of [band], or null when it is not a band letter. */
    fun bandLatitudes(band: Char): ClosedFloatingPointRange<Double>? {
        val index = BANDS.indexOf(band.uppercaseChar())
        if (index < 0) return null
        val south = MIN_LATITUDE + index * BAND_HEIGHT
        val north = if (index == BANDS.length - 1) MAX_LATITUDE else south + BAND_HEIGHT
        return south..north
    }

    /**
     * The zone covering [latitude], [longitude], with the Norway and Svalbard exceptions. Longitude 180° belongs to
     * zone 60.
     */
    fun zoneOf(latitude: Double, longitude: Double): Int {
        val band = bandOf(latitude)
        val exception = ZONE_EXCEPTIONS.firstOrNull { it.band == band && longitude >= it.west && longitude < it.east }
        return exception?.zone ?: (floor((longitude + HALF_TURN) / ZONE_WIDTH).toInt() + 1).coerceIn(1, ZONE_COUNT)
    }

    /**
     * West and east longitude of [zone] within [band], or null when that cell does not exist (32X, 34X and 36X).
     * Norway: 31V spans 0°–3° E and 32V 3°–12° E. Svalbard: 31X 0°–9°, 33X 9°–21°, 35X 21°–33°, 37X 33°–42° E.
     */
    fun zoneLongitudes(zone: Int, band: Char): ClosedFloatingPointRange<Double>? {
        val upperBand = band.uppercaseChar()
        val west = (zone - 1) * ZONE_WIDTH - HALF_TURN
        return when {
            zone !in 1..ZONE_COUNT -> null

            upperBand == SVALBARD_BAND && zone in MISSING_SVALBARD_ZONES -> null

            else ->
                ZONE_EXCEPTIONS.firstOrNull { it.band == upperBand && it.zone == zone }?.let { it.west..it.east }
                    ?: west..(west + ZONE_WIDTH)
        }
    }

    /**
     * Whether [point] lies in grid cell [zone][band], within [CELL_TOLERANCE_DEGREES]. A reference whose numbers land
     * outside the zone and band it names is a typing error, not a position.
     */
    fun contains(zone: Int, band: Char, point: LatLon): Boolean {
        val latitudes = bandLatitudes(band)
        val longitudes = zoneLongitudes(zone, band)
        return latitudes != null &&
            longitudes != null &&
            point.latitude in latitudes.widened() &&
            point.longitude in longitudes.widened()
    }

    private fun ClosedFloatingPointRange<Double>.widened(): ClosedFloatingPointRange<Double> =
        (start - CELL_TOLERANCE_DEGREES)..(endInclusive + CELL_TOLERANCE_DEGREES)

    /** [point] in UTM, or null outside 80° S–84° N. */
    fun fromLatLon(point: LatLon): UtmCoordinate? {
        val band = bandOf(point.latitude) ?: return null
        val zone = zoneOf(point.latitude, point.longitude)
        val (easting, northing) = project(point.latitude, point.longitude, zone)
        return UtmCoordinate(zone, band, easting, northing)
    }

    /** Easting and northing of [latitude], [longitude] in [zone] (which may be a neighbouring one). */
    internal fun project(latitude: Double, longitude: Double, zone: Int): Pair<Double, Double> {
        val phi = latitude.toRadians()
        val lambda = (longitude - centralMeridian(zone)).toRadians()
        val cosLambda = cos(lambda)
        val tau = tan(phi)
        val sigma = sinh(E * atanh(E * tau / sqrt(1 + tau * tau)))
        val tauPrime = tau * sqrt(1 + sigma * sigma) - sigma * sqrt(1 + tau * tau)
        val xiPrime = atan2(tauPrime, cosLambda)
        val etaPrime = asinh(sin(lambda) / sqrt(tauPrime * tauPrime + cosLambda * cosLambda))
        var xi = xiPrime
        var eta = etaPrime
        for (j in 1..ALPHA.size) {
            xi += ALPHA[j - 1] * sin(2 * j * xiPrime) * cosh(2 * j * etaPrime)
            eta += ALPHA[j - 1] * cos(2 * j * xiPrime) * sinh(2 * j * etaPrime)
        }
        val easting = K0 * AA * eta + FALSE_EASTING
        val y = K0 * AA * xi
        val northing = if (latitude < 0) y + FALSE_NORTHING_SOUTH else y
        return easting to northing
    }

    /** Latitude and longitude of [easting], [northing] in [zone], in the northern hemisphere when [north]. */
    fun toLatLon(zone: Int, north: Boolean, easting: Double, northing: Double): LatLon {
        val eta = (easting - FALSE_EASTING) / (K0 * AA)
        val xi = (if (north) northing else northing - FALSE_NORTHING_SOUTH) / (K0 * AA)
        var xiPrime = xi
        var etaPrime = eta
        for (j in 1..BETA.size) {
            xiPrime -= BETA[j - 1] * sin(2 * j * xi) * cosh(2 * j * eta)
            etaPrime -= BETA[j - 1] * cos(2 * j * xi) * sinh(2 * j * eta)
        }
        val sinhEtaPrime = sinh(etaPrime)
        val cosXiPrime = cos(xiPrime)
        val tauPrime = sin(xiPrime) / hypot(sinhEtaPrime, cosXiPrime)
        var tau = tauPrime
        var iterations = 0
        do {
            val sigma = sinh(E * atanh(E * tau / sqrt(1 + tau * tau)))
            val tauI = tau * sqrt(1 + sigma * sigma) - sigma * sqrt(1 + tau * tau)
            val delta =
                (tauPrime - tauI) / sqrt(1 + tauI * tauI) * (1 + (1 - E * E) * tau * tau) /
                    ((1 - E * E) * sqrt(1 + tau * tau))
            tau += delta
            iterations++
        } while (abs(delta) > NEWTON_TOLERANCE && iterations < NEWTON_MAX_ITERATIONS)
        val latitude = atan(tau).toDegrees()
        var longitude = atan2(sinhEtaPrime, cosXiPrime).toDegrees() + centralMeridian(zone)
        if (longitude > HALF_TURN) longitude -= FULL_TURN
        if (longitude < -HALF_TURN) longitude += FULL_TURN
        return LatLon(latitude.coerceIn(-QUARTER_TURN, QUARTER_TURN), longitude)
    }

    private class ZoneException(val band: Char, val zone: Int, val west: Double, val east: Double)

    private const val SVALBARD_BAND = 'X'
    private val MISSING_SVALBARD_ZONES = setOf(32, 34, 36)
    private val ZONE_EXCEPTIONS =
        listOf(
            ZoneException('V', 31, 0.0, 3.0),
            ZoneException('V', 32, 3.0, 12.0),
            ZoneException(SVALBARD_BAND, 31, 0.0, 9.0),
            ZoneException(SVALBARD_BAND, 33, 9.0, 21.0),
            ZoneException(SVALBARD_BAND, 35, 21.0, 33.0),
            ZoneException(SVALBARD_BAND, 37, 33.0, 42.0),
        )

    private fun centralMeridian(zone: Int): Double = (zone - 1) * ZONE_WIDTH - HALF_TURN + ZONE_WIDTH / 2.0
}

private const val DEGREES_PER_HALF_TURN = 180.0

private fun Double.toRadians(): Double = this * PI / DEGREES_PER_HALF_TURN

private fun Double.toDegrees(): Double = this * DEGREES_PER_HALF_TURN / PI
