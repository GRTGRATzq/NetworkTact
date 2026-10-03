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
package org.meshtastic.core.model.freshness

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * How long ago a message was received, in the coarse steps a conversation shows ("2 min ago"). Each step is truncated,
 * never rounded up or down to look more recent: 1 min 59 s is still "1 min".
 */
sealed interface MessageAge {
    /** No usable time: the age is unknown and is not estimated. */
    data object Unknown : MessageAge

    /** Under a minute, or slightly ahead within [FreshnessThresholds.clockTolerance]. */
    data object JustNow : MessageAge

    data class Minutes(val minutes: Long) : MessageAge

    data class Hours(val hours: Long) : MessageAge

    data class Days(val days: Long) : MessageAge

    /** The time lies further in the future than the clock tolerance allows. */
    data class InconsistentTimestamp(val ahead: Duration) : MessageAge

    companion object {
        private const val MINUTES_PER_HOUR = 60
        private const val HOURS_PER_DAY = 24

        /**
         * @param timestampMillis when the message was received, epoch milliseconds; 0 or less when unknown.
         * @param nowMillis the current time, epoch milliseconds.
         */
        fun of(
            timestampMillis: Long,
            nowMillis: Long,
            thresholds: FreshnessThresholds = FreshnessThresholds.Default,
        ): MessageAge {
            if (timestampMillis <= 0L) return Unknown
            val age = (nowMillis - timestampMillis).milliseconds
            return when {
                -age > thresholds.clockTolerance -> InconsistentTimestamp(-age)
                age.inWholeMinutes < 1 -> JustNow
                age.inWholeMinutes < MINUTES_PER_HOUR -> Minutes(age.inWholeMinutes)
                age.inWholeHours < HOURS_PER_DAY -> Hours(age.inWholeHours)
                else -> Days(age.inWholeDays)
            }
        }
    }
}
