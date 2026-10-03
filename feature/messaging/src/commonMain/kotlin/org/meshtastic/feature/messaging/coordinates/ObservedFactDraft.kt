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

import org.meshtastic.core.model.geo.CoordinateFormat
import org.meshtastic.core.model.geo.CoordinateInput
import org.meshtastic.core.model.geo.FormattedCoordinates
import org.meshtastic.core.model.geo.ObservedFactMessage
import org.meshtastic.core.model.geo.PositionMessage
import org.meshtastic.core.model.utf8Size

/** The report being written: [message] once the place parses, its size, and whether it may go into the input. */
internal data class ObservedFactDraft(val message: String?, val bytes: Int, val canInsert: Boolean)

/** A place that does not parse or an empty description gives no insertable report; nor does one over 200 bytes. */
internal fun observedFactDraft(
    place: CoordinateInput,
    typedAs: CoordinateFormat,
    description: String,
): ObservedFactDraft {
    val message =
        (place as? CoordinateInput.Valid)?.let {
            ObservedFactMessage.build(FormattedCoordinates.of(it.point), typedAs, description)
        }
    val bytes = message?.utf8Size() ?: 0
    return ObservedFactDraft(
        message = message,
        bytes = bytes,
        canInsert = message != null && description.isNotBlank() && bytes <= PositionMessage.MAX_MESSAGE_BYTES,
    )
}
