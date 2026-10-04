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
package org.meshtastic.feature.messaging.navigation

import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.meshtastic.core.common.util.CommonUri
import org.meshtastic.core.repository.EventFirmwareRepository
import org.meshtastic.core.repository.FirmwareUpdateStatusRepository
import org.meshtastic.core.repository.NodeRestartTracker
import org.meshtastic.core.repository.NotificationManager
import org.meshtastic.core.repository.PacketRepository
import org.meshtastic.core.testing.FakeDemoMode
import org.meshtastic.core.testing.FakeFirmwareReleaseRepository
import org.meshtastic.core.testing.FakeLockdownCoordinator
import org.meshtastic.core.testing.FakeMeshLogRepository
import org.meshtastic.core.testing.FakeNodeRepository
import org.meshtastic.core.testing.FakeRadioController
import org.meshtastic.core.testing.FakeRadioInterfaceService
import org.meshtastic.core.testing.FakeServiceRepository
import org.meshtastic.core.testing.FakeUiPrefs
import org.meshtastic.core.ui.util.AlertManager
import org.meshtastic.core.ui.util.SnackbarManager
import org.meshtastic.core.ui.viewmodel.UIViewModel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The screens hand links to [UIViewModel.handleDeepLink] by reference, typed `(CommonUri, onInvalid)` (see
 * ContactsNavigation). This test goes through that same reference: an invalid link must reach `onInvalid`, and must
 * neither end a demo nor open anything, with demo mode on or off.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HandleDeepLinkReferenceTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val demoMode = FakeDemoMode()
    private lateinit var viewModel: UIViewModel
    private lateinit var onHandleDeepLink: (CommonUri, onInvalid: () -> Unit) -> Unit

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        val scope = CoroutineScope(dispatcher)
        val packetRepository: PacketRepository = mock(MockMode.autofill)
        every { packetRepository.getUnreadCountTotal() } returns flowOf(0)
        viewModel =
            UIViewModel(
                nodeDB = FakeNodeRepository(),
                serviceRepository = FakeServiceRepository(),
                radioController = FakeRadioController(),
                lockdownCoordinator = FakeLockdownCoordinator(),
                radioInterfaceService = FakeRadioInterfaceService(scope),
                meshLogRepository = FakeMeshLogRepository(),
                firmwareReleaseRepository = FakeFirmwareReleaseRepository(),
                eventFirmwareRepository = mock<EventFirmwareRepository>(MockMode.autofill),
                firmwareUpdateStatusRepository = FirmwareUpdateStatusRepository(),
                uiPrefs = FakeUiPrefs(),
                notificationManager = mock<NotificationManager>(MockMode.autofill),
                packetRepository = packetRepository,
                alertManager = AlertManager(),
                snackbarManager = SnackbarManager(),
                nodeRestartTracker = NodeRestartTracker(scope),
                demoMode = demoMode,
            )
        onHandleDeepLink = viewModel::handleDeepLink
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun openInvalidLinks(): Int {
        var invalid = 0
        INVALID_LINKS.forEach { link -> onHandleDeepLink(CommonUri.parse(link)) { invalid++ } }
        return invalid
    }

    @Test
    fun anInvalidLinkReportsItselfOutsideADemo() {
        assertEquals(INVALID_LINKS.size, openInvalidLinks())

        assertEquals(false, demoMode.isActive.value)
        assertNull(viewModel.requestChannelSet.value)
        assertNull(viewModel.sharedContactRequested.value)
    }

    @Test
    fun anInvalidLinkReportsItselfAndLeavesTheDemoOn() {
        demoMode.activate()

        assertEquals(INVALID_LINKS.size, openInvalidLinks())

        assertTrue(demoMode.isActive.value)
        assertNull(viewModel.requestChannelSet.value)
        assertNull(viewModel.sharedContactRequested.value)
    }

    @Test
    fun aValidLinkEndsTheDemo() {
        demoMode.activate()
        var invalid = 0

        onHandleDeepLink(CommonUri.parse("meshtastic://meshtastic/messages/abc123")) { invalid++ }

        assertEquals(0, invalid)
        assertEquals(false, demoMode.isActive.value)
    }

    private companion object {
        /** A channel link with no channel in it, and an address that is no Meshtastic link at all. */
        val INVALID_LINKS = listOf("https://meshtastic.org/e/", "https://example.com/nothing")
    }
}
