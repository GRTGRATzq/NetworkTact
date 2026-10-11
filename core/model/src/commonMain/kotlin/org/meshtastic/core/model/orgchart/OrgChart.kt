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

import kotlinx.serialization.Serializable
import org.meshtastic.core.model.team.TeamSuffix
import org.meshtastic.core.model.utf8Size

/**
 * One person of the organisation chart. [callsign] is the start of a radio's long name, without its team suffix
 * (`ALPHA-1` for `ALPHA-1 [Alpha]`): a member is tied to whichever radio carries that call sign, never to a hardware
 * id, so a replaced radio finds its place again. [role] is free text ("chef d'équipe", "radio"), possibly empty.
 */
@Serializable data class OrgMember(val callsign: String, val role: String = "")

/** A team of the chart and its members, in rank order: the first is rank 1. */
@Serializable data class OrgTeam(val name: String, val members: List<OrgMember>)

/**
 * The chain of command: the command post (PC) and its members, then the teams and theirs. Ranks are the order of the
 * lists. [author] is the call sign of the phone's radio that made it, empty when unknown, and [createdSeconds] the time
 * of its last change.
 */
@Serializable
data class OrgChart(
    val author: String,
    val createdSeconds: Long,
    val commandPost: List<OrgMember>,
    val teams: List<OrgTeam>,
) {
    val memberCount: Int
        get() = commandPost.size + teams.sumOf { it.members.size }

    /** Every member with the team it belongs to, null for the command post. */
    val allMembers: List<Pair<String?, OrgMember>>
        get() = commandPost.map { null to it } + teams.flatMap { team -> team.members.map { team.name to it } }

    /** The members of [team] (null for the command post), empty when the chart does not hold it. */
    fun membersOf(team: String?): List<OrgMember> =
        if (team == null) commandPost else teams.firstOrNull { it.name == team }?.members.orEmpty()

    /** Same people in the same places, whoever made it and whenever: nothing to adopt when it is the same. */
    fun sameStructureAs(other: OrgChart?): Boolean =
        other != null && commandPost == other.commandPost && teams == other.teams

    companion object {
        /** A call sign fills at most a whole long name ([TeamSuffix.LONG_NAME_MAX_BYTES]). */
        const val MAX_CALLSIGN_BYTES = TeamSuffix.LONG_NAME_MAX_BYTES

        /** Long enough for "chef d'équipe adjoint", short enough to keep the QR code readable. */
        const val MAX_ROLE_BYTES = 24

        /** Well above the 9 radios in use; the QR code size is the real limit ([OrgChartCode.MAX_CODE_CHARS]). */
        const val MAX_MEMBERS = 40

        val EMPTY = OrgChart(author = "", createdSeconds = 0L, commandPost = emptyList(), teams = emptyList())

        private val WHITESPACE = Regex("\\s+")

        /** [text] trimmed and with its inner whitespace collapsed, as call signs and roles are stored. */
        fun clean(text: String): String = text.trim().replace(WHITESPACE, " ")

        /**
         * Checks a member as typed. [others] are the call signs already in the chart: two members with one call sign
         * could not tell their radios apart.
         */
        fun checkMember(callsign: String, role: String, others: Collection<String>): MemberInput {
            val sign = clean(callsign)
            val function = clean(role)
            return when {
                sign.isEmpty() -> MemberInput.EmptyCallsign
                sign.utf8Size() > MAX_CALLSIGN_BYTES -> MemberInput.CallsignTooLong(sign.utf8Size())
                function.utf8Size() > MAX_ROLE_BYTES -> MemberInput.RoleTooLong(function.utf8Size())
                others.any { Callsign.sameAs(it, sign) } -> MemberInput.DuplicateCallsign(sign)
                else -> MemberInput.Valid(OrgMember(sign, function))
            }
        }
    }
}

sealed interface MemberInput {
    data class Valid(val member: OrgMember) : MemberInput

    data object EmptyCallsign : MemberInput

    data class CallsignTooLong(val bytes: Int) : MemberInput

    data class RoleTooLong(val bytes: Int) : MemberInput

    /** Another member already has [callsign], ignoring case. */
    data class DuplicateCallsign(val callsign: String) : MemberInput
}
