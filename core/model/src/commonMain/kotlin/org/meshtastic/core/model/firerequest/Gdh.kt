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
package org.meshtastic.core.model.firerequest

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * The time zone a date-time group is written in, chosen by the operator: the phone's own (`loc`), Zulu (`Z`) or French
 * legal time (`fra`, Europe/Paris, summer and winter time included).
 */
enum class GdhZone(val code: String) {
    LOCAL("loc"),
    ZULU("Z"),
    FRANCE("fra"),
    ;

    /** The time zone the clock is read in; [LOCAL] is [phoneZone]. */
    fun timeZone(phoneZone: TimeZone): TimeZone = when (this) {
        LOCAL -> phoneZone
        ZULU -> TimeZone.UTC
        FRANCE -> TimeZone.of("Europe/Paris")
    }
}

/**
 * A date-time group (GDH) as the fire request sheet writes it: `112250 loc NOV 26`, `112150 fra NOV 26`, and Zulu the
 * military way, the zone joined to the time: `112150Z NOV 26`. Months are the NATO abbreviations.
 */
data class Gdh(val day: Int, val hour: Int, val minute: Int, val zone: GdhZone, val month: Int, val year: Int) {

    /** The written form; [year] is printed on two digits. */
    fun format(): String {
        val time = two(day) + two(hour) + two(minute)
        val stamp = if (zone == GdhZone.ZULU) "${time}Z" else "$time ${zone.code}"
        return "$stamp ${MONTHS[month - 1]} ${two(year % CENTURY)}"
    }

    companion object {
        /** NATO month abbreviations, January first. */
        val MONTHS = listOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")

        private const val CENTURY = 100
        private const val FIRST_YEAR = 2000
        private const val LAST_HOUR = 23
        private const val LAST_MINUTE = 59
        private const val FEBRUARY = 2
        private const val LEAP_FEBRUARY_DAYS = 29
        private val MONTH_DAYS = listOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)

        private val PATTERN = Regex("""^(\d{2})(\d{2})(\d{2})(?:(Z)| (LOC|FRA)) ([A-Z]{3}) (\d{2})$""")
        private val WHITESPACE = Regex("""\s+""")
        private const val GROUP_DAY = 1
        private const val GROUP_HOUR = 2
        private const val GROUP_MINUTE = 3
        private const val GROUP_ZULU = 4
        private const val GROUP_ZONE = 5
        private const val GROUP_MONTH = 6
        private const val GROUP_YEAR = 7

        /**
         * The phone's clock at [epochSeconds] read in [zone]: day, month and year are those of that zone (at 00:30 in
         * France, Zulu is still the day before). [phoneZone] is the phone's own zone, used for [GdhZone.LOCAL].
         */
        fun of(epochSeconds: Long, zone: GdhZone, phoneZone: TimeZone): Gdh {
            val time = Instant.fromEpochSeconds(epochSeconds).toLocalDateTime(zone.timeZone(phoneZone))
            return Gdh(time.day, time.hour, time.minute, zone, time.month.ordinal + 1, time.year)
        }

        /**
         * A typed GDH, or null when it does not read: the forms of [format], case and spacing aside, with a day that
         * exists in that month (the year is taken in 2000–2099), an hour from 00 to 23 and minutes from 00 to 59.
         */
        fun parse(text: String): Gdh? {
            val match = PATTERN.find(text.trim().replace(WHITESPACE, " ").uppercase()) ?: return null
            val group = match.groupValues
            val zone =
                when {
                    group[GROUP_ZULU].isNotEmpty() -> GdhZone.ZULU
                    group[GROUP_ZONE] == "LOC" -> GdhZone.LOCAL
                    else -> GdhZone.FRANCE
                }
            val month = MONTHS.indexOf(group[GROUP_MONTH]) + 1
            val year = FIRST_YEAR + group[GROUP_YEAR].toInt()
            val gdh =
                Gdh(group[GROUP_DAY].toInt(), group[GROUP_HOUR].toInt(), group[GROUP_MINUTE].toInt(), zone, month, year)
            return gdh.takeIf {
                month > 0 && it.day in 1..daysIn(month, year) && it.hour <= LAST_HOUR && it.minute <= LAST_MINUTE
            }
        }

        private fun daysIn(month: Int, year: Int): Int =
            if (month == FEBRUARY && isLeap(year)) LEAP_FEBRUARY_DAYS else MONTH_DAYS[month - 1]

        @Suppress("MagicNumber")
        private fun isLeap(year: Int): Boolean = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0

        private fun two(value: Int): String = value.toString().padStart(2, '0')
    }
}
