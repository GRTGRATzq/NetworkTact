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
import org.meshtastic.core.model.ContactKey
import org.meshtastic.core.model.geo.SharedPoint
import org.meshtastic.core.model.team.TeamSuffix

/** A point the user may share after sending a message that holds a coordinate, on the [contactKey] it was sent to. */
data class SharePointOffer(val point: SharedPoint, val contactKey: String)

/**
 * The offer for [text], just sent to [contactKey] by our node [ourNum] named [ourLongName], or null when the text holds
 * no complete coordinate, the conversation is retired, or our node is not known yet (the waypoint is locked to it, so
 * it must be a real number). The call sign is the long name without its team suffix.
 */
internal fun sharePointOffer(
    text: String,
    contactKey: String,
    ourNum: Int?,
    ourLongName: String?,
    nowEpochSeconds: Long,
    timeZone: TimeZone,
): SharePointOffer? {
    val sender = ourNum?.takeIf { it != 0 && !ContactKey(contactKey).isRetired }
    val callsign = ourLongName?.let(TeamSuffix::displayName).orEmpty()
    return sender
        ?.let { SharedPoint.of(text, callsign, it, nowEpochSeconds, timeZone) }
        ?.let { SharePointOffer(it, contactKey) }
}
