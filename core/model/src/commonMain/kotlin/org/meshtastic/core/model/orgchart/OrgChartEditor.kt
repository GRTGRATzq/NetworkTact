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

/** A change the command post makes to its chart. [team] is null for the command post itself. */
sealed interface OrgChartEdit {
    val team: String?

    data class Add(override val team: String?, val callsign: String, val role: String) : OrgChartEdit

    /** Replaces the member at [index] of [team]. */
    data class Update(override val team: String?, val index: Int, val callsign: String, val role: String) : OrgChartEdit

    data class Remove(override val team: String?, val index: Int) : OrgChartEdit

    /** Moves the member at [index] one rank up (towards rank 1) or down. */
    data class Move(override val team: String?, val index: Int, val up: Boolean) : OrgChartEdit
}

sealed interface EditResult {
    /** The new chart, stamped with its author and the time of the change. */
    data class Changed(val chart: OrgChart) : EditResult

    data class Invalid(val reason: MemberInput) : EditResult

    /** The chart would no longer fit in a readable QR code: its code would be [length] characters long. */
    data class TooLarge(val length: Int) : EditResult

    /** Nothing to change: an index out of range, or a move past the first or last rank. */
    data object Unchanged : EditResult
}

object OrgChartEditor {
    /**
     * Applies [edit] to [chart] (null when there is none yet). An addition or a replacement is refused when it would
     * push the chart past [OrgChart.MAX_MEMBERS] or past what a readable QR code holds; a removal or a move never is,
     * so a chart can always be brought back within the limits. A team whose last member leaves is dropped.
     */
    fun apply(chart: OrgChart?, edit: OrgChartEdit, author: String, nowSeconds: Long): EditResult {
        val base = chart ?: OrgChart.EMPTY
        val members = base.membersOf(edit.team)
        val result =
            when (edit) {
                is OrgChartEdit.Add -> checked(base, edit.team, null, edit.callsign, edit.role)

                is OrgChartEdit.Update ->
                    if (edit.index in members.indices) {
                        checked(base, edit.team, edit.index, edit.callsign, edit.role)
                    } else {
                        EditResult.Unchanged
                    }

                is OrgChartEdit.Remove ->
                    members
                        .takeIf { edit.index in it.indices }
                        ?.let { list ->
                            EditResult.Changed(
                                base.withMembers(edit.team, list.filterIndexed { i, _ -> i != edit.index }),
                            )
                        } ?: EditResult.Unchanged

                is OrgChartEdit.Move -> moved(base, members, edit)
            }
        return when (result) {
            is EditResult.Changed -> stamped(result.chart, edit, author, nowSeconds)
            else -> result
        }
    }

    /** Adds a member to [team] ([index] null) or replaces the one at [index], once the input is valid. */
    private fun checked(base: OrgChart, team: String?, index: Int?, callsign: String, role: String): EditResult {
        val members = base.membersOf(team)
        val others = base.allMembers.map { it.second.callsign } - setOfNotNull(index?.let { members[it].callsign })
        val input = OrgChart.checkMember(callsign, role, others)
        return when {
            input !is MemberInput.Valid -> EditResult.Invalid(input)

            index == null && base.memberCount >= OrgChart.MAX_MEMBERS ->
                EditResult.TooLarge(OrgChartCode.size(base).length)

            index == null -> EditResult.Changed(base.withMembers(team, members + input.member))

            else ->
                EditResult.Changed(base.withMembers(team, members.toMutableList().also { it[index] = input.member }))
        }
    }

    private fun moved(base: OrgChart, members: List<OrgMember>, edit: OrgChartEdit.Move): EditResult {
        val target = if (edit.up) edit.index - 1 else edit.index + 1
        return if (edit.index in members.indices && target in members.indices) {
            val list = members.toMutableList().also { it.add(target, it.removeAt(edit.index)) }
            EditResult.Changed(base.withMembers(edit.team, list))
        } else {
            EditResult.Unchanged
        }
    }

    private fun stamped(chart: OrgChart, edit: OrgChartEdit, author: String, nowSeconds: Long): EditResult {
        val stampedChart = chart.copy(author = OrgChart.clean(author), createdSeconds = nowSeconds)
        val size = OrgChartCode.size(stampedChart)
        val grows = edit is OrgChartEdit.Add || edit is OrgChartEdit.Update
        return if (grows && !size.fits) EditResult.TooLarge(size.length) else EditResult.Changed(stampedChart)
    }

    /** [chart] with [members] for [team]: a new team goes last, an emptied one is dropped. */
    private fun OrgChart.withMembers(team: String?, members: List<OrgMember>): OrgChart = when {
        team == null -> copy(commandPost = members)
        teams.none { it.name == team } -> copy(teams = teams + OrgTeam(team, members))
        members.isEmpty() -> copy(teams = teams.filterNot { it.name == team })
        else -> copy(teams = teams.map { if (it.name == team) it.copy(members = members) else it })
    }
}
