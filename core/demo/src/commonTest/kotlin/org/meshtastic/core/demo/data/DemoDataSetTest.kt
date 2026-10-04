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
package org.meshtastic.core.demo.data

import kotlinx.datetime.TimeZone
import org.meshtastic.core.model.MessageStatus
import org.meshtastic.core.model.freshness.NodeFreshness
import org.meshtastic.core.model.freshness.PositionState
import org.meshtastic.core.model.team.TeamSuffix
import org.meshtastic.core.model.util.getChannel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DemoDataSetTest {

    private val now = 1_790_000_000_000L
    private val data = DemoDataSet.create(now, TimeZone.UTC)
    private val nowSeconds = now / 1_000

    private fun node(num: Int) = data.nodes.single { it.num == num }

    private fun texts() = data.packets.mapNotNull { it.packet.text }

    @Test
    fun fourFictitiousNodesWithTheirTeams() {
        assertEquals(
            listOf("PC-0", "ALPHA-1 [Alpha]", "BRAVO-2 [Bravo]", "CHARLIE-3 [Alpha]"),
            data.nodes.map { it.user.long_name },
        )
        assertEquals(DemoDataSet.PC0_NUM, data.myNodeInfo.myNodeNum)
        assertEquals("Alpha", TeamSuffix.teamOf(node(DemoDataSet.ALPHA1_NUM).user.long_name))
        assertEquals("Bravo", TeamSuffix.teamOf(node(DemoDataSet.BRAVO2_NUM).user.long_name))
        assertEquals("Alpha", TeamSuffix.teamOf(node(DemoDataSet.CHARLIE3_NUM).user.long_name))
        assertEquals(listOf("Alpha", "Bravo"), data.roster.teams)
    }

    @Test
    fun positionsAreFreshOldAndWithoutFixTimeForTheCommandPostView() {
        assertIs<PositionState.Fresh>(NodeFreshness.position(node(DemoDataSet.ALPHA1_NUM), nowSeconds))
        assertIs<PositionState.Stale>(NodeFreshness.position(node(DemoDataSet.BRAVO2_NUM), nowSeconds))
        assertEquals(PositionState.NoFixTime, NodeFreshness.position(node(DemoDataSet.CHARLIE3_NUM), nowSeconds))
    }

    @Test
    fun threeNamedChannels() {
        assertEquals("Général", data.channelSet.getChannel(0)?.settings?.name)
        assertEquals("Équipe Alpha", data.channelSet.getChannel(1)?.settings?.name)
        assertEquals("PC", data.channelSet.getChannel(2)?.settings?.name)
    }

    @Test
    fun everyKindOfMessageIsStaged() {
        val texts = texts()
        assertTrue(texts.any { !it.startsWith("[") }, "information")
        assertTrue(texts.any { it.startsWith("[CR] ") && !it.startsWith("[CR] FAIT OBSERVÉ") }, "report")
        assertTrue(texts.any { it.startsWith("[URG] ") }, "urgent")
        assertTrue(texts.any { it.startsWith("[POS] ALPHA-1 · MGRS ") && "relevée" in it }, "position")
        assertTrue(texts.any { it.startsWith("[CR] FAIT OBSERVÉ · Lieu DMS ") }, "observed fact")
        assertTrue(texts.any { it == "[EQUIPES] Alpha;Bravo" }, "team list")
    }

    @Test
    fun sentMessagesCoverEveryStateAndNoChannelMessageIsAcknowledged() {
        val sent = data.packets.filter { it.packet.from == DemoDataSet.id(DemoDataSet.PC0_NUM) }
        val onChannels = sent.filter { it.contactKey.endsWith("^all") }.map { it.packet.status }.toSet()
        val direct = sent.filterNot { it.contactKey.endsWith("^all") }.map { it.packet.status }.toSet()
        assertEquals(setOf(MessageStatus.QUEUED, MessageStatus.ENROUTE, MessageStatus.DELIVERED), onChannels)
        assertEquals(setOf(MessageStatus.RECEIVED, MessageStatus.ERROR), direct)
    }

    @Test
    fun identifiersAreUnique() {
        assertEquals(data.packets.size, data.packets.map { it.uuid }.toSet().size)
        assertEquals(data.packets.size, data.packets.map { it.packet.id }.toSet().size)
    }

    @Test
    fun agesFollowTheActivationTime() {
        val later = DemoDataSet.create(now + 3_600_000L, TimeZone.UTC)
        assertEquals(data.packets.map { it.receivedTime + 3_600_000L }, later.packets.map { it.receivedTime })
    }
}
