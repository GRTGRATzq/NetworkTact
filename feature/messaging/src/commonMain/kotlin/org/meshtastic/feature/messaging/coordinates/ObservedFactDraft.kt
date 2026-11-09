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
import org.meshtastic.core.model.geo.CoordinateParser
import org.meshtastic.core.model.geo.FormattedCoordinates
import org.meshtastic.core.model.geo.ObservedFactMessage
import org.meshtastic.core.model.geo.ObservedTime
import org.meshtastic.core.model.geo.PositionMessage
import org.meshtastic.core.model.utf8Size

/**
 * The report being written: [message] once the place and the time parse, its size, whether it may go into the input,
 * and the time understood from what was typed ([observedAt], null while it does not parse). While the place or the time
 * does not parse, [bytes] is an estimate ([sizeIsEstimate]) made with a typical MGRS place, so a description that is
 * already too long is flagged before the rest of the form is filled in.
 */
internal data class ObservedFactDraft(
    val message: String?,
    val bytes: Int,
    val canInsert: Boolean,
    val observedAt: ObservedTime?,
    val sizeIsEstimate: Boolean = false,
) {
    /** More than [PositionMessage.MAX_MESSAGE_BYTES]: the size is shown and the report cannot be inserted. */
    val tooLong: Boolean
        get() = bytes > PositionMessage.MAX_MESSAGE_BYTES
}

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
    val bytes = message?.utf8Size() ?: estimatedBytes(typedAs, description, observedAt)
    return ObservedFactDraft(
        message = message,
        bytes = bytes,
        canInsert = message != null && description.isNotBlank() && bytes <= PositionMessage.MAX_MESSAGE_BYTES,
        observedAt = observedAt,
        sizeIsEstimate = message == null,
    )
}

/** The size the report would have with a typical MGRS place and, when the time does not parse yet, a plain `00:00`. */
private fun estimatedBytes(typedAs: CoordinateFormat, description: String, observedAt: ObservedTime?): Int {
    val place = (CoordinateParser.parse(REFERENCE_PLACE, CoordinateFormat.MGRS) as? CoordinateInput.Valid) ?: return 0
    return ObservedFactMessage.build(
        FormattedCoordinates.of(place.point),
        typedAs,
        description,
        observedAt ?: ObservedTime(0, 0, previousDay = false),
    )
        .utf8Size()
}

private const val REFERENCE_PLACE = "31U DQ 48251 11932"

private val CLOCK = Regex("""^(\d{1,2}) ?[:hH. ]? ?(\d{2})$""")

/** Hours and minutes typed as `14:05`, `14h05`, `14 05`, `1405` or `9:05`; null for anything else. */
internal fun parseClock(text: String): Pair<Int, Int>? =
    CLOCK.find(text.trim())?.destructured?.let { (hour, minute) -> hour.toInt() to minute.toInt() }
