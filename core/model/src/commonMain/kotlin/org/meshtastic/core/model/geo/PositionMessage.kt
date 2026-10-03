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
import org.meshtastic.core.model.freshness.PositionState
import org.meshtastic.core.model.utf8Size
import kotlin.time.Duration
import kotlin.time.Instant

/** Where the position of a `[POS]` message comes from. */
enum class PositionSource {
    /** The user's own radio (its GPS, or the phone position it was given). */
    RADIO,

    /** The phone's own last fix, used when the radio has no position; the message says so. */
    PHONE,
}

/**
 * The `[POS]` message: a plain-text convention, readable in any Meshtastic client, that gives a position in the three
 * formats with the time it was taken:
 *
 * `[POS] ALPHA-1 · MGRS 31U DQ 48251 11932 · UTM 31U 448251 5411932 · DMS 48°51'24"N 002°21'03"E · relevée 14:32`
 *
 * The time part never lets an old position pass for a current one: past the command post threshold it reads `POSITION
 * ANCIENNE, relevée il y a 25 min (14:07)`, and without a GPS fix time `heure de relevé inconnue`. The words are a
 * fixed French convention, so every phone on the net sends the same text whatever its language.
 */
object PositionMessage {
    const val PREFIX = "[POS]"

    /** Largest text payload a Meshtastic radio carries, in UTF-8 bytes. */
    const val MAX_MESSAGE_BYTES = 200

    private const val SEPARATOR = " · "
    private const val MINUTES_PER_HOUR = 60
    private const val HOURS_PER_DAY = 24
    private const val CLOCK_DIGITS = 2

    /**
     * The message for [point], taken by [name]'s [source], whose age is [state] at [nowEpochSeconds]. Clock times are
     * shown in [timeZone].
     *
     * When the full message passes [MAX_MESSAGE_BYTES], DMS is dropped first, then UTM: MGRS, the name and the time are
     * always kept. Beyond 84° N and 80° S, where MGRS and UTM are undefined, only DMS is given.
     */
    fun build(
        name: String,
        point: LatLon,
        source: PositionSource,
        state: PositionState,
        nowEpochSeconds: Long,
        timeZone: TimeZone,
    ): String {
        val formatted = FormattedCoordinates.of(point)
        val who = if (source == PositionSource.PHONE) "$name (téléphone)" else name
        val time = timeText(state, nowEpochSeconds, timeZone)
        val mgrs = formatted.mgrs?.let { "MGRS $it" }
        val utm = formatted.utm?.let { "UTM $it" }
        val dms = "DMS ${formatted.dms}"
        val candidates =
            if (mgrs == null || utm == null) {
                listOf(listOf(dms))
            } else {
                listOf(listOf(mgrs, utm, dms), listOf(mgrs, utm), listOf(mgrs))
            }
        val messages =
            candidates.map { coordinates -> (listOf("$PREFIX $who") + coordinates + time).joinToString(SEPARATOR) }
        return messages.firstOrNull { it.utf8Size() <= MAX_MESSAGE_BYTES } ?: messages.last()
    }

    /** What can honestly be said about when the position was taken. */
    internal fun timeText(state: PositionState, nowEpochSeconds: Long, timeZone: TimeZone): String = when (state) {
        is PositionState.Fresh -> "relevée ${clock(nowEpochSeconds, state.age, timeZone)}"

        is PositionState.Stale ->
            "POSITION ANCIENNE, relevée il y a ${age(state.age)} (${clock(nowEpochSeconds, state.age, timeZone)})"

        is PositionState.StaleAtLeast ->
            "POSITION ANCIENNE, heure de relevé inconnue, reçue il y a ${age(state.minAge)}"

        is PositionState.InconsistentTimestamp -> "heure de relevé incohérente"

        is PositionState.ReceivedFixTimeUnknown,
        PositionState.NoFixTime,
        PositionState.NoPosition,
        -> "heure de relevé inconnue"
    }

    /** `25 min`, `2 h 05`, `3 j`. */
    internal fun age(age: Duration): String {
        val minutes = age.inWholeMinutes
        val hours = minutes / MINUTES_PER_HOUR
        return when {
            hours == 0L -> "$minutes min"
            hours < HOURS_PER_DAY -> "$hours h ${(minutes % MINUTES_PER_HOUR).toString().padStart(CLOCK_DIGITS, '0')}"
            else -> "${hours / HOURS_PER_DAY} j"
        }
    }

    private fun clock(nowEpochSeconds: Long, age: Duration, timeZone: TimeZone): String {
        val time = Instant.fromEpochSeconds(nowEpochSeconds - age.inWholeSeconds).toLocalDateTime(timeZone).time
        return "${time.hour.toString().padStart(
            CLOCK_DIGITS,
            '0',
        )}:${time.minute.toString().padStart(CLOCK_DIGITS, '0')}"
    }
}
