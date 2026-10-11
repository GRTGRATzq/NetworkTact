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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import org.meshtastic.core.demo.DemoModeController
import org.meshtastic.core.demo.data.DemoDataSet
import org.meshtastic.core.demo.repository.DemoExpectedLoraPrefs
import org.meshtastic.core.demo.repository.DemoNodeRepository
import org.meshtastic.core.demo.repository.DemoOrgChartPrefs
import org.meshtastic.core.demo.repository.DemoPacketRepository
import org.meshtastic.core.demo.repository.DemoPhonePositionSource
import org.meshtastic.core.demo.repository.DemoRadioActivityProvider
import org.meshtastic.core.demo.repository.DemoRadioConfigRepository
import org.meshtastic.core.demo.repository.DemoTeamRosterPrefs
import org.meshtastic.core.demo.send.DemoMessagingController
import org.meshtastic.core.demo.send.DemoSendMessageUseCase
import org.meshtastic.core.demo.store.DemoStore
import org.meshtastic.core.di.CoroutineDispatchers
import org.meshtastic.core.model.DataPacket
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.NodeAddress
import org.meshtastic.core.model.diagnostic.LoraSetting
import org.meshtastic.core.model.geo.RadioPositionPacket
import org.meshtastic.core.model.geo.SharedPoint
import org.meshtastic.core.model.geo.toWaypoint
import org.meshtastic.core.model.orgchart.OrgChart
import org.meshtastic.core.model.orgchart.OrgChartRecord
import org.meshtastic.core.model.orgchart.OrgMember
import org.meshtastic.core.model.team.TeamRosterRecord
import org.meshtastic.core.repository.MessagingController
import org.meshtastic.core.repository.PhoneFix
import org.meshtastic.core.repository.PhonePositionSource
import org.meshtastic.core.repository.RadioActivityProvider
import org.meshtastic.core.repository.usecase.SendMessageUseCase
import org.meshtastic.core.testing.FakeExpectedLoraPrefs
import org.meshtastic.core.testing.FakeNodeRepository
import org.meshtastic.core.testing.FakeOrgChartPrefs
import org.meshtastic.core.testing.FakeRadioController
import org.meshtastic.core.testing.FakeTeamRosterPrefs
import org.meshtastic.core.testing.FakeUiPrefs
import org.meshtastic.proto.Config
import org.meshtastic.proto.PortNum
import org.meshtastic.proto.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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
    private val realOrgChart = FakeOrgChartPrefs()
    private val realUiPrefs = FakeUiPrefs()
    private val realRadio = FakeRadioController()

    private val store = DemoStore()
    private val demoMode = DemoModeController(store, realPackets, realUiPrefs, dispatchers)

    private val nodes = ScreenNodeRepository(realNodes, DemoNodeRepository(store), demoMode)
    private val packets = ScreenPacketRepository(realPackets, DemoPacketRepository(store), demoMode)
    private val teams = ScreenTeamRosterPrefs(realTeams, DemoTeamRosterPrefs(store), demoMode)
    private val orgChart = ScreenOrgChartPrefs(realOrgChart, DemoOrgChartPrefs(store), demoMode)
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
    fun sharingAPointDuringADemoEmitsNoWaypoint() = runTest(dispatcher) {
        // Strict mock: a waypoint reaching the real messaging controller fails the test.
        val realMessaging = mock<MessagingController>()
        val messaging = ScreenMessagingController(realMessaging, DemoMessagingController(), demoMode)
        demoMode.activate()
        val point =
            assertNotNull(
                SharedPoint.of(
                    "[CR] FAIT OBSERVÉ · 14:05 · Lieu MGRS 31U DQ 48251 11932 · fumée",
                    "PC-0",
                    DemoDataSet.PC0_NUM,
                    REAL_TIME / 1_000,
                    TimeZone.UTC,
                ),
            )

        messaging.sendMessage(DataPacket(NodeAddress.ID_BROADCAST, 0, point.toWaypoint()))

        assertTrue(realRadio.sentPackets.isEmpty())
        assertTrue(store.packets.value.none { it.packet.dataType == PortNum.WAYPOINT_APP.value })
        assertTrue(realStore.packets.value.none { it.packet.dataType == PortNum.WAYPOINT_APP.value })
    }

    @Test
    fun broadcastingThePositionDuringADemoEmitsNothing() = runTest(dispatcher) {
        // Strict mock: the position packet reaching the real messaging controller fails the test.
        val realMessaging = mock<MessagingController>()
        val messaging = ScreenMessagingController(realMessaging, DemoMessagingController(), demoMode)
        demoMode.activate()
        val pc0 = assertNotNull(nodes.ourNodeInfo.value)

        messaging.sendMessage(RadioPositionPacket.of(pc0.position))

        assertTrue(realRadio.sentPackets.isEmpty())
        assertTrue(store.packets.value.none { it.packet.dataType == PortNum.POSITION_APP.value })
        assertTrue(realStore.packets.value.none { it.packet.dataType == PortNum.POSITION_APP.value })
    }

    @Test
    fun phonePositionIsTheFictitiousOneDuringADemoOnly() = runTest(dispatcher) {
        val realFix = PhoneFix(latitude = 45.0, longitude = 5.0, fixEpochSeconds = 1L)
        val phone =
            ScreenPhonePositionSource(PhonePositionSource { realFix }, DemoPhonePositionSource { 1_000L }, demoMode)
        assertEquals(realFix, phone.lastFix())

        demoMode.activate()
        val demoFix = assertNotNull(phone.lastFix())
        assertEquals(DemoDataSet.PHONE_POINT.latitude, demoFix.latitude)
        assertEquals(DemoDataSet.PHONE_POINT.longitude, demoFix.longitude)
        assertEquals(1_000L - DemoDataSet.PHONE_FIX_AGE.inWholeSeconds, demoFix.fixEpochSeconds)

        demoMode.deactivate()
        assertEquals(realFix, phone.lastFix())
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

    @Test
    fun theChartShownIsTheDemoOneAndNoChangeReachesTheRealOne() = runTest(dispatcher) {
        val real =
            OrgChartRecord(
                OrgChart("REAL-1", 1L, listOf(OrgMember("REAL-1")), emptyList()),
                OrgChartRecord.Source.EDITED,
                1L,
            )
        realOrgChart.save(real)
        orgChart.current.test {
            assertEquals(real, awaitItem())

            demoMode.activate()
            assertEquals("PC-0", awaitItem()?.chart?.author)
            val scanned = OrgChartRecord(real.chart.copy(author = "PC-9"), OrgChartRecord.Source.SCANNED, 2L)
            orgChart.adopt(scanned)
            assertEquals(scanned, awaitItem())
            orgChart.save(scanned.copy(source = OrgChartRecord.Source.EDITED))
            awaitItem()

            demoMode.deactivate()
            assertEquals(real, awaitItem())
            expectNoEvents()
        }
        assertEquals(real, realOrgChart.current.value)
        assertEquals(null, realOrgChart.previous.value)
    }

    @Test
    fun theExpectedLoraSettingShownIsTheDemoOneAndNoChangeReachesTheRealOne() = runTest(dispatcher) {
        val realSetting = LoraSetting("US", "MEDIUM_FAST")
        val realExpected = FakeExpectedLoraPrefs(realSetting)
        val expected = ScreenExpectedLoraPrefs(realExpected, DemoExpectedLoraPrefs(store), demoMode)
        assertEquals(realSetting, expected.expected.value)

        demoMode.activate()
        assertEquals(DemoDataSet.DEMO_LORA, expected.expected.value)
        expected.setExpected(null)
        assertNull(expected.expected.value)

        demoMode.deactivate()
        assertEquals(realSetting, expected.expected.value)
        assertEquals(realSetting, realExpected.expected.value)
    }

    @Test
    fun theDiagnosticReadsTheDemoRadioDuringADemoOnly() = runTest(dispatcher) {
        val real =
            object : RadioActivityProvider {
                override val lastPacketFromRadioMillis = MutableStateFlow<Long?>(42L)
            }
        val activity = ScreenRadioActivityProvider(real, DemoRadioActivityProvider(store), demoMode)
        assertEquals(42L, activity.lastPacketFromRadioMillis.value)

        demoMode.activate()
        val demoPacket = assertNotNull(activity.lastPacketFromRadioMillis.value)
        assertTrue(demoPacket != 42L)
        val lora = DemoRadioConfigRepository(store).localConfigFlow.first().lora
        assertEquals(Config.LoRaConfig.RegionCode.EU_868, lora?.region)
        assertEquals(Config.LoRaConfig.ModemPreset.LONG_FAST, lora?.modem_preset)
        assertEquals(DemoDataSet.PC0_BATTERY_PERCENT, nodes.ourNodeInfo.value?.batteryLevel)

        demoMode.deactivate()
        assertEquals(42L, activity.lastPacketFromRadioMillis.value)
    }

    private companion object {
        const val REAL_TIME = 1_700_000_000_000L
    }
}
