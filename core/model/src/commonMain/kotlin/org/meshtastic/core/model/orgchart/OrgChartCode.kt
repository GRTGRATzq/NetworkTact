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

import org.meshtastic.core.model.team.TeamRoster
import org.meshtastic.core.model.utf8Size

/**
 * The text an organisation chart travels in, inside a QR code shown by the command post and scanned by the other
 * phones; nothing of it goes over the radio. One line per entry:
 * ```
 * NTORGA1
 * A|PC-0|1791720000
 * P|PC-0|Chef PC
 * T|Alpha
 * M|ALPHA-1|Chef d%C3%A9quipe
 * ```
 *
 * `A` gives the author and the time of the last change (Unix seconds), `P` a command post member, `T` a team and `M` a
 * member of the team above it. The text is plain ASCII: `%`, `|` and every byte outside printable ASCII are written as
 * `%XX` of their UTF-8 bytes, so no scanner can misread the character set.
 *
 * A chart with a fire chief starts with [HEADER_WITH_CHIEF] instead, and its second line after the author's is `F|` and
 * the chief's call sign (`F|ALPHA-1`). The line names the chief by call sign only and is read on its own, apart from
 * the member lines, so it stays as it is whatever form the member lines take later. Without a chief the code is the
 * [HEADER] one, byte for byte, which phones older than the fire chief still read; they refuse the other whole.
 * `NTORGA2` is kept for the expected LoRa setting (not written yet), hence the jump to 3.
 */
object OrgChartCode {
    const val HEADER = "NTORGA1"

    /** The header of a chart that designates a fire chief. */
    const val HEADER_WITH_CHIEF = "NTORGA3"

    /**
     * Longest code, in characters. With the medium error correction the screen uses, it stays a version 22 QR code (105
     * × 105 modules), which a phone camera reads from another phone's screen. Nine members with call signs and roles of
     * the longest ASCII length fit; a chart beyond this is refused when it is made, not when it is scanned.
     */
    const val MAX_CODE_CHARS = 770

    internal const val SEPARATOR = '|'
    private const val MAX_TIME_DIGITS = 12
    private val FORBIDDEN_IN_TEAM = charArrayOf('[', ']', TeamRoster.SEPARATOR)

    /** Whether [text] is meant as an organisation chart. */
    fun isOrgChartCode(text: String): Boolean = text.startsWith(HEADER) || text.startsWith(HEADER_WITH_CHIEF)

    fun encode(chart: OrgChart): String = buildList {
        add(if (chart.fireChief.isEmpty()) HEADER else HEADER_WITH_CHIEF)
        add(line('A', chart.author, chart.createdSeconds.toString()))
        if (chart.fireChief.isNotEmpty()) add(line('F', chart.fireChief))
        chart.commandPost.forEach { add(line('P', it.callsign, it.role)) }
        chart.teams.forEach { team ->
            add(line('T', team.name))
            team.members.forEach { add(line('M', it.callsign, it.role)) }
        }
    }
        .joinToString("\n")

    /** Whether [chart] fits in a readable QR code, and the length of its code either way. */
    fun size(chart: OrgChart): CodeSize = encode(chart).length.let { CodeSize(it, it <= MAX_CODE_CHARS) }

    /**
     * The chart [text] carries, or null when it is not one or is malformed in any part: a damaged code never installs
     * half a chart.
     */
    fun decode(text: String): OrgChart? {
        val withChief = text.startsWith(HEADER_WITH_CHIEF)
        val header = if (withChief) HEADER_WITH_CHIEF else HEADER
        val headerLines = if (withChief) HEAD_LINES_WITH_CHIEF else HEAD_LINES
        val lines =
            text.split('\n').takeIf { text.length <= MAX_CODE_CHARS && it.size >= headerLines && it[0] == header }
        val head = lines?.let { fields(it[1], 'A', 2) }
        val author = head?.get(0)?.takeIf { it.utf8Size() <= OrgChart.MAX_CALLSIGN_BYTES }
        val created = head?.get(1)?.takeIf { it.length in 1..MAX_TIME_DIGITS && it.all { c -> c in '0'..'9' } }
        val chief = if (withChief) lines?.let { chiefLine(it[2]) } else ""
        val body = lines?.let { BodyParser().takeIf { parser -> it.drop(headerLines).all(parser::add) } }
        val chart =
            if (author != null && created != null && chief != null && body != null) {
                OrgChart(OrgChart.clean(author), created.toLong(), body.commandPost, body.teams).withChief(chief)
            } else {
                null
            }
        return chart?.takeIf(::isConsistent)
    }

