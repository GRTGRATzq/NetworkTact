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
 * « Accusé DT »: a post that received request [number] in full on a channel tells its writer, in a direct message, at
 * [gdh]: `[DT AB1234] Accusé DT 112251 loc NOV 26`. Sent by hand only, once per request and post.
 */
data class FireRequestAck(val number: String, val gdh: String) {

    fun text(): String = "[${FireRequestSheet.TAG} $number] $LABEL ${FireRequestText.clean(gdh)}"

    companion object {
        const val LABEL = "Accusé DT"
        private val ACK = Regex("""^\[DT ([A-Z0-9]{1,10})] $LABEL (\S.*)$""")

        fun parse(text: String): FireRequestAck? =
            ACK.find(text.trim())?.destructured?.let { (number, gdh) -> FireRequestAck(number, gdh.trim()) }
    }
}

/**
 * The fire chief tells the radio they allocated request [number] to, in a direct message: `[DT AB1234] Tir attribué à
 * vous par OA · GDH attribution 112252 loc NOV 26`. [requester] is the short name of the request's writer, to whom that
 * radio sends « GDH fin »; the chief is the message's sender.
 */
data class FireRequestDesignation(val number: String, val requester: String, val gdh: String) {

    fun text(): String = "[${FireRequestSheet.TAG} $number] $TO_YOU ${FireRequestText.clean(requester)} · " +
        "${ReplyKind.ALLOCATION.label} ${FireRequestText.clean(gdh)}"

    companion object {
        const val TO_YOU = "Tir attribué à vous par"
        private val DESIGNATION = Regex("""^\[DT ([A-Z0-9]{1,10})] $TO_YOU (\S.*?) · GDH attribution (\S.*)$""")

        fun parse(text: String): FireRequestDesignation? =
            DESIGNATION.find(text.trim())?.destructured?.let { (number, requester, gdh) ->
                FireRequestDesignation(number, requester.trim(), gdh.trim())
            }
    }
}
