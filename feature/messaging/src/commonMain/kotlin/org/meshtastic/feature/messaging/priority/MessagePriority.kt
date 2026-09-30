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
package org.meshtastic.feature.messaging.priority

/**
 * Priority of a text message, carried purely by a plain-text prefix so the message stays readable in any other
 * Meshtastic client: `[URG]` is urgent, `[CR]` a report (compte rendu), anything else is information.
 */
enum class MessagePriority(val prefix: String?) {
    INFO(null),
    REPORT("[CR]"),
    URGENT("[URG]"),
    ;

    companion object {
        /**
         * The priority [text] declares. Only a prefix at the very start counts; leading whitespace is ignored and the
         * tag is matched case-insensitively. A tag anywhere else in the text is ordinary content.
         */
        fun of(text: CharSequence): MessagePriority {
            val body = text.trimStart()
            return entries.firstOrNull { priority ->
                priority.prefix != null && body.startsWith(priority.prefix, ignoreCase = true)
            } ?: INFO
        }

        /**
         * [text] with its priority prefix replaced by [priority]'s: any existing prefix and the spaces after it are
         * removed, then the new prefix and one space are prepended. [INFO] leaves the text without a prefix.
         */
        fun withPriority(text: String, priority: MessagePriority): String {
            val body = stripPrefix(text)
            return priority.prefix?.let { "$it $body" } ?: body
        }

        /** [text] without its leading priority prefix, or [text] unchanged when it has none. */
        fun stripPrefix(text: String): String {
            val trimmed = text.trimStart()
            val prefix = of(trimmed).prefix ?: return text
            return trimmed.substring(prefix.length).trimStart()
        }
    }
}
