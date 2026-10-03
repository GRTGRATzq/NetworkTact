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

/** The three formats a position can be typed in. */
enum class CoordinateFormat {
    MGRS,
    UTM,
    DMS,
}

/** Why a typed position was refused. Each maps to one message telling the user what to fix. */
enum class CoordinateError {
    EMPTY,

    /** Not the shape of the chosen format at all. */
    SYNTAX,

    /** Zone outside 1–60, or 32X, 34X, 36X, which do not exist. */
    ZONE,

    /** Band letter outside C–X, or I or O. */
    BAND,

    /** The two 100 km square letters do not belong to the zone. */
    SQUARE,

    /** MGRS without exactly 5 + 5 digits (1 m). */
    PRECISION,

    /** UTM easting outside 100 000–900 000 m. */
    EASTING,

    /** UTM northing outside 0–10 000 000 m. */
    NORTHING,

    /** Latitude degrees above 90 or longitude degrees above 180. */
    DEGREES,

    /** Minutes or seconds of 60 or more. */
    MINUTES_SECONDS,

    /** The numbers are well formed but land outside the zone and band they name. */
    OUTSIDE_CELL,
}

sealed interface CoordinateInput {
    data class Valid(val point: LatLon) : CoordinateInput

    data class Invalid(val error: CoordinateError) : CoordinateInput
}

/** One position in all three formats. [mgrs] and [utm] are null outside 80° S–84° N, where they are undefined. */
data class FormattedCoordinates(val point: LatLon, val mgrs: String?, val utm: String?, val dms: String) {
    companion object {
        fun of(point: LatLon): FormattedCoordinates = FormattedCoordinates(
            point = point,
            mgrs = Mgrs.fromLatLon(point)?.format(),
            utm = Utm.fromLatLon(point)?.format(),
            dms = Dms.format(point),
        )
    }

    fun format(format: CoordinateFormat): String? = when (format) {
        CoordinateFormat.MGRS -> mgrs
        CoordinateFormat.UTM -> utm
        CoordinateFormat.DMS -> dms
    }
}

/**
 * Strict reading of a typed position. Case and spacing are free; everything else must match:
 * - MGRS `31U DQ 48251 11932` (or `31UDQ4825111932`), exactly ten digits;
 * - UTM `31U 448251 5411932`: zone and latitude band (not a hemisphere letter), easting, northing in metres;
 * - DMS `48°51'24"N 002°21'03"E`: degrees, minutes and seconds all present, hemisphere after each; ′ and ″ are
 *   accepted, so are decimal seconds and O for west (ouest). The symbols may be replaced by spaces, `48 51 24 N 2 21 3
 *   E`, since ° is hard to reach on a phone keyboard.
 */
object CoordinateParser {
    private const val MAX_EASTING = 900_000.0
    private const val MIN_EASTING = 100_000.0
    private const val MAX_NORTHING = 10_000_000.0
    private const val MAX_LATITUDE_DEGREES = 90
    private const val MAX_LONGITUDE_DEGREES = 180
    private const val SIXTY = 60.0
    private const val MGRS_DIGITS = MgrsCoordinate.DIGITS * 2

    private val MGRS = Regex("""^(?<zone>\d{1,2})(?<band>[A-Z])(?<column>[A-Z])(?<row>[A-Z])(?<digits>\d+)$""")
    private val UTM =
        Regex("""^(?<zone>\d{1,2}) ?(?<band>[A-Z]) (?<easting>\d+(?:[.,]\d+)?) (?<northing>\d+(?:[.,]\d+)?)$""")
    private val DMS = Regex("""^${dmsAxis("lat")}(?<latH>[NS])[ ,;]+${dmsAxis("lon")}(?<lonH>[EWO])$""")

    private fun dmsAxis(name: String) = """(?<${name}D>\d{1,3})(?: ?° ?| )(?<${name}M>\d{1,2})(?: ?['′] ?| )""" +
        """(?<${name}S>\d{1,2}(?:[.,]\d+)?) ?(?:"|″|'')? ?"""

    fun parse(text: String, format: CoordinateFormat): CoordinateInput {
        val normalized = text.trim().uppercase().replace(Regex("""\s+"""), " ")
        if (normalized.isEmpty()) return CoordinateInput.Invalid(CoordinateError.EMPTY)
        return when (format) {
            CoordinateFormat.MGRS -> parseMgrs(normalized.replace(" ", ""))
            CoordinateFormat.UTM -> parseUtm(normalized)
            CoordinateFormat.DMS -> parseDms(normalized)
        }
    }

