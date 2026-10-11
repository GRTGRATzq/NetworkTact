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
package org.meshtastic.feature.node.diagnostic

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.meshtastic.core.model.ConnectionState
import org.meshtastic.core.model.Position
import org.meshtastic.core.model.TelemetryType
import org.meshtastic.core.testing.FakeDemoMode
import org.meshtastic.core.testing.FakeServiceRepository
import org.meshtastic.feature.node.detail.NodeRequestActions
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LinkTestViewModelTest {

    /** Records the requests; [accept] false plays a radio that refuses them (no traceroute time recorded). */
    private class RecordingRequests(var now: () -> Long) : NodeRequestActions {
        val traceroutes = mutableListOf<Int>()
        var accept = true
        override val lastTracerouteTime = MutableStateFlow<Long?>(null)
        override val lastRequestNeighborTimes: StateFlow<Map<Int, Long>> = MutableStateFlow(emptyMap())

        override suspend fun requestUserInfo(destNum: Int, longName: String): Unit = error("not expected")

        override suspend fun requestNeighborInfo(destNum: Int, longName: String): Unit = error("not expected")

        override suspend fun requestPosition(destNum: Int, longName: String, position: Position): Unit =
            error("a link test never requests a position")

        override suspend fun requestTelemetry(destNum: Int, longName: String, type: TelemetryType): Unit =
            error("not expected")

        override suspend fun requestTraceroute(destNum: Int, longName: String) {
            traceroutes += destNum
            if (accept) lastTracerouteTime.value = now()
        }
    }

    private var now = 1_000_000L
    private val requests = RecordingRequests { now }
    private val service = FakeServiceRepository()
    private val demoMode = FakeDemoMode()
    private val bravo = TestTarget(3, "BRAVO-2", 1)
    private lateinit var viewModel: LinkTestViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        service.setConnectionState(ConnectionState.Connected)
        viewModel = LinkTestViewModel(requests, service, demoMode, clockMillis = { now })
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun oneTracerouteToTheChosenNodeThenAWaitOfThirtySeconds() {
        assertTrue(viewModel.traceroute(bravo))
        assertEquals(listOf(3), requests.traceroutes)

        now += 29_000
        assertFalse(viewModel.traceroute(bravo))
        assertEquals(1, LinkTestViewModel.waitSeconds(requests.lastTracerouteTime.value, now))

        now += 1_000
        assertTrue(viewModel.traceroute(bravo))
        assertEquals(listOf(3, 3), requests.traceroutes)
    }

    @Test
    fun nothingIsSentWhileTheRadioIsNotConnected() {
        service.setConnectionState(ConnectionState.Disconnected)
        assertFalse(viewModel.traceroute(bravo))
        service.setConnectionState(ConnectionState.DeviceSleep)
        assertFalse(viewModel.traceroute(bravo))
        assertTrue(requests.traceroutes.isEmpty())
    }

    @Test
    fun aRefusedRequestStartsNoWait() {
        requests.accept = false
        assertTrue(viewModel.traceroute(bravo))
        assertEquals(0, LinkTestViewModel.waitSeconds(requests.lastTracerouteTime.value, now))
        assertTrue(viewModel.traceroute(bravo))
        assertEquals(listOf(3, 3), requests.traceroutes)
    }

    @Test
    fun inDemoModeTheTestIsSimulatedAndNothingReachesTheRadio() {
        demoMode.activate()
        service.setConnectionState(ConnectionState.Disconnected)
        assertTrue(viewModel.traceroute(bravo))
        assertFalse(viewModel.traceroute(bravo))
        now += 30_000
        assertTrue(viewModel.traceroute(bravo))
        assertTrue(requests.traceroutes.isEmpty())
    }

    @Test
    fun waitSecondsRoundsUp() {
        assertEquals(0, LinkTestViewModel.waitSeconds(null, 0))
        assertEquals(30, LinkTestViewModel.waitSeconds(0, 0))
        assertEquals(1, LinkTestViewModel.waitSeconds(0, 29_999))
        assertEquals(0, LinkTestViewModel.waitSeconds(0, 30_000))
    }
}
