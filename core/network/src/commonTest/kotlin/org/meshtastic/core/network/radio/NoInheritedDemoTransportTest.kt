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
package org.meshtastic.core.network.radio

import dev.mokkery.MockMode
import dev.mokkery.mock
import kotlinx.coroutines.Dispatchers
import org.meshtastic.core.ble.BleConnectionFactory
import org.meshtastic.core.ble.BleScanner
import org.meshtastic.core.di.CoroutineDispatchers
import org.meshtastic.core.model.DeviceType
import org.meshtastic.core.repository.RadioInterfaceService
import org.meshtastic.core.repository.RadioTransport
import org.meshtastic.core.testing.FakeBluetoothRepository
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * NetworkTact removed the fake radios inherited from Meshtastic ("Demo Mode" and "Demo Mode (Replay)" in the device
 * list). A factory that does not override the gate must keep them out of the list and refuse their addresses, so a
 * previously saved choice or a deep link cannot swap the real radio for a fake one.
 */
class NoInheritedDemoTransportTest {

    private val factory =
        object :
            BaseRadioTransportFactory(
                scanner = mock<BleScanner>(MockMode.autofill),
                bluetoothRepository = FakeBluetoothRepository(),
                connectionFactory = mock<BleConnectionFactory>(MockMode.autofill),
                dispatchers =
                CoroutineDispatchers(
                    io = Dispatchers.Unconfined,
                    main = Dispatchers.Unconfined,
                    default = Dispatchers.Unconfined,
                ),
            ) {
            override val supportedDeviceTypes: List<DeviceType> = listOf(DeviceType.BLE)

            // Even a build that ships the capture asset must not bring the replay entry back.
            override val isReplayTransportAvailable: Boolean = true

            override fun createPlatformTransport(address: String, service: RadioInterfaceService): RadioTransport =
                NopRadioTransport(address)
        }

    @Test
    fun `the inherited demo gate is closed by default`() {
        assertFalse(factory.mockTransportEnabled.value)
    }

    @Test
    fun `mock and replay addresses are refused`() {
        assertFalse(factory.isAddressValid("m"))
        assertFalse(factory.isAddressValid("r"))
        assertFalse(factory.isAddressValid("m1"))
    }

    @Test
    fun `real radios are still admitted`() {
        assertTrue(factory.isAddressValid("t10.0.0.2"))
        assertTrue(factory.isAddressValid("x11:22:33:44:55:66"))
    }
}
