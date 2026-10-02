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
package org.meshtastic.feature.settings.team

import dev.mokkery.MockMode
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.capture.capture
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode.Companion.exactly
import dev.mokkery.verifySuspend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okio.ByteString.Companion.encodeUtf8
import org.meshtastic.core.domain.usecase.settings.RadioConfigUseCase
import org.meshtastic.core.model.ConnectionState
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.team.TeamRoster
import org.meshtastic.core.model.team.TeamRosterRecord
import org.meshtastic.core.repository.usecase.SendMessageUseCase
import org.meshtastic.core.testing.FakeNodeRepository
import org.meshtastic.core.testing.FakeRadioConfigRepository
import org.meshtastic.core.testing.FakeServiceRepository
import org.meshtastic.core.testing.FakeTeamRosterPrefs
import org.meshtastic.core.testing.FakeUiPrefs
import org.meshtastic.proto.HardwareModel
import org.meshtastic.proto.User
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TeamsViewModelTest {

    private val myNum = 0x0badcafe

    private val owner =
        User(
            id = "!0badcafe",
            long_name = "ALPHA-1",
            short_name = "A1",
            hw_model = HardwareModel.TBEAM,
            is_licensed = false,
            is_unmessagable = true,
            public_key = "key".encodeUtf8(),
        )

    private val prefs = FakeTeamRosterPrefs()
    private val nodes = FakeNodeRepository()
    private val service = FakeServiceRepository()
    private val radioConfigUseCase: RadioConfigUseCase = mock(MockMode.autofill)
    private val sendMessageUseCase: SendMessageUseCase = mock(MockMode.autofill)
    private lateinit var viewModel: TeamsViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        everySuspend { radioConfigUseCase.setOwner(any(), any(), any()) } returns 1
        nodes.setOurNode(Node(num = myNum, user = owner))
        service.setConnectionState(ConnectionState.Connected)
        everySuspend { sendMessageUseCase(any(), any(), any()) } returns 1
        viewModel =
            TeamsViewModel(
                teamRosterPrefs = prefs,
                nodeRepository = nodes,
                serviceRepository = service,
                radioConfigUseCase = radioConfigUseCase,
                sendMessageUseCase = sendMessageUseCase,
                radioConfigRepository = FakeRadioConfigRepository(),
                uiPrefs = FakeUiPrefs(),
            )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun only_the_long_name_changes_in_the_owner_sent() {
        val (sent, change) = ownerWithTeam(owner, "Alpha")

        assertEquals("ALPHA-1 [Alpha]", change.longName)
        assertEquals(owner.copy(long_name = "ALPHA-1 [Alpha]"), sent)
        assertEquals(owner.short_name, sent.short_name)
        assertEquals(owner.is_licensed, sent.is_licensed)
        assertEquals(owner.is_unmessagable, sent.is_unmessagable)
        assertEquals(owner.public_key, sent.public_key)
        assertEquals(owner.hw_model, sent.hw_model)
        assertEquals(owner.id, sent.id)
    }

    @Test
    fun choosing_a_team_sends_set_owner_to_my_radio_only() = runTest {
        val sent = mutableListOf<User>()
        everySuspend { radioConfigUseCase.setOwner(any(), capture(sent), any()) } returns 1

        viewModel.applyTeam("Bravo")

        verifySuspend { radioConfigUseCase.setOwner(myNum, any(), any()) }
        assertEquals(listOf(owner.copy(long_name = "ALPHA-1 [Bravo]")), sent)
        assertEquals(TeamSendResult.SENT, viewModel.sendResult.value)
    }

    @Test
    fun nothing_is_sent_while_my_radio_is_not_connected() = runTest {
        service.setConnectionState(ConnectionState.Disconnected)

        assertNull(viewModel.previewTeam("Alpha"))
        viewModel.applyTeam("Alpha")

        verifySuspend(exactly(0)) { radioConfigUseCase.setOwner(any(), any(), any()) }
        assertNull(viewModel.sendResult.value)
    }

    @Test
    fun a_licensed_radio_is_left_alone() = runTest {
        nodes.setOurNode(Node(num = myNum, user = owner.copy(is_licensed = true)))

        assertNull(viewModel.previewTeam("Alpha"))
        viewModel.applyTeam("Alpha")

        verifySuspend(exactly(0)) { radioConfigUseCase.setOwner(any(), any(), any()) }
    }

    @Test
    fun a_failed_send_is_reported() = runTest {
        everySuspend { radioConfigUseCase.setOwner(any(), any(), any()) } calls { error("radio gone") }

        viewModel.applyTeam("Alpha")

        assertEquals(TeamSendResult.FAILED, viewModel.sendResult.value)
    }

    @Test
    fun preview_warns_before_truncating() {
        nodes.setOurNode(Node(num = myNum, user = owner.copy(long_name = "A".repeat(35))))

        val change = viewModel.previewTeam("Alpha")

        assertTrue(change!!.truncated)
        assertEquals("A".repeat(31) + " [Alpha]", change.longName)
    }

    @Test
    fun ownerWriteBlock_explains_why_nothing_can_be_sent() {
        val node = Node(num = myNum, user = owner)
        assertEquals(OwnerWriteBlock.NOT_CONNECTED, ownerWriteBlock(ConnectionState.Connecting, node))
        assertEquals(OwnerWriteBlock.NOT_CONNECTED, ownerWriteBlock(ConnectionState.DeviceSleep, node))
        assertEquals(OwnerWriteBlock.NO_LOCAL_NODE, ownerWriteBlock(ConnectionState.Connected, null))
        assertEquals(
            OwnerWriteBlock.NO_LOCAL_NODE,
            ownerWriteBlock(ConnectionState.Connected, node.copy(user = owner.copy(long_name = " "))),
        )
        assertEquals(
            OwnerWriteBlock.LICENSED,
            ownerWriteBlock(ConnectionState.Connected, node.copy(user = owner.copy(is_licensed = true))),
        )
        assertNull(ownerWriteBlock(ConnectionState.Connected, node))
    }

    @Test
    fun my_team_missing_from_a_new_list_is_kept_and_flagged() {
        val state =
            TeamsUiState(
                roster = TeamRosterRecord(listOf("Bravo"), TeamRosterRecord.Source.MANUAL, timeSeconds = 1),
                myLongName = "ALPHA-1 [Alpha]",
                myTeam = "Alpha",
                block = null,
            )
        assertTrue(state.myTeamMissingFromList)
        assertFalse(state.copy(myTeam = "BRAVO").myTeamMissingFromList)
        assertFalse(state.copy(roster = null).myTeamMissingFromList)
    }

    @Test
    fun adopting_a_list_never_touches_the_radio() = runTest {
        viewModel.adoptManual(TeamRoster(listOf("Alpha", "Bravo")))

        assertEquals(listOf("Alpha", "Bravo"), prefs.roster.value?.teams)
        assertEquals(TeamRosterRecord.Source.MANUAL, prefs.roster.value?.source)
        verifySuspend(exactly(0)) { radioConfigUseCase.setOwner(any(), any(), any()) }
    }

    @Test
    fun pending_list_is_adopted_or_dismissed_on_request() {
        val received =
            TeamRosterRecord(
                listOf("Alpha", "Bravo"),
                TeamRosterRecord.Source.RADIO,
                senderNum = 0x1234abcd,
                senderName = "PC-0",
                timeSeconds = 2,
            )
        prefs.offerReceived(received)
        viewModel.dismissPending(received)
        assertNull(prefs.pendingRoster.value)
        assertNull(prefs.roster.value)

        prefs.offerReceived(received)
        viewModel.acceptPending(received)
        assertEquals(received, prefs.roster.value)
        assertNull(prefs.pendingRoster.value)
    }

    @Test
    fun broadcast_sends_the_list_as_text_on_the_chosen_channel_and_adopts_it() = runTest {
        viewModel.broadcast(TeamRoster(listOf("Alpha", "Bravo")), channelIndex = 2)

        verifySuspend { sendMessageUseCase("[EQUIPES] Alpha;Bravo", "2^all", null) }
        assertEquals(listOf("Alpha", "Bravo"), prefs.roster.value?.teams)
        assertEquals(TeamRosterRecord.Source.BROADCAST, prefs.roster.value?.source)
        assertEquals(BroadcastResult.Sent("2"), viewModel.broadcastResult.value)
    }

    @Test
    fun broadcast_is_refused_while_my_radio_is_not_connected() = runTest {
        service.setConnectionState(ConnectionState.Disconnected)

        viewModel.broadcast(TeamRoster(listOf("Alpha")), channelIndex = 0)

        verifySuspend(exactly(0)) { sendMessageUseCase(any(), any(), any()) }
        assertNull(prefs.roster.value)
    }

    @Test
    fun a_failed_broadcast_adopts_nothing() = runTest {
        everySuspend { sendMessageUseCase(any(), any(), any()) } calls { error("queue full") }

        viewModel.broadcast(TeamRoster(listOf("Alpha")), channelIndex = 0)

        assertNull(prefs.roster.value)
        assertEquals(BroadcastResult.Failed, viewModel.broadcastResult.value)
    }
}
