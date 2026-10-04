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
package org.meshtastic.core.demo.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import org.meshtastic.core.demo.data.DemoDataSet
import org.meshtastic.core.demo.store.DemoStore
import org.meshtastic.core.model.team.TeamRosterRecord
import org.meshtastic.core.model.util.getChannel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DemoRepositoriesTest {

    private val now = 1_790_000_000_000L
    private val store = DemoStore().apply { load(DemoDataSet.create(now, TimeZone.UTC), commandPostMode = false) }
    private val nodes = DemoNodeRepository(store)
    private val packets = DemoPacketRepository(store)
    private val radioConfig = DemoRadioConfigRepository(store)
    private val teams = DemoTeamRosterPrefs(store)

    private val getNode: suspend (String?) -> org.meshtastic.core.model.Node = { nodes.getNode(it.orEmpty()) }

    @Test
    fun nodesAreTheDemoOnesWithPc0AsOurs() = runTest {
        assertEquals("PC-0", nodes.ourNodeInfo.value?.user?.long_name)
        assertEquals(DemoDataSet.id(DemoDataSet.PC0_NUM), nodes.myId.value)
        assertEquals(4, nodes.getNodes().first().size)
        assertEquals("BRAVO-2 [Bravo]", nodes.getUser(DemoDataSet.BRAVO2_NUM).long_name)
    }

    @Test
    fun conversationsListOneEntryPerChannelAndDirectContact() = runTest {
        assertEquals(
            setOf(
                DemoDataSet.GENERAL_KEY,
                DemoDataSet.TEAM_ALPHA_KEY,
                DemoDataSet.COMMAND_POST_KEY,
                DemoDataSet.BRAVO2_DIRECT_KEY,
                DemoDataSet.CHARLIE3_DIRECT_KEY,
            ),
            packets.getContacts().first().keys,
        )
    }

    @Test
    fun messagesComeNewestFirstAndOursAreFromLocal() = runTest {
        val messages = packets.getMessagesFrom(DemoDataSet.GENERAL_KEY, getNode = getNode).first()
        assertEquals(messages.sortedByDescending { it.receivedTime }, messages)
        assertTrue(messages.filter { it.fromLocal }.all { it.node.num == DemoDataSet.PC0_NUM })
        assertTrue(messages.any { !it.fromLocal && it.node.num == DemoDataSet.ALPHA1_NUM })
    }

    @Test
    fun readingDraftingAndDeletingStayInTheDemoStore() = runTest {
        val unreadBefore = packets.getUnreadCount(DemoDataSet.GENERAL_KEY)
        assertTrue(unreadBefore > 0)
        packets.clearUnreadCount(DemoDataSet.GENERAL_KEY, timestamp = now)
        assertEquals(0, packets.getUnreadCount(DemoDataSet.GENERAL_KEY))

        packets.setDraft(DemoDataSet.GENERAL_KEY, "brouillon")
        assertEquals("brouillon", packets.getDraft(DemoDataSet.GENERAL_KEY))

        val first = packets.getMessagesFrom(DemoDataSet.GENERAL_KEY, getNode = getNode).first().first()
        packets.deleteMessages(listOf(first.uuid))
        assertTrue(store.packets.value.none { it.uuid == first.uuid })
    }

    @Test
    fun threeChannelsAndTheTeamList() {
        assertEquals("Équipe Alpha", store.channelSet.value.getChannel(1)?.settings?.name)
        assertEquals(listOf("Alpha", "Bravo"), teams.roster.value?.teams)
    }

    @Test
    fun aPendingDemoListIsDroppedWithTheStore() = runTest {
        val offered = TeamRosterRecord(listOf("Charlie"), TeamRosterRecord.Source.RADIO, timeSeconds = 1L)
        teams.offerReceived(offered)
        assertEquals(offered, teams.pendingRoster.value)

        store.clear()
        assertNull(teams.pendingRoster.value)
        assertNull(teams.roster.value)
        assertTrue(radioConfig.channelSetFlow.first().settings.isEmpty())
    }
}
