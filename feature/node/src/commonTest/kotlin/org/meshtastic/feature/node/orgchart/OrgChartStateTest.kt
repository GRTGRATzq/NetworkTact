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

import okio.ByteString.Companion.toByteString
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.NodeAddress
import org.meshtastic.core.model.freshness.ContactState
import org.meshtastic.core.model.freshness.PositionState
import org.meshtastic.core.model.orgchart.OrgChart
import org.meshtastic.core.model.orgchart.OrgChartRecord
import org.meshtastic.core.model.orgchart.OrgMember
import org.meshtastic.core.model.orgchart.OrgTeam
import org.meshtastic.proto.Position
import org.meshtastic.proto.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OrgChartStateTest {

    private val now = 1_790_000_000L
    private val key = ByteArray(32) { 0x2B.toByte() }.toByteString()

    private fun node(num: Int, longName: String, heardAgo: Int = 60, fixAgo: Int? = null, ignored: Boolean = false) =
        Node(
            num = num,
            user =
            User.Builder()
                .also { wb ->
                    wb.id = NodeAddress.numToDefaultId(num)
                    wb.long_name = longName
                    wb.short_name = longName.take(4)
                    wb.public_key = key
                }
                .build(),
            lastHeard = (now - heardAgo).toInt(),
            isIgnored = ignored,
            position =
            Position.Builder()
                .also { wb ->
                    if (fixAgo != null) {
                        wb.latitude_i = 488_566_000
                        wb.longitude_i = 23_522_000
                        wb.timestamp = (now - fixAgo).toInt()
                    }
                }
                .build(),
        )

    private val chart =
        OrgChartRecord(
            OrgChart(
                author = "PC-0",
                createdSeconds = now - 600,
                commandPost = listOf(OrgMember("PC-0", "Chef PC")),
                teams =
                listOf(
                    OrgTeam("Alpha", listOf(OrgMember("ALPHA-1", "Chef d'équipe"), OrgMember("CHARLIE-3"))),
                    OrgTeam("Delta", listOf(OrgMember("DELTA-4"))),
                ),
            ),
            OrgChartRecord.Source.SCANNED,
            timeSeconds = now - 60,
        )

    private val pc = node(100, "PC-0", heardAgo = 0)

    private fun build(
        nodes: List<Node>,
        record: OrgChartRecord? = chart,
        teamList: List<String> = listOf("Alpha", "Bravo"),
        commandPostMode: Boolean = false,
    ) = buildOrgChartState(record, null, teamList, nodes, pc, now, commandPostMode)

    private fun OrgChartUiState.member(callsign: String) =
        sections.flatMap { it.members }.single { it.member.callsign == callsign }

    @Test
    fun sectionsAreTheCommandPostTheTeamListThenTeamsNoLongerInIt() {
        val state = build(listOf(pc))
        assertEquals(listOf(null, "Alpha", "Bravo", "Delta"), state.sections.map { it.team })
        assertEquals(listOf(true, true, true, false), state.sections.map { it.inTeamList })
        assertTrue(state.sections.single { it.team == "Bravo" }.members.isEmpty())
        assertEquals(listOf(1, 2), state.sections.single { it.team == "Alpha" }.members.map { it.rank })
    }

    @Test
    fun aMemberShowsItsRadiosFreshnessByCallSign() {
        val state = build(listOf(pc, node(1, "ALPHA-1 [Alpha]", heardAgo = 120, fixAgo = 120)))
        val radio = state.member("ALPHA-1").radios.single()
        assertEquals(1, radio.num)
        assertIs<ContactState.SeenRecently>(radio.contact)
        assertIs<PositionState.Fresh>(radio.position)
        assertTrue(radio.teamMatches)
        assertEquals("${NodeAddress.PKC_CHANNEL_INDEX}${NodeAddress.numToDefaultId(1)}", radio.directMessageKey)
    }

    @Test
    fun aReplacedRadioWithTheSameCallSignTakesTheSamePlace() {
        val replaced = build(listOf(pc, node(0x77, "ALPHA-1 [Alpha]")))
        assertEquals(listOf(0x77), replaced.member("ALPHA-1").radios.map { it.num })
        assertEquals(1, replaced.member("ALPHA-1").rank)
    }

    @Test
    fun aMemberWithoutRadioHasNone() {
        assertTrue(build(listOf(pc)).member("CHARLIE-3").radios.isEmpty())
    }

    @Test
    fun myOwnRadioIsAMemberWithoutAConversation() {
        val radio = build(listOf(pc)).member("PC-0").radios.single()
        assertTrue(radio.isMine)
        assertNull(radio.directMessageKey)
    }

    @Test
    fun ignoredRadiosAreLeftOutAndUnclaimedOnesListed() {
        val state = build(listOf(pc, node(1, "ALPHA-1", ignored = true), node(9, "ECHO-5 [Bravo]")))
        assertTrue(state.member("ALPHA-1").radios.isEmpty())
        assertEquals(listOf(9), state.unlisted.map { it.num })
    }

    @Test
    fun theMostRecentlyHeardRadioComesFirst() {
        val state = build(listOf(pc, node(1, "ALPHA-1", heardAgo = 900), node(2, "alpha-1 [Alpha]", heardAgo = 30)))
        assertEquals(listOf(2, 1), state.member("ALPHA-1").radios.map { it.num })
        assertEquals(listOf(true, false), state.member("ALPHA-1").radios.map { it.teamMatches })
    }

    @Test
    fun withoutAChartOnlyTheCommandPostGetsEmptySectionsToFill() {
        assertTrue(build(listOf(pc), record = null).sections.isEmpty())
        val pcMode = build(listOf(pc), record = null, commandPostMode = true)
        assertEquals(listOf(null, "Alpha", "Bravo"), pcMode.sections.map { it.team })
        assertTrue(pcMode.sections.all { it.members.isEmpty() })
    }
}
