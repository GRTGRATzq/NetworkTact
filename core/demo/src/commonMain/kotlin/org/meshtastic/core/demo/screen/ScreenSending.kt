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

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.meshtastic.core.model.ConnectionEpochs
import org.meshtastic.core.model.ConnectionLifecycle
import org.meshtastic.core.model.ConnectionState
import org.meshtastic.core.model.DataPacket
import org.meshtastic.core.repository.ConnectionStateProvider
import org.meshtastic.core.repository.DemoMode
import org.meshtastic.core.repository.MessagingController
import org.meshtastic.core.repository.usecase.SendMessageUseCase
import org.meshtastic.proto.SharedContact

/** The screens' [SendMessageUseCase]: the real send while demo mode is off, the simulated one while it is on. */
class ScreenSendMessageUseCase(
    private val real: SendMessageUseCase,
    private val demo: SendMessageUseCase,
    demoMode: DemoMode,
) : SendMessageUseCase {
    private val switch = ScreenSwitch(demoMode)

    override suspend operator fun invoke(text: String, contactKey: String, replyId: Int?): Int =
        switch.pick({ real }, { demo }).invoke(text, contactKey, replyId)
}

/** The screens' [MessagingController]: the real one while demo mode is off, the demo one (which drops all) while on. */
class ScreenMessagingController(
    private val real: MessagingController,
    private val demo: MessagingController,
    demoMode: DemoMode,
) : MessagingController {
    private val switch = ScreenSwitch(demoMode)

    private fun current(): MessagingController = switch.pick({ real }, { demo })

    override suspend fun sendMessage(packet: DataPacket) = current().sendMessage(packet)

    override suspend fun sendReaction(emoji: String, replyId: Int, contactKey: String) =
        current().sendReaction(emoji, replyId, contactKey)

    override suspend fun importContact(contact: SharedContact) = current().importContact(contact)

    override suspend fun sendSharedContact(nodeNum: Int): Boolean = current().sendSharedContact(nodeNum)
}

/**
 * The screens' [ConnectionStateProvider]. While demo mode is on, the demo radio reads as connected so that its screens
 * accept input; the banner says that every send is simulated. The real radio's state is untouched.
 */
class ScreenConnectionStateProvider(private val real: ConnectionStateProvider, demoMode: DemoMode) :
    ConnectionStateProvider {
    private val switch = ScreenSwitch(demoMode)

    private val demoState = MutableStateFlow<ConnectionState>(ConnectionState.Connected)
    private val demoEpochs = MutableStateFlow(ConnectionEpochs(completedHandshakes = 1))
    private val demoLifecycle =
        MutableStateFlow(ConnectionLifecycle(state = ConnectionState.Connected, epochs = demoEpochs.value))

    override val connectionLifecycle: StateFlow<ConnectionLifecycle> =
        switch.state(real.connectionLifecycle, demoLifecycle)
    override val connectionState: StateFlow<ConnectionState> = switch.state(real.connectionState, demoState)
    override val connectionEpochs: StateFlow<ConnectionEpochs> = switch.state(real.connectionEpochs, demoEpochs)
}
