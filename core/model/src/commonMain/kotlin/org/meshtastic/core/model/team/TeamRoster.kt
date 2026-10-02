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
 * The list of teams, carried as a plain text message any Meshtastic client can read: `[EQUIPES] Alpha;Bravo;Charlie`.
 *
 * Team names are normalized (outer spaces trimmed, inner runs of whitespace collapsed to one space) and kept in the
 * order given, the first spelling winning when two names differ only by case. A name may not contain `[`, `]` or `;`,
 * which delimit the list and the long-name suffix (see [TeamSuffix]).
 *
 * Nothing authenticates the list: any node can send one.
 */
data class TeamRoster(val teams: List<String>) {

    /** The message that carries this list, e.g. `[EQUIPES] Alpha;Bravo`. */
    fun toMessage(): String = "$PREFIX ${teams.joinToString(SEPARATOR.toString())}"

    /** Whether [name] is one of the teams, ignoring case. */
    fun contains(name: String): Boolean = teams.any { it.equals(name, ignoreCase = true) }

    companion object {
        const val PREFIX = "[EQUIPES]"

        const val SEPARATOR = ';'

        /** Size limit of a text message payload, in UTF-8 bytes (same value as the messaging screen's limit). */
        const val MAX_MESSAGE_BYTES = 200

        /**
         * Longest team name, in UTF-8 bytes. Keeps the ` [name]` suffix at 19 bytes, leaving at least 20 of the 39
         * bytes of a long name for the call sign.
         */
        const val MAX_TEAM_NAME_BYTES = 16

        private val WHITESPACE = Regex("\\s+")
        private val FORBIDDEN = charArrayOf('[', ']', SEPARATOR)

        /** Whether [text] is meant as a team list: it starts with [PREFIX], ignoring leading spaces and case. */
        fun isRosterMessage(text: CharSequence): Boolean = text.trimStart().startsWith(PREFIX, ignoreCase = true)

        /**
         * The list a received message carries, or null when [text] is not a team list or is malformed. A malformed list
         * is dropped whole rather than in part, so a broken message can never install half a list.
         */
        fun parseMessage(text: String): TeamRoster? =
            if (isRosterMessage(text) && text.utf8Size() <= MAX_MESSAGE_BYTES) {
                (parseNames(text.trimStart().substring(PREFIX.length)) as? RosterInput.Valid)?.roster
            } else {
                null
            }

        /**
         * Validates a list typed by hand, names separated by `;` or new lines, and checks that the resulting message
         * fits in [MAX_MESSAGE_BYTES].
         */
        fun parseInput(input: String): RosterInput {
            val result = parseNames(input.replace('\n', SEPARATOR))
            if (result !is RosterInput.Valid) return result
            val bytes = result.roster.toMessage().utf8Size()
            return if (bytes > MAX_MESSAGE_BYTES) RosterInput.MessageTooLong(bytes) else result
        }

        private fun parseNames(body: String): RosterInput {
            val names = body.split(SEPARATOR).map { it.trim().replace(WHITESPACE, " ") }.filter { it.isNotEmpty() }
            val invalid = names.firstOrNull { name -> name.any { it in FORBIDDEN } }
            val tooLong = names.firstOrNull { it.utf8Size() > MAX_TEAM_NAME_BYTES }
            return when {
                invalid != null -> RosterInput.InvalidName(invalid)
                tooLong != null -> RosterInput.NameTooLong(tooLong)
                names.isEmpty() -> RosterInput.Empty
                else -> RosterInput.Valid(TeamRoster(names.distinctBy { it.lowercase() }))
            }
        }
    }
}

/** Outcome of reading a team list. */
sealed interface RosterInput {
    data class Valid(val roster: TeamRoster) : RosterInput

    /** No team name at all once empty entries are dropped. */
    data object Empty : RosterInput

    /** [name] contains `[`, `]` or `;`. */
    data class InvalidName(val name: String) : RosterInput

    /** [name] is longer than [TeamRoster.MAX_TEAM_NAME_BYTES]. */
    data class NameTooLong(val name: String) : RosterInput

    /** The whole message would be [bytes] long, more than [TeamRoster.MAX_MESSAGE_BYTES]. */
    data class MessageTooLong(val bytes: Int) : RosterInput
}
