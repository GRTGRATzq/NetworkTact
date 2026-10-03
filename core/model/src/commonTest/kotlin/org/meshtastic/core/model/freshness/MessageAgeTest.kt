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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class MessageAgeTest {

    private val now = 1_790_000_000_000L

    private fun ageOf(elapsedMillis: Long) = MessageAge.of(timestampMillis = now - elapsedMillis, nowMillis = now)

    @Test
    fun missingTimeIsUnknownNeverJustNow() {
        assertEquals(MessageAge.Unknown, MessageAge.of(timestampMillis = 0L, nowMillis = now))
        assertEquals(MessageAge.Unknown, MessageAge.of(timestampMillis = -1L, nowMillis = now))
    }

    @Test
    fun underAMinuteIsJustNow() {
        assertEquals(MessageAge.JustNow, ageOf(0L))
        assertEquals(MessageAge.JustNow, ageOf(59.seconds.inWholeMilliseconds))
    }

    @Test
    fun minutesAreTruncatedNeverRoundedDown() {
        assertEquals(MessageAge.Minutes(1), ageOf(60.seconds.inWholeMilliseconds))
        assertEquals(MessageAge.Minutes(1), ageOf(119.seconds.inWholeMilliseconds))
        assertEquals(MessageAge.Minutes(2), ageOf(2.minutes.inWholeMilliseconds))
        assertEquals(MessageAge.Minutes(59), ageOf((60.minutes - 1.seconds).inWholeMilliseconds))
    }

    @Test
    fun hoursThenDays() {
        assertEquals(MessageAge.Hours(1), ageOf(60.minutes.inWholeMilliseconds))
        assertEquals(MessageAge.Hours(23), ageOf((24.hours - 1.seconds).inWholeMilliseconds))
        assertEquals(MessageAge.Days(1), ageOf(24.hours.inWholeMilliseconds))
        assertEquals(MessageAge.Days(3), ageOf((3.days + 5.hours).inWholeMilliseconds))
    }

    @Test
    fun slightlyAheadWithinClockToleranceIsJustNow() {
        assertEquals(MessageAge.JustNow, ageOf(-(2.minutes.inWholeMilliseconds)))
    }

    @Test
    fun aheadBeyondClockToleranceIsInconsistent() {
        val ahead = 2.minutes + 1.seconds
        assertEquals(MessageAge.InconsistentTimestamp(ahead), ageOf(-ahead.inWholeMilliseconds))
    }
}
