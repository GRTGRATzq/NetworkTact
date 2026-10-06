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

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * When a fact was observed, typed as hours and minutes. A time later than the current minute cannot have been observed
 * today, so it is taken as the day before and the message says so: `23:50 (veille)`.
 */
data class ObservedTime(val hour: Int, val minute: Int, val previousDay: Boolean) {

    /** `14:05`, or `23:50 (veille)` for the day before. */
    fun label(): String {
        val clock = "${hour.toString().padStart(CLOCK_DIGITS, '0')}:${minute.toString().padStart(CLOCK_DIGITS, '0')}"
        return if (previousDay) "$clock $PREVIOUS_DAY" else clock
    }

    companion object {
        const val PREVIOUS_DAY = "(veille)"

        private const val CLOCK_DIGITS = 2
        private const val HOURS_PER_DAY = 24
        private const val MINUTES_PER_HOUR = 60

        /**
         * [hour]:[minute] as typed at [nowEpochSeconds] in [timeZone]: today, or the day before when it is later than
         * the current minute. Null when the hour or the minute is out of range.
         */
        fun of(hour: Int, minute: Int, nowEpochSeconds: Long, timeZone: TimeZone): ObservedTime? {
            if (hour !in 0 until HOURS_PER_DAY || minute !in 0 until MINUTES_PER_HOUR) return null
            val now = Instant.fromEpochSeconds(nowEpochSeconds).toLocalDateTime(timeZone).time
            val typed = hour * MINUTES_PER_HOUR + minute
            val current = now.hour * MINUTES_PER_HOUR + now.minute
            return ObservedTime(hour, minute, previousDay = typed > current)
        }

        /** The current minute at [nowEpochSeconds] in [timeZone], the form's starting value. */
        fun now(nowEpochSeconds: Long, timeZone: TimeZone): ObservedTime {
            val now = Instant.fromEpochSeconds(nowEpochSeconds).toLocalDateTime(timeZone).time
            return ObservedTime(now.hour, now.minute, previousDay = false)
        }
    }
}
