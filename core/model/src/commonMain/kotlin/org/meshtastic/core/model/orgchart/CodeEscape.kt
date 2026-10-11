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
package org.meshtastic.core.model.orgchart

/**
 * The escaping of [OrgChartCode]: `%`, the field separator and every byte outside printable ASCII become `%XX` of their
 * UTF-8 bytes, so the code is plain ASCII.
 */
internal object CodeEscape {
    private const val ESCAPE = '%'
    private const val HEX_RADIX = 16
    private const val FIRST_PRINTABLE = 0x20
    private const val LAST_PRINTABLE = 0x7E
    private const val BYTE_MASK = 0xFF
    private const val ESCAPE_LENGTH = 3

    fun escape(value: String): String = buildString {
        value.encodeToByteArray().forEach { byte ->
            val code = byte.toInt() and BYTE_MASK
            val char = code.toChar()
            if (code in FIRST_PRINTABLE..LAST_PRINTABLE && char != ESCAPE && char != OrgChartCode.SEPARATOR) {
                append(char)
            } else {
                append(ESCAPE).append(code.toString(HEX_RADIX).uppercase().padStart(2, '0'))
            }
        }
    }

    /** [value] with its `%XX` decoded, or null when an escape is broken or the bytes are not UTF-8. */
    fun unescape(value: String): String? {
        val bytes = ByteArray(value.length)
        var size = 0
        var index = 0
        var valid = true
        while (valid && index < value.length) {
            val char = value[index]
            val code =
                if (char == ESCAPE) {
                    hexByte(value.substring(index + 1, minOf(index + ESCAPE_LENGTH, value.length)))
                } else {
                    char.code.takeIf { it in FIRST_PRINTABLE..LAST_PRINTABLE }
                }
            valid = code != null
            if (code != null) bytes[size++] = code.toByte()
            index += if (char == ESCAPE) ESCAPE_LENGTH else 1
        }
        return if (valid) utf8(bytes.copyOf(size)) else null
    }

    private fun hexByte(digits: String): Int? = digits
        .takeIf { it.length == 2 && it.all { c -> c in '0'..'9' || c in 'A'..'F' || c in 'a'..'f' } }
        ?.toInt(HEX_RADIX)

    private fun utf8(bytes: ByteArray): String? = try {
        bytes.decodeToString(throwOnInvalidSequence = true)
    } catch (_: CharacterCodingException) {
        null
    }
}