    /** The call sign an `F|` line names, or null when the line is anything else. */
    private fun chiefLine(raw: String): String? = fields(raw, 'F', 1)?.single()?.let(OrgChart::clean)?.takeIf {
        it.isNotEmpty() && it.utf8Size() <= OrgChart.MAX_CALLSIGN_BYTES
    }

    /**
     * [this] with the member called [chief] (case ignored) as fire chief, spelled as the member is; null when no member
     * has that call sign: a chief outside the chart refuses the whole code.
     */
    private fun OrgChart.withChief(chief: String): OrgChart? = if (chief.isEmpty()) {
        this
    } else {
        allMembers
            .firstOrNull { Callsign.sameAs(it.second.callsign, chief) }
            ?.let { copy(fireChief = it.second.callsign) }
    }

    /** The header line and the author's, then the chief's when there is one. */
    private const val HEAD_LINES = 2
    private const val HEAD_LINES_WITH_CHIEF = 3

    /** Reads the lines after the author's, in order: command post members first, then each team and its members. */
    private class BodyParser {
        val commandPost = mutableListOf<OrgMember>()
        val teams = mutableListOf<OrgTeam>()

        fun add(raw: String): Boolean = when (raw.firstOrNull()) {
            'P' -> teams.isEmpty() && member(raw, 'P')?.also { commandPost += it } != null

            'T' -> team(raw)?.also { teams += OrgTeam(it, emptyList()) } != null

            'M' -> {
                val member = member(raw, 'M')
                val last = teams.lastOrNull()
                if (member != null && last != null) {
                    teams[teams.lastIndex] = last.copy(members = last.members + member)
                }
                member != null && last != null
            }

            else -> false
        }
    }

    /** No two teams with one name, no two members with one call sign, and not more members than allowed. */
    private fun isConsistent(chart: OrgChart): Boolean {
        val callsigns = chart.allMembers.map { Callsign.key(it.second.callsign) }
        val teamNames = chart.teams.map { it.name.lowercase() }
        return callsigns.size <= OrgChart.MAX_MEMBERS &&
            callsigns.distinct().size == callsigns.size &&
            teamNames.distinct().size == teamNames.size
    }

    private fun member(raw: String, tag: Char): OrgMember? {
        val (callsign, role) = fields(raw, tag, 2) ?: return null
        return (OrgChart.checkMember(callsign, role, emptyList()) as? MemberInput.Valid)?.member
    }

    private fun team(raw: String): String? {
        val name = fields(raw, 'T', 1)?.single()?.let(OrgChart::clean) ?: return null
        val valid =
            name.isNotEmpty() &&
                name.utf8Size() <= TeamRoster.MAX_TEAM_NAME_BYTES &&
                name.none { it in FORBIDDEN_IN_TEAM }
        return name.takeIf { valid }
    }

    private fun line(tag: Char, vararg values: String): String =
        (listOf(tag.toString()) + values.map(CodeEscape::escape)).joinToString(SEPARATOR.toString())

    /** The [count] values of a line tagged [tag], unescaped, or null when the line is anything else. */
    private fun fields(raw: String, tag: Char, count: Int): List<String>? {
        val parts = raw.split(SEPARATOR)
        if (parts.size != count + 1 || parts[0] != tag.toString()) return null
        val values = parts.drop(1).map(CodeEscape::unescape)
        return values.takeIf { it.none { value -> value == null } }?.map { it.orEmpty() }
    }
}

/** A chart's code: its [length] in characters, and whether it [fits] in a readable QR code. */
data class CodeSize(val length: Int, val fits: Boolean)
