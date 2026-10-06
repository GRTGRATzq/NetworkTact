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
package org.meshtastic.core.model.team

import org.meshtastic.core.model.utf8Size

/**
 * Team membership, carried as a suffix of the node's long name: `ALPHA-1 [Alpha]`. Every Meshtastic client shows the
 * name as is; NetworkTact reads the suffix back as the team and shows the name without it.
 *
 * The app appends ` (MQTT)` to the long name of a node heard through MQTT, so that mark is looked through.
 */
object TeamSuffix {

    /** Usable bytes in `User.long_name` (`max_size:40`, one byte of which is the NUL terminator). */
    const val LONG_NAME_MAX_BYTES = 39

    private const val MQTT_MARK = " (MQTT)"

    // A bracketed name at the very end, alone or after whitespace. A bracket elsewhere is ordinary text.
    private val SUFFIX = Regex("""(?:^|\s+)\[([^\[\]]+)\]$""")

    /** The team [longName] declares, or null when it carries no team suffix. */
    fun teamOf(longName: String): String? =
        SUFFIX.find(withoutMqttMark(longName))?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }

    /**
     * [longName] without its team suffix, for display next to the team. The ` (MQTT)` mark is kept. A name that is
     * nothing but a suffix is returned unchanged, so it never displays as blank.
     */
    fun displayName(longName: String): String {
        val core = withoutMqttMark(longName)
        val base = withoutTeam(core)
        return when {
            base.isEmpty() -> longName
            longName.endsWith(MQTT_MARK) -> base + MQTT_MARK
            else -> base
        }
    }

    /**
     * The long name that declares [team], replacing any existing suffix, or with the suffix removed when [team] is
     * null. When the result would exceed [LONG_NAME_MAX_BYTES], the name part is shortened, never the team, and the
     * result is flagged as truncated so the user can be warned before anything is sent.
     */
    fun withTeam(longName: String, team: String?): TeamNameChange {
        val base = withoutTeam(longName.trim())
        val suffix = team?.let { " [$it]" }.orEmpty()
        val clipped = base.clipToUtf8Bytes(LONG_NAME_MAX_BYTES - suffix.utf8Size()).trimEnd()
        return TeamNameChange(longName = (clipped + suffix).trim(), truncated = clipped != base)
    }

    private fun withoutMqttMark(longName: String): String = longName.removeSuffix(MQTT_MARK).trimEnd()

    private fun withoutTeam(name: String): String =
        SUFFIX.find(name)?.let { name.substring(0, it.range.first).trimEnd() } ?: name
}

/** A long name built by [TeamSuffix.withTeam]; [truncated] is true when part of the name had to be cut. */
data class TeamNameChange(val longName: String, val truncated: Boolean)

/** Clips to at most [maxBytes] of UTF-8, stepping whole code points so a surrogate pair is never cut in half. */
internal fun String.clipToUtf8Bytes(maxBytes: Int): String {
    var end = 0
    var used = 0
    while (end < length) {
        val step = if (this[end].isHighSurrogate() && end + 1 < length && this[end + 1].isLowSurrogate()) 2 else 1
        val bytes = substring(end, end + step).utf8Size()
        if (used + bytes > maxBytes) break
        used += bytes
        end += step
    }
    return substring(0, end)
}
