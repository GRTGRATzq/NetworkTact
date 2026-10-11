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

import org.meshtastic.core.model.team.TeamSuffix

/** A radio that carries a member's call sign, and the team its own long name declares. */
data class LinkedRadio<T>(val radio: T, val declaredTeam: String?, val teamMatches: Boolean)

/**
 * A member in its place: [team] (null for the command post), [rank] from 1, and every radio carrying its call sign.
 * More than one radio means several carry the same call sign: all are shown, none is chosen for the user.
 */
data class MemberLink<T>(val team: String?, val rank: Int, val member: OrgMember, val radios: List<LinkedRadio<T>>)

/** Every member with its radios, and the radios whose call sign no member has. */
data class ChartLinks<T>(val members: List<MemberLink<T>>, val unlisted: List<T>)

/**
 * Ties members to radios by call sign alone, never by hardware id: a replaced radio that takes the same call sign takes
 * the same place. Radios keep the order they are given in.
 */
object OrgChartLinks {
    fun <T> link(chart: OrgChart, radios: Collection<T>, longName: (T) -> String): ChartLinks<T> {
        val named = radios.filter { longName(it).isNotBlank() }
        val byCallsign = named.groupBy { Callsign.key(Callsign.ofLongName(longName(it))) }
        val members =
            chart.allMembers.groupBy({ it.first }, { it.second }).flatMap { (team, members) ->
                members.mapIndexed { index, member ->
                    val carriers = byCallsign[Callsign.key(member.callsign)].orEmpty()
                    MemberLink(team, index + 1, member, carriers.map { linked(it, longName(it), team) })
                }
            }
        val listed = chart.allMembers.map { Callsign.key(it.second.callsign) }.toSet()
        val unlisted = named.filter { Callsign.key(Callsign.ofLongName(longName(it))) !in listed }
        return ChartLinks(members, unlisted)
    }

    private fun <T> linked(radio: T, longName: String, team: String?): LinkedRadio<T> {
        val declared = TeamSuffix.teamOf(longName)
        val matches = if (team == null) declared == null else declared.equals(team, ignoreCase = true)
        return LinkedRadio(radio, declared, matches)
    }
}
