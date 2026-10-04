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
package org.meshtastic.core.demo.screen

import app.cash.turbine.test
import dev.mokkery.mock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import org.meshtastic.core.demo.DemoModeController
import org.meshtastic.core.demo.data.DemoDataSet
import org.meshtastic.core.demo.repository.DemoNodeRepository
import org.meshtastic.core.demo.repository.DemoPacketRepository
import org.meshtastic.core.demo.repository.DemoTeamRosterPrefs
import org.meshtastic.core.demo.send.DemoMessagingController
import org.meshtastic.core.demo.send.DemoSendMessageUseCase
import org.meshtastic.core.demo.store.DemoStore
import org.meshtastic.core.di.CoroutineDispatchers
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.team.TeamRosterRecord
import org.meshtastic.core.repository.MessagingController
import org.meshtastic.core.repository.usecase.SendMessageUseCase
import org.meshtastic.core.testing.FakeNodeRepository
import org.meshtastic.core.testing.FakeRadioController
import org.meshtastic.core.testing.FakeTeamRosterPrefs
import org.meshtastic.core.testing.FakeUiPrefs
import org.meshtastic.proto.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The real side is played by stand-ins holding recognisably real data ("REAL-1", a real history): every assertion
 * checks that a screen sees one side only, and that nothing done during a demo lands on the real side.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ScreenFacadesTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val dispatchers = CoroutineDispatchers(dispatcher, dispatcher, dispatcher)

    private val realOurNode = Node(num = 0x5eed, user = User.Builder().also { it.long_name = "REAL-1" }.build())
    private val realNodes = FakeNodeRepository().apply { setOurNode(realOurNode) }

    /** A real history, kept in its own store so that it can be compared before and after the demo. */
    private val realStore = DemoStore().apply { load(DemoDataSet.create(REAL_TIME, TimeZone.UTC), false) }
    private val realPackets = DemoPacketRepository(realStore)
    private val realTeams = FakeTeamRosterPrefs()
    private val realUiPrefs = FakeUiPrefs()
    private val realRadio = FakeRadioController()

    private val store = DemoStore()
    private val demoMode = DemoModeController(store, realPackets, realUiPrefs, dispatchers)

    private val nodes = ScreenNodeRepository(realNodes, DemoNodeRepository(store), demoMode)
    private val packets = ScreenPacketRepository(realPackets, DemoPacketRepository(store), demoMode)
    private val teams = ScreenTeamRosterPrefs(realTeams, DemoTeamRosterPrefs(store), demoMode)
    private val uiPrefs = ScreenUiPrefs(realUiPrefs, store, demoMode)
    private val owner = ScreenRadioConfigUseCase(realRadio, store, demoMode)

    @Test
    fun anAlreadySubscribedScreenOnlyEverSeesTheCurrentSide() = runTest(dispatcher) {
        nodes.ourNodeInfo.test {
            assertEquals("REAL-1", awaitItem()?.user?.long_name)

            demoMode.activate()
            assertEquals("PC-0", nodes.ourNodeInfo.value?.user?.long_name)
            assertEquals("PC-0", awaitItem()?.user?.long_name)

            demoMode.deactivate()
            assertEquals("REAL-1", nodes.ourNodeInfo.value?.user?.long_name)
            assertEquals("REAL-1", awaitItem()?.user?.long_name)
            expectNoEvents()
        }
    }

    @Test
    fun anAlreadySubscribedConversationListSwitchesBothWays() = runTest(dispatcher) {
        realStore.packets.value = realStore.packets.value.filter { it.contactKey == DemoDataSet.GENERAL_KEY }
        packets.getContacts().test {
            assertEquals(setOf(DemoDataSet.GENERAL_KEY), awaitItem().keys)

            demoMode.activate()
            assertEquals(5, awaitItem().size)

            demoMode.deactivate()
            assertEquals(setOf(DemoDataSet.GENERAL_KEY), awaitItem().keys)
            expectNoEvents()
        }
    }

    @Test
    fun nothingWrittenDuringADemoReachesTheRealSide() = runTest(dispatcher) {
        val realHistory = realStore.packets.value
        val realSettings = realStore.contactSettings.value
        val realRoster = realTeams.roster.value
        realUiPrefs.setCommandPostMode(false)
        demoMode.activate()

        // Mark as read, delete a message, draft, pick a team, switch to the command post view.
        packets.clearUnreadCount(DemoDataSet.GENERAL_KEY, timestamp = Long.MAX_VALUE)
        val first =
            packets.getMessagesFrom(DemoDataSet.GENERAL_KEY, getNode = { nodes.getNode(it.orEmpty()) }).first()
        packets.deleteMessages(listOf(first.first().uuid))
        packets.setDraft(DemoDataSet.GENERAL_KEY, "brouillon de démo")
        teams.adopt(TeamRosterRecord(listOf("Charlie"), TeamRosterRecord.Source.MANUAL, timeSeconds = 1L))
        val renamed = User.Builder().also { it.long_name = "PC-0 [Charlie]" }.build()
        owner.setOwner(DemoDataSet.PC0_NUM, renamed)
        uiPrefs.setCommandPostMode(true)
        assertTrue(uiPrefs.commandPostMode.value)
        assertEquals("PC-0 [Charlie]", nodes.ourNodeInfo.value?.user?.long_name)

        demoMode.deactivate()

        assertEquals(realHistory, realStore.packets.value)
        assertEquals(realSettings, realStore.contactSettings.value)
        assertEquals(realRoster, realTeams.roster.value)
        assertTrue(realRadio.ownerWrites.isEmpty())
        assertEquals(false, realUiPrefs.commandPostMode.value)
        assertEquals(false, uiPrefs.commandPostMode.value)
        assertEquals("REAL-1", nodes.ourNodeInfo.value?.user?.long_name)
    }

    @Test
    fun outsideADemoTheRealSideIsUsedAsBefore() = runTest(dispatcher) {
        uiPrefs.setCommandPostMode(true)
        assertTrue(realUiPrefs.commandPostMode.value)

        owner.setOwner(0x5eed, User.Builder().also { it.long_name = "REAL-1 [Alpha]" }.build())
        assertEquals(1, realRadio.ownerWrites.size)
    }

    @Test
    fun sendingAndReactingDuringADemoNeverCallTheRealRadioPath() = runTest(dispatcher) {
        // Strict mocks: any call to the real send or the real messaging controller fails the test.
        val realSend = mock<SendMessageUseCase>()
        val realMessaging = mock<MessagingController>()
        val demoPackets = DemoPacketRepository(store)
        val send =
            ScreenSendMessageUseCase(
                realSend,
                DemoSendMessageUseCase(store, demoPackets, backgroundScope),
                demoMode,
            )
        val messaging = ScreenMessagingController(realMessaging, DemoMessagingController(), demoMode)
        demoMode.activate()

        val id = send("Message de démo", DemoDataSet.GENERAL_KEY)
        messaging.sendReaction("👍", replyId = id, contactKey = DemoDataSet.GENERAL_KEY)

        assertTrue(store.packets.value.any { it.packet.id == id })
        assertTrue(realRadio.sentPackets.isEmpty())
        assertTrue(realStore.packets.value.none { it.packet.id == id })
    }

    @Test
    fun aRealTeamListArrivingDuringTheDemoWaitsForItsEnd() = runTest(dispatcher) {
        demoMode.activate()
        val received = TeamRosterRecord(listOf("Delta"), TeamRosterRecord.Source.RADIO, timeSeconds = 2L)
        // The radio service writes the real preferences directly, as it does with or without a demo.
        realTeams.offerReceived(received)
        assertEquals(null, teams.pendingRoster.value)

        demoMode.deactivate()
        assertEquals(received, teams.pendingRoster.value)
    }

    private companion object {
        const val REAL_TIME = 1_700_000_000_000L
    }
}
