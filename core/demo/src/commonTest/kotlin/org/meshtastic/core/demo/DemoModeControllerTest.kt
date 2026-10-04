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
package org.meshtastic.core.demo

import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.meshtastic.core.demo.data.DemoDataSet
import org.meshtastic.core.demo.store.DemoStore
import org.meshtastic.core.di.CoroutineDispatchers
import org.meshtastic.core.repository.PacketRepository
import org.meshtastic.core.testing.FakeUiPrefs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DemoModeControllerTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val unread = MutableStateFlow(3)

    // Strict mock: any call other than the unread total would fail the test.
    private val realPackets = mock<PacketRepository> { every { getUnreadCountTotal() } returns unread }
    private val realUiPrefs = FakeUiPrefs()
    private val store = DemoStore()

    private fun controller() =
        DemoModeController(store, realPackets, realUiPrefs, CoroutineDispatchers(dispatcher, dispatcher, dispatcher))

    @Test
    fun startsOffSoARestartNeverResumesADemo() {
        assertFalse(controller().isActive.value)
        assertTrue(store.nodes.value.isEmpty())
    }

    @Test
    fun activationLoadsAFreshDataSetAndDeactivationDropsIt() = runTest(dispatcher) {
        val demo = controller()

        demo.activate()
        assertTrue(demo.isActive.value)
        assertEquals(DemoDataSet.PC0_NUM, store.ourNode?.num)
        assertEquals(4, store.nodes.value.size)

        demo.deactivate()
        assertFalse(demo.isActive.value)
        assertNull(store.ourNode)
        assertTrue(store.packets.value.isEmpty())
        assertNull(store.roster.value)
    }

    @Test
    fun countsRealMessagesArrivingDuringTheDemoOnly() = runTest(dispatcher) {
        val demo = controller()
        demo.activate()
        assertEquals(0, demo.realMessagesSinceActivation.value)

        unread.value = 5
        assertEquals(2, demo.realMessagesSinceActivation.value)

        // Read from a notification: no new message, nothing counted.
        unread.value = 4
        assertEquals(2, demo.realMessagesSinceActivation.value)

        unread.value = 6
        assertEquals(4, demo.realMessagesSinceActivation.value)

        demo.deactivate()
        assertEquals(0, demo.realMessagesSinceActivation.value)
        unread.value = 9
        assertEquals(0, demo.realMessagesSinceActivation.value)
    }

    @Test
    fun terrainOrCommandPostChoiceIsSeededButNeverWrittenBack() = runTest(dispatcher) {
        realUiPrefs.setCommandPostMode(true)
        val demo = controller()

        demo.activate()
        assertTrue(store.commandPostMode.value)
        store.commandPostMode.value = false
        demo.deactivate()

        assertTrue(realUiPrefs.commandPostMode.value)
    }
}