    private fun parseMgrs(text: String): CoordinateInput {
        val match = MGRS.matchEntire(text) ?: return invalid(CoordinateError.SYNTAX)
        val zone = match.group("zone").toInt()
        val band = match.group("band").single()
        val column = match.group("column").single()
        val row = match.group("row").single()
        val digits = match.group("digits")
        return checkGridZone(zone, band)
            ?: when {
                !Mgrs.isValidSquare(zone, column, row) -> invalid(CoordinateError.SQUARE)

                digits.length != MGRS_DIGITS -> invalid(CoordinateError.PRECISION)

                else -> {
                    val easting = digits.take(MgrsCoordinate.DIGITS).toInt()
                    val northing = digits.drop(MgrsCoordinate.DIGITS).toInt()
                    Mgrs.toUtm(MgrsCoordinate(zone, band, column, row, easting, northing))?.let {
                        CoordinateInput.Valid(Utm.toLatLon(it.zone, it.isNorth, it.easting, it.northing))
                    } ?: invalid(CoordinateError.OUTSIDE_CELL)
                }
            }
    }

    private fun parseUtm(text: String): CoordinateInput {
        val match = UTM.matchEntire(text) ?: return invalid(CoordinateError.SYNTAX)
        val zone = match.group("zone").toInt()
        val band = match.group("band").single()
        val easting = match.group("easting").toDecimal()
        val northing = match.group("northing").toDecimal()
        return checkGridZone(zone, band)
            ?: when {
                easting !in MIN_EASTING..MAX_EASTING -> invalid(CoordinateError.EASTING)

                northing !in 0.0..MAX_NORTHING -> invalid(CoordinateError.NORTHING)

                else -> {
                    val point = Utm.toLatLon(zone, band >= 'N', easting, northing)
                    if (Utm.contains(zone, band, point)) {
                        CoordinateInput.Valid(point)
                    } else {
                        invalid(CoordinateError.OUTSIDE_CELL)
                    }
                }
            }
    }

    private fun parseDms(text: String): CoordinateInput {
        val match = DMS.matchEntire(text) ?: return invalid(CoordinateError.SYNTAX)
        val latitude = match.axis("lat", MAX_LATITUDE_DEGREES)
        val longitude = match.axis("lon", MAX_LONGITUDE_DEGREES)
        return when {
            latitude == null -> outOfRange(match, "lat")

            longitude == null -> outOfRange(match, "lon")

            else -> {
                val signedLatitude = if (match.group("latH") == "S") -latitude else latitude
                val signedLongitude = if (match.group("lonH") == "E") longitude else -longitude
                CoordinateInput.Valid(LatLon(signedLatitude, signedLongitude))
            }
        }
    }

    private fun MatchResult.group(name: String): String = checkNotNull(groups[name]).value

    private fun String.toDecimal(): Double = replace(',', '.').toDouble()

    /** Decimal degrees of axis [name], or null when a part is out of range or the total passes [maxDegrees]. */
    private fun MatchResult.axis(name: String, maxDegrees: Int): Double? {
        val minutes = group("${name}M").toInt()
        val seconds = group("${name}S").toDecimal()
        val value = group("${name}D").toInt() + minutes / SIXTY + seconds / (SIXTY * SIXTY)
        return value.takeIf { minutes < SIXTY && seconds < SIXTY && value <= maxDegrees }
    }

    private fun outOfRange(match: MatchResult, name: String): CoordinateInput = invalid(
        if (match.group("${name}M").toInt() >= SIXTY || match.group("${name}S").toDecimal() >= SIXTY) {
            CoordinateError.MINUTES_SECONDS
        } else {
            CoordinateError.DEGREES
        },
    )

    /** Null when [zone][band] is a grid zone that exists, else the error to report. */
    private fun checkGridZone(zone: Int, band: Char): CoordinateInput? = when {
        zone !in 1..Utm.ZONE_COUNT -> invalid(CoordinateError.ZONE)
        band !in Utm.BANDS -> invalid(CoordinateError.BAND)
        Utm.zoneLongitudes(zone, band) == null -> invalid(CoordinateError.ZONE)
        else -> null
    }

    private fun invalid(error: CoordinateError) = CoordinateInput.Invalid(error)
}
