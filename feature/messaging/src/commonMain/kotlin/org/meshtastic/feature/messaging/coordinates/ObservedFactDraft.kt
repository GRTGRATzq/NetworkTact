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
package org.meshtastic.feature.messaging.coordinates

import kotlinx.datetime.TimeZone
import org.meshtastic.core.model.geo.CoordinateFormat
import org.meshtastic.core.model.geo.CoordinateInput
import org.meshtastic.core.model.geo.FormattedCoordinates
import org.meshtastic.core.model.geo.ObservedFactMessage
import org.meshtastic.core.model.geo.ObservedTime
import org.meshtastic.core.model.geo.PositionMessage
import org.meshtastic.core.model.utf8Size

/**
 * The report being written: [message] once the place and the time parse, its size, whether it may go into the input,
 * and the time understood from what was typed ([observedAt], null while it does not parse).
 */
internal data class ObservedFactDraft(
    val message: String?,
    val bytes: Int,
    val canInsert: Boolean,
    val observedAt: ObservedTime?,
)

/**
 * A place or a time that does not parse, or an empty description, gives no insertable report; nor does one over 200
 * bytes. [timeText] is read at [nowEpochSeconds] in [timeZone]: a time still to come today is the day before.
 */
internal fun observedFactDraft(
    place: CoordinateInput,
    typedAs: CoordinateFormat,
    description: String,
    timeText: String,
    nowEpochSeconds: Long,
    timeZone: TimeZone,
): ObservedFactDraft {
    val observedAt =
        parseClock(timeText)?.let { (hour, minute) -> ObservedTime.of(hour, minute, nowEpochSeconds, timeZone) }
    val message =
        if (place is CoordinateInput.Valid && observedAt != null) {
            ObservedFactMessage.build(FormattedCoordinates.of(place.point), typedAs, description, observedAt)
        } else {
            null
        }
    val bytes = message?.utf8Size() ?: 0
    return ObservedFactDraft(
        message = message,
        bytes = bytes,
        canInsert = message != null && description.isNotBlank() && bytes <= PositionMessage.MAX_MESSAGE_BYTES,
        observedAt = observedAt,
    )
}

private val CLOCK = Regex("""^(\d{1,2}) ?[:hH. ]? ?(\d{2})$""")

/** Hours and minutes typed as `14:05`, `14h05`, `14 05`, `1405` or `9:05`; null for anything else. */
internal fun parseClock(text: String): Pair<Int, Int>? =
    CLOCK.find(text.trim())?.destructured?.let { (hour, minute) -> hour.toInt() to minute.toInt() }
