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
package org.meshtastic.feature.messaging.firerequest

import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okio.ByteString.Companion.toByteString
import org.meshtastic.core.model.ConnectionState
import org.meshtastic.core.model.firerequest.Command
import org.meshtastic.core.model.firerequest.FireRequest
import org.meshtastic.core.repository.ConnectionStateProvider
import org.meshtastic.core.repository.usecase.SendMessageUseCase
import org.meshtastic.core.testing.FakeNodeRepository
import org.meshtastic.core.testing.TestDataFactory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Sends recorded in order; nothing goes anywhere. */
class RecordingSendMessageUseCase : SendMessageUseCase {
    val sent = mutableListOf<Pair<String, String>>()

    override suspend fun invoke(text: String, contactKey: String, replyId: Int?): Int {
        sent += text to contactKey
        return sent.size
    }
}

/** Sending: one direct message per part, in order, to the recipient (`docs/PLAN-DEMANDE-DE-TIR.md` § 8). */
class FireRequestSendTest {

    private val nodeRepository = FakeNodeRepository()
    private val connection = MutableStateFlow<ConnectionState>(ConnectionState.Connected)
    private val connectionStateProvider: ConnectionStateProvider = mock(MockMode.autofill)
    private val send = RecordingSendMessageUseCase()
    private val key = byteArrayOf(1, 2, 3).toByteString()

    private val ours = TestDataFactory.createTestNode(num = 1, userId = "!00000001", longName = "PC-0")
    private val cdsTir =
        TestDataFactory.createTestNode(num = 2, userId = "!00000002", longName = "CDS TIR [Tir]").copy(channel = 1)

    /** A request whose « Destinataire » box is left to the node chosen. */
    private val draft =
        FireRequest(
            origin = "OA",
            requestGdh = "112250 loc NOV 26",
            number = "AB1234",
            command = Command(whenReady = true),
        )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { connectionStateProvider.connectionState } returns connection
        nodeRepository.setOurNode(ours)
        nodeRepository.setNodes(listOf(ours, cdsTir))
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(prefs: FakeFireRequestPrefs = FakeFireRequestPrefs(draft = draft)) =
        FireRequestViewModel(nodeRepository, prefs, send, connectionStateProvider)

    @Test
    fun `the direct conversation gives the recipient, its name without team in the Destinataire box`() {
        val vm = viewModel()

        vm.start("1!00000002")

        val cds = RecipientOption(2, "!00000002", "CDS TIR")
        assertEquals(cds, vm.recipient.value)
        assertEquals("CDS TIR", vm.request.value.recipient)
        assertEquals(listOf(cds), vm.recipients.value)
        assertNull(vm.sendBlock.value)
    }

    @Test
    fun `a channel conversation chooses no recipient`() {
        val vm = viewModel()

        vm.start("0^all")

        assertNull(vm.recipient.value)
        assertEquals(SendBlock.NO_RECIPIENT, vm.sendBlock.value)
    }

    @Test
    fun `every part goes in order, in a direct message on the node's channel`() = runTest {
        val vm = viewModel()
        vm.start("1!00000002")
        val parts = FireRequestPreview.of(vm.request.value).parts

        val conversation = vm.send()

        assertEquals("1!00000002", conversation)
        assertEquals(parts.map { it to "1!00000002" }, send.sent)
        assertTrue(parts.first().startsWith("[DT AB1234 1/${parts.size}]\n"))
    }

    @Test
    fun `with keys on both radios the parts go on the PKI channel`() = runTest {
        nodeRepository.setOurNode(ours.copy(publicKey = key))
        nodeRepository.setNodes(listOf(ours.copy(publicKey = key), cdsTir.copy(publicKey = key)))
        val vm = viewModel()
        vm.start("8!00000002")

        assertEquals("8!00000002", vm.send())
        assertTrue(send.sent.isNotEmpty())
        assertTrue(send.sent.all { (_, contactKey) -> contactKey == "8!00000002" })
    }

    @Test
    fun `once sent the request becomes the last one and the draft is gone`() = runTest {
        val prefs = FakeFireRequestPrefs(draft = draft)
        val vm = viewModel(prefs)
        vm.start("1!00000002")

        vm.send()

        assertNull(prefs.draft.value)
        assertEquals(draft.copy(recipient = "CDS TIR"), prefs.lastSent.value)
        assertTrue(vm.canRenew.value)
    }

    @Test
    fun `nothing goes while sending is refused`() = runTest {
        val vm = viewModel(FakeFireRequestPrefs(draft = draft.copy(number = "")))
        vm.start("1!00000002")
        assertEquals(SendBlock.NO_NUMBER, vm.sendBlock.value)
        assertNull(vm.send())

        vm.edit { it.copy(number = "AB1234") }
        connection.value = ConnectionState.Disconnected
        assertEquals(SendBlock.DISCONNECTED, vm.sendBlock.value)
        assertNull(vm.send())

        assertTrue(send.sent.isEmpty())
    }
}
