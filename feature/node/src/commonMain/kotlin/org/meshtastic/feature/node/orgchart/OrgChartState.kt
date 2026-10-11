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
package org.meshtastic.feature.node.orgchart

import org.meshtastic.core.model.Node
import org.meshtastic.core.model.freshness.ContactState
import org.meshtastic.core.model.freshness.FreshnessThresholds
import org.meshtastic.core.model.freshness.NodeFreshness
import org.meshtastic.core.model.freshness.PositionState
import org.meshtastic.core.model.orgchart.LinkedRadio
import org.meshtastic.core.model.orgchart.OrgChartLinks
import org.meshtastic.core.model.orgchart.OrgChartRecord
import org.meshtastic.core.model.orgchart.OrgMember
import org.meshtastic.feature.node.commandpost.directMessageKey
import org.meshtastic.feature.node.model.canDirectMessage

/** A radio carrying a member's call sign, with what the node database says of it, as the command post view shows. */
data class OrgRadioRow(
    val num: Int,
    val longName: String,
    val isMine: Boolean,
    val declaredTeam: String?,
    val teamMatches: Boolean,
    val contact: ContactState,
    val position: PositionState,
    /** Contact key of the existing direct conversation, or null when a direct message cannot be delivered. */
    val directMessageKey: String?,
)

/** A member in its place. [index] is its position in its list, [rank] the number shown (index + 1). */
data class OrgMemberRow(
    val team: String?,
    val index: Int,
    val rank: Int,
    val member: OrgMember,
    val radios: List<OrgRadioRow>,
)

/** The command post ([team] null) or a team. [inTeamList] is false for a team the adopted team list no longer holds. */
data class OrgSection(val team: String?, val inTeamList: Boolean, val members: List<OrgMemberRow>)

data class OrgChartUiState(
    val record: OrgChartRecord? = null,
    val previous: OrgChartRecord? = null,
    val sections: List<OrgSection> = emptyList(),
    /** Radios whose call sign no member has. */
    val unlisted: List<OrgRadioRow> = emptyList(),
    val commandPostMode: Boolean = false,
)

/**
 * Lays the chart out: the command post, then the teams of the adopted team list in its order (empty ones too, so the
 * command post can fill them), then the chart's teams the list no longer holds. Without any chart, and outside command
 * post mode, there is nothing to lay out. Ignored nodes are left out; this phone's own radio is kept, it may be a
 * member. Radios come most recently heard first.
 */
fun buildOrgChartState(
    record: OrgChartRecord?,
    previous: OrgChartRecord?,
    teamList: List<String>,
    nodes: Collection<Node>,
    ourNode: Node?,
    nowSeconds: Long,
    commandPostMode: Boolean,
    thresholds: FreshnessThresholds = FreshnessThresholds.Default,
): OrgChartUiState {
    val chart = record?.chart
    val radios =
        nodes.filter { !it.isIgnored }.sortedWith(compareByDescending<Node> { it.lastHeard.toUInt() }.thenBy { it.num })
    val links = chart?.let { OrgChartLinks.link(it, radios) { node -> node.user.long_name } }
    fun row(linked: LinkedRadio<Node>) = radioRow(linked, ourNode, nowSeconds, thresholds)
    val members = links?.members.orEmpty().groupBy { it.team }
    val teams = teamList + chart?.teams.orEmpty().map { it.name }.filter { name -> teamList.none { it == name } }
    val sections =
        if (chart == null && !commandPostMode) {
            emptyList()
        } else {
            (listOf<String?>(null) + teams).map { team ->
                OrgSection(
                    team = team,
                    inTeamList = team == null || team in teamList,
                    members =
                    members[team].orEmpty().map { link ->
                        OrgMemberRow(team, link.rank - 1, link.rank, link.member, link.radios.map(::row))
                    },
                )
            }
        }
    val unlisted = links?.unlisted.orEmpty().map { row(LinkedRadio(it, null, teamMatches = true)) }
    return OrgChartUiState(record, previous, sections, unlisted, commandPostMode)
}

private fun radioRow(
    linked: LinkedRadio<Node>,
    ourNode: Node?,
    nowSeconds: Long,
    thresholds: FreshnessThresholds,
): OrgRadioRow {
    val node = linked.radio
    val isMine = node.num == ourNode?.num
    return OrgRadioRow(
        num = node.num,
        longName = node.user.long_name,
        isMine = isMine,
        declaredTeam = linked.declaredTeam,
        teamMatches = linked.teamMatches,
        contact = NodeFreshness.contact(node, nowSeconds, thresholds),
        position = NodeFreshness.position(node, nowSeconds, thresholds),
        directMessageKey = node.takeIf { !isMine && it.canDirectMessage }?.let { directMessageKey(it, ourNode) },
    )
}
