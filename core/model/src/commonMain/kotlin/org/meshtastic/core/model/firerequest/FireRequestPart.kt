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
package org.meshtastic.core.model.firerequest

/**
 * One part of a fire request as received: request [number], part [k] of [count], and the [lines] after its tag. Only a
 * text that starts with `[DT <number> <k>/<N>]` and a line break, with a number of 1 to 10 capital letters and digits
 * and 1 ≤ k ≤ N ≤ 9, is a part; any other text is an ordinary message.
 */
data class FireRequestPart(val number: String, val k: Int, val count: Int, val lines: List<String>) {
    companion object {
        private val TAG = Regex("""^\[DT ([A-Z0-9]{1,10}) ([1-9])/([1-9])]\n""")

        fun parse(text: String): FireRequestPart? {
            val match = TAG.find(text) ?: return null
            val (number, k, count) = match.destructured
            val lines = text.substring(match.range.last + 1).lines().filter { it.isNotBlank() }
            return FireRequestPart(number, k.toInt(), count.toInt(), lines).takeIf { it.k <= it.count }
        }
    }
}

/** The two date-time groups the recipient sends back. */
enum class ReplyKind(val label: String) {
    ALLOCATION(FireRequestSheet.ALLOCATION_GDH),
    END(FireRequestSheet.END_GDH),
}

/**
 * The recipient's answer to request [number]: `[DT AB1234] GDH attribution 112252 loc NOV 26`, or `GDH fin`. [gdh] is
 * the date-time group as written; it is shown as it came, even when it does not read as a [Gdh].
 */
data class FireRequestReply(val number: String, val kind: ReplyKind, val gdh: String) {

    /** The message sent. */
    fun text(): String = "[${FireRequestSheet.TAG} $number] ${kind.label} ${FireRequestText.clean(gdh)}"

    companion object {
        private val REPLY = Regex("""^\[DT ([A-Z0-9]{1,10})] GDH (attribution|fin) (\S.*)$""")

        fun parse(text: String): FireRequestReply? {
            val match = REPLY.find(text.trim()) ?: return null
            val (number, kind, gdh) = match.destructured
            return FireRequestReply(number, if (kind == "fin") ReplyKind.END else ReplyKind.ALLOCATION, gdh.trim())
        }
    }
}
