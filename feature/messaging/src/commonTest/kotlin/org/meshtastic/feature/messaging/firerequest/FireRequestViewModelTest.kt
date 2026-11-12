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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.meshtastic.core.model.ConnectionState
import org.meshtastic.core.model.firerequest.Command
import org.meshtastic.core.model.firerequest.FireRequest
import org.meshtastic.core.model.firerequest.Gdh
import org.meshtastic.core.model.firerequest.GdhZone
import org.meshtastic.core.repository.ConnectionStateProvider
import org.meshtastic.core.repository.FireRequestPrefs
import org.meshtastic.core.repository.usecase.SendMessageUseCase
import org.meshtastic.core.testing.FakeNodeRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** [FireRequestPrefs] in memory, as the demo keeps them. */
class FakeFireRequestPrefs(draft: FireRequest? = null, lastSent: FireRequest? = null, zone: GdhZone = GdhZone.LOCAL) :
    FireRequestPrefs {
    val zoneFlow = MutableStateFlow(zone)
    val draftFlow = MutableStateFlow(draft)
    val lastSentFlow = MutableStateFlow(lastSent)

    /** Every draft saved, in order. */
    val savedDrafts = mutableListOf<FireRequest?>()

    override val gdhZone: StateFlow<GdhZone> = zoneFlow
    override val draft: StateFlow<FireRequest?> = draftFlow
    override val lastSent: StateFlow<FireRequest?> = lastSentFlow

    override fun setGdhZone(zone: GdhZone) {
        zoneFlow.value = zone
    }

    override fun saveDraft(request: FireRequest?) {
        savedDrafts += request
        draftFlow.value = request
    }

    override fun recordSent(request: FireRequest) {
        lastSentFlow.value = request
        draftFlow.value = null
    }
}

/** The draft kept at every change, and « Renouveler » (`docs/SPEC-DEMANDE-DE-TIR.md` § 7, plan § 3.9). */
class FireRequestViewModelTest {

    private val nodeRepository = FakeNodeRepository()
    private val connection = MutableStateFlow<ConnectionState>(ConnectionState.Connected)
    private val connectionStateProvider: ConnectionStateProvider = mock(MockMode.autofill)
    private val sendMessageUseCase: SendMessageUseCase = mock(MockMode.autofill)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { connectionStateProvider.connectionState } returns connection
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(prefs: FireRequestPrefs) =
        FireRequestViewModel(nodeRepository, prefs, sendMessageUseCase, connectionStateProvider)

    @Test
    fun `an empty form starts with the request date-time group at now in the chosen zone`() {
        val vm = viewModel(FakeFireRequestPrefs(zone = GdhZone.ZULU))

        val gdh = assertNotNull(Gdh.parse(vm.request.value.requestGdh))
        assertEquals(GdhZone.ZULU, gdh.zone)
        assertEquals(FireRequest(requestGdh = vm.request.value.requestGdh), vm.request.value)
        assertFalse(vm.hasDraft())
    }

    @Test
    fun `the draft kept on the phone comes back in the form`() {
        val draft = FireRequest(origin = "OA", requestGdh = "112250 loc NOV 26", number = "AB1234")

        val vm = viewModel(FakeFireRequestPrefs(draft = draft))

        assertEquals(draft, vm.request.value)
        assertTrue(vm.hasDraft())
    }

    @Test
    fun `every change is kept as the draft`() {
        val prefs = FakeFireRequestPrefs()
        val vm = viewModel(prefs)

        vm.edit { it.copy(number = "AB1234") }
        vm.edit { it.copy(origin = "OA") }

        assertEquals(2, prefs.savedDrafts.size)
        assertEquals("AB1234", prefs.savedDrafts[0]?.number)
        assertEquals("", prefs.savedDrafts[0]?.origin)
        assertEquals(vm.request.value, prefs.draft.value)
        assertEquals("OA", prefs.draft.value?.origin)
    }

    @Test
    fun `choosing the zone keeps it and puts the request date-time group at now in it`() {
        val prefs = FakeFireRequestPrefs(draft = FireRequest(requestGdh = "010000 loc JAN 20", number = "AB1234"))
        val vm = viewModel(prefs)

        vm.chooseZone(GdhZone.FRANCE)

        assertEquals(GdhZone.FRANCE, prefs.gdhZone.value)
        assertEquals(GdhZone.FRANCE, Gdh.parse(vm.request.value.requestGdh)?.zone)
        assertEquals("AB1234", vm.request.value.number)
        assertEquals(vm.request.value, prefs.draft.value)
    }

    @Test
    fun `Renouveler is offered only once a request was sent`() {
        assertFalse(viewModel(FakeFireRequestPrefs()).canRenew.value)
        assertTrue(viewModel(FakeFireRequestPrefs(lastSent = FireRequest(number = "AB1234"))).canRenew.value)
    }

    @Test
    fun `Renouveler brings back the last request, same number, date-time group at now`() {
        val last =
            FireRequest(
                origin = "OA",
                recipient = "CDS TIR",
                requestGdh = "010000Z JAN 20",
                number = "AB1234",
                command = Command(whenReady = true),
            )
        val prefs = FakeFireRequestPrefs(lastSent = last, zone = GdhZone.ZULU)
        val vm = viewModel(prefs)

        vm.renew()

        val renewed = vm.request.value
        assertEquals(last.copy(requestGdh = renewed.requestGdh), renewed)
        assertNotEquals(last.requestGdh, renewed.requestGdh)
        assertEquals(GdhZone.ZULU, Gdh.parse(renewed.requestGdh)?.zone)
        assertEquals(renewed, prefs.draft.value)
        assertEquals(last, prefs.lastSent.value)
    }

    @Test
    fun `Renouveler without a request sent changes nothing`() {
        val draft = FireRequest(requestGdh = "112250 loc NOV 26", number = "AB1235")
        val prefs = FakeFireRequestPrefs(draft = draft)
        val vm = viewModel(prefs)

        vm.renew()

        assertEquals(draft, vm.request.value)
        assertTrue(prefs.savedDrafts.isEmpty())
    }

    @Test
    fun `a form holding only its date-time group has nothing Renouveler would replace`() {
        val vm = viewModel(FakeFireRequestPrefs())
        vm.edit { it.copy(requestGdh = "112250 loc NOV 26") }

        assertFalse(vm.hasDraft())

        vm.edit { it.copy(origin = "OA") }

        assertTrue(vm.hasDraft())
        assertNull(viewModel(FakeFireRequestPrefs()).recipient.value)
    }
}
