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

import kotlin.math.floor

/**
 * An MGRS reference to the metre: grid zone ([zone] and latitude [band]), 100 km square ([column] and [row] letters),
 * then [easting] and [northing] inside the square, 0–99 999 m.
 */
data class MgrsCoordinate(
    val zone: Int,
    val band: Char,
    val column: Char,
    val row: Char,
    val easting: Int,
    val northing: Int,
) {
    /** `31U DQ 48251 11932`: ten digits, 1 m. */
    fun format(): String = "$zone$band $column$row ${easting.toString().padStart(DIGITS, '0')} " +
        northing.toString().padStart(DIGITS, '0')

    companion object {
        /** Digits per axis at 1 m precision. */
        const val DIGITS = 5
    }
}

/**
 * UTM ↔ MGRS (WGS84, the "new" AA lettering scheme). MGRS truncates: a reference names the south-west corner of the
 * metre square holding the point, never a rounded neighbour. Same limits as [Utm]: 80° S–84° N.
 */
object Mgrs {
    private const val SQUARE = 100_000
    private const val ROW_CYCLE = 2_000_000
    private const val MAX_NORTHING = 10_000_000

    /**
     * One micrometre added before truncating: a whole-metre reference converted to latitude/longitude and back comes
     * out a hair under its value (448250.9999999), which plain truncation would turn into the metre below.
     */
    private const val ROUND_TRIP_SLACK = 1e-6

    /** Column letters for zones 1, 4, 7…; 2, 5, 8…; 3, 6, 9…. */
    private val COLUMN_SETS = listOf("ABCDEFGH", "JKLMNPQR", "STUVWXYZ")

    /** Row letters, repeating every 2 000 km; even zones start 5 letters further on. */
    private const val ROWS = "ABCDEFGHJKLMNPQRSTUV"
    private const val EVEN_ZONE_ROW_OFFSET = 5

    fun fromLatLon(point: LatLon): MgrsCoordinate? = Utm.fromLatLon(point)?.let(::fromUtm)

    fun fromUtm(utm: UtmCoordinate): MgrsCoordinate {
        val columns = COLUMN_SETS[(utm.zone - 1) % COLUMN_SETS.size]
        val eastingMetres = floor(utm.easting + ROUND_TRIP_SLACK).toLong()
        val northingMetres = floor(utm.northing + ROUND_TRIP_SLACK).toLong()
        val columnIndex = (eastingMetres / SQUARE - 1).toInt().coerceIn(0, columns.length - 1)
        val rowIndex = ((northingMetres / SQUARE).toInt() + rowOffset(utm.zone)) % ROWS.length
        return MgrsCoordinate(
            zone = utm.zone,
            band = utm.band,
            column = columns[columnIndex],
            row = ROWS[rowIndex],
            easting = (eastingMetres % SQUARE).toInt(),
            northing = (northingMetres % SQUARE).toInt(),
        )
    }

    /** Whether [column] and [row] name a 100 km square of [zone]. */
    fun isValidSquare(zone: Int, column: Char, row: Char): Boolean = zone in 1..Utm.ZONE_COUNT &&
        COLUMN_SETS[(zone - 1) % COLUMN_SETS.size].contains(column.uppercaseChar()) &&
        ROWS.contains(row.uppercaseChar())

    /**
     * [mgrs] in UTM. The row letters repeat every 2 000 km, so the band picks the repetition whose latitude falls in
     * it. Null when the square is not one of the zone's, or when no repetition falls in the band and zone.
     */
    fun toUtm(mgrs: MgrsCoordinate): UtmCoordinate? {
        if (!isValidSquare(mgrs.zone, mgrs.column, mgrs.row)) return null
        val north = mgrs.band.uppercaseChar() >= 'N'
        val columnIndex = COLUMN_SETS[(mgrs.zone - 1) % COLUMN_SETS.size].indexOf(mgrs.column.uppercaseChar())
        val easting = ((columnIndex + 1) * SQUARE + mgrs.easting).toDouble()
        val rowIndex = (ROWS.indexOf(mgrs.row.uppercaseChar()) - rowOffset(mgrs.zone)).mod(ROWS.length)
        val baseNorthing = rowIndex * SQUARE + mgrs.northing
        return generateSequence(baseNorthing) { it + ROW_CYCLE }
            .takeWhile { it < MAX_NORTHING }
            .map { northing -> UtmCoordinate(mgrs.zone, mgrs.band.uppercaseChar(), easting, northing.toDouble()) }
            .firstOrNull { utm ->
                Utm.contains(utm.zone, utm.band, Utm.toLatLon(utm.zone, north, easting, utm.northing))
            }
    }

    private fun rowOffset(zone: Int): Int = if (zone % 2 == 0) EVEN_ZONE_ROW_OFFSET else 0
}
