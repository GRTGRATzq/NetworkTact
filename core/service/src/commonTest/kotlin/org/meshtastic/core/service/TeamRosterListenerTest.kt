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
package org.meshtastic.core.service

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okio.ByteString.Companion.encodeUtf8
import okio.ByteString.Companion.toByteString
import org.meshtastic.core.model.team.TeamRosterRecord
import org.meshtastic.core.repository.TeamRosterPrefs
import org.meshtastic.core.testing.FakeNodeRepository
import org.meshtastic.core.testing.FakeServiceRepository
import org.meshtastic.core.testing.FakeTeamRosterPrefs
import org.meshtastic.core.testing.TestDataFactory
import org.meshtastic.proto.Data
import org.meshtastic.proto.MeshPacket
import org.meshtastic.proto.PortNum
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TeamRosterListenerTest {

    private val pcNum = 0x1234abcd
    private val myNum = 0x0badcafe

    private fun packet(
        text: String,
        from: Int = pcNum,
        portnum: PortNum = PortNum.TEXT_MESSAGE_APP,
        emoji: Int = 0,
    ): MeshPacket =
        MeshPacket(from = from, decoded = Data(portnum = portnum, payload = text.encodeUtf8(), emoji = emoji))

    private val pc = TestDataFactory.createTestNode(num = pcNum, userId = "!1234abcd", longName = "PC-0")

    @Test
    fun a_team_list_is_recorded_with_its_sender() {
        val record = teamRosterRecordOf(packet("[EQUIPES] Alpha;Bravo"), myNum, pc, nowSeconds = 1_000)

        assertEquals(
            TeamRosterRecord(
                teams = listOf("Alpha", "Bravo"),
                source = TeamRosterRecord.Source.RADIO,
                senderNum = pcNum,
                senderName = "PC-0",
                timeSeconds = 1_000,
            ),
            record,
        )
    }

    @Test
    fun an_unknown_sender_still_gives_its_node_number() {
        val record = teamRosterRecordOf(packet("[EQUIPES] Alpha"), myNum, sender = null, nowSeconds = 1_000)

        assertEquals(pcNum, record?.senderNum)
        assertNull(record?.senderName)
    }

    @Test
    fun anything_else_is_left_alone() {
        assertNull(teamRosterRecordOf(packet("Alpha;Bravo"), myNum, pc, 1_000))
        assertNull(teamRosterRecordOf(packet("[EQUIPES] Al]pha"), myNum, pc, 1_000))
        assertNull(teamRosterRecordOf(packet("[EQUIPES] Alpha", portnum = PortNum.RANGE_TEST_APP), myNum, pc, 1_000))
        assertNull(teamRosterRecordOf(packet("[EQUIPES] Alpha", emoji = 1), myNum, pc, 1_000))
        assertNull(teamRosterRecordOf(packet("[EQUIPES] Alpha", from = myNum), myNum, pc, 1_000))
        assertNull(teamRosterRecordOf(packet("[EQUIPES] Alpha"), myNum, pc.copy(isIgnored = true), 1_000))
        assertNull(teamRosterRecordOf(MeshPacket(from = pcNum), myNum, pc, 1_000))
    }

    @Test
    fun invalid_utf8_is_not_a_team_list() {
        val garbage =
            MeshPacket(
                from = pcNum,
                decoded =
                Data(
                    portnum = PortNum.TEXT_MESSAGE_APP,
                    payload = byteArrayOf(0xC3.toByte(), 0x28, 0xFF.toByte()).toByteString(),
                ),
            )
        assertNull(teamRosterRecordOf(garbage, myNum, pc, 1_000))
    }

    @Test
    fun a_received_list_waits_for_confirmation() = runTest(UnconfinedTestDispatcher()) {
        val serviceRepository = FakeServiceRepository()
        val prefs = FakeTeamRosterPrefs()
        val nodes = FakeNodeRepository().apply { setNodes(listOf(pc)) }
        TeamRosterListener(serviceRepository, nodes, prefs).start(backgroundScope)

        serviceRepository.emitMeshPacket(packet("[EQUIPES] Alpha;Bravo"))

        assertNull(prefs.roster.value)
        assertEquals(listOf("Alpha", "Bravo"), prefs.pendingRoster.value?.teams)
        assertEquals("PC-0", prefs.pendingRoster.value?.senderName)
    }

    @Test
    fun a_failure_never_stops_the_listener_nor_the_packet_flow() = runTest(UnconfinedTestDispatcher()) {
        val serviceRepository = FakeServiceRepository()
        val delegate = FakeTeamRosterPrefs()
        var calls = 0
        // Fails on the first list it is given, as a damaged store or an unexpected bug would.
        val failingOnce =
            object : TeamRosterPrefs by delegate {
                override fun offerReceived(record: TeamRosterRecord) {
                    calls++
                    check(calls > 1) { "simulated failure" }
                    delegate.offerReceived(record)
                }
            }
        val listener =
            TeamRosterListener(serviceRepository, FakeNodeRepository(), failingOnce).start(backgroundScope)

        serviceRepository.emitMeshPacket(packet("[EQUIPES] Alpha"))
        serviceRepository.emitMeshPacket(MeshPacket(from = pcNum))
        serviceRepository.emitMeshPacket(packet("[EQUIPES] Bravo"))

        // Every emit returned, and the list after the failure still got through.
        assertEquals(2, calls)
        assertEquals(listOf("Bravo"), delegate.pendingRoster.value?.teams)
        assertTrue(listener.isActive)
    }
}
